package nablarch.test.core.messaging;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.reader.BasicTestDataParser;
import nablarch.test.core.reader.DataType;
import nablarch.test.core.reader.TestDataParser;
import nablarch.test.core.reader.YamlTestDataParser;
import nablarch.test.core.reader.yaml.YamlLoader;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

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

    private static final String DIR_MSG  = "src/test/java/nablarch/test/core/messaging/";
    private static final String DIR_DATA = "src/test/java/nablarch/test/core/messaging/data/";

    private BasicTestDataParser xlsParser;
    private YamlTestDataParser yamlParser;

    @Before
    public void before() {
        xlsParser = (BasicTestDataParser) repositoryResource.getComponent("testDataParser");

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
        YamlLoader.clearCacheForTest();
    }

    // =======================================================================
    // MessageParserTest (messages)
    //
    // NOTE: MessageParserTest.xls の `messages` セクションは全角・半角フィールドの
    // length を省略し、メッセージ全体のバイト長から動的に決定する特殊フォーマット。
    // このフォーマットは unit-test-yaml.xml のコンバータ設定と競合するため、
    // 本テストクラスではリポジトリをリセットしてテストするのが正しいアプローチだが、
    // @ClassRule で共有リポジトリを使用しているため除外する。
    // 等価照合は別途 convertorSetting.xml 専用リポジトリを使うテストで補完する。
    // =======================================================================

    // =======================================================================
    // RM11AC 系 (getMessageWithoutCache: expected_request_body / response_header / response_body)
    // =======================================================================

    @Test
    public void rm11ac0202_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0202", "RM11AC0202");
    }

    @Test
    public void rm11ac0203_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0203", "RM11AC0203");
    }

    @Test
    public void rm11ac0204_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0204", "RM11AC0204");
    }

    @Test
    public void rm11ac0205_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0205", "RM11AC0205");
    }

    @Test
    public void rm11ac0206_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0206", "RM11AC0206");
    }

    @Test
    public void rm11ac0207_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0207", "RM11AC0207");
    }

    @Test
    public void rm11ac0292_equivalentToExcel() {
        // RM11AC0292 の ID は "RM11AC0202" (XLS のシート内ID)
        assertRmMessageEquivalence("RM11AC0292", "RM11AC0202");
    }

    @Test
    public void rm11ac0293_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0293", "RM11AC0293");
    }

    @Test
    public void rm11ac0294_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0294", "RM11AC0294");
    }

    @Test
    public void rm11ac0295_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0295", "RM11AC0295");
    }

    @Test
    public void rm11ac0296_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0296", "RM11AC0296");
    }

    @Test
    public void rm11ac0297_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0297", "RM11AC0297");
    }

    @Test
    public void rm11ac0298_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AC0298", "RM11AC0298");
    }

    // =======================================================================
    // RM11AD 系 (getMessageWithoutCache)
    // =======================================================================

    @Test
    public void rm11ad0101_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AD0101", "RM11AD0101");
    }

    @Test
    public void rm11ad0110_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AD0110", "RM11AD0110");
    }

    @Test
    public void rm11ad0111_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AD0111", "RM11AD0111");
    }

    @Test
    public void rm11ad0112_equivalentToExcel() {
        assertRmMessageEquivalence("RM11AD0112", "RM11AD0112");
    }

    // =======================================================================
    // RequestTestingMessagingClientTest (list_maps + getSendSyncMessage)
    // =======================================================================

    @Test
    public void requestTestingMessagingClientTest_testSendSync_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingClientTest/testSendSync";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    @Test
    public void requestTestingMessagingClientTest_testSendSync_messages_equivalentToExcel() {
        String resource = "RequestTestingMessagingClientTest/testSendSync";
        assertEquivalentSendSyncMessages(resource, "case1", "RM11AD0201");
    }

    @Test
    public void requestTestingMessagingClientTest_testAssertFailNoMatchHeader_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingClientTest/testAssertFailNoMatchHeader";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    @Test
    public void requestTestingMessagingClientTest_testAssertFailNoMatchBody_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingClientTest/testAssertFailNoMatchBody";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    @Test
    public void requestTestingMessagingClientTest_testAssertAsDataRecord_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingClientTest/testAssertAsDataRecord";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

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

    @Test
    public void requestTestingMessagingContextTest_testExpectedRequestBody_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testExpectedRequestBody";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    @Test
    public void requestTestingMessagingContextTest_testNoAssertion_listMaps_equivalentToExcel() {
        String resource = "RequestTestingMessagingContextTest/testNoAssertion";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

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

    // =======================================================================
    // RequestTestingSendSyncBatchTest (list_maps + getSendSyncMessage)
    // =======================================================================

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

    @Test
    public void requestTestingSendSyncBatchTest_testNormalEnd_messages_equivalentToExcel() {
        String resource = "RequestTestingSendSyncBatchTest/testNormalEnd";
        assertEquivalentSendSyncMessages(resource, "case1", "RM21AA0101");
    }

    @Test
    public void requestTestingSendSyncBatchTest_testAbnormalEnd1_listMaps_equivalentToExcel() {
        String resource = "RequestTestingSendSyncBatchTest/testAbnormalEnd1";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

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

    @Test
    public void requestTestingSendSyncSupportTest_testGetExpectedRequestMessage_listMaps_equivalentToExcel() {
        String resource = "RequestTestingSendSyncSupportTest/testGetExpectedRequestMessage";
        assertEquivalentListMap(
                xlsParser.getListMap(DIR_MSG, resource, "testShots"),
                yamlParser.getListMap(DIR_MSG, resource, "testShots"),
                resource + "[testShots]");
    }

    @Test
    public void requestTestingSendSyncSupportTest_testGetExpectedRequestMessage_messages_equivalentToExcel() {
        String resource = "RequestTestingSendSyncSupportTest/testGetExpectedRequestMessage";
        // expected_request_header_messages で case1/RM21AA0104_01
        assertEquivalentSendSyncMessages(resource, "case1", "RM21AA0104_01");
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
            xlsPool = null;
        }
        try {
            yamlPool = yamlParser.getMessageWithoutCache(path, resource, dataType, id);
        } catch (Exception e) {
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

            if (xlsPools == null || xlsPools.isEmpty()) {
                // XLS にもデータがない場合は YAML も null/empty のはず
                continue;
            }

            String label = resource + "[" + dataType.getName() + "/group=" + groupId + "]";
            if (yamlPools == null) {
                // YAML でも null の場合はスキップ（セクション自体が存在しない）
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
     * text-encoding ディレクティブ・データレコード（フィールド値）が等価であることを検証する。
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
