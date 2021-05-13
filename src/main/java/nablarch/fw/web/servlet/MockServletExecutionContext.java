package nablarch.fw.web.servlet;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 単体テスト用の {@link ServletExecutionContext} のモッククラス。
 * <p>
 * このモッククラスでは、 {@link ServletExecutionContext} 内の Servlet API や
 * HTTP 処理に関係する部分を仮実装に置き換えている。
 * これによって、 Servlet API などのインスタンスを用意しなくても、
 * インスタンス生成が可能となっている。
 * </p>
 * <p>
 * リクエストスコープとセッションスコープの情報は、本クラス内のインスタンス変数で定義された
 * Map 内に格納される。<br>
 * また、 {@link #getHttpRequest()} や {@link ServletExecutionContext#getServletContext()}
 * などの HTTP 処理に関係するメソッドは、別途定義されたセッターなどで渡した値をそのまま返すだけの
 * 実装に置き換えられている。<br>
 * これら以外の、もともと {@link ExecutionContext} にあるメソッド({@link #handleNext(Object)} など)は、
 * 本来の処理がそのまま実行される。
 * </p>
 *
 * @author Tanaka Tomoyuki
 */
@Published(tag = "architect")
public class MockServletExecutionContext extends ServletExecutionContext {
    /**
     * リクエストスコープ用のモックのマップ。
     */
    private Map<String, Object> mockRequestScopeMap = new HashMap<String, Object>();
    /**
     * セッションスコープ用のモックのマップ。
     */
    private Map<String, Object> mockSessionScopeMap = new HashMap<String, Object>();
    /**
     * {@link #invalidateSession()}が実行された回数。
     */
    private int invalidateSessionInvokedCount;
    /**
     * {@link #isNewSession()}が返す値。
     */
    private boolean isNewSessionValue;
    /**
     * {@link #hasSession()}が返す値。
     * <p>
     * {@link #getSessionScopedVar(String)} は、 {@link #hasSession()} が {@code true} を
     * 返さないと処理をスキップするようになっている。<br>
     * テストの度に {@code true} を設定することは非効率と考えられるため、デフォルトは {@code true} にしている。
     * </p>
     */
    private boolean hasSessionValue = true;
    /**
     * {@link #getHttpRequest()}が返す値。
     */
    private HttpRequestWrapper httpRequestValue;
    /**
     * {@link #getServletRequest()}が返す値。
     */
    private NablarchHttpServletRequestWrapper servletRequestValue;
    /**
     * {@link #getServletResponse()}が返す値。
     */
    private HttpServletResponse servletResponseValue;
    /**
     * {@link #getServletContext()}が返す値。
     */
    private ServletContext servletContextValue;
    /**
     * {@link #getNativeHttpSession(boolean)}が返す値。
     */
    private HttpSession nativeHttpSessionValue;
    /**
     * 最後に {@link #getNativeHttpSession(boolean)} を実行したときに引数に渡された値
     */
    private Boolean create;

    /**
     * コンストラクタ。
     */
    public MockServletExecutionContext() {
        super(new MockServletRequest(), null, null);
    }

    /**
     * リクエストスコープ用のモックのマップを取得する。
     * @return リクエストスコープ用のモックのマップ
     */
    @Override
    public Map<String, Object> getRequestScopeMap() {
        return mockRequestScopeMap;
    }

    /**
     * リクエストスコープ用のモックのマップを設定する。
     * @param scope リクエストスコープ用のモックのマップ
     * @return このオブジェクト自体
     */
    @Override
    public ExecutionContext setRequestScopeMap(Map<String, Object> scope) {
        mockRequestScopeMap = scope;
        return this;
    }

    /**
     * セッションスコープ用のモックのマップを取得する。
     * @return セッションスコープ用のモックのマップ
     */
    @Override
    public Map<String, Object> getSessionScopeMap() {
        return mockSessionScopeMap;
    }

    /**
     * セッションスコープ用のモックのマップを設定する。
     * @param scope セッションスコープ用のモックのマップ
     * @return このオブジェクト自体
     */
    @Override
    public ExecutionContext setSessionScopeMap(Map<String, Object> scope) {
        mockSessionScopeMap = scope;
        return this;
    }

    /**
     * セッション破棄の処理は行わず、メソッドが実行された回数の記録だけを行う。
     * @return このオブジェクト自体
     */
    @Override
    public ExecutionContext invalidateSession() {
        invalidateSessionInvokedCount++;
        return this;
    }

    /**
     * {@link #invalidateSession()}が実行された回数を取得する。
     * @return {@link #invalidateSession()}が実行された回数
     */
    public int getInvalidateSessionInvokedCount() {
        return invalidateSessionInvokedCount;
    }

    /**
     * {@link #isNewSession()}が返す値を設定する。
     * @param isNewSessionValue {@link #isNewSession()}が返す値
     */
    public void setIsNewSessionValue(boolean isNewSessionValue) {
        this.isNewSessionValue = isNewSessionValue;
    }

    /**
     * {@link #setIsNewSessionValue(boolean)}で設定した値を返す。
     * <p>
     * デフォルトは {@code false} を返す。
     * </p>
     * @return {@link #setIsNewSessionValue(boolean)}で設定した値
     */
    @Override
    public boolean isNewSession() {
        return isNewSessionValue;
    }

    /**
     * {@link #hasSession()}が返す値を設定する。
     * @param hasSessionValue {@link #hasSession()}が返す値
     */
    public void setHasSessionValue(boolean hasSessionValue) {
        this.hasSessionValue = hasSessionValue;
    }

    /**
     * {@link #setHasSessionValue(boolean)}で設定した値を返す。
     * <p>
     * デフォルトは {@code true} を返す。
     * </p>
     * @return {@link #setHasSessionValue(boolean)}で設定した値
     */
    @Override
    public boolean hasSession() {
        return hasSessionValue;
    }


    /**
     * {@link #getHttpRequest()}が返す値を設定する。
     * @param httpRequestValue {@link #getHttpRequest()}が返す値
     */
    public void setHttpRequestValue(HttpRequestWrapper httpRequestValue) {
        this.httpRequestValue = httpRequestValue;
    }

    /**
     * {@link #setHttpRequestValue(HttpRequestWrapper)}で設定した値を返す。
     * <p>
     * デフォルトは {@code null} を返す。
     * </p>
     * @return {@link #setHttpRequestValue(HttpRequestWrapper)}で設定した値
     */
    @Override
    public HttpRequestWrapper getHttpRequest() {
        return httpRequestValue;
    }

    /**
     * {@link #getServletRequest()}が返す値を設定する。
     * @param servletRequestValue {@link #getServletRequest()}が返す値
     */
    public void setServletRequestValue(NablarchHttpServletRequestWrapper servletRequestValue) {
        this.servletRequestValue = servletRequestValue;
    }

    /**
     * {@link #setServletRequestValue(NablarchHttpServletRequestWrapper)}で設定した値を返す。
     * <p>
     * デフォルトは {@code null} を返す。
     * </p>
     * @return {@link #setServletRequestValue(NablarchHttpServletRequestWrapper)}で設定した値
     */
    @Override
    public NablarchHttpServletRequestWrapper getServletRequest() {
        return servletRequestValue;
    }

    /**
     * {@link #getServletResponse()}が返す値を設定する。
     * @param servletResponseValue {@link #getServletResponse()}が返す値
     */
    public void setServletResponseValue(HttpServletResponse servletResponseValue) {
        this.servletResponseValue = servletResponseValue;
    }

    /**
     * {@link #setServletResponseValue(HttpServletResponse)}で設定した値を返す。
     * <p>
     * デフォルトは {@code null} を返す。
     * </p>
     * @return {@link #setServletResponseValue(HttpServletResponse)}で設定した値
     */
    @Override
    public HttpServletResponse getServletResponse() {
        return servletResponseValue;
    }

    /**
     * {@link #getServletContext()}が返す値を設定する。
     * @param servletContextValue {@link #getServletContext()}が返す値
     */
    public void setServletContextValue(ServletContext servletContextValue) {
        this.servletContextValue = servletContextValue;
    }

    /**
     * {@link #setServletContextValue(ServletContext)}で設定した値を返す。
     * <p>
     * デフォルトは {@code null} を返す。
     * </p>
     * @return {@link #setServletContextValue(ServletContext)}で設定した値
     */
    @Override
    public ServletContext getServletContext() {
        return servletContextValue;
    }

    /**
     * {@link #getNativeHttpSession(boolean)}が返す値を設定する。
     * @param nativeHttpSessionValue {@link #getNativeHttpSession(boolean)}が返す値
     */
    public void setNativeHttpSessionValue(HttpSession nativeHttpSessionValue) {
        this.nativeHttpSessionValue = nativeHttpSessionValue;
    }

    /**
     * {@link #setNativeHttpSessionValue(HttpSession)}で設定した値を返す。
     * <p>
     * デフォルトは {@code null} を返す。
     * </p>
     * @param create この値は使用しない
     * @return {@link #setNativeHttpSessionValue(HttpSession)}で設定した値
     */
    @Override
    public HttpSession getNativeHttpSession(boolean create) {
        this.create = create;
        return nativeHttpSessionValue;
    }

    /**
     * 最後に {@link #getNativeHttpSession(boolean)} を実行したときに引数に渡された値を取得する。
     * <p>
     * 一度もメソッドが実行されていない状態では {@code null} を返す。
     * </p>
     * @return 最後に {@link #getNativeHttpSession(boolean)} を実行したときに引数に渡された値
     */
    public Boolean getCreate() {
        return create;
    }

    /**
     * {@link HttpServletRequest}のモック。
     * <p>
     * {@link ServletExecutionContext}のインスタンス化でエラーが発生しないように最低限の実装のみがされている。
     * </p>
     */
    private static class MockServletRequest implements HttpServletRequest {
        @Override
        public String getAuthType() {
            return null;
        }

        @Override
        public Cookie[] getCookies() {
            return new Cookie[0];
        }

        @Override
        public long getDateHeader(String s) {
            return 0;
        }

        @Override
        public String getHeader(String s) {
            return null;
        }

        @Override
        public Enumeration<String> getHeaders(String s) {
            return null;
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return Collections.enumeration(Collections.emptyList());
        }

        @Override
        public int getIntHeader(String s) {
            return 0;
        }

        @Override
        public String getMethod() {
            return null;
        }

        @Override
        public String getPathInfo() {
            return null;
        }

        @Override
        public String getPathTranslated() {
            return null;
        }

        @Override
        public String getContextPath() {
            return "";
        }

        @Override
        public String getQueryString() {
            return null;
        }

        @Override
        public String getRemoteUser() {
            return null;
        }

        @Override
        public boolean isUserInRole(String s) {
            return false;
        }

        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public String getRequestedSessionId() {
            return null;
        }

        @Override
        public String getRequestURI() {
            return "";
        }

        @Override
        public StringBuffer getRequestURL() {
            return null;
        }

        @Override
        public String getServletPath() {
            return null;
        }

        @Override
        public HttpSession getSession(boolean b) {
            return null;
        }

        @Override
        public HttpSession getSession() {
            return null;
        }

        @Override
        public String changeSessionId() {
            return null;
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromUrl() {
            return false;
        }

        @Override
        public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
            return false;
        }

        @Override
        public void login(String username, String password) throws ServletException {
        }

        @Override
        public void logout() throws ServletException {
        }

        @Override
        public Collection<Part> getParts() throws IOException, ServletException {
            return null;
        }

        @Override
        public Part getPart(String name) throws IOException, ServletException {
            return null;
        }

        @Override
        public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
            return null;
        }

        @Override
        public Object getAttribute(String s) {
            return null;
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return null;
        }

        @Override
        public String getCharacterEncoding() {
            return null;
        }

        @Override
        public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

        }

        @Override
        public int getContentLength() {
            return 0;
        }

        @Override
        public long getContentLengthLong() {
            return 0;
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return null;
        }

        @Override
        public String getParameter(String s) {
            return null;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return null;
        }

        @Override
        public String[] getParameterValues(String s) {
            return new String[0];
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return null;
        }

        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public String getScheme() {
            return null;
        }

        @Override
        public String getServerName() {
            return null;
        }

        @Override
        public int getServerPort() {
            return 0;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return null;
        }

        @Override
        public String getRemoteAddr() {
            return null;
        }

        @Override
        public String getRemoteHost() {
            return null;
        }

        @Override
        public void setAttribute(String s, Object o) {

        }

        @Override
        public void removeAttribute(String s) {

        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public Enumeration<Locale> getLocales() {
            return null;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String s) {
            return null;
        }

        @Override
        public String getRealPath(String s) {
            return null;
        }

        @Override
        public int getRemotePort() {
            return 0;
        }

        @Override
        public String getLocalName() {
            return null;
        }

        @Override
        public String getLocalAddr() {
            return null;
        }

        @Override
        public int getLocalPort() {
            return 0;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public AsyncContext startAsync() throws IllegalStateException {
            return null;
        }

        @Override
        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
            return null;
        }

        @Override
        public boolean isAsyncStarted() {
            return false;
        }

        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        @Override
        public AsyncContext getAsyncContext() {
            return null;
        }

        @Override
        public DispatcherType getDispatcherType() {
            return null;
        }
    }
}
