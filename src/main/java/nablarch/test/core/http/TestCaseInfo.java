package nablarch.test.core.http;

import nablarch.core.util.Builder;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.fw.web.HttpRequest;

import java.util.List;
import java.util.Map;


/**
 * データシートに定義されたテストケース情報を格納するクラス。<br/>
 *
 * @author Tsuyoshi Kawasaki
 *
 */
@Published
public class TestCaseInfo {

    /** ユーザIDを定義しているカラム名 */
    protected static final String USER_ID_COLUMN_NAME = "USER_ID";

    /** リクエストIDを定義しているカラム名 */
    protected static final String REQUEST_ID = "REQUEST_ID";

    /** HTTPメソッドを定義しているカラム名 */
    protected static final String HTTP_METHOD_COLUMN_NAME = "HTTP_METHOD";

    /** テストケース番号を定義しているカラム名 */
    protected static final String TEST_CASE_NO = "no";

    /** テストケースを記述しているカラム名 */
    protected static final String CASE = "case";

    /** ショットの説明を記述しているカラム名 */
    protected static final String DESCRIPTION = "description";

    /** コンテキストパラメータのLIST_MAP定義名 */
    protected static final String CONTEXT_LIST_MAP = "context";

    /** CookieのLIST_MAP定義名 */
    protected static final String COOKIE_LIST_MAP = "cookie";
    /** CookieのLIST_MAP定義名 */
    protected static final String QUERYPARAMS_LIST_MAP = "queryParams";

    /** トークンを設定するかどうかを記述しているカラム名 */
    protected static final String IS_VALID_TOKEN = "isValidToken";

    /** データベースにデータを投入する際のグループIDを表すカラム名 */
    protected static final String SET_UP_TABLE = "setUpTable";

    /** HTTPステータスコードの期待値を定義しているカラム名(LIST_MAP＝テストケース） */
    protected static final String EXPECTED_HTTP_STATUS_COLUMN = "expectedStatusCode";

    /** HTTPステータスコードの期待値を定義しているカラム名(LIST_MAP＝テストケース） */
    protected static final String EXPECTED_MESSAGE_ID_COLUMN = "expectedMessageId";

    /** 検索結果の期待値を定義しているカラム名(LIST_MAP＝テストケース） */
    protected static final String EXPECTED_SEARCH_COLUMN = "expectedSearch";

    /** テーブル期待値を定義しているカラム名(LIST_MAP＝テストケース） */
    protected static final String EXPECTED_TABLE_COLUMN = "expectedTable";

    /** 要求電文の期待値を定義しているカラム名(LIST_MAP＝テストケース） */
    protected static final String EXPECTED_MESSAGE_COLUMN = "expectedMessage";

    /** 応答電文を定義しているカラム名(LIST_MAP＝テストケース） */
    protected static final String RESPONSE_MESSAGE_COLUMN = "responseMessage";
  
    /** リクエストスコープに格納する検索結果のキー名 */
    protected static final String DEFAULT_SEARCH_RESULT_KEY = "searchResult";

    /** 期待するフォワードURIを定義しているカラム名(LIST_MAP＝テストケース） */
    protected static final String EXPECTED_FORWARD_URI = "forwardUri";

    /** コンテンツレングス・ヘッダの期待値を定義しているカラム名(LIST_MAP＝テストケース） */
    protected static final String EXPECTED_CONTENT_LENGTH = "expectedContentLength";

    /** コンテンツタイプ・ヘッダの期待値を定義しているカラム名(LIST_MAP＝テストケース） */
    protected static final String EXPECTED_CONTENT_TYPE = "expectedContentType";

    /** コンテンツディスポジション・ヘッダに指定されたファイル名の期待値を定義しているカラム名(LIST_MAP＝テストケース） */
    protected static final String EXPECTED_CONTENT_FILENAME = "expectedContentFileName";

    /** シート名 */
    private String sheetName;
    /** テストケース毎のパラメータ */
    private Map<String, String> testCaseParams;
    /** コンテキスト */
    private List<Map<String, String>> context;
    /** リクエスト */
    private List<Map<String, String>> request;
    /** 期待するレスポンスのパラメータ */
    private List<Map<String, String>> expectedResponseParams;
    /** Cookie情報 */
    private List<Map<String, String>> cookie;
    /** クエリパラメータ情報 */
    private List<Map<String, String>> queryParams;

    /** リクエストスコープ値アサートを行うか？（各テストケースで個別検証する場合にfalseを設定） */
    private boolean isAssertRequestScopeVar = true;

    /** メッセージIDのアサートを行うか？（各テストケースで個別検証する場合にfalseを設定） */
    private boolean isAssertApplicationMessageId = true;

