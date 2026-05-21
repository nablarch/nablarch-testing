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
}
