package nablarch.test.core.reader;

import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.db.TableData;
import nablarch.test.core.db.TestTable;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.file.VariableLengthFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.messaging.RequestTestingMessagePool;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * {@link YamlTestDataParser} のテストクラス。
 *
 * <p>
 * 仕様ID RS-01〜RS-08 を網羅する。
 * RS-02（{@code readLine()} が終端で null を返す）は {@link TestDataReader} 実装の仕様であり、
 * {@code YamlTestDataParser} は {@link TestDataReader} を使用しないため非適用。
 * </p>
 */
@RunWith(DatabaseTestRunner.class)
public class YamlTestDataParserTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    private static final String RESOURCE_ROOT = "src/test/java/";

    private static final String DIR = RESOURCE_ROOT + "nablarch/test/core/reader/";

    private YamlTestDataParser sut;

    @BeforeClass
    public static void beforeClass() {
        VariousDbTestHelper.createTable(TestTable.class);
    }

    @Before
    public void before() {
        DbInfo dbInfo = repositoryResource.getComponent("dbInfo");
        DefaultValues defaultValues = new BasicDefaultValues();
        List<nablarch.test.core.util.interpreter.TestDataInterpreter> interpreters =
                repositoryResource.getComponent("interpreters");

        sut = new YamlTestDataParser();
        sut.setDbInfo(dbInfo);
        sut.setDefaultValues(defaultValues);
        sut.setInterpreters(interpreters);
    }

    @After
    public void after() {
        // static YAML_CACHE をリセットしてテスト間の汚染を防ぐ（B-5）
        YamlTestDataParser.clearCacheForTest();
    }

    // ========================================================================
    // RS-01: {dataName}.yaml ファイルを検索する
    // ========================================================================

    /**
     * [RS-01] getSetupTableData: .yaml ファイルを path/resourceName.yaml として開けること。
     *
     * <p>
     * Given: YAML ファイルが path/resourceName.yaml として配置されている<br>
     * When:  getSetupTableData(dir, "YamlTestDataParserTest/tableData") を呼ぶ<br>
     * Then:  setup_tables のデータが取得できること
     * </p>
     */
    @Test
    public void testRs01_getSetupTableDataLoadsYamlFile() {
        // Given / When
        List<TableData> result = sut.getSetupTableData(DIR, "YamlTestDataParserTest/tableData");

        // Then: グループID なしの 1 件が取得される
        assertThat(result.size(), is(1));
        TableData td = result.get(0);
        assertThat(td.getTableName(), is("TEST_TABLE"));
        assertThat(td.getValue(0, "PK_COL1").toString(), is("0000000001"));
    }

    // ========================================================================
    // RS-08: isResourceExisting
    // ========================================================================

    /**
     * [RS-08] isResourceExisting: YAML ファイルが存在する場合は true を返すこと。
     *
     * <p>
     * Given: YamlTestDataParserTest/existingForTest.yaml が配置されている<br>
     * When:  isResourceExisting(dir, "YamlTestDataParserTest/existingForTest") を呼ぶ<br>
     * Then:  true が返ること
     * </p>
     */
    @Test
    public void testRs08_isResourceExistingReturnsTrueWhenFileExists() {
        // Given / When / Then
        assertTrue(sut.isResourceExisting(DIR, "YamlTestDataParserTest/existingForTest"));
    }

    /**
     * [RS-08] isResourceExisting: YAML ファイルが存在しない場合は false を返すこと。
     *
     * <p>
     * Given: 存在しないファイル名<br>
     * When:  isResourceExisting を呼ぶ<br>
     * Then:  false が返ること
     * </p>
     */
    @Test
    public void testRs08_isResourceExistingReturnsFalseWhenFileNotExists() {
        // Given / When / Then
        assertFalse(sut.isResourceExisting(DIR, "YamlTestDataParserTest/noSuchFile"));
    }

    // ========================================================================
    // RS-07: null 返却後の最終セクションデータ欠落防止
    // ========================================================================

    /**
     * [RS-07] getExpectedFile: YAML 末尾セクション（expected_files）のデータが欠落しないこと。
     *
     * <p>
     * Given: setup_files に続いて expected_files が YAML ファイル末尾に記述されている<br>
     * When:  getExpectedFile を呼ぶ<br>
     * Then:  末尾セクション（expected_files）のデータが欠落せずに取得できること（RS-07）
     * </p>
     */
    @Test
    public void testRs07_lastSectionDataNotLostAtEndOfFile() {
        // Given / When
        List<DataFile> result = sut.getExpectedFile(DIR, "YamlTestDataParserTest/fileData");

        // Then: 末尾セクションのデータが欠落していないこと
        assertThat(result.size(), is(2));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
        assertThat(result.get(1), instanceOf(VariableLengthFile.class));
    }

    // ========================================================================
    // RS-03: YAML ネイティブ null は Java null
    // RS-04: YAML ネイティブ boolean は文字列化
    // RS-05: YAML ネイティブ integer/float は文字列化
    // ========================================================================

    /**
     * [RS-03] getListMap: YAML ネイティブ null は Java null として取得されること。
     *
     * <p>
     * Given: NULL_COL の値が YAML ネイティブ null（アンクォート）<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  NULL_COL の値が Java null であること
     * </p>
     */
    @Test
    public void testRs03_yamlNativeNullIsJavaNull() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/nativeTypes", "nativeTypeTest");

        // Then
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertNull(row.get("NULL_COL"));
    }

    /**
     * [RS-04] getListMap: YAML ネイティブ boolean は文字列 "true"/"false" として取得されること。
     *
     * <p>
     * Given: BOOL_TRUE が YAML ネイティブ boolean true、BOOL_FALSE が false（クォートなし）<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  それぞれ文字列 "true", "false" として取得されること
     * </p>
     */
    @Test
    public void testRs04_yamlNativeBooleanIsStringified() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/nativeTypes", "nativeTypeTest");

        // Then
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertThat(row.get("BOOL_TRUE"), is("true"));
        assertThat(row.get("BOOL_FALSE"), is("false"));
    }

    /**
     * [RS-05] getListMap: YAML ネイティブ integer/float は文字列として取得されること。
     *
     * <p>
     * Given: INT_COL が YAML ネイティブ整数 42、FLOAT_COL が 3.14（クォートなし）<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  それぞれ文字列 "42", "3.14" として取得されること
     * </p>
     */
    @Test
    public void testRs05_yamlNativeNumberIsStringified() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/nativeTypes", "nativeTypeTest");

        // Then
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertThat(row.get("INT_COL"), is("42"));
        assertThat(row.get("FLOAT_COL"), is("3.14"));
    }

    /**
     * [RS-05] getListMap: YAML 科学的記数法（1e10）は文字列として取得されること。
     *
     * <p>
     * Given: FLOAT_SCIENTIFIC が YAML ネイティブ 1e10（SnakeYAML が Double 1.0E10 として解釈）<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  Java の {@code Double.toString(1.0E10)} の出力（"1.0E10"）として取得されること
     * </p>
     */
    @Test
    public void testRs05_yamlScientificNotationIsStringified() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/nativeTypes", "nativeTypeTest");

        // Then: Java の Double.toString(1e10) = "1.0E10"
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertThat(row.get("FLOAT_SCIENTIFIC"), is(Double.toString(1e10)));
    }

    // ========================================================================
    // RS-06: YAML ネイティブ null は Java null（末尾キー省略含む）
    // ========================================================================

    /**
     * [RS-06] getListMap: YAML ネイティブ null（明示記述）は Java null として取得されること。
     *
     * <p>
     * Given: rows の各行に COL2/COL3: null が明示的に含まれる YAML データ<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  null 値のカラムが Java null として返ること（RS-03 仕様による）
     * </p>
     */
    @Test
    public void testRs06_trailingNativeNullIsJavaNull() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/trailingNulls", "trailingNullTest");

        // Then
        assertThat(result.size(), is(2));

        // 1 行目の確認
        Map<String, String> row0 = result.get(0);
        assertThat(row0.get("COL1"), is("val1"));
        assertThat(row0.get("COL2"), is("val2"));
        // COL3: null → SnakeYAML が Java null に変換し、objectToString() がそのまま null を返す（RS-03）
        assertNull(row0.get("COL3"));

        // 2 行目の確認
        Map<String, String> row1 = result.get(1);
        assertThat(row1.get("COL1"), is("val4"));
        assertNull(row1.get("COL2"));
        assertNull(row1.get("COL3"));
    }

    /**
     * [RS-06] getListMap: YAML 後続行で末尾キーを省略した場合、省略キーの値は null として取得されること。
     *
     * <p>
     * Given: 2 行目に COL3 キーが省略されている list_maps エントリ<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  2 行目の COL3 が null として取得されること
     * </p>
     */
    @Test
    public void testRs06_trailingKeyOmittedIsNull() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/trailingNulls", "trailingKeyOmitTest");

        // Then
        assertThat(result.size(), is(2));
        assertThat(result.get(0).get("COL3"), is("row1_c"));
        // 2 行目は COL3 キーが YAML に記述されていない → Map に存在しないため null
        assertNull(result.get(1).get("COL3"));
    }

    // ========================================================================
    // getSetupTableData / getExpectedTableData（グループID 付き）
    // ========================================================================

    /**
     * [RS-01] getSetupTableData: グループ ID 指定で対象グループのみ取得されること。
     *
     * <p>
     * Given: setup_tables に groupA / groupB のエントリがある<br>
     * When:  getSetupTableData(dir, resource, "groupA") を呼ぶ<br>
     * Then:  groupA の 1 件のみ返ること
     * </p>
     */
    @Test
    public void testGetSetupTableDataWithGroupId() {
        // Given / When
        List<TableData> result = sut.getSetupTableData(DIR, "YamlTestDataParserTest/tableData", "groupA");

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000002"));
    }

    /**
     * [RS-01] getSetupTableData: 存在しないグループ ID を指定した場合に空リストが返ること。
     *
     * <p>
     * Given: 存在しないグループ ID<br>
     * When:  getSetupTableData を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void testGetSetupTableDataNotExist() {
        // Given / When
        List<TableData> result = sut.getSetupTableData(DIR, "YamlTestDataParserTest/tableData", "noSuchGroup");

        // Then
        assertThat(result.size(), is(0));
    }

    /**
     * [RS-01] getExpectedTableData: グループ ID 付きで取得できること。
     *
     * <p>
     * Given: expected_tables に groupA のエントリがある<br>
     * When:  getExpectedTableData(dir, resource, "groupA") を呼ぶ<br>
     * Then:  groupA の 1 件が返ること
     * </p>
     */
    @Test
    public void testGetExpectedTableDataWithGroupId() {
        // Given / When
        List<TableData> result = sut.getExpectedTableData(DIR, "YamlTestDataParserTest/tableData", "groupA");

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000002"));
    }

    /**
     * [RS-01] getExpectedTableData: グループ ID なしで全件取得できること。
     *
     * <p>
     * Given: expected_tables にグループ ID なしのエントリ<br>
     * When:  getExpectedTableData(dir, resource) を呼ぶ<br>
     * Then:  グループ ID なしの 1 件が返ること
     * </p>
     */
    @Test
    public void testGetExpectedTableDataWithoutGroupId() {
        // Given / When
        List<TableData> result = sut.getExpectedTableData(DIR, "YamlTestDataParserTest/tableData");

        // Then: expected_tables（グループIDなし 1 件）のみ
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000001"));
    }

    /**
     * [RS-01] getExpectedTableData: ファイルが存在しない場合は IllegalStateException がスローされること。
     *
     * <p>
     * Given: 存在しない YAML ファイルのリソース名<br>
     * When:  getExpectedTableData を呼ぶ<br>
     * Then:  IllegalStateException がスローされること
     * </p>
     */
    @Test(expected = IllegalStateException.class)
    public void testGetExpectedTableDataThrowsWhenFileNotExists() {
        // Given / When / Then
        sut.getExpectedTableData(DIR, "YamlTestDataParserTest/noSuchFile");
    }

    // ========================================================================
    // getListMap
    // ========================================================================

    /**
     * [RS-01] getListMap: 指定 ID のデータが取得できること。
     *
     * <p>
     * Given: list_maps に id=testListMap が 2 行<br>
     * When:  getListMap(dir, resource, "testListMap") を呼ぶ<br>
     * Then:  2 行のデータが返ること
     * </p>
     */
    @Test
    public void testGetListMap() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/tableData", "testListMap");

        // Then
        assertThat(result.size(), is(2));
        assertThat(result.get(0).get("KEY1"), is("val1"));
        assertThat(result.get(0).get("KEY2"), is("val2"));
        assertThat(result.get(1).get("KEY1"), is("val3"));
        assertThat(result.get(1).get("KEY2"), is("val4"));
    }

    // ========================================================================
    // getSetupFile / getExpectedFile
    // ========================================================================

    /**
     * [RS-01] getSetupFile: 固定長ファイルと可変長ファイルが取得できること。
     *
     * <p>
     * Given: setup_files に fixed と variable の 2 エントリ<br>
     * When:  getSetupFile を呼ぶ<br>
     * Then:  FixedLengthFile と VariableLengthFile の 2 件が返ること
     * </p>
     */
    @Test
    public void testGetSetupFile() {
        // Given / When
        List<DataFile> result = sut.getSetupFile(DIR, "YamlTestDataParserTest/fileData");

        // Then
        assertThat(result.size(), is(2));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
        assertThat(result.get(1), instanceOf(VariableLengthFile.class));
    }

    /**
     * [RS-01] getSetupFile: 取得した DataFile の path が正しく設定されていること。
     *
     * <p>
     * Given: setup_files に path=dummy/setup_fixed.dat のエントリ<br>
     * When:  getSetupFile を呼ぶ<br>
     * Then:  getPath() が "dummy/setup_fixed.dat" を返すこと
     * </p>
     */
    @Test
    public void testGetSetupFileHasCorrectPath() {
        // Given / When
        List<DataFile> result = sut.getSetupFile(DIR, "YamlTestDataParserTest/fileData");

        // Then
        assertThat(result.get(0).getPath(), is("dummy/setup_fixed.dat"));
        assertThat(result.get(1).getPath(), is("dummy/setup_variable.csv"));
    }

    /**
     * [RS-01] getSetupFile: グループ ID 指定で対象グループのみ取得されること。
     *
     * <p>
     * Given: setup_files に grp1 のエントリがある<br>
     * When:  getSetupFile(dir, resource, "grp1") を呼ぶ<br>
     * Then:  grp1 の 1 件のみ返ること
     * </p>
     */
    @Test
    public void testGetSetupFileWithGroupId() {
        // Given / When
        List<DataFile> result = sut.getSetupFile(DIR, "YamlTestDataParserTest/fileData", "grp1");

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
    }

    /**
     * [RS-01] getExpectedFile: 固定長ファイルと可変長ファイルが取得できること。
     *
     * <p>
     * Given: expected_files に fixed と variable の 2 エントリ<br>
     * When:  getExpectedFile を呼ぶ<br>
     * Then:  FixedLengthFile と VariableLengthFile の 2 件が返ること
     * </p>
     */
    @Test
    public void testGetExpectedFile() {
        // Given / When
        List<DataFile> result = sut.getExpectedFile(DIR, "YamlTestDataParserTest/fileData");

        // Then
        assertThat(result.size(), is(2));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
        assertThat(result.get(1), instanceOf(VariableLengthFile.class));
    }

    /**
     * [RS-01] getExpectedFile: グループ ID 指定で対象グループのみ取得されること。
     *
     * <p>
     * Given: setup_files と同構造で expected_files にも grp1 のエントリを追加したテストデータ<br>
     * When:  getExpectedFile(dir, resource, "grp1") を呼ぶ<br>
     * Then:  grp1 の 1 件のみ返ること
     * </p>
     */
    @Test
    public void testGetExpectedFileWithGroupId() {
        // Given / When
        List<DataFile> result = sut.getExpectedFile(DIR, "YamlTestDataParserTest/fileDataWithGroup", "grp1");

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
    }

    /**
     * [RS-01] getExpectedFile: 取得した DataFile の path が正しく設定されていること。
     *
     * <p>
     * Given: expected_files に path=dummy/expected_fixed.dat のエントリ<br>
     * When:  getExpectedFile を呼ぶ<br>
     * Then:  getPath() が "dummy/expected_fixed.dat" を返すこと
     * </p>
     */
    @Test
    public void testGetExpectedFileHasCorrectPath() {
        // Given / When
        List<DataFile> result = sut.getExpectedFile(DIR, "YamlTestDataParserTest/fileData");

        // Then
        assertThat(result.get(0).getPath(), is("dummy/expected_fixed.dat"));
        assertThat(result.get(1).getPath(), is("dummy/expected_variable.csv"));
    }

    // ========================================================================
    // getMessage
    // ========================================================================

    /**
     * [RS-01] getMessage: メッセージが取得でき、FW ヘッダ値（requestId・userId）が設定されていること。
     *
     * <p>
     * Given: messages の FW_HEADER レコードに requestId="0000000001", userId="testUser01" が含まれる<br>
     * When:  getMessage を呼ぶ<br>
     * Then:  MessagePool が返り、requestId と userId が extractFwHeader で正しく抽出されていること
     * </p>
     */
    @Test
    public void testGetMessage() throws Exception {
        // Given / When
        MessagePool result = sut.getMessage(DIR, "YamlTestDataParserTest/messageData", "req001");

        // Then: non-null かつ RequestTestingMessagePool であること
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));

        // FW ヘッダ実値の検証: MessagePool.getFwHeader() はパッケージプライベートのため
        // リフレクションで fwHeader フィールドを直接取得して検証する
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);

        assertThat("requestId が設定されていること", fwHeader.get("requestId"), is("0000000001"));
        assertThat("userId が設定されていること", fwHeader.get("userId"), is("testUser01"));
        assertThat("resendFlag が設定されていること", fwHeader.get("resendFlag"), is("0"));
        assertThat("resultCode が設定されていること", fwHeader.get("resultCode"), is("0000"));
    }

    // ========================================================================
    // getMessageWithoutCache（SendSyncMessageParser 相当）
    // ========================================================================

    /**
     * [RS-01] getMessageWithoutCache(EXPECTED_REQUEST_BODY_MESSAGES): メッセージが取得できること。
     *
     * <p>
     * Given: expected_request_body_messages に id=req001 と SEARCH_KEY フィールドがある<br>
     * When:  getMessageWithoutCache(dir, resource, EXPECTED_REQUEST_BODY_MESSAGES, "req001") を呼ぶ<br>
     * Then:  MessagePool が返ること
     * </p>
     */
    @Test
    public void testGetMessageWithoutCache_expectedRequestBodyMessages() {
        // Given / When
        MessagePool result = sut.getMessageWithoutCache(
                DIR, "YamlTestDataParserTest/messageData",
                DataType.EXPECTED_REQUEST_BODY_MESSAGES, "req001");

        // Then: non-null かつ RequestTestingMessagePool であること
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));
    }

    /**
     * [RS-01] getMessageWithoutCache(EXPECTED_REQUEST_HEADER_MESSAGES): メッセージが取得できること。
     *
     * <p>
     * Given: expected_request_header_messages に id=req001 と requestId/userId フィールドがある<br>
     * When:  getMessageWithoutCache(dir, resource, EXPECTED_REQUEST_HEADER_MESSAGES, "req001") を呼ぶ<br>
     * Then:  MessagePool が返ること
     * </p>
     */
    @Test
    public void testGetMessageWithoutCache_expectedRequestHeaderMessages() {
        // Given / When
        MessagePool result = sut.getMessageWithoutCache(
                DIR, "YamlTestDataParserTest/messageData",
                DataType.EXPECTED_REQUEST_HEADER_MESSAGES, "req001");

        // Then: non-null かつ RequestTestingMessagePool であること
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));
    }

    /**
     * [RS-01] getMessageWithoutCache(RESPONSE_BODY_MESSAGES): メッセージが取得できること。
     *
     * <p>
     * Given: response_body_messages に group_id=grp1, id=resp001, RESULT_CODE="0000" のエントリ<br>
     * When:  getMessageWithoutCache(dir, resource, RESPONSE_BODY_MESSAGES, "resp001") を呼ぶ<br>
     * Then:  MessagePool が返ること
     * </p>
     */
    @Test
    public void testGetMessageWithoutCache_responseBodyMessages() {
        // Given / When
        MessagePool result = sut.getMessageWithoutCache(
                DIR, "YamlTestDataParserTest/messageData",
                DataType.RESPONSE_BODY_MESSAGES, "resp001");

        // Then: non-null かつ RequestTestingMessagePool であること
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));
    }

    /**
     * [RS-01] getMessageWithoutCache(RESPONSE_HEADER_MESSAGES): メッセージが取得できること。
     *
     * <p>
     * Given: response_header_messages に group_id=grp1, id=resp001, requestId="0000000001" のエントリ<br>
     * When:  getMessageWithoutCache(dir, resource, RESPONSE_HEADER_MESSAGES, "resp001") を呼ぶ<br>
     * Then:  MessagePool が返ること
     * </p>
     */
    @Test
    public void testGetMessageWithoutCache_responseHeaderMessages() {
        // Given / When
        MessagePool result = sut.getMessageWithoutCache(
                DIR, "YamlTestDataParserTest/messageData",
                DataType.RESPONSE_HEADER_MESSAGES, "resp001");

        // Then: non-null かつ RequestTestingMessagePool であること
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));
    }

    // ========================================================================
    // getSendSyncMessage（GroupMessageParser 相当）
    // ========================================================================

    /**
     * [RS-01] getSendSyncMessage: グループ ID 付きのメッセージリストが取得できること。
     *
     * <p>
     * Given: response_body_messages に group_id=grp1 のエントリ<br>
     * When:  getSendSyncMessage(dir, resource, "grp1", RESPONSE_BODY_MESSAGES) を呼ぶ<br>
     * Then:  RequestTestingMessagePool のリストが返ること
     * </p>
     */
    @Test
    public void testGetSendSyncMessage() {
        // Given / When
        List<RequestTestingMessagePool> result = sut.getSendSyncMessage(
                DIR, "YamlTestDataParserTest/messageData",
                "grp1", DataType.RESPONSE_BODY_MESSAGES);

        // Then
        assertNotNull(result);
        assertThat(result.size(), is(1));
    }

    /**
     * [RS-01] getSendSyncMessage: 存在しないグループ ID を指定した場合は null が返ること。
     *
     * <p>
     * Given: 存在しないグループ ID "noSuchGroup"<br>
     * When:  getSendSyncMessage を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void testGetSendSyncMessageReturnsNullForUnknownGroupId() {
        // Given / When
        List<RequestTestingMessagePool> result = sut.getSendSyncMessage(
                DIR, "YamlTestDataParserTest/messageData",
                "noSuchGroup", DataType.RESPONSE_BODY_MESSAGES);

        // Then
        assertNull(result);
    }

    // ========================================================================
    // getSetupTableData: ファイル不存在時は空リストを返す
    // ========================================================================

    /**
     * [RS-01] getSetupTableData: YAML ファイルが存在しない場合は空リストを返すこと。
     *
     * <p>
     * Given: 存在しない YAML ファイルのリソース名<br>
     * When:  getSetupTableData を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void testGetSetupTableDataReturnsEmptyWhenFileNotExists() {
        // Given / When
        List<TableData> result = sut.getSetupTableData(DIR, "YamlTestDataParserTest/noSuchFile");

        // Then
        assertThat(result.size(), is(0));
    }

    // ========================================================================
    // setup_tables: rows が空のエントリは除外される
    // ========================================================================

    /**
     * [RS-01] getSetupTableData: rows が空のエントリは結果から除外されること。
     *
     * <p>
     * Given: setup_tables に rows: [] のエントリ（emptyRows グループ）<br>
     * When:  getSetupTableData(dir, resource, "emptyRows") を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void testGetSetupTableDataExcludesEmptyRows() {
        // Given / When
        List<TableData> result = sut.getSetupTableData(DIR, "YamlTestDataParserTest/tableData", "emptyRows");

        // Then
        assertThat(result.size(), is(0));
    }

    // ========================================================================
    // getListMap: 存在しない ID は空リストを返す
    // ========================================================================

    /**
     * [RS-01] getListMap: 存在しない ID を指定した場合は空リストが返ること。
     *
     * <p>
     * Given: list_maps に存在しない id<br>
     * When:  getListMap(dir, resource, "noSuchId") を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void testGetListMapReturnsEmptyWhenIdNotFound() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/tableData", "noSuchId");

        // Then
        assertThat(result.size(), is(0));
    }

    // ========================================================================
    // getListMap: マーカーカラム（[COL] 形式）は除外される
    // ========================================================================

    /**
     * [RS-01] getListMap: マーカーカラム（[COL] 形式）は結果の Map から除外されること。
     *
     * <p>
     * Given: list_maps に "[NO]" キーを含む行<br>
     * When:  getListMap(dir, resource, "markerColTest") を呼ぶ<br>
     * Then:  "[NO]" キーが結果に含まれず、通常カラムのみ返ること
     * </p>
     */
    @Test
    public void testGetListMapExcludesMarkerColumns() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/tableData", "markerColTest");

        // Then
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertFalse(row.containsKey("[NO]"));
        assertThat(row.get("KEY1"), is("val1"));
        assertThat(row.get("KEY2"), is("val2"));
    }

    // ========================================================================
    // getMessage / getMessageWithoutCache: 存在しない ID は null を返す
    // ========================================================================

    /**
     * [RS-01] getMessage: 存在しない ID を指定した場合は null が返ること。
     *
     * <p>
     * Given: messages に存在しない id<br>
     * When:  getMessage(dir, resource, "noSuchId") を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void testGetMessageReturnsNullWhenIdNotFound() {
        // Given / When
        MessagePool result = sut.getMessage(DIR, "YamlTestDataParserTest/messageData", "noSuchId");

        // Then
        assertNull(result);
    }

    /**
     * [RS-01] getMessageWithoutCache: 存在しない ID を指定した場合は null が返ること。
     *
     * <p>
     * Given: expected_request_body_messages に存在しない id<br>
     * When:  getMessageWithoutCache を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void testGetMessageWithoutCacheReturnsNullWhenIdNotFound() {
        // Given / When
        MessagePool result = sut.getMessageWithoutCache(
                DIR, "YamlTestDataParserTest/messageData",
                DataType.EXPECTED_REQUEST_BODY_MESSAGES, "noSuchId");

        // Then
        assertNull(result);
    }

    // ========================================================================
    // setTestDataReader: UnsupportedOperationException がスローされること
    // ========================================================================

    /**
     * [RS-01] setTestDataReader: UnsupportedOperationException がスローされること。
     *
     * <p>
     * Given: YamlTestDataParser インスタンス<br>
     * When:  setTestDataReader(reader) を呼ぶ<br>
     * Then:  UnsupportedOperationException がスローされること
     * </p>
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testSetTestDataReaderThrowsUnsupported() {
        // Given / When / Then
        sut.setTestDataReader(new MockTestDataReader());
    }

    // ========================================================================
    // S-6: JSON Schema 全項目網羅テスト
    // ========================================================================

    /**
     * [S-6] schemaFullCoverage: スキーマの全トップレベルキー・全 directives・length="-" を含む YAML を
     * 実装が正しく解釈できること。
     *
     * <p>
     * Given: スキーマ（ntf-testdata-yaml-schema.json）の全項目を含む schemaFullCoverage.yaml<br>
     * When:  各 get* メソッドで読み込む<br>
     * Then:  エラーなしに読み込まれ、各トップレベルキーの件数が正しいこと
     * </p>
     *
     * <p>
     * 【設計方針】このメソッドは「スキーマ全11トップレベルキーを一括で通過させる統合煙突テスト」である。
     * 各 DataType・DataFile 型・メッセージ経路の個別検証は既存の testRs0x_ / testGetSetupFile 等のメソッドで
     * 実施済みであり、本メソッドはスキーマに定義されたすべての項目が実装によって解釈可能であることを
     * 一括で確認することを目的とするため、意図的に1メソッドに集約している。
     * </p>
     *
     * <p>
     * 【ディレクティブ値の検証方針】DataFile#setDirective() は無効なキーを IllegalArgumentException で
     * スローする（DR-11・DataFileTest#testConvertValueWithInvalidDirective で確認済み）。そのため、
     * スキーマに記載した全 directives キー（text-encoding, record-separator, file-type, record-length,
     * positive/negative-zone/pack-sign-nibble, required-decimal-point, fixed-sign-position, required-plus-sign,
     * field-separator, quoting-delimiter, ignore-blank-lines, requires-title, max-record-length, title-record-type-name）を
     * 含む YAML が例外なく読み込まれた時点で、全キーが実装で有効であることが証明される。
     * </p>
     */
    @Test
    public void testSchemaFullCoverage() throws Exception {
        final String resource = "YamlTestDataParserTest/schemaFullCoverage";

        // setup_tables: group_id なし・grp1・emptySetup の 3 エントリ。
        // グループID なし呼び出しは group_id フィールドのないエントリのみ返す（rows 空除外後の 1 件）。
        List<TableData> setupTables = sut.getSetupTableData(DIR, resource);
        assertThat("setup_tables: group_id なしエントリが取得できること", setupTables.size(), is(1));
        assertThat(setupTables.get(0).getTableName(), is("TEST_TABLE"));

        List<TableData> setupTablesGrp1 = sut.getSetupTableData(DIR, resource, "grp1");
        assertThat("setup_tables: grp1 エントリが取得できること", setupTablesGrp1.size(), is(1));

        // expected_tables: group_id なし・grp1・emptyExpected の 3 エントリ。
        // getExpectedTableData はグループID なしでは expected_tables(1件) + expected_complete_tables(1件) = 2 件。
        List<TableData> expectedTables = sut.getExpectedTableData(DIR, resource);
        assertThat("expected_tables + expected_complete_tables: group_id なしエントリが取得できること", expectedTables.size(), is(2));

        List<TableData> expectedTablesGrp1 = sut.getExpectedTableData(DIR, resource, "grp1");
        assertThat("expected_tables: grp1 エントリが取得できること", expectedTablesGrp1.size(), is(1));

        // list_maps: id=listMapId1 が取得できること
        List<Map<String, String>> listMap = sut.getListMap(DIR, resource, "listMapId1");
        assertThat("list_maps: 2 行取得できること", listMap.size(), is(2));
        assertThat("list_maps: KEY1 が val1 であること", listMap.get(0).get("KEY1"), is("val1"));
        assertThat("list_maps: KEY2 の null 値が null として取得されること", listMap.get(1).get("KEY2"), nullValue());

        // setup_files: group_id なしエントリが3件（all_directives[fixed], variable, empty_file[fixed]）。
        // group_id=grpFixed のエントリはグループIDなし呼び出しではフィルタされるため除外される。
        List<DataFile> setupFiles = sut.getSetupFile(DIR, resource);
        assertThat("setup_files: グループなしの 3 件が取得できること", setupFiles.size(), is(3));
        assertThat("setup_files[0]: FixedLengthFile であること", setupFiles.get(0), instanceOf(FixedLengthFile.class));
        assertThat("setup_files[0]: path が正しいこと",
                setupFiles.get(0).getPath(), is("dummy/setup_fixed_all_directives.dat"));
        assertThat("setup_files[1]: VariableLengthFile であること", setupFiles.get(1), instanceOf(VariableLengthFile.class));
        assertThat("setup_files[2]: records 空の FixedLengthFile であること", setupFiles.get(2), instanceOf(FixedLengthFile.class));

        List<DataFile> setupFilesGrp = sut.getSetupFile(DIR, resource, "grpFixed");
        assertThat("setup_files: grpFixed エントリが取得できること", setupFilesGrp.size(), is(1));

        // expected_files: fixed 1 件 + variable 1 件
        List<DataFile> expectedFiles = sut.getExpectedFile(DIR, resource);
        assertThat("expected_files: 2 件取得できること", expectedFiles.size(), is(2));
        assertThat("expected_files[0]: FixedLengthFile であること", expectedFiles.get(0), instanceOf(FixedLengthFile.class));
        assertThat("expected_files[1]: VariableLengthFile であること", expectedFiles.get(1), instanceOf(VariableLengthFile.class));

        // messages: id=msgId1 が取得できること
        MessagePool msg = sut.getMessage(DIR, resource, "msgId1");
        assertThat("messages: non-null であること", msg, notNullValue());
        assertThat("messages: RequestTestingMessagePool であること", msg, instanceOf(RequestTestingMessagePool.class));

        // expected_request_header_messages: id=msgId1 が取得できること
        MessagePool reqHeader = sut.getMessageWithoutCache(
                DIR, resource, DataType.EXPECTED_REQUEST_HEADER_MESSAGES, "msgId1");
        assertThat("expected_request_header_messages: non-null であること", reqHeader, notNullValue());

        // expected_request_body_messages: id=msgId1 が取得できること
        MessagePool reqBody = sut.getMessageWithoutCache(
                DIR, resource, DataType.EXPECTED_REQUEST_BODY_MESSAGES, "msgId1");
        assertThat("expected_request_body_messages: non-null であること", reqBody, notNullValue());

        // response_body_messages: getSendSyncMessage で grp1 エントリが取得できること
        List<RequestTestingMessagePool> respBody = sut.getSendSyncMessage(
                DIR, resource, "grp1", DataType.RESPONSE_BODY_MESSAGES);
        assertThat("response_body_messages: grp1 の 1 件が取得できること", respBody.size(), is(1));

        // response_header_messages: getSendSyncMessage で grp1 エントリが取得できること（GroupData 経路）
        List<RequestTestingMessagePool> respHeader = sut.getSendSyncMessage(
                DIR, resource, "grp1", DataType.RESPONSE_HEADER_MESSAGES);
        assertThat("response_header_messages: grp1 の 1 件が取得できること", respHeader.size(), is(1));

        // response_header/body_messages: SingleData 経路（group_id なし）のエントリが取得できること
        MessagePool respHeaderSingle = sut.getMessageWithoutCache(
                DIR, resource, DataType.RESPONSE_HEADER_MESSAGES, "respHeaderSingle");
        assertThat("response_header_messages: SingleData 経路のエントリが取得できること", respHeaderSingle, notNullValue());

        MessagePool respBodySingle = sut.getMessageWithoutCache(
                DIR, resource, DataType.RESPONSE_BODY_MESSAGES, "respBodySingle");
        assertThat("response_body_messages: SingleData 経路のエントリが取得できること", respBodySingle, notNullValue());
    }

    // ========================================================================
    // expected_complete_tables: fillDefaultValues が呼ばれること
    // ========================================================================

    /**
     * [RS-01] getExpectedTableData: expected_complete_tables では fillDefaultValues が呼ばれること。
     *
     * <p>
     * Given: expected_complete_tables に PK_COL1/PK_COL2 のみのエントリ（他カラム省略）<br>
     * When:  getExpectedTableData を呼ぶ<br>
     * Then:  省略カラムにデフォルト値が補完されていること（カラム数が増え、具体的なデフォルト値が設定されること）
     * </p>
     */
    @Test
    public void testGetExpectedTableDataCompleted() {
        // Given / When
        List<TableData> result = sut.getExpectedTableData(DIR, "YamlTestDataParserTest/completedTable");

        // Then: expected_complete_tables の 1 件が返り、省略カラムが補完されていること
        assertThat(result.size(), is(1));
        TableData td = result.get(0);
        assertThat(td.getTableName(), is("TEST_TABLE"));
        // fillDefaultValues() により DB の全カラムが追加される（YAML 記述の 2 カラムより多い）
        assertTrue("fillDefaultValues により全カラムが補完されていること", td.getColumnNames().length > 2);
        // 数値型（NUMBER_COL）のデフォルト値は "0"（BasicDefaultValues の仕様）
        assertThat("NUMBER_COL のデフォルト値が補完されていること",
                td.getValue(0, "NUMBER_COL").toString(), is("0"));
        // 文字列型（VARCHAR2_COL）のデフォルト値は " "（半角スペース）
        assertThat("VARCHAR2_COL のデフォルト値が補完されていること",
                td.getValue(0, "VARCHAR2_COL").toString(), is(" "));
    }

    // ========================================================================
    // T1: フィールド型記法を日本語名称に統一
    // ========================================================================

    /**
     * [IV-12] フィールド型を日本語名称で記述した固定長/可変長 YAML が
     * dataTypeMapping identity mapping なしで例外なく読み込めること。
     * BasicDataTypeMapping.DEFAULT_TABLE の全 22 エントリ（X/N/XN/Z/SZ/P/SP/X9/SX9/B 系）を網羅する。
     *
     * <p>
     * Given: 半角英字/半角数字/半角記号/半角カナ/半角英数字/半角英数字記号/半角/
     *        全角英字/全角数字/全角ひらがな/全角カタカナ/全角漢字/全角/全半角/
     *        数値/符号無ゾーン10進数/符号付ゾーン10進数/符号無パック10進数/符号付パック10進数/
     *        符号無数値/符号付数値/バイナリ を含む YAML<br>
     * When:  getSetupFile / getExpectedFile を呼ぶ<br>
     * Then:  FixedLengthFile / VariableLengthFile が取得でき、例外が発生しないこと
     * </p>
     */
    @Test
    public void japaneseFieldTypeIsReadableWithoutIdentityMapping() {
        // Given / When
        List<DataFile> setupFiles = sut.getSetupFile(DIR, "YamlTestDataParserTest/japaneseFieldType");
        List<DataFile> expectedFiles = sut.getExpectedFile(DIR, "YamlTestDataParserTest/japaneseFieldType");

        // Then: 3固定長（X系/N系/misc系）+ 1可変長
        assertThat(setupFiles.size(), is(4));
        assertThat(setupFiles.get(0), instanceOf(FixedLengthFile.class));
        assertThat(setupFiles.get(1), instanceOf(FixedLengthFile.class));
        assertThat(setupFiles.get(2), instanceOf(FixedLengthFile.class));
        assertThat(setupFiles.get(3), instanceOf(VariableLengthFile.class));
        assertThat(expectedFiles.size(), is(1));
        assertThat(expectedFiles.get(0), instanceOf(FixedLengthFile.class));
    }

    /**
     * [IV-12] identity mapping 廃止後、旧記法（型記号 X）を YAML に書くと例外になること。
     *
     * <p>
     * Given: type: X（旧型記号記法）を含む YAML<br>
     * When:  getSetupFile を呼ぶ<br>
     * Then:  BasicDataTypeMapping が "X" を変換できず IllegalArgumentException が送出されること
     * </p>
     */
    @Test(expected = IllegalArgumentException.class)
    public void oldTypeSymbolThrowsWhenIdentityMappingAbsent() {
        // Given / When / Then: IllegalArgumentException を期待
        sut.getSetupFile(DIR, "YamlTestDataParserTest/oldTypeSymbol");
    }

    // ========================================================================
    // T6: expected_tables / expected_complete_tables 混在順序非依存
    // ========================================================================

    /**
     * getExpectedTableData: expected_tables → expected_complete_tables の順で記述しても
     * 両セクションが全件取得されること（通常順）。
     *
     * <p>
     * Given: expected_tables(1件) → expected_complete_tables(1件) の順で記述した YAML<br>
     * When:  getExpectedTableData(dir, resource) を呼ぶ<br>
     * Then:  expected_tables 1 件 + expected_complete_tables 1 件 = 合計 2 件が取得されること
     *        （取得順: expected_tables → expected_complete_tables のセクション結合順）
     * </p>
     */
    @Test
    public void mixedTablesNormalOrderReturnsBothSections() {
        // Given / When
        List<TableData> result = sut.getExpectedTableData(DIR, "YamlTestDataParserTest/mixedTablesNormalOrder");

        // Then: expected_tables(1) + expected_complete_tables(1) = 2 件
        assertThat("通常順: 2 件取得できること", result.size(), is(2));
        assertThat("通常順: 1 件目は PK_COL1=0000000001 であること",
                result.get(0).getValue(0, "PK_COL1").toString(), is("0000000001"));
        assertThat("通常順: 2 件目は PK_COL1=0000000002 であること",
                result.get(1).getValue(0, "PK_COL1").toString(), is("0000000002"));
    }

    /**
     * getExpectedTableData: expected_complete_tables → expected_tables の順で記述しても
     * 両セクションが全件取得されること（逆順）。
     *
     * <p>
     * Given: expected_complete_tables(1件) → expected_tables(1件) の順で記述した YAML<br>
     * When:  getExpectedTableData(dir, resource) を呼ぶ<br>
     * Then:  expected_tables 1 件 + expected_complete_tables 1 件 = 合計 2 件が取得されること
     *        （取得順は YAML 記述順ではなくセクション結合順: expected_tables → expected_complete_tables）
     * </p>
     */
    @Test
    public void mixedTablesReverseOrderReturnsBothSections() {
        // Given / When
        List<TableData> result = sut.getExpectedTableData(DIR, "YamlTestDataParserTest/mixedTablesReverseOrder");

        // Then: expected_tables(1) + expected_complete_tables(1) = 2 件（YAML 記述順に関係なく両方取得）
        // 取得順は YAML 記述順ではなくセクション結合順（expected_tables → expected_complete_tables）に従う
        assertThat("逆順: 2 件取得できること", result.size(), is(2));
        assertThat("逆順: 1 件目は expected_tables の PK_COL1=0000000001 であること",
                result.get(0).getValue(0, "PK_COL1").toString(), is("0000000001"));
        assertThat("逆順: 2 件目は expected_complete_tables の PK_COL1=0000000002 であること",
                result.get(1).getValue(0, "PK_COL1").toString(), is("0000000002"));
    }

    /**
     * getExpectedTableData: 各セクションに複数エントリがあっても
     * グループID指定で正しくフィルタリングされること（複数エントリ混在）。
     *
     * <p>
     * Given: expected_tables(2件: groupIDなし + grpA) と expected_complete_tables(2件: groupIDなし + grpA) を含む YAML<br>
     * When(a): getExpectedTableData(dir, resource) を呼ぶ<br>
     * Then(a): groupID なしの expected_tables 1件 + expected_complete_tables 1件 = 合計 2 件<br>
     * When(b): getExpectedTableData(dir, resource, "grpA") を呼ぶ<br>
     * Then(b): grpA の expected_tables 1件 + expected_complete_tables 1件 = 合計 2 件
     * </p>
     */
    @Test
    public void mixedTablesMultipleEntriesReturnsCorrectEntriesPerGroupId() {
        final String resource = "YamlTestDataParserTest/mixedTablesMultipleEntries";

        // When(a): groupID なし
        List<TableData> withoutGroupId = sut.getExpectedTableData(DIR, resource);

        // Then(a): groupID なしの 2 件（expected_tables の groupIDなし + expected_complete_tables の groupIDなし）
        assertThat("複数エントリ混在: groupIDなし → 2 件取得できること", withoutGroupId.size(), is(2));
        assertThat("複数エントリ混在: 1 件目は expected_tables の PK_COL1=0000000001 であること",
                withoutGroupId.get(0).getValue(0, "PK_COL1").toString(), is("0000000001"));
        assertThat("複数エントリ混在: 2 件目は expected_complete_tables の PK_COL1=0000000002 であること",
                withoutGroupId.get(1).getValue(0, "PK_COL1").toString(), is("0000000002"));

        // When(b): grpA 指定
        List<TableData> withGrpA = sut.getExpectedTableData(DIR, resource, "grpA");

        // Then(b): grpA の 2 件（expected_tables の grpA + expected_complete_tables の grpA）
        assertThat("複数エントリ混在: grpA → 2 件取得できること", withGrpA.size(), is(2));
        assertThat("複数エントリ混在: grpA 1 件目は expected_tables の PK_COL1=0000000003 であること",
                withGrpA.get(0).getValue(0, "PK_COL1").toString(), is("0000000003"));
        assertThat("複数エントリ混在: grpA 2 件目は expected_complete_tables の PK_COL1=0000000004 であること",
                withGrpA.get(1).getValue(0, "PK_COL1").toString(), is("0000000004"));
    }
}
