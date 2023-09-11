package nablarch.test.core.http;

import static nablarch.core.util.Builder.concat;
import static nablarch.test.Assertion.assertEqualsAsString;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.util.Builder;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.test.Assertion;
import nablarch.test.core.file.FileSupport;
import nablarch.test.core.messaging.RequestTestingMessagingProvider.RequestTestingMessagingContext;
import nablarch.test.core.messaging.RequestTestingMessagingClient;

/**
 * リクエスト単体テストをテンプレート化するクラス。<br/>
 * 本クラスを使用することで、リクエスト単体テストのテストソース、テストデータを 定型化することができる。
 * <p/>
 * <pre>
 * 指定されたテストシートに定義されたテストを実行する.<br/>
 * 実行順序は以下のとおり。
 * 1. データシートからテストケースリスト(testCases LISTMAP）を取得
 * 2. 取得したテストケース分、以下を繰り返し実行
 *    1) データベース初期化
 *    2) ExecutionContext、HTTPリクエストを生成
 *    3) 業務テストコード用拡張ポイント呼出(beforeExecuteRequestメソッド）
 *    4) Tokenが必要な場合、Tokenを設定
 *    5) テスト対象のリクエスト実行
 *    6) 実行結果の検証
 *      ・HTTPステータスコード および メッセージID
 *      ・HTTPレスポンス値(リクエストスコープ値)
 *      ・検索結果
 *      ・テーブル更新結果
 *      ・フォワード先URI
 *      ・メッセージ同期送信で送信されたメッセージ
 *    7) 業務テストコード用拡張ポイント呼出(afterExecuteRequestメソッド）
 *
 * ※セッションスコープは原則利用しないため検証しない。
 * 　必要な場合afterExecuteRequestメソッドを拡張して検証すること。
 * </pre>
 * <p/>
 * テンプレートの拡張が可能となるよう、{@link TestCaseInfo}の型を総称化している。 {@link TestCaseInfo}
 * のサブクラスを作成し、本クラスのサブクラスでその型を指定することで、テンプレートを拡張できる。 以下に例を示す。 <code>
 * <pre>
 * public abstract class SpecialHttpRequestTestTemplate extends AbstractHttpRequestTestTemplate<SpecialTestCaseInfo> {
 * </pre>
 * </code>
 *
 * @param <INF> テストケース情報の型
 * @author Tsuyoshi Kawasaki
 */
@Published
public abstract class AbstractHttpRequestTestTemplate<INF extends TestCaseInfo> extends HttpRequestTestSupport {

    /** テストクラス共通データを定義しているシート名 */
    private static final String SETUP_TABLE_SHEET = "setUpDb";

    /** テストショットのLIST_MAP定義名 */
    private static final String TEST_SHOTS_LIST_MAP = "testShots";

    /** テストケースのLIST_MAP定義名（互換性維持の為） */
    private static final String TEST_CASES_LIST_MAP = "testCases";

    /** リクエストパラメータのLIST_MAP定義名 */
    private static final String REQUEST_PARAMS_LIST_MAP = "requestParams";

    /** レスポンス期待値を定義しているLIST_MAP定義名 */
    private static final String EXPECTED_RESPONSE_LIST_MAP = "responseResult";

    /** Assert対象から除外するカラム */
    private static final List<String> ASSERT_SKIP_EXPECTED_COLUMNS = Collections.singletonList("no");

    /** LIST_MAPキャッシュ */
    private final Map<String, List<Map<String, String>>> listMapCache = new HashMap<String, List<Map<String, String>>>();

    /** 何も行わない{@link Advice}実装。 */
    private final Advice<INF> nopAdvice = new Advice<INF>() {
        public void afterExecute(INF testCaseInfo, ExecutionContext context) {
        }

        public void beforeExecute(INF testCaseInfo, ExecutionContext context) {
        }
    };

    /** ファイルサポート（ファイルアップロード用に使用する） */
    private final FileSupport fileSupport;


    /** コンストラクタ。 */
    protected AbstractHttpRequestTestTemplate() {
        super();
        fileSupport = new FileSupport(getClass());
    }

