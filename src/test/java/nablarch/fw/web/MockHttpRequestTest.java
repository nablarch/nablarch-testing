package nablarch.fw.web;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.upload.PartInfo;
import nablarch.fw.web.useragent.UserAgent;
import nablarch.fw.web.useragent.UserAgentParser;
import nablarch.test.core.http.MockHttpServer;
import nablarch.test.support.tool.Hereis;

import org.junit.Assert;
import org.junit.Test;

import mockit.Expectations;

/**
 * {@link MockHttpRequest}のテストクラス。
 */
public class MockHttpRequestTest {

    @Test
    public void testDefaultConstructorAndAccessorsWorkProperly() {
        MockHttpRequest req = new MockHttpRequest();
        req.setMethod("GET");
        req.setRequestUri("/index.html");
        req.setHost("www.example.com");

        Assert.assertEquals("GET", req.getMethod());
        Assert.assertEquals("/index.html", req.getRequestUri());
        Assert.assertEquals("www.example.com", req.getHost());
        Assert.assertEquals("HTTP/1.1", req.getHttpVersion());

        String message = req.toString();
        Assert.assertTrue(message.contains("GET /index.html HTTP/1.1\r\n"));
        Assert.assertTrue(message.contains("Host: www.example.com\r\n"));
        Assert.assertTrue(message.contains("Content-Length: 0\r\n"));
    }

    @Test
    public void testAccessorsToHttpVersion() {
        MockHttpRequest req = new MockHttpRequest();
        Assert.assertEquals("HTTP/1.1", req.getHttpVersion());
        req.setHttpVersion("HTTP/0.9");
        Assert.assertEquals("HTTP/0.9", req.getHttpVersion());

        try {
            req.setHttpVersion("HTTP/0.1");
            Assert.fail();
        } catch (Throwable e) {
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
        }
    }

    @Test
    public void testAccessorsToRequestParameter() {
        MockHttpRequest req = new MockHttpRequest();
        Assert.assertTrue(req.getParamMap()
                .isEmpty());

        Map<String, String[]> params = new HashMap<String, String[]>();
        params.put("key1", new String[] {"val1"});
        params.put("key2", new String[] {"val2"});
        req.setParamMap(params);

        Assert.assertEquals(2, req.getParamMap()
                .size());
        Assert.assertEquals("val1", req.getParam("key1")[0]);
        Assert.assertEquals("val2", req.getParam("key2")[0]);

        req.setParam("key1", "value1");
        assertThat(req.getParam("key1").length, is(1));
        assertThat(req.getParam("key1")[0], is("value1"));

        req.setParam("key3", "v1", "v2");
        assertThat(req.getParam("key3").length, is(2));
        assertThat(req.getParam("key3")[0], is("v1"));
        assertThat(req.getParam("key3")[1], is("v2"));
    }


    @Test
    public void testAccessorsToHttpHeaders() {
        MockHttpRequest req = new MockHttpRequest();
        Assert.assertTrue(req.getHeaderMap()
                .isEmpty());

        req.setHost("www.example.com");
        Assert.assertEquals(1, req.getHeaderMap()
                .size());
        Assert.assertEquals("www.example.com", req.getHeader("Host"));
        Assert.assertEquals("www.example.com", req.getHost());

        req.setHeaderMap(new HashMap<String, String>());
        Assert.assertTrue(req.getHeaderMap()
                .isEmpty());
    }

    /**
     * {@link MockHttpRequest#setCookie(HttpCookie)}と{@link MockHttpRequest#getCookie()}のテスト。
     */
    @Test
    public void testCookie() throws Exception {
        final MockHttpCookie cookie = new MockHttpCookie();
        cookie.put("k1", "v1");

        final MockHttpRequest sut1 = new MockHttpRequest();
        sut1.setCookie(cookie);

        final String requestString = sut1.toString();
        assertThat("Cookieヘッダーが出力されること", requestString, is(containsString("Cookie: k1=v1\r\n")));

        final MockHttpRequest sut2 = new MockHttpRequest(requestString);
        final HttpCookie cookie2 = sut2.getCookie();
        assertThat(cookie2.size(), is(1));
        assertThat(cookie2.get("k1"), is("v1"));
    }

