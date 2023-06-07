package nablarch.test.core.http;

import org.junit.Assert;
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
        String sheetName = "testSheet";

        Map<String, String> testCaseParams = new HashMap<String, String>();
        testCaseParams.put("no", "1");
        testCaseParams.put("description", "test01");
        testCaseParams.put("expectedStatusCode", "200");

        List<Map<String, String>> context = new ArrayList<Map<String, String>>();
        Map<String, String> contextElement = new HashMap<String, String>();
        contextElement.put("USER_ID", "TESTUSER");
        context.add(contextElement);

        List<Map<String, String>> requestParams = new ArrayList<Map<String, String>>();
        Map<String, String> requestParamElement = new HashMap<String, String>();
        requestParamElement.put("testParam1", "testValue1");
        requestParams.add(requestParamElement);

        List<Map<String, String>> expectedResponseListMap = new ArrayList<Map<String, String>>();
        Map<String, String> expectedResponseElement = new HashMap<String, String>();
        expectedResponseElement.put("requestScopedVar", "foobar");
        expectedResponseListMap.add(expectedResponseElement);

        List<Map<String, String>> cookie = new ArrayList<Map<String, String>>();
        Map<String, String> cookieElement = new HashMap<String, String>();
        cookieElement.put("testCookieName1", "testCookieValue1");
        cookie.add(cookieElement);

        TestCaseInfo sut = new TestCaseInfo(sheetName, testCaseParams, context, requestParams, expectedResponseListMap, cookie);

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
        String sheetName = "testSheet";

        Map<String, String> testCaseParams = new HashMap<String, String>();
        testCaseParams.put("no", "1");
        testCaseParams.put("description", "test01");
        testCaseParams.put("expectedStatusCode", "200");

        List<Map<String, String>> context = new ArrayList<Map<String, String>>();
        Map<String, String> contextElement = new HashMap<String, String>();
        contextElement.put("USER_ID", "TESTUSER");
        context.add(contextElement);

        List<Map<String, String>> requestParams = new ArrayList<Map<String, String>>();
        Map<String, String> requestParamElement = new HashMap<String, String>();
        requestParamElement.put("testParam1", "testValue1");
        requestParams.add(requestParamElement);

        List<Map<String, String>> expectedResponseListMap = new ArrayList<Map<String, String>>();
        Map<String, String> expectedResponseElement = new HashMap<String, String>();
        expectedResponseElement.put("requestScopedVar", "foobar");
        expectedResponseListMap.add(expectedResponseElement);

        TestCaseInfo sut = new TestCaseInfo(sheetName, testCaseParams, context, requestParams, expectedResponseListMap);

        assertEquals("testSheet", sut.getSheetName());
        assertEquals("200", sut.getExpectedStatusCode());
        assertEquals("TESTUSER", sut.getUserId());
        assertEquals("testValue1", sut.getRequestParameters().get("testParam1"));
        assertEquals("foobar", sut.getExpectedRequestScopeVar().get("requestScopedVar"));
        assertNull(sut.getCookie());
        assertNull(sut.getQueryParams());

    }

}
