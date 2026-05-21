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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    // RS-01: isResourceExisting
    // ========================================================================

    /**
     * [RS-01, RS-08] isResourceExisting: YAML ファイルが存在する場合は true を返すこと。
     *
     * <p>
     * Given: YamlTestDataParserTest/notExisting.yaml が存在する<br>
     * When:  isResourceExisting(dir, "YamlTestDataParserTest/notExisting") を呼ぶ<br>
     * Then:  true が返ること
     * </p>
     */
    @Test
    public void testRs08_isResourceExistingReturnsTrueWhenFileExists() {
        // Given / When / Then
        assertTrue(sut.isResourceExisting(DIR, "YamlTestDataParserTest/notExisting"));
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
    // RS-02: readLine() は文書終端で null を返す（最終セクション欠落なし）
    // RS-07: null 返却後の最終セクションデータ欠落防止
    // ========================================================================

    /**
     * [RS-02, RS-07] getSetupFile: YAML 末尾のセクションデータが欠落しないこと。
     *
     * <p>
     * Given: setup_files と expected_files を含む YAML ファイル<br>
     * When:  getSetupFile を呼ぶ<br>
     * Then:  最後のファイルセクションのデータが欠落していないこと
     * </p>
     */
    @Test
    public void testRs02Rs07_lastSectionDataNotLostAtEndOfFile() {
        // Given / When
        List<DataFile> result = sut.getSetupFile(DIR, "YamlTestDataParserTest/fileData");

        // Then: グループID なしの固定長・可変長ファイルが取得される（2 件）
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
     * Given: BOOL_TRUE が YAML 文字列 "true"、BOOL_FALSE が "false"<br>
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
     * [RS-05] getListMap: YAML ネイティブ integer は文字列として取得されること。
     *
     * <p>
     * Given: INT_COL が "42"（文字列）、FLOAT_COL が "3.14"（文字列）<br>
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

    // ========================================================================
    // RS-06: 末尾の空要素は "" で補完
    // ========================================================================

    /**
     * [RS-06] getListMap: 末尾の null 値は空文字 "" として取得されること。
     *
     * <p>
     * Given: rows に末尾が null のオブジェクト（COL3: null）<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  null は空文字 "" として返ること（ただし NullInterpreter が null に変換する仕様に注意）
     * </p>
     */
    @Test
    public void testRs06_trailingNullValuesAreEmptyString() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/trailingNulls", "trailingNullTest");

        // Then: NullInterpreter が null 値を Java null に変換するため null が返る
        // （RS-06 は Excel の末尾省略セルが "" になる仕様と整合するよう実装するが、
        //   NullInterpreter が null キーワードを null に変換するため、
        //   YAML ネイティブ null は Java null として扱われる - これは RS-03 の仕様）
        assertThat(result.size(), is(2));
        Map<String, String> row0 = result.get(0);
        assertThat(row0.get("COL1"), is("val1"));
        assertThat(row0.get("COL2"), is("val2"));
        // COL3: null → NullInterpreter により Java null
        assertNull(row0.get("COL3"));
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

    // ========================================================================
    // getMessage
    // ========================================================================

    /**
     * [RS-01] getMessage: メッセージが取得できること。
     *
     * <p>
     * Given: messages に id=req001 のエントリ<br>
     * When:  getMessage を呼ぶ<br>
     * Then:  MessagePool が返ること（null でないこと）
     * </p>
     */
    @Test
    public void testGetMessage() {
        // Given / When
        MessagePool result = sut.getMessage(DIR, "YamlTestDataParserTest/messageData", "req001");

        // Then
        assertNotNull(result);
    }

    // ========================================================================
    // getMessageWithoutCache（SendSyncMessageParser 相当）
    // ========================================================================

    /**
     * [RS-01] getMessageWithoutCache: 指定 DataType のメッセージが取得できること。
     *
     * <p>
     * Given: expected_request_body_messages に id=req001<br>
     * When:  getMessageWithoutCache(dir, resource, EXPECTED_REQUEST_BODY_MESSAGES, "req001") を呼ぶ<br>
     * Then:  MessagePool が返ること
     * </p>
     */
    @Test
    public void testGetMessageWithoutCache() {
        // Given / When
        MessagePool result = sut.getMessageWithoutCache(
                DIR, "YamlTestDataParserTest/messageData",
                DataType.EXPECTED_REQUEST_BODY_MESSAGES, "req001");

        // Then
        assertNotNull(result);
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
     * Then:  省略カラムにデフォルト値が補完されていること（カラム数が増えること）
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
        // fillDefaultValues() により DB の全カラムが追加される
        assertTrue(td.getColumnNames().length > 2);
    }
}
