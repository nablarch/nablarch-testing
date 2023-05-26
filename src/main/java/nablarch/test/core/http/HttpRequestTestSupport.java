package nablarch.test.core.http;

import static nablarch.core.util.Builder.concat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nablarch.common.web.WebConfig;
import nablarch.common.web.WebConfigFinder;
import nablarch.common.web.token.TokenUtil;
import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.db.statement.SqlRow;
import nablarch.core.exception.IllegalConfigurationException;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.message.ApplicationException;
import nablarch.core.message.Message;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.Builder;
import nablarch.core.util.FileUtil;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpCookie;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.HttpServerFactory;
import nablarch.fw.web.MockHttpCookie;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.ResourceLocator;
import nablarch.fw.web.handler.SessionConcurrentAccessHandler;
import nablarch.fw.web.servlet.WebFrontController;
import nablarch.fw.web.upload.PartInfo;
import nablarch.fw.web.upload.PartInfoHolder;
import nablarch.test.Assertion;
import nablarch.test.NablarchTestUtils;
import nablarch.test.RepositoryInitializer;
import nablarch.test.TestSupport;
import nablarch.test.core.db.DbAccessTestSupport;
import nablarch.test.core.db.EntityTestSupport;
import nablarch.test.core.util.FileUtils;
import nablarch.test.core.util.ListWrapper;
import nablarch.test.event.TestEventDispatcher;
import nablarch.test.tool.htmlcheck.HtmlChecker;

/**
 * HTTPリクエストテスト用の基底クラス。
 *
 * @author hisaaki sioiri
 * @author Masato Inoue
 */
@Published
public class HttpRequestTestSupport extends TestEventDispatcher {

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(HttpRequestTestSupport.class);

    /** HTMLであることを判定する正規表現 */
    private static final Pattern HTML_TYPE = Pattern.compile("[^/]*/html?.*");

    /** ファイルセパレータ */
    private static char fileSeparator = File.separatorChar;

    /** HttpServer */
    private static HttpServer server;

    /** HttpTestConfigurationのリポジトリキー */
    private static final String HTTP_TEST_CONFIGURATION = "httpTestConfiguration";

    /** request processor handler */
    private static HttpRequestTestSupportHandler handler;

    /** フォワード先検証クラス */
    private static ServletForwardVerifier servletForwardVerifier = new ServletForwardVerifier();

    /** HTMLリソースコピー抑止システムプロパティ */
    private static final String SKIP_RESOURCE_COPY = "nablarch.test.skip-resource-copy";

    /** データベースアクセス自動テスト用基底クラス */
    private final DbAccessTestSupport dbSupport;

    /** 静的ファイルコピー時に内容を置き換える対象のファイルリスト */
    private List<File> replaceFiles = new ArrayList<File>();

    /** 初期化完了フラグ。 */
    private static boolean initialized;

    /** ExecutionContextにダンプファイルを格納する際に使用するキー */
    private static final String DUMP_FILE_KEY = ExecutionContext.FW_PREFIX + "testFW_dumpFile";

    /** テストクラス */
    protected final Class<?> testClass;    // SUPPRESS CHECKSTYLE サブクラスに対してカプセル化不要のため

    /** 前回{@link #execute(Class, String, nablarch.fw.web.HttpRequest, nablarch.fw.ExecutionContext)}が実行された時のクラス名 */
    private String preClassName = null;

    /**
     * コンストラクタ。
     *
     * @param testClass テストクラス
     */
    public HttpRequestTestSupport(Class<?> testClass) {
        this.testClass = testClass;
        dbSupport = new DbAccessTestSupport(testClass);
    }

    /**
     * コンストラクタ<br/>
     * 本メソッドはサブクラスから使用されることを想定している。
     */
    protected HttpRequestTestSupport() {
        this.testClass = getClass();
        dbSupport = new DbAccessTestSupport(getClass());
    }


    /**
     * 自動テスト用HTTPサーバを使用して、リクエスト単体テストを実現する。
     *
     * @param req      テスト対象のアクションを呼び出すためのHttpRequest
     * @param caseName テストケース名
     * @param ctx      ExecutionContext
     * @return HttpResponse
     */
    protected HttpResponse execute(String caseName, HttpRequest req, ExecutionContext ctx) {
        return execute(testClass, caseName, req, ctx);
    }

    /**
     * 初回時のみ初期化を実行する。
     *
     * @param config    HttpTestConfiguration
     * @param dumpDir   HTMLダンプ先のディレクトリ
     * @param className テストクラス
     */
    protected void initializeIfNotYet(HttpTestConfiguration config, File dumpDir, String className) {
        if (!initialized) {
            initialize(config, dumpDir, className);
        }
    }

    /**
     * 初期化する。
     *
     * @param config    HttpTestConfiguration
     * @param dumpDir   ダンプディレクトリ
     * @param className クラス名
     */
    private void initialize(HttpTestConfiguration config, File dumpDir, String className) {

        if (config.isBackup()) {
            backupDumpFile(config);
            // バックアップの際にディレクトリをクリーンするので、再度ダンプディレクトリを生成する
            makeDumpDir(className, config);
        }

        // 内蔵サーバ生成
        createHttpServer(config);
        initialized = true;
    }


    
    /**
     * HTMLリソースをコピーする。
     *
     * @param config  HttpTestConfiguration
     * @param dumpDir ダンプディレクトリ
     */
    private void copyHtmlResources(HttpTestConfiguration config, File dumpDir) {

        if (StringUtil.isNullOrEmpty(config.getHtmlResourcesRoot())) {
            throw new IllegalStateException("htmlResourcesRoot must not be null or empty.");
        }

        // 既にコピーされており、コピー抑止指定がされている場合はコピーしない。
        if (isResourceCopySuppressed()) {
            File dir = config.getHtmlResourcesDir();
            if (dir.exists()) {
                LOGGER.logWarn("copying HTML Resources is skipped."
                                       + "since System Property [" + SKIP_RESOURCE_COPY + "] is true, ");
                return;
            }
            LOGGER.logInfo(concat("though System Property [" + SKIP_RESOURCE_COPY + "] is true, ",
                                  "copying HTML Resources is not skipped. ",
                                  "since HTML Resource directory [", dir.getAbsolutePath(), "] is not exist."));
        }

        // 重複したリソースは上書きされないので、前から順番にコピーする。
        for (ResourceLocator warBaseLocator : getWarBasePaths(config)) {
            // ダンプディレクトリにwarディレクトリ内のHTMLリソース（jpg,cssなど）をコピーする。
            copyHtmlResourceToDumpDir(config, dumpDir, warBaseLocator);
            // HTMLリソースディレクトリ内のパス置き換え対象のファイルの内容を置き換える。
            rewriteResourceFile(config, dumpDir, warBaseLocator);
        }
    }

