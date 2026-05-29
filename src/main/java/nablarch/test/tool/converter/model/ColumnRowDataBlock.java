package nablarch.test.tool.converter.model;

import nablarch.test.core.reader.DataType;

import java.util.List;

/**
 * テーブルデータ・LIST_MAP の共通基底クラス。
 * カラム名リストとデータ行リストを保持する。
 */
public abstract sealed class ColumnRowDataBlock extends TestDataBlock permits TableDataBlock, ListMapBlock {

    private final List<String> columnNames;
    private final List<List<String>> rows;

    protected ColumnRowDataBlock(DataType dataType, String groupId, String identifier,
                                  List<String> columnNames, List<List<String>> rows) {
        super(dataType, groupId, identifier);
        this.columnNames = columnNames;
        this.rows = rows;
    }

    /** カラム名リスト（マーカーカラムを含む）。 */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /** データ行のリスト（null・空文字を区別して保持）。 */
    public List<List<String>> getRows() {
        return rows;
    }
}