    /**
     * コンストラクタ。
     *
     * @param testClass テストクラス
     */
    public AbstractHttpRequestTestTemplate(Class<?> testClass) {
        super(testClass);
        fileSupport = new FileSupport(testClass);
    }
    
    /**
     * テストを実行する。<br/>
     * 実行前後に特別な処理が不要な場合は、このメソッドを使用する。
     */
    public void execute() {
        execute(testName.getMethodName(), nopAdvice);
    }

    /**
     * テストを実行する。<br/>
     * 実行前後に特別な処理が不要な場合は、このメソッドを使用する。
     *
     * @param sheetName テスト対象のシート名
     */
    public void execute(String sheetName) {
        execute(sheetName, nopAdvice);
    }

    /**
     * テストを実行する。
     * データベースのセットアップ要否を指定できる。
     *
     * @param shouldSetUpDb データベースのセットアップ要否
     */
    public void execute(boolean shouldSetUpDb) {
        execute(testName.getMethodName(), nopAdvice, shouldSetUpDb);
    }

    /**
     * テストを実行する。
     * データベースのセットアップ要否を指定できる。
     *
     * @param sheetName     シート名
     * @param shouldSetUpDb データベースのセットアップ要否
     */
    public void execute(String sheetName, boolean shouldSetUpDb) {
        execute(sheetName, nopAdvice, shouldSetUpDb);
    }

    /**
     * テストを実行する。
     * テスト前後に特別な準備処理や結果確認処理が必要な場合はこのメソッドを使用する。
     *
     * @param advice    実行前後の処理を実装した{@link Advice}
     */
    public void execute(final Advice<INF> advice) {
        execute(testName.getMethodName(), advice, true);
    }

    /**
     * テストを実行する。
     * テスト前後に特別な準備処理や結果確認処理が必要な場合はこのメソッドを使用する。
     *
     * @param sheetName テスト対象シート名
     * @param advice    実行前後の処理を実装した{@link Advice}
     */
    public void execute(String sheetName, Advice<INF> advice) {
        execute(sheetName, advice, true);
    }

    /**
     * テストを実行する。
     *
     * @param advice        コールバック
     * @param shouldSetUpDb データベースのセットアップ要否
     */
    public void execute(Advice<INF> advice, boolean shouldSetUpDb) {
    this.execute(testName.getMethodName(), advice, shouldSetUpDb);
    }

    /**
     * テストを実行する。
     *
     * @param sheetName     シート名
     * @param advice        コールバック
     * @param shouldSetUpDb データベースのセットアップ要否
     */
    public void execute(String sheetName, Advice<INF> advice, boolean shouldSetUpDb) {
        if (StringUtil.isNullOrEmpty(sheetName)) {
            throw new IllegalArgumentException("sheetName must not null or empty.");
        }

        if (advice == null) {
            throw new IllegalArgumentException("advice must not be null.");
        }
        if (shouldSetUpDb) {
            setUpDb(SETUP_TABLE_SHEET);
        }
        // テストケースリストを取得して、ケース分テストを実施
        List<Map<String, String>> testCaseListMap = getTestCases(sheetName);
        for (Map<String, String> testCaseParams : testCaseListMap) {
            executeTestCase(sheetName, testCaseParams, advice);
        }
    }


    /**
     * テストケース一覧を取得する。<br/>
     * テストケース一覧は必須である為、取得できない場合は例外が発生する。
     *
     * @param sheetName 取得先のシート名
     * @return テストケース一覧
     */
    private List<Map<String, String>> getTestCases(String sheetName) {
        String id = TEST_SHOTS_LIST_MAP;
        List<Map<String, String>> testCases = getCachedListMap(sheetName, id);
        if (testCases.isEmpty()) {
            // 下位互換性維持の為、testCasesでも検索する
            id = TEST_CASES_LIST_MAP;
            testCases = getCachedListMap(sheetName, id);
        }
        if (testCases.isEmpty()) {
            throw new IllegalStateException(concat(
                    "testShots (LIST_MAP=", TEST_SHOTS_LIST_MAP, ") must have one or more test shots. ",
                    "check test data. sheet=[", sheetName, "]"));
        }
        return testCases;
    }

