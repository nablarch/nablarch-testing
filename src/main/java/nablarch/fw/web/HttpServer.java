package nablarch.fw.web;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.Builder;
import nablarch.core.util.FileUtil;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.fw.Handler;
import nablarch.fw.HandlerQueueManager;
import nablarch.fw.handler.GlobalErrorHandler;
import nablarch.fw.web.handler.ForwardingHandler;
import nablarch.fw.web.handler.HttpCharacterEncodingHandler;
import nablarch.fw.web.handler.HttpErrorHandler;
import nablarch.fw.web.handler.HttpResponseHandler;
import nablarch.fw.web.servlet.WebFrontController;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * エンベディドHTTPサーバー&サーブレットコンテナ。
 * <pre>
 * 主に単体テスト時の画面確認や打鍵テストで使用することを想定した、
 * JVMプロセス内の1スレッドとして動作する軽量アプリケーションサーバである。
 * 現行の実装では、内部的にJettyサーバを使用しており、
 * 本クラスは単なるラッパーに過ぎない。
 * </pre>
 *
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
@Published(tag = "architect")
public abstract class HttpServer extends HandlerQueueManager<HttpServer> implements HttpRequestHandler {

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(HttpServer.class);

    /** HTMLパターン */
    private static final Pattern HTML_PATTERN = Pattern.compile("[^/]*/html?.*");

    /** スラッシュ */
    private static final Pattern SLASH = Pattern.compile("/");

    /** 拡張子を抜き出すための正規表現 */
    private static final Pattern EXTENSION_PATTERN = Pattern.compile("^.*?/(.*?)(;.*)?$");

    /**
     * サーバを起動する。
     *
     * @return このオブジェクト自体
     */
    public abstract HttpServer start();


    /**
     * このサーバをテストモードで起動する。
     * @return このオブジェクト自体
     */
    public abstract HttpServer startLocal();

    /**
     * サーバスレッドが終了するまでカレントスレッドをwaitさせる。
     *
     * @return このオブジェクト自体
     */
    public abstract HttpServer join();


    /**
     * {@inheritDoc}
     * この実装では、サーバが処理を委譲するフロントコントローラ内の
     * ハンドラキューを返す。
     */
    @Override
    public List<Handler> getHandlerQueue() {
        return controller.getHandlerQueue();
    }
    
    /** 処理を委譲するサーブレットフィルタ */
    private final WebFrontController controller = new WebFrontController();

    /** サーバインスタンスを生成する。 */
    public HttpServer() {
        setHandlerQueue(Arrays.asList(
                new GlobalErrorHandler()
                , new HttpCharacterEncodingHandler()
                , new HttpResponseHandler()
                , new ForwardingHandler()
                , new HttpErrorHandler()));
        setMethodBinder(new HttpMethodBinding.Binder());
    }

    protected WebFrontController getWebFrontController() {
        return controller;
    }

    /**
     * 本サーバにデプロイされるWARのコンテキストパスを返す。
     *
     * @return コンテキストパス
     */
    public String getServletContextPath() {
        return servletContextPath;
    }

    /**
     * ベースURIを設定する。
     * <pre>
     * 本サーバにデプロイされるWARのコンテキストパスを設定する。
     * デフォルト値は"/"である。
     * </pre>
     *
     * @param path コンテキストパス
     * @return このオブジェクト自体
     */
    public HttpServer setServletContextPath(String path) {
        servletContextPath = path;
        return this;
    }

    /** 本サーバにデプロイされるWARのコンテキストパス */
    private String servletContextPath = "/";

    /**
     * このサーバが使用するポート番号を取得する。
     *
     * @return ポート番号
     */
    public int getPort() {
        return this.port;
    }

    /**
     * このサーバが使用するポート番号を設定する。
     * <pre>
     * デフォルトのポート番号は7777である。
     * </pre>
     *
     * @param port ポート番号
     * @return このオブジェクト自体
     */
    public HttpServer setPort(int port) {
        assert port > 0;
        this.port = port;
        return this;
    }

    /** このサーバが使用するポート番号 */
    private int port = 7777;

    /**
     * このサーバにデプロイするWARのパスを設定する。
     * <pre>
     * 明示的に指定しなかった場合のデフォルト値は、
     * "classpath://nablarch/fw/web/servlet/docroot/"
     * </pre>
     *
     * @param warBasePath このサーバにデプロイするWARのパス
     * @return このオブジェクト自体
     */
    public HttpServer setWarBasePath(String warBasePath) {
        this.warBasePaths = toLocatorList(warBasePath);
        return this;
    }

