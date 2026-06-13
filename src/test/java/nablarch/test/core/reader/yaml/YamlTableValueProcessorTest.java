package nablarch.test.core.reader.yaml;

import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.TableData;
import nablarch.test.core.db.TestTable;
import nablarch.test.core.util.interpreter.DateTimeInterpreter;
import nablarch.test.core.util.interpreter.NullInterpreter;
import nablarch.test.core.util.interpreter.QuotationTrimmer;
import nablarch.test.core.util.interpreter.TestDataInterpreter;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link YamlValueProcessor} のテーブル系メソッド（{@code toTableDataList}／{@code toListMapRows}）のテストクラス。
 *
 * <p>
 * 構造マッピング層 {@link YamlTableStructureMapper} が返した生の構造レコードを {@link YamlValueProcessor}
 * が値加工（{@code ${...}} の解釈・マーカー列除外・グループ絞り込み・{@code fillDefaultValues}・
 * list_maps の TreeMap ソート）して {@link TableData}・ListMap を組み立てる一連のロジックを検証する。
 * 構造層が記法のまま保持することの直接検証は {@link YamlTableStructureMapperTest} が担う。
 * </p>
 */
@RunWith(DatabaseTestRunner.class)
public class YamlTableValueProcessorTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    private static final String RESOURCE_ROOT = "src/test/java/";
    private static final String DIR = RESOURCE_ROOT + "nablarch/test/core/reader/yaml/";

    private DbInfo dbInfo;
    private YamlValueProcessor valueProcessor;
    private final YamlTableStructureMapper tableMapper = new YamlTableStructureMapper();

    @BeforeClass
    public static void beforeClass() {
        VariousDbTestHelper.createTable(TestTable.class);
    }

    @Before
    public void before() {
        dbInfo = repositoryResource.getComponent("dbInfo");
        List<TestDataInterpreter> interpreters = repositoryResource.getComponent("interpreters");
        valueProcessor = new YamlValueProcessor(dbInfo, new BasicDefaultValues(), interpreters);
    }

    @After
    public void after() {
        YamlLoader.clearCacheForTest();
    }

    // ------------------------------------------------------------------------
    // 構造マッピング層（Map→Raw*）→ 値加工層（Raw*→本体器）の 2 層を通すヘルパー。
    // 本テストは両層を通したエンドツーエンドの本体器構築を検証する。
    // ------------------------------------------------------------------------

    private List<TableData> buildTableDataList(Map<String, Object> yaml, String sectionKey,
                                               String groupId, boolean fillDefaults, String path) {
        return valueProcessor.toTableDataList(
                tableMapper.mapTables(yaml, sectionKey), sectionKey, groupId, fillDefaults, path);
    }

    private List<Map<String, String>> buildListMapRows(Map<String, Object> yaml, String id, String path) {
        return valueProcessor.toListMapRows(tableMapper.mapListMaps(yaml), id, path);
    }

    // ========================================================================
    // buildTableDataList: グループ ID なしでデータを取得できること
    // ========================================================================

    /**
     * [YamlTableValueProcessor] buildTableDataList: グループ ID なしで setup_tables の TableData が取得できること。
     *
     * <p>
     * Given: setup_tables にグループ ID なしの 1 エントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "", false, path) を呼ぶ<br>
     * Then:  1 件の TableData が返り、テーブル名・カラム値が正しいこと
     * </p>
     */
    @Test
    public void testBuildTableDataList_noGroupId() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getTableName(), is("TEST_TABLE"));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000001"));
    }

    /**
     * [YamlTableValueProcessor] buildTableDataList: グループ ID 指定で対象グループのみ取得されること。
     *
     * <p>
     * Given: setup_tables に groupA / groupB のエントリがある<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[groupA]", false, path) を呼ぶ<br>
     * Then:  groupA の 1 件のみ返ること
     * </p>
     */
    @Test
    public void testBuildTableDataList_withGroupId() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[groupA]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000002"));
    }

    /**
     * [YamlTableValueProcessor] buildTableDataList: rows が空のエントリは除外されること。
     *
     * <p>
     * Given: setup_tables に rows: [] のエントリ（emptyRows グループ）<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[emptyRows]", false, path) を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void testBuildTableDataList_emptyRowsExcluded() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[emptyRows]", false, DIR);

        // Then
        assertThat(result.size(), is(0));
    }

    /**
     * [YamlTableValueProcessor] buildTableDataList: fillDefaults=true の場合、fillDefaultValues が適用されること。
     *
     * <p>
     * Given: expected_complete_tables に PK_COL1/PK_COL2 のみのエントリ<br>
     * When:  buildTableDataList(yaml, "expected_complete_tables", "", true, path) を呼ぶ<br>
     * Then:  省略カラムにデフォルト値が補完されていること
     * </p>
     */
    @Test
    public void testBuildTableDataList_fillDefaultValues() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/completedTable");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_complete_tables", "", true, DIR);

        // Then
        assertThat(result.size(), is(1));
        TableData td = result.get(0);
        assertTrue("fillDefaultValues により全カラムが補完されていること", td.getColumnNames().length > 2);
        assertThat("NUMBER_COL のデフォルト値が補完されていること",
                td.getValue(0, "NUMBER_COL").toString(), is("0"));
    }

    /**
     * [YamlTableValueProcessor] buildTableDataList: セクションが存在しない場合は空リストが返ること。
     *
     * <p>
     * Given: setup_tables キーが存在しない YAML<br>
     * When:  buildTableDataList(yaml, "setup_tables", "", false, path) を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void testBuildTableDataList_sectionNotExists() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/emptyYaml");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "", false, DIR);

        // Then
        assertThat(result.size(), is(0));
    }

    // ========================================================================
    // buildListMapRows
    // ========================================================================

    /**
     * [YamlTableValueProcessor] buildListMapRows: 指定 ID のデータが取得できること。
     *
     * <p>
     * Given: list_maps に id=testListMap が 2 行<br>
     * When:  buildListMapRows(yaml, "testListMap", path) を呼ぶ<br>
     * Then:  2 行のデータが返ること
     * </p>
     */
    @Test
    public void testBuildListMapRows_normalCase() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "testListMap", DIR);

        // Then
        assertThat(result.size(), is(2));
        assertThat(result.get(0).get("KEY1"), is("val1"));
        assertThat(result.get(0).get("KEY2"), is("val2"));
        assertThat(result.get(1).get("KEY1"), is("val3"));
        assertThat(result.get(1).get("KEY2"), is("val4"));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: マーカーカラム（[COL] 形式）は除外されること。
     *
     * <p>
     * Given: list_maps に "[NO]" キーを含む行<br>
     * When:  buildListMapRows(yaml, "markerColTest", path) を呼ぶ<br>
     * Then:  "[NO]" キーが結果に含まれないこと
     * </p>
     */
    @Test
    public void testBuildListMapRows_markerColumnsExcluded() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "markerColTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertFalse(result.get(0).containsKey("[NO]"));
        assertThat(result.get(0).get("KEY1"), is("val1"));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: 存在しない ID を指定した場合は空リストが返ること。
     *
     * <p>
     * Given: list_maps に存在しない id<br>
     * When:  buildListMapRows(yaml, "noSuchId", path) を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void testBuildListMapRows_idNotFound() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "noSuchId", DIR);

        // Then
        assertThat(result.size(), is(0));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: YAML ネイティブ null は Java null として取得されること。
     *
     * <p>
     * Given: list_maps に NULL_COL: null（YAML ネイティブ null）<br>
     * When:  buildListMapRows(yaml, "nativeNullTest", path) を呼ぶ<br>
     * Then:  NULL_COL の値が null であること
     * </p>
     */
    @Test
    public void testBuildListMapRows_nativeNullIsJavaNull() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "nativeTypeTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertNull(result.get(0).get("NULL_COL"));
    }

    /**
     * [YamlTableValueProcessor] buildTableDataList: 同一グループID に同一テーブル名のエントリが複数ある場合、
     * 全件取得できること（QA観点2-軽微）。
     *
     * <p>
     * Given: setup_tables に group_id=dupTable で TEST_TABLE が 2 エントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[dupTable]", false, path) を呼ぶ<br>
     * Then:  2 件の TableData が返り、それぞれのデータが正しいこと
     * </p>
     */
    @Test
    public void testBuildTableDataList_duplicateTableNamesInSameGroup() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[dupTable]", false, DIR);

        // Then
        assertThat("同一グループの同一テーブル名エントリが 2 件返ること", result.size(), is(2));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000010"));
        assertThat(result.get(1).getValue(0, "PK_COL1").toString(), is("0000000011"));
    }

    /**
     * [YamlTableValueProcessor] buildTableDataList: table キーが存在しないエントリで IllegalStateException がスローされること（E-1）。
     *
     * <p>
     * Given: setup_tables に table キーがない missingTable グループのエントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[missingTable]", false, path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、メッセージにセクション名とファイルパスが含まれること
     * </p>
     */
    @Test
    public void testBuildTableDataList_missingTableThrowsException() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When / Then
        try {
            buildTableDataList(yaml, "setup_tables", "[missingTable]", false, DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            assertThat("フィールド名がメッセージに含まれること", e.getMessage(), containsString("table"));
            assertThat("セクション名がメッセージに含まれること", e.getMessage(), containsString("setup_tables"));
            assertThat("ファイルパスがメッセージに含まれること", e.getMessage(), containsString(DIR));
        }
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: 同一ファイル内で同一 ID のエントリが 2 件ある場合、先着一致で最初の 1 件のみ返ること。
     *
     * <p>
     * 解説書 5.5: 同一ファイル内で同一 ID の重複エントリは先着一致で、2件目以降は無視されます<br>
     * Given: list_maps に id=dupIdFirst が 2 エントリ（1件目 KEY1="first", 2件目 KEY1="second"）<br>
     * When:  buildListMapRows(yaml, "dupIdFirst", path) を呼ぶ<br>
     * Then:  1件目の KEY1="first" が返ること
     * </p>
     */
    @Test
    public void testBuildListMapRows_duplicateIdReturnsFirst() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "dupIdFirst", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("先着一致で最初の 1 件のみ返ること", result.get(0).get("KEY1"), is("first"));
    }

    /**
     * [YamlTableValueProcessor] buildTableDataList: rows 内の空エントリ（{}）は読み飛ばされること。
     *
     * <p>
     * 解説書 10.5: rows 内の要素が空マッピング（{}）の場合にスキップされます<br>
     * Given: setup_tables の emptyRowMixed グループに 通常行・{} 行・通常行 の 3 エントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[emptyRowMixed]", false, path) を呼ぶ<br>
     * Then:  {} 行がスキップされ、2 行のみ返ること
     * </p>
     */
    @Test
    public void testBuildTableDataList_emptyRowEntrySkipped() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[emptyRowMixed]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        // result.get(0).size() は TableData の行数（addRow された件数）を返す
        assertThat("空エントリ {} をスキップして 2 行のみ返ること", result.get(0).size(), is(2));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000020"));
        assertThat(result.get(0).getValue(1, "PK_COL1").toString(), is("0000000021"));
    }

    /**
     * [YamlTableValueProcessor] buildTableDataList: 先頭行が空エントリ（{}）の場合はカラム 0 件の TableData が返ること（JE-6）。
     *
     * <p>
     * 解説書 10.5: 先頭行が {} の場合、カラム定義が 0 件の TableData が生成され、行データは 0 件となること<br>
     * Given: setup_tables の allEmptyRows グループに {} × 2 のみ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[allEmptyRows]", false, path) を呼ぶ<br>
     * Then:  TableData が 1 件返り、カラム 0 件・行 0 件であること
     * </p>
     */
    @Test
    public void testBuildTableDataList_allEmptyRowsReturnsTableDataWithZeroColumns() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[allEmptyRows]", false, DIR);

        // Then
        assertThat("先頭行が {} の場合も TableData は 1 件生成されること", result.size(), is(1));
        assertThat("カラム数が 0 件であること", result.get(0).getColumnNames().length, is(0));
        assertThat("行数が 0 件であること", result.get(0).size(), is(0));
    }

    /**
     * [YamlTableValueProcessor] buildTableDataList: setup_tables のマーカーカラム（[COL] 形式）は除外されること。
     *
     * <p>
     * 解説書 10.2: YAML では setup_tables / expected_tables / list_maps すべてでマーカーカラムが除外されます<br>
     * Given: setup_tables の markerColInTable グループに "[NO]" カラムを含む行<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[markerColInTable]", false, path) を呼ぶ<br>
     * Then:  "[NO]" カラムが TableData のカラム名に含まれないこと
     * </p>
     */
    @Test
    public void testBuildTableDataList_markerColumnsExcluded() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[markerColInTable]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        String[] columnNames = result.get(0).getColumnNames();
        for (String col : columnNames) {
            assertFalse("マーカーカラム [NO] が含まれないこと", col.equals("[NO]"));
        }
        assertThat("PK_COL1 は含まれること", result.get(0).getValue(0, "PK_COL1").toString(), is("0000000001"));
    }

    /**
     * [YamlTableValueProcessor] buildTableDataList: expected_tables のマーカーカラム（[COL] 形式）は除外されること。
     *
     * <p>
     * 解説書 10.2: YAML では setup_tables / expected_tables / list_maps すべてでマーカーカラムが除外されます<br>
     * Given: expected_tables の markerColInTable グループに "[NO]" カラムを含む行<br>
     * When:  buildTableDataList(yaml, "expected_tables", "[markerColInTable]", false, path) を呼ぶ<br>
     * Then:  "[NO]" カラムが TableData のカラム名に含まれないこと
     * </p>
     */
    @Test
    public void testBuildTableDataList_markerColumnsExcludedInExpectedTables() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_tables", "[markerColInTable]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        String[] columnNames = result.get(0).getColumnNames();
        for (String col : columnNames) {
            assertFalse("マーカーカラム [NO] が含まれないこと", col.equals("[NO]"));
        }
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: クォートあり "null" は Java null として取得されること。
     *
     * <p>
     * 解説書 8.1: YAML の "null"（クォートあり）も Java null になります（NullInterpreter が変換）<br>
     * Given: list_maps に QUOTED_NULL: "null"（クォートあり）<br>
     * When:  buildListMapRows(yaml, "interpreterTest", path) を呼ぶ<br>
     * Then:  QUOTED_NULL の値が null であること
     * </p>
     */
    @Test
    public void testBuildListMapRows_quotedNullIsJavaNull() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "interpreterTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertNull("\"null\"（クォートあり）は Java null になること", result.get(0).get("QUOTED_NULL"));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: " " はクォート除去後にスペース1文字になること。
     *
     * <p>
     * 解説書 8.1: " "（スペースをダブルクォートで囲む）→ QuotationTrimmer が外側クォートを除去してスペース1文字<br>
     * Given: list_maps に SPACE_COL: " "<br>
     * When:  buildListMapRows(yaml, "interpreterTest", path) を呼ぶ<br>
     * Then:  SPACE_COL の値がスペース1文字であること
     * </p>
     */
    @Test
    public void testBuildListMapRows_spaceBetweenQuotesIsSpace() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "interpreterTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("\" \" はスペース1文字になること", result.get(0).get("SPACE_COL"), is(" "));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: "\\r" は CR（キャリッジリターン）文字に変換されること。
     *
     * <p>
     * 解説書 8.1/8.3: "\\r" → LineSeparatorInterpreter が CR（0x0D）に変換（デフォルト設定）<br>
     * Given: list_maps に CR_COL: "\\r"<br>
     * When:  buildListMapRows(yaml, "interpreterTest", path) を呼ぶ<br>
     * Then:  CR_COL の値が CR 文字（"\r"）であること
     * </p>
     */
    @Test
    public void testBuildListMapRows_escapedCrIsCarriageReturn() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "interpreterTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("\"\\\\r\" は CR 文字に変換されること", result.get(0).get("CR_COL"), is("\r"));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: "${systemTime}" 完全一致の場合はシステム時刻に変換されること（8.4）。
     *
     * <p>
     * 解説書 8.4: DateTimeInterpreter は完全一致のみ変換する。部分文字列は変換されない<br>
     * Given: list_maps に EXACT_COL="${systemTime}", PARTIAL_COL="prefix_${systemTime}"<br>
     * When:  buildListMapRows(yaml, "dateTimeTest", path) を呼ぶ<br>
     * Then:  EXACT_COL はシステム時刻文字列になり、PARTIAL_COL は変換されないこと
     * </p>
     */
    @Test
    public void testBuildListMapRows_dateTimeInterpreterExactMatchOnly() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "dateTimeTest", DIR);

        // Then
        // 期待値 "2010-09-14 12:34:56.0" は unit-test-yaml.xml の dateProvider（BasicDateTimeProvider）に
        // 固定値 "2010-09-14 12:34:56" が設定されているため。
        assertThat(result.size(), is(1));
        assertThat("${systemTime} 完全一致はシステム時刻に変換されること",
                result.get(0).get("EXACT_COL"), is("2010-09-14 12:34:56.0"));
        assertThat("部分文字列 prefix_${systemTime} は変換されないこと",
                result.get(0).get("PARTIAL_COL"), is("prefix_${systemTime}"));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: "${binaryFile:path}" はファイル内容の HexString に変換されること（8.6）。
     *
     * <p>
     * 解説書 8.6: BinaryFileInterpreter のパスは YAML ファイルのディレクトリからの相対パス<br>
     * Given: list_maps に BIN_COL="${binaryFile:YamlTableValueProcessorTest/test.bin}"<br>
     * When:  buildListMapRows(yaml, "binaryFileTest", path) を呼ぶ<br>
     * Then:  BIN_COL が test.bin のバイト列 HexString（"414243"）になること
     * </p>
     */
    @Test
    public void testBuildListMapRows_binaryFileInterpreterResolvesRelativePath() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "binaryFileTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("${binaryFile:path} はファイル内容の HexString に変換されること",
                result.get(0).get("BIN_COL"), is("414243"));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: "${半角英字,N}" 形式で指定長の文字列が生成されること（8.5）。
     *
     * <p>
     * 解説書 8.5: BasicJapaneseCharacterInterpreter が ${文字種,文字数} を生成する<br>
     * Given: list_maps に ALPHA_COL="${半角英字,10}", NUM_COL="${半角数字,5}"<br>
     * When:  buildListMapRows(yaml, "charGenTest", path) を呼ぶ<br>
     * Then:  ALPHA_COL は 10 文字の半角英字、NUM_COL は 5 文字の半角数字になること
     * </p>
     */
    @Test
    public void testBuildListMapRows_charTypeGeneratorProducesSpecifiedLength() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "charGenTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        String alphaVal = result.get(0).get("ALPHA_COL");
        assertThat("${半角英字,10} は 10 文字になること", alphaVal.length(), is(10));
        assertTrue("${半角英字,10} は半角英字のみであること", alphaVal.matches("[a-zA-Z]{10}"));
        String numVal = result.get(0).get("NUM_COL");
        assertThat("${半角数字,5} は 5 文字になること", numVal.length(), is(5));
        assertTrue("${半角数字,5} は半角数字のみであること", numVal.matches("[0-9]{5}"));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: "\""（YAML エスケープ）はダブルクォート1文字になること（8.1/8.2 G-1）。
     *
     * <p>
     * 解説書 8.1/examples-special 8.2: `"\""` → YAML パース後は `"` 1文字。
     * QuotationTrimmer は前後クォート囲みがない1文字 `"` には適用されず、そのまま `"` が返ること<br>
     * Given: list_maps に DQ_COL: "\""<br>
     * When:  buildListMapRows(yaml, "quotationTest", path) を呼ぶ<br>
     * Then:  DQ_COL の値がダブルクォート1文字（`"`）であること
     * </p>
     */
    @Test
    public void testBuildListMapRows_escapedDoubleQuoteIsDoubleQuoteChar() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "quotationTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("\"\\\"\" はダブルクォート1文字になること",
                result.get(0).get("DQ_COL"), is("\""));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: '"'（YAML シングルクォート記法）でのダブルクォート1文字になること（8.2 QA-3）。
     *
     * <p>
     * 解説書 8.2: シングルクォートで囲んだ '"' も YAML パース後は " 1文字。
     * QuotationTrimmer は前後クォート囲みがない1文字 '"' には適用されず、そのまま '"' が返ること<br>
     * Given: list_maps に DQ_COL: '"'（YAML シングルクォート記法）<br>
     * When:  buildListMapRows(yaml, "singleQuoteNotationTest", path) を呼ぶ<br>
     * Then:  DQ_COL の値がダブルクォート1文字（"）であること
     * </p>
     */
    @Test
    public void testBuildListMapRows_singleQuoteNotationForDoubleQuote() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "singleQuoteNotationTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("'\"'（シングルクォート記法）はダブルクォート1文字になること",
                result.get(0).get("DQ_COL"), is("\""));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: "${updateTime}" / "${setUpTime}" はシステム時刻に変換されること（8.1/8.4 G-2）。
     *
     * <p>
     * 解説書 8.1/8.4: DateTimeInterpreter は "${updateTime}" と "${setUpTime}" も完全一致で変換する<br>
     * Given: list_maps に UPDATE_COL="${updateTime}", SET_UP_TIME_COL="${setUpTime}"、
     *        DateTimeInterpreter に setSetUpDateTime("2010-09-14 12:34:56.0") 設定済み<br>
     * When:  buildListMapRows(yaml, "quotationTest", path) を呼ぶ<br>
     * Then:  両カラムがシステム時刻文字列（"2010-09-14 12:34:56.0"）になること
     * </p>
     */
    @Test
    public void testBuildListMapRows_updateTimeAndSetUpTimeConverted() {
        // Given
        // @Before の sut は setSetUpDateTime 未設定のため、ここで専用インスタンスを生成する
        DateTimeInterpreter dateTimeInterpreter = new DateTimeInterpreter();
        dateTimeInterpreter.setSystemTimeProvider(repositoryResource.getComponent("dateProvider"));
        dateTimeInterpreter.setSetUpDateTime("2010-09-14 12:34:56.0");
        List<TestDataInterpreter> interpreters = Arrays.asList(
                new NullInterpreter(),
                new QuotationTrimmer(),
                dateTimeInterpreter
        );
        YamlValueProcessor vpWithSetUp = new YamlValueProcessor(dbInfo, new BasicDefaultValues(), interpreters);
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/nativeTypes");

        // When
        List<Map<String, String>> result = vpWithSetUp.toListMapRows(tableMapper.mapListMaps(yaml), "quotationTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("${updateTime} はシステム時刻に変換されること",
                result.get(0).get("UPDATE_COL"), is("2010-09-14 12:34:56.0"));
        assertThat("${setUpTime} はシステム時刻に変換されること",
                result.get(0).get("SET_UP_TIME_COL"), is("2010-09-14 12:34:56.0"));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: "[" で始まるが "]" で終わらないキーは除外されないこと。
     *
     * <p>
     * 解説書 10.2: マーカーカラムは "[COL]" 形式（両端が角括弧）のみ除外される。
     * "[OPEN" のように "[" で始まっても "]" で終わらないキーは通常カラムとして扱われること<br>
     * Given: list_maps の partialBracketColTest に "[OPEN" キーと "KEY1" キーを含む行<br>
     * When:  buildListMapRows(yaml, "partialBracketColTest", path) を呼ぶ<br>
     * Then:  "[OPEN" キーが結果 Map に含まれること（マーカーと見なされないこと）
     * </p>
     */
    @Test
    public void testBuildListMapRows_partialBracketKeyIsNotExcluded() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "partialBracketColTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertTrue("\"[OPEN\" はマーカーではないため結果 Map に含まれること",
                result.get(0).containsKey("[OPEN"));
        assertThat("KEY1 の値が正しいこと", result.get(0).get("KEY1"), is("real_val"));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: rows に Map でない要素（スカラー）が含まれる場合はスキップされること。
     *
     * <p>
     * 解説書 10.x: list_maps の rows に Map でない要素が混在しても例外なくスキップされること<br>
     * Given: list_maps の nonMapRowTest に 通常行・スカラー文字列・通常行 の 3 エントリ<br>
     * When:  buildListMapRows(yaml, "nonMapRowTest", path) を呼ぶ<br>
     * Then:  Map でない行はスキップされ、Map の行 2 件のみ返ること
     * </p>
     */
    @Test
    public void testBuildListMapRows_nonMapRowSkipped() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "nonMapRowTest", DIR);

        // Then
        assertThat("Map でない行はスキップされ 2 件のみ返ること", result.size(), is(2));
        assertThat("1 件目が正しいこと", result.get(0).get("KEY1"), is("valid"));
        assertThat("2 件目が正しいこと", result.get(1).get("KEY1"), is("also_valid"));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: setSetUpDateTime 未設定時に "${setUpTime}" が変換されないこと（8.4 QA-4）。
     *
     * <p>
     * 解説書 8.4: setSetUpDateTime を呼ばずに "${setUpTime}" を使った場合、変換されずにそのまま残ること<br>
     * Given: @Before の sut（setSetUpDateTime 未設定）で list_maps に SET_UP_TIME_COL="${setUpTime}"<br>
     * When:  buildListMapRows(yaml, "quotationTest", path) を呼ぶ<br>
     * Then:  SET_UP_TIME_COL の値が "${setUpTime}" のまま変換されないこと
     * </p>
     */
    @Test
    public void testBuildListMapRows_setUpTimeNotConvertedWithoutSetSetUpDateTime() {
        // Given: @Before の sut は setSetUpDateTime 未設定
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "quotationTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("setSetUpDateTime 未設定時は ${setUpTime} が変換されないこと",
                result.get(0).get("SET_UP_TIME_COL"), is("${setUpTime}"));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: testShots 予約 ID で list_maps が正しく取得できること（4章 G-6）。
     *
     * <p>
     * 解説書 4.1: testShots は予約 ID であり、通常の list_maps エントリと同様に取得できること<br>
     * Given: list_maps に id=testShots で no/description/expectedStatusCode/setUpTable/expectedTable カラムを持つ2件のエントリ<br>
     * When:  buildListMapRows(yaml, "testShots", path) を呼ぶ<br>
     * Then:  2件取得でき、各カラム値が保持されていること
     * </p>
     */
    @Test
    public void testBuildListMapRows_testShotsReservedId() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "testShots", DIR);

        // Then
        assertThat("2件取得できること", result.size(), is(2));
        Map<String, String> row1 = result.get(0);
        assertThat("no カラムが保持されること", row1.get("no"), is("1"));
        assertThat("description カラムが保持されること", row1.get("description"), is("ケース1"));
        assertThat("expectedStatusCode カラムが保持されること", row1.get("expectedStatusCode"), is("200"));
        assertThat("setUpTable カラムが保持されること", row1.get("setUpTable"), is(""));
        Map<String, String> row2 = result.get(1);
        assertThat("2件目の no カラムが保持されること", row2.get("no"), is("2"));
        assertThat("2件目の setUpTable カラムが保持されること", row2.get("setUpTable"), is("case2"));
    }

    /**
     * [YamlTableValueProcessor] buildListMapRows: YAML ネイティブ boolean / integer / float は文字列化されること。
     *
     * <p>
     * Given: BOOL_TRUE=true, INT_COL=42, FLOAT_COL=3.14（クォートなし）<br>
     * When:  buildListMapRows(yaml, "nativeTypeTest", path) を呼ぶ<br>
     * Then:  それぞれ "true", "42", "3.14" として取得されること
     * </p>
     */
    @Test
    public void testBuildListMapRows_nativeTypesStringified() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableValueProcessorTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "nativeTypeTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertThat(row.get("BOOL_TRUE"), is("true"));
        assertThat(row.get("BOOL_FALSE"), is("false"));
        assertThat(row.get("INT_COL"), is("42"));
        assertThat(row.get("FLOAT_COL"), is("3.14"));
    }
}