    /**
     * テストケースを実行する。
     *
     * @param sheetName      シート名
     * @param testCaseParams テストケースパラメータ
     * @param advice         実行前後の処理を実装した{@link Advice}
     */
    protected void executeTestCase(String sheetName, Map<String, String> testCaseParams,
                                   Advice<INF> advice) {

        // テストケース情報を生成
        INF testCaseInfo = createTestCaseInfo(sheetName, testCaseParams);

        clearPreviousTestData(testCaseInfo);
        
        // テストデータのセットアップ
        setUp(testCaseInfo, testCaseParams);

        // ExecutionContextの生成
        ExecutionContext context = createExecutionContext(testCaseInfo);

        // HttpRequestの生成
        fileSupport.setUpFileIfNecessary(sheetName);
        HttpRequest request = createHttpRequest(testCaseInfo);
        if (testCaseInfo.isValidToken()) {
            setValidToken(request, context);
        }
        testCaseInfo.setHttpRequest(request);
        // テストケース拡張用メソッド呼出し(リクエスト実行前）
        beforeExecuteRequest(testCaseInfo, context, advice);
        
        // テスト対象リクエストを実行
        HttpResponse response = execute(testCaseInfo.getTestCaseName(), request, context);

        // 結果検証
        assertAll(testCaseInfo, testCaseParams, context, response);

        // テストケース拡張用メソッド呼出し(リクエスト実行後）
        afterExecuteRequest(testCaseInfo, context, advice);
    }

    /**
     * テストで使用するデータのキャッシュをクリアする
     * @param testCaseInfo テストケース情報
     */
    protected void clearPreviousTestData(@SuppressWarnings("unused") INF testCaseInfo) {

        // メッセージ同期送信を行う場合に、MockMessagingContextに必要なテストケースの情報を格納する
        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingContext.clearSendingMessageCache();
 
    }

    /** 
     * 準備を行う。 
     * @param testCaseInfo テストケース情報
     * @param testCaseParams テストケースパラメータ
     */
    protected void setUp(INF testCaseInfo, Map<String, String> testCaseParams) {
        setUpDbForTestCase(testCaseInfo); // DB
        setUpMessage(testCaseInfo, testCaseParams);  // 要求電文（期待値）
    }

    /**
     * テストケース毎のデータベースセットアップを行う。
     *
     * @param testCaseInfo テストケース情報
     */
    protected void setUpDbForTestCase(INF testCaseInfo) {
        // テストシートごとのデータを投入
        if (testCaseInfo.isSetUpTable()) {
            setUpDb(testCaseInfo.getSheetName(), testCaseInfo.getSetUpTableGroupId());
        }
    }

