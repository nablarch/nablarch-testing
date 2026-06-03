package nablarch.test.core.reader.yaml;

import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.file.MockMessages;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.messaging.RequestTestingMessagePool;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static nablarch.test.core.reader.yaml.YamlSection.FIELD_FW_HEADER;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_GROUP_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_ID;
import static nablarch.test.core.reader.yaml.YamlSection.KEY_MESSAGES;
import static nablarch.test.core.reader.yaml.YamlSection.castMap;
import static nablarch.test.core.reader.yaml.YamlSection.getList;
import static nablarch.test.core.reader.yaml.YamlSection.objectToString;
import static nablarch.test.core.reader.yaml.YamlSection.toStr;

/**
 * YAML から {@link MessagePool} および {@link MockMessages} を構築するビルダー。
 *
 * <p>
 * {@code nablarch.test.core.reader.yaml} パッケージ内のビルダークラスおよび
 * {@link nablarch.test.core.reader.YamlTestDataParser} から使用する。
 * </p>
 *
 * @author kiyotis
 */
public final class YamlMessageBuilder {

    private final YamlFileBuilder fileBuilder;

    public YamlMessageBuilder(List<TestDataInterpreter> interpreters) {
        this.fileBuilder = new YamlFileBuilder(interpreters);
    }

    /**
     * メッセージプールを構築する（getMessage / getMessageWithoutCache 相当）。
     *
     * @param yaml       YAML トップレベル Map
     * @param sectionKey セクションキー。{@code "messages"} の場合のみエントリ直下の {@code fw_header:} マップを読む。
     *                   それ以外（{@code expected_request_*}・{@code response_*}）は空 Map を使用する
     * @param id         メッセージ ID
     * @param basePath   インタープリタ用ベースパス
     * @return {@link RequestTestingMessagePool}、または存在しない場合 null（呼び出し元で null チェックが必要）
     */
    public MessagePool buildMessagePool(Map<String, Object> yaml, String sectionKey,
                                        String id, String basePath) {
        FixedLengthFile file = fileBuilder.buildMessageFile(yaml, sectionKey, id, basePath);
        if (file == null) {
            return null;
        }
        // messages（MESSAGE）経路のみ fw_header: マップを読む。
        // expected_request_* / response_* 経路は空 Map を渡す（フィールド単位で records に定義するため）。
        Map<String, String> fwHeader = KEY_MESSAGES.equals(sectionKey)
                ? extractFwHeader(yaml, id)
                : Collections.emptyMap();
        return new RequestTestingMessagePool(file, fwHeader);
    }

    /**
     * SendSync 用メッセージリストを構築する（getSendSyncMessage 相当）。
     *
     * @param yaml       YAML トップレベル Map
     * @param sectionKey セクションキー
     * @param groupId    グループ ID
     * @param basePath   インタープリタ用ベースパス
     * @return {@link RequestTestingMessagePool} リスト、または存在しない場合 null（呼び出し元で null チェックが必要）
     */
    public List<RequestTestingMessagePool> buildSendSyncMessageList(Map<String, Object> yaml, String sectionKey,
                                                                   String groupId, String basePath) {
        List<Object> entries = getList(yaml, sectionKey);
        List<RequestTestingMessagePool> result = new ArrayList<RequestTestingMessagePool>();
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String entryGroupId = toStr(map.get(FIELD_GROUP_ID));
            if (entryGroupId != null && entryGroupId.equals(groupId)) {
                MockMessages file = buildMockMessages(map, basePath);
                RequestTestingMessagePool pool = new RequestTestingMessagePool(file, Collections.emptyMap());
                String entryId = toStr(map.get(FIELD_ID));
                if (entryId != null) {
                    pool.setRequestId(entryId);
                }
                result.add(pool);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private MockMessages buildMockMessages(Map<String, Object> map, String basePath) {
        String entryId = toStr(map.get(FIELD_ID));
        MockMessages file = new MockMessages(entryId != null ? entryId : "");
        YamlSection.applyDirectives(file, map);
        // MockMessages は固定長メッセージのため buildFragmentsForMock（skipFwHeader=true）でビルドする。
        // skipFwHeader=true により、length が未指定でも setLengths が呼ばれ NPE を防ぐ。
        fileBuilder.buildFragmentsForMock(file, map, basePath);
        return file;
    }

    private Map<String, String> extractFwHeader(Map<String, Object> yaml, String id) {
        List<Object> entries = getList(yaml, KEY_MESSAGES);
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String entryId = toStr(map.get(FIELD_ID));
            if (id.equals(entryId)) {
                Object fwHeaderObj = map.get(FIELD_FW_HEADER);
                if (fwHeaderObj == null) {
                    return Collections.emptyMap();
                }
                if (!(fwHeaderObj instanceof Map)) {
                    throw new IllegalStateException(
                            "fw_header in messages entry id='" + id + "' must be a map, "
                                    + "but was: " + fwHeaderObj.getClass().getSimpleName());
                }
                Map<String, String> fwHeader = new LinkedHashMap<String, String>();
                Map<?, ?> rawMap = (Map<?, ?>) fwHeaderObj;
                for (Map.Entry<?, ?> kv : rawMap.entrySet()) {
                    fwHeader.put(objectToString(kv.getKey()), objectToString(kv.getValue()));
                }
                return fwHeader;
            }
        }
        return Collections.emptyMap();
    }
}
