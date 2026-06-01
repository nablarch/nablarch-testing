package nablarch.test.core.reader;

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

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * V-1: Excel → YAML 変換後の等価性確認テスト（統合テスト）。
 *
 * <p>
 * 変換ツールで Excel を YAML に変換した結果を {@link YamlTestDataParser} で読み込み、
 * {@code BasicTestDataParser} で Excel を読み込んだ結果と等価であることを確認する。
 * </p>
 *
 * <p>
 * テストデータの配置:<br>
 * {@code src/test/java/nablarch/test/core/reader/BasicTestDataParserTest/*.yaml}
 * は {@code BasicTestDataParserTest.xls} を変換ツールで変換して生成したもの。
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
     * Then:  BasicTestDataParser で Excel を読んだ結果と件数・テーブル名が等価である
     */
    @Test
    public void getExpectedTableData_withoutGroupId_equivalentToExcel() {
        List<TableData> fromXls = xlsParser.getExpectedTableData(DIR, "BasicTestDataParserTest/withoutGroupId");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR, "BasicTestDataParserTest/withoutGroupId");

        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            TableData xlsTable = fromXls.get(i);
            TableData yamlTable = fromYaml.get(i);
            assertThat("テーブル名が等価 [" + i + "]",
                    yamlTable.getTableName().toUpperCase(), is(xlsTable.getTableName().toUpperCase()));
            assertThat("行数が等価 [" + i + "]", yamlTable.size(), is(xlsTable.size()));
            for (int row = 0; row < xlsTable.size(); row++) {
                for (String col : xlsTable.getColumnNames()) {
                    Object xlsVal = xlsTable.getValue(row, col);
                    Object yamlVal = yamlTable.getValue(row, col);
                    assertThat("値が等価 [" + i + "][" + row + "][" + col + "]",
                            yamlVal == null ? null : yamlVal.toString(),
                            is(xlsVal == null ? null : xlsVal.toString()));
                }
            }
        }
    }

    // =========================================================================
    // getSetupTableData: グループIDなし
    // =========================================================================

    /**
     * Given: BasicTestDataParserTest/withoutGroupId.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupTableData() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と件数・テーブル名・行数・値が等価である
     */
    @Test
    public void getSetupTableData_withoutGroupId_equivalentToExcel() {
        List<TableData> fromXls = xlsParser.getSetupTableData(DIR, "BasicTestDataParserTest/withoutGroupId");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR, "BasicTestDataParserTest/withoutGroupId");

        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            TableData xlsTable = fromXls.get(i);
            TableData yamlTable = fromYaml.get(i);
            assertThat("テーブル名が等価 [" + i + "]",
                    yamlTable.getTableName().toUpperCase(), is(xlsTable.getTableName().toUpperCase()));
            assertThat("行数が等価 [" + i + "]", yamlTable.size(), is(xlsTable.size()));
            for (int row = 0; row < xlsTable.size(); row++) {
                for (String col : xlsTable.getColumnNames()) {
                    Object xlsVal = xlsTable.getValue(row, col);
                    Object yamlVal = yamlTable.getValue(row, col);
                    assertThat("値が等価 [" + i + "][" + row + "][" + col + "]",
                            yamlVal == null ? null : yamlVal.toString(),
                            is(xlsVal == null ? null : xlsVal.toString()));
                }
            }
        }
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
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "case01[" + i + "]");
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
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "case02[" + i + "]");
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
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "case01[" + i + "]");
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
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "case02[" + i + "]");
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

        assertThat("行数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            Map<String, String> xlsRow = fromXls.get(i);
            Map<String, String> yamlRow = fromYaml.get(i);
            assertThat("キー数が等価 [" + i + "]", yamlRow.size(), is(xlsRow.size()));
            for (Map.Entry<String, String> entry : xlsRow.entrySet()) {
                assertThat("値が等価 [" + i + "][" + entry.getKey() + "]",
                        yamlRow.get(entry.getKey()), is(entry.getValue()));
            }
        }
    }

    // =========================================================================
    // invisibleTail（末尾の不可視セル）
    // =========================================================================

    /**
     * Given: BasicTestDataParserTest/invisibleTail.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap("expectedUgroup") を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である（末尾不可視セルが除去されている）
     */
    @Test
    public void getListMap_invisibleTail_equivalentToExcel() {
        List<Map<String, String>> fromXls = xlsParser.getListMap(DIR, "BasicTestDataParserTest/invisibleTail", "expectedUgroup");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR, "BasicTestDataParserTest/invisibleTail", "expectedUgroup");

        assertThat("行数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            Map<String, String> xlsRow = fromXls.get(i);
            Map<String, String> yamlRow = fromYaml.get(i);
            for (Map.Entry<String, String> entry : xlsRow.entrySet()) {
                assertThat("値が等価 [" + i + "][" + entry.getKey() + "]",
                        yamlRow.get(entry.getKey()), is(entry.getValue()));
            }
        }
    }

    /**
     * Given: BasicTestDataParserTest/invisibleTail.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedTableData() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である（末尾不可視セルが除去されている）
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
    // completedWithId / completedWithoutId
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
     * When:  YamlTestDataParser で getExpectedTableData(groupId="case01") を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void getExpectedTableData_completedWithId_case01_equivalentToExcel() {
        List<TableData> fromXls = xlsParser.getExpectedTableData(DIR, "BasicTestDataParserTest/completedWithId", "case01");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR, "BasicTestDataParserTest/completedWithId", "case01");

        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "completedWithId_case01[" + i + "]");
        }
    }

    // =========================================================================
    // ヘルパー
    // =========================================================================

    private void assertEquivalentTable(TableData xlsTable, TableData yamlTable, String label) {
        assertThat("テーブル名が等価 [" + label + "]",
                yamlTable.getTableName().toUpperCase(), is(xlsTable.getTableName().toUpperCase()));
        assertThat("行数が等価 [" + label + "]", yamlTable.size(), is(xlsTable.size()));
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
}