    /** 
     * メッセージ同期送信のリクエスト単体テストを実行するための準備を行う
     * @param testCaseInfo テストケース情報
     * @param testCaseParams テストケースパラメータ
     */
    protected void setUpMessage(INF testCaseInfo, Map<String, String> testCaseParams) {
        // メッセージ同期送信を行う場合に、MockMessagingContextに必要なテストケースの情報を格納する
        RequestTestingMessagingClient.initializeForRequestUnitTesting(testClass,
                testCaseInfo.getSheetName(), testCaseInfo.getTestCaseNo(), testCaseParams.get("responseMessageByClient"), 
                testCaseParams.get("expectedMessageByClient"));
        RequestTestingMessagingContext.initializeForRequestUnitTesting(testClass,
                testCaseInfo.getSheetName(), testCaseInfo.getTestCaseNo(), testCaseParams.get("responseMessage"), 
                testCaseParams.get("expectedMessage"));
    }

    
    /**
     * テストケース情報を作成する。
     *
     * @param sheetName      シート名
     * @param testCaseParams テストケースパラメータ
     * @return 作成したテストケース情報
     */
    protected INF createTestCaseInfo(String sheetName, Map<String, String> testCaseParams) {
        // データシートに定義したリストマップを取得
        List<Map<String, String>> contexts = getCachedListMap(sheetName,
                                                              getValue(testCaseParams, TestCaseInfo.CONTEXT_LIST_MAP));
        List<Map<String, String>> requests = getCachedListMap(sheetName,
                                                              REQUEST_PARAMS_LIST_MAP);
        List<Map<String, String>> expectedResponses = getCachedListMap(sheetName,
                                                                       EXPECTED_RESPONSE_LIST_MAP);
        // Cookie(任意項目)
        List<Map<String, String>> cookie = null;
        if (testCaseParams.containsKey(TestCaseInfo.COOKIE_LIST_MAP)
                && !StringUtil.isNullOrEmpty(getValue(testCaseParams, TestCaseInfo.COOKIE_LIST_MAP))) {
            String listMapName = getValue(testCaseParams, TestCaseInfo.COOKIE_LIST_MAP);
            cookie = getCachedListMap(sheetName, listMapName);
            if (cookie.isEmpty()) {
                throw new IllegalArgumentException("Cookie LIST_MAP was not found. name = [" + listMapName + "]");
            }
        }

        // クエリパラメータ(任意項目)
        List<Map<String, String>> queryParams = null;
        if (testCaseParams.containsKey(TestCaseInfo.QUERYPARAMS_LIST_MAP)
                && !StringUtil.isNullOrEmpty(getValue(testCaseParams, TestCaseInfo.QUERYPARAMS_LIST_MAP))) {
            String listMapName = getValue(testCaseParams, TestCaseInfo.QUERYPARAMS_LIST_MAP);
            queryParams = getCachedListMap(sheetName, listMapName);
            if (queryParams.isEmpty()) {
                throw new IllegalArgumentException("Query parameter LIST_MAP was not found. name = [" + listMapName + ']');
            }
        }

        // createTestCaseInfo()は公開APIとして定義されており、個別にオーバーライドされている可能性がある。
        // そのため、後方互換性を考慮して場合分けを行う。
        if (null == queryParams) {
            return createTestCaseInfo(sheetName, testCaseParams, contexts, requests, expectedResponses, cookie);
        }
        return createTestCaseInfo(sheetName, testCaseParams, contexts, requests, expectedResponses, cookie, queryParams);

    }

    /**
     * テストケース情報を作成する。
     *
     *
     * @param sheetName         シート名
     * @param testCaseParams    テストケースパラメータ
     * @param contexts          コンテキスト全件
     * @param requests          リクエスト全件
     * @param expectedResponses 期待するレスポンス全件
     * @param cookie            本テストで使用するクッキー情報
     * @param queryParams       本テストで使用するクエリパラメータ情報
     * @return 作成したテストケース情報
     */
    @SuppressWarnings("unchecked")
    protected INF createTestCaseInfo(String sheetName,
            Map<String, String> testCaseParams,
            List<Map<String, String>> contexts,
            List<Map<String, String>> requests,
            List<Map<String, String>> expectedResponses,
            List<Map<String, String>> cookie,
            List<Map<String, String>> queryParams) {
        return (INF) new TestCaseInfo(sheetName, testCaseParams, contexts, requests, expectedResponses, cookie, queryParams);
    }

    /**
     * テストケース情報を作成する。
     *
     *
     * @param sheetName         シート名
     * @param testCaseParams    テストケースパラメータ
     * @param contexts          コンテキスト全件
     * @param requests          リクエスト全件
     * @param expectedResponses 期待するレスポンス全件
     * @param cookie            本テストで使用するクッキー情報
     * @return 作成したテストケース情報
     */
    protected INF createTestCaseInfo(String sheetName,
            Map<String, String> testCaseParams,
                                     List<Map<String, String>> contexts,
            List<Map<String, String>> requests,
            List<Map<String, String>> expectedResponses,
            List<Map<String, String>> cookie) {
        return createTestCaseInfo(sheetName, testCaseParams, contexts, requests, expectedResponses, cookie, null);
    }