    /**
     * HTMLリソースのコピーを行うかどうか判定する。
     * システムプロパティの指定により判定する。
     *
     * @return 判定結果
     */
    private boolean isResourceCopySuppressed() {
        return Boolean.getBoolean(SKIP_RESOURCE_COPY);
    }

    /**
     * 自動テスト用HTTPサーバを使用して、リクエスト単体テストを実現する。
     *
     * @param testClass テストクラス
     * @param req       テスト対象のアクションを呼び出すためのHttpRequest
     * @param caseName  テストケース名
     * @param ctx       ExecutionContext
     * @return HttpResponse
     */
    public HttpResponse execute(Class<?> testClass, String caseName, HttpRequest req, ExecutionContext ctx) {

        // HTTPテスト実行用設定情報の取得
        HttpTestConfiguration config = (HttpTestConfiguration) SystemRepository.getObject(HTTP_TEST_CONFIGURATION);

        // クラス名の取得
        String className = testClass.getSimpleName();

        // ダンプディレクトリの作成
        File dumpDir = makeDumpDir(className, config);
        
        initializeIfNotYet(config, dumpDir, className);
        
        // HTMLリソースを生成する
        // リソースコピー
        if (!className.equals(preClassName)) {
            copyHtmlResources(config, dumpDir);
            preClassName = className;
        }

        // HTTPヘッダーを設定する。
        setHttpHeader(req, config);
        // HTMLダンプの設定
        String dumpFilePath = dumpDir.getAbsolutePath() + fileSeparator + caseName
                + '.' + config.getDumpFileExtension();

        server.setHttpDumpFilePath(dumpFilePath);
        // ExecutionContextの設定(ハンドラ実行中にExecutionContextの移送を行う為）
        handler.setContext(ctx);
        handler.setMultipart(req);
        // リポジトリの再初期化（指定された場合のみ）
        String xmlComponentFile = config.getXmlComponentFile();
        if (StringUtil.hasValue(xmlComponentFile)) {
            RepositoryInitializer.reInitializeRepository(xmlComponentFile);
        }

        // ダンプHTMLへの可変項目の出力可否を設定
        server.setDumpVariableItem(config.isDumpVariableItem());

        // 実行
        HttpResponse res = server.handle(req, ctx);  // 第2引数は使用されない (テスト用に引渡し）

        // アサート用にダンプファイルを設定
        setDumpFile(ctx, server.getHttpDumpFile());

        // 生成されたHTMLファイルを文法チェックする。
        String contentType = res.getContentType();
        if (config.isCheckHtml()
                && res.getStatusCode() < 500
                && contentType != null
                && HTML_TYPE.matcher(contentType).matches()) {
            checkHtml(dumpFilePath, config);
        }
        return res;
    }

    /**
     * 生成されたHtmlファイルのチェックを行う。<br/>
     * チェックする内容は下記のとおりである。
     * <ul>
     * <li>正しい文法に則って記載されていること。</li>
     * <li>使用を許可されていないタグ・属性が使用されていないこと。</li>
     * </ul>
     *
     * @param dumpFilePath チェック対象htmlファイルパス
     * @param config HttpTestConfiguration
     */
    void checkHtml(String dumpFilePath, HttpTestConfiguration config) {
        HtmlChecker checker = config.getHtmlChecker();
        
        if (checker == null) {
            // 置き換えを使わない場合のHTMLチェック(後方互換性維持のため)
            throw new RuntimeException("HtmlChecker not found. please set property htmlCheckerConfig of " + HTTP_TEST_CONFIGURATION + ".");
        }
        checker.checkHtml(new File(dumpFilePath));
    }

    /**
     * HttpServerを生成する。
     *
     * @param config HttpTestConfiguration
     * @return HTTPサーバ
     */
    protected HttpServer createHttpServer(HttpTestConfiguration config) {
        // HTTPサーバ生成
        server = createHttpServer();
        // HttpTestConfigurationの値を設定する
        server.setTempDirectory(config.getTempDirectory());
        server.setWarBasePaths(getWarBasePaths(config));
        // サーバ起動
        server.startLocal();
        handler = new HttpRequestTestSupportHandler(config);

        // ハンドラキューの準備
        WebFrontController controller = SystemRepository.get("webFrontController");
        List<Handler> handlerQueue = controller.getHandlerQueue();
        prepareHandlerQueue(handlerQueue);
        server.setHandlerQueue(handlerQueue);
        return server;
    }
    
    /**
     * サポートハンドラを取得する。
     * @return サポートハンドラ
     */
    public static HttpRequestTestSupportHandler getTestSupportHandler() {
        return handler;
    }

    /**
     * ハンドラキューの準備を行う。
     *
     * @param handlerQueue ハンドラキュー
     */
    @SuppressWarnings("rawtypes")
    protected void prepareHandlerQueue(List<Handler> handlerQueue) {

        // セッションアクセスハンドラの準備
        SessionConcurrentAccessHandler sessionHandler
                = ListWrapper.wrap(handlerQueue)
                             .select(SessionConcurrentAccessHandler.class);

        // リクエスト単体テストに必要なハンドラをハンドラキューに挿入
        servletForwardVerifier.register(handlerQueue);
        handler.register(handlerQueue);
    }

