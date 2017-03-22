package nablarch.test.core.log;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import nablarch.core.log.basic.LogContext;
import nablarch.core.log.basic.LogLevel;

import org.junit.Test;

/** {@link ExpectedLogMessage}のテストクラス。 */
public class ExpectedLogMessageTest {

    /** テスト対象 */
    private ExpectedLogMessage target = new ExpectedLogMessage(
            LogLevel.ERROR,
            Arrays.asList("エラー", "ららら"));

    @Test
    public void testMatchesInException() {
        assertThat(target.matches(
                new LogContextForTesting(
                        LogLevel.ERROR,
                        "ららら",
                        new Exception("例外のなかのエラー"))),
                is(true));
    }


    @Test
    public void testMatchesInNestedException() {
        assertThat(target.matches(
                new LogContextForTesting(
                        LogLevel.ERROR,
                        "",
                        new Exception("hoge", new Error("ららら", new RuntimeException("エラー"))))),
                is(true));
    }

    /** ログメッセージがマッチするテストケース。 */
    @Test
    public void testMatches() {
        assertThat(target.matches(
                new LogContextForTesting(
                        LogLevel.ERROR,
                        "あららら。これはエラーですよ。")),
                is(true));
    }

    /** ログレベルがマッチしないテストケース。 */
    @Test
    public void testLevelNotMatch() {
        assertThat(target.matches(
                new LogContextForTesting(
                        LogLevel.WARN,      // ログレベルがちがう
                        "これはエラーではなく警告です。")),
                is(false));
    }

    /** メッセージ文言がマッチしないテストケース(例外あり) */
    @Test
    public void testMessageNotMatchWithException() {
        assertThat(target.matches(
                new LogContextForTesting(
                        LogLevel.ERROR,
                        "This is an error.",
                        new Exception("exception", new Exception("nested one.")))),   // メッセージがちがう
                is(false));
    }

    /** メッセージ文言がマッチしないテストケース(例外なし) */
    @Test
    public void testMessageNotMatch() {
        assertThat(target.matches(
                new LogContextForTesting(
                        LogLevel.ERROR,
                        "This is an error.")),    // メッセージがちがう
                is(false));
    }



    /** メッセージ文言が部分的にマッチしないテストケース */
    @Test
    public void testMessageNotMatchPartially() {
        assertThat(target.matches(
                new LogContextForTesting(
                        LogLevel.ERROR,
                        "ららら")),
                is(false));
    }

    /** 全くマッチしないケース */
    @Test
    public void testNotMatch() {
        assertThat(target.matches(
                new LogContextForTesting(
                        LogLevel.WARN,
                        "This is not an error.")),
                is(false));
    }

    /**
     * 文字列に変換できること。
     */
    @Test
    public void testToString() {
        assertThat(target.toString(), containsString("ERROR"));
        assertThat(target.toString(), containsString("エラー"));
    }

    /** テスト用LogContext */
    private static class LogContextForTesting extends LogContext {
        /**
         * コンストラクタ。
         *
         * @param level   {@link LogLevel}
         * @param message メッセージ
         */
        protected LogContextForTesting(LogLevel level, String message) {
            super(null, level, message, null);
        }

        private LogContextForTesting(LogLevel level, String message, Throwable error) {
            super(null, level, message, error);
        }
    }
}
