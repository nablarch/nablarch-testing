package nablarch.test.core.util.interpreter;

import org.junit.Assert;
import org.junit.Test;

/**
 * {@link QuotationTrimmer}のテストクラス。
 *
 * @author T.Kawasaki
 */
public class QuotationTrimmerTest {

    /** テスト対象クラス */
    private TestDataInterpreter target = new QuotationTrimmer();

    /**
     * 半角の２重引用符で囲われた文字列が入力された場合のテストケース。
     */
    @Test
    public void testInterpretHalfWidthQuotation()  {

        // 両端が２重引用符で囲われた場合
        assertResult(
                "\"abc\"",
                "abc");  // 引用符が除去されること

        // ２重引用符で２重に囲われた場合
        assertResult(
                "\"\"abc\"\"",
                "\"abc\"");  // 外側の２重引用符が除去されること

        // ２重引用符内に空白が存在する場合
        assertResult(
                "\" abc \"",
                " abc ");  // 空白は削除されないこと
    }

    /**
     * 全角の２重引用符で囲われた文字列が入力された場合のテストケース。
     */
    @Test
    public void testInterpretFullWidthQuotation()  {
        // 両端が全角２重引用符で囲われた場合
        assertResult(
                "” あいう ”",
                " あいう ");  // 引用符が除去されること
    }

    /**
     * 引用符で囲われていない場合、入力された文字列がそのまま返却されること。
     */
    @Test
    public void testInterpretNotQuoted()  {
         // 両端にないものは対象外
        assertResult(
                "あ\"い\"う”え”お",
                "あ\"い\"う”え”お"
        );

        // 先頭にしかないので対象外
        assertResult(
                "\"abc",
                "\"abc");

        // 末尾にしかないので対象外
        assertResult(
                "abc\"",
                "abc\"");

        // 先頭にしかないので対象外
        assertResult(
                "”あいう",
                "”あいう");

        // 末尾にしかないので対象外
        assertResult(
                "あいう”",
                "あいう”");
    }

    /**
     * 境界値: ダブルクォート1文字・2文字・全角ダブルクォート2文字の境界動作（SW-3）。
     */
    @Test
    public void testBoundaryValues() {
        // Given: 半角ダブルクォート 1文字
        // When: interpret を呼ぶ
        // Then: 前後クォートにならないのでそのまま返ること
        assertResult("\"", "\"");

        // Given: 半角ダブルクォート 2文字
        // When: interpret を呼ぶ
        // Then: 前後クォートで囲まれているので空文字になること
        assertResult("\"\"", "");

        // Given: 全角ダブルクォート 2文字
        // When: interpret を呼ぶ
        // Then: 前後クォートで囲まれているので空文字になること
        assertResult("""", "");
    }

    /**
     * テスト対象の実行結果をアサートする。
     *
     * @param input 入力値
     * @param expected 期待値
     */
    private void assertResult(String input, String expected) {
        InterpretationContext ctx = new InterpretationContext(input, target);
        Assert.assertEquals(expected, ctx.invokeNext());
    }
}
