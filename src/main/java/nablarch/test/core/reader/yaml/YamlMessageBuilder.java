package nablarch.test.core.reader.yaml;

import nablarch.core.repository.SystemRepository;
import nablarch.test.NablarchTestUtils;
import nablarch.test.core.file.DataFile;
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
import java.util.Set;

import static nablarch.core.util.StringUtil.isNullOrEmpty;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_FIELDS;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_GROUP_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_NAME;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_RECORDS;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_ROWS;
import static nablarch.test.core.reader.yaml.YamlSection.FW_HEADER_RECORD_TYPE;
import static nablarch.test.core.reader.yaml.YamlSection.addBinaryFileInterpreter;
import static nablarch.test.core.reader.yaml.YamlSection.castMap;
import static nablarch.test.core.reader.yaml.YamlSection.getList;
import static nablarch.test.core.reader.yaml.YamlSection.objectToString;
import static nablarch.test.core.reader.yaml.YamlSection.toStr;

/**
 * YAML から {@link MessagePool} および {@link MockMessages} を構築するビルダー。
 *
 * <p>
 * パッケージプライベート。{@code nablarch.test.core.reader.yaml} パッケージ内からのみ使用する。
 * </p>
 */
public final class YamlMessageBuilder {

    /**
     * FW ヘッダフィールド名を SystemRepository から読み込むためのキー。
     * {@link nablarch.test.core.reader.MessageParser} と同じキーを参照する。
     */
    private static final String FW_HEADER_KEY = "reader.fwHeaderfields";

    /**
     * FW 制御ヘッダフィールド名セット。
     * {@value #FW_HEADER_KEY} が SystemRepository に設定されている場合はその値を使用し、
     * 設定がない場合はデフォルト値 {@code {requestId, userId, resendFlag, resultCode}} を使用する。
     */
    private final Set<String> fwHeaderFields;

    private final List<TestDataInterpreter> interpreters;
    private final YamlFileBuilder fileBuilder;

    public YamlMessageBuilder(List<TestDataInterpreter> interpreters) {
        this.interpreters = interpreters;
        this.fileBuilder = new YamlFileBuilder(interpreters);
        this.fwHeaderFields =
                isNullOrEmpty(SystemRepository.getString(FW_HEADER_KEY))
                ? NablarchTestUtils.asSet("requestId", "userId", "resendFlag", "resultCode")
                : NablarchTestUtils.asSet(NablarchTestUtils.makeArray(SystemRepository.getString(FW_HEADER_KEY)));
    }

    /**
     * メッセージプールを構築する（getMessage / getMessageWithoutCache 相当）。
     *
     * @param yaml       YAML トップレベル Map
     * @param sectionKey セクションキー（例: "messages"）
     * @param id         メッセージ ID
     * @param basePath   インタープリタ用ベースパス
     * @return {@link RequestTestingMessagePool}、または存在しない場合 null
     */
    public MessagePool buildMessagePool(Map<String, Object> yaml, String sectionKey,
                                  String id, String basePath) {
        FixedLengthFile file = fileBuilder.buildMessageFile(yaml, sectionKey, id, basePath);
        if (file == null) {
            return null;
        }
        Map<String, String> fwHeader = extractFwHeader(yaml, sectionKey, id);
        return new RequestTestingMessagePool(file, fwHeader);
    }

    /**
     * SendSync 用メッセージリストを構築する（getSendSyncMessage 相当）。
     *
     * @param yaml       YAML トップレベル Map
     * @param sectionKey セクションキー
     * @param groupId    グループ ID
     * @param basePath   インタープリタ用ベースパス
     * @return {@link RequestTestingMessagePool} リスト、または存在しない場合 null
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
                Map<String, String> emptyHeader = Collections.emptyMap();
                RequestTestingMessagePool pool = new RequestTestingMessagePool(file, emptyHeader);
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
        applyDirectives(file, map);
        fileBuilder.buildFragmentsCore(file, map, false, addBinaryFileInterpreter(basePath, interpreters));
        return file;
    }

    private void applyDirectives(DataFile file, Map<String, Object> map) {
        Object directivesObj = map.get(YamlSection.FIELD_DIRECTIVES);
        if (directivesObj == null) {
            return;
        }
        Map<String, Object> directives = castMap(directivesObj);
        for (Map.Entry<String, Object> e : directives.entrySet()) {
            file.setDirective(e.getKey(), toStr(e.getValue()));
        }
    }

    private Map<String, String> extractFwHeader(Map<String, Object> yaml, String sectionKey, String id) {
        List<Object> entries = getList(yaml, sectionKey);
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String entryId = toStr(map.get(FIELD_ID));
            if (id.equals(entryId)) {
                Map<String, String> fwHeader = new LinkedHashMap<String, String>();
                List<Object> records = getList(map, FIELD_RECORDS);
                for (Object recordObj : records) {
                    Map<String, Object> record = castMap(recordObj);
                    if (!FW_HEADER_RECORD_TYPE.equals(toStr(record.get(YamlSection.FIELD_RECORD_TYPE)))) {
                        continue;
                    }
                    List<Object> fields = getList(record, FIELD_FIELDS);
                    List<Object> rows = getList(record, FIELD_ROWS);
                    for (Object fieldObj : fields) {
                        Map<String, Object> field = castMap(fieldObj);
                        String fieldName = toStr(field.get(FIELD_NAME));
                        if (fwHeaderFields.contains(fieldName) && !rows.isEmpty()) {
                            @SuppressWarnings("unchecked")
                            List<Object> firstRow = (List<Object>) rows.get(0);
                            int fieldIndex = fieldIndexOf(fields, fieldName);
                            if (fieldIndex >= 0 && fieldIndex < firstRow.size()) {
                                fwHeader.put(fieldName, objectToString(firstRow.get(fieldIndex)));
                            }
                        }
                    }
                }
                return fwHeader;
            }
        }
        return Collections.emptyMap();
    }

    private int fieldIndexOf(List<Object> fields, String fieldName) {
        for (int i = 0; i < fields.size(); i++) {
            Map<String, Object> field = castMap(fields.get(i));
            if (fieldName.equals(toStr(field.get(FIELD_NAME)))) {
                return i;
            }
        }
        return -1;
    }
}