    /**
     * ExecutionContextを生成する。
     *
     * @param testCaseInfo テスト情報
     * @return ExecutionContextインスタンス
     */
    protected ExecutionContext createExecutionContext(INF testCaseInfo) {
        return createExecutionContext(testCaseInfo.getUserId());
    }

    /**
     * HTTPRequestパラメータを生成する。
     *
     * @param testCaseInfo テスト情報
     * @return HttpRequestインスタンス
     */
    protected HttpRequest createHttpRequest(INF testCaseInfo) {
        String uri = getBaseUri() + testCaseInfo.getRequestId();
        return createHttpRequestWithConversion(uri, testCaseInfo.getHttpMethod(), testCaseInfo.getRequestParameters(), testCaseInfo.getCookie(), testCaseInfo.getQueryParams());
    }

    /**
     * 全アサートを実行する。<br/>
     * 以下の項目についてアサートを実施する。
     * <ul>
     * <li>HTTPステータスコードおよびメッセージID</li>
     * <li>リクエストスコープの値検証</li>
     * <li>検索結果の検証</li>
     * <li>テーブル更新結果の検証</li>
     * <li>フォワード先URI</li>
     * <li>メッセージ同期送信で送信されたメッセージ</li>
     * </ul>
     *
     * @param testCaseInfo テストケース情報
     * @param testCaseParams テストケースパラメータ
     * @param context      ExecutionContextインスタンス
     * @param response     HttpResponseインスタンス
     */
    protected void assertAll(INF testCaseInfo, Map<String, String> testCaseParams, ExecutionContext context, HttpResponse response) {
        // 結果検証
        assertResponse(testCaseInfo, response);
        assertContentLength(testCaseInfo, context);
        assertContentType(testCaseInfo, response);
        assertContentFileName(testCaseInfo, response, context);
        assertApplicationMessageId(testCaseInfo, context);
        assertRequestScopeVar(testCaseInfo, context);
        assertSqlResultSet(testCaseInfo, context);
        assertTable(testCaseInfo);
        assertForwardUri(testCaseInfo);
        
        // メッセージ同期送信で送信されたメッセージのアサートを行う
        RequestTestingMessagingClient.assertSendingMessage(
                testClass, testCaseInfo.getSheetName(), testCaseInfo.getTestCaseNo(), testCaseParams.get("expectedMessageByClient"));
        RequestTestingMessagingContext.assertSendingMessage(
                testClass, testCaseInfo.getSheetName(), testCaseInfo.getTestCaseNo(), testCaseParams.get("expectedMessage"));
    }
    
    /**
     * HTTPレスポンスオブジェクトの内容をアサートする。
     * 
     * @param testCaseInfo テストケース情報
     * @param response     レスポンスオブジェクト
     */
    protected void assertResponse(INF testCaseInfo, HttpResponse response) {
        String message  = testCaseInfo.getTestCaseName() + "[HTTP STATUS]";
        assertStatusCode(message, Integer.parseInt(testCaseInfo.getExpectedStatusCode()),  response);
    }

    /**
     * コンテンツレングス・ヘッダの値をアサートする。
     *
     * @param testCaseInfo テストケース情報
     * @param context      ExecutionContext
     */
    protected void assertContentLength(INF testCaseInfo, ExecutionContext context) {
        String expectedContentLength = testCaseInfo.getExpectedContentLength();
        if (StringUtil.isNullOrEmpty(expectedContentLength)) {
            return;
        }
        File file = getDumpFile(context);
        if (file == null) {
            Assertion.fail("dump file was not found. testCaseName = [",
                           testCaseInfo.getTestCaseName(), "]");
        }
        Assertion.assertEqualsAsString(testCaseInfo.getTestCaseName() + "[CONTENT LENGTH]",
                                       expectedContentLength, file.length());
    }

    /**
     * コンテンツタイプ・ヘッダの値をアサートする。
     *
     * @param testCaseInfo テストケース情報
     * @param response     HTTPレスポンス
     */
    protected void assertContentType(INF testCaseInfo, HttpResponse response) {
        String expectedContentType = testCaseInfo.getExpectedContentType();
        if (StringUtil.isNullOrEmpty(expectedContentType)) {
            return;
        }
        Assertion.assertEqualsAsString(testCaseInfo.getTestCaseName() + "[CONTENT TYPE]",
                                       expectedContentType, response.getContentType());
    }

