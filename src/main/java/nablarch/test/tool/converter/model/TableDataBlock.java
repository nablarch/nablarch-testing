package nablarch.test.tool.converter.model;

import nablarch.test.core.reader.DataType;

import java.util.List;

/**
 * テーブルデータのデータブロック。
 *
 * <p>
 * データ種別は {@link DataType#SETUP_TABLE_DATA}／{@link DataType#EXPECTED_TABLE_DATA}／
 * {@link DataType#EXPECTED_COMPLETED} のいずれか。識別子はテーブル名を保持する。
 * 省略カラムへのデフォルト補完（{@code EXPECTED_COMPLETE_TABLE}）は本体読み込み側の責務であり、
 * 中間モデルでは補完しない。
 * </p>
 *
 * @author kiyotis
 */
public final class TableDataBlock extends ColumnRowDataBlock {

    /**
     * コンストラクタ。
     *
     * @param dataType    データ種別（SETUP_TABLE_DATA／EXPECTED_TABLE_DATA／EXPECTED_COMPLETED）
     * @param groupId     グループ ID（省略時は空文字）
     * @param identifier  テーブル名
     * @param columnNames カラム名リスト（マーカーカラムを含む）
     * @param rows        データ行のリスト
     */
    public TableDataBlock(DataType dataType, String groupId, String identifier,
                          List<String> columnNames, List<List<String>> rows) {
        super(dataType, groupId, identifier, columnNames, rows);
    }
}
