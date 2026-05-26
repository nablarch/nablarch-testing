package nablarch.test.core.reader.yaml;

import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.TableData;
import nablarch.test.core.db.TestTable;
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

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link YamlTableDataBuilder} のテストクラス。
 *
 * <p>
 * TableData・ListMap の構築ロジックを検証する。
 * </p>
 */
@RunWith(DatabaseTestRunner.class)
public class YamlTableDataBuilderTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    private static final String RESOURCE_ROOT = "src/test/java/";
    private static final String DIR = RESOURCE_ROOT + "nablarch/test/core/reader/yaml/";

    private DbInfo dbInfo;
    private YamlTableDataBuilder sut;

    @BeforeClass
    public static void beforeClass() {
        VariousDbTestHelper.createTable(TestTable.class);
    }

    @Before
    public void before() {
        dbInfo = repositoryResource.getComponent("dbInfo");
        List<TestDataInterpreter> interpreters = repositoryResource.getComponent("interpreters");
        sut = new YamlTableDataBuilder(dbInfo, new BasicDefaultValues(), interpreters);
    }

    @After
    public void after() {
        YamlLoader.clearCacheForTest();
    }

    // ========================================================================
    // buildTableDataList: グループ ID なしでデータを取得できること
    // ========================================================================

    /**
     * [YamlTableDataBuilder] buildTableDataList: グループ ID なしで setup_tables の TableData が取得できること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = sut.buildTableDataList(yaml, "setup_tables", "", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getTableName(), is("TEST_TABLE"));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000001"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: グループ ID 指定で対象グループのみ取得されること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = sut.buildTableDataList(yaml, "setup_tables", "[groupA]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000002"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: rows が空のエントリは除外されること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = sut.buildTableDataList(yaml, "setup_tables", "[emptyRows]", false, DIR);

        // Then
        assertThat(result.size(), is(0));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: fillDefaults=true の場合、fillDefaultValues が適用されること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/completedTable");

        // When
        List<TableData> result = sut.buildTableDataList(yaml, "expected_complete_tables", "", true, DIR);

        // Then
        assertThat(result.size(), is(1));
        TableData td = result.get(0);
        assertTrue("fillDefaultValues により全カラムが補完されていること", td.getColumnNames().length > 2);
        assertThat("NUMBER_COL のデフォルト値が補完されていること",
                td.getValue(0, "NUMBER_COL").toString(), is("0"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: セクションが存在しない場合は空リストが返ること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/emptyYaml");

        // When
        List<TableData> result = sut.buildTableDataList(yaml, "setup_tables", "", false, DIR);

        // Then
        assertThat(result.size(), is(0));
    }

    // ========================================================================
    // buildListMapRows
    // ========================================================================

    /**
     * [YamlTableDataBuilder] buildListMapRows: 指定 ID のデータが取得できること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = sut.buildListMapRows(yaml, "testListMap", DIR);

        // Then
        assertThat(result.size(), is(2));
        assertThat(result.get(0).get("KEY1"), is("val1"));
        assertThat(result.get(0).get("KEY2"), is("val2"));
        assertThat(result.get(1).get("KEY1"), is("val3"));
        assertThat(result.get(1).get("KEY2"), is("val4"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: マーカーカラム（[COL] 形式）は除外されること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = sut.buildListMapRows(yaml, "markerColTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertFalse(result.get(0).containsKey("[NO]"));
        assertThat(result.get(0).get("KEY1"), is("val1"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: 存在しない ID を指定した場合は空リストが返ること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = sut.buildListMapRows(yaml, "noSuchId", DIR);

        // Then
        assertThat(result.size(), is(0));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: YAML ネイティブ null は Java null として取得されること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = sut.buildListMapRows(yaml, "nativeTypeTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0).get("NULL_COL"), nullValue());
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: 同一グループID に同一テーブル名のエントリが複数ある場合、
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = sut.buildTableDataList(yaml, "setup_tables", "[dupTable]", false, DIR);

        // Then
        assertThat("同一グループの同一テーブル名エントリが 2 件返ること", result.size(), is(2));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000010"));
        assertThat(result.get(1).getValue(0, "PK_COL1").toString(), is("0000000011"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: table キーが存在しないエントリで IllegalStateException がスローされること（E-1）。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When / Then
        try {
            sut.buildTableDataList(yaml, "setup_tables", "[missingTable]", false, DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            assertThat("フィールド名がメッセージに含まれること", e.getMessage(), containsString("table"));
            assertThat("セクション名がメッセージに含まれること", e.getMessage(), containsString("setup_tables"));
            assertThat("ファイルパスがメッセージに含まれること", e.getMessage(), containsString(DIR));
        }
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: 同一ファイル内で同一 ID のエントリが 2 件ある場合、先着一致で最初の 1 件のみ返ること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = sut.buildListMapRows(yaml, "dupIdFirst", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("先着一致で最初の 1 件のみ返ること", result.get(0).get("KEY1"), is("first"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: setup_tables のマーカーカラム（[COL] 形式）は除外されること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = sut.buildTableDataList(yaml, "setup_tables", "[markerColInTable]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        String[] columnNames = result.get(0).getColumnNames();
        for (String col : columnNames) {
            assertFalse("マーカーカラム [NO] が含まれないこと", col.equals("[NO]"));
        }
        assertThat("PK_COL1 は含まれること", result.get(0).getValue(0, "PK_COL1").toString(), is("0000000001"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: expected_tables のマーカーカラム（[COL] 形式）は除外されること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = sut.buildTableDataList(yaml, "expected_tables", "[markerColInTable]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        String[] columnNames = result.get(0).getColumnNames();
        for (String col : columnNames) {
            assertFalse("マーカーカラム [NO] が含まれないこと", col.equals("[NO]"));
        }
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: クォートあり "null" は Java null として取得されること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = sut.buildListMapRows(yaml, "interpreterTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("\"null\"（クォートあり）は Java null になること", result.get(0).get("QUOTED_NULL"), nullValue());
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: " " はクォート除去後にスペース1文字になること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = sut.buildListMapRows(yaml, "interpreterTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("\" \" はスペース1文字になること", result.get(0).get("SPACE_COL"), is(" "));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: "\\r" は CR（キャリッジリターン）文字に変換されること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = sut.buildListMapRows(yaml, "interpreterTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("\"\\\\r\" は CR 文字に変換されること", result.get(0).get("CR_COL"), is("\r"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: YAML ネイティブ boolean / integer / float は文字列化されること。
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
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = sut.buildListMapRows(yaml, "nativeTypeTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertThat(row.get("BOOL_TRUE"), is("true"));
        assertThat(row.get("BOOL_FALSE"), is("false"));
        assertThat(row.get("INT_COL"), is("42"));
        assertThat(row.get("FLOAT_COL"), is("3.14"));
    }
}