    /**
     * {@link MockHttpRequest#setMultipart(Map)}、{@link MockHttpRequest#getMultipart()}、{@link MockHttpRequest#getPart(String)}のテスト。
     *
     */
    @Test
    public void testMultiPart() throws Exception {
        final MockHttpRequest sut = new MockHttpRequest();
        sut.setMultipart(new HashMap<String, List<PartInfo>>() {{
                             put("file1", new ArrayList<PartInfo>() {{
                                 add(PartInfo.newInstance("file1"));
                             }});
                         }}
        );

        final Map<String, List<PartInfo>> multipart = sut.getMultipart();
        assertThat("サイズは1", multipart.size(), is(1));
        assertThat("file1が存在していること", multipart.containsKey("file1"), is(true));

        final List<PartInfo> file1 = sut.getPart("file1");
        assertThat("file1の情報がとれること", file1.size(), is(1));

        assertThat("存在しないファイル情報は取れないこと", sut.getPart("file2").size(), is(0));

        // nullも問題ない
        sut.setMultipart(null);
        assertThat("nullが設定されている場合、からが取得できる", sut.getMultipart().size(), is(0));
        assertThat("getPartも例外は出ない", sut.getPart("file").size(), is(0));
    }

    /**
     * パース処理で{@link UnsupportedEncodingException}が発生した場合、
     * {@link IllegalStateException}が送出されること。
     *
     */
    @Test(expected = IllegalStateException.class)
    public void testParseUrlError() throws Exception {
        StringBuilder request = new StringBuilder();
        request.append("POST /index.html?key=value HTTP/1.1\r\n");
        request.append("Host: www.example.com\r\n");
        request.append("Content-Type: application/x-www-form-urlencoded\r\n");
        request.append("\r\n");
        request.append("userid=joe&password=verysecure\r\n");

        new Expectations(URLDecoder.class) {{
            URLDecoder.decode(anyString, "UTF-8");
            result = new UnsupportedEncodingException("utf-8 error");
        }};
        new MockHttpRequest(request.toString());
    }

    @Test
    public void testShouldParseHttpRequestMessage() {
        StringBuilder request = new StringBuilder();
        request.append("POST /index.html HTTP/1.1\r\n");
        request.append("Host: www.example.com\r\n");
        request.append("Content-Type: application/x-www-form-urlencoded\r\n");
        request.append("\r\n");
        request.append("userid=joe&password=verysecure\r\n");
        
        HttpRequest req = new MockHttpRequest(request.toString());
        Assert.assertEquals("POST", req.getMethod());
        Assert.assertEquals("/index.html", req.getRequestUri());
        Assert.assertEquals("www.example.com", req.getHost());
        Assert.assertEquals("HTTP/1.1", req.getHttpVersion());

        Assert.assertEquals(2, req.getParamMap()
                .size());
        Assert.assertEquals("joe", req.getParamMap()
                .get("userid")[0]);
        Assert.assertEquals(1, req.getParamMap()
                .get("userid").length);
        Assert.assertEquals("verysecure", req.getParam("password")[0]);
    }


