package nablarch.test.core.reader.yaml;

import nablarch.core.dataformat.LayoutDefinition;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
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
     * Given: messages_path_id に id="sendSyncTestData/REQ001/message"<br>
     * When:  buildMessagePool(yaml, "messages_path_id", "sendSyncTestData/REQ001/message", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が返り、FW ヘッダの requestId="REQ0000001" であること
     * </p>
     */
    @Test
    public void testBuildMessagePool_idWithPathSegments() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "messages_path_id", "sendSyncTestData/REQ001/message", DIR);

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
        assertThat(result, notNullValue());
        assertThat(result.get(0).getRequestId(), is("sync001"));
    }

    // ========================================================================
    // buildMessageFile: skipFwHeader=true で FW_HEADER フラグメント除外（QA観点1-軽微）
    // ========================================================================

    /**
     * [YamlMessageBuilder/YamlFileBuilder] buildMessagePool: FW_HEADER レコードが FixedLengthFile から除外されること。
     *
     * <p>
     * Given: messages に id=req001 が FW_HEADER + BODY の 2 レコードで定義されている<br>
     * When:  buildMessagePool を呼ぶ（内部で buildMessageFile(skipFwHeader=true) を使用）<br>
     * Then:  FixedLengthFile の layout に BODY レコード 1 件のみ含まれること（FW_HEADER は除外）
     * </p>
     */
    @Test
    public void testBuildMessagePool_fwHeaderFragmentExcluded() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When: YamlFileBuilder 経由で buildMessagePool を呼ぶ（YamlMessageBuilder が buildMessageFile を内部で使用）
        YamlFileBuilder fileBuilder = new YamlFileBuilder(repositoryResource.<List<TestDataInterpreter>>getComponent("interpreters"));
        FixedLengthFile file = fileBuilder.buildMessageFile(yaml, "messages", "req001", DIR);

        // Then: FW_HEADER が除外され BODY のみ 1 フラグメントであること
        assertNotNull(file);
        LayoutDefinition layout = file.createLayout();
        assertThat("FW_HEADER を除いた BODY レコードのみが含まれること", layout.getRecords().size(), is(1));
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
        assertThat(result, notNullValue());
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
     * [YamlMessageBuilder] buildMessagePool: FW_HEADER の rows が空リストの場合、
     * 例外をスローせず空の fwHeader で MessagePool が返ること（E-3 分岐D）。
     *
     * <p>
     * Given: messages_empty_fw_header_rows に id=emptyRows001 の FW_HEADER rows が空リスト<br>
     * When:  buildMessagePool(yaml, "messages_empty_fw_header_rows", "emptyRows001", path) を呼ぶ<br>
     * Then:  MessagePool が返り、fwHeader が空 Map であること（型チェック分岐には到達しない）
     * </p>
     */
    @Test
    public void testBuildMessagePool_emptyFwHeaderRows() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "messages_empty_fw_header_rows", "emptyRows001", DIR);

        // Then: 例外なく返り、fwHeader は空 Map
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("rows が空のとき fwHeader は空 Map であること", fwHeader.size(), is(0));
    }

    // ========================================================================
    // RS-20: FW_HEADER フラグメントが存在しない場合は空 Map を FW ヘッダとして使用すること
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: FW_HEADER フラグメントが存在しない場合、
     * 空 Map を FW ヘッダとして MessagePool が返ること（RS-20）。
     *
     * <p>
     * 解説書: RS-20（FW_HEADER フラグメント不在の代替フロー）<br>
     * Given: messages_no_fw_header に id=bodyOnly001 の BODY レコードのみ（FW_HEADER レコードなし）<br>
     * When:  buildMessagePool(yaml, "messages_no_fw_header", "bodyOnly001", path) を呼ぶ<br>
     * Then:  MessagePool が返り、fwHeader が空 Map であること
     * </p>
     */
    @Test
    public void testBuildMessagePool_noFwHeaderFragmentReturnsEmptyFwHeader() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = sut.buildMessagePool(yaml, "messages_no_fw_header", "bodyOnly001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("FW_HEADER フラグメントが存在しない場合は空 Map が使用されること", fwHeader.size(), is(0));
    }

    // ========================================================================
    // FW_HEADER rows が Map 形式（誤記）のとき IllegalStateException + context（E-3）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: FW_HEADER の rows が Map 形式の場合、
     * IllegalStateException がスローされセクションキーと ID がメッセージに含まれること（E-3）。
     *
     * <p>
     * Given: messages_malformed_fw_header に id=malformed001 の FW_HEADER rows が Map 形式<br>
     * When:  buildMessagePool(yaml, "messages_malformed_fw_header", "malformed001", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、sectionKey と id がメッセージに含まれること
     * </p>
     */
    @Test
    public void testBuildMessagePool_malformedFwHeaderRowsThrowsException() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When / Then
        try {
            sut.buildMessagePool(yaml, "messages_malformed_fw_header", "malformed001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            assertThat("セクションキーがメッセージに含まれること", e.getMessage(), containsString("messages_malformed_fw_header"));
            assertThat("IDがメッセージに含まれること", e.getMessage(), containsString("malformed001"));
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
    // fwHeaderFields カスタム設定（QA-4）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: reader.fwHeaderfields が SystemRepository に設定されている場合、
     * そのフィールドのみ FW ヘッダとして抽出されること（QA-4）。
     *
     * <p>
     * Given: SystemRepository に reader.fwHeaderfields=customField を一時設定した YamlMessageBuilder<br>
     *        messages に id=req001 が FW_HEADER/BODY レコードで定義されている<br>
     * When:  buildMessagePool を呼ぶ<br>
     * Then:  customField が FW ヘッダに含まれ、requestId は含まれないこと（QA-4）
     * </p>
     */
    @Test
    public void testBuildMessagePool_customFwHeaderFields() throws Exception {
        // Given: reader.fwHeaderfields を一時設定
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("reader.fwHeaderfields", "customField");
                return map;
            }
        });
        try {
            List<TestDataInterpreter> interpreters = repositoryResource.getComponent("interpreters");
            YamlMessageBuilder customSut = new YamlMessageBuilder(interpreters);
            Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/customFwHeaderData");

            // When
            MessagePool result = customSut.buildMessagePool(yaml, "messages", "req001", DIR);

            // Then
            assertNotNull(result);
            Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
            fwHeaderField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
            assertThat("customField が設定されていること", fwHeader.get("customField"), is("CUSTOM_VALUE"));
            assertThat("requestId は含まれないこと", fwHeader.containsKey("requestId"), is(false));
        } finally {
            // テスト後に reader.fwHeaderfields を null に戻す。
            // YamlMessageBuilder は isNullOrEmpty(null) を true と判断してデフォルト値にフォールバックするため、
            // 後続テストが reader.fwHeaderfields の影響を受けないことが保証される。
            SystemRepository.load(new ObjectLoader() {
                @Override
                public Map<String, Object> load() {
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    map.put("reader.fwHeaderfields", null);
                    return map;
                }
            });
        }
    }
}
