package nablarch.test.tool.converter.model;

import nablarch.test.core.reader.DataType;

import java.util.List;
import java.util.Map;

/**
 * メッセージ（電文）のデータブロック。
 *
 * <p>
 * データ種別は {@link DataType#MESSAGE}／{@link DataType#EXPECTED_REQUEST_HEADER_MESSAGES}／
 * {@link DataType#EXPECTED_REQUEST_BODY_MESSAGES}／{@link DataType#RESPONSE_HEADER_MESSAGES}／
 * {@link DataType#RESPONSE_BODY_MESSAGES} のいずれか。識別子はメッセージ ID を保持する。
 * ディレクティブ・FW 制御ヘッダフィールド・本文レコードレイアウト群を記述順で持つ。
 * </p>
 *
 * <p>
 * FW 制御ヘッダ（{@code requestId}／{@code userId} 等）を読むのは {@code messages} 経路（{@link DataType#MESSAGE}）
 * のみで、{@code expected_request_*}／{@code response_*} 経路は空 Map とする（仕様 MS-04）。
 * </p>
 *
 * <p>getter が返すコレクションは防御的コピーせず保持参照を返すため、呼び出し側は読み取り専用として扱うこと。</p>
 *
 * @author kiyotis
 */
public final class MessageDataBlock extends TestDataBlock {

    private final Map<String, String> directives;
    private final Map<String, String> fwHeaderFields;
    private final List<RecordLayout> records;

    /**
     * コンストラクタ。
     *
     * @param dataType       データ種別
     * @param groupId        グループ ID（省略時は空文字）
     * @param identifier     メッセージ ID
     * @param directives     ディレクティブ（記述順を保つため挿入順を維持する Map を渡すこと）
     * @param fwHeaderFields FW 制御ヘッダフィールド（記述順。FW ヘッダを読まない経路では空 Map）
     * @param records        本文レコードレイアウト群（記述順）
     */
    public MessageDataBlock(DataType dataType, String groupId, String identifier,
                            Map<String, String> directives,
                            Map<String, String> fwHeaderFields, List<RecordLayout> records) {
        super(dataType, groupId, identifier);
        this.directives = directives;
        this.fwHeaderFields = fwHeaderFields;
        this.records = records;
    }

    /** @return ディレクティブ（記述順） */
    public Map<String, String> getDirectives() {
        return directives;
    }

    /** @return FW 制御ヘッダフィールド（記述順。FW ヘッダを読まない経路では空 Map） */
    public Map<String, String> getFwHeaderFields() {
        return fwHeaderFields;
    }

    /** @return 本文レコードレイアウト群（記述順） */
    public List<RecordLayout> getRecords() {
        return records;
    }
}