    @Test
    public void testPasingQueryParameters() {
        StringBuilder request = new StringBuilder();
        request.append("GET /index.html?userid=joe&password=verysecure HTTP/1.1\r\n");
        request.append("Host: www.example.com\r\n");
        request.append("Content-Type: application/x-www-form-urlencoded\r\n");
        HttpRequest req = new MockHttpRequest(request.toString());

        Assert.assertEquals("GET", req.getMethod());
        Assert.assertEquals(
                "/index.html?userid=joe&password=verysecure"
                , req.getRequestUri()
        );
        Assert.assertEquals("/index.html", req.getRequestPath());
        Assert.assertEquals("www.example.com", req.getHost());
        Assert.assertEquals("HTTP/1.1", req.getHttpVersion());

        Assert.assertEquals(2, req.getParamMap()
                .size());
        Assert.assertEquals("joe", req.getParamMap()
                .get("userid")[0]);
        Assert.assertEquals(1, req.getParamMap()
                .get("userid").length);
        Assert.assertEquals("verysecure", req.getParam("password")[0]);

        request = new StringBuilder();
        request.append(
                "GET /index.html?userid=joe&password=verysecure&options=1&options=6;jsessionid=igrbj79h06lkg5pmetlz19it HTTP/1.1\r\n");
        request.append("Host: www.example.com\r\n");
        request.append("Content-Type: application/x-www-form-urlencoded");
        
        req = new MockHttpRequest(request.toString());
        
        Assert.assertEquals("GET", req.getMethod());
        Assert.assertEquals(
                "/index.html?userid=joe&password=verysecure&options=1&options=6;jsessionid=igrbj79h06lkg5pmetlz19it"
                , req.getRequestUri()
        );
        Assert.assertEquals("/index.html", req.getRequestPath());
        Assert.assertEquals("www.example.com", req.getHost());
        Assert.assertEquals("HTTP/1.1", req.getHttpVersion());

        Assert.assertEquals(4, req.getParamMap()
                .size());
        Assert.assertEquals("joe", req.getParamMap()
                .get("userid")[0]);
        Assert.assertEquals(1, req.getParamMap()
                .get("userid").length);
        Assert.assertEquals(2, req.getParamMap()
                .get("options").length);
        Assert.assertEquals("1", req.getParamMap()
                .get("options")[0]);
        Assert.assertEquals("6", req.getParamMap()
                .get("options")[1]);
        Assert.assertEquals(
                "igrbj79h06lkg5pmetlz19it"
                , req.getParamMap()
                        .get("jsessionid")[0]
        );

        request = new StringBuilder();
        request.append("GET /index.html;jsessionid=igrbj79h06lkg5pmetlz19it HTTP/1.1\r\n");
        request.append("Host: www.example.com");
        request.append("Content-Type: application/x-www-form-urlencoded");
        req = new MockHttpRequest(request.toString());
        Assert.assertEquals(1, req.getParamMap()
                .size());
    }

    @Test
    public void testParsingPostParametersWithDuplicatedKey() {
        final StringBuilder request = new StringBuilder();
        request.append("POST /index.html HTTP/1.1\r\n");
        request.append("Host: www.example.com\r\n");
        request.append("Content-Type: application/x-www-form-urlencoded\r\n");
        request.append("\r\n");
        request.append("fruits=APPLE&fruits=BANANA&fruits=ORANGE\r\n");
                
        HttpRequest req = new MockHttpRequest(request.toString());
        
        Assert.assertEquals("POST", req.getMethod());

        Assert.assertEquals(1, req.getParamMap()
                .size());
        Assert.assertEquals(3, req.getParam("fruits").length);
        Assert.assertEquals("APPLE", req.getParam("fruits")[0]);
        Assert.assertEquals("BANANA", req.getParam("fruits")[1]);
        Assert.assertEquals("ORANGE", req.getParam("fruits")[2]);
    }

    @Test
    public void testToString() {
        
        StringBuilder request = new StringBuilder();
        request.append("POST /index.html HTTP/1.1\r\n");
        request.append("Host: www.example.com\r\n");
        request.append("Content-Length: 42\r\n");
        request.append("Content-Type: application/x-www-form-urlencoded\r\n");
        request.append("Cookie: cookie-1=value-1; cookie-2=value-2\r\n");
        request.append("\r\n");
        request.append("fruits=APPLE&fruits=BANANA&fruits=ORANGE\r\n");

        HttpRequest req = new MockHttpRequest(request.toString());

        String message = req.toString();
        Assert.assertTrue(message.contains("POST /index.html HTTP/1.1\r\n"));
        Assert.assertTrue(message.contains("Host: www.example.com\r\n"));
        Assert.assertTrue(message.contains("Content-Length: 42\r\n"));
        Assert.assertTrue(message.contains("Content-Type: application/x-www-form-urlencoded\r\n"));
        Assert.assertTrue(message.contains("Cookie: cookie-1=value-1; cookie-2=value-2\r\n"));
        Assert.assertTrue(message.contains("fruits=APPLE&fruits=BANANA&fruits=ORANGE"));
    }

