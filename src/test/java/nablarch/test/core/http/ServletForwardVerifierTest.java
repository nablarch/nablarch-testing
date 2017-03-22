package nablarch.test.core.http;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;

import nablarch.common.web.download.StreamResponse;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpErrorResponse;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.handler.HttpErrorHandler;
import nablarch.test.support.tool.Hereis;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author T.Kawasaki
 */
public class ServletForwardVerifierTest {

    private ServletForwardVerifier target = new ServletForwardVerifier();

    /**
     * 通常のレスポンス返却時、フォワード先のアサートが成功すること。
     */
    @Test
    public void testHandle() {

        new ExecutionContext().addHandlers(Arrays.asList(target, new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                return new HttpResponse("/path/to/next.jsp");
            }
        })).handleNext(new MockHttpRequest());

        target.verifyForward("/path/to/next.jsp");
    }
    
    @Test
    public void testAssertAboutForwardingWithVariousSchemes() {
        // servlet://
        new ExecutionContext().addHandlers(Arrays.asList(target, new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                return new HttpResponse("servlet:///path/to/next.jsp");
            }
        })).handleNext(new MockHttpRequest());

        // redirect://
        target.verifyForward("/path/to/next.jsp");
        
        new ExecutionContext().addHandlers(Arrays.asList(target, new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                return new HttpResponse("redirect:///path/to/redirect.do");
            }
        })).handleNext(new MockHttpRequest());

        target.verifyForward("/path/to/redirect.do");
        
                
        new ExecutionContext().addHandlers(Arrays.asList(target, new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                return new HttpResponse("http://www.example.com/path/to/external_page.html");
            }
        })).handleNext(new MockHttpRequest());

        target.verifyForward("http://www.example.com/path/to/external_page.html");
        
        // https://
        new ExecutionContext().addHandlers(Arrays.asList(target, new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                return new HttpResponse("https://www.example.com/path/to/secure_page.html");
            }
        })).handleNext(new MockHttpRequest());

        target.verifyForward("https://www.example.com/path/to/secure_page.html");
        
        
        // 内部フォーワードは最終的なレスポンスには現れないのでエラー        
        new ExecutionContext().addHandlers(Arrays.asList(target, new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                return new HttpResponse("forward:///path/to/forward.do");
            }
        })).handleNext(new MockHttpRequest());
        try {
            target.verifyForward("/path/to/forward.do");
            fail();
        } catch (Throwable e) {
            assertEquals(AssertionError.class, e.getClass());
        } 
    }

    /**
     * エラーレスポンスが返却された時、フォワード先のアサートが成功すること。
     */
    @Test
    public void testHandleErrorResponse() {

        new ExecutionContext().addHandlers(Arrays.asList(target, new HttpErrorHandler(), new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                throw new HttpErrorResponse(500, "/path/to/error.jsp");
            }
        })).handleNext(new MockHttpRequest());

        target.verifyForward("/path/to/error.jsp");
    }

    @Test
    public void testHandleWhenSystemError() {
        HttpErrorHandler httpErrorHandler = new HttpErrorHandler();
        //デフォルトではエラーページが設定されていないため、設定する。
        httpErrorHandler.setDefaultPage("500", "/jsp/systemError.jsp");

        HttpRequestHandler httpRequestHandler = new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                throw new RuntimeException("exception for test.(NOT test failure)");
            }
        };

        new ExecutionContext().addHandlers(Arrays.asList(target, httpErrorHandler, httpRequestHandler))
                .handleNext(new MockHttpRequest());

        target.verifyForward("/jsp/systemError.jsp");  // HttpErrorHandlerで設定される。
    }
    /**
     * フォワードされない場合は、期待値空文字でアサート成功となること。
     */
    @Test
    public void testHandleNotForwarded() {

        new ExecutionContext().addHandlers(Arrays.asList(target, new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                return new HttpResponse(200); // フォワード指定なし。
            }
        })).handleNext(new MockHttpRequest());

        target.verifyForward("");
    }
    
    /**
     * ダウンロードでフォワードされない場合は、期待値空文字でアサート成功となること。
     */
    @Test
    public void testHandleDownload() {

        new ExecutionContext().addHandlers(Arrays.asList(target, new HttpRequestHandler() {
            public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                // ダウンロード
                File file = Hereis.fileWithEncoding("./work/テスト一時ファイル.txt", "UTF-8");
                boolean deleteOnCleanup = true;
                /*
                あいうえお
                かきくけこさしすせそ
                たちつてとなにぬねのはひふへほ
                 */
                StreamResponse response = new StreamResponse(file, deleteOnCleanup);
                response.setContentType("text/plain; charset=UTF-8");
                response.setContentDisposition(file.getName());
                return response;
            }
        })).handleNext(new MockHttpRequest());

        target.verifyForward("");
    }
}
