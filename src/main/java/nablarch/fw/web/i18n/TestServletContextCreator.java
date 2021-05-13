package nablarch.fw.web.i18n;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * {@link ServletContextCreator}のテスト環境用実装クラス。<br />
 * 
 * 明示的に生成したHTTPセッションからサーブレットコンテキストを生成する。
 * 
 * @author Naoki Yamamoto
 */
public class TestServletContextCreator implements ServletContextCreator {

    @Override
    public ServletContext create(HttpServletRequest request) {
        return request.getSession(true).getServletContext();
    }
}
