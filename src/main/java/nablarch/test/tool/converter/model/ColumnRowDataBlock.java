package nablarch.test.tool.converter.model;

import nablarch.test.core.reader.DataType;

import java.util.List;

/**
 * カラム名行とデータ行で表現されるデータブロックの抽象基底。
 *
 * <p>
 * テーブルデータ（{@link TableDataBlock}）と LIST_MAP（{@link ListMapBlock}）の共通基底（sealed）。
 * カラム名リスト（マーカーカラムを含む）とデータ行リストを、加工せず記述順・記法のまま保持する。
 * </p>
 *
 * <p>getter が返すコレクションは防御的コピーせず保持参照を返すため、呼び出し側は読み取り専用として扱うこと。</p>
 *
 * @author kiyotis
 */
public abstract sealed class ColumnRowDataBlock extends TestDataBlock
        permits TableDataBlock, ListMapBlock {

    private final List<String> columnNames;
    private final List<List<String>> rows;

    /**
     * コンストラクタ。
     *
     * @param dataType    データ種別
     * @param groupId     グループ ID（省略時は空文字）
     * @param identifier  識別子（テーブル名／LIST_MAP ID）
     * @param columnNames カラム名リスト（マーカーカラムを含む。記述順・大文字化なし）
     * @param rows        データ行のリスト（{@code null} セルと空文字 {@code ""} を区別し、特殊記法を記法のまま保持）
     */
    protected ColumnRowDataBlock(DataType dataType, String groupId, String identifier,
                                 List<String> columnNames, List<List<String>> rows) {
        super(dataType, groupId, identifier);
        this.columnNames = columnNames;
        this.rows = rows;
    }

    /** @return カラム名リスト（マーカーカラムを含む。記述順・大文字化なし） */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /** @return データ行のリスト（{@code null}・空文字・特殊記法を未加工で保持） */
    public List<List<String>> getRows() {
        return rows;
    }
}