    /** リクエストスコープ内に格納された検索結果を取得するためのキー */
    private String searchResultKey = DEFAULT_SEARCH_RESULT_KEY;

    /**
     * リクエストスコープ内に格納された検索結果を取得するためのキーを取得する。
     * @return キー
     */
    public String getSearchResultKey() {
        return searchResultKey;
    }

    /**
     * リクエストスコープ内に格納された検索結果を取得するためのキーを設定する。
     * @param searchResultKey キー
     */
    public void setSearchResultKey(String searchResultKey) {
        this.searchResultKey = searchResultKey;
    }

    /**
     * コンストラクタ
     *
     * @param sheetName テストシート名
     * @param testCaseParams テストケースパラメータ
     * @param context スレッドコンテキスト・ユーザコンテキスト生成用パラメータ
     * @param request リクエストパラメータ
     * @param expectedResponseListMap レスポンス期待値パラメータ
     */
    public TestCaseInfo(String sheetName, Map<String, String> testCaseParams,
            List<Map<String, String>> context,
            List<Map<String, String>> request,
            List<Map<String, String>> expectedResponseListMap) {
        this(sheetName, testCaseParams, context, request, expectedResponseListMap, null);
    }

    /**
     * コンストラクタ
     *
     * @param sheetName テストシート名
     * @param testCaseParams テストケースパラメータ
     * @param context スレッドコンテキスト・ユーザコンテキスト生成用パラメータ
     * @param request リクエストパラメータ
     * @param expectedResponseListMap レスポンス期待値パラメータ
     * @param cookie Cookie情報
     */
    public TestCaseInfo(String sheetName, Map<String, String> testCaseParams,
            List<Map<String, String>> context,
            List<Map<String, String>> request,
            List<Map<String, String>> expectedResponseListMap,
            List<Map<String, String>> cookie) {
        this(sheetName, testCaseParams, context, request, expectedResponseListMap, cookie, null);
    }

    /**
     * コンストラクタ
     *
     * @param sheetName テストシート名
     * @param testCaseParams テストケースパラメータ
     * @param context スレッドコンテキスト・ユーザコンテキスト生成用パラメータ
     * @param request リクエストパラメータ
     * @param expectedResponseListMap レスポンス期待値パラメータ
     * @param cookie Cookie情報
     * @param queryParams クエリパラメータ情報
     */
    public TestCaseInfo(String sheetName, Map<String, String> testCaseParams,
            List<Map<String, String>> context,
            List<Map<String, String>> request,
            List<Map<String, String>> expectedResponseListMap,
            List<Map<String, String>> cookie,
            List<Map<String, String>> queryParams) {
        this.sheetName = sheetName;
        this.testCaseParams = testCaseParams;
        this.context = context;
        this.request = request;
        this.expectedResponseParams = expectedResponseListMap;
        this.cookie = cookie;
        this.queryParams = queryParams;
    }

    /**
     * メッセージID（期待値）を返却する.<br/>
     *
     * @return メッセージID（複数存在する場合はカンマ区切り）
     */
    public String getExpectedMessageId() {
        return getValue(testCaseParams, EXPECTED_MESSAGE_ID_COLUMN);
    }

    /**
     * HTTPレスポンス（期待値）を返却する.<br/>
     *
     * @return リクエストスコープに設定されるはずの期待値
     */
    public Map<String, String> getExpectedRequestScopeVar() {
        return expectedResponseParams.get(Integer.valueOf(getTestCaseNo()) - 1);
    }

    /**
     * 検索結果検証の期待値を特定するIDを返却する.<br/>
     *
     * @return テーブル期待値ID(expectedSearchカラムに定義したデータ)
     */
    public String getExpectedSearchId() {
        return  getValue(testCaseParams, EXPECTED_SEARCH_COLUMN);
    }

    /**
     * HTTPステータスコード(期待値)を返却する.<br/>
     *
     * @return HTTPステータスコード
     */
    public String getExpectedStatusCode() {
        return getValue(testCaseParams, EXPECTED_HTTP_STATUS_COLUMN);
    }

    /**
     * テーブル検証の期待値を特定するIDを返却する.<br/>
     *
     * @return テーブル期待値ID(expectedTableカラムに定義したデータ)
     */
    public String getExpectedTable() {
        return testCaseParams.get(EXPECTED_TABLE_COLUMN);
    }

    /**
     * フォワード先URIの期待値を返却する。
     *
     * @return フォワード先URIの期待値
     */
    public String getExpectedForwardUri() {
        return getValue(testCaseParams, EXPECTED_FORWARD_URI);
    }

