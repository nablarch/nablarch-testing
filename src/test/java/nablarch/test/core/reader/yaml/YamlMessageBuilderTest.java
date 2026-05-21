package nablarch.test.core.reader.yaml;

import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.messaging.RequestTestingMessagePool;
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
import java.util.Set;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

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
        List<nablarch.test.core.util.interpreter.TestDataInterpreter> interpreters =
                repositoryResource.getComponent("interpreters");
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
        nablarch.core.repository.SystemRepository.load(new nablarch.core.repository.ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                java.util.HashMap<String, Object> map = new java.util.HashMap<String, Object>();
                map.put("reader.fwHeaderfields", "customField");
                return map;
            }
        });
        try {
            List<nablarch.test.core.util.interpreter.TestDataInterpreter> interpreters =
                    repositoryResource.getComponent("interpreters");
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
            // リポジトリを元の状態に戻す
            nablarch.core.repository.SystemRepository.load(new nablarch.core.repository.ObjectLoader() {
                @Override
                public Map<String, Object> load() {
                    java.util.HashMap<String, Object> map = new java.util.HashMap<String, Object>();
                    map.put("reader.fwHeaderfields", null);
                    return map;
                }
            });
        }
    }
}
