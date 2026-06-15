package nablarch.test.tool.converter;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * {@link ConverterException} のテストクラス。
 *
 * <p>非検査例外であること・メッセージ／原因の保持を検証する。</p>
 *
 * @author kiyobot
 */
public class ConverterExceptionTest {

    /**
     * Given: メッセージのみ。
     * When : 生成。
     * Then : メッセージを保持し原因は無し。非検査例外。
     */
    @Test
    public void holdsMessage() {
        ConverterException e = new ConverterException("boom");

        assertThat(e.getMessage(), is("boom"));
        assertThat(e.getCause(), is((Throwable) null));
        assertThat(e, is(instanceOf(RuntimeException.class)));
    }

    /**
     * Given: メッセージと原因。
     * When : 生成。
     * Then : 双方を保持する。
     */
    @Test
    public void holdsMessageAndCause() {
        Throwable cause = new IllegalStateException("root");
        ConverterException e = new ConverterException("wrap", cause);

        assertThat(e.getMessage(), is("wrap"));
        assertThat(e.getCause(), is(sameInstance(cause)));
    }
}