    /**
     * Warベースパスを取得する。
     *
     * @param config HttpTestConfiguration
     * @return Warベースパス
     */
    private List<ResourceLocator> getWarBasePaths(HttpTestConfiguration config) {
        String[] baseDirs = config.getWebBaseDir().split(",");
        List<ResourceLocator> basePaths = new ArrayList<ResourceLocator>();
        for (String dir : baseDirs) {
            basePaths.add(buildWarDirUri(dir));
        }
        return basePaths;
    }

    /**
     * WarディレクトリのURIを組み立てる。
     *
     * @param pathToWarDir Warディレクトリへの相対パス
     * @return URI
     */
    private ResourceLocator buildWarDirUri(String pathToWarDir) {
        return ResourceLocator.valueOf(
                "file://" + NablarchTestUtils.toCanonicalPath(pathToWarDir));
    }


    private static final String HTTP_SERVER_FACTORY_KEY = "httpServerFactory";
    /**
     * HttpServerのインスタンスを生成する。
     *
     * @return HttpServerのインスタンス
     */
    protected HttpServer createHttpServer() {
        HttpServerFactory factory = SystemRepository.get(HTTP_SERVER_FACTORY_KEY);
        if (factory == null) {
            throw new IllegalConfigurationException("could not find component. name=[" + HTTP_SERVER_FACTORY_KEY + "].");
        }
        return factory.create();
    }


    /**
     * ダンプファイルの出力ディレクトリを作成する。<br>
     *
     * @param className クラス名
     * @param config    HttpTestConfiguration
     * @return ダンプディレクトリ
     */
    private static File makeDumpDir(String className, HttpTestConfiguration config) {
        File dumpDir = new File(config.getHtmlDumpDir() + fileSeparator + className);
        FileUtils.mkdir(dumpDir);
        return dumpDir;
    }

    /**
     * HTMLリソースをダンプファイルの出力ディレクトリへコピーする。
     *
     * @param config         HttpTestConfiguration
     * @param destDir        出力ディレクトリ 
     * @param warBaseLocator warベースディレクトリのリソースロケータ
     */
    protected void copyHtmlResourceToDumpDir(HttpTestConfiguration config, File destDir, ResourceLocator warBaseLocator) {
        File warDir = new File(warBaseLocator.getRealPath());
        // 既に存在する場合は、一度削除する。
        deleteHtmlResourceFile(warDir, destDir);
        FileFilter filter = getFileFilter(config);
        FileUtils.copyDir(warDir, destDir, filter, false);
        String jsTestFileName = testClass.getSimpleName() + ".js";
        URL jsTestFilePath = testClass.getResource(jsTestFileName);
        if (jsTestFilePath == null) {
            return;
        }
        File jsTestCase = new File(jsTestFilePath.getPath().replaceAll("/", "\\" + fileSeparator));
        if (jsTestCase.exists()) {
            FileUtil.copy(jsTestCase, new File(destDir, "js" + fileSeparator + jsTestFileName));
            FileUtils.copyDir(new File(config.getJsTestResourceDir()), destDir, filter, true);
        }
    }
    
    /**
     * ダンプディレクトリのHTMLリソースファイルを削除する。
     *
     * @param srcDir HTMLリソースフォルダ
     * @param destDir HTMLリソースのコピーフォルダ
     */
    protected void deleteHtmlResourceFile(File srcDir, File destDir) {
        File[] destFiles = destDir.listFiles();
        if (destFiles == null) {
            return;
        }
        String dumpFileExtension = getConfig().getDumpFileExtension();
        for (File destFile : destFiles) {
            // htmlファイルは削除対象から除外。
            if (destFile.getName().endsWith(dumpFileExtension)) {
                continue;
            }
            File srcFile = new File(srcDir, destFile.getName());
            if (!srcFile.exists()) {
                // コピー元が存在しない場合は、削除されたファイルなので、
                // コピー先からも削除する。
                deleteBackupFile(destFile);
                continue;
            }
            if (destFile.isDirectory()) {
                deleteHtmlResourceFile(srcFile, destFile);
            } else {
                if (srcFile.lastModified() != destFile.lastModified()) {
                    // タイムスタンプが異なるものは削除する。
                    // 構成管理からのrevertも考慮して、タイムスタンプが古くなった場合も削除する。
                    deleteBackupFile(destFile);
                }
            }
        }
    }

    /** テスト用のJavaScriptリソースの配置パス */
    static String jsTestResourcePath = null;        // SUPPRESS CHECKSTYLE テストコードからの制御用

