package nablarch.test.tool.htmlcheck;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.test.tool.htmlcheck.parser.ParseException;

import org.junit.Test;

/**
 * HtmlCheckerのテスト
 * 
 * @author Tomokazu Kagawa
 */
public class HtmlCheckerTest {

    private static final String ROOT_DIR = "src/test/java/nablarch/test/tool/htmlcheck";

    /**
     * 閉じタグが必要な全てのタグに対して、閉じタグ・開始タグが存在しない場合、 指摘を行うことを確認する。
     * 
     * @throws IOException 入出力に関する例外
     */
    @Test
    public void testCanDetectNoStartOrEndTag() throws IOException {

        Html4HtmlChecker htmlChecker = new Html4HtmlChecker(ROOT_DIR + "/conf/empty-html-check-config.csv");
        String testFileRoot = ROOT_DIR + "/html/testCanDetectNoStartOrEndTag/";

        // チェック対象ファイル名と期待するエラーメッセージを保管するファイルを読み込む
        List<String[]> htmlFilesForCheck = null;
        htmlFilesForCheck = arrowSeparator(ROOT_DIR + "/conf/testCanDetectNoStartOrEndTag.txt");

        // 各チェック対象HTMLファイルに対し、チェックを行い、エラーメッセージを比較する。
        for (String[] testCase : htmlFilesForCheck) {
            try {
                htmlChecker.checkHtml(new File(testFileRoot + testCase[0]));

                fail(testCase[0]);

            } catch (InvalidHtmlException e) {
                // testCase[0]はチェック対象HTMLファイル名
                // testCase[1]は期待するエラーメッセージ
                assertEquals(testCase[0], testCase[1], e.getMessage().replace(File.separator, "/"));
                e.printStackTrace();
            }
        }
    }

    /**
     * テキストファイルを読み込み、各行を「 -> 」区切りで読み込む。
     * 
     * @param textPath 読み込み対象CSVファイルパス
     * @return 読み込んだ情報を保有するList<String[]>オブジェクト
     * @throws IOException ファイル入出力中にエラーが生じたことを示す。
     */
    private static List<String[]> arrowSeparator(String textPath) throws IOException {
        List<String[]> row = new ArrayList<String[]>();
        for (String line : readLines(textPath)) {
            row.add(line.split(" -> ", -1));
        }
        return row;
    }
    
    private static List<String> readLines(String filePath) throws IOException {
        BufferedReader inBuffer = null;
        List<String> row = new ArrayList<String>();
        try {
            inBuffer = new BufferedReader(new FileReader(filePath));
            String line = null;
            while ((line = inBuffer.readLine()) != null) {
                row.add(line);
            }
        } finally {
            if (inBuffer != null) {
                inBuffer.close();
            }
        }
        return row;
    }
    
    /**
     * バグ対応を確認するテスト。
     */
    @Test
    public void testCanDetectUsageOfBoldTagInsideLargeSizeHtml() {
        Html4HtmlChecker htmlChecker = new Html4HtmlChecker(ROOT_DIR + "/conf/html-check-config.csv");
        htmlChecker.checkHtml(new File(ROOT_DIR + "/html/testUsers00101Normal_Case1_ユーザID.html"));
    }

    /**
     * HTMLチェックで警告が出ないことを確認するテスト。
     * 
     */
    @Test
    public void testValidHtml() {
        Html4HtmlChecker htmlChecker = new Html4HtmlChecker(ROOT_DIR + "/conf/html-check-config.csv");
        htmlChecker.checkHtml(new File(ROOT_DIR + "/html/validHtml.html"));
    }

