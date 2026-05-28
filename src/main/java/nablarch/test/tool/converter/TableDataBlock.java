package nablarch.test.tool.converter;

import nablarch.test.core.reader.DataType;

import java.util.List;

/**
 * SETUP_TABLE / EXPECTED_TABLE / EXPECTED_COMPLETE_TABLE のデータブロック。
 */
public class TableDataBlock extends ColumnRowDataBlock {

    public TableDataBlock(DataType dataType, String groupId, String identifier,
                          List<String> columnNames, List<List<String>> rows) {
        super(dataType, groupId, identifier, columnNames, rows);
    }
}