    /**
     * HTMLリソースディレクトリ内のCSSファイルを置換する。
     *
     * 出力したCSSファイルのタイムスタンプには、出力元CSSファイルのタイムスタンプを設定する。
     * 次回、出力時にはタイムスタンプに変更がない限り、出力は行わない。
     *
     * @param config         HttpTestConfiguration
     * @param dumpDir        出力先ディレクトリ
     * @param warBaseLocator warベースのリソースロケータ
     */
    protected void rewriteResourceFile(HttpTestConfiguration config, File dumpDir, ResourceLocator warBaseLocator) {

        if (jsTestResourcePath == null) {
            jsTestResourcePath = new File(config.getJsTestResourceDir()).getAbsolutePath();
        }
        String realPath = warBaseLocator.getRealPath();

        for (File file : replaceFiles) {

            BufferedReader reader = null;
            PrintWriter writer = null;

            File outputFile;
            try {
                // 出力先のファイルのwriterを生成
                String path = file.getAbsolutePath();
                if (path.startsWith(realPath)) {
                    path = path.substring(realPath.length());
                } else if (path.startsWith(jsTestResourcePath)) {
                    path = path.substring(jsTestResourcePath.length());
                }
                String relativePath = path;

                outputFile = new File(dumpDir, relativePath);
                if (file.lastModified() == outputFile.lastModified()) {
                    continue;
                }

                // 入力元のCSSファイルのReaderを生成
                InputStream fis = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(
                        fis, config.getHtmlResourcesCharset()));

                FileOutputStream fos = new FileOutputStream(outputFile);
                writer = new PrintWriter(new OutputStreamWriter(fos, config.getHtmlResourcesCharset()));

                // 一行づつ書き出す
                String line;
                while ((line = reader.readLine()) != null) {
                    String rewriteLine = rewritePath(line, relativePath);
                    writer.println(rewriteLine);
                }

                writer.flush();

            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                FileUtil.closeQuietly(reader, writer);
            }
            // 出力したファイルのタイムスタンプに出力元ファイルのタイムスタンプを設定する。
            outputFile.setLastModified(file.lastModified());
        }
    }

    /** CSSのurlパターン。 */
    private static final Pattern CSS_URL_PATTERN = Pattern.compile("(url\\(\\s*[\"']?)((/?).*[\"']?)(.*\\).*)");

    /** JSの置き換えパターン */
    private static final Map<Pattern, String> JS_REPLACED_PATTERN = new HashMap<Pattern, String>() {
        {
            put(Pattern.compile("\\{contextPath\\}"), ".");
        }
    };

    /**
     * 静的リソース内のパスを置き換える。
     *
     * @param text                 文字列
     * @param replaceAbsolutePath ファイルの絶対パスからwarのルートパスを取り除いたパス。
     * @return 置換後の文字列
     */
    protected String rewritePath(String text, String replaceAbsolutePath) {
        StringBuffer buff = new StringBuffer();
        Matcher m = CSS_URL_PATTERN.matcher(text);
        while (m.find()) {
            if (!m.group(3).isEmpty()) {
                String cssUri = getAbsoluteCssUriPrefix(m.group(2), replaceAbsolutePath);
                m.appendReplacement(buff, "$1" + cssUri + "$4");
            }
        }
        m.appendTail(buff);

        // JS
        String result = buff.toString();
        for (Entry<Pattern, String> entry : JS_REPLACED_PATTERN.entrySet()) {
            result = entry.getKey().matcher(result).replaceAll(entry.getValue());
        }
        return result;
    }


    /**
     * URI型の絶対パス参照文字列から、URIのプレフィックスを取得する。
     *
     * @param uri                 URI型の絶対パス参照文字列
     * @param replaceAbsolutePath HTMLリソースの絶対パスからwarのルートパスを取り除いたパス。
     * @return URIのプレフックス
     */
    protected String getAbsoluteCssUriPrefix(String uri, String replaceAbsolutePath) {

        int depth = getDepth(replaceAbsolutePath);

        StringBuilder builder = new StringBuilder();
        for (int count = 0; count < depth; count++) {
            builder.append("../");
        }

        return builder + uri.substring(1);
    }

    /**
     * warのルートパスからのHTMLリソースの深さを取得する。
     * 深さを判別するための区切り文字は、システムプロパティから取得する。
     *
     * @param replaceAbsolutePath HTMLリソースの絶対パスからwarのルートパスを取り除いたパス。
     * @return warのルートパスからの深さ
     */
    private int getDepth(String replaceAbsolutePath) {

        int count = 0;
        int pos = 0;
        int idx;
        String separator = System.getProperty("file.separator");

        while ((idx = replaceAbsolutePath.indexOf(separator, pos)) != -1) {
            ++count;
            pos = idx + separator.length();
        }
        return count - 1;
    }


    /**
     * FileFilterを取得する。
     *
     * @param config HttpTestConfiguration
     * @return FileFilter
     */
    protected FileFilter getFileFilter(HttpTestConfiguration config) {
        return new HtmlResourceExtensionFilter(config);
    }

    /** コピー対象となるHTMLリソースの拡張子。 */
    protected class HtmlResourceExtensionFilter implements FileFilter {

        /** HttpTestConfiguration */
        private HttpTestConfiguration configuration = null;

        /**
         * コンストラクタ。
         *
         * @param config HttpTestConfiguration
         */
        public HtmlResourceExtensionFilter(HttpTestConfiguration config) {
            configuration = config;
        }

        /**
         * 指定された拡張子を持つファイルと、全ディレクトリをコピー対象とする。
         *
         * @param file ファイルまたはディレクトリのオブジェクト。
         * @return コピー対象の場合trueを
         */
        public boolean accept(File file) {
            String name = file.getName();
            if (file.isDirectory()) {
                List<String> ignoreHtmlResourceDirectory = configuration.getIgnoreHtmlResourceDirectory();
                // コピー対象以外のディレクトリのみをコピーするようにする。
                return ignoreHtmlResourceDirectory == null || !ignoreHtmlResourceDirectory.contains(name);
            }

            for (String extension : configuration.getHtmlResourcesExtensionList()) {
                // cssファイルに関しては、コピーする前に置換する必要があるので、コピーの対象外とする
                if (name.endsWith(".css") || name.endsWith(".js") || name.endsWith(".template")) {
                    replaceFiles.add(file);
                    return false;
                }
                if (name.endsWith("." + extension)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * HTTPHeaderを設定する。<br>
     * すでにHttpRequestに設定されている項目は、設定しない。
     *
     * @param req    HTTPHeaderを設定するHttpRequest
     * @param config HttpTestConfiguration
     */
    protected static void setHttpHeader(HttpRequest req, HttpTestConfiguration config) {

        Map<String, String> headerMap = req.getHeaderMap();
        Map<String, String> configHttpHeader = config.getHttpHeader();

        for (Entry<String, String> stringStringEntry : configHttpHeader.entrySet()) {
            if (headerMap.get(stringStringEntry.getKey()) == null) {
                // 対象のキーがnullまたは、設定されていない場合のみ追加する。
                headerMap.put(stringStringEntry.getKey(), stringStringEntry.getValue());
            }
        }
    }

    /**
     * フォワード先URIが想定通りであることを表明する。
     *
     * @param msg         アサート失敗時のメッセージ
     * @param expectedUri 期待するフォワード先URI
     */
    public void assertForward(String msg, String expectedUri) {
        servletForwardVerifier.verifyForward(msg, expectedUri);
    }
    
    /**
     * ステータスコードが想定通りであることを表明する。<br/>
     * 内蔵サーバから戻り値で返却されたHTTPレスポンスがリダイレクトである場合、
     * ステータスコードが303または302であることを表明する。
     * このとき、内蔵サーバから返却されるHTTPレスポンスと比較しないのは、後方互換性を保つためである。
     * （内蔵サーバは、リダイレクト時のステータスコードに'302 FOUND'を使用する）
     *
     * 上記以外の場合は、{@link HttpRequestTestSupportHandler#getStatusCode()}
     * のステータスコードを比較対象とする。
     *
     * @param message  アサート失敗時のメッセージ
     * @param expected 期待するステータスコード値
     * @param response HTTPレスポンス
     * @see HttpRequestTestSupportHandler
     */
    protected void assertStatusCode(String message, int expected, HttpResponse response) {
        // AFW の HttpResponseHandler でレスポンスコードの詰め替えを行っており、300 系のエラー以外の
        // 場合は HttpResponse のステータスコードは 200 になってしまう。
        // このため、 300 系以外のエラーコードは handler から取得、 3XX 系のコードは HttpResponse
        // から取得してアサートする。 
        int handlerStatusCode = handler.getStatusCode();
        int responseStatusCode = response.getStatusCode();
        int actual = is3XXStatusCode(responseStatusCode) ? responseStatusCode : handlerStatusCode;
        
        Assertion.assertEquals(message, expected, actual);
    }

    /**
     * 300系の HTTP ステータスコードかどうか判定する
     * 
     * @param statusCode 判定対象のHTTPステータスコード  
     * @return 300系の HTTP ステータスコードであれば true 
     */
    public boolean is3XXStatusCode(int statusCode) {
        return 300 <= statusCode && statusCode <= 399;
    }

    /**
     * ステータスコードがリダイレクト(302 or 303)であるかどうか判定する。
     * 
     * @param statusCode ステータスコード
     * @return ステータスコードがリダイレクトであればtrue。
     */
    private boolean isRedirected(int statusCode) {
        return 302 == statusCode || statusCode == 303;
    }

    /**
     * メッセージIDのアサートを行う。<br>
     *
     * @param expectedCommaSeparated 期待するメッセージID（カンマ区切り）
     * @param actual                 実行結果(メッセージIDをリクエストスコープにもつExecutionContext)
     */
    public void assertApplicationMessageId(String expectedCommaSeparated, ExecutionContext actual) {
        assertApplicationMessageId("", expectedCommaSeparated, actual);
    }

    /**
     * メッセージIDのアサートを行う。<br>
     *
     * @param msg                    任意のメッセージ
     * @param expectedCommaSeparated 期待するメッセージID（カンマ区切り）
     * @param actual                 実行結果(メッセージIDをリクエストスコープにもつExecutionContext)
     */
    public void assertApplicationMessageId(String msg, String expectedCommaSeparated, ExecutionContext actual) {
        String[] expectedMsgIds = splitAndTrim(expectedCommaSeparated);
        assertApplicationMessageId(msg, expectedMsgIds, actual);
    }


    /**
     * 指定された文字列をカンマ(,)で分割し各要素をトリムする。
     *
     * @param commaSeparated カンマ区切り文字列
     * @return 変換した配列（引数がnullまたは空文字列の場合には、サイズ0の配列）
     */
    private String[] splitAndTrim(String commaSeparated) {
        String[] split = NablarchTestUtils.makeArray(commaSeparated.trim());
        trim(split);
        return split;
    }

    /**
     * 配列の各要素をトリムする(破壊的メソッド)。
     *
     * @param array 配列
     */
    private void trim(String[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = array[i].trim();
        }
    }

    /**
     * メッセージIDのアサートを行う。<br>
     *
     * @param expected 期待するメッセージIDの配列
     * @param actual   実行結果(メッセージIDをリクエストスコープにもつExecutionContext)
     */
    public void assertApplicationMessageId(String[] expected, ExecutionContext actual) {
        assertApplicationMessageId("", expected, actual);
    }


    /**
     * メッセージIDのアサートを行う。<br>
     *
     * @param msg      任意のメッセージ
     * @param expected 期待するメッセージIDの配列
     * @param actual   実行結果(メッセージIDをリクエストスコープにもつExecutionContext)
     */
    public void assertApplicationMessageId(String msg, String[] expected, ExecutionContext actual) {
        HttpTestConfiguration config = getConfig();
        ApplicationException applicationError = actual.getRequestScopedVar(config.getExceptionRequestVarKey());
        if (applicationError == null) {
            if (expected.length != 0) {
                // 期待値のメッセージIDが設定されている場合には、fail
                Assertion.fail("the request is normal end. message = [", msg, "]");
            }
            return;
        }

        // 実行結果
        List<String> actualMessages = new ArrayList<String>();
        for (Message message : applicationError.getMessages()) {
            actualMessages.add(message.getMessageId());
        }
        Assertion.assertEqualsIgnoringOrder(msg, Arrays.asList(expected), actualMessages);

    }

    /**
     * ダンプファイルをバックアップする。
     *
     * @param config HttpsConfigurator
     */
    private static void backupDumpFile(HttpTestConfiguration config) {
        File dumpDir = new File(config.getHtmlDumpDir());

        // ダンプディレクトリが存在しない場合はバックアップを行わない。
        if (!dumpDir.exists()) {
            return;
        }

        File backupDir = new File(config.getHtmlDumpDir() + "_bk");
        if (backupDir.exists()) {
            deleteBackupFile(backupDir);
        }
        boolean success = dumpDir.renameTo(backupDir);
        assert success;
    }

    /**
     * delete from backup file.
     *
     * @param target target file
     */
    private static void deleteBackupFile(File target) {
        if (!target.exists()) {
            return;
        }
        if (target.isDirectory()) {
            File[] files = target.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteBackupFile(file);
                } else {
                    boolean success = file.delete();
                    assert success;
                }
            }
        }
        boolean success = target.delete();
        assert success;
    }

    /**
     * 有効なトークンをリクエストパラメータとセッションスコープに設定する。<br/>
     * 二重サブミットを防止しているアクションのメソッドをテストする場合は、このメソッドを呼び出しトークンを設定する。
     *
     * @param request テスト対象のアクションを呼び出すためのHttpRequest
     * @param context ExecutionContext
     */
    public void setValidToken(HttpRequest request, ExecutionContext context) {
        setToken(request, context, true);
    }

    /**
     * トークンをリクエストパラメータとセッションスコープに設定する。<br/>
     * 引数validが真の場合、有効なトークンを設定する。偽の場合はトークンを無効にする。
     *
     * @param request テスト対象のアクションを呼び出すためのHttpRequest
     * @param context ExecutionContext
     * @param valid   有効なトークンを設定するかどうか
     */
    public void setToken(HttpRequest request, ExecutionContext context, boolean valid) {
        WebConfig webConfig = WebConfigFinder.getWebConfig();
        if (valid) {
            String token = TokenUtil.getTokenGenerator().generate();
            request.getParamMap().put(webConfig.getDoubleSubmissionTokenParameterName(), new String[]{token});
            context.getSessionScopeMap().put(webConfig.getDoubleSubmissionTokenSessionAttributeName(), token);
        } else {
            context.getSessionScopeMap().put(webConfig.getDoubleSubmissionTokenSessionAttributeName(), null);
        }
    }


    /**
     * リクエストパラメータを作成する。
     *
     * @param requestUri リクエストURI
     * @param httpMethod HTTPメソッド
     * @param params     パラメータが格納されたMap
     * @return リクエストパラメータ
     */
    public HttpRequest createHttpRequest(String requestUri, String httpMethod, Map<String, String[]> params) {

        MockHttpRequest httpRequest = new MockHttpRequest();
        httpRequest.setRequestUri(requestUri);    // URI設定
        httpRequest.setMethod(httpMethod);           // HTTPメソッド設定

        // マルチパート情報を抽出・作成
        PartInfoHolder parts = extractMultipart(params);
        httpRequest.setMultipart(parts);
        httpRequest.setParamMap(params);
        return httpRequest;
    }

    /**
     * リクエストパラメータを作成する。
     *
     * @param requestUri リクエストURI
     * @param params     パラメータが格納されたMap
     * @return リクエストパラメータ
     */
    public HttpRequest createHttpRequest(String requestUri, Map<String, String[]> params) {
        return createHttpRequest(requestUri, "POST", params);
    }

    /**
     * マルチパートの抽出を行う。
     *
     * @param params リクエストパラメータのテストデータ
     * @return 抽出されたマルチパート
     */
    private PartInfoHolder extractMultipart(Map<String, String[]> params) {
        PartInfoHolder parts = new PartInfoHolder();
        for (Entry<String, String[]> entry : params.entrySet()) {
            String name = entry.getKey();
            List<String> attachedFileNames = new ArrayList<String>();
            for (String value : entry.getValue()) {
                if (isAttached(value)) {
                    // 添付ファイルあり
                    String fileName = extractFileName(value);
                    File srcFile = new File(fileName);
                    File dstFile = new File(getConfig().getUploadTmpDirectory(), srcFile.getName());
                    FileUtils.mkdir(dstFile.getParentFile());
                    FileUtil.copy(srcFile, dstFile);
                    PartInfo part = createPartInfo(name, dstFile.getAbsolutePath());
                    parts.addPart(part);
                    attachedFileNames.add(dstFile.getName());
                }
            }
            if (!attachedFileNames.isEmpty()) {
                // 添付ファイルのHTTPパラメータを書き換え
                // key = inputタグのname属性、value = アップロードされたファイル名)
                String[] array = attachedFileNames.toArray(new String[attachedFileNames.size()]);
                params.put(name, array);
            }
        }
        return parts;
    }

    /** マルチパート指定の記法 */
    private static final String ATTACH = "${attach:";

    /** マルチパート指定の閉じ括弧 */
    private static final String ATTACH_CLOSE = "}";

    /**
     * テストデータの値がアップロード用の情報であるかどうか判定する
     *
     * @param value テストデータ
     * @return {@literal ${attach:ファイル名}}に合致する場合、真
     */
    private boolean isAttached(String value) {
        if (value.startsWith(ATTACH)) {
            if (value.endsWith(ATTACH_CLOSE)) {
                return true;
            }
            throw new IllegalStateException(concat(
                    "missing closed parenthesis. [", value, "]"));
        }
        return false;
    }

    /**
     * ファイル名を抽出する。
     *
     * @param value パラメータの値
     * @return 抽出されたファイル名
     */
    private String extractFileName(String value) {
        return value.substring(ATTACH.length(), value.length() - 1);
    }

    /**
     * マルチパート情報を作成する。
     *
     * @param name     name属性
     * @param fileName ファイル名
     * @return 作成したインスタンス
     * @throws IllegalStateException アップロードファイルが存在しない場合
     */
    private PartInfo createPartInfo(String name, String fileName) throws IllegalStateException {
        PartInfo part = PartInfo.newInstance(name);
        File attached = new File(fileName);
        if (!attached.exists()) {
            throw new IllegalStateException(Builder.concat(
                    "upload file not found. name=[", name, "]",
                    " fileName=[", fileName, "]"));
        }
        part.setSavedFile(attached);
        part.setSize((int) attached.length());
        return part;
    }

    /**
     * リクエストパラメータを作成する。
     *
     * @param requestUri     リクエストURI
     * @param httpMethod     HTTPメソッド
     * @param commaSeparated パラメータが格納されたMap
     * @param cookie         Cookie情報が格納されたMap
     * @param queryParams    クエリパラメータ情報が格納されたMap
     * @return リクエストパラメータ
     */

    public HttpRequest createHttpRequestWithConversion(String requestUri, String httpMethod,
            Map<String, String> commaSeparated, Map<String, String> cookie, Map<String, String> queryParams) {
        // リクエストパラメータの形式に変換
        Map<String, String[]> requestParameters = TestSupport.convert(commaSeparated);
        HttpRequest request = createHttpRequest(requestUri, httpMethod, requestParameters);

        // Cookieが準備データで定義されている場合は、Cookieを追加する。
        if (cookie != null && !cookie.isEmpty()) {
            HttpCookie httpCookie = new MockHttpCookie();
            httpCookie.putAll(cookie);
            ((MockHttpRequest) request).setCookie(httpCookie);
        }

        // クエリパラメータが準備データで定義されている場合は、クエリパラメータを追加する。
        if (queryParams != null && !queryParams.isEmpty()) {
            String uri = request.getRequestUri();
            uri = appendQueryParamsToUri(uri, queryParams);
            request.setRequestUri(uri);
        }
        return request;
    }

    private String appendQueryParamsToUri (String uri, Map<String, String> queryParams) {
        StringBuilder sb = new StringBuilder();
        sb.append(uri).append("?");
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            sb.append(percentEncodedString(entry.getKey()))
                .append("=")
                .append(percentEncodedString(entry.getValue()))
                .append("&");
        }
        // 最後の&を削除
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private String percentEncodedString(String rawString) {
        try {
            return URLEncoder.encode(rawString, "UTF-8");
        } catch  (UnsupportedEncodingException e) {
            // ここには来ない
            throw new RuntimeException(e);
        }
    }

    /**
     * リクエストパラメータを作成する。
     *
     * @param requestUri     リクエストURI
     * @param commaSeparated パラメータが格納されたMap
     * @param cookie         Cookie情報が格納されたMap
     * @return リクエストパラメータ
     */
    public HttpRequest createHttpRequestWithConversion(String requestUri,
            Map<String, String> commaSeparated, Map<String, String> cookie) {
        return createHttpRequestWithConversion(requestUri, "POST", commaSeparated, cookie, null);
    }

    /**
     * リクエストパラメータを作成する。
     *
     * @param requestUri     リクエストURI
     * @param httpMethod     HTTPメソッド
     * @param commaSeparated パラメータが格納されたMap
     * @param cookie         Cookie情報が格納されたMap
     * @return リクエストパラメータ
     */
    public HttpRequest createHttpRequestWithConversion(String requestUri, String httpMethod,
            Map<String, String> commaSeparated, Map<String, String> cookie) {
        return createHttpRequestWithConversion(requestUri, httpMethod, commaSeparated, cookie, null);
    }


    /**
     * {@link nablarch.fw.ExecutionContext}を生成する。
     *
     * @param userId セッションスコープに格納するユーザID
     * @return 生成したExecutionContext
     */
    public ExecutionContext createExecutionContext(String userId) {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setSessionScopedVar(getConfig().getUserIdSessionKey(), userId);
        return ctx;
    }

    /**
     * リクエスト単体テスト用のコンフィギュレーションを取得する。
     *
     * @return コンフィギュレーション
     */
    private HttpTestConfiguration getConfig() {
        return (HttpTestConfiguration) SystemRepository.getObject(HTTP_TEST_CONFIGURATION);
    }

    /**
     * HTTPレスポンスボディが出力されたファイルを取得する。
     *
     * @param ctx ExecutionContext
     * @return ファイル。HTTPダンプ出力が無効な場合はnull
     */
    protected File getDumpFile(ExecutionContext ctx) {
        return ctx.getRequestScopedVar(DUMP_FILE_KEY);
    }

    /**
     * HTTPレスポンスボディが出力されたファイルを設定する。
     *
     * @param ctx  ExecutionContext
     * @param file ファイル
     */
    protected void setDumpFile(ExecutionContext ctx, File file) {
        ctx.setRequestScopedVar(DUMP_FILE_KEY, file);
    }

    /* 以下、委譲メソッド */

    /**
     * {@link nablarch.test.core.db.DbAccessTestSupport#setUpDb(String)}への委譲メソッド。
     *
     * @param sheetName シート名
     * @see nablarch.test.core.db.DbAccessTestSupport#setUpDb(String)
     */
    public void setUpDb(String sheetName) {
        dbSupport.setUpDb(sheetName);
    }


    /**
     * {@link nablarch.test.core.db.DbAccessTestSupport#setUpDb(String, String)}への委譲メソッド。
     *
     * @param sheetName シート名
     * @param groupId   グループID
     * @see nablarch.test.core.db.DbAccessTestSupport#setUpDb(String, String)
     */
    public void setUpDb(String sheetName, String groupId) {
        dbSupport.setUpDb(sheetName, groupId);
    }


    /**
     * {@link nablarch.test.core.db.DbAccessTestSupport#assertSqlResultSetEquals(String, String, String, nablarch.core.db.statement.SqlResultSet)}
     * への委譲メソッド。
     *
     * @param message   比較失敗時のメッセージ
     * @param sheetName 期待値を格納したシート名
     * @param id        シート内のデータを特定するためのID
     * @param actual    実際の値
     * @see nablarch.test.core.db.DbAccessTestSupport#assertSqlResultSetEquals(String, String, String, nablarch.core.db.statement.SqlResultSet)
     */
    public void assertSqlResultSetEquals(String message, String sheetName, String id, SqlResultSet actual) {
        dbSupport.assertSqlResultSetEquals(message, sheetName, id, actual);
    }

    /**
     * {@link nablarch.test.core.db.DbAccessTestSupport#assertSqlRowEquals(String, String, String, nablarch.core.db.statement.SqlRow)}
     * への委譲メソッド。
     *
     * @param message   比較失敗時のメッセージ
     * @param sheetName 期待値を格納したシート名
     * @param id        シート内のデータを特定するためのID
     * @param actual    実際の値
     */
    public void assertSqlRowEquals(String message, String sheetName, String id, SqlRow actual) {
        dbSupport.assertSqlRowEquals(message, sheetName, id, actual);
    }

    /**
     * {@link nablarch.test.core.db.DbAccessTestSupport#getListMap(String, String)}への委譲メソッド。
     *
     * @param sheetName シート名
     * @param id        ID
     * @return List-Map形式のデータ
     * @see nablarch.test.core.db.DbAccessTestSupport#getListMap(String, String)
     */
    public List<Map<String, String>> getListMap(String sheetName, String id) {
        return dbSupport.getListMap(sheetName, id);
    }

    /**
     * {@link nablarch.test.core.db.DbAccessTestSupport#getListParamMap(String, String)}への委譲メソッド。
     *
     * @param sheetName シート名
     * @param id        ID
     * @return List-Map<String, String[]>形式のデータ
     * @see nablarch.test.core.db.DbAccessTestSupport#getListParamMap(String, String)
     */
    public List<Map<String, String[]>> getListParamMap(String sheetName, String id) {
        return dbSupport.getListParamMap(sheetName, id);
    }

    /**
     * {@link nablarch.test.core.db.DbAccessTestSupport#getParamMap(String, String)}への委譲メソッド。
     *
     * @param sheetName シート名
     * @param id        ID
     * @return Map<String,String[]>形式のデータ
     * @see nablarch.test.core.db.DbAccessTestSupport#getParamMap(String, String)
     */
    public Map<String, String[]> getParamMap(String sheetName, String id) {
        return dbSupport.getParamMap(sheetName, id);
    }

    /**
     * {@link nablarch.test.core.db.DbAccessTestSupport#assertTableEquals(String)}への委譲メソッド。
     *
     * @param sheetName 期待値を格納したシート名
     * @see nablarch.test.core.db.DbAccessTestSupport#assertTableEquals(String)
     */
    public void assertTableEquals(String sheetName) {
        dbSupport.assertTableEquals(sheetName);
    }

    /**
     * {@link nablarch.test.core.db.DbAccessTestSupport#assertTableEquals(String, String)}への委譲メソッド。
     *
     * @param sheetName 期待値を格納したシート名
     * @param groupId   グループID（オプション）
     * @see nablarch.test.core.db.DbAccessTestSupport#assertTableEquals(String, String)
     */
    public void assertTableEquals(String sheetName, String groupId) {
        dbSupport.assertTableEquals(sheetName, groupId);
    }

    /**
     * {@link nablarch.test.core.db.DbAccessTestSupport#assertTableEquals(String, String, String)} への委譲メソッド。
     *
     * @param message   比較失敗時のメッセージ
     * @param groupId   グループID（オプション）
     * @param sheetName 期待値を格納したシート名
     * @see nablarch.test.core.db.DbAccessTestSupport#assertTableEquals(String, String, String)
     */
    public void assertTableEquals(String message, String sheetName, String groupId) {
        dbSupport.assertTableEquals(message, sheetName, groupId);
    }

    /**
     * {@link nablarch.test.core.db.EntityTestSupport#assertGetterMethod(String, String, Object)}への移譲メソッド。
     *
     * @param sheetName シート名
     * @param id        ケース表のID(LIST_MAP=testの場合は、testを指定する。)
     * @param actual    実行結果のオブジェクト(Java Beansオブジェクト)
     */
    public void assertEntity(String sheetName, String id, Object actual) {
        EntityTestSupport entityTestSupport = new EntityTestSupport(testClass);
        entityTestSupport.assertGetterMethod(sheetName, id, actual);
    }

    /**
     * Object に設定されたプロパティをアサートする。 <br />
     * チェック条件の詳細は {@link nablarch.test.Assertion#assertProperties(java.util.Map, Object)} を参照。
     *
     * @param message   メッセージ
     * @param sheetName シート名
     * @param id        ケース表のID(LIST_MAP=testの場合は、testを指定する。)
     * @param actual    実際の値
     */
    public void assertObjectPropertyEquals(String message, String sheetName, String id, Object actual) {
        List<Map<String, String>> list = getListMap(sheetName, id);
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("no data row. sheetName=[" + sheetName + "], id=[" + id + "]");
        }
        Assertion.assertProperties(message, list.get(0), actual);
    }


    /**
     * Object配列に設定されたプロパティをアサートする。 <br />
     * チェック条件の詳細は {@link nablarch.test.Assertion#assertProperties(java.util.Map, Object)} を参照。
     *
     * @param message   メッセージ
     * @param sheetName シート名
     * @param id        ケース表のID(LIST_MAP=testの場合は、testを指定する。)
     * @param actual    実際の値
     */
    public void assertObjectArrayPropertyEquals(String message, String sheetName, String id, Object[] actual) {
        List<Map<String, String>> list = getListMap(sheetName, id);
        if (actual == null) {
            if (list == null || list.isEmpty()) {
                // OK
                return;
            }

            Assertion.failComparing(message + "; target size does not match; ", list.size(), null);
        }

        if (actual.length != list.size()) {
            Assertion.failComparing(message + "; target size does not match; ", list.size(), actual.length);
        }

        for (int i = 0; i < list.size(); i++) {
            Assertion.assertProperties(message + "; target index [" + i + "]", list.get(i), actual[i]);
        }
    }


    /**
     * Object に設定されたプロパティをアサートする。 <br />
     * チェック条件の詳細は {@link nablarch.test.Assertion#assertProperties(java.util.Map, Object)} を参照。
     *
     * @param message   メッセージ
     * @param sheetName シート名
     * @param id        ケース表のID(LIST_MAP=testの場合は、testを指定する。)
     * @param actual    実際の値
     */
    public void assertObjectListPropertyEquals(String message, String sheetName, String id, List<?> actual) {
        Object[] array = actual == null ? null : actual.toArray();
        assertObjectArrayPropertyEquals(message, sheetName, id, array);
    }

    /**
     * キャッシュした HttpServer をリセットする。
     */
    public static void resetHttpServer() {
        servletForwardVerifier = new ServletForwardVerifier();
        initialized = false;
    }
}
