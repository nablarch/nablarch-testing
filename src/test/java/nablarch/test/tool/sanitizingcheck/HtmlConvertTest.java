package nablarch.test.tool.sanitizingcheck;


import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * HtmlConvertテスト
 * 
 * @author Tomokazu Kagawa
 */
public class HtmlConvertTest extends SanitizingCheckTestSupport {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    /**
     * mainメソッドテスト
     */
    @Test
    public void testMainErrorCase() throws IOException {

        // 引数が2の場合
        String[] args = new String[2];
        args[0] = "java/nablarch/test/tool/sanitizingcheck/testHtmlConvert.xml";
        args[1] = "java/nablarch/test/tool/sanitizingcheck/TransformToHTML.xsl";
        try {
            HtmlConvert.main(args);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("enter paths of xml, xslt and html.", e.getMessage());
        }
        
        // 引数が4の場合
        args = new String[4];
        args[0] = "src/test/java/nablarch/test/tool/sanitizingcheck/testHtmlConvert.xml";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/TransformToHTML.xsl";
        args[2] = "src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualHtml.html";
        args[3] = "test";
        try {
            HtmlConvert.main(args);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("enter paths of xml, xslt and html.", e.getMessage());
        }
        
        // xsltファイルが存在しない場合
        args = new String[3];
        args[0] = "src/test/java/nablarch/test/tool/sanitizingcheck/testHtmlConvert.xml";
        args[1] = "noFile";
        args[2] = "src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualHtml.html";
        try {
            HtmlConvert.main(args);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Xslt file doesn't exist.", e.getMessage());
        }
        
        // xmlファイルが存在しない場合
        args = new String[3];
        args[0] = "nofile";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/TransformToHTML.xsl";
        args[2] = "src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualHtml.html";
        try {
            HtmlConvert.main(args);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Check result file doesn't exist.", e.getMessage());
        }
    }

    /**
     * 正常系のテスト(Java6以前用)。
     *
     * 出力されされるHTMLが変換結果が、jdk1.8_151以前と以後で異なるため場合わけしている。<br />
     * テスト環境で、jdk1.8_151以前に該当するのはJava6の環境なので、この場合分けとしている。
     * @throws IOException
     */
    @Test
    public void testMainSuccessCaseBeforeJava6() throws IOException {
        Assume.assumeTrue(new BigDecimal(System.getProperty("java.specification.version")).compareTo(new BigDecimal("1.6")) <= 0);

        // 引数が3かつ、引数が適切な場合。
        String[] args = new String[3];
        args[0] = "src/test/java/nablarch/test/tool/sanitizingcheck/testHtmlConvert.xml";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/TransformToHTML.xsl";
        args[2] = tmpDir.newFile().getAbsolutePath();
        HtmlConvert.main(args);
        assertFile("src/test/java/nablarch/test/tool/sanitizingcheck/expected/expectedHtmlBeforeJava6.html", args[2]);
    }

    /**
     * 正常系のテスト(Java8用)。
     *
     * 出力されされるHTMLが変換結果が、jdk1.8_152以降とJava11で異なるため場合わけしている。
     *
     * @throws IOException
     */
    @Test
    public void testMainSuccessCaseJava8() throws IOException {
        Assume.assumeTrue(new BigDecimal(System.getProperty("java.specification.version")).compareTo(new BigDecimal("1.8")) == 0);

        // 引数が3かつ、引数が適切な場合。
        String[] args = new String[3];
        args[0] = "src/test/java/nablarch/test/tool/sanitizingcheck/testHtmlConvert.xml";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/TransformToHTML.xsl";
        args[2] = tmpDir.newFile().getAbsolutePath();
        HtmlConvert.main(args);
        assertFile("src/test/java/nablarch/test/tool/sanitizingcheck/expected/expectedHtmlJava8.html", args[2]);
    }

    /**
     * 正常系のテスト(Java11以降用)。
     *
     * @throws IOException
     */
    @Test
    public void testMainSuccessCaseAfterJava11() throws IOException {
        Assume.assumeTrue(new BigDecimal(System.getProperty("java.specification.version")).compareTo(new BigDecimal("11")) >= 0);

        // 引数が3かつ、引数が適切な場合。
        String[] args = new String[3];
        args[0] = "src/test/java/nablarch/test/tool/sanitizingcheck/testHtmlConvert.xml";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/TransformToHTML.xsl";
        args[2] = tmpDir.newFile().getAbsolutePath();
        HtmlConvert.main(args);
        assertFile("src/test/java/nablarch/test/tool/sanitizingcheck/expected/expectedHtmlAfterJava11.html", args[2]);
    }

}
