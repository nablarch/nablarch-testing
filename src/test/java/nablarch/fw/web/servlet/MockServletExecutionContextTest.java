package nablarch.fw.web.servlet;

import mockit.Mocked;
import nablarch.fw.ExecutionContext;
import org.junit.Test;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link MockServletExecutionContext}の単体テスト。
 * @author Tanaka Tomoyuki
 */
public class MockServletExecutionContextTest {

    private MockServletExecutionContext sut = new MockServletExecutionContext();

    @Test
    public void RequestScopeMapの設定と取得ができる() {
        Map<String, Object> map = new HashMap<String, Object>();

        assertThat(map, is(not(sameInstance(sut.getRequestScopeMap()))));

        sut.setRequestScopeMap(map);

        assertThat(sut.getRequestScopeMap(), is(sameInstance(map)));
    }

    @Test
    public void SessionScopeMapの設定と取得ができる() {
        Map<String, Object> map = new HashMap<String, Object>();

        assertThat(map, is(not(sameInstance(sut.getSessionScopeMap()))));

        sut.setSessionScopeMap(map);

        assertThat(sut.getSessionScopeMap(), is(sameInstance(map)));
    }

    @Test
    public void セッションスコープの値を出し入れできる() {
        assertThat(sut.getSessionScopedVar("test"), is(nullValue()));

        sut.setSessionScopedVar("test", "Hello");

        assertThat(sut.getSessionScopedVar("test"), is((Object)"Hello"));
    }

    @Test
    public void invalidateSessionを実行すると自分自身を返却する() {
        assertThat(sut.invalidateSession(), is(sameInstance((ExecutionContext) sut)));
    }

    @Test
    public void invalidateSessionを実行すると_実行回数が記録される() {
        assertThat(sut.getInvalidateSessionInvokedCount(), is(0));

        sut.invalidateSession();

        assertThat(sut.getInvalidateSessionInvokedCount(), is(1));

        sut.invalidateSession();

        assertThat(sut.getInvalidateSessionInvokedCount(), is(2));
    }

    @Test
    public void isNewSessionメソッドは_設定したisNewSessionValueの値を返すこと() {
        assertThat(sut.isNewSession(), is(false));

        sut.setIsNewSessionValue(true);

        assertThat(sut.isNewSession(), is(true));
    }

    @Test
    public void hasSessionメソッドは_設定したhasSessionValueの値を返すこと() {
        assertThat(sut.hasSession(), is(true));

        sut.setHasSessionValue(false);

        assertThat(sut.hasSession(), is(false));
    }

    @Test
    public void getHttpRequestメソッドは_設定したhttpRequestValueの値を返すこと(
        @Mocked final HttpRequestWrapper mockHttpRequest
    ) {
        assertThat(sut.getHttpRequest(), is(nullValue()));

        sut.setHttpRequestValue(mockHttpRequest);

        assertThat(sut.getHttpRequest(), is(sameInstance(mockHttpRequest)));
    }

    @Test
    public void getServletRequestメソッドは_設定したServletRequestValueの値を返すこと(
        @Mocked NablarchHttpServletRequestWrapper mockServletRequest
    ) {
        assertThat(sut.getServletRequest(), is(nullValue()));

        sut.setServletRequestValue(mockServletRequest);

        assertThat(sut.getServletRequest(), is(sameInstance(mockServletRequest)));
    }

    @Test
    public void getServletResponseメソッドは_設定したServletResponseValueの値を返すこと(
        @Mocked HttpServletResponse mockServletResponse
    ) {
        assertThat(sut.getServletResponse(), is(nullValue()));

        sut.setServletResponseValue(mockServletResponse);

        assertThat(sut.getServletResponse(), is(sameInstance(mockServletResponse)));
    }

    @Test
    public void getServletContextメソッドは_設定したServletContextValueの値を返すこと(
        @Mocked ServletContext mockServletContext
    ) {
        assertThat(sut.getServletContext(), is(nullValue()));

        sut.setServletContextValue(mockServletContext);

        assertThat(sut.getServletContext(), is(sameInstance(mockServletContext)));
    }

    @Test
    public void getNativeHttpSessionメソッドは_設定したNativeHttpSessionValueの値を返すこと(
        @Mocked HttpSession mockHttpSession
    ) {
        assertThat(sut.getNativeHttpSession(true), is(nullValue()));

        sut.setNativeHttpSessionValue(mockHttpSession);

        assertThat(sut.getNativeHttpSession(true), is(sameInstance(mockHttpSession)));
    }

    @Test
    public void 最後にgetNativeHttpSessionメソッドに渡したbooleanの値を取得できる() {
        assertThat(sut.getCreate(), is(nullValue()));

        sut.getNativeHttpSession(false);

        assertThat(sut.getCreate(), is(false));

        sut.getNativeHttpSession(true);

        assertThat(sut.getCreate(), is(true));
    }
}