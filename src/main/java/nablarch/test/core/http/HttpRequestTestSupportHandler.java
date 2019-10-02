package nablarch.test.core.http;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.upload.PartInfo;


/**
 * HTTPリクエストのテスト時に先頭の設定するリクエストハンドラ。<br/>
 * 本クラスでは、以下の処理を行う。
 * <ul>
 * <li>テストクラスから指定されたExecutionContextを、
 * handleメソッドの引数のExecutionContextへ移送する。</li>
 * <li>ExecutionContext#handleNext呼び出し後のExecutionContextの情報を
 * テストクラスから指定されたExecutionContextへ移送する。</li>
 * <li>ハンドラキューの処理結果となるHttpResponseオブジェクトのステータスコードを
 * アサートできるようにスタティック変数として保持しておく。</li>
 * </ul>
 * {@link HttpRequestTestSupport#execute(Class, String, HttpRequest, ExecutionContext)}から
 * nablarch.fw.web.HttpServer#handle(HttpRequest, ExecutionContext)が呼ばれるが、
 * ここで引数に{@link ExecutionContext}を渡しても使用されない。
 * （nablarch.fw.Handler#handle(Object, nablarch.fw.ExecutionContext)を実装する為、
 * 引数として用意されているだけで、実際には使用しない）
 * 実際には内蔵サーバ内で新たに生成された{@link ExecutionContext}が使用される。
 * テストクラス側で生成した{@link ExecutionContext}の情報を使用してテストを行う為に、
 * 本ハンドラにて移送を行う。
 * <p/>
 * また、コンテンツパスを指定してレスポンスを行った場合、
 * 内蔵サーバからは一律ステータスコード200が返却される。
 * (JSP等のリソースに対するサーブレットフォーワードは全てこれに含まれる。)
 * このため、HttpResponseオブジェクトのステータスコードと、クライアントに対する
 * 実際のレスポンスコードが一致しなくなるので、本ハンドラにHttpResponseオブジェクトのステータス
 * コードを保持しておき、後続のアサート処理で参照する。
 *
 * ただし、リダイレクトの場合はServletAPI({@link javax.servlet.http.HttpServletResponse#sendRedirect(String)})
 * を使用しているため、{@link HttpResponse}にはリダイレクトのステータスコードは設定されない。
 * （ステータスコードの設定をコンテナに任せているため）
 * 従って、リダイレクトの場合のステータスコードは、本クラスではなく、
 * コンテナが返却するレスポンスから取得する必要がある。
 * 
 * @see nablarch.fw.web.handler.HttpResponseHandler#setStatusCode(HttpResponse, nablarch.fw.web.servlet.ServletExecutionContext)
 * @see HttpRequestTestSupport#assertStatusCode(String, int, HttpResponse)
 */
@Published
public class HttpRequestTestSupportHandler implements HttpRequestHandler {

    /**
     * {@link CountDownLatch}のキー。
     * サーバ側の処理完了を待つためのラッチを格納・取得する用途に使用する。
     */
    public static final String NABLARCH_JETTY_CONNECTOR_LATCH = "nablarch.jetty.connector.larch";

    /** テストクラスから指定されたExecutionContext */
    private ExecutionContext contextFromTest;

    /** HTTPリクエストテスト用設定情報 */
    private HttpTestConfiguration config;

    /** マルチパート */
    private Map<String, List<PartInfo>> multipart;
    
    /** ステータスコード */
    private Integer statusCode = null;

    /**
     * コンストラクタ。
     *
     * @param config HttpTestConfiguration
     */
    public HttpRequestTestSupportHandler(HttpTestConfiguration config) {
        this.config = config;
    }

    /**
     * {@inheritDoc}
     * {@link ExecutionContext}の移送を行う。
     *
     * @param context nablarch.fw.web.HttpServerで生成された実際の{@link ExecutionContext}
     */
    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
        try {
            // ExecutionContextの移送を行う
            convertExecutionContext(contextFromTest, context, false);

            // セッション情報の設定
            setDefaultScopeVars(context.getSessionScopeMap(), config.getSessionInfo());

            // マルチパート情報の移送を行う。
            request.setMultipart(multipart);

            // 次のハンドラを起動
            HttpResponse httpResponse = context.handleNext(request);

            // ステータスコードを設定
            this.statusCode = httpResponse.getStatusCode();

            // ExecutionContextの移送を行う
            // 移送先のcontextは初期化を行う。
            convertExecutionContext(context, contextFromTest, true);
            return httpResponse;
        } finally {
            CountDownLatch latch = context.getRequestScopedVar(NABLARCH_JETTY_CONNECTOR_LATCH);
            if (latch != null) {
                latch.countDown();
            }
        }
    }


    
    /**
     * 指定されたスコープの変数に既定の値を設定する。
     * 既に値が設定されている変数についてはなにもしない。
     * 
     * @param scope    スコープ
     * @param defaults 各変数の既定値を収めたMap
     */
    private void setDefaultScopeVars(Map<String, Object> scope, Map<String, Object> defaults) {
        for (Entry<String, Object> entry : defaults.entrySet()) {
            if (!scope.containsKey(entry.getKey())) {
                scope.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * ExecutionContextの情報を移送する。
     *
     * @param src 移送元
     * @param dst 移送先
     * @param override 移送先の情報を移送元の情報で完全に上書きをするか否か(trueを指定された場合は上書きをする。falseを指定された場合には、追記を行う。)
     */
    private static void convertExecutionContext(ExecutionContext src, ExecutionContext dst, boolean override) {
        if (override) {
            dst.getRequestScopeMap().clear();
            dst.getRequestScopeMap().putAll(src.getRequestScopeMap());
            dst.getSessionScopeMap().clear();
            dst.getSessionScopeMap().putAll(src.getSessionScopeMap());
            dst.getSessionStoreMap().clear();
            dst.getSessionStoreMap().putAll(src.getSessionStoreMap());
        } else {
            dst.getRequestScopeMap().putAll(src.getRequestScopeMap());
            dst.getSessionScopeMap().putAll(src.getSessionScopeMap());
            dst.getSessionStoreMap().putAll(src.getSessionStoreMap());
        }
    }

    /**
     * ステータスコードの値を取得する。
     * @return ステータスコード値
     */
    Integer getStatusCode() {
        return statusCode;
    }

    /**
     * ステータスコードの値を設定する。
     * @param statusCode ステータスコード値
     */
    void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    } 
    
    /**
     * {@link ExecutionContext}を設定する。
     *
     * @param context ExecutionContext
     */
    public void setContext(ExecutionContext context) {
        this.contextFromTest = context;
    }

    /**
     * 自インスタンスをハンドラキューに登録する。<br/>
     * 本クラスは、テストデータに記載された内容をExecutionContextに移送する責務を持つ。
     * よって、各ハンドラでリクエストスコープ、セッションスコープの値を使用するより前に
     * 呼び出される必要がある。本メソッドでは、自インスタンスをハンドラキューの先頭に配置する。
     *
     * @param handlerQueue 自インスタンスの登録対象となるハンドラキュー
     */
    @SuppressWarnings("rawtypes")
    void register(List<Handler> handlerQueue) {
        handlerQueue.add(0, this);
    }

    /**
     * マルチパート情報を設定する。
     *
     * @param req 設定元のリクエスト
     */
    public void setMultipart(HttpRequest req) {
        this.multipart = req.getMultipart();
    }
}

