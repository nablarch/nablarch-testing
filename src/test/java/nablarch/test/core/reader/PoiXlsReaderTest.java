package nablarch.test.core.reader;

import nablarch.test.Trap;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * XlsReaderForPoiのテストクラス。
 *
 * @author Hisaaki Sioiri
 */
public class PoiXlsReaderTest {

    private PoiXlsReader target = new PoiXlsReader();

    @After
    public void tearDown() {
        target.close();
    }

    private static String xlsFileDir = new File("src/test/java/nablarch/test/core/reader/").getAbsolutePath();

    /**
     * XlsReaderForPoiのテスト。<br>
     * Excelから正しくデータを読み込めることを確認
     */
    @Test
    public void read1() {
        target.open(xlsFileDir, "PoiXlsReaderTestData/EXCEL_TEST_DATA");
        assertReadData();
    }

    public void assertReadData() {
        // 1行目のデータ比較
        List<String> actual1 = target.readLine();
        List<String> expected1 = new ArrayList<String>();
        expected1.add("1");
        expected1.add("2");
        expected1.add("3");
        expected1.add("4");
        expected1.add("5");
        assertThat(actual1, is(expected1));

        // 2行目のデータ
        List<String> actual2 = target.readLine();
        List<String> expected2 = new ArrayList<String>();
        expected2.add("2010/8/27");
        expected2.add("16:37");
        expected2.add("2010/8/27 16:37");
        assertThat(actual2, is(expected2));

        // 3行目のデータ
        List<String> actual3 = target.readLine();
        List<String> expected3 = new ArrayList<String>();
        expected3.add("ABC");
        expected3.add("D");
        expected3.add("EF");
        expected3.add("G");
        assertThat(actual3, is(expected3));

        // 4行目のデータ
        List<String> actual4 = target.readLine();
        List<String> expected4 = new ArrayList<String>();
        expected4.add("あいうえお");
        expected4.add("かきくけこ");
        assertThat(actual4, is(expected4));

        // 5行目のデータ
        List<String> actual5 = target.readLine();
        List<String> expected5 = new ArrayList<String>();
        expected5.add("TRUE");
        expected5.add("FALSE");
        assertThat(actual5, is(expected5));

        // 6行目は存在しないのでnull
        assertThat(target.readLine(), nullValue());
    }

    /** テストデータ名がnullの場合、例外が発生すること。 */
    @Test(expected = IllegalArgumentException.class)
    public void testOpenWithDataNameNull() {
        target.open("path", null);
    }

    /** テストデータ名が空文字の場合、例外が発生すること。 */
    @Test(expected = IllegalArgumentException.class)
    public void testOpenWithDataNameEmpty() {
        target.open("path", "");
    }

    /** テストデータ名の形式が不正な場合、例外が発生すること。 */
    @Test(expected = IllegalArgumentException.class)
    public void testOpenWithInvalidDataName() {
        // データ名は、ファイル名/シート名でなければならない
        target.open("path", "hoge/");
    }


    @Test
    public void testOpenXlsxFile() {
        target.open(xlsFileDir, "PoiXlsReaderXLSXTestData/EXCEL_TEST_DATA");

        assertReadData();

    }

    @Test
    public void testIsResourceExisting() {

        // リソース名が / 終わりなら、 / を除去して処理
        assertThat(target.isResourceExisting("src/test/java/nablarch/test", "TestSupportTest/SetUpDb"), is(true));

        // xlsx ファイル
        assertThat(target.isResourceExisting("src/test/java/nablarch/test/core/reader/", "PoiXlsReaderXLSXTestData/SetUpDb"),
                   is(true));

        // ディレクトリが存在しない場合
        assertThat(target.isResourceExisting("xxx/yyy", "TestSupportTest/SetUpDb"), is(false));

        // ファイルが存在しない場合
        assertThat(target.isResourceExisting("src/test/java/nablarch/test/core/reader/", "NoSuchFile/SetUpDb"), is(false));

    }

    @Test
    public void testIsDataExisting() {

        // xls ファイル
        assertThat(target.isDataExisting("src/test/java/nablarch/test/core/reader/", "PoiXlsReaderTestData/EXCEL_TEST_DATA"),
                is(true));

        // xlsx ファイル
        assertThat(target.isDataExisting("src/test/java/nablarch/test/core/reader/", "PoiXlsReaderXLSXTestData/EXCEL_TEST_DATA"),
                is(true));

        // ディレクトリが存在しない場合
        assertThat(target.isDataExisting("xxx/yyy", "TestSupportTest/SetUpDb"), is(false));

        // ファイルが存在しない場合
        assertThat(target.isDataExisting("src/test/java/nablarch/test/core/reader/", "NoSuchFile/SetUpDb"), is(false));

        // シートが存在しない場合
        assertThat(target.isDataExisting("src/test/java/nablarch/test/core/reader/", "PoiXlsReaderTestData/SetUpDb"), is(false));
    }

    /**
     * 完全な空行を含むシートを読む場合、空行が読み込まれないこと。
     */
    @Test
    public void testEmptyRow() {
        target.open(xlsFileDir, "PoiXlsReaderTestData/EMPTY_ROW");
        {
            // 1行目のデータ比較
            List<String> actual = target.readLine();
            List<String> expected = Arrays.asList(
                    "1",
                    "2",
                    "3",
                    "4",
                    "5");
            assertThat(actual, is(expected));
        }
        // 2行目は読み込まれない

        // 3行目のデータ
        {
            List<String> actual = target.readLine();
            List<String> expected = Arrays.asList(
                    "あいうえお",
                    "かきくけこ"
                    );
            assertThat(actual, is(expected));
        }

        // 読み込み終了
        assertThat(target.readLine(), is(nullValue()));
    }


    @Test
    public void testBookNotFound() {
        new Trap("存在しないファイルをオープンしようとする場合、例外が発生すること。") {
            @Override
            protected void shouldFail() throws Exception {
                target.open(xlsFileDir, "NO_SUCH_BOOK/NO_SUCH_SHEET");
            }
        }.capture(RuntimeException.class)
         .whichMessageIs("test data file open failed.")
         .whichCauseIs(IllegalArgumentException.class);
    }

    /** 指定したシートが存在しない場合、例外が発生すること。*/
    @Test(expected = IllegalArgumentException.class)
    public void testSheetNotFound() {

        target.open(xlsFileDir, "PoiXlsReaderTestData/NO_SUCH_SHEET");
    }
}