    /**
     * {@link MockHttpRequest#toString()}で{@link UnsupportedEncodingException}が発生した場合、
     * {@link IllegalStateException}が送出されること。
     */
    @Test(expected = IllegalStateException.class)
    public void testToStringEncodeError() throws Exception {
        StringBuilder request = new StringBuilder();
        request.append("POST /index.html HTTP/1.1\r\n");
        request.append("Host: www.example.com\r\n");
        request.append("Content-Length: 42\r\n");
        request.append("Content-Type: application/x-www-form-urlencoded\r\n");
        request.append("Cookie: cookie-1=value-1; cookie-2=value-2\r\n");
        request.append("\r\n");
        request.append("fruits=APPLE&fruits=BANANA&fruits=ORANGE\r\n");

        new Expectations(URLEncoder.class) {{
            URLEncoder.encode(anyString, "UTF-8");
            result = new UnsupportedEncodingException("error utf-8");
        }};

        HttpRequest req = new MockHttpRequest(request.toString());
        req.toString();
    }

    @Test
    public void testToStringWithRequestParameterAsMap() {
        MockHttpRequest req = new MockHttpRequest();
        req.setMethod("POST");
        req.setRequestUri("/index.html");
        req.setHeaderMap(new HashMap<String, String>() {{
            put("Host", "www.example.com");
            put("Content-Type", "application/x-www-form-urlencoded");
        }})
                .setParamMap(new HashMap<String, String[]>() {{
                    put("fruits", new String[] {"APPLE", "BANANA", "ORANGE"});
                    put("vegetables", new String[] {"CARROT"});
                }});

        String reqMessage = req.toString();
        Assert.assertTrue(reqMessage.contains("vegetables=CARROT"));
        Assert.assertTrue(reqMessage.contains("fruits=APPLE"));
        Assert.assertTrue(reqMessage.contains("fruits=BANANA"));
        Assert.assertTrue(reqMessage.contains("fruits=ORANGE"));
        Assert.assertTrue(reqMessage.contains("&fruits="));
        Assert.assertTrue(reqMessage.contains("&vegetables="));
    }