    /**
     * このサーバにデプロイするWARのパスを複数設定する。
     * 引数で渡されたリストの順にリソースが探索される。
     *
     * <pre>
     * 明示的に指定しなかった場合のデフォルト値は、
     * "classpath://nablarch/fw/web/servlet/docroot/"
     * </pre>
     *
     * @param warBasePaths このサーバにデプロイするWARのパス
     * @return このオブジェクト自体
     */
    public HttpServer setWarBasePaths(List<ResourceLocator> warBasePaths) {
        this.warBasePaths = warBasePaths;
        return this;
    }

    /**
     * 文字列のパスを{@link ResourceLocator}のリストに変換する。
     * @param path 変換元のパス
     * @return 変換後の {@link ResourceLocator}のリスト
     */
    private List<ResourceLocator> toLocatorList(String path) {
        return Arrays.asList(getResourceLocatorOf(path));
    }


    /**
     * 指定されたWarディレクトリに対応する{@link ResourceLocator}を取得する。
     * @param pathToWarDir Warディレクトリへのパス
     * @return Warディレクトリへのパスを表す{@link ResourceLocator}
     */
    private ResourceLocator getResourceLocatorOf(String pathToWarDir) {

        ResourceLocator path = ResourceLocator.valueOf(pathToWarDir);
        String scheme = path.getScheme();
        if (scheme.equals("servlet") || scheme.equals("forward")) {
            throw new IllegalArgumentException("invalid path: " + pathToWarDir);
        }

        if (path.getRealPath() == null) {
            throw new IllegalArgumentException(
                    "there was no war archive or context-root path at: " + pathToWarDir
            );
        }

        if (scheme.equals("classpath") && (path.getRealPath().startsWith("jar:") || path.getRealPath().startsWith("jrt:"))) {
            throw new IllegalArgumentException(
                    "WAR base path can not be a JAR interior path. "
                            +  "Assign the path of the WAR archive itself "
                            +  "or a context-root directory path of a extracted WAR archive."
            );
        }
        return path;
    }



    /**
     * このサーバにデプロイするWARのパスを返す。
     * @return このサーバにデプロイするWARのパス
     */
    public ResourceLocator getWarBasePath() {
        return warBasePaths.get(0);     // 互換性維持のために先頭にひとつを返却する。
    }

    /** このサーバにデプロイするWARのパス。 */
    private List<ResourceLocator> warBasePaths
            = Arrays.asList(ResourceLocator.valueOf("classpath://nablarch/fw/web/servlet/docroot/"));


    protected List<ResourceLocator> getWarBasePaths() {
        return warBasePaths;
    }

    /**
     * HTTPダンプ機能の有効化/無効化を設定する。
     * <pre>
     * デフォルトでは無効化されている。
     * </pre>
     * 
     * @param enabled 
     *     HTTPダンプを有効化する場合はtrue、無効化する場合はfalseを設定する。
     * @return
     *     このオブジェクト自体
     * @see #setHttpDumpRoot(String)
     */
    public HttpServer setHttpDumpEnabled(boolean enabled) {
        httpDumpEnabled = enabled;
        return this;
    }
    
    /**
     * HTTPダンプ機能が有効化されているか？
     * @return HTTPダンプ機能が有効であればtrueを返す。
     */
    public boolean isHttpDumpEnabled() {
        return httpDumpEnabled;
    }
    
    /** HTTPダンプ機能の有効/無効化設定。 */
    private boolean httpDumpEnabled = false;
    
    /**
     * HTTPダンプの出力先フォルダのパスを設定する。
     * また、HTTPダンプ機能が無効化されている場合は有効化される。
     * <pre>
     * デフォルトでは、カレントパス直下の"http_dump"ディレクトリになる。
     * ダンプ出力時に当該のディレクトリが存在しなかった場合は自動的に作成される。
     * </pre>
     * @param path HTTPダンプの出力先フォルダ
     * @return このオブジェクト自体
     */
    public HttpServer setHttpDumpRoot(String path) {
        httpDumpRoot = new File(path);
        httpDumpFile = null;
        setHttpDumpEnabled(true);
        return this;
    }
    /**
     * HTTPダンプの出力先フォルダを返す。
     * @return HTTPダンプの出力先フォルダ
     */
    public File getHttpDumpRoot() {
        return httpDumpRoot;
    }
    /** HTTPダンプの出力先フォルダ */
    private File httpDumpRoot = new File("http_dump");
        
