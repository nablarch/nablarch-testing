package nablarch.test.core.reader.yaml.model;

import nablarch.core.util.annotation.Published;

import java.util.List;

/**
 * 構造マッピング層が返すテーブルデータ（値未加工）。
 *
 * <p>
 * {@code setup_tables}／{@code expected_tables}／{@code expected_complete_tables} の 1 エントリ分を、
 * 解釈・補完・大文字化を一切施さずに保持する不変オブジェクト（フィールドは final。ただし getter が返すコレクションは防御的コピーせず公開するため、呼び出し側は読み取り専用として扱うこと）。
 * </p>
 *
 * <p>
 * {@link #getColumnNames()} はマーカーカラム（{@code [COL]} 形式）を含み、YAML の記述順・大文字小文字を保持する。
 * {@link #getRows()} は各カラムに揃えた値（欠損セルは {@code null}）を未加工で保持する。
 * </p>
 *
 * @author kiyotis
 */
@Published(tag = "architect")
public final class RawTableData {

    private final String groupId;
    private final String tableName;
    private final List<String> columnNames;
    private final List<List<String>> rows;

    /**
     * コンストラクタ。
     *
     * @param groupId     グループ ID（YAML 記述のまま。省略時 {@code null}）
     * @param tableName   テーブル名（YAML 記述のまま）
     * @param columnNames カラム名（マーカー含む・YAML 順・大文字化なし）
     * @param rows        データ行（カラムに揃えた未加工値。欠損セルは {@code null}）
     */
    public RawTableData(String groupId, String tableName,
                        List<String> columnNames, List<List<String>> rows) {
        this.groupId = groupId;
        this.tableName = tableName;
        this.columnNames = columnNames;
        this.rows = rows;
    }

    /** @return グループ ID（省略時 {@code null}） */
    public String getGroupId() {
        return groupId;
    }

    /** @return テーブル名 */
    public String getTableName() {
        return tableName;
    }

    /** @return カラム名（マーカー含む・YAML 順・大文字化なし） */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /** @return データ行（未加工値・欠損セルは {@code null}） */
    public List<List<String>> getRows() {
        return rows;
    }
}
