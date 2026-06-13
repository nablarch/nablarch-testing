package nablarch.test.core.reader.yaml.model;

import nablarch.core.util.annotation.Published;

import java.util.List;

/**
 * 構造マッピング層が返す list_maps データ（値未加工）。
 *
 * <p>
 * {@code list_maps} の 1 エントリ分を、解釈を施さずに保持する不変オブジェクト（フィールドは final。ただし getter が返すコレクションは防御的コピーせず公開するため、呼び出し側は読み取り専用として扱うこと）。
 * {@link #getColumnNames()} は YAML の記述順を保持する（旧実装の {@code TreeMap} による
 * キーソートは行わない）。マーカーカラム（{@code [COL]} 形式）も保持する。
 * </p>
 *
 * @author kiyotis
 */
@Published(tag = "architect")
public final class RawListMap {

    private final String id;
    private final List<String> columnNames;
    private final List<List<String>> rows;

    /**
     * コンストラクタ。
     *
     * @param id          list_maps エントリの ID
     * @param columnNames カラム名（マーカー含む・YAML 順）
     * @param rows        データ行（カラムに揃えた未加工値。欠損セルは {@code null}）
     */
    public RawListMap(String id, List<String> columnNames, List<List<String>> rows) {
        this.id = id;
        this.columnNames = columnNames;
        this.rows = rows;
    }

    /** @return list_maps エントリの ID */
    public String getId() {
        return id;
    }

    /** @return カラム名（マーカー含む・YAML 順） */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /** @return データ行（未加工値・欠損セルは {@code null}） */
    public List<List<String>> getRows() {
        return rows;
    }
}