    /**
     * HTTPダンプの出力先ファイルパスを指定する。
     * @param path 出力先ファイルパス
     * @return このオブジェクト自体
     */
    public HttpServer setHttpDumpFilePath(String path) {
        httpDumpFile = new File(path);
        setHttpDumpEnabled(true);
        return this;
    }

    /**
     * HTTPダンプの出力先ファイルを取得する。
     * @return 出力先ファイル
     */
    public File getHttpDumpFile() {
        return httpDumpFile;
    }
    
    /** HTTPダンプの出力先ファイル */
    private File httpDumpFile = null;

    /**
     * context用の一時ディレクトリパスを指定する。
     *
     * 本パスを指定した場合、jspのコンパイル後のクラスなどはこのディレクトリ配下に保存される。
     * @param path context用の一時ディレクトリパス
     * @return このオブジェクト自体
     */
    public HttpServer setTempDirectory(String path) {
        if (path != null) {
            tempDirectory = new File(path);
        }
        return this;
    }

    /** context用の一時ディレクトリパス */
    private File tempDirectory;

    protected File getTempDirectory() {
        return tempDirectory;
    }


    /**
     * ダンプHTMLへの可変項目の出力可否。
     */
    private boolean dumpVariableItem = false;

    /**
     * ダンプHTMLへの可変項目の出力可否を設定する。
     * @param dumpVariableItem dumpVariableItem ダンプHTMLへの可変項目の出力可否
     */
    public void setDumpVariableItem(boolean dumpVariableItem) {
        this.dumpVariableItem = dumpVariableItem;
    }

    /** jsessionidの正規表現 */
    private static final Pattern JSESSIONID_PATTERN = Pattern
            .compile(";jsessionid=[a-zA-Z0-9]+");

    /** nablarch_tokenの正規表現 */
    private static final Pattern NABLARCH_TOKEN_PATTERN = Pattern
            .compile("nablarch_token=[a-zA-Z0-9\\+\\-=/]+");

    /**
     * HTTPレスポンスボディをローカルファイルに出力する。
     * 
     * @param req HTTPリクエストオブジェクト
     * @param res HTTPレスポンスオブジェクト
     */
    protected void dumpHttpMessage(HttpRequest req, HttpResponse res) {
        if (httpDumpFile != null && !httpDumpRoot.exists()) {
            if (!httpDumpRoot.mkdirs()) {
                LOGGER.logWarn(
                "couldn't create the directory to which http dump is written."
                );
            }
        }
        
        boolean isHtml = HTML_PATTERN.matcher(res.getContentType()).matches();
        
        if (!isHtml) {
            // HTML以外の場合はContent-Dispositionヘッダに指定されたファイル名の取得を試みる。
            File file = getFileByContentDisposition(res);
            if (file != null) {
                httpDumpFile = file;
            }
        }

        if (httpDumpFile == null) {
            httpDumpFile = new File(httpDumpRoot, getHttpDumpFileName(req, res));
        }
        
        Closeable closeable = null;
        try {
            if (!httpDumpFile.exists()) {
                boolean success = httpDumpFile.createNewFile();
                assert success : httpDumpFile;
            }
            if (isHtml) {
                InputStream in = res.getBodyStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len;
                byte[] buf = new byte[1024];
                while ((len = in.read(buf)) != -1) {
                    baos.write(buf, 0, len);
                }
                String html = new String(baos.toByteArray(), res.getCharset().toString());
                html = rewriteUriPath(html, req);
                if (dumpVariableItem) {
                    html = JSESSIONID_PATTERN.matcher(html).replaceAll("");
                    html = NABLARCH_TOKEN_PATTERN.matcher(html).replaceAll("");
                }
                closeable = new BufferedWriter(new OutputStreamWriter(
                                new FileOutputStream(httpDumpFile), res.getCharset()));
                Writer writer = (Writer) closeable;
                writer.append(html).flush();
            } else {
                // バイナリとして書き出す。
                closeable = new BufferedOutputStream(new FileOutputStream(httpDumpFile));
                OutputStream out = (OutputStream) closeable;
                InputStream in = res.getBodyStream();
                int len;
                byte[] buf = new byte[512];
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                out.flush();
            }
        } catch (IOException e) {
            String message = "an error occurred while the http dump was being written."
                    + " make sure dump file path is valid (especially file name)."
                    + " path = [" + httpDumpFile.getPath() + "]";
            LOGGER.logWarn(message, e);
            throw new RuntimeException(message, e);
        } finally {
            FileUtil.closeQuietly(closeable);
        }

    }

