package nablarch.test.core.http;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SuppressWarnings("NonAsciiCharacters")
public class TestCaseInfoTest {

    @Test
    public void クエリパラメータを指定しないコンストラクタで適切にインスタンスを生成できること() {

        TestCaseInfo sut = new TestCaseInfo("testSheet",
            createTestCaseParams(),
            createContext(),
            createRequestParams(),
            createExpectedResponse(),
            createCookie());

        assertRequiredParams(sut);
        assertEquals("testCookieValue1", sut.getCookie().get("testCookieName1"));
        assertNull(sut.getQueryParams());

    }

    @Test
    public void クッキー及びクエリパラメータを指定しないコンストラクタで適切にインスタンスを生成できること() {

        TestCaseInfo sut = new TestCaseInfo("testSheet",
            createTestCaseParams(),
            createContext(),
            createRequestParams(),
            createExpectedResponse());

        assertRequiredParams(sut);
        assertNull(sut.getCookie());
        assertNull(sut.getQueryParams());

    }

    private void assertRequiredParams(TestCaseInfo sut) {
        assertEquals("testSheet", sut.getSheetName());
        assertEquals("200", sut.getExpectedStatusCode());
        assertEquals("TESTUSER", sut.getUserId());
        assertEquals("testValue1", sut.getRequestParameters().get("testParam1"));
        assertEquals("foobar", sut.getExpectedRequestScopeVar().get("requestScopedVar"));
    }

    private Map<String, String> createTestCaseParams() {
        Map<String, String> testCaseParams = new HashMap<String, String>();
        testCaseParams.put("no", "1");
        testCaseParams.put("description", "test01");
        testCaseParams.put("expectedStatusCode", "200");
        return testCaseParams;
    }

    private List<Map<String, String>> createSimpleListMap(String key, String value) {
        Map<String, String> contextElement = new HashMap<String, String>();
        contextElement.put(key, value);

        List<Map<String, String>> listMap = new ArrayList<Map<String, String>>();
        listMap.add(contextElement);

        return listMap;

    }

    private List<Map<String, String>> createContext() {
        return createSimpleListMap("USER_ID", "TESTUSER");
    }

    private List<Map<String, String>> createRequestParams() {
        return createSimpleListMap("testParam1", "testValue1");

    }

    private List<Map<String,String>> createExpectedResponse() {
        return createSimpleListMap("requestScopedVar", "foobar");
    }

    private List<Map<String,String>> createCookie() {
        return createSimpleListMap("testCookieName1", "testCookieValue1");
    }

    /**
     * TS-20: context LIST_MAP の REQUEST_ID が null の場合に IllegalArgumentException がスローされること
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetRequestId_throwsWhenRequestIdIsNull() {
        // Given: REQUEST_ID が null のコンテキスト
        List<Map<String, String>> context = createSimpleListMap("REQUEST_ID", null);
        TestCaseInfo sut = new TestCaseInfo("testSheet",
            createTestCaseParams(),
            context,
            createRequestParams(),
            createExpectedResponse());
        // When / Then: getRequestId() で IllegalArgumentException がスローされる
        sut.getRequestId();
    }

    /**
     * TS-20: context LIST_MAP の REQUEST_ID が空文字の場合に IllegalArgumentException がスローされること
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetRequestId_throwsWhenRequestIdIsEmpty() {
        // Given: REQUEST_ID が空文字のコンテキスト
        List<Map<String, String>> context = createSimpleListMap("REQUEST_ID", "");
        TestCaseInfo sut = new TestCaseInfo("testSheet",
            createTestCaseParams(),
            context,
            createRequestParams(),
            createExpectedResponse());
        // When / Then: getRequestId() で IllegalArgumentException がスローされる
        sut.getRequestId();
    }

    /**
     * TS-21: context LIST_MAP が1行でない（2行以上）場合に IllegalArgumentException がスローされること
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetUserId_throwsWhenContextHasMultipleRows() {
        // Given: context が2行のリスト
        List<Map<String, String>> context = new ArrayList<Map<String, String>>();
        Map<String, String> row1 = new HashMap<String, String>();
        row1.put("USER_ID", "user1");
        Map<String, String> row2 = new HashMap<String, String>();
        row2.put("USER_ID", "user2");
        context.add(row1);
        context.add(row2);
        TestCaseInfo sut = new TestCaseInfo("testSheet",
            createTestCaseParams(),
            context,
            createRequestParams(),
            createExpectedResponse());
        // When / Then: getUserId() で IllegalArgumentException がスローされる
        sut.getUserId();
    }

    /**
     * TS-23: testShots の no カラムが空の場合に IllegalArgumentException がスローされること
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetTestCaseNo_throwsWhenNoIsEmpty() {
        // Given: no カラムが空文字のテストケースパラメータ
        Map<String, String> params = new HashMap<String, String>();
        params.put("no", "");
        params.put("description", "test");
        params.put("expectedStatusCode", "200");
        TestCaseInfo sut = new TestCaseInfo("testSheet",
            params,
            createContext(),
            createRequestParams(),
            createExpectedResponse());
        // When / Then: getTestCaseNo() で IllegalArgumentException がスローされる
        sut.getTestCaseNo();
    }

    /**
     * TS-24: description カラムも case カラムも未定義の場合に IllegalStateException がスローされること
     */
    @Test(expected = IllegalStateException.class)
    public void testGetTestCaseName_throwsWhenNeitherDescriptionNorCaseDefined() {
        // Given: description も case も含まないテストケースパラメータ
        Map<String, String> params = new HashMap<String, String>();
        params.put("no", "1");
        params.put("expectedStatusCode", "200");
        TestCaseInfo sut = new TestCaseInfo("testSheet",
            params,
            createContext(),
            createRequestParams(),
            createExpectedResponse());
        // When / Then: getTestCaseName() で IllegalStateException がスローされる
        sut.getTestCaseName();
    }

}
