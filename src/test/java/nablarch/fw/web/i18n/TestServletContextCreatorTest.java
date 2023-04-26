package nablarch.fw.web.i18n;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link TestServletContextCreator}のテスト。
 * @author Naoki Yamamoto
 */
public class TestServletContextCreatorTest {
    /**
     * {@link TestServletContextCreator#create(HttpServletRequest)}のテスト。
     */
    @Test
    public void testCreate() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        final ServletContext context = mock(ServletContext.class);

        when(request.getSession(true)).thenReturn(session);
        when(session.getServletContext()).thenReturn(context);

        ServletContextCreator creator = new TestServletContextCreator();
        assertThat(creator.create(request), instanceOf(ServletContext.class));
    }
}