    private void testCanUseHtml4(String htmlFilePath, String expectedLogFilePath) throws IOException {
        
        Html4HtmlChecker checker = new Html4HtmlChecker(ROOT_DIR + "/conf/empty-html-check-config.csv");
        
        // 構文エラーが出ないこと。
        try {
            OnMemoryWriter.clearLog();
            checker.checkHtml(new File(htmlFilePath), UTF8);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        
        // 構文を正しくパース出来ていること。
        List<String> actual = OnMemoryWriter.getLog();
        List<String> expected = readLines(expectedLogFilePath);
        assertThat(actual.size(), is(expected.size()));
        for (int i = 0; i < expected.size(); i++) {
            assertThat(String.format("file = [%s], line = [%s]", expectedLogFilePath, i),
                       actual.get(i), is(expected.get(i)));
        }

    }
    
    private static final Charset UTF8 = Charset.forName("UTF-8");
    
    /**
     * HTML4が規定している全てのタグと属性を使用できること。
     */
    @Test
    public void testCanUseAllTagsAndAttributesDefinedByHtml4() throws IOException {
        
        // フレーム関連タグを除いた全てのタグを含むHTMLファイル。
        // 内容がないタグはスラッシュあり。例）<br />
        testCanUseHtml4(ROOT_DIR + "/html/noframe.html",
                ROOT_DIR + "/log/noframe.txt");
        
        // フレーム関連タグを除いた全てのタグを含むHTMLファイル。
        // 内容がないタグはスラッシュなし。例）<br>
        testCanUseHtml4(ROOT_DIR + "/html/noframeWithoutEndTagSlash.html",
                ROOT_DIR + "/log/noframe.txt");
        
        // フレーム関連のタグを含むHTMLファイル。
        // 内容がないタグはスラッシュあり。例）<br />
        testCanUseHtml4(ROOT_DIR + "/html/frame.html",
                ROOT_DIR + "/log/frame.txt");
        
        // フレーム関連のタグを含むHTMLファイル。
        // 内容がないタグはスラッシュなし。例）<br>
        testCanUseHtml4(ROOT_DIR + "/html/frameWithoutEndTagSlash.html",
                ROOT_DIR + "/log/frame.txt");
        
        // フレーム関連タグを除いた全てのタグを含むHTMLファイル。
        // タグの位置をずらすためにフォーマット済み。
        // タグと属性に大文字・小文字を含む。
        testCanUseHtml4(ROOT_DIR + "/html/noframeFormat.html",
                ROOT_DIR + "/log/noframe.txt");
    }
    
    /**
     * boolean属性指定において値を省略できること。
     */
    @Test
    public void testCanAbbreviateBooleanAttributes() throws IOException {
        
        // フレーム関連タグを除いた全てのタグを含むHTMLファイル。
        // boolean属性の値を省略。例）disabled="disabled" -> disabled
        testCanUseHtml4(ROOT_DIR + "/html/noframeAbbrBoolean.html",
                ROOT_DIR + "/log/noframe.txt");
        
        // フレーム関連タグを含むHTMLファイル。
        // boolean属性の値を省略。例）disabled="disabled" -> disabled
        testCanUseHtml4(ROOT_DIR + "/html/frameAbbrBoolean.html",
                ROOT_DIR + "/log/frame.txt");
    }
    
    /**
     * 属性指定においてクォーテーションを省略できないこと。
     */
    @Test
    public void testCanNotAbbreviateDobuleQuotationForAttributes() throws IOException {
        Html4HtmlChecker checker = new Html4HtmlChecker(
                ROOT_DIR + "/conf/empty-html-check-config.csv");
        
        // クォーテーションを省略してない場合
        File manyElementsNoAbbr = new File(ROOT_DIR + "/html/manyElementsNoAbbr.html");
        checker.checkHtml(manyElementsNoAbbr, UTF8);
        
        File noAbbrivation = new File(ROOT_DIR + "/html/noAbbrivation.html");
        checker.checkHtml(noAbbrivation, UTF8);
        
        File oneElementNoAbbr = new File(ROOT_DIR + "/html/oneElementNoAbbr.html");
        checker.checkHtml(oneElementNoAbbr, UTF8);
        
        File twoElementNoAbbr = new File(ROOT_DIR + "/html/twoElementNoAbbr.html");
        checker.checkHtml(twoElementNoAbbr, UTF8);

        // クォーテーションを省略している場合
        checkEveryHtmlHasAbbriviation(checker,
                ROOT_DIR + "/html/quoteabbr",
                ROOT_DIR + "/html/quoteabbr/expectedMessage.txt");

        // 全てのタグ要素に対して、クォーテーションがない場合に指摘することの確認
        checkEveryHtmlHasAbbriviation(
                checker,
                ROOT_DIR + "/html/quoteabbr/alltagelements",
                ROOT_DIR + "/html/quoteabbr/alltagelements/expectedMessages.txt");
    }

    /**
     * クォーテーションを省略しているHTMLをチェックし、指摘を正しく行っていることを確認する。
     * 
     * @param checker HTMLチェックツールインスタンス
     * @param testDataDirPath クォーテーションを省略しているHTMLを含んでいるディレクトリパス
     * @param expectedMessagesFilePath 期待するメッセージを記述しているファイル（ファイル名 + " -> " + 期待するメッセージ）
     * @throws IOException ファイル入出力時例外
     */
    private void checkEveryHtmlHasAbbriviation(Html4HtmlChecker checker, String testDataDirPath, String expectedMessagesFilePath) throws IOException {
        
        File testDataDir = new File(testDataDirPath);
        
        Map<String, String> expectedMessageMap = arrowSeparatorToMap(expectedMessagesFilePath);
        File[] files = testDataDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".html");
            }
        });
        for (File htmlFileData : files) {
            String fileName = htmlFileData.getName();
            try {
                checker.checkHtml(htmlFileData, UTF8);
                fail(fileName);
            } catch (Exception e) {
                Throwable cause = e.getCause().getCause();
                assertTrue(fileName, cause instanceof ParseException);
                assertTrue(fileName, expectedMessageMap.containsKey(fileName));
                assertEquals(fileName, expectedMessageMap.get(fileName), cause.getMessage());
            }
        }
    }
    
    /**
     * テキストファイルを読み込み、各行を「 -> 」区切りで読み込む。
     * 
     * @param textPath 読み込み対象CSVファイルパス
     * @return 読み込んだ情報を保有するList<String[]>オブジェクト
     * @throws IOException ファイル入出力中にエラーが生じたことを示す。
     */
    private static Map<String, String> arrowSeparatorToMap(String textPath) throws IOException {
        Map<String, String> row = new HashMap<String, String>();
        for (String line : readLines(textPath)) {
            String[] rowItems = line.split(" -> ", -1);
            row.put(rowItems[0], rowItems[1]);
        }
        return row;
    }
    
    /**
     * HTML4が規定していないタグを指摘できること。
     */
    @Test
    public void testCanDetectTagNotDefinedByHtml4() {
        
        Html4HtmlChecker htmlChecker = new Html4HtmlChecker(ROOT_DIR + "/conf/html-check-config.csv");
        
        // HTML5のタグを含むHTMLファイル。
        File htmlFile = new File(ROOT_DIR + "/html/tagNotDefinedByHtml4.html");
        
        try {
            htmlChecker.checkHtml(htmlFile, Charset.forName("UTF-8"));
            fail("couldn't detect tag not defined by HTML4.01. file = [" + htmlFile.getAbsolutePath() + "]");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("syntax check failed."));
            assertThat(e.getCause().getMessage(), containsString("Parse error at line 18, column 3."));
        }
    }
    
    /**
     * 文字コードが合わないなど、HTMLタグが見つからない場合に指摘できること。
     */
    @Test
    public void testCanDetectNoTag() {
        
        Html4HtmlChecker htmlChecker = new Html4HtmlChecker(ROOT_DIR + "/conf/html-check-config.csv");
        
        // BOM付きのUTF-8でエンコーディングしたHTMLファイル。
        File htmlFile = new File(ROOT_DIR + "/html/utf8WithBom.html");
        
        try {
            htmlChecker.checkHtml(htmlFile, Charset.forName("UTF-8"));
            fail("couldn't detect no tag. file = [" + htmlFile.getAbsolutePath() + "]");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("syntax check failed."));
            assertThat(e.getCause().getMessage(), containsString("Parse error at line 1, column 1."));
        }
    }
    
    /**
     * デフォルトの設定ファイルに基づいて使用禁止タグ・属性を指摘できること。
     */
    @Test
    public void testCheckHtmlForDefaultSettings() {

        Html4HtmlChecker htmlChecker = new Html4HtmlChecker(ROOT_DIR + "/conf/html-check-config.csv");
        
        // フレーム関連タグを除いた全てのタグを含むHTMLファイル。
        File htmlFile = new File(ROOT_DIR + "/html/noframe.html");
        String actualMessage = "";
        try {
            htmlChecker.checkHtml(htmlFile, Charset.forName("UTF-8"));
            fail("couldn't detect forbidden tag or attribute. file = [" + htmlFile.getAbsolutePath() + "]");
        } catch (InvalidHtmlException e) {
            actualMessage = e.getMessage();
        }
        
        String[] expectedMessages = new String[] {
                "(applet) at line 63 column 1 is forbidden.",
                "(basefont) at line 25 column 1 is forbidden.",
                "(body, alink) at line 15 column 551 is forbidden.",
                "(body, background) at line 15 column 481 is forbidden.",
                "(body, bgcolor) at line 15 column 499 is forbidden.",
                "(body, link) at line 15 column 526 is forbidden.",
                "(body, text) at line 15 column 514 is forbidden.",
                "(body, vlink) at line 15 column 538 is forbidden.",
                "(br, clear) at line 44 column 74 is forbidden.",
                "(caption, align) at line 138 column 15 is forbidden.",
                "(center) at line 19 column 1 is forbidden.",
                "(dir) at line 99 column 1 is forbidden.",
                "(dl, compact) at line 83 column 421 is forbidden.",
                "(font) at line 45 column 1 is forbidden.",
                "(h1, align) at line 71 column 421 is forbidden.",
                "(h2, align) at line 72 column 421 is forbidden.",
                "(h3, align) at line 73 column 421 is forbidden.",
                "(h4, align) at line 74 column 421 is forbidden.",
                "(h5, align) at line 75 column 421 is forbidden.",
                "(h6, align) at line 76 column 421 is forbidden.",
                "(hr, align) at line 68 column 421 is forbidden.",
                "(hr, noshade) at line 69 column 15 is forbidden.",
                "(hr, size) at line 69 column 23 is forbidden.",
                "(hr, width) at line 69 column 35 is forbidden.",
                "(html, version) at line 3 column 37 is forbidden.",
                "(iframe, align) at line 174 column 106 is forbidden.",
                "(img, align) at line 56 column 93 is forbidden.",
                "(img, border) at line 56 column 128 is forbidden.",
                "(img, hspace) at line 56 column 139 is forbidden.",
                "(img, vspace) at line 56 column 151 is forbidden.",
                "(input, align) at line 118 column 29 is forbidden.",
                "(isindex) at line 82 column 1 is forbidden.",
                "(legend, align) at line 113 column 32 is forbidden.",
                "(li, type) at line 91 column 15 is forbidden.",
                "(li, value) at line 91 column 27 is forbidden.",
                "(li, type) at line 96 column 421 is forbidden.",
                "(li, value) at line 96 column 433 is forbidden.",
                "(menu) at line 104 column 1 is forbidden.",
                "(object, align) at line 59 column 199 is forbidden.",
                "(object, border) at line 59 column 235 is forbidden.",
                "(object, hspace) at line 59 column 211 is forbidden.",
                "(object, vspace) at line 59 column 223 is forbidden.",
                "(ol, compact) at line 89 column 37 is forbidden.",
                "(ol, start) at line 89 column 27 is forbidden.",
                "(ol, type) at line 89 column 15 is forbidden.",
                "(p, align) at line 70 column 421 is forbidden.",
                "(pre, width) at line 77 column 421 is forbidden.",
                "(s) at line 26 column 1 is forbidden.",
                "(script, language) at line 10 column 53 is forbidden.",
                "(strike) at line 27 column 1 is forbidden.",
                "(table, align) at line 136 column 113 is forbidden.",
                "(table, bgcolor) at line 136 column 126 is forbidden.",
                "(th, bgcolor) at line 149 column 68 is forbidden.",
                "(th, height) at line 150 column 117 is forbidden.",
                "(th, nowrap) at line 150 column 90 is forbidden.",
                "(tr, bgcolor) at line 147 column 68 is forbidden.",
                "(td, bgcolor) at line 167 column 68 is forbidden.",
                "(td, height) at line 168 column 117 is forbidden.",
                "(td, nowrap) at line 168 column 90 is forbidden.",
                "(th, bgcolor) at line 158 column 68 is forbidden.",
                "(th, height) at line 159 column 117 is forbidden.",
                "(th, nowrap) at line 159 column 90 is forbidden.",
                "(tr, bgcolor) at line 147 column 68 is forbidden.",
                "(tr, bgcolor) at line 156 column 68 is forbidden.",
                "(tr, bgcolor) at line 165 column 68 is forbidden.",
                "(u) at line 28 column 1 is forbidden.",
                "(ul, compact) at line 95 column 27 is forbidden.",
                "(ul, type) at line 95 column 15 is forbidden.",
                "(td, width) at line 168 column 106 is forbidden.",
                "(th, width) at line 159 column 106 is forbidden.",
                "(div, align) at line 18 column 421 is forbidden."
                /*
                 * exclude unknown tags in html-check-config.csv
                 * 
                 *   listing
                 *   plaintext
                 *   xmp
                 *   
                 */
        };
        
        assertThat(actualMessage.split("\n").length, is(expectedMessages.length));
        
        for (String expectedMessage : expectedMessages) {
            assertThat(actualMessage, containsString(expectedMessage));
        }
        
        // フレーム関連タグを除いた全てのタグを含むHTMLファイル。
        // HTML4.01の非推奨タグと属性を削除。
        htmlFile = new File(ROOT_DIR + "/html/noframeWithoutDeprecated.html");
        try {
            htmlChecker.checkHtml(htmlFile, Charset.forName("UTF-8"));
        } catch (InvalidHtmlException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        
        // フレーム関連のタグを含むHTMLファイル。
        htmlFile = new File(ROOT_DIR + "/html/frame.html");
        actualMessage = "";
        try {
            htmlChecker.checkHtml(htmlFile, Charset.forName("UTF-8"));
            fail("couldn't detect forbidden tag or attribute. file = [" + htmlFile.getAbsolutePath() + "]");
        } catch (InvalidHtmlException e) {
            actualMessage = e.getMessage();
        }
        
        expectedMessages = new String[] {
                "(html, version) at line 3 column 37 is forbidden.",
                "(script, language) at line 10 column 53 is forbidden.",
        };
        
        assertThat(actualMessage.split("\n").length, is(expectedMessages.length));
        
        for (String expectedMessage : expectedMessages) {
            assertThat(actualMessage, containsString(expectedMessage));
        }
        
        // フレーム関連のタグを含むHTMLファイル。
        // HTML4.01の非推奨タグと属性を削除。
        htmlFile = new File(ROOT_DIR + "/html/frameWithoutDeprecated.html");
        try {
            htmlChecker.checkHtml(htmlFile, Charset.forName("UTF-8"));
        } catch (InvalidHtmlException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * カスタムの設定ファイルに基づいて使用禁止タグ・属性を指摘できること。
     * HTMLファイルのタグ・属性に対して大文字・小文字を区別せずに指摘できること。
     * 設定ファイルのタグ・属性に対して大文字・小文字を区別せずに指定できること。
     */
    @Test
    public void testCheckHtmlForCustomSettings() {
        
        // カスタムの設定ファイル
        // 大文字・小文字を含む。
        Html4HtmlChecker htmlChecker = new Html4HtmlChecker(ROOT_DIR + "/conf/custom-html-check-config.csv");
        
        // フレーム関連タグを除いた全てのタグを含むHTMLファイル。
        File htmlFile = new File(ROOT_DIR + "/html/noframe.html");
        String actualMessage = "";
        try {
            htmlChecker.checkHtml(htmlFile, Charset.forName("UTF-8"));
            fail("couldn't detect forbidden tag or attribute. file = [" + htmlFile.getAbsolutePath() + "]");
        } catch (InvalidHtmlException e) {
            actualMessage = e.getMessage();
        }
        
        String[] expectedMessages = new String[] {
                "(input, readonly) at line 117 column 72 is forbidden.",
                "(col) at line 141 column 1 is forbidden.",
                "(thead, onclick) at line 144 column 96 is forbidden.",
                "(tbody, align) at line 163 column 15 is forbidden.",
                "(iframe) at line 173 column 1 is forbidden."
        };
        
        assertThat(actualMessage.split("\n").length, is(expectedMessages.length));
        
        for (String expectedMessage : expectedMessages) {
            assertThat(actualMessage, containsString(expectedMessage));
        }
        
        // フレーム関連タグを除いた全てのタグを含むHTMLファイル。
        // 内容がないタグはスラッシュなし。例）<br>
        htmlFile = new File(ROOT_DIR + "/html/noframeWithoutEndTagSlash.html");
        actualMessage = "";
        try {
            htmlChecker.checkHtml(htmlFile, Charset.forName("UTF-8"));
            fail("couldn't detect forbidden tag or attribute. file = [" + htmlFile.getAbsolutePath() + "]");
        } catch (InvalidHtmlException e) {
            actualMessage = e.getMessage();
        }
        
        expectedMessages = new String[] {
                "(input, readonly) at line 117 column 72 is forbidden.",
                "(col) at line 141 column 1 is forbidden.",
                "(thead, onclick) at line 144 column 96 is forbidden.",
                "(tbody, align) at line 163 column 15 is forbidden.",
                "(iframe) at line 173 column 1 is forbidden."
        };
        
        assertThat(actualMessage.split("\n").length, is(expectedMessages.length));
        
        for (String expectedMessage : expectedMessages) {
            assertThat(actualMessage, containsString(expectedMessage));
        }
        
        // フレーム関連タグを除いた全てのタグを含むHTMLファイル。
        // boolean属性の値を省略。例）disabled="disabled" -> disabled
        htmlFile = new File(ROOT_DIR + "/html/noframeAbbrBoolean.html");
        actualMessage = "";
        try {
            htmlChecker.checkHtml(htmlFile, Charset.forName("UTF-8"));
            fail("couldn't detect forbidden tag or attribute. file = [" + htmlFile.getAbsolutePath() + "]");
        } catch (InvalidHtmlException e) {
            actualMessage = e.getMessage();
        }
        
        expectedMessages = new String[] {
                "(input, readonly) at line 117 column 72 is forbidden.",
                "(col) at line 141 column 1 is forbidden.",
                "(thead, onclick) at line 144 column 96 is forbidden.",
                "(tbody, align) at line 163 column 15 is forbidden.",
                "(iframe) at line 173 column 1 is forbidden."
        };
        
        assertThat(actualMessage.split("\n").length, is(expectedMessages.length));
        
        for (String expectedMessage : expectedMessages) {
            assertThat(actualMessage, containsString(expectedMessage));
        }

        // フレーム関連タグを除いた全てのタグを含むHTMLファイル。
        // タグの位置をずらすためにフォーマット済み。
        // 指摘対象のタグと属性に大文字・小文字を含む。
        htmlFile = new File(ROOT_DIR + "/html/noframeFormat.html");
        actualMessage = "";
        try {
            htmlChecker.checkHtml(htmlFile, Charset.forName("UTF-8"));
            fail("couldn't detect forbidden tag or attribute. file = [" + htmlFile.getAbsolutePath() + "]");
        } catch (InvalidHtmlException e) {
            actualMessage = e.getMessage();
        }
        
        // tab文字は8桁でカウント。
        expectedMessages = new String[] {
                "(input, readonly) at line 499 column 9 is forbidden.",
                "(col) at line 580 column 17 is forbidden.",
                "(thead, onclick) at line 590 column 39 is forbidden.",
                "(tbody, align) at line 651 column 47 is forbidden.",
                "(iframe) at line 675 column 1 is forbidden."
        };
        
        assertThat(actualMessage.split("\n").length, is(expectedMessages.length));
        
        for (String expectedMessage : expectedMessages) {
            assertThat(actualMessage, containsString(expectedMessage));
        }
        
        // フレーム関連タグを含むHTMLファイル。
        htmlFile = new File(ROOT_DIR + "/html/frame.html");
        actualMessage = "";
        try {
            htmlChecker.checkHtml(htmlFile, Charset.forName("UTF-8"));
            fail("couldn't detect forbidden tag or attribute. file = [" + htmlFile.getAbsolutePath() + "]");
        } catch (InvalidHtmlException e) {
            actualMessage = e.getMessage();
        }
        
        // tab文字は8桁でカウント。
        expectedMessages = new String[] {
                "(frame, longdesc) at line 18 column 15 is forbidden."
        };
        
        assertThat(actualMessage.split("\n").length, is(expectedMessages.length));
        
        for (String expectedMessage : expectedMessages) {
            assertThat(actualMessage, containsString(expectedMessage));
        }
    }

    /** 空Bodyのタグが許容されること。*/
    @Test
    public void testAllowEmpty() {
        Html4HtmlChecker checker = new Html4HtmlChecker(ROOT_DIR + "/conf/empty-html-check-config.csv");
        File html = new File(ROOT_DIR + "/html", "allow_empty_body/all.html");
        assert html.exists() : html;
        checker.checkHtml(html);  // should succeed
    }
}
