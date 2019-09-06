package nablarch.test.core.http;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.handler.GlobalErrorHandler;
import nablarch.fw.web.HttpMethodBinding;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.handler.ForwardingHandler;
import nablarch.fw.web.handler.HttpCharacterEncodingHandler;
import nablarch.fw.web.handler.HttpErrorHandler;
import nablarch.fw.web.handler.HttpResponseHandler;

import java.util.Arrays;
import java.util.List;

/**
 * HTTPServerのモック。
 */
public class MockHttpServer extends HttpServer {

    public MockHttpServer() {
        setHandlerQueue(Arrays.asList(
                new GlobalErrorHandler()
                // これらのハンドラはServletExecutionContextを要求するためサーブレットコンテナ外では動作しない
                //, new HttpCharacterEncodingHandler()
                //, new HttpResponseHandler()
                , new ForwardingHandler()
                , new HttpErrorHandler()));
        setMethodBinder(new HttpMethodBinding.Binder());
    }

    /**
     * handleメソッドでは何もしない。
     *
     * @param req HttpRequest
     * @param context ExecutionContext
     */
    @Override
    public HttpResponse handle(HttpRequest req, ExecutionContext context) {
        List<Handler> handlerQueue = getHandlerQueue();
        context.setHandlerQueue(handlerQueue);
        return context.handleNext(req);
    }

    @Override
    public HttpServer start() {
        return this;
    }

    @Override
    public HttpServer join() {
        return this;
    }

    @Override
    public HttpServer startLocal() {
        return this;
    }
}
