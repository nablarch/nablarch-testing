package nablarch.fw.web;

/**
 * {@link HttpServer}のファクトリインタフェース。
 *
 * {@link HttpServer}はJettyに依存しているが、Jettyバージョンによって対応するJavaが異なる。
 * Jettyのバージョン（6系、9系）を切り替えられるようにするためには、
 * nablarch-testingが直接Jettyに依存しないようにする必要がある。
 * このため本インタフェースを導入し、{@link HttpServer}を抽象クラスとしている。
 * Jettyに直接依存するモジュールは、nablarch-testing-jetty6とnablarch-testing-jetty9となる。
 *
 * @link https://www.eclipse.org/jetty/documentation/current/what-jetty-version.html
 * @author Tsuyoshi Kawasaki
 */
public interface HttpServerFactory {

    /**
     * {@link HttpServer}を作成する。
     * @return {@link HttpServer}
     */
    HttpServer create();

}
