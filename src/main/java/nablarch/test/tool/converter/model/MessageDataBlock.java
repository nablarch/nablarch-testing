package nablarch.test.tool.converter.model;

import nablarch.test.core.reader.DataType;

import java.util.List;
import java.util.Map;

/**
 * MESSAGE / EXPECTED_REQUEST_*_MESSAGES / RESPONSE_*_MESSAGES のデータブロック。
 */
public final class MessageDataBlock extends TestDataBlock {

    /** ディレクティブ（text-encoding 等）。Excel の行順を保持するため LinkedHashMap を使用する。 */
    private final Map<String, String> directives;
    /** FW 制御ヘッダフィールド（requestId/userId 等）。Excel の行順を保持するため LinkedHashMap を使用する。 */
    private final Map<String, String> fwHeaderFields;
    /** レコードレイアウトのリスト（FieldDef は name のみ）。 */
    private final List<RecordLayout> records;

    public MessageDataBlock(DataType dataType, String groupId, String identifier,
                            Map<String, String> directives,
                            Map<String, String> fwHeaderFields, List<RecordLayout> records) {
        super(dataType, groupId, identifier);
        this.directives = directives;
        this.fwHeaderFields = fwHeaderFields;
        this.records = records;
    }

    public Map<String, String> getDirectives() {
        return directives;
    }

    public Map<String, String> getFwHeaderFields() {
        return fwHeaderFields;
    }

    public List<RecordLayout> getRecords() {
        return records;
    }
}
