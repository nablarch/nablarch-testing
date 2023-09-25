package nablarch.test.core.entity;

import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * {@link MessageComparedByContent}のテスト。
 *
 * <p>
 * テスト対象クラスは、{@link BeanValidationTestStrategy#createExpectedMessage(MessageLevel, String, Object[])}を
 * 通してテストされるため、本テストクラスでは上記で検証できない処理の検証を行う。
 */
public class MessageComparedByContentTest {

    @Test
    public void nullのメッセージを渡した時_hashCodeが0となること() {
        // setup
        Message actual = new MessageComparedByContent(null);

        // execute, verify
        assertEquals(0, actual.hashCode());
    }

}