package nablarch.test.tool.htmlcheck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.Charset;

import nablarch.test.tool.htmlcheck.HtmlSyntaxChecker;
import nablarch.test.tool.htmlcheck.InvalidHtmlException;

import org.junit.Test;

/**
 * HtmlGrammarCheckerのテストクラス<br>
 * HtmlCheckerTestクラスにてテストしきれないケースの確認を行う。
 * 
 * @author Tomokazu Kagawa
 */
public class HtmlSyntaxCheckerTest {

    /**
     * checkメソッドのテスト<br>
     * 指定したHtmlファイルが存在しない場合
     * 
     * @see HtmlSyntaxChecker#check(File)
     */
    @Test
    public void testCheckFileNotFound() {
        try {
            HtmlSyntaxChecker syntaxChecker = new HtmlSyntaxChecker();
            syntaxChecker.check(new File("notFound.html"), Charset.forName("UTF-8"));
        } catch (IllegalArgumentException e) {
            return;
        }
        fail();
    }

    /**
     * checkメソッドのテスト<br>
     * TokenMgrError発生時
     * 
     * @see HtmlSyntaxChecker#check(File)
     */
    @Test
    public void testCheckTokenMgrError() {

        String htmlPath = "java/nablarch/test/tool/htmlcheck/html/testCheckTokenMgrError.html";
        try {

            HtmlSyntaxChecker syntaxChecker = new HtmlSyntaxChecker();
            syntaxChecker.check(new File(htmlPath), Charset.forName("UTF-8"));
            fail();
        } catch (InvalidHtmlException e) {
            assertEquals((new InvalidHtmlException("")).getClass(), e.getClass());
        }
    }

}
