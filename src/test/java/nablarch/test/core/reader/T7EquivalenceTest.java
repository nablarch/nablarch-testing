package nablarch.test.core.reader;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.core.dataformat.RecordDefinition;
import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.util.interpreter.TestDataInterpreter;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * T7: 等価性テストの拡充（旧 V-1 統合）。
 *
 * <p>
 * 各業務テストデータの Excel と変換 YAML が等価であることを確認する。
 * 対象は list_maps 系・ファイルデータ系の Excel ファイル。
 * messaging 系は {@link T7MessagingEquivalenceTest} で別途確認する。
 * </p>
 *
 * <p>テストデータの配置:
 * <ul>
 *   <li>EntityTestSupportTest: {@code src/test/java/nablarch/test/core/db/EntityTestSupportTest/}</li>
 *   <li>TestBeanTest:          {@code src/test/java/nablarch/test/core/entity/TestBeanTest/}</li>
 *   <li>TestEntityTest:        {@code src/test/java/nablarch/test/core/entity/TestEntityTest/}</li>
 *   <li>AbstractHttpRequestTestTemplateTest2: {@code src/test/java/nablarch/test/core/http/AbstractHttpRequestTestTemplateTest2/}</li>
 *   <li>HttpRequestTestSupportTest: {@code src/test/java/nablarch/test/core/http/HttpRequestTestSupportTest/}</li>
 *   <li>FileSupportTest:       {@code src/test/java/nablarch/test/core/file/FileSupportTest/}</li>
 *   <li>FileSupportWithDbLessTestDataParserTest: {@code src/test/java/nablarch/test/core/file/FileSupportWithDbLessTestDataParserTest/}</li>
 *   <li>VariableLengthFileParserTest: {@code src/test/java/nablarch/test/core/reader/VariableLengthFileParserTest/}</li>
 * </ul>
 * </p>
 */