    /**
     * コンテンツディスポジション・ヘッダに指定されたファイル名をアサートする。
     * <p/>
     * コンテンツタイプがHTMLの場合はアサートしない。
     *
     * @param testCaseInfo テストケース情報
     * @param response     HTTPレスポンス
     * @param context      ExecutionContext
     */
    protected void assertContentFileName(INF testCaseInfo, HttpResponse response, ExecutionContext context) {
        String expectedContentFileName = testCaseInfo.getExpectedContentFileName();
        if (StringUtil.isNullOrEmpty(expectedContentFileName)) {
            return;
        }
        String contentType = response.getContentType();
        if (contentType != null && contentType.matches("[^/]*/html?.*")) {
            return;
        }
        File file = getDumpFile(context);
        if (file == null) {
            throw new IllegalArgumentException(
                    Builder.concat("dump file was not found. testCaseName = [",
                                   testCaseInfo.getTestCaseName(), "]"));
        }
        String fileName = file.getName();
        fileName = fileName.substring(fileName.lastIndexOf("_") + 1);
        Assertion.assertEqualsAsString(testCaseInfo.getTestCaseName() + "[CONTENT FILENAME]",
                                       expectedContentFileName, fileName);
    }

    /**
     * フォワード先URIをアサートする。
     *
     * @param testCaseInfo テストケース情報
     */
    protected void assertForwardUri(INF testCaseInfo) {
        String msg = testCaseInfo.getTestCaseName() + " [forward URI] ";
        assertForward(msg, testCaseInfo.getExpectedForwardUri());
    }

    /**
     * LIST_MAPから取得したレコードから、指定したカラム名に対応する値を取得する<br/>
     *
     * @param row        行レコード(LIST_MAPの各要素）
     * @param columnName カラム名
     * @return 指定したカラム名に対応する値
     */
    private String getValue(Map<String, String> row, String columnName) {
        if (!row.containsKey(columnName)) {
            throw new IllegalArgumentException("column '" + columnName + "' is not defined.");
        }
        return row.get(columnName);
    }

    /**
     * キャッシュからLIST_MAPを取得する。<br/>
     * キャッシュにない場合は、データシートから取得しメモリ上にキャッシュする。
     *
     * @param sheetName   データシート名
     * @param listMapName LIST_MAP名
     * @return LIST_MAP
     */
    protected List<Map<String, String>> getCachedListMap(String sheetName, String listMapName) {
        if (StringUtil.isNullOrEmpty(listMapName)) {
            throw new IllegalArgumentException("LIST_MAP name parameter is null or empty.");
        }
        String key = sheetName + listMapName;
        if (!listMapCache.containsKey(key)) {
            listMapCache.put(key, getListMap(sheetName, listMapName));
        }
        return listMapCache.get(key);
    }

    /**
     * メッセージIDの検証を行う。
     *
     * @param testCaseInfo テストケース情報
     * @param context      ExecutionContextインスタンス
     */
    private void assertApplicationMessageId(INF testCaseInfo, ExecutionContext context) {
        if (!testCaseInfo.isAssertApplicationMessageId()) {
            return;
        }
        assertApplicationMessageId(testCaseInfo.getTestCaseName(),
                                   testCaseInfo.getExpectedMessageId(), context);
    }


