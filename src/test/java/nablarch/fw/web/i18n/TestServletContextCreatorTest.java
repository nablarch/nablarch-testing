package nablarch.fw.web.i18n;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import mockit.Expectations;
import mockit.Mocked;

import org.junit.Test;

/**
 * {@link TestServletContextCreator}のテスト。
 * @author Naoki Yamamoto
 */
public class TestServletContextCreatorTest {
    /**
     * {@link TestServletContextCreator#create(HttpServletRequest)}のテスト。
     */
    @Test
    public void testCreate(@Mocked final HttpServletRequest request,  @Mocked final HttpSession session, @Mocked final ServletContext context) {
        new Expectations() {{
            request.getSession(true); result = session;
            session.getServletContext(); result = context;
        }};

        ServletContextCreator creator = new TestServletContextCreator();
        assertThat(creator.create(request), instanceOf(ServletContext.class));
    }
}
