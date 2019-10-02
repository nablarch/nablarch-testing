package nablarch.test.core.http;

import java.io.File;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;

/**
 * {@link AbstractHttpRequestTestTemplate}のテスト用サブクラス。
 *
 * @author T.Kawasaki
 */
class MockHttpRequestTestTemplate
        extends AbstractHttpRequestTestTemplate<TestCaseInfo> {

    /** {@link AbstractHttpRequestTestTemplate}のテスト用モック。*/
    private static final HttpRequestHandler DEFAULT_HANDLER = new HttpRequestHandler() {
        @Override
        public HttpResponse handle(HttpRequest request, ExecutionContext context) {
            return new HttpResponse(); // 200 OK
        }
    };

    /** HttpServer内で起動されるハンドラ */
    private final HttpRequestHandler handler;

    /**
     * コンストラクタ。
     * {@link AbstractHttpRequestTestTemplate}のテスト用モックを作成する。
     * このモックは200 OKを返却する。
     *
     * @param testClass テストクラス
     */
    MockHttpRequestTestTemplate(Class<?> testClass) {
        this(testClass, DEFAULT_HANDLER);
    }
    /**
     * コンストラクタ。
     * {@link AbstractHttpRequestTestTemplate}のテスト用モックを作成する。
     * 起動するハンドラを明示的に指定する。
     *
     * @param testClass テストクラス
     * @param handler HttpServer内で起動されるハンドラ
     */
    MockHttpRequestTestTemplate(Class<?> testClass,
                                HttpRequestHandler handler) {
        super(testClass);
        this.handler = handler;
    }

    /** 強制的に初期化 */
    @Override
    protected final void initializeIfNotYet(HttpTestConfiguration config,
                                            File dumpDir,
                                            String className) {
        createHttpServer(config);
    }

    /** テスト用HttpServerを返却する。 */
    @Override
    protected HttpServer createHttpServer() {
        return new HttpServerForTesting();
    }

    /** just for compilation... */
    @Override
    protected String getBaseUri() {
        return null;
    }


    /** テスト用HttpServer */
    class HttpServerForTesting extends MockHttpServer {

        /** サーバは起動しない */
        @Override
        public final HttpServer startLocal() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
            // 指定されたハンドラを起動する。
            HttpResponse res = handler.handle(req, ctx);
            // ステータスコードを設定する。
            HttpRequestTestSupport.getTestSupportHandler().setStatusCode(res.getStatusCode());
            return res;
        }
    }
}