    /**
     * リクエストスコープの値と期待値を比較検証する。
     *
     * @param testCaseInfo テストケース情報
     * @param context      ExecutionContextインスタンス
     */
    private void assertRequestScopeVar(INF testCaseInfo, ExecutionContext context) {

        if (!testCaseInfo.isAssertRequestScopeVar()) {
            return;
        }

        // 期待値とリクエスト値を比較検証する
        Map<String, String> expectedRow = testCaseInfo.getExpectedRequestScopeVar();
        for (Map.Entry<String, String> expectedColumn : expectedRow.entrySet()) {
            String assertKey = expectedColumn.getKey();
            String assertMsg = testCaseInfo.getTestCaseName() + "[" + assertKey + "]";
            if (!ASSERT_SKIP_EXPECTED_COLUMNS.contains(assertKey)) {
                Object actual = null;
                // 検証対象カラム名で値取得を試みる
                if (context.getRequestScopeMap().containsKey(assertKey)) {
                    actual = context.getRequestScopedVar(assertKey);
                }

                // 取得できない場合、prefixを利用した階層構造での値取得を試みる
                // 現在は2階層まで対応
                int prefixLength = assertKey.indexOf(".");
                if (actual == null && prefixLength != -1) {
                    String prefix = assertKey.substring(0, prefixLength);
                    String key = assertKey.substring(prefixLength + 1);
                    if (context.getRequestScopeMap().containsKey(prefix)) {
                        Map<String, Object> varMap = context.getRequestScopedVar(prefix);
                        Object actValue = varMap.get(key);
                        if (actValue != null) {
                            if (actValue.getClass().isArray()) {
                                Object[] actArray = (Object[]) actValue;
                                // 要素があれば先頭を取得
                                if (actArray.length == 0) {
                                    actual = actArray;
                                    assertMsg += " actual value is empty array.";
                                } else {
                                    actual = actArray[0];
                                }
                            } else {
                                actual = actValue;
                            }
                        }
                    }
                }
                assertEqualsAsString(assertMsg, expectedRow.get(assertKey), actual);
            }
        }
    }


    /**
     * 検索結果(SQLResultSet)を期待値と比較検証する<br/>
     * 検索結果をリクエスト格納する際のキーは、"searchResult"をデフォルトとしているので
     * キー名を変更したい場合は、getSearchResultKey()メソッドを各テストケースで オーバライドすること（暫定対応）。
     *
     * @param testCaseInfo テストケース情報
     * @param context      ExecutionContextインスタンス
     */
    private void assertSqlResultSet(INF testCaseInfo, ExecutionContext context) {
        if (!testCaseInfo.isAssertSearch()) {
            return;
        }
        String key = testCaseInfo.getSearchResultKey();
        SqlResultSet actual = context.getRequestScopedVar(key);
        assertSqlResultSetEquals(testCaseInfo.getTestCaseName(), testCaseInfo.getSheetName(),
                                 testCaseInfo.getExpectedSearchId(), actual);

    }

    /**
     * テーブル内容を期待値と比較検証する<br/>
     *
     * @param testCaseInfo テストケース情報
     */
    private void assertTable(INF testCaseInfo) {
        if (testCaseInfo.isAssertTable()) {
            String message = testCaseInfo.getTestCaseName() + "[Table TEST : "
                    + testCaseInfo.getExpectedTable() + "]";
            assertTableEquals(message, testCaseInfo.getSheetName(), testCaseInfo.getExpectedTable());
        }
    }

    /**
     * ベースURIを返却する。
     *
     * @return ベースURI
     */
    protected abstract String getBaseUri();

    /**
     * 各業務テストコードの拡張ポイント<br/>
     * テスト対象リクエストの実行前に呼び出される。<br/>
     *
     * @param testCaseInfo テストケース情報
     * @param context      ExecutionContextインスタンス
     * @param advice       実行前後の処理を実装した{@link Advice}
     */
    protected void beforeExecuteRequest(INF testCaseInfo, ExecutionContext context, Advice<INF> advice) {
        advice.beforeExecute(testCaseInfo, context);
    }

    /**
     * 各業務テストコードの拡張ポイント<br/>
     * テスト対象リクエストの実行後に呼び出される。処理が不要であれば空実装でかまわない。<br/>
     *
     * @param testCaseInfo テストケース情報
     * @param context      ExecutionContextインスタンス
     * @param advice       実行前後の処理を実装した{@link Advice}
     */
    protected void afterExecuteRequest(INF testCaseInfo, ExecutionContext context, Advice<INF> advice) {
        advice.afterExecute(testCaseInfo, context);
    }

}