    /** Content-Dispositionヘッダの正規表現 */
    private static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile(".*; filename=\"(.+)\".*");

    /**
     * Content-Dispositionヘッダに指定されたファイル名を用いて、
     * HTTPダンプの出力先フォルダにレスポンスを出力するためのファイルを返す。
     * <p/>
     * Content-Dispositionヘッダにファイル名が指定されていない場合はnullを返す。
     * @param res レスポンス
     * @return ファイル
     */
    private File getFileByContentDisposition(HttpResponse res) {
        String contentDisposition = res.getContentDisposition();
        if (StringUtil.isNullOrEmpty(contentDisposition)) {
            return null;
        }
        Matcher m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
        if (!m.find()) {
            return null;
        }
        String httpDumpFilePath = httpDumpFile.getAbsolutePath();
        String basePathAndFileNamePrefix = httpDumpFilePath.substring(0, httpDumpFilePath.lastIndexOf('.'));
        try {
            return new File(basePathAndFileNamePrefix + '_' + URLDecoder.decode(m.group(1), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOGGER.logWarn("an error occurred while url decoding for the file name of the Content-Disposition header.", e);
            throw new RuntimeException(e);
        }
    }
    /**
     * HTML中のURI型の参照先を、ダンプHTML用に書き換える。
     * 
     * @param html HTML文字列
     * @param req HttpRequest
     * @return 編集後文字列
     */
    protected String rewriteUriPath(String html, HttpRequest req) {

        String relativeUriPrefix = '.' + getRelativeUriPrefix(req);

        StringBuffer buff = new StringBuffer();
        Matcher m = URI_TYPED_ATTRIBUTES.matcher(html);
        while (m.find()) {
            String hostRoot = (m.group(3).isEmpty())
                            ? relativeUriPrefix : ".";
            m.appendReplacement(buff, "$1" + hostRoot + "$2");
        }
        m.appendTail(buff);
        return buff.toString();
    }
    
    /**
     * URI型の相対パス参照文字列から、URIのプレフィックスを取得する。
     * 
     * @param req HTTPリクエストオブジェクト
     * @return URIのプレフックス
     */
    private String getRelativeUriPrefix(HttpRequest req) {
        
        String resourcePath = req.getRequestUri().substring(0, req.getRequestUri().lastIndexOf('/') + 1);
        return resourcePath;
    }
    
    
    /** %URI型の属性をもつタグ */
    private static final String TAG_HAS_URI_ATTRIBUTES
        = "(?:a|area|base|link|img|object|q|blockquote|input|head|script)";
    /** %URI型の属性名 */
    private static final String URI_TYPED_ATTRIBUTE_NAMES
        = "(?:href|src|longdesc|usemap|classid|codebase|data|cite|action)";
    /** %URI型の属性 */
    private static final Pattern URI_TYPED_ATTRIBUTES = Pattern.compile(
    Builder.linesf(
      "(                   " // Capture#1: 対象属性を含むタグの先頭から置換対象直前までの文字列
    , "  <%s\\s+           ", TAG_HAS_URI_ATTRIBUTES
    , "    [^>]*?          "
    , "  %s\\s*=\\s*[\"']? ", URI_TYPED_ATTRIBUTE_NAMES
    , ")                   "
    , "((/?)[^\\s\"';]*)   " // Capture#2: 対象属性値 #3: 絶対パスかどうか
    , "(;jsessionid=[a-zA-Z0-9]+)?" 
    ), Pattern.COMMENTS | Pattern.CASE_INSENSITIVE);

    
    /**
     * HTTPレスポンスボディをローカルファイルに出力する際に使用するファイル名。
     * 
     * @param req HTTPリクエストオブジェクト
     * @param res HTTPレスポンスオブジェクト
     * @return ファイル名
     */
    private String getHttpDumpFileName(HttpRequest req, HttpResponse res) {
        DateFormat format = new SimpleDateFormat("yyyy-MMdd-HHmmss-SSS_");
        String extension = EXTENSION_PATTERN.matcher(res.getContentType()).replaceAll(".$1");
        return format.format(new Date())
                + req.getMethod()
                + SLASH.matcher(req.getRequestUri()).replaceAll("_")
                + "=="
                + res.getStatusCode()
                + '-'
                + res.getReasonPhrase()
                + extension;
    }
}
