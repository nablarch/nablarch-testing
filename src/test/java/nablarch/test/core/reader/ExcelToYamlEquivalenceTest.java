package nablarch.test.core.reader;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.core.dataformat.RecordDefinition;
import nablarch.test.core.batch.DBtoDBBatchSampleTest;
import nablarch.test.core.db.Daughter;
import nablarch.test.core.db.Father;
import nablarch.test.core.db.Granpa;
import nablarch.test.core.db.Son;
import nablarch.test.core.db.TableData;
import nablarch.test.core.db.TestTable;
import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.http.AbstractHttpRequestTestTemplateTest;
import nablarch.test.core.messaging.MessagingReceiveTestSupportTest;
import nablarch.test.TestSupportTest;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    private static final String DIR       = "src/test/java/nablarch/test/core/reader/";
    private static final String DIR_TEST  = "src/test/java/nablarch/test/";
    private static final String DIR_BATCH = "src/test/java/nablarch/test/core/batch/";
    private static final String DIR_DB    = "src/test/java/nablarch/test/core/db/";
    private static final String DIR_MSG   = "src/test/java/nablarch/test/core/messaging/";
    private static final String DIR_HTTP  = "src/test/java/nablarch/test/core/http/";
    private static final String DIR_CORE  = "src/test/java/nablarch/test/core/";
    private static final String DIR_STANDALONE = "src/test/java/nablarch/test/core/standalone/";
    private static final String DIR_JAVA_ROOT  = "src/test/java/";
    private static final String DIR_RES_MASTERDATA = "src/test/resources/nablarch/test/core/db/masterdata/";
    private static final String DIR_RES_MSG        = "src/test/resources/nablarch/test/core/messaging/";

    private TestDataParser xlsParser;
    private YamlTestDataParser yamlParser;

    @BeforeClass
    public static void beforeClass() {
        // BasicTestDataParserTest 用
        VariousDbTestHelper.createTable(TestTable.class);
        // 分類B: TestSupportTest
        VariousDbTestHelper.createTable(TestSupportTest.TestSupportTestTable.class);
        // 分類B: BatchRequestTestSupportTest / BatchTestCaseInfoTest
        VariousDbTestHelper.createTable(DBtoDBBatchSampleTest.BatchSample.class);
        VariousDbTestHelper.createTable(BatchSample2.class);
        // 分類B: DbAccessTestSupportTest (GRANPA / FATHER / SON / DAUGHTER)
        VariousDbTestHelper.createTable(Granpa.class);
        VariousDbTestHelper.createTable(Father.class);
        VariousDbTestHelper.createTable(Son.class);
        VariousDbTestHelper.createTable(Daughter.class);
        // 分類B: MessagingReceiveTestSupportTest
        VariousDbTestHelper.createTable(MessagingReceiveTestSupportTest.ReceiveTest.class);
        // 分類B: AbstractHttpRequestTestTemplateTest
        VariousDbTestHelper.createTable(AbstractHttpRequestTestTemplateTest.SearchResultAssertTest.class);
        VariousDbTestHelper.createTable(AbstractHttpRequestTestTemplateTest.CrlfTest.class);
        // 分類B: resources/messaging — USERS / SYSTEM_ACCOUNT / ID_GENERATE
        VariousDbTestHelper.createTable(Users.class);
        VariousDbTestHelper.createTable(SystemAccount.class);
        VariousDbTestHelper.createTable(IdGenerate.class);
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
    // 分類B: DB接続必要 — TestSupportTest.xls
    // =========================================================================

    @Test
    public void getSetupTableData_testSupportTest_withoutGroupId_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_TEST, "TestSupportTest/withoutGroupId");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_TEST, "TestSupportTest/withoutGroupId");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "TestSupportTest/withoutGroupId[" + i + "]");
        }
    }

    @Test
    public void getExpectedTableData_testSupportTest_withoutGroupId_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getExpectedTableData(DIR_TEST, "TestSupportTest/withoutGroupId");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR_TEST, "TestSupportTest/withoutGroupId");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "TestSupportTest/withoutGroupId_expected[" + i + "]");
        }
    }

    @Test
    public void getSetupTableData_testSupportTest_withGroupId_case01_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_TEST, "TestSupportTest/withGroupId", "case01");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_TEST, "TestSupportTest/withGroupId", "case01");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "TestSupportTest/withGroupId_case01[" + i + "]");
        }
    }

    @Test
    public void getSetupTableData_testSupportTest_withGroupId_case02_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_TEST, "TestSupportTest/withGroupId", "case02");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_TEST, "TestSupportTest/withGroupId", "case02");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "TestSupportTest/withGroupId_case02[" + i + "]");
        }
    }

    @Test
    public void getListMap_testSupportTest_testGetParameterMap_equivalentToExcel() {
        List<Map<String, String>> fromXls  = xlsParser.getListMap(DIR_TEST, "TestSupportTest/testGetParameterMap", "parameters");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR_TEST, "TestSupportTest/testGetParameterMap", "parameters");
        assertEquivalentListMap(fromXls, fromYaml, "TestSupportTest/testGetParameterMap[parameters]");
    }

    // =========================================================================
    // 分類B: BatchRequestTestSupportTest.xls
    // =========================================================================

    @Test
    public void getListMap_batchRequestTestSupportTest_testCompareStatus_equivalentToExcel() {
        List<Map<String, String>> fromXls  = xlsParser.getListMap(DIR_BATCH, "BatchRequestTestSupportTest/testCompareStatus", "testShots");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR_BATCH, "BatchRequestTestSupportTest/testCompareStatus", "testShots");
        assertEquivalentListMap(fromXls, fromYaml, "BatchRequestTestSupportTest/testCompareStatus[testShots]");
    }

    @Test
    public void getSetupTableData_batchRequestTestSupportTest_testCompareStatus_equivalentToExcel() {
        // testCompareStatus の setup_tables は HOGE_TABLE（空行のみ）のため filterNonEmpty で比較
        List<TableData> fromXls  = filterNonEmpty(xlsParser.getSetupTableData(DIR_BATCH, "BatchRequestTestSupportTest/testCompareStatus"));
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_BATCH, "BatchRequestTestSupportTest/testCompareStatus");
        assertThat("テーブル数が等価（非空行のみ）", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "BatchRequestTestSupportTest/testCompareStatus_setup[" + i + "]");
        }
    }

    @Test
    public void getListMap_batchRequestTestSupportTest_testExpectedLogNotFound_equivalentToExcel() {
        List<Map<String, String>> fromXls  = xlsParser.getListMap(DIR_BATCH, "BatchRequestTestSupportTest/testExpectedLogNotFound", "testShots");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR_BATCH, "BatchRequestTestSupportTest/testExpectedLogNotFound", "testShots");
        assertEquivalentListMap(fromXls, fromYaml, "BatchRequestTestSupportTest/testExpectedLogNotFound[testShots]");
    }

    @Test
    public void getSetupTableData_batchRequestTestSupportTest_testTestCasesNotFound_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_BATCH, "BatchRequestTestSupportTest/testTestCasesNotFound");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_BATCH, "BatchRequestTestSupportTest/testTestCasesNotFound");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "BatchRequestTestSupportTest/testTestCasesNotFound[" + i + "]");
        }
    }

    @Test
    public void getExpectedTableData_batchRequestTestSupportTest_testTestCasesNotFound_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getExpectedTableData(DIR_BATCH, "BatchRequestTestSupportTest/testTestCasesNotFound");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR_BATCH, "BatchRequestTestSupportTest/testTestCasesNotFound");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "BatchRequestTestSupportTest/testTestCasesNotFound_expected[" + i + "]");
        }
    }

    @Test
    public void getSetupTableData_batchRequestTestSupportTest_testExpectedLogNotFound_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_BATCH, "BatchRequestTestSupportTest/testExpectedLogNotFound");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_BATCH, "BatchRequestTestSupportTest/testExpectedLogNotFound");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "BatchRequestTestSupportTest/testExpectedLogNotFound_setup[" + i + "]");
        }
    }

    @Test
    public void getExpectedTableData_batchRequestTestSupportTest_testExpectedLogNotFound_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getExpectedTableData(DIR_BATCH, "BatchRequestTestSupportTest/testExpectedLogNotFound");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR_BATCH, "BatchRequestTestSupportTest/testExpectedLogNotFound");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "BatchRequestTestSupportTest/testExpectedLogNotFound_expected[" + i + "]");
        }
    }

    // =========================================================================
    // 分類B: DBtoDBBatchSampleTest.xls
    // =========================================================================

    @Test
    public void getListMap_dBtoDBBatchSampleTest_testExecute_equivalentToExcel() {
        List<Map<String, String>> fromXls  = xlsParser.getListMap(DIR_BATCH, "DBtoDBBatchSampleTest/testExecute", "testShots");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR_BATCH, "DBtoDBBatchSampleTest/testExecute", "testShots");
        assertEquivalentListMap(fromXls, fromYaml, "DBtoDBBatchSampleTest/testExecute[testShots]");
    }

    @Test
    public void getSetupTableData_dBtoDBBatchSampleTest_testExecute_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_BATCH, "DBtoDBBatchSampleTest/testExecute");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_BATCH, "DBtoDBBatchSampleTest/testExecute");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "DBtoDBBatchSampleTest/testExecute_setup[" + i + "]");
        }
    }

    @Test
    public void getExpectedTableData_dBtoDBBatchSampleTest_testExecute_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getExpectedTableData(DIR_BATCH, "DBtoDBBatchSampleTest/testExecute");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR_BATCH, "DBtoDBBatchSampleTest/testExecute");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "DBtoDBBatchSampleTest/testExecute_expected[" + i + "]");
        }
    }

    // =========================================================================
    // 分類B: FileToFileBatchSampleTest.xls
    // =========================================================================

    @Test
    public void getListMap_fileToFileBatchSampleTest_testHandle_equivalentToExcel() {
        List<Map<String, String>> fromXls  = xlsParser.getListMap(DIR_BATCH, "FileToFileBatchSampleTest/testHandle", "testShots");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR_BATCH, "FileToFileBatchSampleTest/testHandle", "testShots");
        assertEquivalentListMap(fromXls, fromYaml, "FileToFileBatchSampleTest/testHandle[testShots]");
    }

    @Test
    public void getSetupFile_fileToFileBatchSampleTest_testHandle_equivalentToExcel() {
        List<DataFile> fromXls  = xlsParser.getSetupFile(DIR_BATCH, "FileToFileBatchSampleTest/testHandle");
        List<DataFile> fromYaml = yamlParser.getSetupFile(DIR_BATCH, "FileToFileBatchSampleTest/testHandle");
        assertEquivalentFileList(fromXls, fromYaml, "FileToFileBatchSampleTest/testHandle_setup_files");
    }

    @Test
    public void getExpectedFile_fileToFileBatchSampleTest_testHandle_equivalentToExcel() {
        List<DataFile> fromXls  = xlsParser.getExpectedFile(DIR_BATCH, "FileToFileBatchSampleTest/testHandle");
        List<DataFile> fromYaml = yamlParser.getExpectedFile(DIR_BATCH, "FileToFileBatchSampleTest/testHandle");
        assertEquivalentFileList(fromXls, fromYaml, "FileToFileBatchSampleTest/testHandle_expected_files");
    }

    // =========================================================================
    // 分類B: SimpleBatchSampleTest.xls
    // =========================================================================

    @Test
    public void getListMap_simpleBatchSampleTest_testExecute_equivalentToExcel() {
        List<Map<String, String>> fromXls  = xlsParser.getListMap(DIR_BATCH, "SimpleBatchSampleTest/testExecute", "testShots");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR_BATCH, "SimpleBatchSampleTest/testExecute", "testShots");
        assertEquivalentListMap(fromXls, fromYaml, "SimpleBatchSampleTest/testExecute[testShots]");
    }

    // =========================================================================
    // 分類B: DbAccessTestSupportTest.xls
    // =========================================================================

    @Test
    public void getExpectedTableData_dbAccessTestSupportTest_testAssertTableEquals_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getExpectedTableData(DIR_DB, "DbAccessTestSupportTest/testAssertTableEquals");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR_DB, "DbAccessTestSupportTest/testAssertTableEquals");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "DbAccessTestSupportTest/testAssertTableEquals[" + i + "]");
        }
    }

    @Test
    public void getSetupTableData_dbAccessTestSupportTest_testExpectedCompleteTable_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_DB, "DbAccessTestSupportTest/testExpectedCompleteTable");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_DB, "DbAccessTestSupportTest/testExpectedCompleteTable");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "DbAccessTestSupportTest/testExpectedCompleteTable_setup[" + i + "]");
        }
    }

    @Test
    public void getExpectedTableData_dbAccessTestSupportTest_testExpectedCompleteTable_equivalentToExcel() {
        // expected_complete_tables を含む（getExpectedTableData は expected_tables + expected_complete_tables を返す）
        List<TableData> fromXls  = xlsParser.getExpectedTableData(DIR_DB, "DbAccessTestSupportTest/testExpectedCompleteTable");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR_DB, "DbAccessTestSupportTest/testExpectedCompleteTable");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "DbAccessTestSupportTest/testExpectedCompleteTable_expected[" + i + "]");
        }
    }

    @Test
    public void getSetupTableData_dbAccessTestSupportTest_testSetUpDb_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_DB, "DbAccessTestSupportTest/testSetUpDb");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_DB, "DbAccessTestSupportTest/testSetUpDb");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "DbAccessTestSupportTest/testSetUpDb[" + i + "]");
        }
    }

    @Test
    public void getSetupTableData_dbAccessTestSupportTest_testSetUpDbInOrder_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_DB, "DbAccessTestSupportTest/testSetUpDbInOrder");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_DB, "DbAccessTestSupportTest/testSetUpDbInOrder");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "DbAccessTestSupportTest/testSetUpDbInOrder[" + i + "]");
        }
    }

    // =========================================================================
    // 分類B: MessagingReceiveTestSupportTest.xls
    // =========================================================================

    @Test
    public void getListMap_messagingReceiveTestSupportTest_testExtends_equivalentToExcel() {
        List<Map<String, String>> fromXls  = xlsParser.getListMap(DIR_MSG, "MessagingReceiveTestSupportTest/testExtends", "testShots");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR_MSG, "MessagingReceiveTestSupportTest/testExtends", "testShots");
        assertEquivalentListMap(fromXls, fromYaml, "MessagingReceiveTestSupportTest/testExtends[testShots]");
    }

    @Test
    public void getSetupTableData_messagingReceiveTestSupportTest_testExtends_equivalentToExcel() {
        // YAML パーサは空行テーブル（rows: []）をスキップするため、XLS 側も空行テーブルを除外して比較する
        List<TableData> fromXls  = filterNonEmpty(xlsParser.getSetupTableData(DIR_MSG, "MessagingReceiveTestSupportTest/testExtends"));
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_MSG, "MessagingReceiveTestSupportTest/testExtends");
        assertThat("テーブル数が等価（非空行のみ）", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "MessagingReceiveTestSupportTest/testExtends[" + i + "]");
        }
    }

    @Test
    public void getExpectedTableData_messagingReceiveTestSupportTest_testExtends_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getExpectedTableData(DIR_MSG, "MessagingReceiveTestSupportTest/testExtends");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR_MSG, "MessagingReceiveTestSupportTest/testExtends");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "MessagingReceiveTestSupportTest/testExtends_expected[" + i + "]");
        }
    }

    @Test
    public void getListMap_messagingReceiveTestSupportTest_testUnExtends_equivalentToExcel() {
        List<Map<String, String>> fromXls  = xlsParser.getListMap(DIR_MSG, "MessagingReceiveTestSupportTest/testUnExtends", "testShots");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR_MSG, "MessagingReceiveTestSupportTest/testUnExtends", "testShots");
        assertEquivalentListMap(fromXls, fromYaml, "MessagingReceiveTestSupportTest/testUnExtends[testShots]");
    }

    @Test
    public void getSetupTableData_messagingReceiveTestSupportTest_testUnExtends_equivalentToExcel() {
        // YAML パーサは空行テーブル（rows: []）をスキップするため、XLS 側も空行テーブルを除外して比較する
        List<TableData> fromXls  = filterNonEmpty(xlsParser.getSetupTableData(DIR_MSG, "MessagingReceiveTestSupportTest/testUnExtends"));
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_MSG, "MessagingReceiveTestSupportTest/testUnExtends");
        assertThat("テーブル数が等価（非空行のみ）", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "MessagingReceiveTestSupportTest/testUnExtends_setup[" + i + "]");
        }
    }

    @Test
    public void getExpectedTableData_messagingReceiveTestSupportTest_testUnExtends_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getExpectedTableData(DIR_MSG, "MessagingReceiveTestSupportTest/testUnExtends");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR_MSG, "MessagingReceiveTestSupportTest/testUnExtends");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "MessagingReceiveTestSupportTest/testUnExtends_expected[" + i + "]");
        }
    }

    // =========================================================================
    // 分類B: MessagingRequestTestSupportTest.xls (java/)
    // NOTE: java/ の XLS は testSuccess 〜 testExpectedMsgLackingFail を持つ（testMessagingSample なし）。
    //       testMessagingSample は resources/ の XLS にのみ存在する。
    // =========================================================================

    @Test
    public void getListMap_messagingRequestTestSupportTest_testSuccess_equivalentToExcel() {
        List<Map<String, String>> fromXls  = xlsParser.getListMap(DIR_MSG, "MessagingRequestTestSupportTest/testSuccess", "testShots");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR_MSG, "MessagingRequestTestSupportTest/testSuccess", "testShots");
        assertEquivalentListMap(fromXls, fromYaml, "MessagingRequestTestSupportTest/testSuccess[testShots]");
    }

    @Test
    public void getListMap_messagingRequestTestSupportTest_testDbAssertionFailed_equivalentToExcel() {
        List<Map<String, String>> fromXls  = xlsParser.getListMap(DIR_MSG, "MessagingRequestTestSupportTest/testDbAssertionFailed", "testShots");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR_MSG, "MessagingRequestTestSupportTest/testDbAssertionFailed", "testShots");
        assertEquivalentListMap(fromXls, fromYaml, "MessagingRequestTestSupportTest/testDbAssertionFailed[testShots]");
    }

    @Test
    public void getListMap_messagingRequestTestSupportTest_testStatusCodeFail_equivalentToExcel() {
        List<Map<String, String>> fromXls  = xlsParser.getListMap(DIR_MSG, "MessagingRequestTestSupportTest/testStatusCodeFail", "testShots");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR_MSG, "MessagingRequestTestSupportTest/testStatusCodeFail", "testShots");
        assertEquivalentListMap(fromXls, fromYaml, "MessagingRequestTestSupportTest/testStatusCodeFail[testShots]");
    }

    // =========================================================================
    // 分類B: AbstractHttpRequestTestTemplateTest.xls
    // =========================================================================

    @Test
    public void getListMap_abstractHttpRequestTestTemplateTest_testAssertRequest_equivalentToExcel() {
        List<Map<String, String>> fromXls  = xlsParser.getListMap(DIR_HTTP, "AbstractHttpRequestTestTemplateTest/testAssertRequest", "user");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR_HTTP, "AbstractHttpRequestTestTemplateTest/testAssertRequest", "user");
        assertEquivalentListMap(fromXls, fromYaml, "AbstractHttpRequestTestTemplateTest/testAssertRequest[user]");
    }

    @Test
    public void getSetupTableData_abstractHttpRequestTestTemplateTest_testAssertSqlResultSet_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_HTTP, "AbstractHttpRequestTestTemplateTest/testAssertSqlResultSet");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_HTTP, "AbstractHttpRequestTestTemplateTest/testAssertSqlResultSet");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "AbstractHttpRequestTestTemplateTest/testAssertSqlResultSet[" + i + "]");
        }
    }

    @Test
    public void getExpectedTableData_abstractHttpRequestTestTemplateTest_testAssertSqlResultSet_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getExpectedTableData(DIR_HTTP, "AbstractHttpRequestTestTemplateTest/testAssertSqlResultSet");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR_HTTP, "AbstractHttpRequestTestTemplateTest/testAssertSqlResultSet");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "AbstractHttpRequestTestTemplateTest/testAssertSqlResultSet_expected[" + i + "]");
        }
    }

    @Test
    public void getListMap_abstractHttpRequestTestTemplateTest_testGetEmptyTestCase_equivalentToExcel() {
        List<Map<String, String>> fromXls  = xlsParser.getListMap(DIR_HTTP, "AbstractHttpRequestTestTemplateTest/testGetEmptyTestCase", "testCases");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR_HTTP, "AbstractHttpRequestTestTemplateTest/testGetEmptyTestCase", "testCases");
        assertEquivalentListMap(fromXls, fromYaml, "AbstractHttpRequestTestTemplateTest/testGetEmptyTestCase[testCases]");
    }

    @Test
    public void getSetupFile_abstractHttpRequestTestTemplateTest_testUpload_equivalentToExcel() {
        List<DataFile> fromXls  = xlsParser.getSetupFile(DIR_HTTP, "AbstractHttpRequestTestTemplateTest/testUpload");
        List<DataFile> fromYaml = yamlParser.getSetupFile(DIR_HTTP, "AbstractHttpRequestTestTemplateTest/testUpload");
        assertEquivalentFileList(fromXls, fromYaml, "AbstractHttpRequestTestTemplateTest/testUpload_setup_files");
    }

    @Test
    public void getSetupTableData_abstractHttpRequestTestTemplateTest_testAssertTablesCRLF_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_HTTP, "AbstractHttpRequestTestTemplateTest/testAssertTablesCRLF");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_HTTP, "AbstractHttpRequestTestTemplateTest/testAssertTablesCRLF");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "AbstractHttpRequestTestTemplateTest/testAssertTablesCRLF[" + i + "]");
        }
    }

    // =========================================================================
    // 分類B: MultiResourceDataSetUpTest.xlsx
    // =========================================================================

    @Test
    public void getSetupFile_multiResourceDataSetUpTest_testFileAndDatabaseSetUp_equivalentToExcel() {
        List<DataFile> fromXls  = xlsParser.getSetupFile(DIR_CORE, "MultiResourceDataSetUpTest/testFileAndDatabaseSetUp");
        List<DataFile> fromYaml = yamlParser.getSetupFile(DIR_CORE, "MultiResourceDataSetUpTest/testFileAndDatabaseSetUp");
        assertEquivalentFileList(fromXls, fromYaml, "MultiResourceDataSetUpTest/testFileAndDatabaseSetUp_setup_files");
    }

    @Test
    public void getSetupTableData_multiResourceDataSetUpTest_testFileAndDatabaseSetUp_equivalentToExcel() {
        // YAML パーサは空行テーブル（rows: []）をスキップするため、XLS 側も空行テーブルを除外して比較する
        List<TableData> fromXls  = filterNonEmpty(xlsParser.getSetupTableData(DIR_CORE, "MultiResourceDataSetUpTest/testFileAndDatabaseSetUp"));
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_CORE, "MultiResourceDataSetUpTest/testFileAndDatabaseSetUp");
        assertThat("テーブル数が等価（非空行のみ）", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "MultiResourceDataSetUpTest/testFileAndDatabaseSetUp_setup[" + i + "]");
        }
    }

    @Test
    public void getExpectedTableData_multiResourceDataSetUpTest_testFileAndDatabaseSetUp_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getExpectedTableData(DIR_CORE, "MultiResourceDataSetUpTest/testFileAndDatabaseSetUp");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR_CORE, "MultiResourceDataSetUpTest/testFileAndDatabaseSetUp");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "MultiResourceDataSetUpTest/testFileAndDatabaseSetUp_expected[" + i + "]");
        }
    }

    // =========================================================================
    // 分類B: BatchTestCaseInfoTest.xls
    // =========================================================================

    @Test
    public void getSetupTableData_batchTestCaseInfoTest_testSetUpDbDouble_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_STANDALONE, "BatchTestCaseInfoTest/testSetUpDbDouble");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_STANDALONE, "BatchTestCaseInfoTest/testSetUpDbDouble");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "BatchTestCaseInfoTest/testSetUpDbDouble[" + i + "]");
        }
    }

    @Test
    public void getExpectedTableData_batchTestCaseInfoTest_testSetUpDbDouble_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getExpectedTableData(DIR_STANDALONE, "BatchTestCaseInfoTest/testSetUpDbDouble");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR_STANDALONE, "BatchTestCaseInfoTest/testSetUpDbDouble");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "BatchTestCaseInfoTest/testSetUpDbDouble_expected[" + i + "]");
        }
    }

    // =========================================================================
    // 分類B: MASTER_DATA.xls / MASTER_DATA2.xls (src/test/java/)
    // NOTE: java/ の MASTER_DATA.xls は `hoge` シートのみ（FATHER/SON/DAUGHTER/GRANPA はなし）。
    //       FATHER/SON 等は resources/ の MASTER_DATA.xls に存在する（下の resources セクションで照合）。
    // =========================================================================

    @Test
    public void getSetupTableData_masterDataJava_hoge_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_JAVA_ROOT, "MASTER_DATA/hoge");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_JAVA_ROOT, "MASTER_DATA/hoge");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "MASTER_DATA/hoge[" + i + "]");
        }
    }

    @Test
    public void getSetupTableData_masterData2Java_hoge_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_JAVA_ROOT, "MASTER_DATA2/hoge");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_JAVA_ROOT, "MASTER_DATA2/hoge");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "MASTER_DATA2/hoge[" + i + "]");
        }
    }

    // =========================================================================
    // 分類B: resources/masterdata/MASTER_DATA.xls / MASTER_DATA2.xls
    // MASTER_DATA.xls  → sheets: FATHER, SON
    // MASTER_DATA2.xls → sheets: GRANPA, DAUGHTER
    // =========================================================================

    @Test
    public void getSetupTableData_resMasterData_father_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_RES_MASTERDATA, "MASTER_DATA/FATHER");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_RES_MASTERDATA, "MASTER_DATA/FATHER");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "res/MASTER_DATA/FATHER[" + i + "]");
        }
    }

    @Test
    public void getSetupTableData_resMasterData_son_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_RES_MASTERDATA, "MASTER_DATA/SON");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_RES_MASTERDATA, "MASTER_DATA/SON");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "res/MASTER_DATA/SON[" + i + "]");
        }
    }

    @Test
    public void getSetupTableData_resMasterData2_daughter_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_RES_MASTERDATA, "MASTER_DATA2/DAUGHTER");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_RES_MASTERDATA, "MASTER_DATA2/DAUGHTER");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "res/MASTER_DATA2/DAUGHTER[" + i + "]");
        }
    }

    @Test
    public void getSetupTableData_resMasterData2_granpa_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getSetupTableData(DIR_RES_MASTERDATA, "MASTER_DATA2/GRANPA");
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_RES_MASTERDATA, "MASTER_DATA2/GRANPA");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "res/MASTER_DATA2/GRANPA[" + i + "]");
        }
    }

    // =========================================================================
    // 分類B: resources/messaging/MessagingRequestTestSupportTest.xls
    // =========================================================================

    @Test
    public void getListMap_resMessagingRequestTestSupportTest_testMessagingSample_equivalentToExcel() {
        List<Map<String, String>> fromXls  = xlsParser.getListMap(DIR_RES_MSG, "MessagingRequestTestSupportTest/testMessagingSample", "testCases");
        List<Map<String, String>> fromYaml = yamlParser.getListMap(DIR_RES_MSG, "MessagingRequestTestSupportTest/testMessagingSample", "testCases");
        assertEquivalentListMap(fromXls, fromYaml, "res/MessagingRequestTestSupportTest/testMessagingSample[testCases]");
    }

    @Test
    public void getSetupTableData_resMessagingRequestTestSupportTest_testMessagingSample_equivalentToExcel() {
        // YAML パーサは空行テーブル（rows: []）をスキップするため、XLS 側も空行テーブルを除外して比較する
        List<TableData> fromXls  = filterNonEmpty(xlsParser.getSetupTableData(DIR_RES_MSG, "MessagingRequestTestSupportTest/testMessagingSample", "input"));
        List<TableData> fromYaml = yamlParser.getSetupTableData(DIR_RES_MSG, "MessagingRequestTestSupportTest/testMessagingSample", "input");
        assertThat("テーブル数が等価（非空行のみ）", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "res/MessagingRequestTestSupportTest/testMessagingSample_setup[" + i + "]");
        }
    }

    @Test
    public void getExpectedTableData_resMessagingRequestTestSupportTest_testMessagingSample_equivalentToExcel() {
        List<TableData> fromXls  = xlsParser.getExpectedTableData(DIR_RES_MSG, "MessagingRequestTestSupportTest/testMessagingSample", "case1");
        List<TableData> fromYaml = yamlParser.getExpectedTableData(DIR_RES_MSG, "MessagingRequestTestSupportTest/testMessagingSample", "case1");
        assertThat("テーブル数が等価", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentTable(fromXls.get(i), fromYaml.get(i), "res/MessagingRequestTestSupportTest/testMessagingSample_expected_case1[" + i + "]");
        }
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

    /**
     * TableData リストから空行（rows=0）のテーブルを除外して返す。
     *
     * <p>
     * YAML パーサは {@code rows: []} の空行テーブルをスキップするため、
     * XLS パーサの結果と比較する際に空行テーブルを除外する必要がある。
     * </p>
     */
    private List<TableData> filterNonEmpty(List<TableData> tables) {
        List<TableData> result = new java.util.ArrayList<>();
        for (TableData t : tables) {
            if (t.size() > 0) {
                result.add(t);
            }
        }
        return result;
    }

    /**
     * List&lt;DataFile&gt; の等価性を確認する。
     * ファイル数・レイアウト（ディレクティブ・レコード型・フィールド名）・データ行数・値が等価であることを検証する。
     */
    private void assertEquivalentFileList(List<DataFile> fromXls, List<DataFile> fromYaml, String label) {
        assertThat("ファイル数が等価 [" + label + "]", fromYaml.size(), is(fromXls.size()));
        for (int i = 0; i < fromXls.size(); i++) {
            assertEquivalentFile(fromXls.get(i), fromYaml.get(i), label + "[" + i + "]");
        }
    }

    /**
     * DataFile の等価性を確認する。
     * レイアウト（ディレクティブ・レコード定義数・各レコードのタイプ名・フィールド名）と
     * データレコード（行数・各フィールドの文字列表現）を検証する。
     */
    private void assertEquivalentFile(DataFile xlsFile, DataFile yamlFile, String label) {
        LayoutDefinition xlsLayout  = xlsFile.createLayout();
        LayoutDefinition yamlLayout = yamlFile.createLayout();

        assertThat("ディレクティブが等価 [" + label + "]",
                yamlLayout.getDirective(), is(xlsLayout.getDirective()));

        List<RecordDefinition> xlsRecs  = xlsLayout.getRecords();
        List<RecordDefinition> yamlRecs = yamlLayout.getRecords();
        assertThat("レコード定義数が等価 [" + label + "]", yamlRecs.size(), is(xlsRecs.size()));
        for (int r = 0; r < xlsRecs.size(); r++) {
            RecordDefinition xlsRec  = xlsRecs.get(r);
            RecordDefinition yamlRec = yamlRecs.get(r);
            assertThat("レコードタイプ名が等価 [" + label + "][r=" + r + "]",
                    yamlRec.getTypeName(), is(xlsRec.getTypeName()));
            assertThat("フィールド数が等価 [" + label + "][r=" + r + "]",
                    yamlRec.getFields().size(), is(xlsRec.getFields().size()));
            for (int f = 0; f < xlsRec.getFields().size(); f++) {
                assertThat("フィールド名が等価 [" + label + "][r=" + r + "][f=" + f + "]",
                        yamlRec.getFields().get(f).getName(), is(xlsRec.getFields().get(f).getName()));
            }
        }

        List<DataRecord> xlsRecords  = xlsFile.toDataRecords();
        List<DataRecord> yamlRecords = yamlFile.toDataRecords();
        assertThat("データ行数が等価 [" + label + "]", yamlRecords.size(), is(xlsRecords.size()));
        for (int i = 0; i < xlsRecords.size(); i++) {
            DataRecord xlsRow  = xlsRecords.get(i);
            DataRecord yamlRow = yamlRecords.get(i);
            for (Map.Entry<String, Object> entry : xlsRow.entrySet()) {
                assertThat("値が等価 [" + label + "][row=" + i + "][" + entry.getKey() + "]",
                        String.valueOf(yamlRow.get(entry.getKey())),
                        is(String.valueOf(entry.getValue())));
            }
        }
    }

    // =========================================================================
    // テスト用エンティティ定義（分類B で必要なテーブル）
    // =========================================================================

    /** BatchTestCaseInfoTest.xls で参照される BATCH_SAMPLE2 テーブル */
    @Entity
    @Table(name = "BATCH_SAMPLE2")
    public static class BatchSample2 {
        public BatchSample2() {}
        @Id
        @Column(name = "ID", length = 5, nullable = false)
        public String id;
        @Column(name = "COUNTER", length = 5)
        public Long counter;
        @Column(name = "MESSAGE", length = 50)
        public String message;
    }

    /** resources/messaging テストで参照される USERS テーブル */
    @Entity
    @Table(name = "USERS")
    public static class Users {
        public Users() {}
        @Id
        @Column(name = "USER_ID", length = 10, nullable = false)
        public String userId;
        @Column(name = "USER_NAME", length = 50)
        public String userName;
    }

    /** resources/messaging テストで参照される SYSTEM_ACCOUNT テーブル */
    @Entity
    @Table(name = "SYSTEM_ACCOUNT")
    public static class SystemAccount {
        public SystemAccount() {}
        @Id
        @Column(name = "ID", length = 10, nullable = false)
        public String id;
        @Column(name = "NAME", length = 50)
        public String name;
        @Column(name = "REMARKS", length = 100)
        public String remarks;
    }

    /** resources/messaging テストで参照される ID_GENERATE テーブル（MessagingReceiveTestSupportTest.IdGenerate と同構造） */
    @Entity
    @Table(name = "ID_GENERATE")
    public static class IdGenerate {
        public IdGenerate() {}
        @Id
        @Column(name = "ID", length = 2, nullable = false)
        public String id;
        @Column(name = "NO", length = 10, nullable = false)
        public Long no;
    }
}
