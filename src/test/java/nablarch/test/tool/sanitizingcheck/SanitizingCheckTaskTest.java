package nablarch.test.tool.sanitizingcheck;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/**
 * SanitizingCheckTaskTestのテストクラス
 * 
 * @author Tomokazu Kagawa
 * @see SanitizingCheckTask
 */
public class SanitizingCheckTaskTest extends SanitizingCheckTestSupport {

    /**
     * mainメソッドのテスト
     * 
     * @see SanitizingCheckTask#main(String[])
     */
    @Test
    public void testMain() throws Exception {

        // メソッドの呼び出し確認（additionalextの指定がない場合）
        String[] args = new String[5];
        args[0] = "src/test/java/nablarch/test/tool/sanitizingcheck/testCheckSanitizing/testCheckSanitizing";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualOutput.xml";
        args[2] = "src/test/java/nablarch/test/tool/sanitizingcheck/config.txt";
        args[3] = "UTF-8";
        args[4] = "\\r\\n";
        SanitizingCheckTask.main(args);
        File file = new File("src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualOutput.xml");
        assertTrue(file.exists());

        // メソッドの呼び出し確認（additionalextの指定がある場合）
        assertTrue(file.delete());
        args = new String[6];
        args[0] = "src/test/java/nablarch/test/tool/sanitizingcheck/testCheckSanitizing/testCheckSanitizing";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualOutput.xml";
        args[2] = "src/test/java/nablarch/test/tool/sanitizingcheck/config.txt";
        args[3] = "UTF-8";
        args[4] = "\\r\\n";
        args[5] = "tag";
        SanitizingCheckTask.main(args);
        assertTrue(file.exists());

        // 引数が2の場合
        args = new String[2];
        args[0] = "src/test/java/nablarch/test/tool/sanitizingcheck/testCheckSanitizing/testCheckSanitizing";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualOutput.xml";
        try {
            SanitizingCheckTask.main(args);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(
                    "enter paths of jsp directory, xml and configuration. enter charset and lineseparator.",
                    e.getMessage());
        }

        // 引数が4の場合
        args = new String[4];
        args[0] = "src/test/java/nablarch/test/tool/sanitizingcheck/testCheckSanitizing/testCheckSanitizing";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualOutputArgs4.xml";
        args[2] = "src/test/java/nablarch/test/tool/sanitizingcheck/config.txt";
        args[3] = "UTF-8";
        try {
            SanitizingCheckTask.main(args);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(
                    "enter paths of jsp directory, xml and configuration. enter charset and lineseparator.",
                    e.getMessage());
        }

        // 引数が5の場合
        args = new String[5];
        args[0] = "src/test/java/nablarch/test/tool/sanitizingcheck/testCheckSanitizing/testCheckSanitizing";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualOutputArgs5.xml";
        args[2] = "src/test/java/nablarch/test/tool/sanitizingcheck/config.txt";
        args[3] = "UTF-8";
        args[4] = "\\r\\n";
        SanitizingCheckTask.main(args);
        file = new File("src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualOutputArgs5.xml");
        assertTrue(file.exists());

        // 引数が7の場合
        args = new String[8];
        args[0] = "src/test/java/nablarch/test/tool/sanitizingcheck/testCheckSanitizing/testCheckSanitizing";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualOutput.xml";
        args[2] = "src/test/java/nablarch/test/tool/sanitizingcheck/config.txt";
        args[3] = "UTF-8";
        args[4] = "\\r\\n";
        args[5] = "tag";
        args[6] = "tag";
        args[7] = "tag";
        try {
            SanitizingCheckTask.main(args);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(
                    "enter paths of jsp directory, xml and configuration. enter charset and lineseparator.",
                    e.getMessage());
        }

        // 設定ファイルが存在しない場合
        args = new String[5];
        args[0] = "src/test/java/nablarch/test/tool/sanitizingcheck/testCheckSanitizing/testCheckSanitizing";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualOutput.xml";
        args[2] = "noSuchFile";
        args[3] = "UTF-8";
        args[4] = "\\r\\n";
        File conf = new File(args[2]);
        if (conf.exists()) {
            conf.delete();
        }
        try {
            SanitizingCheckTask.main(args);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("noSuchFile isn't a file.", e.getMessage());
        }

        // チェック対象JSPが存在しない場合
        args = new String[5];
        args[0] = "noSuchDir";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualOutput.xml";
        args[2] = "src/test/java/nablarch/test/tool/sanitizingcheck/config.txt";
        args[3] = "UTF-8";
        args[4] = "\\r\\n";
        try {
            SanitizingCheckTask.main(args);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("confirm the existence of noSuchDir.", e.getMessage());
        }

        // 使用できない文字コードを指定した場合
        args = new String[5];
        args[0] = "src/test/java/nablarch/test/tool/sanitizingcheck/all.jsp";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualOutputForAllJspTag.xml";
        args[2] = "src/test/java/nablarch/test/tool/sanitizingcheck/config.txt";
        args[3] = "test";
        args[4] = "\\r\\n";
        try {
            SanitizingCheckTask.main(args);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("confirm the charset test.", e.getMessage());
        }

        // 全てのタグに対して指摘を行えることの確認
        args = new String[6];
        args[0] = "src/test/java/nablarch/test/tool/sanitizingcheck/all.jsp";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualOutputForAllJspTag.xml";
        args[2] = "src/test/java/nablarch/test/tool/sanitizingcheck/config.txt";
        args[3] = "UTF-8";
        args[4] = "\\r\\n";
        args[5] = "tag,fragment";
        SanitizingCheckTask.main(args);
        file = new File("src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualOutputForAllJspTag.xml");
        assertTrue(file.exists());
        assertThat(readFileToString(file.getAbsolutePath()), is(containsString("all.jsp")));

        // 除外設定が有効であることの確認
        args = new String[7];
        args[0] = "src/test/java/nablarch/test/tool/sanitizingcheck/all.jsp";
        args[1] = "src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualOutputForAllJspTag.xml";
        args[2] = "src/test/java/nablarch/test/tool/sanitizingcheck/config.txt";
        args[3] = "UTF-8";
        args[4] = "\\r\\n";
        args[5] = "tag,fragment";
        args[6] = "sanitizingcheck(\\\\|/)all\\.jsp";
        SanitizingCheckTask.main(args);
        file = new File("src/test/java/nablarch/test/tool/sanitizingcheck/actual/actualOutputForAllJspTag.xml");
        assertTrue(file.exists());
        assertThat(readFileToString(file.getAbsolutePath()), is(not(containsString("all.jsp"))));

    }

    @Test
    public void testSplitComma() throws Exception {
        assertThat(SanitizingCheckTask.splitComma(null), is(Collections.<String>emptyList()));
        assertThat(SanitizingCheckTask.splitComma(""), is(Collections.<String>emptyList()));
        assertThat(SanitizingCheckTask.splitComma("tag"), is(Arrays.asList("tag")));
        assertThat(SanitizingCheckTask.splitComma("tag,"), is(Arrays.asList("tag")));
        assertThat(SanitizingCheckTask.splitComma("tag,inc"), is(Arrays.asList("tag","inc")));
        assertThat(SanitizingCheckTask.splitComma("tag,,fragment"), is(Arrays.asList("tag", "", "fragment")));
    }
}