@RunWith(DatabaseTestRunner.class)
public class T7EquivalenceTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource =
            new SystemRepositoryResource("unit-test-yaml.xml");

    // -----------------------------------------------------------------------
    // ディレクトリ定数
    // -----------------------------------------------------------------------

    private static final String DIR_DB   = "src/test/java/nablarch/test/core/db/";
    private static final String DIR_ENT  = "src/test/java/nablarch/test/core/entity/";
    private static final String DIR_HTTP = "src/test/java/nablarch/test/core/http/";
    private static final String DIR_FILE = "src/test/java/nablarch/test/core/file/";
    private static final String DIR_READ = "src/test/java/nablarch/test/core/reader/";

    private TestDataParser xlsParser;
    private YamlTestDataParser yamlParser;

    @Before
    public void before() {
        xlsParser = repositoryResource.getComponent("testDataParser");

        DbInfo dbInfo = repositoryResource.getComponent("dbInfo");
        DefaultValues defaultValues = new BasicDefaultValues();
        List<TestDataInterpreter> interpreters =
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

    // =======================================================================
    // EntityTestSupportTest (list_maps)
    // =======================================================================

    /**
     * Given: EntityTestSupportTest/test1.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void entityTestSupportTest_test1_entity_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/test1", "entity"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/test1", "entity"),
                "EntityTestSupportTest/test1[entity]");
    }

    /**
     * Given: EntityTestSupportTest/test1.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void entityTestSupportTest_test1_constructor_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/test1", "constructor"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/test1", "constructor"),
                "EntityTestSupportTest/test1[constructor]");
    }

    /**
     * Given: EntityTestSupportTest/testBeanValidation.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void entityTestSupportTest_testBeanValidation_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/testBeanValidation", "testShots"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/testBeanValidation", "testShots"),
                "EntityTestSupportTest/testBeanValidation[testShots]");
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/testBeanValidation", "params"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/testBeanValidation", "params"),
                "EntityTestSupportTest/testBeanValidation[params]");
    }

    /**
     * Given: EntityTestSupportTest/beanValidationWithInterpolate.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void entityTestSupportTest_beanValidationWithInterpolate_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/beanValidationWithInterpolate", "testShots"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/beanValidationWithInterpolate", "testShots"),
                "EntityTestSupportTest/beanValidationWithInterpolate[testShots]");
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/beanValidationWithInterpolate", "params"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/beanValidationWithInterpolate", "params"),
                "EntityTestSupportTest/beanValidationWithInterpolate[params]");
    }

    /**
     * Given: EntityTestSupportTest/testValidateAndConvert.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void entityTestSupportTest_testValidateAndConvert_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/testValidateAndConvert", "testShots"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/testValidateAndConvert", "testShots"),
                "EntityTestSupportTest/testValidateAndConvert[testShots]");
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/testValidateAndConvert", "params"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/testValidateAndConvert", "params"),
                "EntityTestSupportTest/testValidateAndConvert[params]");
    }

    /**
     * Given: EntityTestSupportTest/testValidateAndConvertFail.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void entityTestSupportTest_testValidateAndConvertFail_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/testValidateAndConvertFail", "testCases"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/testValidateAndConvertFail", "testCases"),
                "EntityTestSupportTest/testValidateAndConvertFail[testCases]");
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/testValidateAndConvertFail", "params"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/testValidateAndConvertFail", "params"),
                "EntityTestSupportTest/testValidateAndConvertFail[params]");
    }

    /**
     * Given: EntityTestSupportTest/testRequiredColumnAbsent.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void entityTestSupportTest_testRequiredColumnAbsent_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/testRequiredColumnAbsent", "testShots"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/testRequiredColumnAbsent", "testShots"),
                "EntityTestSupportTest/testRequiredColumnAbsent[testShots]");
    }

    /**
     * Given: EntityTestSupportTest/testCastFailure.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void entityTestSupportTest_testCastFailure_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/testCastFailure", "entity"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/testCastFailure", "entity"),
                "EntityTestSupportTest/testCastFailure[entity]");
    }

    /**
     * Given: EntityTestSupportTest/testParseFailure.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void entityTestSupportTest_testParseFailure_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/testParseFailure", "entity"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/testParseFailure", "entity"),
                "EntityTestSupportTest/testParseFailure[entity]");
    }

    /**
     * Given: EntityTestSupportTest/testInvalidInput.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void entityTestSupportTest_testInvalidInput_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/testInvalidInput", "entity"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/testInvalidInput", "entity"),
                "EntityTestSupportTest/testInvalidInput[entity]");
    }

    /**
     * Given: EntityTestSupportTest/testPrivateConstructorEntity.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void entityTestSupportTest_testPrivateConstructorEntity_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/testPrivateConstructorEntity", "entity"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/testPrivateConstructorEntity", "entity"),
                "EntityTestSupportTest/testPrivateConstructorEntity[entity]");
    }

    /**
     * Given: EntityTestSupportTest/testValidateParamsNotFound.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void entityTestSupportTest_testValidateParamsNotFound_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/testValidateParamsNotFound", "testCases"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/testValidateParamsNotFound", "testCases"),
                "EntityTestSupportTest/testValidateParamsNotFound[testCases]");
    }

    /**
     * Given: EntityTestSupportTest/testValidateTestShotsNotFound.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void entityTestSupportTest_testValidateTestShotsNotFound_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_DB, "EntityTestSupportTest/testValidateTestShotsNotFound", "params"),
                yamlParser.getListMap(DIR_DB, "EntityTestSupportTest/testValidateTestShotsNotFound", "params"),
                "EntityTestSupportTest/testValidateTestShotsNotFound[params]");
    }

    // =======================================================================
    // TestBeanTest (list_maps)
    // =======================================================================

    /**
     * Given: TestBeanTest/testCharsetAndLength.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void testBeanTest_testCharsetAndLength_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_ENT, "TestBeanTest/testCharsetAndLength", "charsetAndLength"),
                yamlParser.getListMap(DIR_ENT, "TestBeanTest/testCharsetAndLength", "charsetAndLength"),
                "TestBeanTest/testCharsetAndLength[charsetAndLength]");
    }

    /**
     * Given: TestBeanTest/testCharsetAndLengthWithGroup.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void testBeanTest_testCharsetAndLengthWithGroup_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_ENT, "TestBeanTest/testCharsetAndLengthWithGroup", "charsetAndLength"),
                yamlParser.getListMap(DIR_ENT, "TestBeanTest/testCharsetAndLengthWithGroup", "charsetAndLength"),
                "TestBeanTest/testCharsetAndLengthWithGroup[charsetAndLength]");
    }

    /**
     * testSingleValidation は {@code ${全角ひらがな,50}} 等のランダム値インタープリタを含むため、
     * Excel と YAML を別々に評価すると乱数が異なる値になり等価比較が不可能。
     * そのため行数・キー名集合のみを確認し、値は比較しない。
     */
    @Test
    public void testBeanTest_testSingleValidation_structureEquivalentToExcel() {
        assertEquivalentListMapStructureOnly(
                xlsParser.getListMap(DIR_ENT, "TestBeanTest/testSingleValidation", "singleValidation"),
                yamlParser.getListMap(DIR_ENT, "TestBeanTest/testSingleValidation", "singleValidation"),
                "TestBeanTest/testSingleValidation[singleValidation]");
    }

    /**
     * testSingleValidationWithGroup は {@code ${全角ひらがな,50}} 等のランダム値インタープリタを含むため、
     * 行数・キー名集合のみを確認する。
     */
    @Test
    public void testBeanTest_testSingleValidationWithGroup_structureEquivalentToExcel() {
        assertEquivalentListMapStructureOnly(
                xlsParser.getListMap(DIR_ENT, "TestBeanTest/testSingleValidationWithGroup", "singleValidation"),
                yamlParser.getListMap(DIR_ENT, "TestBeanTest/testSingleValidationWithGroup", "singleValidation"),
                "TestBeanTest/testSingleValidationWithGroup[singleValidation]");
    }

    /**
     * Given: TestBeanTest/withoutInterpolate1.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void testBeanTest_withoutInterpolate1_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_ENT, "TestBeanTest/withoutInterpolate1", "singleValidation"),
                yamlParser.getListMap(DIR_ENT, "TestBeanTest/withoutInterpolate1", "singleValidation"),
                "TestBeanTest/withoutInterpolate1[singleValidation]");
    }

    /**
     * Given: TestBeanTest/withoutInterpolate2.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void testBeanTest_withoutInterpolate2_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_ENT, "TestBeanTest/withoutInterpolate2", "charsetAndLength"),
                yamlParser.getListMap(DIR_ENT, "TestBeanTest/withoutInterpolate2", "charsetAndLength"),
                "TestBeanTest/withoutInterpolate2[charsetAndLength]");
    }

    // =======================================================================
    // TestEntityTest (list_maps)
    // =======================================================================

    /**
     * Given: TestEntityTest/testCharsetAndLength.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void testEntityTest_testCharsetAndLength_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_ENT, "TestEntityTest/testCharsetAndLength", "charsetAndLength"),
                yamlParser.getListMap(DIR_ENT, "TestEntityTest/testCharsetAndLength", "charsetAndLength"),
                "TestEntityTest/testCharsetAndLength[charsetAndLength]");
    }

    /**
     * Given: TestEntityTest/testCharsetAndLengthWithMessage.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void testEntityTest_testCharsetAndLengthWithMessage_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_ENT, "TestEntityTest/testCharsetAndLengthWithMessage", "charsetAndLength"),
                yamlParser.getListMap(DIR_ENT, "TestEntityTest/testCharsetAndLengthWithMessage", "charsetAndLength"),
                "TestEntityTest/testCharsetAndLengthWithMessage[charsetAndLength]");
    }

    /**
     * testSingleValidation は {@code ${全角ひらがな,50}} 等のランダム値インタープリタを含むため、
     * 行数・キー名集合のみを確認する。
     */
    @Test
    public void testEntityTest_testSingleValidation_structureEquivalentToExcel() {
        assertEquivalentListMapStructureOnly(
                xlsParser.getListMap(DIR_ENT, "TestEntityTest/testSingleValidation", "singleValidation"),
                yamlParser.getListMap(DIR_ENT, "TestEntityTest/testSingleValidation", "singleValidation"),
                "TestEntityTest/testSingleValidation[singleValidation]");
    }

    // =======================================================================
    // AbstractHttpRequestTestTemplateTest2 (list_maps)
    // =======================================================================

    /**
     * Given: AbstractHttpRequestTestTemplateTest2/testAssertRequest.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void abstractHttpRequestTestTemplateTest2_testAssertRequest_equivalentToExcel() {
        String resource = "AbstractHttpRequestTestTemplateTest2/testAssertRequest";
        for (String id : new String[]{"user", "cookie1", "testShots", "requestParams", "responseResult"}) {
            assertEquivalentListMap(
                    xlsParser.getListMap(DIR_HTTP, resource, id),
                    yamlParser.getListMap(DIR_HTTP, resource, id),
                    "AbstractHttpRequestTestTemplateTest2/testAssertRequest[" + id + "]");
        }
    }

    // =======================================================================
    // HttpRequestTestSupportTest (list_maps)
    // =======================================================================

    /**
     * Given: HttpRequestTestSupportTest/testAssertObjectPropertyEquals.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void httpRequestTestSupportTest_testAssertObjectPropertyEquals_equivalentToExcel() {
        String resource = "HttpRequestTestSupportTest/testAssertObjectPropertyEquals";
        for (String id : new String[]{"beanProps", "beanArrayProps", "beanListProps"}) {
            assertEquivalentListMap(
                    xlsParser.getListMap(DIR_HTTP, resource, id),
                    yamlParser.getListMap(DIR_HTTP, resource, id),
                    "HttpRequestTestSupportTest/testAssertObjectPropertyEquals[" + id + "]");
        }
    }

    /**
     * Given: HttpRequestTestSupportTest/testAssertObjectPropertyEquals2.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void httpRequestTestSupportTest_testAssertObjectPropertyEquals2_equivalentToExcel() {
        String resource = "HttpRequestTestSupportTest/testAssertObjectPropertyEquals2";
        for (String id : new String[]{"beanProps1", "beanArrayProps1", "beanListProps1", "nullValue"}) {
            assertEquivalentListMap(
                    xlsParser.getListMap(DIR_HTTP, resource, id),
                    yamlParser.getListMap(DIR_HTTP, resource, id),
                    "HttpRequestTestSupportTest/testAssertObjectPropertyEquals2[" + id + "]");
        }
    }

    /**
     * Given: HttpRequestTestSupportTest/testDelegatingToDbSupport.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void httpRequestTestSupportTest_testDelegatingToDbSupport_listMaps_equivalentToExcel() {
        String resource = "HttpRequestTestSupportTest/testDelegatingToDbSupport";
        for (String id : new String[]{"resultSetEquals", "rowEquals"}) {
            assertEquivalentListMap(
                    xlsParser.getListMap(DIR_HTTP, resource, id),
                    yamlParser.getListMap(DIR_HTTP, resource, id),
                    "HttpRequestTestSupportTest/testDelegatingToDbSupport[" + id + "]");
        }
    }

    /**
     * testDelegatingToDbSupport のテーブルデータは HTTP_TEST_SUPPORT_TABLE を参照するが、
     * このテーブルは TestTable クラスで作成される DB テーブルとは別物で、
     * DB 上に存在しないためカラム型情報が取得できない。
     * テーブル等価照合は ExcelToYamlEquivalenceTest（DatabaseTestRunner + DB 接続）で
     * テーブル作成後に行うことが前提のため、本クラスでは list_maps のみを確認する。
     * (See httpRequestTestSupportTest_testDelegatingToDbSupport_listMaps_equivalentToExcel)
     */

    /**
     * Given: HttpRequestTestSupportTest/testDelegatingToEntitySupport.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void httpRequestTestSupportTest_testDelegatingToEntitySupport_equivalentToExcel() {
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_HTTP, "HttpRequestTestSupportTest/testDelegatingToEntitySupport", "entity"),
                yamlParser.getListMap(DIR_HTTP, "HttpRequestTestSupportTest/testDelegatingToEntitySupport", "entity"),
                "HttpRequestTestSupportTest/testDelegatingToEntitySupport[entity]");
    }

    /**
     * Given: HttpRequestTestSupportTest/testExecute.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void httpRequestTestSupportTest_testExecute_equivalentToExcel() {
        String resource = "HttpRequestTestSupportTest/testExecute";
        for (String id : new String[]{"user", "testCases", "requestParams"}) {
            assertEquivalentListMap(
                    xlsParser.getListMap(DIR_HTTP, resource, id),
                    yamlParser.getListMap(DIR_HTTP, resource, id),
                    "HttpRequestTestSupportTest/testExecute[" + id + "]");
        }
    }

    /**
     * Given: HttpRequestTestSupportTest/testExecuteDummy.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void httpRequestTestSupportTest_testExecuteDummy_equivalentToExcel() {
        String resource = "HttpRequestTestSupportTest/testExecuteDummy";
        for (String id : new String[]{"user", "testCases", "requestParams"}) {
            assertEquivalentListMap(
                    xlsParser.getListMap(DIR_HTTP, resource, id),
                    yamlParser.getListMap(DIR_HTTP, resource, id),
                    "HttpRequestTestSupportTest/testExecuteDummy[" + id + "]");
        }
    }

    /**
     * Given: HttpRequestTestSupportTest/testPrepareHandlerQueue.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void httpRequestTestSupportTest_testPrepareHandlerQueue_equivalentToExcel() {
        String resource = "HttpRequestTestSupportTest/testPrepareHandlerQueue";
        for (String id : new String[]{"user", "testCases", "requestParams"}) {
            assertEquivalentListMap(
                    xlsParser.getListMap(DIR_HTTP, resource, id),
                    yamlParser.getListMap(DIR_HTTP, resource, id),
                    "HttpRequestTestSupportTest/testPrepareHandlerQueue[" + id + "]");
        }
    }

    /**
     * Given: HttpRequestTestSupportTest/testSimple.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void httpRequestTestSupportTest_testSimple_equivalentToExcel() {
        String resource = "HttpRequestTestSupportTest/testSimple";
        for (String id : new String[]{"user", "testCases", "requestParams"}) {
            assertEquivalentListMap(
                    xlsParser.getListMap(DIR_HTTP, resource, id),
                    yamlParser.getListMap(DIR_HTTP, resource, id),
                    "HttpRequestTestSupportTest/testSimple[" + id + "]");
        }
    }

    // =======================================================================
    // FileSupportTest (file data)
    //
    // 検証観点:
    //  - 型=日本語で正しく変換されるか（半角数字 等）
    //  - フィールド名/型の列ずれが無いか
    //  - 数値書式セルが正しく文字列化されているか（testNumberStringDecimal）
    // =======================================================================

    /**
     * Given: FileSupportTest/testSetUpFixedLengthFile.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportTest_testSetUpFixedLengthFile_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getSetupFile(DIR_FILE, "FileSupportTest/testSetUpFixedLengthFile"),
                yamlParser.getSetupFile(DIR_FILE, "FileSupportTest/testSetUpFixedLengthFile"),
                "FileSupportTest/testSetUpFixedLengthFile[setup]");
    }

    /**
     * Given: FileSupportTest/testAssertFixedLengthFile.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportTest_testAssertFixedLengthFile_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportTest/testAssertFixedLengthFile"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportTest/testAssertFixedLengthFile"),
                "FileSupportTest/testAssertFixedLengthFile[expected]");
    }

    /**
     * Given: FileSupportTest/testSetUpVariableLengthFile.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportTest_testSetUpVariableLengthFile_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getSetupFile(DIR_FILE, "FileSupportTest/testSetUpVariableLengthFile"),
                yamlParser.getSetupFile(DIR_FILE, "FileSupportTest/testSetUpVariableLengthFile"),
                "FileSupportTest/testSetUpVariableLengthFile[setup]");
    }

    /**
     * Given: FileSupportTest/testAssertVariableLengthFile.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportTest_testAssertVariableLengthFile_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportTest/testAssertVariableLengthFile"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportTest/testAssertVariableLengthFile"),
                "FileSupportTest/testAssertVariableLengthFile[expected]");
    }

    /**
     * Given: FileSupportTest/testNumberStringDecimal1.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile()/getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である（数値書式セルの文字列化検証）
     */
    @Test
    public void fileSupportTest_testNumberStringDecimal1_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getSetupFile(DIR_FILE, "FileSupportTest/testNumberStringDecimal1"),
                yamlParser.getSetupFile(DIR_FILE, "FileSupportTest/testNumberStringDecimal1"),
                "FileSupportTest/testNumberStringDecimal1[setup]");
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportTest/testNumberStringDecimal1"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportTest/testNumberStringDecimal1"),
                "FileSupportTest/testNumberStringDecimal1[expected]");
    }

    /**
     * Given: FileSupportTest/testNumberStringDecimal2.yaml が配置されている（桁数超過の数値を含む .xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser と YAML パーサの両方が同様に InvalidDataFormatException をスローする
     */
    @Test
    public void fileSupportTest_testNumberStringDecimal2_bothThrowEquivalently() {
        boolean xlsThrew = false;
        boolean yamlThrew = false;
        try {
            List<DataFile> xlsFiles = xlsParser.getSetupFile(DIR_FILE, "FileSupportTest/testNumberStringDecimal2");
            for (DataFile f : xlsFiles) {
                f.toDataRecords();
            }
        } catch (InvalidDataFormatException e) {
            xlsThrew = true;
        }
        try {
            List<DataFile> yamlFiles = yamlParser.getSetupFile(DIR_FILE, "FileSupportTest/testNumberStringDecimal2");
            for (DataFile f : yamlFiles) {
                f.toDataRecords();
            }
        } catch (InvalidDataFormatException e) {
            yamlThrew = true;
        }
        assertThat("XLS が InvalidDataFormatException をスローすること", xlsThrew, is(true));
        assertThat("YAML も同様に InvalidDataFormatException をスローすること（等価なエラー挙動）", yamlThrew, is(true));
    }

    /**
     * Given: FileSupportTest/testVariation.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile()/getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportTest_testVariation_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getSetupFile(DIR_FILE, "FileSupportTest/testVariation"),
                yamlParser.getSetupFile(DIR_FILE, "FileSupportTest/testVariation"),
                "FileSupportTest/testVariation[setup]");
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportTest/testVariation"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportTest/testVariation"),
                "FileSupportTest/testVariation[expected]");
    }

    /**
     * testVariationUTF8 はカスタムデータ型マッピング（customBasicDataTypeMapping.xml）が必要なケース。
     * 標準設定では DoubleByteCharacterString の奇数長で YAML パーサが SyntaxErrorException をスローするが、
     * XLS パーサは createLayout() 時の検証挙動が異なるためスローしない。
     * 等価照合はカスタムリポジトリ設定を伴う専用テストに委ねるため本クラスでは対象外とする。
     */

    /**
     * Given: FileSupportTest/testSetUpFixedEmptyLine.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportTest_testSetUpFixedEmptyLine_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getSetupFile(DIR_FILE, "FileSupportTest/testSetUpFixedEmptyLine"),
                yamlParser.getSetupFile(DIR_FILE, "FileSupportTest/testSetUpFixedEmptyLine"),
                "FileSupportTest/testSetUpFixedEmptyLine[setup]");
    }

    /**
     * Given: FileSupportTest/testSetUpVariableEmptyLine.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportTest_testSetUpVariableEmptyLine_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getSetupFile(DIR_FILE, "FileSupportTest/testSetUpVariableEmptyLine"),
                yamlParser.getSetupFile(DIR_FILE, "FileSupportTest/testSetUpVariableEmptyLine"),
                "FileSupportTest/testSetUpVariableEmptyLine[setup]");
    }

    /**
     * Given: FileSupportTest/testAssertEmptyLineFixed.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportTest_testAssertEmptyLineFixed_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportTest/testAssertEmptyLineFixed"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportTest/testAssertEmptyLineFixed"),
                "FileSupportTest/testAssertEmptyLineFixed[expected]");
    }

    /**
     * Given: FileSupportTest/testAssertEmptyLineVariable.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportTest_testAssertEmptyLineVariable_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportTest/testAssertEmptyLineVariable"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportTest/testAssertEmptyLineVariable"),
                "FileSupportTest/testAssertEmptyLineVariable[expected]");
    }

    /**
     * Given: FileSupportTest/testAssertEmptyLine2.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportTest_testAssertEmptyLine2_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportTest/testAssertEmptyLine2"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportTest/testAssertEmptyLine2"),
                "FileSupportTest/testAssertEmptyLine2[expected]");
    }

    /**
     * Given: FileSupportTest/testAssertEmptyLineVariable2.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportTest_testAssertEmptyLineVariable2_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportTest/testAssertEmptyLineVariable2"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportTest/testAssertEmptyLineVariable2"),
                "FileSupportTest/testAssertEmptyLineVariable2[expected]");
    }

    /**
     * Given: FileSupportTest/testAssertEmptyVariableFile.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportTest_testAssertEmptyVariableFile_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportTest/testAssertEmptyVariableFile"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportTest/testAssertEmptyVariableFile"),
                "FileSupportTest/testAssertEmptyVariableFile[expected]");
    }

    /**
     * testAssertFixedAsEmptyFail・testAssertVariableAsEmptyFail・testSetUpFixedLengthFileFail:
     * これらのシートは FileSupportTest の「エラーケース」であり、
     * XLS のフォーマット定義行（フィールド名行・型名行）が YAML の rows データとして
     * 変換されているため、XLS と YAML でレコード数が異なりレコード単位の等価照合ができない。
     * 等価照合は除外し T7.md に理由を記録する。
     */

    /**
     * Given: FileSupportTest/testSetUpVariableEmptyLine2.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportTest_testSetUpVariableEmptyLine2_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getSetupFile(DIR_FILE, "FileSupportTest/testSetUpVariableEmptyLine2"),
                yamlParser.getSetupFile(DIR_FILE, "FileSupportTest/testSetUpVariableEmptyLine2"),
                "FileSupportTest/testSetUpVariableEmptyLine2[setup]");
    }

    /**
     * Given: FileSupportTest/testFixedDuplicateName.yaml が配置されている（重複フィールド名を含む .xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser と YAML パーサの両方が同様に例外をスローする
     */
    @Test
    public void fileSupportTest_testFixedDuplicateName_bothThrowEquivalently() {
        boolean xlsThrew = false;
        boolean yamlThrew = false;
        try {
            xlsParser.getSetupFile(DIR_FILE, "FileSupportTest/testFixedDuplicateName");
        } catch (IllegalArgumentException | IllegalStateException e) {
            xlsThrew = true;
        }
        try {
            yamlParser.getSetupFile(DIR_FILE, "FileSupportTest/testFixedDuplicateName");
        } catch (IllegalArgumentException | IllegalStateException e) {
            yamlThrew = true;
        }
        assertThat("XLS が重複フィールド名エラーをスローすること", xlsThrew, is(true));
        assertThat("YAML も同様に重複フィールド名エラーをスローすること（等価なエラー挙動）", yamlThrew, is(true));
    }

    /**
     * Given: FileSupportTest/testVariableDuplicateName.yaml が配置されている（重複フィールド名を含む .xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser と YAML パーサの両方が同様に例外をスローする
     */
    @Test
    public void fileSupportTest_testVariableDuplicateName_bothThrowEquivalently() {
        boolean xlsThrew = false;
        boolean yamlThrew = false;
        try {
            xlsParser.getSetupFile(DIR_FILE, "FileSupportTest/testVariableDuplicateName");
        } catch (IllegalArgumentException | IllegalStateException e) {
            xlsThrew = true;
        }
        try {
            yamlParser.getSetupFile(DIR_FILE, "FileSupportTest/testVariableDuplicateName");
        } catch (IllegalArgumentException | IllegalStateException e) {
            yamlThrew = true;
        }
        assertThat("XLS が重複フィールド名エラーをスローすること", xlsThrew, is(true));
        assertThat("YAML も同様に重複フィールド名エラーをスローすること（等価なエラー挙動）", yamlThrew, is(true));
    }

    // =======================================================================
    // FileSupportWithDbLessTestDataParserTest (file data)
    // =======================================================================

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testSetUpFixedLengthFile.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportWithDbLessTest_testSetUpFixedLengthFile_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testSetUpFixedLengthFile"),
                yamlParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testSetUpFixedLengthFile"),
                "FileSupportWithDbLessTestDataParserTest/testSetUpFixedLengthFile[setup]");
    }

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testAssertFixedLengthFile.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportWithDbLessTest_testAssertFixedLengthFile_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testAssertFixedLengthFile"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testAssertFixedLengthFile"),
                "FileSupportWithDbLessTestDataParserTest/testAssertFixedLengthFile[expected]");
    }

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testSetUpVariableLengthFile.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportWithDbLessTest_testSetUpVariableLengthFile_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testSetUpVariableLengthFile"),
                yamlParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testSetUpVariableLengthFile"),
                "FileSupportWithDbLessTestDataParserTest/testSetUpVariableLengthFile[setup]");
    }

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testAssertVariableLengthFile.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportWithDbLessTest_testAssertVariableLengthFile_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testAssertVariableLengthFile"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testAssertVariableLengthFile"),
                "FileSupportWithDbLessTestDataParserTest/testAssertVariableLengthFile[expected]");
    }

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testNumberStringDecimal1.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile()/getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である（数値書式セルの文字列化検証）
     */
    @Test
    public void fileSupportWithDbLessTest_testNumberStringDecimal1_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testNumberStringDecimal1"),
                yamlParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testNumberStringDecimal1"),
                "FileSupportWithDbLessTestDataParserTest/testNumberStringDecimal1[setup]");
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testNumberStringDecimal1"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testNumberStringDecimal1"),
                "FileSupportWithDbLessTestDataParserTest/testNumberStringDecimal1[expected]");
    }

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testNumberStringDecimal2.yaml が配置されている（桁数超過の数値を含む .xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser と YAML パーサの両方が同様に InvalidDataFormatException をスローする
     */
    @Test
    public void fileSupportWithDbLessTest_testNumberStringDecimal2_bothThrowEquivalently() {
        boolean xlsThrew = false;
        boolean yamlThrew = false;
        try {
            List<DataFile> xlsFiles = xlsParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testNumberStringDecimal2");
            for (DataFile f : xlsFiles) {
                f.toDataRecords();
            }
        } catch (InvalidDataFormatException e) {
            xlsThrew = true;
        }
        try {
            List<DataFile> yamlFiles = yamlParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testNumberStringDecimal2");
            for (DataFile f : yamlFiles) {
                f.toDataRecords();
            }
        } catch (InvalidDataFormatException e) {
            yamlThrew = true;
        }
        assertThat("XLS が InvalidDataFormatException をスローすること", xlsThrew, is(true));
        assertThat("YAML も同様に InvalidDataFormatException をスローすること（等価なエラー挙動）", yamlThrew, is(true));
    }

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testAssertEmptyLineFixed.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportWithDbLessTest_testAssertEmptyLineFixed_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testAssertEmptyLineFixed"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testAssertEmptyLineFixed"),
                "FileSupportWithDbLessTestDataParserTest/testAssertEmptyLineFixed[expected]");
    }

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testAssertEmptyLineVariable.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportWithDbLessTest_testAssertEmptyLineVariable_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testAssertEmptyLineVariable"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testAssertEmptyLineVariable"),
                "FileSupportWithDbLessTestDataParserTest/testAssertEmptyLineVariable[expected]");
    }

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testFixedDuplicateName.yaml が配置されている（重複フィールド名を含む .xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser と YAML パーサの両方が同様に例外をスローする
     */
    @Test
    public void fileSupportWithDbLessTest_testFixedDuplicateName_bothThrowEquivalently() {
        boolean xlsThrew = false;
        boolean yamlThrew = false;
        try {
            xlsParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testFixedDuplicateName");
        } catch (IllegalArgumentException | IllegalStateException e) {
            xlsThrew = true;
        }
        try {
            yamlParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testFixedDuplicateName");
        } catch (IllegalArgumentException | IllegalStateException e) {
            yamlThrew = true;
        }
        assertThat("XLS が重複フィールド名エラーをスローすること", xlsThrew, is(true));
        assertThat("YAML も同様に重複フィールド名エラーをスローすること（等価なエラー挙動）", yamlThrew, is(true));
    }

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testVariableDuplicateName.yaml が配置されている（重複フィールド名を含む .xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser と YAML パーサの両方が同様に例外をスローする
     */
    @Test
    public void fileSupportWithDbLessTest_testVariableDuplicateName_bothThrowEquivalently() {
        boolean xlsThrew = false;
        boolean yamlThrew = false;
        try {
            xlsParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testVariableDuplicateName");
        } catch (IllegalArgumentException | IllegalStateException e) {
            xlsThrew = true;
        }
        try {
            yamlParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testVariableDuplicateName");
        } catch (IllegalArgumentException | IllegalStateException e) {
            yamlThrew = true;
        }
        assertThat("XLS が重複フィールド名エラーをスローすること", xlsThrew, is(true));
        assertThat("YAML も同様に重複フィールド名エラーをスローすること（等価なエラー挙動）", yamlThrew, is(true));
    }

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testSetUpFixedEmptyLine.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportWithDbLessTest_testSetUpFixedEmptyLine_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testSetUpFixedEmptyLine"),
                yamlParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testSetUpFixedEmptyLine"),
                "FileSupportWithDbLessTestDataParserTest/testSetUpFixedEmptyLine[setup]");
    }

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testSetUpVariableEmptyLine.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportWithDbLessTest_testSetUpVariableEmptyLine_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testSetUpVariableEmptyLine"),
                yamlParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testSetUpVariableEmptyLine"),
                "FileSupportWithDbLessTestDataParserTest/testSetUpVariableEmptyLine[setup]");
    }

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testAssertEmptyLine2.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportWithDbLessTest_testAssertEmptyLine2_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testAssertEmptyLine2"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testAssertEmptyLine2"),
                "FileSupportWithDbLessTestDataParserTest/testAssertEmptyLine2[expected]");
    }

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testAssertEmptyLineVariable2.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportWithDbLessTest_testAssertEmptyLineVariable2_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testAssertEmptyLineVariable2"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testAssertEmptyLineVariable2"),
                "FileSupportWithDbLessTestDataParserTest/testAssertEmptyLineVariable2[expected]");
    }

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testAssertEmptyVariableFile.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportWithDbLessTest_testAssertEmptyVariableFile_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testAssertEmptyVariableFile"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testAssertEmptyVariableFile"),
                "FileSupportWithDbLessTestDataParserTest/testAssertEmptyVariableFile[expected]");
    }

    /**
     * testAssertFixedAsEmptyFail・testAssertVariableAsEmptyFail・testSetUpFixedLengthFileFail
     * (FileSupportWithDbLessTestDataParserTest):
     * これらのシートは FileSupportWithDbLessTestDataParserTest の「エラーケース」であり、
     * XLS のフォーマット定義行（フィールド名行・型名行）が YAML の rows データとして
     * 変換されているため、XLS と YAML でレコード数が異なりレコード単位の等価照合ができない。
     * 等価照合は除外し T7.md に理由を記録する。
     */

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testSetUpVariableEmptyLine2.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportWithDbLessTest_testSetUpVariableEmptyLine2_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testSetUpVariableEmptyLine2"),
                yamlParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testSetUpVariableEmptyLine2"),
                "FileSupportWithDbLessTestDataParserTest/testSetUpVariableEmptyLine2[setup]");
    }

    /**
     * testVariationUTF8 はカスタムデータ型マッピング（customBasicDataTypeMapping.xml）が必要なため除外。
     * 標準設定では等価照合テストの実施が困難。T7.md に除外理由を記録する。
     */

    /**
     * Given: FileSupportWithDbLessTestDataParserTest/testVariation.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSetupFile()/getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void fileSupportWithDbLessTest_testVariation_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testVariation"),
                yamlParser.getSetupFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testVariation"),
                "FileSupportWithDbLessTestDataParserTest/testVariation[setup]");
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testVariation"),
                yamlParser.getExpectedFile(DIR_FILE, "FileSupportWithDbLessTestDataParserTest/testVariation"),
                "FileSupportWithDbLessTestDataParserTest/testVariation[expected]");
    }

    // =======================================================================
    // VariableLengthFileParserTest (file data)
    // =======================================================================

    /**
     * Given: VariableLengthFileParserTest/testEmptyRowSingleItem.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void variableLengthFileParserTest_testEmptyRowSingleItem_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_READ, "VariableLengthFileParserTest/testEmptyRowSingleItem"),
                yamlParser.getExpectedFile(DIR_READ, "VariableLengthFileParserTest/testEmptyRowSingleItem"),
                "VariableLengthFileParserTest/testEmptyRowSingleItem[expected]");
    }

    /**
     * Given: VariableLengthFileParserTest/testEmptyRowMultiItems.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getExpectedFile() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void variableLengthFileParserTest_testEmptyRowMultiItems_equivalentToExcel() {
        assertEquivalentFileList(
                xlsParser.getExpectedFile(DIR_READ, "VariableLengthFileParserTest/testEmptyRowMultiItems"),
                yamlParser.getExpectedFile(DIR_READ, "VariableLengthFileParserTest/testEmptyRowMultiItems"),
                "VariableLengthFileParserTest/testEmptyRowMultiItems[expected]");
    }

    // =======================================================================
    // ヘルパー
    // =======================================================================

    /**
     * List&lt;Map&lt;String, String&gt;&gt; の構造のみを確認する（行数・キー名集合）。
     * ランダム値インタープリタを含むシートに使用する。
     */
    private void assertEquivalentListMapStructureOnly(
            List<Map<String, String>> fromXls,
            List<Map<String, String>> fromYaml,
            String label) {
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
        }
    }

    /**
     * List&lt;Map&lt;String, String&gt;&gt; の等価性を確認する。
     */
    private void assertEquivalentListMap(
            List<Map<String, String>> fromXls,
            List<Map<String, String>> fromYaml,
            String label) {
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
     * DataFile リストの等価性を確認する。
     * ファイル数・ディレクティブ・レコード定義・データ行が等価であることを検証する。
     */
    private void assertEquivalentFileList(List<DataFile> xlsFiles, List<DataFile> yamlFiles, String label) {
        assertThat("ファイル数が等価 [" + label + "]", yamlFiles.size(), is(xlsFiles.size()));
        for (int f = 0; f < xlsFiles.size(); f++) {
            assertEquivalentFile(xlsFiles.get(f), yamlFiles.get(f), label + "[file=" + f + "]");
        }
    }

    /**
     * DataFile の等価性を確認する。
     * ディレクティブ・レコード定義・データ行が等価であることを検証する。
     *
     * <p>検証観点:</p>
     * <ul>
     *   <li>ディレクティブ（text-encoding / record-separator）</li>
     *   <li>レコード種別数・レコード種別名</li>
     *   <li>フィールド数・フィールド名（型=日本語の列ずれ検出）</li>
     *   <li>データ行数・全フィールドの値（数値書式の文字列化確認）</li>
     * </ul>
     */
    private void assertEquivalentFile(DataFile xlsFile, DataFile yamlFile, String label) {
        LayoutDefinition xlsLayout = xlsFile.createLayout();
        LayoutDefinition yamlLayout = yamlFile.createLayout();

        // ディレクティブ
        assertThat("ディレクティブが等価 [" + label + "]",
                yamlLayout.getDirective(), is(xlsLayout.getDirective()));

        // レコード定義
        List<RecordDefinition> xlsRecs = xlsLayout.getRecords();
        List<RecordDefinition> yamlRecs = yamlLayout.getRecords();
        assertThat("レコード数が等価 [" + label + "]", yamlRecs.size(), is(xlsRecs.size()));
        for (int r = 0; r < xlsRecs.size(); r++) {
            RecordDefinition xlsRec = xlsRecs.get(r);
            RecordDefinition yamlRec = yamlRecs.get(r);
            assertThat("レコードタイプ名が等価 [" + label + "][r=" + r + "]",
                    yamlRec.getTypeName(), is(xlsRec.getTypeName()));
            List<FieldDefinition> xlsFields = xlsRec.getFields();
            List<FieldDefinition> yamlFields = yamlRec.getFields();
            assertThat("フィールド数が等価 [" + label + "][r=" + r + "]",
                    yamlFields.size(), is(xlsFields.size()));
            for (int fi = 0; fi < xlsFields.size(); fi++) {
                assertThat("フィールド名が等価 [" + label + "][r=" + r + "][f=" + fi + "]",
                        yamlFields.get(fi).getName(), is(xlsFields.get(fi).getName()));
            }
        }

        // データ行
        List<DataRecord> xlsDataRecords = xlsFile.toDataRecords();
        List<DataRecord> yamlDataRecords = yamlFile.toDataRecords();
        assertThat("データ行数が等価 [" + label + "]", yamlDataRecords.size(), is(xlsDataRecords.size()));
        for (int i = 0; i < xlsDataRecords.size(); i++) {
            DataRecord xlsDr = xlsDataRecords.get(i);
            DataRecord yamlDr = yamlDataRecords.get(i);
            assertThat("データキー数が等価 [" + label + "][row=" + i + "]",
                    yamlDr.size(), is(xlsDr.size()));
            for (Map.Entry<String, Object> entry : xlsDr.entrySet()) {
                String key = entry.getKey();
                assertThat("値が等価 [" + label + "][row=" + i + "][" + key + "]",
                        toComparableString(yamlDr.get(key)), is(toComparableString(entry.getValue())));
            }
        }
    }

    /**
     * 値を比較可能な文字列に変換する。
     * byte[] は内容に基づいた文字列（Arrays.toString）に変換する。
     */
    private String toComparableString(Object value) {
        if (value instanceof byte[]) {
            return Arrays.toString((byte[]) value);
        }
        return String.valueOf(value);
    }
}
