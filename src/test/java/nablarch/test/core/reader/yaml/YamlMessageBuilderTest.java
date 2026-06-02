package nablarch.test.core.reader.yaml;

import nablarch.core.dataformat.LayoutDefinition;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.messaging.RequestTestingMessagePool;
import nablarch.test.core.reader.DataType;
import nablarch.test.core.util.interpreter.TestDataInterpreter;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link YamlMessageBuilder} のテストクラス。
 *
 * <p>
 * MessagePool・MockMessages の構築ロジックを検証する。
 * </p>
 */
@RunWith(DatabaseTestRunner.class)
public class YamlMessageBuilderTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    private static final String RESOURCE_ROOT = "src/test/java/";
    private static final String DIR = RESOURCE_ROOT + "nablarch/test/core/reader/yaml/";

    private YamlMessageBuilder sut;

    @Before
    public void before() {
        List<TestDataInterpreter> interpreters = repositoryResource.getComponent("interpreters");
        sut = new YamlMessageBuilder(interpreters);
    }

    @After
    public void after() {
        YamlLoader.clearCacheForTest();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getFwHeader(MessagePool pool) throws Exception {
        Field f = MessagePool.class.getDeclaredField("fwHeader");
        f.setAccessible(true);
        return (Map<String, String>) f.get(pool);
    }

    // ========================================================================
    // buildMessagePool: getMessage 相当
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: messages の id 指定でメッセージが取得でき、
     * FW ヘッダ（requestId・userId 等）が設定されていること。
     *
     * <p>
     * Given: messages に id=req001 が FW_HEADER/BODY レコードで定義されている<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が返り、requestId="0000000001", userId="testUser01" が設定されていること
     * </p>
     */
    @Test
    public void testBuildMessagePool_withFwHeader() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "messages", "req001", DIR);

        // Then
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));

        // FW ヘッダ実値の検証
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("requestId が設定されていること", fwHeader.get("requestId"), is("0000000001"));
        assertThat("userId が設定されていること", fwHeader.get("userId"), is("testUser01"));
        assertThat("resendFlag が設定されていること", fwHeader.get("resendFlag"), is("0"));
        assertThat("resultCode が設定されていること", fwHeader.get("resultCode"), is("0000"));
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: 存在しない ID を指定した場合は null が返ること。
     *
     * <p>
     * Given: messages に存在しない id<br>
     * When:  buildMessagePool(yaml, "messages", "noSuchId", path) を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void testBuildMessagePool_idNotFound() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "messages", "noSuchId", DIR);

        // Then
        assertNull(result);
    }

    // ========================================================================
    // buildMessagePool: セクションキーに応じたメッセージが取得できること
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: expected_request_body_messages から取得できること。
     *
     * <p>
     * Given: expected_request_body_messages に id=req001<br>
     * When:  buildMessagePool(yaml, "expected_request_body_messages", "req001", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が返ること
     * </p>
     */
    @Test
    public void testBuildMessagePool_expectedRequestBodyMessages() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "expected_request_body_messages", "req001", DIR);

        // Then
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: expected_request_header_messages から取得できること（7.2 G-5）。
     *
     * <p>
     * 解説書 7.2: expected_request_header_messages セクションから buildMessagePool で取得できること<br>
     * Given: expected_request_header_messages に id=req001（FW_HEADER レコード）<br>
     * When:  buildMessagePool(yaml, "expected_request_header_messages", "req001", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が返ること
     * </p>
     */
    @Test
    public void testBuildMessagePool_expectedRequestHeaderMessages() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "expected_request_header_messages", "req001", DIR);

        // Then
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: messages の id にパスセグメントを含む形式が正しく取得できること（7.3 G-4）。
     *
     * <p>
     * 解説書 7.1/7.3: sendSyncTestData/{requestId}/message という id 形式が正しく取得できること<br>
     * Given: messages に id="sendSyncTestData/REQ001/message"<br>
     * When:  buildMessagePool(yaml, "messages", "sendSyncTestData/REQ001/message", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が返り、FW ヘッダの requestId="REQ0000001" であること
     * </p>
     */
    @Test
    public void testBuildMessagePool_idWithPathSegments() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "messages", "sendSyncTestData/REQ001/message", DIR);

        // Then
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("requestId が正しく設定されていること", fwHeader.get("requestId"), is("REQ0000001"));
        assertThat("userId が正しく設定されていること", fwHeader.get("userId"), is("pathUser01"));
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: response_body_messages の id 指定で取得できること。
     *
     * <p>
     * Given: response_body_messages に id=resp001<br>
     * When:  buildMessagePool(yaml, "response_body_messages", "resp001", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が返ること
     * </p>
     */
    @Test
    public void testBuildMessagePool_responseBodyMessages() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "response_body_messages", "resp001", DIR);

        // Then
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));
    }

    // ========================================================================
    // buildSendSyncMessageList: getSendSyncMessage 相当
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: group_id 指定でメッセージリストが取得できること。
     *
     * <p>
     * Given: response_body_messages に group_id=grp1 のエントリ<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "grp1", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool のリストが返ること
     * </p>
     */
    @Test
    public void testBuildSendSyncMessageList_normalCase() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<RequestTestingMessagePool> result = sut.buildSendSyncMessageList(
                yaml, "response_body_messages", "grp1", DIR);

        // Then
        assertNotNull(result);
        assertThat(result.size(), is(1));
    }

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: 存在しない group_id を指定した場合は null が返ること。
     *
     * <p>
     * Given: 存在しない group_id "noSuchGroup"<br>
     * When:  buildSendSyncMessageList を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void testBuildSendSyncMessageList_groupIdNotFound() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<RequestTestingMessagePool> result = sut.buildSendSyncMessageList(
                yaml, "response_body_messages", "noSuchGroup", DIR);

        // Then
        assertNull(result);
    }

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: requestId が MessagePool に設定されること（QA-3）。
     *
     * <p>
     * Given: response_body_messages に id=sync001, group_id=grp1 のエントリ<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "grp1", path) を呼ぶ<br>
     * Then:  result.get(0).getRequestId() が "sync001" を返すこと（QA-3）
     * </p>
     */
    @Test
    public void testBuildSendSyncMessageList_requestIdIsSet() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<RequestTestingMessagePool> result = sut.buildSendSyncMessageList(
                yaml, "response_body_messages", "grp1", DIR);

        // Then
        assertNotNull(result);
        assertThat(result.get(0).getRequestId(), is("sync001"));
    }

    // ========================================================================
    // buildMessageFile: skipFwHeader=true で FW_HEADER フラグメント除外（QA観点1-軽微）
    // ========================================================================

    /**
     * [YamlMessageBuilder/YamlFileBuilder] buildMessagePool: BODY のみの messages を読んだとき
     * FixedLengthFile に 1 フラグメントだけ含まれること。
     *
     * <p>
     * Given: messages に id=req001 が fw_header: マップ + BODY レコードで定義されている<br>
     * When:  YamlFileBuilder.buildMessageFile を呼ぶ（records の BODY のみがフラグメントになる）<br>
     * Then:  FixedLengthFile の layout に BODY レコード 1 件のみ含まれること
     * </p>
     */
    @Test
    public void testBuildMessagePool_fwHeaderFragmentExcluded() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        YamlFileBuilder fileBuilder = new YamlFileBuilder(repositoryResource.<List<TestDataInterpreter>>getComponent("interpreters"));
        FixedLengthFile file = fileBuilder.buildMessageFile(yaml, "messages", "req001", DIR);

        // Then: records に BODY のみ 1 フラグメントであること
        assertNotNull(file);
        LayoutDefinition layout = file.createLayout();
        assertThat("BODY レコードのみが含まれること", layout.getRecords().size(), is(1));
        assertThat("レコードタイプが 'default' に固定されること", layout.getRecords().get(0).getTypeName(), is("default"));
    }

    // ========================================================================
    // buildSendSyncMessageList: directives が MockMessages に設定されること（QA観点1-軽微）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: directives が MockMessages に設定されること。
     *
     * <p>
     * Given: response_body_messages の grp1 エントリに text-encoding: UTF-8 が指定されている<br>
     * When:  buildSendSyncMessageList を呼ぶ<br>
     * Then:  result.get(0).createLayout().getDirective("text-encoding") が "UTF-8" を返すこと
     * </p>
     */
    @Test
    public void testBuildSendSyncMessageList_directivesAreSet() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<RequestTestingMessagePool> result = sut.buildSendSyncMessageList(
                yaml, "response_body_messages", "grp1", DIR);

        // Then: directives が MockMessages に設定されていること（source フィールド経由で確認）
        assertNotNull(result);
        Field sourceField = MessagePool.class.getDeclaredField("source");
        sourceField.setAccessible(true);
        FixedLengthFile source = (FixedLengthFile) sourceField.get(result.get(0));
        assertThat(source.createLayout().getDirective().get("text-encoding"), is("UTF-8"));
    }

    // ========================================================================
    // buildMessageFile: 存在しない ID で null が返ること（QA観点2-軽微）
    // ========================================================================

    /**
     * [YamlFileBuilder] buildMessageFile: 存在しない ID を指定した場合は null が返ること（QA観点2-軽微）。
     *
     * <p>
     * Given: messages に存在しない id<br>
     * When:  YamlFileBuilder.buildMessageFile(yaml, "messages", "noSuchId", path) を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void testBuildMessageFile_idNotFound() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");
        YamlFileBuilder fileBuilder = new YamlFileBuilder(repositoryResource.<List<TestDataInterpreter>>getComponent("interpreters"));

        // When
        FixedLengthFile result = fileBuilder.buildMessageFile(yaml, "messages", "noSuchId", DIR);

        // Then
        assertNull(result);
    }

    // ========================================================================
    // FW_HEADER rows が空のとき例外なく空 Map が返ること（E-3 分岐D）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: fw_header: が空マップの場合、
     * 例外をスローせず空の fwHeader で MessagePool が返ること（E-3 分岐D）。
     *
     * <p>
     * Given: messages に id=emptyRows001 の fw_header が空マップ<br>
     * When:  buildMessagePool(yaml, "messages", "emptyRows001", path) を呼ぶ<br>
     * Then:  MessagePool が返り、fwHeader が空 Map であること
     * </p>
     */
    @Test
    public void testBuildMessagePool_emptyFwHeaderRows() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "messages", "emptyRows001", DIR);

        // Then: 例外なく返り、fwHeader は空 Map
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("fw_header が空マップのとき fwHeader は空 Map であること", fwHeader.size(), is(0));
    }

    // ========================================================================
    // RS-20: FW_HEADER フラグメントが存在しない場合は空 Map を FW ヘッダとして使用すること
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: fw_header: マップがない場合、
     * 空 Map を FW ヘッダとして MessagePool が返ること（RS-20）。
     *
     * <p>
     * 解説書: RS-20（fw_header マップ不在の代替フロー）<br>
     * Given: messages に id=bodyOnly001 の BODY レコードのみ（fw_header マップなし）<br>
     * When:  buildMessagePool(yaml, "messages", "bodyOnly001", path) を呼ぶ<br>
     * Then:  MessagePool が返り、fwHeader が空 Map であること
     * </p>
     */
    @Test
    public void testBuildMessagePool_noFwHeaderFragmentReturnsEmptyFwHeader() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "messages", "bodyOnly001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("fw_header マップがない場合は空 Map が使用されること", fwHeader.size(), is(0));
    }

    // ========================================================================
    // FW_HEADER rows が Map 形式（誤記）のとき IllegalStateException + context（E-3）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: fw_header: の値がマップでなくリスト形式の場合、
     * IllegalStateException がスローされ id がメッセージに含まれること（E-3）。
     *
     * <p>
     * Given: messages に id=malformed001 の fw_header がリスト形式（誤記）<br>
     * When:  buildMessagePool(yaml, "messages", "malformed001", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、id がメッセージに含まれること
     * </p>
     */
    @Test
    public void testBuildMessagePool_malformedFwHeaderRowsThrowsException() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When / Then
        try {
            sut.buildMessagePool(yaml, "messages", "malformed001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            assertThat("id がメッセージに含まれること", e.getMessage(), containsString("malformed001"));
        }
    }

    // ========================================================================
    // dataTypeToSectionKey: 不正DataTypeで IllegalArgumentException（QA観点2-中）
    // ========================================================================

    /**
     * [YamlSection] dataTypeToSectionKey: messaging 以外の DataType を渡した場合 IllegalArgumentException がスローされること（QA観点2-中）。
     *
     * <p>
     * Given: DataType.SETUP_TABLE_DATA（messaging 系以外）<br>
     * When:  YamlSection.dataTypeToSectionKey(DataType.SETUP_TABLE_DATA) を呼ぶ<br>
     * Then:  IllegalArgumentException がスローされること
     * </p>
     */
    @Test
    public void testDataTypeToSectionKey_unsupportedDataTypeThrowsException() {
        // Given / When / Then
        try {
            YamlSection.dataTypeToSectionKey(DataType.SETUP_TABLE_DATA);
            fail("IllegalArgumentException が期待される");
        } catch (IllegalArgumentException e) {
            // OK: 不正な DataType に対して例外がスローされること
        }
    }

    // ========================================================================
    // buildSendSyncMessageList: id なしエントリで requestId が設定されないこと
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: group_id があるが id がないエントリの場合、
     * MessagePool の requestId が null のまま返ること。
     *
     * <p>
     * Given: response_body_messages に group_id=grp2 のエントリが id フィールドなしで定義されている<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "grp2", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が 1 件返り、getRequestId() が null であること
     * </p>
     */
    @Test
    public void testBuildSendSyncMessageList_noIdEntryReturnsPoolWithNullRequestId() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<RequestTestingMessagePool> result = sut.buildSendSyncMessageList(
                yaml, "response_body_messages", "grp2", DIR);

        // Then
        assertNotNull(result);
        assertThat("id なしエントリは 1 件返ること", result.size(), is(1));
        assertNull("id なしエントリの requestId は null であること", result.get(0).getRequestId());
    }

    // ========================================================================
    // extractFwHeader: FW_HEADER の行がフィールド数より少ない場合はスキップされること
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: fw_header: マップに一部のキーのみ含まれる場合、
     * 記載されたキーのみ fwHeader に設定されること。
     *
     * <p>
     * Given: messages_partial_fw_header に id=partialHeader001 の fw_header が requestId のみ<br>
     * When:  buildMessagePool(yaml, "messages_partial_fw_header", "partialHeader001", path) を呼ぶ<br>
     * Then:  fwHeader に requestId のみ設定され、userId は含まれないこと
     * </p>
     */
    @Test
    public void testBuildMessagePool_shortFwHeaderRowOnlyCoversAvailableFields() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "messages", "partialHeader001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("記載された requestId は設定されること", fwHeader.get("requestId"), is("0000000001"));
        assertThat("記載されていない userId は含まれないこと", fwHeader.containsKey("userId"), is(false));
    }


    // ========================================================================
    // fwHeaderFields カスタム設定（QA-4）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: fw_header: マップにプロジェクト独自キーと既定キーが混在する場合、
     * fw_header に記述した全キーが保持されること（fwHeaderFields フィルタ廃止後の確認）。
     *
     * <p>
     * Given: messages に id=req001 の fw_header マップに customField/requestId を記述<br>
     * When:  buildMessagePool を呼ぶ<br>
     * Then:  customField と requestId の両方が FW ヘッダに含まれること
     * </p>
     */
    @Test
    public void testBuildMessagePool_customFwHeaderFields() throws Exception {
        // Given
        List<TestDataInterpreter> interpreters = repositoryResource.getComponent("interpreters");
        YamlMessageBuilder customSut = new YamlMessageBuilder(interpreters);
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/customFwHeaderData");

        // When
        MessagePool result = customSut.buildMessagePool(yaml, "messages", "req001", DIR);

        // Then: fw_header に記述した全キーが保持されること（フィルタなし）
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("独自キー customField が保持されること", fwHeader.get("customField"), is("CUSTOM_VALUE"));
        assertThat("既定キー requestId も保持されること", fwHeader.get("requestId"), is("0000000001"));
    }

    // ========================================================================
    // T2: fw_header マップ対応（ランタイム、messages 限定）
    // ========================================================================

    /**
     * [MS-04] messages の fw_header: マップの全キー（既定＋独自）が getFwHeader() に保持されること。
     *
     * <p>
     * Given: messages に id=req001 のエントリが fw_header: マップ
     *        （requestId/userId/resendFlag/resultCode + customProjectKey）を持つ YAML<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  fwHeader に全キー（既定＋独自）が保持されること
     * </p>
     */
    @Test
    public void testBuildMessagePool_fwHeaderMapAllKeysRetainedIncludingCustom() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "messages", "req001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("requestId が設定されていること", fwHeader.get("requestId"), is("0000000001"));
        assertThat("userId が設定されていること", fwHeader.get("userId"), is("testUser01"));
        assertThat("resendFlag が設定されていること", fwHeader.get("resendFlag"), is("0"));
        assertThat("resultCode が設定されていること", fwHeader.get("resultCode"), is("0000"));
        assertThat("独自キー customProjectKey が黙って消えないこと", fwHeader.get("customProjectKey"), is("PROJECT_VALUE"));
    }

    /**
     * [MS-04] records 側に FW_HEADER レコードがなくても fw_header: マップから FW ヘッダが取得できること。
     *
     * <p>
     * Given: messages エントリに fw_header: マップがあり records には FW_HEADER レコードがない YAML<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  fwHeader に requestId が設定されていること
     * </p>
     */
    @Test
    public void testBuildMessagePool_fwHeaderMapReadableWithoutFwHeaderRecord() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "messages", "req001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("records に FW_HEADER レコードがなくても fw_header マップから取得できること",
                fwHeader.get("requestId"), is("0000000001"));
    }

    /**
     * [MS-04] getMessageWithoutCache（expected/response）経路は extractFwHeader を呼ばず空 Map を渡すこと。
     *
     * <p>
     * Given: expected_request_body_messages に id=req001 のエントリ（fw_header: なし）<br>
     * When:  buildMessagePool(yaml, "expected_request_body_messages", "req001", path) を呼ぶ<br>
     * Then:  fwHeader が空 Map であること（extractFwHeader を呼ばない）
     * </p>
     */
    @Test
    public void testBuildMessagePool_expectedRequestBodyMessagesReturnsEmptyFwHeader() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "expected_request_body_messages", "req001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("expected_request_* 経路は fwHeader が空 Map であること", fwHeader.isEmpty(), is(true));
    }

    /**
     * [MS-04] getMessageWithoutCache（response_*）経路は extractFwHeader を呼ばず空 Map を渡すこと。
     *
     * <p>
     * Given: response_body_messages に id=resp001 のエントリ<br>
     * When:  buildMessagePool(yaml, "response_body_messages", "resp001", path) を呼ぶ<br>
     * Then:  fwHeader が空 Map であること
     * </p>
     */
    @Test
    public void testBuildMessagePool_responseBodyMessagesReturnsEmptyFwHeader() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "response_body_messages", "resp001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("response_* 経路は fwHeader が空 Map であること", fwHeader.isEmpty(), is(true));
    }

    /**
     * [MS-04] getMessageWithoutCache（expected_request_header_messages）経路は extractFwHeader を呼ばず空 Map を渡すこと。
     *
     * <p>
     * Given: expected_request_header_messages に id=req001 のエントリ<br>
     * When:  buildMessagePool(yaml, "expected_request_header_messages", "req001", path) を呼ぶ<br>
     * Then:  fwHeader が空 Map であること
     * </p>
     */
    @Test
    public void testBuildMessagePool_expectedRequestHeaderMessagesReturnsEmptyFwHeader() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "expected_request_header_messages", "req001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("expected_request_header_messages 経路は fwHeader が空 Map であること", fwHeader.isEmpty(), is(true));
    }

    /**
     * [MS-04] getMessageWithoutCache（response_header_messages）経路は extractFwHeader を呼ばず空 Map を渡すこと。
     *
     * <p>
     * Given: response_header_messages に id=resp001 のエントリ<br>
     * When:  buildMessagePool(yaml, "response_header_messages", "resp001", path) を呼ぶ<br>
     * Then:  fwHeader が空 Map であること
     * </p>
     */
    @Test
    public void testBuildMessagePool_responseHeaderMessagesReturnsEmptyFwHeader() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "response_header_messages", "resp001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("response_header_messages 経路は fwHeader が空 Map であること", fwHeader.isEmpty(), is(true));
    }

    /**
     * [MS-04] fw_header: の値がクォートなしの数値・真偽値の場合、文字列に変換されること。
     *
     * <p>
     * Given: messages に id=numericValues001 の fw_header にクォートなし数値 (0, 1234) と真偽値 (true) を記述<br>
     * When:  buildMessagePool(yaml, "messages", "numericValues001", path) を呼ぶ<br>
     * Then:  fwHeader の各値が文字列に変換されていること（"0", "1234", "true"）
     * </p>
     */
    @Test
    public void testBuildMessagePool_fwHeaderMapWithUnquotedNumericAndBooleanValues() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "messages", "numericValues001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("整数値が文字列に変換されること", fwHeader.get("resendFlag"), is("0"));
        assertThat("4桁整数が文字列に変換されること", fwHeader.get("resultCode"), is("1234"));
        assertThat("真偽値が文字列に変換されること", fwHeader.get("boolFlag"), is("true"));
    }

    /**
     * [MS-04] messages の fw_header: がない場合は空 Map を返すこと。
     *
     * <p>
     * Given: messages に id=bodyOnly001 のエントリ（fw_header: なし）<br>
     * When:  buildMessagePool(yaml, "messages", "bodyOnly001", path) を呼ぶ<br>
     * Then:  fwHeader が空 Map であること
     * </p>
     */
    @Test
    public void testBuildMessagePool_messagesWithoutFwHeaderMapReturnsEmptyFwHeader() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "messages", "bodyOnly001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("fw_header: マップなしの messages エントリは空 Map であること", fwHeader.isEmpty(), is(true));
    }
}