    @Test
    public void testParseThroughServletContainer() {
        MockHttpRequest req = new MockHttpRequest();
        req.setMethod("POST");
        req.setRequestUri("/index.html");
        req.setHeaderMap(new HashMap<String, String>() {{
            put("Host", "www.example.com");
            put("Content-Type", "application/x-www-form-urlencoded");
        }})
                .setParamMap(new HashMap<String, String[]>() {{
                    put("fruits", new String[] {"APPLE", "BANANA", "ORANGE"});
                    put("vegetables", new String[] {"CARROT"});
                }});

        final List<HttpRequest> holder = new ArrayList<HttpRequest>();

        HttpServer server = new MockHttpServer()
                .addHandler("//*", new HttpRequestHandler() {
                    public HttpResponse handle(HttpRequest request, ExecutionContext ctx) {
                        request.getParamMap(); // リクエストスレッド内でパラメータの取得を行っておく必要がある。
                        holder.add(request);
                        Assert.assertEquals("POST", request.getMethod());
                        return new HttpResponse(201);
                    }
                })
                .startLocal();

        HttpResponse res = server.handle(req, new ExecutionContext());
        Assert.assertEquals(201, res.getStatusCode());
        HttpRequest request = holder.get(0);
        Assert.assertEquals(2, request.getParamMap()
                .size());
        Assert.assertEquals("APPLE", request.getParam("fruits")[0]);
        Assert.assertEquals("BANANA", request.getParam("fruits")[1]);
        Assert.assertEquals("ORANGE", request.getParam("fruits")[2]);
        Assert.assertEquals(3, request.getParam("fruits").length);
        Assert.assertEquals(1, request.getParam("vegetables").length);
        Assert.assertEquals("CARROT", request.getParam("vegetables")[0]);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testUrlDecodingMessageBodyWhenSerializesMessage() {
        String apple = URLEncoder.encode("あっぷる");
        String banana = URLEncoder.encode("ばなな");
        String orange = URLEncoder.encode("おれんじ");

        final StringBuilder request = new StringBuilder();
        request.append("POST /index.html HTTP/1.1\r\n");
        request.append("Host: www.example.com\r\n");
        request.append("Content-Type: application/x-www-form-urlencoded\r\n");
        request.append("\r\n");
        request.append("fruits=")
                .append(apple)
                .append("&fruits=")
                .append(banana)
                .append("&fruits=")
                .append(orange)
                .append("\r\n");

        HttpRequest req = new MockHttpRequest(request.toString());

        final List<HttpRequest> holder = new ArrayList<HttpRequest>();

        HttpServer server = new MockHttpServer()
                .addHandler("//*", new HttpRequestHandler() {
                    public HttpResponse handle(HttpRequest request, ExecutionContext ctx) {
                        request.getParamMap(); // リクエストスレッド内でパラメータの取得を行っておく必要がある。
                        holder.add(request);
                        Assert.assertEquals("POST", request.getMethod());
                        return new HttpResponse(201);
                    }
                })
                .startLocal();

        HttpResponse res = server.handle(req, new ExecutionContext());
        Assert.assertEquals(201, res.getStatusCode());
        HttpRequest serverSideReuqest = holder.get(0);

        Assert.assertEquals(1, serverSideReuqest.getParamMap()
                .size());
        Assert.assertEquals(3, serverSideReuqest.getParam("fruits").length);
        Assert.assertEquals("あっぷる", serverSideReuqest.getParam("fruits")[0]);
        Assert.assertEquals("ばなな", serverSideReuqest.getParam("fruits")[1]);
        Assert.assertEquals("おれんじ", serverSideReuqest.getParam("fruits")[2]);
    }

    @Test
    public void testUrlDecodingMessageBodyWhenSerializesMessage2() {
        MockHttpRequest req = new MockHttpRequest();
        req.setMethod("POST");
        req.setRequestUri("/index.html");
        req.setHeaderMap(new HashMap<String, String>() {{
                    put("Host", "www.example.com");
                    put("Content-Type", "application/x-www-form-urlencoded");
                }})
                .setParamMap(new HashMap<String, String[]>() {{
                    put("fruits", new String[] {"あっぷる", "ばなな", "おれんじ"});
                }});

        final List<HttpRequest> holder = new ArrayList<HttpRequest>();

        HttpServer server = new MockHttpServer()
                .addHandler("//*", new HttpRequestHandler() {
                    public HttpResponse handle(HttpRequest request, ExecutionContext ctx) {
                        request.getParamMap(); // リクエストスレッド内でパラメータの取得を行っておく必要がある。
                        holder.add(request);
                        Assert.assertEquals("POST", request.getMethod());
                        return new HttpResponse(201);
                    }
                })
                .startLocal();

        HttpResponse res = server.handle(req, new ExecutionContext());
        Assert.assertEquals(201, res.getStatusCode());
        HttpRequest request = holder.get(0);

        Assert.assertEquals(1, request.getParamMap()
                .size());
        Assert.assertEquals(3, request.getParam("fruits").length);
        Assert.assertEquals("あっぷる", request.getParam("fruits")[0]);
        Assert.assertEquals("ばなな", request.getParam("fruits")[1]);
        Assert.assertEquals("おれんじ", request.getParam("fruits")[2]);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testParsingUrlEncodedCharactersInMessageBody() {
        String apple = URLEncoder.encode("あっぷる");
        String banana = URLEncoder.encode("ばなな");
        String orange = URLEncoder.encode("おれんじ");

        StringBuilder request = new StringBuilder();
        request.append("POST /index.html HTTP/1.1\r\n");
        request.append("Host: www.example.com\r\n");
        request.append("Content-Type: application/x-www-form-urlencoded\r\n");
        request.append("\r\n");
        request.append("fruits=")
                .append(apple)
                .append("&fruits=")
                .append(banana)
                .append("&fruits=")
                .append(orange)
                .append("\r\n");


        HttpRequest req = new MockHttpRequest(request.toString());

        Assert.assertEquals("POST", req.getMethod());

        Assert.assertEquals(1, req.getParamMap()
                .size());
        Assert.assertEquals(3, req.getParam("fruits").length);
        Assert.assertEquals("あっぷる", req.getParam("fruits")[0]);
        Assert.assertEquals("ばなな", req.getParam("fruits")[1]);
        Assert.assertEquals("おれんじ", req.getParam("fruits")[2]);
    }

    @Test
    public void testParsingRequestHasMultilineHeader() {
        final StringBuilder request = new StringBuilder();
        request.append("POST /index.html HTTP/1.1\r\n");
        request.append("Host: www.example.com\r\n");
        request.append("To: person1@domain1.org, person2@domain2.com,");
        request.append("person3@domain3.net, person4@domain4.edu\r\n");
        request.append("Content-Type: application/x-www-form-urlencoded\r\n");
        request.append("\r\n");
        request.append("fruits=APPLE&fruits=BANANA&fruits=ORANGE\r\n");
        
        HttpRequest req = new MockHttpRequest(request.toString());

        Assert.assertEquals(3, req.getHeaderMap()
                .size());
        String[] customHeader = req.getHeader("To")
                .split("\\s*,\\s*");
        Assert.assertEquals(4, customHeader.length);
        Assert.assertEquals("person1@domain1.org", customHeader[0]);
        Assert.assertEquals("person4@domain4.edu", customHeader[3]);
    }

    @Test
    public void testThrowingParseErrorWhenMalformedHeaderIsPassed() {
        try {
            new MockHttpRequest(Hereis.string());
            /***********************************************
             POST /index.html HTTP/1.1
             Host www.example.com
             Content-Type: application/x-www-form-urlencoded

             fruits=APPLE&fruits=BANANA&fruits=ORANGE
             ************************************************/
            Assert.fail();
        } catch (RuntimeException e) {
            Assert.assertTrue(true);
        } catch (Throwable e) {
            Assert.fail();
        }
    }

    @Test
    public void testThrowingParseErrorWhenIllegalMethodWasRequested() {
        try {
            new MockHttpRequest(Hereis.string());
            /***********************************************
             UNKNOWN /index.html HTTP/1.1
             Host: www.example.com
             Content-Type: application/x-www-form-urlencoded

             fruits=APPLE&fruits=BANANA&fruits=ORANGE
             ************************************************/
            Assert.fail();
        } catch (RuntimeException e) {
            Assert.assertTrue(true);
        } catch (Throwable e) {
            Assert.fail();
        }
    }

    @Test
    public void testThrowingParseErrorWithMalformedRequestLine() {
        try {
            String nullbyte = "\u0000";
            new MockHttpRequest(Hereis.string(nullbyte));
            /***********************************************
             POST /index${nullbyte}.html HTTP/1.1
             Host: www.example.com
             Content-Type: application/x-www-form-urlencoded

             fruits=APPLE&fruits=BANANA&fruits=ORANGE
             ************************************************/
            Assert.fail();
        } catch (RuntimeException e) {
            Assert.assertTrue(true);
        } catch (Throwable e) {
            Assert.fail();
        }
    }

    /**
     * パーサ指定が無い場合のUserAgent取得テスト
     *
     * @throws Exception 例外
     */
    @Test
    public void testUserAgentNoParser() throws Exception {
        SystemRepository.clear();
        final StringBuilder request = new StringBuilder();
        request.append("POST /index.html HTTP/1.1\r\n");
        request.append("Host: www.example.com");
        request.append("Content-Type: application/x-www-form-urlencoded\r\n");
        request.append("User-Agent: test\r\n");
        request.append("\r\n");
        
        HttpRequest req = new MockHttpRequest(request.toString());
        UserAgent userAgent = req.getUserAgent();
        assertThat(userAgent.getText(), is("test"));
        assertThat(userAgent.getOsType(), is("UnknownType"));
        assertThat(userAgent.getOsName(), is("UnknownName"));
        assertThat(userAgent.getOsVersion(), is("UnknownVersion"));
        assertThat(userAgent.getBrowserType(), is("UnknownType"));
        assertThat(userAgent.getBrowserName(), is("UnknownName"));
        assertThat(userAgent.getBrowserVersion(), is("UnknownVersion"));
    }

    /**
     * UserAgentヘッダが無く、パーサーも指定されていない場合のUserAgent取得テスト
     *
     * @throws Exception 例外
     */
    @Test
    public void testUserAgentNoHeaderNoParser() throws Exception {
        SystemRepository.clear();
        final StringBuilder request = new StringBuilder();
        request.append("POST /index.html HTTP/1.1\r\n");
        request.append("Host: www.example.com\r\n");
        request.append("Content-Type: application/x-www-form-urlencoded\r\n");
        request.append("\r\n");
        
        HttpRequest req = new MockHttpRequest(request.toString());
        UserAgent userAgent = req.getUserAgent();
        assertThat(userAgent.getText(), is(""));
        assertThat(userAgent.getOsType(), is("UnknownType"));
        assertThat(userAgent.getOsName(), is("UnknownName"));
        assertThat(userAgent.getOsVersion(), is("UnknownVersion"));
        assertThat(userAgent.getBrowserType(), is("UnknownType"));
        assertThat(userAgent.getBrowserName(), is("UnknownName"));
        assertThat(userAgent.getBrowserVersion(), is("UnknownVersion"));
    }


    /**
     * カスタムパーサを指定した場合のUserAgent取得テスト
     *
     * @throws Exception 例外
     */
    @Test
    public void testUserAgentCustomParser() throws Exception {
        File file = File.createTempFile("test", ".xml");
        file.deleteOnExit();

        final BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
        br.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
        br.write("<component-configuration xmlns=\"http://tis.co.jp/nablarch/component-configuration\"");
        br.write(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        br.write("<component name=\"userAgentParser\" class=\"nablarch.fw.web.MockHttpRequestTest$CustomUserAgentParser\"/>");
        br.write("</component-configuration>");
        br.close();
        
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(file.toURI().toString());
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);

        final StringBuilder request = new StringBuilder();
        request.append("POST /index.html HTTP/1.1\r\n");
        request.append("Host: www.example.com\r\n");
        request.append("Content-Type: application/x-www-form-urlencoded\r\n");
        request.append("User-Agent: test\r\n");


        HttpRequest req = new MockHttpRequest(request.toString());
        
        CustomUserAgent userAgent = req.getUserAgent();
        assertThat(userAgent.getText(), is("test"));

    }

    /**
     * リクエストパスに$が含まれる場合でも、リクエストパスの書き換えが行われること。
     * 
     */
    @Test
    public void testRequestContainsDollarMark() {
        MockHttpRequest sut = new MockHttpRequest("GET /index.html HTTP/1.1");
        sut.setRequestPath("/ind$ex.html");
        String requestUri = sut.getRequestUri();
        assertThat(requestUri, is("/ind$ex.html"));
    }

    public static class CustomUserAgentParser implements UserAgentParser {

        @Override
        public UserAgent parse(String userAgentText) {
            return new CustomUserAgent(userAgentText);
        }
    }

    private static class CustomUserAgent extends UserAgent {

        public CustomUserAgent(String text) {
            super(text);
        }

    }
}
