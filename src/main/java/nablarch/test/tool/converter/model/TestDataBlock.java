package nablarch.test.tool.converter.model;

import nablarch.test.core.reader.DataType;

/**
 * NTF の 1 データブロックに相当する抽象クラス。
 */
public abstract sealed class TestDataBlock permits ColumnRowDataBlock, FileDataBlock, MessageDataBlock {

    private final DataType dataType;
    private final String groupId;
    private final String identifier;

    protected TestDataBlock(DataType dataType, String groupId, String identifier) {
        this.dataType = dataType;
        this.groupId = groupId;
        this.identifier = identifier;
    }

    public DataType getDataType() {
        return dataType;
    }

    /** groupId（省略時は空文字）。 */
    public String getGroupId() {
        return groupId;
    }

    public String getIdentifier() {
        return identifier;
    }
}
