package nablarch.test.core.reader;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * {@link YamlTestDataReader} のテスト。
 * RS-01〜RS-08 の仕様を検証する。
 */
public class YamlTestDataReaderTest {

    private static final String DIR =
            new File("src/test/java/nablarch/test/core/reader/").getAbsolutePath();

    private final YamlTestDataReader sut = new YamlTestDataReader();

    @After
    public void tearDown() {
        sut.close();
    }

    // -------------------------------------------------------------------
    // RS-01: open(path, dataName) は {path}/{dataName}.yaml を開く
    // -------------------------------------------------------------------

    @Test
    public void open_loadsYamlFile() {
        sut.open(DIR, "YamlTestDataReaderTestData");
        assertThat(sut.readLine(), is(notNullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void open_nullDataName_throwsException() {
        sut.open(DIR, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void open_emptyDataName_throwsException() {
        sut.open(DIR, "");
    }

    @Test(expected = RuntimeException.class)
    public void open_fileNotFound_throwsException() {
        sut.open(DIR, "NoSuchFile");
    }

    // -------------------------------------------------------------------
    // RS-02: readLine() は文書終端で null を返す
    // -------------------------------------------------------------------

    @Test
    public void readLine_returnsNullAtEof() {
        sut.open(DIR, "YamlTestDataReaderTestData");
        while (sut.readLine() != null) {
            // drain
        }
        assertThat("EOFの次も null", sut.readLine(), is(nullValue()));
    }

    // -------------------------------------------------------------------
    // RS-03: YAML ネイティブ null → 文字列 "null"
    // RS-04: YAML ネイティブ true/false → "true"/"false"
    // RS-05: YAML ネイティブ integer/float → 数字文字列
    // -------------------------------------------------------------------

    @Test
    public void readLine_convertsNativeTypes() {
        sut.open(DIR, "YamlNativeTypesTestData");

        // セクションヘッダ: ["SETUP_TABLE=NATIVE_TYPES"]
        List<String> sectionHeader = sut.readLine();
        assertThat(sectionHeader.get(0), is("SETUP_TABLE=NATIVE_TYPES"));

        // カラムヘッダ行: ["", "COL_NULL", "COL_BOOL_TRUE", "COL_BOOL_FALSE", "COL_INT", "COL_FLOAT", "COL_STRING"]
        List<String> colHeader = sut.readLine();
        assertThat("先頭セルは空", colHeader.get(0), is(""));

        // データ行: ["", <各値>...]
        List<String> dataRow = sut.readLine();
        assertThat("先頭セルは空", dataRow.get(0), is(""));

        int nullIdx     = colHeader.indexOf("COL_NULL");
        int trueIdx     = colHeader.indexOf("COL_BOOL_TRUE");
        int falseIdx    = colHeader.indexOf("COL_BOOL_FALSE");
        int intIdx      = colHeader.indexOf("COL_INT");
        int floatIdx    = colHeader.indexOf("COL_FLOAT");
        int floatSciIdx = colHeader.indexOf("COL_FLOAT_SCI");
        int strIdx      = colHeader.indexOf("COL_STRING");

        assertThat("null → \"null\"",          dataRow.get(nullIdx),     is("null"));    // RS-03
        assertThat("true → \"true\"",          dataRow.get(trueIdx),     is("true"));    // RS-04
        assertThat("false → \"false\"",        dataRow.get(falseIdx),    is("false"));   // RS-04
        assertThat("int → \"42\"",             dataRow.get(intIdx),      is("42"));      // RS-05
        assertThat("float → \"3.14\"",         dataRow.get(floatIdx),    is("3.14"));    // RS-05
        assertThat("科学表記 float → \"1.0E10\"", dataRow.get(floatSciIdx), is("1.0E10")); // RS-05 境界値
        assertThat("string → \"hello\"",       dataRow.get(strIdx),      is("hello"));
    }

    // -------------------------------------------------------------------
    // RS-06: 末尾の空要素は "" で補完する
    // -------------------------------------------------------------------

    /**
     * 2行目で COL_C が省略されているとき、COL_C の位置が "" で補完されること。
     * YamlTrailingNullTestData:
     *   row1: COL_A="val_a",  COL_B="val_b",  COL_C="val_c"
     *   row2: COL_A="val_a2", COL_B="val_b2"  ← COL_C 省略
     */
    @Test
    public void readLine_trailingNullPaddedWithEmpty() {
        sut.open(DIR, "YamlTrailingNullTestData");

        // セクションヘッダ
        sut.readLine();
        // カラムヘッダ
        List<String> colHeader = sut.readLine();
        int colCount = colHeader.size();

        // 1行目: 全列あり
        List<String> row1 = sut.readLine();
        assertThat("1行目の列数", row1.size(), is(colCount));

        // 2行目: COL_C 省略（末尾省略）→ "" で補完される
        List<String> row2 = sut.readLine();
        assertThat("2行目の列数がヘッダと同じであること", row2.size(), is(colCount));
        int colCIdx = colHeader.indexOf("COL_C");
        assertThat("省略された末尾列は空文字", row2.get(colCIdx), is(""));  // RS-06

        // 3行目: COL_B 省略（中間省略）→ "" で補完される
        List<String> row3 = sut.readLine();
        assertThat("3行目の列数がヘッダと同じであること", row3.size(), is(colCount));
        int colAIdx = colHeader.indexOf("COL_A");
        int colBIdx = colHeader.indexOf("COL_B");
        assertThat("中間省略列は空文字", row3.get(colBIdx), is(""));         // RS-06
        assertThat("中間省略以外の列は正しく取得", row3.get(colAIdx), is("val_a3"));
        assertThat("中間省略以外の列は正しく取得", row3.get(colCIdx), is("val_c3"));
    }

    // -------------------------------------------------------------------
    // RS-07: readLine() が null を返した後、直前セクションデータが欠落しない
    // -------------------------------------------------------------------

    /**
     * ファイル末尾にあるセクションのデータが欠落しないことを確認する。
     * YamlTestDataReaderTestData の最後のセクションは setup_files の値行。
     * 全行ドレインして最後に得た行がデータ行（先頭セルが空）であること。
     */
    @Test
    public void readLine_lastSectionNotLost() {
        sut.open(DIR, "YamlTestDataReaderTestData");

        List<String> lastLine = null;
        List<String> line;
        while ((line = sut.readLine()) != null) {
            lastLine = line;
        }

        assertThat("最終行が存在すること", lastLine, is(notNullValue()));
        // setup_files の最後の値行: ["", "002", "鈴木花子"]
        assertThat("最終行の列数", lastLine.size(), is(3));
        assertThat("最終行の先頭セルが空（データ行）", lastLine.get(0), is(""));
        assertThat("最終値行の1列目（USER_ID）", lastLine.get(1), is("002"));
        assertThat("最終値行の2列目（USER_NAME）", lastLine.get(2), is("鈴木花子"));
    }

    // -------------------------------------------------------------------
    // RS-08: isResourceExisting / isDataExisting
    // -------------------------------------------------------------------

    @Test
    public void isResourceExisting_fileExists_returnsTrue() {
        assertThat(sut.isResourceExisting(DIR, "YamlTestDataReaderTestData"), is(true));
    }

    @Test
    public void isResourceExisting_fileNotExists_returnsFalse() {
        assertThat(sut.isResourceExisting(DIR, "NoSuchFile"), is(false));
    }

    @Test
    public void isResourceExisting_dirNotExists_returnsFalse() {
        assertThat(sut.isResourceExisting("no/such/dir", "YamlTestDataReaderTestData"), is(false));
    }

    @Test
    public void isDataExisting_fileExists_returnsTrue() {
        assertThat(sut.isDataExisting(DIR, "YamlTestDataReaderTestData"), is(true));
    }

    @Test
    public void isDataExisting_fileNotExists_returnsFalse() {
        assertThat(sut.isDataExisting(DIR, "NoSuchFile"), is(false));
    }

    // -------------------------------------------------------------------
    // 行シーケンス確認: setup_tables（グループIDなし）
    // -------------------------------------------------------------------

    @Test
    public void rowSequence_setupTable_noGroupId() {
        sut.open(DIR, "YamlTestDataReaderTestData");

        // セクションヘッダ
        List<String> sectionHeader = sut.readLine();
        assertThat(sectionHeader.get(0), is("SETUP_TABLE=USER"));

        // カラムヘッダ: 先頭セルは空
        List<String> colHeader = sut.readLine();
        assertThat("先頭セルは空", colHeader.get(0), is(""));
        assertThat(colHeader, hasItem("USER_ID"));
        assertThat(colHeader, hasItem("USER_NAME"));

        // データ行: 先頭セルは空
        List<String> dataRow = sut.readLine();
        assertThat("先頭セルは空", dataRow.get(0), is(""));
        assertThat("USER_ID値", dataRow.get(colHeader.indexOf("USER_ID")), is("001"));
        assertThat("MEMO(null)値", dataRow.get(colHeader.indexOf("MEMO")), is("null")); // RS-03
    }

    // -------------------------------------------------------------------
    // 行シーケンス確認: setup_tables（グループIDあり）
    // -------------------------------------------------------------------

    @Test
    public void rowSequence_setupTable_withGroupId() {
        sut.open(DIR, "YamlTestDataReaderTestData");

        // 1つ目のセクション（groupId なし）: ヘッダ + カラムヘッダ + 1データ行
        sut.readLine(); // SETUP_TABLE=USER
        sut.readLine(); // col header
        sut.readLine(); // data row

        // 2つ目のセクション: group_id=case1
        List<String> sectionHeader = sut.readLine();
        assertThat(sectionHeader.get(0), is("SETUP_TABLE[case1]=ORDER"));
    }

    // -------------------------------------------------------------------
    // 行シーケンス確認: list_maps
    // -------------------------------------------------------------------

    @Test
    public void rowSequence_listMap() {
        sut.open(DIR, "YamlTestDataReaderTestData");

        // setup_tables 2セクション（各3行）を読み飛ばす
        for (int i = 0; i < 6; i++) { sut.readLine(); }

        // LIST_MAP セクションヘッダ
        List<String> sectionHeader = sut.readLine();
        assertThat(sectionHeader.get(0), is("LIST_MAP=params"));

        // カラムヘッダ
        List<String> colHeader = sut.readLine();
        assertThat("先頭セルは空", colHeader.get(0), is(""));
        assertThat(colHeader, hasItem("KEY1"));

        // データ行1
        List<String> row1 = sut.readLine();
        assertThat(row1.get(colHeader.indexOf("KEY1")), is("val1"));

        // データ行2
        List<String> row2 = sut.readLine();
        assertThat(row2.get(colHeader.indexOf("KEY1")), is("val3"));
    }

    // -------------------------------------------------------------------
    // 行シーケンス確認: setup_files（固定長）
    // -------------------------------------------------------------------

    @Test
    public void rowSequence_setupFiles_fixed() {
        sut.open(DIR, "YamlTestDataReaderTestData");

        // setup_tables 2セクション（各3行） + list_maps 1セクション（4行）を読み飛ばす
        for (int i = 0; i < 10; i++) { sut.readLine(); }

        // SETUP_FIXED セクションヘッダ
        List<String> sectionHeader = sut.readLine();
        assertThat(sectionHeader.get(0), is("SETUP_FIXED=input/data.dat"));

        // ディレクティブ行
        List<String> directive = sut.readLine();
        assertThat(directive.get(0), is("text-encoding"));
        assertThat(directive.get(1), is("MS932"));

        // フィールド名行: [recordType, field1, field2]
        List<String> names = sut.readLine();
        assertThat(names.get(0), is("DATA"));
        assertThat(names.get(1), is("USER_ID"));
        assertThat(names.get(2), is("USER_NAME"));

        // 型行: ["", "X", "N"]
        List<String> types = sut.readLine();
        assertThat(types.get(0), is(""));
        assertThat(types.get(1), is("X"));
        assertThat(types.get(2), is("N"));

        // 長さ行: ["", "10", "20"]
        List<String> lengths = sut.readLine();
        assertThat(lengths.get(0), is(""));
        assertThat(lengths.get(1), is("10"));
        assertThat(lengths.get(2), is("20"));

        // 値行1
        List<String> values1 = sut.readLine();
        assertThat(values1.get(0), is(""));
        assertThat(values1.get(1), is("001"));
        assertThat(values1.get(2), is("山田太郎"));

        // 値行2
        List<String> values2 = sut.readLine();
        assertThat(values2.get(1), is("002"));
        assertThat(values2.get(2), is("鈴木花子"));
    }
}
