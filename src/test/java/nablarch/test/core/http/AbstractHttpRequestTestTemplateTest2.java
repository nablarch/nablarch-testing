package nablarch.test.core.http;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.tool.Hereis;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * {@link AbstractHttpRequestTestTemplate}のテストクラス。
 *
 * @author T.Kawasaki
 */
public class AbstractHttpRequestTestTemplateTest2 {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
            "nablarch/test/core/http/http-test-configuration.xml");

    @Before
    public void setUp() {
        HttpRequestTestSupport.resetHttpServer();
    }

    /**
     * テスト対象機能にてリクエストパラメータが変更される場合、
     * テスト側で変更内容を検証できること。
     * <p/>
     * EXCELで設定したCookieが正しく設定されていることを確認する。
     */
    @Test
    public void testAssertRequestParameterModified() {

        // テスト用にサブクラス化
        MockHttpRequestTestTemplate target
                = new MockHttpRequestTestTemplate(getClass(), new HttpRequestHandler() {

            /** リクエストパラメータを変更し、200 OKを返却する。 */
            @Override
            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                // リクエストパラメータを変更する
                req.setParam("foo", "modified");

                // Cookieをリクエストスコープに移送
                ctx.setRequestScopedVar("requestScopeLang", req.getCookie()
                                                               .get("lang"));
                ctx.setRequestScopedVar("requestScopeHoge", req.getCookie()
                                                               .get("cookieHoge"));

                // ボディを設定する。
                String body = Hereis.string();
                /*
                <html>
                <head><title>test</title></head>
                <body><p>Hello, World!</p></body>
                </html>*/
                HttpResponse res = new HttpResponse().write(body); // 200 OK

                // ステータスコードを移送
                HttpRequestTestSupport.getTestSupportHandler()
                                      .setStatusCode(res.getStatusCode());

                return res;
            }
        });


        // 実行
        target.execute("testAssertRequest", new BasicAdvice() {
            /** 実行前のコールバック */
            @Override
            public void beforeExecute(TestCaseInfo testCaseInfo, ExecutionContext context) {
                // リクエストパラメータに新たな値は設定されていないこと
                HttpRequest request = testCaseInfo.getHttpRequest();
                assertThat(request.getParam("foo")[0], is("original"));
                assertThat(request.getCookie()
                                  .get("lang"), is("en"));
                assertThat(request.getCookie()
                                  .get("cookieHoge"), is("hoge-value"));
            }

            /** 実行後のコールバック */
            @Override
            public void afterExecute(TestCaseInfo testCaseInfo, ExecutionContext context) {
                // リクエストパラメータに新たな値が設定されていること
                HttpRequest request = testCaseInfo.getHttpRequest();
                assertThat(request.getParam("foo")[0], is("modified"));

                // リクエストスコープに移送したCookieのアサート
                assertThat(context.<String>getRequestScopedVar("requestScopeLang"), is("en"));
                assertThat(context.<String>getRequestScopedVar("requestScopeHoge"), is("hoge-value"));
            }
        });
    }


}
