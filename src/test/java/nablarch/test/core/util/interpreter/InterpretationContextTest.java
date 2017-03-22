package nablarch.test.core.util.interpreter;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import nablarch.test.core.util.interpreter.InterpretationContext.InterpretationFailedException;

import org.junit.Test;

/**
 * {@link InterpretationContextTest}のテストクラス。
 *
 * @author T.Kawasaki
 */
public class InterpretationContextTest {

    /**
     * invokeNext起動時、解釈クラスが起動されること。
     */
    @Test
    public void testInvokeSuccess() {
        InterpretationContext target = new InterpretationContext("hoge", new TestDataInterpreter() {
            public String interpret(InterpretationContext context) {
                return "fuga";
            }
        });
        assertEquals("fuga", target.invokeNext());
    }

    /**
     * invokeNextが失敗した場合、例外が発生すること。
     * その例外クラスには、解釈クラス、対象値、原因となった例外が含まれること。
     */
    @Test
    public void testInvokeNextFail() {
        InterpretationContext target = new InterpretationContext("hoge", new TestDataInterpreter() {
            public String interpret(InterpretationContext context) {
                throw new IllegalStateException("cause");
            }
        });
        try {
            target.invokeNext();
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("interpretation failed."));
            // 対象値
            assertThat(e.getMessage(), containsString("value=[hoge]"));
            // 解釈クラス
            assertThat(e.getMessage(), containsString(
                    "interpreter=[nablarch.test.core.util.interpreter.InterpretationContextTest$"));
            // ネストした例外クラス
            assertThat(e.getCause().getMessage(), is("cause"));
        }
    }

    /**
     * invokeNextが失敗した場合、例外が発生すること。
     * 既に正常に解釈が終了している{@link TestDataInterpreter}では、例外がラップせずにリスローすること。
     */
    @Test
    public void testInvokeNextFailStacked() {
        InterpretationContext target = new InterpretationContext("hoge", new TestDataInterpreter() {
            @Override
            public String interpret(InterpretationContext context) {
                context.setValue("normally interpreted");
                return context.invokeNext();
            }
        }, new TestDataInterpreter() {
            public String interpret(InterpretationContext context) {
                throw new IllegalStateException("cause");
            }
        });
        try {
            target.invokeNext();
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getCause(), is(not(instanceOf(InterpretationFailedException.class))));
            assertThat(target.getValue(), is("normally interpreted"));
        }
    }

    /**
     * invokeNext起動時、全ての解釈クラスが解釈できなかった場合、
     * 元の値がそのまま返却されること。
     */
    @Test
    public void testInvokeNextReturnsOriginalValue() {
        InterpretationContext target = new InterpretationContext("hoge", new TestDataInterpreter() {
            public String interpret(InterpretationContext context) {
                return context.invokeNext();
            }
        });
        assertEquals("hoge", target.invokeNext());
    }


    /**
     * アクセサのテスト
     */
    @Test
    public void testAccessors() {
        InterpretationContext target = new InterpretationContext("hoge", new TestDataInterpreter() {
            public String interpret(InterpretationContext context) {
                return "fuga";
            }
        });
        assertEquals("hoge", target.getValue());
        target.setValue("moge");
        assertEquals("moge", target.getValue());
    }

}
