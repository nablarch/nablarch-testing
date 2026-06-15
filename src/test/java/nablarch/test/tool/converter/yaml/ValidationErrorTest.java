package nablarch.test.tool.converter.yaml;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link ValidationError} のテスト。不変値クラスのアクセサ・整形・null ガードを検証する。
 */
public class ValidationErrorTest {

    /**
     * [Given] filePath / location / message を与えて生成
     * [When]  各アクセサと toString を呼ぶ
     * [Then]  与えた値を返し、toString は "[path] location: message" 形式
     */
    @Test
    public void accessorsAndToString() {
        ValidationError e = new ValidationError("/tmp/case.yaml", "setup_files[0].rows[1]", "[V-COL] 列数不一致");

        assertThat(e.getFilePath(), is("/tmp/case.yaml"));
        assertThat(e.getLocation(), is("setup_files[0].rows[1]"));
        assertThat(e.getMessage(), is("[V-COL] 列数不一致"));
        assertThat(e.toString(), is("[/tmp/case.yaml] setup_files[0].rows[1]: [V-COL] 列数不一致"));
    }

    /**
     * [Given] いずれかの引数に null
     * [When]  生成
     * [Then]  NullPointerException
     */
    @Test
    public void nullArgumentsRejected() {
        assertNpe(() -> new ValidationError(null, "loc", "msg"));
        assertNpe(() -> new ValidationError("path", null, "msg"));
        assertNpe(() -> new ValidationError("path", "loc", null));
    }

    private void assertNpe(Runnable r) {
        try {
            r.run();
            fail("NullPointerException が送出されること");
        } catch (NullPointerException expected) {
            // OK
        }
    }
}
