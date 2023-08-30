package nablarch.test.core.entity;

import nablarch.core.message.Message;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * {@link BeanValidationResultMessage}のテスト。
 *
 * <p>
 * テスト対象クラスは、{@link BeanValidationTestStrategy#createExpectedValidationResultMessage(String, String, Object[])}を
 * 通してテストされるため、本テストクラスでは上記で検証できない処理の検証を行う。
 */
public class BeanValidationResultMessageTest {

    @Test
    public void nullのメッセージを渡した時_hashCodeが0となること() {
        // setup
        Message actual = new BeanValidationResultMessage(null);

        // execute, verify
        assertEquals(0, actual.hashCode());
    }
}