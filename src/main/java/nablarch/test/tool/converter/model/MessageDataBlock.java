package nablarch.test.tool.converter.model;

import nablarch.test.core.reader.DataType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MESSAGE / EXPECTED_REQUEST_*_MESSAGES / RESPONSE_*_MESSAGES のデータブロック。
 */
public final class MessageDataBlock extends TestDataBlock {

    /**
     * 既知のディレクティブ名。「名前｜値」行がこのセットに含まれる場合は
     * FW 制御ヘッダではなくディレクティブとして扱う。
     * NTF の DataRecordFormatterSupport$Directive / FixedLengthDirective / VariableLengthDirective の全名称を網羅する。
     */
    public static final Set<String> KNOWN_DIRECTIVE_NAMES = Set.of(
            "file-type", "text-encoding", "record-separator",
            "record-length",
            "positive-zone-sign-nibble", "negative-zone-sign-nibble",
            "positive-pack-sign-nibble", "negative-pack-sign-nibble",
            "required-decimal-point", "fixed-sign-position", "required-plus-sign",
            "field-separator", "quoting-delimiter", "ignore-blank-lines",
            "requires-title", "max-record-length", "title-record-type-name"
    );

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
