package nablarch.test.core.http;

import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.MockHttpRequest;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

@SuppressWarnings("NonAsciiCharacters")
public class HttpRequestTestSupportTest {

    HttpRequestTestSupport sut = new HttpRequestTestSupport();

    @Test
    public void HTTP_METHODにデフォルト値が設定されたHttpRequestを作成できること() {

        String requestUri = "http://localhost:8080/index";
        Map<String, String[]> requestParams = new HashMap<String, String[]>();

        HttpRequest result = sut.createHttpRequest(requestUri, requestParams);

        assertEquals(requestUri, result.getRequestUri());
        assertEquals("POST", result.getMethod());
    }


    @Test
    public void クッキー及びクエリパラメータそれぞれについて_nullの場合にHttpRequestに値が設定されないこと() {

        String requestUri = "http://localhost:8080/index";
        Map<String, String> requestParams = new HashMap<String, String>();

        HttpRequest result = sut.createHttpRequestWithConversion(requestUri, "GET", requestParams, null, null);

        assertEquals(requestUri, result.getRequestUri());
        assertEquals(0, result.getCookie().size());
    }

    @Test
    public void クッキー及びクエリパラメータそれぞれについて_空の場合にHttpRequestに値が設定されないこと() {

        String requestUri = "http://localhost:8080/index";
        Map<String, String> requestParams = new HashMap<String, String>();

        Map<String, String> cookie = new HashMap<String, String>();
        Map<String, String> queryParams = new HashMap<String, String>();

        HttpRequest result = sut.createHttpRequestWithConversion(requestUri, "GET", requestParams, cookie, queryParams);

        assertEquals(result.getRequestUri(), requestUri);
        assertEquals(0, result.getCookie().size());
    }

    @Test
    public void HTTP_METHODにデフォルト値が設定されクエリパラメータが空のHttpRequestを作成できること() {

        String requestUri = "http://localhost:8080/index";
        Map<String, String> requestParams = new HashMap<String, String>();
        Map<String, String> cookie = new HashMap<String, String>();

        HttpRequest result = sut.createHttpRequestWithConversion(requestUri, requestParams, cookie);

        assertEquals(requestUri, result.getRequestUri());
        assertEquals("POST", result.getMethod());
    }

    @Test
    public void クエリパラメータが空のHttpRequestを作成できること() {

        String requestUri = "http://localhost:8080/index";
        Map<String, String> requestParams = new HashMap<String, String>();
        Map<String, String> cookie = new HashMap<String, String>();

        HttpRequest result = sut.createHttpRequestWithConversion(requestUri, "GET", requestParams, cookie);

        assertEquals(requestUri, result.getRequestUri());
        assertEquals("GET", result.getMethod());
    }

    @Test
    public void getParamでHttpRequestのリクエストパラメータが取得できること() {
        HttpRequest req = new MockHttpRequest().setParam("paramName", "param1", "param2");
        assertArrayEquals(sut.getParam(req, "paramName"), new String[]{"param1", "param2"});
    }

    @Test
    public void getParamMapでHttpRequestのリクエストパラメータが取得できること() {
        HttpRequest req = new MockHttpRequest()
                .setParam("paramName1", "param1-1", "param1-2")
                .setParam("paramName2", "param2");
        Map<String, String[]> result = sut.getParamMap(req);
        assertEquals(2, result.size());
        assertArrayEquals(result.get("paramName1"), new String[]{"param1-1", "param1-2"});
        assertArrayEquals(result.get("paramName2"), new String[]{"param2"});
    }
}
