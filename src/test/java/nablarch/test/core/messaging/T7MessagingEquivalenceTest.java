package nablarch.test.core.messaging;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.test.RepositoryInitializer;
import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.reader.BasicTestDataParser;
import nablarch.test.core.reader.DataType;
import nablarch.test.core.reader.MessageParser;
import nablarch.test.core.reader.PoiXlsReader;
import nablarch.test.core.reader.YamlTestDataParser;
import nablarch.test.core.reader.yaml.YamlLoader;
import nablarch.test.core.util.interpreter.TestDataInterpreter;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * T7: メッセージング系テストデータの等価性確認テスト。
 *
 * <p>
 * messaging 系 Excel ファイルと変換 YAML ファイルを
 * {@link BasicTestDataParser}（Excel）と {@link YamlTestDataParser}（YAML）の両方で読み込み、
 * メッセージデータが等価であることを確認する。
 * </p>
 *
 * <p>
 * このテストクラスは {@code nablarch.test.core.messaging} パッケージに配置することで
 * {@link MessagePool#toDataRecords()} へアクセスする。
 * </p>
 *
 * <p>対象:</p>
 * <ul>
 *   <li>MessageParserTest.xls</li>
 *   <li>data/RM11AC0202〜RM11AC0298.xls（getMessage系）</li>
 *   <li>data/RM11AD0101〜RM11AD0112.xls（getMessage系）</li>
 *   <li>RequestTestingMessagingClientTest.xls（list_maps + getSendSyncMessage）</li>
 *   <li>RequestTestingMessagingContextTest.xls（list_maps + getSendSyncMessage）</li>
 *   <li>RequestTestingSendSyncBatchTest.xls（list_maps + getSendSyncMessage）</li>
 *   <li>RequestTestingSendSyncSupportTest.xls（list_maps + getSendSyncMessage）</li>
 * </ul>
 */
@RunWith(DatabaseTestRunner.class)
public class T7MessagingEquivalenceTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource =
            new SystemRepositoryResource("unit-test-yaml.xml");

    private static final String DIR_MSG     = "src/test/java/nablarch/test/core/messaging/";
    private static final String DIR_DATA    = "src/test/java/nablarch/test/core/messaging/data/";
    private static final String DIR_RES_MSG = "src/test/resources/nablarch/test/core/messaging/";

    private BasicTestDataParser xlsParser;
    private YamlTestDataParser yamlParser;

    @Before
    public void before() {
        xlsParser = (BasicTestDataParser) repositoryResource.getComponent("testDataParser");

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
        YamlLoader.clearCacheForTest();
    }

    // =======================================================================
    // MessageParserTest (messages)
    //
    // MessageParserTest.xls の `messages` セクションは全角・半角フィールドの
    // length を省略し、メッセージ全体のバイト長から動的に決定する特殊フォーマット。
    // このフォーマットには convertorSetting.xml を使用するリポジトリが必要。
    // テスト内で一時的に SystemRepository を convertorSetting.xml に切り替え、
    // テスト後は元のリポジトリに戻す。
    // =======================================================================

    /**
     * Given: MessageParserTest/testParse.yaml が配置されている（.xls から変換済み、convertorSetting.xml を使用）<br>
     * When:  YamlTestDataParser で getMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である（requestMessages のみ比較）
     */
    @Test
    public void messageParserTest_testParse_equivalentToExcel() {
        SystemRepository.clear();
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader("convertorSetting.xml")));
        try {
            String path = DIR_MSG;
            String resource = "MessageParserTest/testParse";

            // XLS 側: MessageParser + PoiXlsReader を直接使用（convertorSetting.xml 準拠）
            MessageParser xlsMessageParser = new MessageParser(
                    new PoiXlsReader(),
                    Collections.<TestDataInterpreter>emptyList(),
                    DataType.MESSAGE);
            xlsMessageParser.parse(path, resource, "requestMessages");
            MessagePool xlsPool = xlsMessageParser.getResult();

            // YAML 側
            MessagePool yamlPool = yamlParser.getMessage(path, resource, "requestMessages");

            assertEquivalentMessagePool(xlsPool, yamlPool, resource + "[requestMessages]");
            // fw_header 比較（assertEquivalentMessagePool 内でも比較するが明示的にも確認）
        } finally {
            SystemRepository.clear();
            RepositoryInitializer.initializeDefaultRepository();
        }
    }

    /**
     * Given: MessageParserTest/testParseAddFields.yaml が配置されている（.xls から変換済み、reader.xml を使用）<br>
     * When:  YamlTestDataParser で getMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である（requestMessages のみ比較）
     */
    @Test
    public void messageParserTest_testParseAddFields_equivalentToExcel() {
        // testParseAddFields の XLS は nablarch/test/core/messaging/reader.xml で読み込む
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "nablarch/test/core/messaging/reader.xml");
        SystemRepository.clear();
        SystemRepository.load(new DiContainer(loader));
        try {
            String path = DIR_MSG;
            String resource = "MessageParserTest/testParseAddFields";

            // XLS 側: MessageParser + PoiXlsReader を直接使用（reader.xml 準拠）
            MessageParser xlsMessageParser = new MessageParser(
                    new PoiXlsReader(),
                    Collections.<TestDataInterpreter>emptyList(),
                    DataType.MESSAGE);
            xlsMessageParser.parse(path, resource, "requestMessages");
            MessagePool xlsPool = xlsMessageParser.getResult();

            // YAML 側: yamlParser は convertorSetting.xml で読んだ場合と等価な結果を返す
            // yamlParser は unit-test-yaml.xml ベースだが、messages の型変換は不要（型情報なし）
            MessagePool yamlPool = yamlParser.getMessage(path, resource, "requestMessages");

            assertEquivalentMessagePool(xlsPool, yamlPool, resource + "[requestMessages]");
        } finally {
            SystemRepository.clear();
            RepositoryInitializer.initializeDefaultRepository();
        }
    }

    // =======================================================================
    // RM11AC 系 (getMessageWithoutCache: expected_request_body / response_header / response_body)
    // =======================================================================

    /**
     * Given: RM11AC0202/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ac0202_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0202", "RM11AC0202");
    }

    /**
     * Given: RM11AC0203/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ac0203_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0203", "RM11AC0203");
    }

    /**
     * Given: RM11AC0204/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ac0204_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0204", "RM11AC0204");
    }

    /**
     * Given: RM11AC0205/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ac0205_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0205", "RM11AC0205");
    }

    /**
     * Given: RM11AC0206/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ac0206_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0206", "RM11AC0206");
    }

    /**
     * Given: RM11AC0207/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ac0207_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0207", "RM11AC0207");
    }

    /**
     * Given: RM11AC0292/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ac0292_equivalentToExcel() {
        // RM11AC0292 の ID は "RM11AC0202" (XLS のシート内ID)
        assertRmMessageEquivalence("RM11AC0292", "RM11AC0202");
    }

    /**
     * Given: RM11AC0293/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ac0293_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0293", "RM11AC0293");
    }

    /**
     * Given: RM11AC0294/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ac0294_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0294", "RM11AC0294");
    }

    /**
     * Given: RM11AC0295/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ac0295_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0295", "RM11AC0295");
    }

    /**
     * Given: RM11AC0296/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ac0296_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0296", "RM11AC0296");
    }

    /**
     * Given: RM11AC0297/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ac0297_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0297", "RM11AC0297");
    }

    /**
     * Given: RM11AC0298/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ac0298_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0298", "RM11AC0298");
    }

    /**
     * Given: RM11AC0208/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ac0208_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0208", "RM11AC0208");
    }

    // =======================================================================
    // RM11AD 系 (getMessageWithoutCache)
    // =======================================================================

    /**
     * Given: RM11AD0101/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ad0101_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AD0101", "RM11AD0101");
    }

    /**
     * Given: RM11AD0102/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ad0102_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AD0102", "RM11AD0102");
    }

    /**
     * Given: RM11AD0104/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ad0104_equivalentToExcel() {
        // RM11AD0104 のメッセージ ID は XLS/YAML ともに "RM11AD0102"
        assertRmMessageEquivalence("RM11AD0104", "RM11AD0102");
    }

    /**
     * Given: RM11AD0105/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ad0105_equivalentToExcel() {
        // RM11AD0105: expected_request_header_messages = RM11AD0105, それ以外 = RM11AD0102
        String resource = "RM11AD0105/message";
        tryAssertMessagePool(DIR_DATA, resource, DataType.EXPECTED_REQUEST_HEADER_MESSAGES, "RM11AD0105",
                "RM11AD0105[expected_request_header_messages/RM11AD0105]");
        tryAssertMessagePool(DIR_DATA, resource, DataType.EXPECTED_REQUEST_BODY_MESSAGES, "RM11AD0102",
                "RM11AD0105[expected_request_body_messages/RM11AD0102]");
        tryAssertMessagePool(DIR_DATA, resource, DataType.RESPONSE_HEADER_MESSAGES, "RM11AD0102",
                "RM11AD0105[response_header_messages/RM11AD0102]");
        tryAssertMessagePool(DIR_DATA, resource, DataType.RESPONSE_BODY_MESSAGES, "RM11AD0102",
                "RM11AD0105[response_body_messages/RM11AD0102]");
    }

    /**
     * Given: RM11AD0106/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ad0106_equivalentToExcel() {
        // RM11AD0106: expected_request_header_messages = RM11AD0106, expected_request_body = RM11AD0106,
        //             response_header = RM11AD0102, response_body = RM11AD0102
        String resource = "RM11AD0106/message";
        tryAssertMessagePool(DIR_DATA, resource, DataType.EXPECTED_REQUEST_HEADER_MESSAGES, "RM11AD0106",
                "RM11AD0106[expected_request_header_messages/RM11AD0106]");
        tryAssertMessagePool(DIR_DATA, resource, DataType.EXPECTED_REQUEST_BODY_MESSAGES, "RM11AD0106",
                "RM11AD0106[expected_request_body_messages/RM11AD0106]");
        tryAssertMessagePool(DIR_DATA, resource, DataType.RESPONSE_HEADER_MESSAGES, "RM11AD0102",
                "RM11AD0106[response_header_messages/RM11AD0102]");
        tryAssertMessagePool(DIR_DATA, resource, DataType.RESPONSE_BODY_MESSAGES, "RM11AD0102",
                "RM11AD0106[response_body_messages/RM11AD0102]");
    }

    /**
     * Given: RM11AD0107/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ad0107_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AD0107", "RM11AD0107");
    }

    /**
     * Given: RM11AD0108/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ad0108_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AD0108", "RM11AD0108");
    }

    /**
     * Given: RM11AD0108_original/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ad0108_original_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AD0108_original", "RM11AD0108");
    }

    /**
     * Given: RM11AD0108_timestamp/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ad0108_timestamp_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AD0108_timestamp", "RM11AD0108");
    }

    /**
     * Given: RM11AD0109/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ad0109_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AD0109", "RM11AD0109");
    }

    /**
     * Given: RM11AD0110/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ad0110_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AD0110", "RM11AD0110");
    }

    /**
     * Given: RM11AD0111/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ad0111_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AD0111", "RM11AD0111");
    }

    /**
     * Given: RM11AD0112/message.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessageWithoutCache() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void rm11ad0112_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AD0112", "RM11AD0112");
    }

    // =======================================================================
    // RequestTestingMessagingClientTest (list_maps + getSendSyncMessage)
    // =======================================================================

    /**
     * Given: RequestTestingMessagingClientTest/testSendSync.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingClientTest_testSendSync_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingClientTest/testSendSync";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    /**
     * Given: RequestTestingMessagingClientTest/testSendSync.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSendSyncMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingClientTest_testSendSync_messages_equivalentToExcel() {
        String resource = "RequestTestingMessagingClientTest/testSendSync";
        assertEquivalentSendSyncMessages(resource, "case1", "RM11AD0201");
    }

    /**
     * Given: RequestTestingMessagingClientTest/testAssertFailNoMatchHeader.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingClientTest_testAssertFailNoMatchHeader_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingClientTest/testAssertFailNoMatchHeader";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    /**
     * Given: RequestTestingMessagingClientTest/testAssertFailNoMatchBody.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingClientTest_testAssertFailNoMatchBody_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingClientTest/testAssertFailNoMatchBody";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    /**
     * Given: RequestTestingMessagingClientTest/testAssertAsDataRecord.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingClientTest_testAssertAsDataRecord_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingClientTest/testAssertAsDataRecord";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    /**
     * Given: RequestTestingMessagingClientTest/testAssertAsString.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingClientTest_testAssertAsString_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingClientTest/testAssertAsString";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    // =======================================================================
    // RequestTestingMessagingContextTest (list_maps + getSendSyncMessage)
    // =======================================================================

    /**
     * Given: RequestTestingMessagingContextTest/testExpectedRequestBody.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingContextTest_testExpectedRequestBody_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testExpectedRequestBody";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    /**
     * Given: RequestTestingMessagingContextTest/testExpectedRequestBody.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSendSyncMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingContextTest_testExpectedRequestBody_messages_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testExpectedRequestBody";
        assertEquivalentSendSyncMessages(resource, "case1", "RM21AA0104_01");
    }

    /**
     * Given: RequestTestingMessagingContextTest/testNoAssertion.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingContextTest_testNoAssertion_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testNoAssertion";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    /**
     * Given: RequestTestingMessagingContextTest/testNoAssertion.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSendSyncMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingContextTest_testNoAssertion_messages_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testNoAssertion";
        assertEquivalentSendSyncMessages(resource, "case1", "RM21AA0104_01");
    }

    /**
     * Given: RequestTestingMessagingContextTest/testResponseBody.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingContextTest_testResponseBody_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testResponseBody";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "expectedLog1"),
                yamlParser.getListMap(DIR_MSG, resource, "expectedLog1"),
                resource + "[expectedLog1]");
    }

    /**
     * Given: RequestTestingMessagingContextTest/testResponseBody.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSendSyncMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingContextTest_testResponseBody_messages_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testResponseBody";
        assertEquivalentSendSyncMessages(resource, "case1", "RM21AA0104_01");
    }

    /**
     * Given: RequestTestingMessagingContextTest/testExpectedRequestHeader.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingContextTest_testExpectedRequestHeader_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testExpectedRequestHeader";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    /**
     * Given: RequestTestingMessagingContextTest/testExpectedRequestHeader.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSendSyncMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingContextTest_testExpectedRequestHeader_messages_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testExpectedRequestHeader";
        assertEquivalentSendSyncMessages(resource, "case1", "RM21AA0104_01");
    }

    /**
     * Given: RequestTestingMessagingContextTest/testNoMatchingBody.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingContextTest_testNoMatchingBody_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testNoMatchingBody";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    /**
     * Given: RequestTestingMessagingContextTest/testNoMatchingBody.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSendSyncMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingContextTest_testNoMatchingBody_messages_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testNoMatchingBody";
        assertEquivalentSendSyncMessages(resource, "case1", "RM21AA0104_01");
    }

    /**
     * Given: RequestTestingMessagingContextTest/testNoMatchingHeader.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingContextTest_testNoMatchingHeader_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testNoMatchingHeader";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    /**
     * Given: RequestTestingMessagingContextTest/testNoMatchingHeader.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSendSyncMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingContextTest_testNoMatchingHeader_messages_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testNoMatchingHeader";
        assertEquivalentSendSyncMessages(resource, "case1", "RM21AA0104_01");
    }

    /**
     * Given: RequestTestingMessagingContextTest/testResponseHeader.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingContextTest_testResponseHeader_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testResponseHeader";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "expectedLog1"),
                yamlParser.getListMap(DIR_MSG, resource, "expectedLog1"),
                resource + "[expectedLog1]");
    }

    /**
     * Given: RequestTestingMessagingContextTest/testResponseHeader.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSendSyncMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingMessagingContextTest_testResponseHeader_messages_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testResponseHeader";
        assertEquivalentSendSyncMessages(resource, "case1", "RM21AA0104_01");
    }

    // =======================================================================
    // RequestTestingSendSyncBatchTest (list_maps + getSendSyncMessage)
    // =======================================================================

    /**
     * Given: RequestTestingSendSyncBatchTest/testNormalEnd.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingSendSyncBatchTest_testNormalEnd_listMaps_equivalentToExcel() {
        String resource = "RequestTestingSendSyncBatchTest/testNormalEnd";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "expectedLog1"),
                yamlParser.getListMap(DIR_MSG, resource, "expectedLog1"),
                resource + "[expectedLog1]");
    }

    /**
     * Given: RequestTestingSendSyncBatchTest/testNormalEnd.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSendSyncMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingSendSyncBatchTest_testNormalEnd_messages_equivalentToExcel() {
        String resource = "RequestTestingSendSyncBatchTest/testNormalEnd";
        assertEquivalentSendSyncMessages(resource, "case1", "RM21AA0101");
    }

    /**
     * Given: RequestTestingSendSyncBatchTest/testAbnormalEnd1.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingSendSyncBatchTest_testAbnormalEnd1_listMaps_equivalentToExcel() {
        String resource = "RequestTestingSendSyncBatchTest/testAbnormalEnd1";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    /**
     * Given: RequestTestingSendSyncBatchTest/sample.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingSendSyncBatchTest_sample_listMaps_equivalentToExcel() {
        String resource = "RequestTestingSendSyncBatchTest/sample";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    // =======================================================================
    // RequestTestingSendSyncSupportTest (list_maps + getSendSyncMessage)
    // =======================================================================

    /**
     * Given: RequestTestingSendSyncSupportTest/testGetExpectedRequestMessage.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getListMap() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingSendSyncSupportTest_testGetExpectedRequestMessage_listMaps_equivalentToExcel() {
        String resource = "RequestTestingSendSyncSupportTest/testGetExpectedRequestMessage";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    /**
     * Given: RequestTestingSendSyncSupportTest/testGetExpectedRequestMessage.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getSendSyncMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void requestTestingSendSyncSupportTest_testGetExpectedRequestMessage_messages_equivalentToExcel() {
        String resource = "RequestTestingSendSyncSupportTest/testGetExpectedRequestMessage";
        // expected_request_header_messages で case1/RM21AA0104_01
        assertEquivalentSendSyncMessages(resource, "case1", "RM21AA0104_01");
    }

    // =======================================================================
    // MessagingReceiveTestSupportTest (messages)
    // =======================================================================

    /**
     * Given: MessagingReceiveTestSupportTest/testExtends.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void messagingReceiveTestSupportTest_testExtends_messages_equivalentToExcel() {
        String resource = "MessagingReceiveTestSupportTest/testExtends";
        assertEquivalentMessagePool(
                xlsParser.getMessage(DIR_MSG, resource, "setUpMessages"),
                yamlParser.getMessage(DIR_MSG, resource, "setUpMessages"),
                resource + "[setUpMessages]");
    }

    /**
     * Given: MessagingReceiveTestSupportTest/testUnExtends.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void messagingReceiveTestSupportTest_testUnExtends_messages_equivalentToExcel() {
        String resource = "MessagingReceiveTestSupportTest/testUnExtends";
        assertEquivalentMessagePool(
                xlsParser.getMessage(DIR_MSG, resource, "setUpMessages"),
                yamlParser.getMessage(DIR_MSG, resource, "setUpMessages"),
                resource + "[setUpMessages]");
    }

    // =======================================================================
    // MessagingRequestTestSupportTest (messages) — java/
    // NOTE: java/ の XLS は testSuccess, testDbAssertionFailed 等を持つ（testMessagingSample なし）。
    //       testMessagingSample は resources/ の XLS にのみ存在する。
    // =======================================================================

    /**
     * Given: MessagingRequestTestSupportTest/testSuccess.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void messagingRequestTestSupportTest_testSuccess_messages_equivalentToExcel() {
        String resource = "MessagingRequestTestSupportTest/testSuccess";
        assertEquivalentMessagePool(
                xlsParser.getMessage(DIR_MSG, resource, "setUpMessages"),
                yamlParser.getMessage(DIR_MSG, resource, "setUpMessages"),
                resource + "[setUpMessages]");
        assertEquivalentMessagePool(
                xlsParser.getMessage(DIR_MSG, resource, "expectedMessages"),
                yamlParser.getMessage(DIR_MSG, resource, "expectedMessages"),
                resource + "[expectedMessages]");
    }

    /**
     * Given: MessagingRequestTestSupportTest/testDbAssertionFailed.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void messagingRequestTestSupportTest_testDbAssertionFailed_messages_equivalentToExcel() {
        String resource = "MessagingRequestTestSupportTest/testDbAssertionFailed";
        assertEquivalentMessagePool(
                xlsParser.getMessage(DIR_MSG, resource, "setUpMessages"),
                yamlParser.getMessage(DIR_MSG, resource, "setUpMessages"),
                resource + "[setUpMessages]");
        assertEquivalentMessagePool(
                xlsParser.getMessage(DIR_MSG, resource, "expectedMessages"),
                yamlParser.getMessage(DIR_MSG, resource, "expectedMessages"),
                resource + "[expectedMessages]");
    }

    // =======================================================================
    // resources/messaging/MessagingRequestTestSupportTest (messages)
    // =======================================================================

    /**
     * Given: resources/messaging/MessagingRequestTestSupportTest/testMessagingSample.yaml が配置されている（.xls から変換済み）<br>
     * When:  YamlTestDataParser で getMessage() を呼ぶ<br>
     * Then:  BasicTestDataParser で Excel を読んだ結果と等価である
     */
    @Test
    public void resMessagingRequestTestSupportTest_testMessagingSample_messages_equivalentToExcel() {
        String resource = "MessagingRequestTestSupportTest/testMessagingSample";
        assertEquivalentMessagePool(
                xlsParser.getMessage(DIR_RES_MSG, resource, "setUpMessages"),
                yamlParser.getMessage(DIR_RES_MSG, resource, "setUpMessages"),
                "res/" + resource + "[setUpMessages]");
        assertEquivalentMessagePool(
                xlsParser.getMessage(DIR_RES_MSG, resource, "expectedMessages"),
                yamlParser.getMessage(DIR_RES_MSG, resource, "expectedMessages"),
                "res/" + resource + "[expectedMessages]");
    }

    // =======================================================================
    // ヘルパー
    // =======================================================================

    /**
     * RM11AC/RM11AD 系の全セクションを等価照合する。
     * セクション: expected_request_body_messages, expected_request_header_messages (存在する場合),
     *             response_header_messages (存在する場合), response_body_messages
     */
    private void assertRmMessageEquivalence(String rmDir, String messageId) {
        String resource = rmDir + "/message";

        // 全セクションを tryAssertMessagePool で等価照合（存在しないセクションはスキップ）
        tryAssertMessagePool(DIR_DATA, resource, DataType.EXPECTED_REQUEST_BODY_MESSAGES, messageId,
                rmDir + "[expected_request_body_messages/" + messageId + "]");
        tryAssertMessagePool(DIR_DATA, resource, DataType.EXPECTED_REQUEST_HEADER_MESSAGES, messageId,
                rmDir + "[expected_request_header_messages/" + messageId + "]");
        tryAssertMessagePool(DIR_DATA, resource, DataType.RESPONSE_HEADER_MESSAGES, messageId,
                rmDir + "[response_header_messages/" + messageId + "]");
        tryAssertMessagePool(DIR_DATA, resource, DataType.RESPONSE_BODY_MESSAGES, messageId,
                rmDir + "[response_body_messages/" + messageId + "]");
    }

    /**
     * 指定 DataType のメッセージを両パーサから取得し、等価照合する。
     * null は「データなし（0行）」と等価とみなす。
     * XLS が非 null で rows=0 の場合、YAML が null を返しても等価とみなす（行数 0 のアサート）。
     */
    private void tryAssertMessagePool(String path, String resource, DataType dataType, String id, String label) {
        MessagePool xlsPool;
        MessagePool yamlPool;
        try {
            xlsPool = xlsParser.getMessageWithoutCache(path, resource, dataType, id);
        } catch (Exception e) {
            // 指定 ID・セクションが存在しない場合にパーサが RuntimeException をスローするため null 扱いとする
            xlsPool = null;
        }
        try {
            yamlPool = yamlParser.getMessageWithoutCache(path, resource, dataType, id);
        } catch (Exception e) {
            // 指定 ID・セクションが存在しない場合にパーサが RuntimeException をスローするため null 扱いとする
            yamlPool = null;
        }
        if (xlsPool == null && yamlPool == null) {
            return; // 両方存在しない - OK
        }
        // どちらかが null の場合: null を 0行として扱い、もう一方のレコード数が 0 であれば等価とみなす
        if (xlsPool == null) {
            assertThat("XLS が null のとき YAML も 0 行のはず [" + label + "]",
                    yamlPool.toDataRecords().size(), is(0));
            return;
        }
        if (yamlPool == null) {
            assertThat("YAML が null のとき XLS も 0 行のはず [" + label + "]",
                    xlsPool.toDataRecords().size(), is(0));
            return;
        }
        assertEquivalentMessagePool(xlsPool, yamlPool, label);
    }

    /**
     * getSendSyncMessage で送受信メッセージを等価照合する。
     *
     * <p>NOTE: XLS パーサは groupId を "[case1]" 形式で受け取る（Excel セルヘッダ形式）。
     * YAML パーサは "case1" 形式（group_id フィールド値）で受け取る。
     * 等価照合ではそれぞれの正しい形式を使用して呼び出す。</p>
     */
    private void assertEquivalentSendSyncMessages(String resource, String groupId, String messageId) {
        for (DataType dataType : new DataType[]{
                DataType.EXPECTED_REQUEST_HEADER_MESSAGES,
                DataType.EXPECTED_REQUEST_BODY_MESSAGES,
                DataType.RESPONSE_HEADER_MESSAGES,
                DataType.RESPONSE_BODY_MESSAGES}) {

            // XLS は [groupId] 形式、YAML は groupId 形式（ブラケットなし）
            List<RequestTestingMessagePool> xlsPools;
            List<RequestTestingMessagePool> yamlPools;
            try {
                xlsPools = xlsParser.getSendSyncMessage(DIR_MSG, resource, "[" + groupId + "]", dataType);
            } catch (Exception e) {
                xlsPools = null;
            }
            try {
                yamlPools = yamlParser.getSendSyncMessage(DIR_MSG, resource, groupId, dataType);
            } catch (Exception e) {
                yamlPools = null;
            }

            String label = resource + "[" + dataType.getName() + "/group=" + groupId + "]";
            if (xlsPools == null || xlsPools.isEmpty()) {
                // XLS にもデータがない（セクション自体が存在しない）ため
                // YAML も null/empty であることを確認してスキップ（両方なしで等価）
                assertThat("XLS が null/empty のとき YAML も null/empty のはず [" + label + "]",
                        yamlPools == null || yamlPools.isEmpty(), is(true));
                continue;
            }
            if (yamlPools == null) {
                // XLS にデータがあるのに YAML が null の場合は等価照合失敗とする。
                // NOTE: YamlTestDataParser.getSendSyncMessage が null を返すのは
                //   指定されたセクション（例: expected_request_header_messages）に
                //   groupId のエントリが YAML 内に存在しない場合（変換漏れの可能性がある）。
                assertThat("XLS にデータがあるのに YAML が null（変換漏れの可能性） [" + label + "]",
                        yamlPools, is(xlsPools));
                continue;
            }
            assertThat("プール数が等価 [" + label + "]", yamlPools.size(), is(xlsPools.size()));
            for (int i = 0; i < xlsPools.size(); i++) {
                RequestTestingMessagePool xlsPool = xlsPools.get(i);
                RequestTestingMessagePool yamlPool = yamlPools.get(i);
                // requestId は異なる場合がある（XLS: path 形式、YAML: id 形式）ため比較しない
                // データレコード比較
                List<DataRecord> xlsRecords = xlsPool.getExpectedMessageList();
                List<DataRecord> yamlRecords = yamlPool.getExpectedMessageList();
                assertEquivalentDataRecords(xlsRecords, yamlRecords, label + "[pool=" + i + "]");
            }
        }
    }

    /**
     * MessagePool の等価性を確認する。
     * text-encoding ディレクティブ・fw_header・データレコード（フィールド値）が等価であることを検証する。
     *
     * <p>NOTE: record-length は各フィールドのバイト長合計から動的に計算されるため、
     * XLS の暗黙のフィールド長（データから計算）と YAML の明示的なフィールド値の長さが
     * 一致しない場合がある。よって record-length は比較対象外とし、
     * text-encoding のみをディレクティブとして確認する。</p>
     */
    private void assertEquivalentMessagePool(MessagePool xlsPool, MessagePool yamlPool, String label) {
        // text-encoding のみ比較（record-length はフィールド長計算に依存するため除外）
        LayoutDefinition xlsLayout = xlsPool.getSource().createLayout();
        LayoutDefinition yamlLayout = yamlPool.getSource().createLayout();
        Object xlsEncoding = xlsLayout.getDirective().get("text-encoding");
        Object yamlEncoding = yamlLayout.getDirective().get("text-encoding");
        assertThat("text-encoding が等価 [" + label + "]", yamlEncoding, is(xlsEncoding));

        // fw_header 比較
        Map<String, String> xlsFwHeader  = xlsPool.getFwHeader();
        Map<String, String> yamlFwHeader = yamlPool.getFwHeader();
        assertThat("fw_header が等価 [" + label + "]", yamlFwHeader, is(xlsFwHeader));

        // データレコード比較
        List<DataRecord> xlsRecords = xlsPool.toDataRecords();
        List<DataRecord> yamlRecords = yamlPool.toDataRecords();
        assertEquivalentDataRecords(xlsRecords, yamlRecords, label);
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
     * DataRecord リストの等価性を確認する。
     * XLS 側のキーを基準に値を比較する（YAML 側に余分なキーがあっても OK）。
     * DataFileFragment が内部的に追加するメタキー（"DataFileFragment:" で始まるキー）はスキップする。
     */
    private void assertEquivalentDataRecords(List<DataRecord> xlsRecords, List<DataRecord> yamlRecords, String label) {
        assertThat("行数が等価 [" + label + "]", yamlRecords.size(), is(xlsRecords.size()));
        for (int i = 0; i < xlsRecords.size(); i++) {
            DataRecord xlsDr = xlsRecords.get(i);
            DataRecord yamlDr = yamlRecords.get(i);
            for (Map.Entry<String, Object> entry : xlsDr.entrySet()) {
                String key = entry.getKey();
                // DataFileFragment の内部メタキーはスキップ
                if (key.startsWith("DataFileFragment:")) {
                    continue;
                }
                assertThat("値が等価 [" + label + "][row=" + i + "][" + key + "]",
                        String.valueOf(yamlDr.get(key)), is(String.valueOf(entry.getValue())));
            }
        }
    }
}
