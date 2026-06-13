package nablarch.test.tool.converter.model;

import java.util.List;

/**
 * ファイル／メッセージのレコードレイアウト。
 *
 * <p>
 * レコード種別・フィールド定義群・データ行を保持する。レコード種別の省略は {@code null} のまま保持し、
 * デフォルト補完は行わない（本体読み込み側の責務）。フィールド定義・データ行は記述順・記法のまま保持する。
 * </p>
 *
 * <p>getter が返すコレクションは防御的コピーせず保持参照を返すため、呼び出し側は読み取り専用として扱うこと。</p>
 *
 * @author kiyotis
 */
public final class RecordLayout {

    private final String recordType;
    private final List<FieldDef> fields;
    private final List<List<String>> rows;

    /**
     * コンストラクタ。
     *
     * @param recordType レコード種別（省略時は {@code null}。FW_HEADER 等もそのまま保持）
     * @param fields     フィールド定義群（記述順）
     * @param rows       データ行のリスト（{@code null}・空文字・特殊記法を未加工で保持）
     */
    public RecordLayout(String recordType, List<FieldDef> fields, List<List<String>> rows) {
        this.recordType = recordType;
        this.fields = fields;
        this.rows = rows;
    }

    /** @return レコード種別（省略時は {@code null}） */
    public String getRecordType() {
        return recordType;
    }

    /** @return フィールド定義群（記述順） */
    public List<FieldDef> getFields() {
        return fields;
    }

    /** @return データ行のリスト（未加工） */
    public List<List<String>> getRows() {
        return rows;
    }
}