    /**
     * コンテンツレングス・ヘッダの期待値を返却する。
     * 
     * @return コンテンツレングス・ヘッダの期待値
     */
    public String getExpectedContentLength() {
        return testCaseParams.get(EXPECTED_CONTENT_LENGTH);
    }

    /**
     * コンテンツタイプ・ヘッダの期待値の期待値を返却する。
     * 
     * @return コンテンツタイプ・ヘッダの期待値の期待値
     */
    public String getExpectedContentType() {
        return testCaseParams.get(EXPECTED_CONTENT_TYPE);
    }

    /**
     * コンテンツディスポジション・ヘッダに指定されたファイル名の期待値を返却する。
     * 
     * @return コンテンツディスポジション・ヘッダに指定されたファイル名の期待値
     */
    public String getExpectedContentFileName() {
        return testCaseParams.get(EXPECTED_CONTENT_FILENAME);
    }

    /**
     * 要求電文（期待値）を返却する.<br/>
     *
     * @return 要求電文（期待値）
     */
    public String getExpectedMessage() {
        return getValue(testCaseParams, EXPECTED_MESSAGE_COLUMN);
    }

    /**
     * 応答電文を返却する.<br/>
     *
     * @return 応答電文
     */
    public String getResponseMessage() {
        return getValue(testCaseParams, RESPONSE_MESSAGE_COLUMN);
    }
    
    /**
     * テスト対象とするリクエストIDを返却する.<br/>
     *
     * @return リクエストID
     */
    public String getRequestId() {
        String requestId = getValue(context.get(0), REQUEST_ID);
        if (StringUtil.isNullOrEmpty(requestId)) {
            throw new IllegalArgumentException(Builder.concat(
                    REQUEST_ID, " value in context LIST_MAP is null or empty.",
                                "case no = [", getTestCaseNo(), "]"));
        }
        return requestId;
    }

    /**
     * テストリクエストのHTTPメソッドを返却する.<br/>
     * 任意指定項目であり、指定されていない場合はPOSTを返却する。
     * @return HTTPメソッド
     */
    public String getHttpMethod() {
        String httpMethod = context.get(0).get(HTTP_METHOD_COLUMN_NAME);
        return StringUtil.hasValue(httpMethod) ? httpMethod : "POST";
    }

    /**
     * Cookieを返却する。
     * @return Cookie情報
     */
    public Map<String, String> getCookie() {
        if (cookie == null || cookie.isEmpty()) {
            return null;
        }
        return cookie.get(0);
    }

    /**
     * クエリパラメータを返却する。
     * @return クエリパラメータ情報
     */
    public Map<String, String> getQueryParams() {
        if (null == queryParams || queryParams.isEmpty()) {
            return null;
        }
        return queryParams.get(0);
    }

    /**
     * データシートに定義されたリクエストパラメータを取得する
     * <pre>
     * デフォルトでは、LIST_MAP「testCases」の「requestParams」カラムで指定されている
     * LIST_MAPのレコードを取得する
     * </pre>
     *
     * @return リクエストパラメータ
     */
    public Map<String, String> getRequestParameters() {

        int caseNo = Integer.valueOf(getTestCaseNo());
        if (request.size() < caseNo) {
            throw new IllegalArgumentException(Builder.concat(
                    "Request parameter is not defined or request parameter list size is invalid.",
                    "case no = [" + caseNo + "]"));
        }

        return request.get(caseNo - 1);
    }

    /**
     * リクエストパラメータを設定するかどうかを返却する
     *
     * @return boolean
     */
    public boolean isRequestParametersSet() {
        return !request.isEmpty();
    }

    /**
     * テーブルセットアップデータ定義を特定するためのIDを返却する.<br/>
     * <pre>
     * SETUP_TABLE[xxxx]=tableName  : 左記例のxxxの部分
     * </pre>
     *
     * @return セットアップデータID(setUpTableカラムに定義したデータ)
     */
    public String getSetUpTableGroupId() {

        if (testCaseParams.containsKey(SET_UP_TABLE)) {
            return getValue(testCaseParams, SET_UP_TABLE);
        }
        return "";
    }

    /**
     * データシート名を返却する<br/>
     *
     * @return データシート名
     */
    public String getSheetName() {
        return sheetName;
    }

