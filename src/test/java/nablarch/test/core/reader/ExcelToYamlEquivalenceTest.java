package nablarch.test.core.reader;

import nablarch.test.core.reader.yaml.YamlTestDataParser;
import nablarch.test.core.db.TableData;
import nablarch.test.core.db.TestTable;
import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * V-1: Excel → YAML 変換後の等価性確認テスト（統合テスト）。
 *
 * <p>
 * 変換ツールで Excel を YAML に変換した結果を {@link YamlTestDataParser} で読み込み、
 * {@code BasicTestDataParser} で Excel を読み込んだ結果と等価であることを確認する。
 * </p>
 *
 * <p>テストデータの配置:
 * {@code src/test/java/nablarch/test/core/reader/BasicTestDataParserTest/*.yaml}
 * は {@code BasicTestDataParserTest.xls} を変換ツールで変換して生成したもの。
 * </p>
 *
 * <p>等価性の定義: 「テーブル名・カラム数・カラム名集合・行数・全カラムの値が双方で一致すること」。
 * ヘルパー {@link #assertEquivalentTable} が両方向（Excel 側の余分カラム・YAML 側の余分カラム）を検出する。
 * </p>
 *
 * <p>対象外シートと除外理由:
 * <ul>
 *   <li>{@code convertedValues}: ランダム値インタープリタ（{@code ${半角数字,...}} 等）を含み、
 *       Excel と YAML を別々に評価すると乱数が異なる値になるため等価比較が不可能。
 *       決定的な {@code ${binaryFile:...}} 行は別テスト（{@link #binaryFileInterpreter_equivalentToExcel}）で補完確認。</li>
 * </ul>
 * </p>
 */
@RunWith(DatabaseTestRunner.class)
public class ExcelToYamlEquivalenceTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    private static final String DIR = "src/test/java/nablarch/test/core/reader/";

    private TestDataParser xlsParser;
    private YamlTestDataParser yamlParser;

    @BeforeClass
    public static void beforeClass() {
        VariousDbTestHelper.createTable(TestTable.class);
    }

    @Before
    public void before() {
        xlsParser = repositoryResource.getComponent("testDataParser");

        DbInfo dbInfo = repositoryResource.getComponent("dbInfo");
        DefaultValues defaultValues = new BasicDefaultValues();
        List<nablarch.test.core.util.interpreter.TestDataInterpreter> interpreters =
                repositoryResource.getComponent("interpreters");

        yamlParser = new YamlTestDataParser();
        yamlParser.setDbInfo(dbInfo);
        yamlParser.setDefaultValues(defaultValues);
        yamlParser.setInterpreters(interpreters);
    }

    @After
    public void after() {
        YamlTestDataParser.clearCacheForTest();
    }

    // =========================================================================
    // getExpectedTableData: グループIDなし
    // =========================================================================

    /**
     * Given: BasicTestDataParserTest/withoutGroupId.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedTableData() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と件数・テーブル名・カラム集合・行数・値が等価である
     */
    @Test
    public void getExpectedTableData_withoutGroupId_equivalentToExcel() {
        List<TableData> fromXls = xlsParser.getExpectedTableData(DIR, "BasicTestDataParserTest/withoutGroupId");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR, "BasicTestDataParserTest/withoutGroupId");

        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "withoutGroupId[" + i + "]");
        }
    }

    // =========================================================================
    // getSetupTableData: グループIDなし
    // =========================================================================

    /**
     * Given: BasicTestDataParserTest/withoutGroupId.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupTableData() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void getSetupTableData_withoutGroupId_equivalentToExcel() {
        List<TableData> fromXls = xlsParser.getSetupTableData(DIR, "BasicTestDataParserTest/withoutGroupId");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR, "BasicTestDataParserTest/withoutGroupId");

        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "withoutGroupId_setup[" + i + "]");
        }
    }

    /**
     * Given: BasicTestDataParserTest/withoutGroupId.yaml が配置されている<br>
     * When:  YamlTestDataParser で getSetupTableData() を呼ぶ<br>
     * Then:  NULL 変換（"Null" → null）が Excel と YAML の両方で等しく機能していること
     */
    @Test
    public void getSetupTableData_nullValue_equivalentToExcel() {
        List<TableData> fromXls = xlsParser.getSetupTableData(DIR, "BasicTestDataParserTest/withoutGroupId");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR, "BasicTestDataParserTest/withoutGroupId");

        // withoutGroupId の SETUP_TABLE 1件目 2行目: VARCHAR2_COL が "Null" → null に変換される（IV 仕様）
        TableData xlsTable = fromXls.get(0);
        TableData yamlTable = fromYaml.get(0);
        assertNull("Excel: VARCHAR2_COL が null に変換されること", xlsTable.getValue(1, "VARCHAR2_COL"));
        assertNull("YAML: VARCHAR2_COL が null に変換されること（Excel と等価）", yamlTable.getValue(1, "VARCHAR2_COL"));
    }

    // =========================================================================
    // getExpectedTableData: グループID指定
    // =========================================================================

    /**
     * Given: BasicTestDataParserTest/withGroupId.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedTableData(groupId="case01") を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void getExpectedTableData_withGroupId_case01_equivalentToExcel() {
        List<TableData> fromXls = xlsParser.getExpectedTableData(DIR, "BasicTestDataParserTest/withGroupId", "case01");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR, "BasicTestDataParserTest/withGroupId", "case01");

        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "withGroupId_expected_case01[" + i + "]");
        }
    }

    /**
     * Given: BasicTestDataParserTest/withGroupId.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedTableData(groupId="case02") を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void getExpectedTableData_withGroupId_case02_equivalentToExcel() {
        List<TableData> fromXls = xlsParser.getExpectedTableData(DIR, "BasicTestDataParserTest/withGroupId", "case02");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR, "BasicTestDataParserTest/withGroupId", "case02");

        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "withGroupId_expected_case02[" + i + "]");
        }
    }

    // =========================================================================
    // getSetupTableData: グループID指定
    // =========================================================================

    /**
     * Given: BasicTestDataParserTest/withGroupId.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupTableData(groupId="case01") を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void getSetupTableData_withGroupId_case01_equivalentToExcel() {
        List<TableData> fromXls = xlsParser.getSetupTableData(DIR, "BasicTestDataParserTest/withGroupId", "case01");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR, "BasicTestDataParserTest/withGroupId", "case01");

        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "withGroupId_setup_case01[" + i + "]");
        }
    }

    /**
     * Given: BasicTestDataParserTest/withGroupId.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupTableData(groupId="case02") を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void getSetupTableData_withGroupId_case02_equivalentToExcel() {
        List<TableData> fromXls = xlsParser.getSetupTableData(DIR, "BasicTestDataParserTest/withGroupId", "case02");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR, "BasicTestDataParserTest/withGroupId", "case02");

        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "withGroupId_setup_case02[" + i + "]");
        }
    }

    // =========================================================================
    // getListMap
    // =========================================================================

    /**
     * Given: BasicTestDataParserTest/getListMap.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap("params") を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void getListMap_params_equivalentToExcel() {
        List<Map<String, String>> fromXls = xlsParser.getListMap(DIR, "BasicTestDataParserTest/getListMap", "params");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR, "BasicTestDataParserTest/getListMap", "params");

        assertEquivalentListMap(fromXls, fromYaml, "getListMap_params");
    }

    /**
     * Given: BasicTestDataParserTest/getListMap.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap("empty_cell") を呼ぶ<br>
     * Then:  空文字列セルが Excel と YAML の両方で等しく扱われる
     */
    @Test
    public void getListMap_emptyCell_equivalentToExcel() {
        List<Map<String, String>> fromXls = xlsParser.getListMap(DIR, "BasicTestDataParserTest/getListMap", "empty_cell");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR, "BasicTestDataParserTest/getListMap", "empty_cell");

        assertEquivalentListMap(fromXls, fromYaml, "getListMap_emptyCell");
    }

    // =========================================================================
    // invisibleTail（末尾の不可視セル）
    // =========================================================================

    /**
     * Given: BasicTestDataParserTest/invisibleTail.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap("expectedUgroup") を呼ぶ<br>
     * Then:  末尾不可視セルが除去され、Excel と YAML の読み込み結果が等価である
     */
    @Test
    public void getListMap_invisibleTail_equivalentToExcel() {
        List<Map<String, String>> fromXls = xlsParser.getListMap(DIR, "BasicTestDataParserTest/invisibleTail", "expectedUgroup");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR, "BasicTestDataParserTest/invisibleTail", "expectedUgroup");

        assertEquivalentListMap(fromXls, fromYaml, "invisibleTail_listmap");
    }

    /**
     * Given: BasicTestDataParserTest/invisibleTail.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedTableData() を呼ぶ<br>
     * Then:  末尾不可視セルが除去され、Excel と YAML の読み込み結果が等価である
     */
    @Test
    public void getExpectedTableData_invisibleTail_equivalentToExcel() {
        List<TableData> fromXls = xlsParser.getExpectedTableData(DIR, "BasicTestDataParserTest/invisibleTail");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR, "BasicTestDataParserTest/invisibleTail");

        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "invisibleTail[" + i + "]");
        }
    }

    // =========================================================================
    // completedWithId / completedWithoutId（EXPECTED_COMPLETED）
    // =========================================================================

    /**
     * Given: BasicTestDataParserTest/completedWithoutId.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedTableData() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void getExpectedTableData_completedWithoutId_equivalentToExcel() {
        List<TableData> fromXls = xlsParser.getExpectedTableData(DIR, "BasicTestDataParserTest/completedWithoutId");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR, "BasicTestDataParserTest/completedWithoutId");

        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "completedWithoutId[" + i + "]");
        }
    }

    /**
     * Given: BasicTestDataParserTest/completedWithId.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedTableData(groupId="with_ID") を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である（デフォルト値補完含む）
     */
    @Test
    public void getExpectedTableData_completedWithId_withID_equivalentToExcel() {
        List<TableData> fromXls = xlsParser.getExpectedTableData(DIR, "BasicTestDataParserTest/completedWithId", "with_ID");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR, "BasicTestDataParserTest/completedWithId", "with_ID");

        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "completedWithId_with_ID[" + i + "]");
        }
    }

    // =========================================================================
    // markerColumn（[no] / [desc] マーカーカラム除外）
    // =========================================================================

    /**
     * Given: BasicTestDataParserTest/markerColumn.yaml が配置されている（.xls から変換済み、[no]/[desc] を含む）<br>
     * When:  YamlTestDataParser で getListMap("params") を呼ぶ<br>
     * Then:  [no]/[desc] マーカーカラムが除外され、Excel と YAML の読み込み結果が等価である
     */
    @Test
    public void getListMap_markerColumn_equivalentToExcel() {
        List<Map<String, String>> fromXls = xlsParser.getListMap(DIR, "BasicTestDataParserTest/markerColumn", "params");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR, "BasicTestDataParserTest/markerColumn", "params");

        assertEquivalentListMap(fromXls, fromYaml, "markerColumn_listmap");
        // マーカーカラムが除外されていること
        for (Map<String, String> row : fromYaml) {
            assertThat("[no] が除外されること", row.containsKey("[no]"), is(false));
            assertThat("[desc] が除外されること", row.containsKey("[desc]"), is(false));
        }
    }

    /**
     * Given: BasicTestDataParserTest/markerColumn.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedTableData() を呼ぶ<br>
     * Then:  [no]/[desc] マーカーカラムが除外され、Excel と YAML の読み込み結果が等価である
     */
    @Test
    public void getExpectedTableData_markerColumn_equivalentToExcel() {
        List<TableData> fromXls = xlsParser.getExpectedTableData(DIR, "BasicTestDataParserTest/markerColumn");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR, "BasicTestDataParserTest/markerColumn");

        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "markerColumn_expected[" + i + "]");
            // マーカーカラムが除外されていること
            Set<String> yamlCols = new HashSet<>(Arrays.asList(fromYaml.get(i).getColumnNames()));
            assertThat("[no] が除外されること", yamlCols.contains("[no]"), is(false));
            assertThat("[desc] が除外されること", yamlCols.contains("[desc]"), is(false));
        }
    }

    // =========================================================================
    // binaryFile インタープリタ（決定的な等価性確認）
    // =========================================================================

    /**
     * Given: BasicTestDataParserTest/withoutGroupId.yaml が配置されている（BLOB_COL に ${binaryFile:...} を含む）<br>
     * When:  YamlTestDataParser で getExpectedTableData() を呼ぶ<br>
     * Then:  ${binaryFile:...} インタープリタが Excel と YAML の両方で同一の HexString に変換される
     */
    @Test
    public void binaryFileInterpreter_equivalentToExcel() {
        List<TableData> fromXls = xlsParser.getExpectedTableData(DIR, "BasicTestDataParserTest/withoutGroupId");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR, "BasicTestDataParserTest/withoutGroupId");

        // EXPECTED_TABLE 1件目 1行目: BLOB_COL = ${binaryFile:testdata.txt}
        String xlsBlobVal = fromXls.get(0).getValue(0, "BLOB_COL").toString();
        String yamlBlobVal = fromYaml.get(0).getValue(0, "BLOB_COL").toString();
        assertThat("${binaryFile:testdata.txt} が両方で同一の HexString に変換される",
                yamlBlobVal, is(xlsBlobVal));
        // 2行目: BLOB_COL = ${binaryFile:BasicTestDataParserTest.xls}
        String xlsBlobVal2 = fromXls.get(0).getValue(1, "BLOB_COL").toString();
        String yamlBlobVal2 = fromYaml.get(0).getValue(1, "BLOB_COL").toString();
        assertThat("${binaryFile:BasicTestDataParserTest.xls} が両方で同一の HexString に変換される",
                yamlBlobVal2, is(xlsBlobVal2));
    }

    // =========================================================================
    // ヘルパー
    // =========================================================================

    /**
     * TableData の等価性を確認する。
     * テーブル名・カラム数・カラム名集合・行数・全カラムの値が等価であることを検証する。
     * Excel 側のカラムのみを対象にせず、YAML 側の余分なカラムも検出できるよう両方向から確認する。
     */
    private void assertEquivalentTable(TableData xlsTable, TableData yamlTable, String label) {
        assertThat("テーブル名が等価 [" + label + "]",
                yamlTable.getTableName().toUpperCase(), is(xlsTable.getTableName().toUpperCase()));
        assertThat("行数が等価 [" + label + "]", yamlTable.size(), is(xlsTable.size()));

        Set<String> xlsCols = new HashSet<>(Arrays.asList(xlsTable.getColumnNames()));
        Set<String> yamlCols = new HashSet<>(Arrays.asList(yamlTable.getColumnNames()));
        assertThat("カラム数が等価 [" + label + "]", yamlTable.getColumnNames().length, is(xlsTable.getColumnNames().length));
        assertThat("カラム名集合が等価 [" + label + "]（YAML 側にのみ存在するカラムなし）",
                yamlCols.containsAll(xlsCols), is(true));
        assertThat("カラム名集合が等価 [" + label + "]（Excel 側にのみ存在するカラムなし）",
                xlsCols.containsAll(yamlCols), is(true));

        for (int row = 0; row < xlsTable.size(); row++) {
            for (String col : xlsTable.getColumnNames()) {
                Object xlsVal = xlsTable.getValue(row, col);
                Object yamlVal = yamlTable.getValue(row, col);
                assertThat("値が等価 [" + label + "][row=" + row + "][col=" + col + "]",
                        yamlVal == null ? null : yamlVal.toString(),
                        is(xlsVal == null ? null : xlsVal.toString()));
            }
        }
    }

    /**
     * List&lt;Map&lt;String, String&gt;&gt; の等価性を確認する。
     * 行数・キー数・キー名集合・値が等価であることを両方向から検証する。
     */
    private void assertEquivalentListMap(List<Map<String, String>> fromXls, List<Map<String, String>> fromYaml, String label) {
        assertThat("行数が等価 [" + label + "]", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            Map<String, String> xlsRow = fromXls.get(i);
            Map<String, String> yamlRow = fromYaml.get(i);
            assertThat("キー数が等価 [" + label + "][" + i + "]", yamlRow.size(), is(xlsRow.size()));
            Set<String> xlsKeys = xlsRow.keySet();
            Set<String> yamlKeys = yamlRow.keySet();
            assertThat("キー名集合が等価 [" + label + "][" + i + "]（YAML 側にのみ存在するキーなし）",
                    yamlKeys.containsAll(xlsKeys), is(true));
            assertThat("キー名集合が等価 [" + label + "][" + i + "]（Excel 側にのみ存在するキーなし）",
                    xlsKeys.containsAll(yamlKeys), is(true));
            for (Map.Entry<String, String> entry : xlsRow.entrySet()) {
                assertThat("値が等価 [" + label + "][" + i + "][" + entry.getKey() + "]",
                        yamlRow.get(entry.getKey()), is(entry.getValue()));
            }
        }
    }
}
