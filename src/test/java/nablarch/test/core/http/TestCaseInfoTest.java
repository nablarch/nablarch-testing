package nablarch.test.core.http;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestCaseInfoTest {

    @Test
    public void クエリパラメータを指定しないコンストラクタで適切にインスタンスを生成できること() {

        TestCaseInfo sut = new TestCaseInfo("testSheet",
            createTestCaseParams(),
            createContext(),
            createRequestParams(),
            createExpectedResponse(),
            createCookie());

        assertEquals("testSheet", sut.getSheetName());
        assertEquals("200", sut.getExpectedStatusCode());
        assertEquals("TESTUSER", sut.getUserId());
        assertEquals("testValue1", sut.getRequestParameters().get("testParam1"));
        assertEquals("foobar", sut.getExpectedRequestScopeVar().get("requestScopedVar"));
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

        assertEquals("testSheet", sut.getSheetName());
        assertEquals("200", sut.getExpectedStatusCode());
        assertEquals("TESTUSER", sut.getUserId());
        assertEquals("testValue1", sut.getRequestParameters().get("testParam1"));
        assertEquals("foobar", sut.getExpectedRequestScopeVar().get("requestScopedVar"));
        assertNull(sut.getCookie());
        assertNull(sut.getQueryParams());

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

}