    /**
     * テストケース名称を取得する.<br/>
     * テスト失敗時のメッセージやHTMLファイル出力名に使用される。 デフォルトは、{データシート名}_{caseIndex}_{Case名}<br/>
     * 例）RGBN00000000_Case0_初期表示<br/>
     *
     * @return テストケース名称
     */
    public String getTestCaseName() {

        String description = testCaseParams.get(DESCRIPTION);
        if (StringUtil.isNullOrEmpty(description)) {
            // 下位互換性維持のため、caseでも取得する。
            description = testCaseParams.get(CASE);
        }
        if (StringUtil.isNullOrEmpty(description)) {
            throw new IllegalStateException("column '" + DESCRIPTION + "' is not defined.");
        }
        return Builder.concat(
                sheetName, "_Shot",
                getValue(testCaseParams, TEST_CASE_NO), "_",
                description);
    }

    /**
     * テストケース番号を取得する。
     * @return テストケース番号
     */
    public String getTestCaseNo() {
        String no = getValue(testCaseParams, TEST_CASE_NO);
        if (StringUtil.isNullOrEmpty(no)) {
            throw new IllegalArgumentException(Builder.concat(
                    TEST_CASE_NO + " value in testShots LIST_MAP is null or empty",
                    "case no = [" + no + "]"));
        }
        return no;
    }

    /**
     * ユーザIDを取得する。
     * @return ユーザID
     */
    public String getUserId() {
        if (context == null || context.size() != 1) {
            throw new IllegalArgumentException("Context LIST_MAP must be 1 row.");
        }
        return getValue(context.get(0), USER_ID_COLUMN_NAME);
    }

    /**
     * LIST_MAPから取得したレコードから、指定したカラム名に対応する値を取得する<br/>
     * @param row 行レコード(LIST_MAPの各要素）
     * @param columnName カラム名
     * @return 指定したカラム名に対応する値
     */
    protected final String getValue(Map<String, String> row, String columnName) {
        if (!row.containsKey(columnName)) {
            throw new IllegalArgumentException("column '" + columnName + "' is not defined.");
        }
        return row.get(columnName);
    }

    /**
     * 検索結果の検証をするかどうかを返却する.<br/>
     *
     * @return boolean
     */
    public boolean isAssertSearch() {
        return StringUtil.hasValue(getExpectedSearchId());
    }

    /**
     * テーブル値を検証するかどうか返却する.<br/>
     *
     * @return boolean
     */
    public boolean isAssertTable() {
        return StringUtil.hasValue(getExpectedTable());
    }

    /**
     * テーブルをセットアップするかどうかを返却する.<br/>
     *
     * @return boolean
     */
    public boolean isSetUpTable() {
        return StringUtil.hasValue(getSetUpTableGroupId());
    }

    /**
     * トークン制御を行うかどうかを返却する.<br/>
     *
     * @return boolean
     */
    public boolean isValidToken() {
        return Boolean.parseBoolean(getValue(testCaseParams, IS_VALID_TOKEN));
    }

    /**
     * リクエストスコープ値を比較検証するかどうかを返却する.<br>
     *
     * @return boolean
     */
    public boolean isAssertRequestScopeVar() {
        return isAssertRequestScopeVar && !expectedResponseParams.isEmpty();
    }

    /**
     * リクエストスコープ値を比較検証するかどうかを設定する
     * <pre>
     * 各テストケースにて比較検証を個別実装する場合にfalseに設定する。
     * {@link AbstractHttpRequestTestTemplate#beforeExecuteRequest(TestCaseInfo, nablarch.fw.ExecutionContext, Advice)}にて
     * にてfalseへの設定を行うこと。
     * </pre>
     * @param isAssert boolean(デフォルト値はTrue）
     */
    public void setIsAssertRequestScopeVar(boolean isAssert) {
        this.isAssertRequestScopeVar = isAssert;
    }

    /**
     * メッセージIDの検証を行うかどうかを返却する。
     * @return boolean
     */
    public boolean isAssertApplicationMessageId() {
        return isAssertApplicationMessageId;
    }

    /**
     * メッセージIDの検証を行うかどうかを設定する。
     * @param isAssert boolean(デフォルト値はTrue）
     */
    public void setIsAssertApplicationMessageId(boolean isAssert) {
       this.isAssertApplicationMessageId = isAssert;
    }

    /** このテストケースのHTTPリクエスト  */
    private HttpRequest requestOfThisCase = null;

    /**
     * HTTPリクエストを取得する。
     *
     * @return このテストケースのHTTPリクエスト
     */
    public HttpRequest getHttpRequest() {
        return requestOfThisCase;
    }

    /**
     * HTTPリクエストを設定する。
     *
     * @param requestOfThisTestCase このテストケースのHTTPリクエスト
     */
    void setHttpRequest(HttpRequest requestOfThisTestCase) {
        this.requestOfThisCase = requestOfThisTestCase;
    }
}
