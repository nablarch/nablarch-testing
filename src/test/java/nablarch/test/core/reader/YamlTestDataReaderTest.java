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
 * 仕様ID参照: {@code docs/ntf-impl-spec-list.md}
 */
public class YamlTestDataReaderTest {

    /** テストデータの配置先（src/test/resources 以下） */
    private static final String DIR =
            new File("src/test/resources/nablarch/test/core/reader/").getAbsolutePath();

    private final YamlTestDataReader sut = new YamlTestDataReader();

    @After
    public void tearDown() {
        sut.close();
    }

    // -------------------------------------------------------------------
    // RS-01: open(path, dataName) は {path}/{dataName}.yaml を開く
    // -------------------------------------------------------------------

    /**
     * Given: 有効なパスとデータ名
     * When:  open を呼び出す
     * Then:  readLine() が "SETUP_TABLE=USER" を先頭セルに持つ行を返す（ファイルがロードされていること）（RS-01）
     */
    @Test
    public void open_loadsYamlFile() {
        // Given / When
        sut.open(DIR, "YamlTestDataReaderTestData");
        // Then: 先頭行がセクションヘッダ "SETUP_TABLE=USER" であることを確認（RS-01）
        List<String> firstRow = sut.readLine();
        assertThat("先頭行が存在すること", firstRow, is(notNullValue()));
        assertThat("先頭行がセクションヘッダであること", firstRow.get(0), is("SETUP_TABLE=USER"));  // RS-01
    }

    /**
     * Given: dataName が null
     * When:  open を呼び出す
     * Then:  IllegalArgumentException がスローされる（RS-01）
     */
    @Test(expected = IllegalArgumentException.class)
    public void open_nullDataName_throwsException() {
        // Given / When / Then
        sut.open(DIR, null);  // RS-01
    }

    /**
     * Given: dataName が空文字列
     * When:  open を呼び出す
     * Then:  IllegalArgumentException がスローされる（RS-01）
     */
    @Test(expected = IllegalArgumentException.class)
    public void open_emptyDataName_throwsException() {
        // Given / When / Then
        sut.open(DIR, "");  // RS-01
    }

    /**
     * Given: path が null
     * When:  open を呼び出す
     * Then:  IllegalArgumentException がスローされる（RS-01）
     */
    @Test(expected = IllegalArgumentException.class)
    public void open_nullPath_throwsException() {
        // Given / When / Then
        sut.open(null, "YamlTestDataReaderTestData");  // RS-01
    }

    /**
     * Given: 存在しないファイル名
     * When:  open を呼び出す
     * Then:  RuntimeException がスローされる（RS-01）
     */
    @Test(expected = RuntimeException.class)
    public void open_fileNotFound_throwsException() {
        // Given / When / Then
        sut.open(DIR, "NoSuchFile");  // RS-01
    }

    /**
     * Given: 1回 open した後、別のファイルを open する
     * When:  2回目の open を呼び出す
     * Then:  最初のファイルのデータは破棄され、2回目のファイルのデータから読み込まれる（RS-01）
     */
    @Test
    public void open_reopenWithDifferentFile_resetsToPreviousData() {
        // Given: 1回目の open
        sut.open(DIR, "YamlNativeTypesTestData");
        List<String> firstFileHeader = sut.readLine();
        assertThat(firstFileHeader.get(0), is("SETUP_TABLE=NATIVE_TYPES"));

        // When: 2回目の open（別ファイル）
        sut.open(DIR, "YamlTestDataReaderTestData");

        // Then: 2回目のファイルの先頭から読み込まれること（RS-01）
        List<String> secondFileHeader = sut.readLine();
        assertThat("再open後はリセットされること", secondFileHeader.get(0), is("SETUP_TABLE=USER"));  // RS-01
    }

    // -------------------------------------------------------------------
    // RS-02: readLine() は文書終端で null を返す
    // -------------------------------------------------------------------

    /**
     * Given: ファイルを開いて全行を読み切った後
     * When:  readLine() を呼び出す
     * Then:  null を返す（RS-02）
     */
    @Test
    public void readLine_returnsNullAtEof() {
        // Given
        sut.open(DIR, "YamlTestDataReaderTestData");
        while (sut.readLine() != null) {
            // drain
        }
        // When / Then
        assertThat("EOFの次も null", sut.readLine(), is(nullValue()));  // RS-02
    }

