package nablarch.test.core.reader.yaml.model;

import nablarch.core.util.annotation.Published;

import java.util.List;

/**
 * 構造マッピング層が返すレコードレイアウト（値未加工）。
 *
 * <p>
 * ファイルデータ・メッセージの 1 レコード分のレイアウト（フィールド定義群）とデータ行を、
 * 解釈を施さずに保持する不変オブジェクト（フィールドは final。ただし getter が返すコレクションは防御的コピーせず公開するため、呼び出し側は読み取り専用として扱うこと）。{@code record_type} は省略時 {@code null}、
 * FW 制御ヘッダ（{@code FW_HEADER}）もスキップせずそのまま保持する。
 * </p>
 *
 * @author kiyotis
 */
@Published(tag = "architect")
public final class RawRecordLayout {

    private final String recordType;
    private final List<RawFieldDef> fields;
    private final List<List<String>> rows;

    /**
     * コンストラクタ。
     *
     * @param recordType レコード種別（YAML 記述のまま。省略時 {@code null}）
     * @param fields     フィールド定義群（YAML 順）
     * @param rows       データ行（各セルは YAML 記述のままの文字列。{@code null} 可）
     */
    public RawRecordLayout(String recordType, List<RawFieldDef> fields, List<List<String>> rows) {
        this.recordType = recordType;
        this.fields = fields;
        this.rows = rows;
    }

    /** @return レコード種別（省略時 {@code null}） */
    public String getRecordType() {
        return recordType;
    }

    /** @return フィールド定義群（YAML 順） */
    public List<RawFieldDef> getFields() {
        return fields;
    }

    /** @return データ行（各セルは未加工） */
    public List<List<String>> getRows() {
        return rows;
    }
}
