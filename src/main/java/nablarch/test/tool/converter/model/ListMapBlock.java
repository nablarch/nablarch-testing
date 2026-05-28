package nablarch.test.tool.converter.model;

import nablarch.test.core.reader.DataType;

import java.util.List;

/**
 * LIST_MAP のデータブロック。
 */
public class ListMapBlock extends ColumnRowDataBlock {

    public ListMapBlock(String groupId, String identifier,
                        List<String> columnNames, List<List<String>> rows) {
        super(DataType.LIST_MAP, groupId, identifier, columnNames, rows);
    }
}