    /**
     * Given: close() 後
     * When:  readLine() を呼び出す
     * Then:  null を返す（RS-02）
     */
    @Test
    public void readLine_afterClose_returnsNull() {
        // Given
        sut.open(DIR, "YamlTestDataReaderTestData");
        sut.close();

        // When / Then: close 後は null
        assertThat("close後はnullを返すこと", sut.readLine(), is(nullValue()));  // RS-02
    }

    // -------------------------------------------------------------------
    // RS-03: YAML ネイティブ null → 文字列 "null"
    // RS-04: YAML ネイティブ true/false → "true"/"false"
    // RS-05: YAML ネイティブ integer/float → 数字文字列
    // -------------------------------------------------------------------

    /**
     * Given: YAML ネイティブ型（null/boolean/integer/float/科学表記）を含むテストデータ
     * When:  readLine() で各データ行を読み込む
     * Then:  各値が仕様どおりに文字列変換される（RS-03/RS-04/RS-05）
     */
    @Test
    public void readLine_convertsNativeTypes() {
        // Given
        sut.open(DIR, "YamlNativeTypesTestData");

        // When: セクションヘッダ
        List<String> sectionHeader = sut.readLine();
        assertThat(sectionHeader.get(0), is("SETUP_TABLE=NATIVE_TYPES"));

        // カラムヘッダ行
        List<String> colHeader = sut.readLine();
        assertThat("先頭セルは空", colHeader.get(0), is(""));

        // Then: データ行の各値を検証
        List<String> dataRow = sut.readLine();
        assertThat("先頭セルは空", dataRow.get(0), is(""));

        int nullIdx     = colHeader.indexOf("COL_NULL");
        int trueIdx     = colHeader.indexOf("COL_BOOL_TRUE");
        int falseIdx    = colHeader.indexOf("COL_BOOL_FALSE");
        int intIdx      = colHeader.indexOf("COL_INT");
        int floatIdx    = colHeader.indexOf("COL_FLOAT");
        int floatSciIdx = colHeader.indexOf("COL_FLOAT_SCI");
        int strIdx      = colHeader.indexOf("COL_STRING");

        assertThat("null → \"null\"",             dataRow.get(nullIdx),     is("null"));    // RS-03
        assertThat("true → \"true\"",             dataRow.get(trueIdx),     is("true"));    // RS-04
        assertThat("false → \"false\"",           dataRow.get(falseIdx),    is("false"));   // RS-04
        assertThat("int → \"42\"",                dataRow.get(intIdx),      is("42"));      // RS-05
        assertThat("float → \"3.14\"",            dataRow.get(floatIdx),    is("3.14"));    // RS-05
        assertThat("科学表記 float → \"1.0E10\"", dataRow.get(floatSciIdx), is("1.0E10")); // RS-05 境界値
        assertThat("string → \"hello\"",          dataRow.get(strIdx),      is("hello"));
    }

    // -------------------------------------------------------------------
    // RS-06: 末尾の空要素は "" で補完する
    // -------------------------------------------------------------------

