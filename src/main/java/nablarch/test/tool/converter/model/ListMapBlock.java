package nablarch.test.tool.converter.model;

import nablarch.test.core.reader.DataType;

import java.util.List;

/**
 * LIST_MAP（{@code List<Map<String, String>>} 形式・SqlResultSet 比較等に用いる）のデータブロック。
 *
 * <p>
 * データ種別は常に {@link DataType#LIST_MAP}。識別子は LIST_MAP の ID を保持する。
 * 空マッピング由来の空行も空リストとして保持し、行の有無を後段が判別できるようにする。
 * </p>
 *
 * @author kiyotis
 */
public final class ListMapBlock extends ColumnRowDataBlock {

    /**
     * コンストラクタ。データ種別は {@link DataType#LIST_MAP} に固定される。
     *
     * @param groupId     グループ ID（省略時は空文字）
     * @param identifier  LIST_MAP の ID
     * @param columnNames カラム名リスト（記述順）
     * @param rows        データ行のリスト（空行も保持）
     */
    public ListMapBlock(String groupId, String identifier,
                        List<String> columnNames, List<List<String>> rows) {
        super(DataType.LIST_MAP, groupId, identifier, columnNames, rows);
    }
}
