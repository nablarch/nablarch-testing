package nablarch.test.core.http;

import nablarch.fw.web.HttpRequest;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

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
}