    /**
     * 2行目で COL_C が省略されているとき、COL_C の位置が "" で補完されること。
     * YamlTrailingNullTestData:
     *   row1: COL_A="val_a",  COL_B="val_b",  COL_C="val_c"
     *   row2: COL_A="val_a2", COL_B="val_b2"  ← COL_C 省略（末尾省略）
     *   row3: COL_A="val_a3", COL_C="val_c3"  ← COL_B 省略（中間省略）
     *
     * Given: 末尾省略・中間省略の行を含むテストデータ
     * When:  readLine() で各データ行を読み込む
     * Then:  省略列は "" で補完される（RS-06）
     */
    @Test
    public void readLine_trailingNullPaddedWithEmpty() {
        // Given
        sut.open(DIR, "YamlTrailingNullTestData");

        // セクションヘッダ
        sut.readLine();
        // カラムヘッダ
        List<String> colHeader = sut.readLine();
        int colCount = colHeader.size();

        // When / Then: 1行目（全列あり）
        List<String> row1 = sut.readLine();
        assertThat("1行目の列数", row1.size(), is(colCount));

        // When / Then: 2行目（COL_C 末尾省略 → "" 補完）
        List<String> row2 = sut.readLine();
        assertThat("2行目の列数がヘッダと同じ", row2.size(), is(colCount));
        int colCIdx = colHeader.indexOf("COL_C");
        assertThat("末尾省略列は空文字", row2.get(colCIdx), is(""));  // RS-06

        // When / Then: 3行目（COL_B 中間省略 → "" 補完）
        List<String> row3 = sut.readLine();
        assertThat("3行目の列数がヘッダと同じ", row3.size(), is(colCount));
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
     *
     * Given: 複数セクションを持つテストデータを開く
     * When:  全行を読み切るまで readLine() を呼び出す
     * Then:  最終行が最後のセクションの値データ行であること（E-3 の回帰防止）（RS-07）
     */
    @Test
    public void readLine_lastSectionNotLost() {
        // Given
        sut.open(DIR, "YamlTestDataReaderTestData");

        // When
        List<String> lastLine = null;
        List<String> line;
        while ((line = sut.readLine()) != null) {
            lastLine = line;
        }

        // Then: setup_files の最後の値行 ["", "002", "鈴木花子"]
        assertThat("最終行が存在すること", lastLine, is(notNullValue()));
        assertThat("最終行の列数", lastLine.size(), is(3));
        assertThat("最終行の先頭セルが空（データ行）", lastLine.get(0), is(""));
        assertThat("最終値行の1列目（USER_ID）", lastLine.get(1), is("002"));  // RS-07
        assertThat("最終値行の2列目（USER_NAME）", lastLine.get(2), is("鈴木花子"));
    }

    // -------------------------------------------------------------------
    // RS-08: isResourceExisting / isDataExisting
    // -------------------------------------------------------------------

    /**
     * Given: 存在する YAML ファイルのパスとリソース名
     * When:  isResourceExisting を呼び出す
     * Then:  true を返す（RS-08）
     */
    @Test
    public void isResourceExisting_fileExists_returnsTrue() {
        // Given / When / Then
        assertThat(sut.isResourceExisting(DIR, "YamlTestDataReaderTestData"), is(true));  // RS-08
    }

    /**
     * Given: 存在しない YAML ファイルのリソース名
     * When:  isResourceExisting を呼び出す
     * Then:  false を返す（RS-08）
     */
    @Test
    public void isResourceExisting_fileNotExists_returnsFalse() {
        // Given / When / Then
        assertThat(sut.isResourceExisting(DIR, "NoSuchFile"), is(false));  // RS-08
    }

    /**
     * Given: 存在しないディレクトリ
     * When:  isResourceExisting を呼び出す
     * Then:  false を返す（RS-08）
     */
    @Test
    public void isResourceExisting_dirNotExists_returnsFalse() {
        // Given / When / Then
        assertThat(sut.isResourceExisting("no/such/dir", "YamlTestDataReaderTestData"), is(false));  // RS-08
    }

    /**
     * Given: 存在する YAML ファイルのパスとリソース名
     * When:  isDataExisting を呼び出す
     * Then:  true を返す（RS-08）
     */
    @Test
    public void isDataExisting_fileExists_returnsTrue() {
        // Given / When / Then
        assertThat(sut.isDataExisting(DIR, "YamlTestDataReaderTestData"), is(true));  // RS-08
    }

    /**
     * Given: 存在しない YAML ファイルのリソース名
     * When:  isDataExisting を呼び出す
     * Then:  false を返す（RS-08）
     */
    @Test
    public void isDataExisting_fileNotExists_returnsFalse() {
        // Given / When / Then
        assertThat(sut.isDataExisting(DIR, "NoSuchFile"), is(false));  // RS-08
    }

    /**
     * Given: 存在しないディレクトリ
     * When:  isDataExisting を呼び出す
     * Then:  false を返す（RS-08）
     */
    @Test
    public void isDataExisting_dirNotExists_returnsFalse() {
        // Given / When / Then
        assertThat(sut.isDataExisting("no/such/dir", "YamlTestDataReaderTestData"), is(false));  // RS-08
    }

    // -------------------------------------------------------------------
    // loadYaml: YAML の最上位がマップ形式でない場合は空マップとして扱われる
    // -------------------------------------------------------------------

    /**
     * Given: YAML の最上位がリスト形式のファイル（マップでない）
     * When:  open して readLine() を呼び出す
     * Then:  即座に null を返す（空マップ扱い・セクションなし）
     */
    @Test
    public void open_yamlNotAMap_readLineReturnsNull() {
        // Given / When
        sut.open(DIR, "YamlNotAMapTestData");

        // Then: セクションが存在しないため先頭から null
        assertThat("非マップYAMLは空として扱われること", sut.readLine(), is(nullValue()));
    }

    // -------------------------------------------------------------------
    // 行シーケンス確認: setup_tables（グループIDなし）
    // -------------------------------------------------------------------

    /**
     * Given: グループIDなしの setup_tables エントリを含むテストデータ
     * When:  readLine() でセクションヘッダ・カラムヘッダ・データ行を読む
     * Then:  "SETUP_TABLE=TABLE_NAME" 形式のヘッダと正しいデータ行が返る（SS-01/RS-01）
     */
    @Test
    public void rowSequence_setupTable_noGroupId() {
        // Given
        sut.open(DIR, "YamlTestDataReaderTestData");

        // When / Then: セクションヘッダ
        List<String> sectionHeader = sut.readLine();
        assertThat(sectionHeader.get(0), is("SETUP_TABLE=USER"));  // SS-01

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

    /**
     * Given: グループIDありの setup_tables エントリを含むテストデータ
     * When:  前のセクションを読み飛ばした後に readLine() を呼ぶ
     * Then:  "SETUP_TABLE[groupId]=TABLE_NAME" 形式のヘッダが返る（SS-02/RS-01）
     */
    @Test
    public void rowSequence_setupTable_withGroupId() {
        // Given
        sut.open(DIR, "YamlTestDataReaderTestData");

        // 1つ目のセクション（groupId なし）: ヘッダ + カラムヘッダ + 1データ行
        sut.readLine(); // SETUP_TABLE=USER
        sut.readLine(); // col header
        sut.readLine(); // data row

        // When / Then: 2つ目のセクション（group_id=case1）
        List<String> sectionHeader = sut.readLine();
        assertThat(sectionHeader.get(0), is("SETUP_TABLE[case1]=ORDER"));  // SS-02
    }

    // -------------------------------------------------------------------
    // 行シーケンス確認: list_maps
    // -------------------------------------------------------------------

    /**
     * Given: list_maps セクションを含むテストデータ（setup_tables 2セクション後に配置）
     * When:  前セクションを読み飛ばした後に list_maps セクションを読む
     * Then:  "LIST_MAP=id" 形式のヘッダと正しいデータ行が返る（SS-19/RS-01）
     */
    @Test
    public void rowSequence_listMap() {
        // Given
        sut.open(DIR, "YamlTestDataReaderTestData");

        // setup_tables 2セクションをスキップ:
        //   セクション1(3行: ヘッダ+カラムヘッダ+データ1行) + セクション2(3行: ヘッダ+カラムヘッダ+データ1行)
        for (int i = 0; i < 6; i++) {
            sut.readLine();
        }

        // When / Then: LIST_MAP セクションヘッダ
        List<String> sectionHeader = sut.readLine();
        assertThat(sectionHeader.get(0), is("LIST_MAP=params"));  // SS-19

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

    /**
     * Given: 固定長 setup_files セクションを含むテストデータ
     * When:  前セクションを読み飛ばした後に setup_files セクションを読む
     * Then:  "SETUP_FIXED=path" ヘッダ・ディレクティブ行・フィールド名行・型行・長さ行・値行が返る（SS-08/RS-01）
     */
    @Test
    public void rowSequence_setupFiles_fixed() {
        // Given
        sut.open(DIR, "YamlTestDataReaderTestData");

        // setup_tables 2セクション(各3行)=6行 + list_maps 1セクション(ヘッダ+カラムヘッダ+データ2行)=4行
        for (int i = 0; i < 10; i++) {
            sut.readLine();
        }

        // When / Then: SETUP_FIXED セクションヘッダ
        List<String> sectionHeader = sut.readLine();
        assertThat(sectionHeader.get(0), is("SETUP_FIXED=input/data.dat"));  // SS-08

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
