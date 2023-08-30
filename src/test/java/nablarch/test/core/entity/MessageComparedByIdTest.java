package nablarch.test.core.entity;

import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * {@link MessageComparedById}のテスト。
 *
 * <p>
 * テスト対象クラスは、{@link NablarchValidationTestStrategy#createExpectedMessage(MessageLevel, String, Object[])}を
 * 通して生成されるため、本テストクラスでは上記で検証できない処理の検証を行う。
 */
public class MessageComparedByIdTest {

    @Test
    public void nullのメッセージを渡した時_hashCodeが0となること() {
        // setup
        Message actual = new MessageComparedById(null);

        // execute, verify
        assertEquals(0, actual.hashCode());
    }


}