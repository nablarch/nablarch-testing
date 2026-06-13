package nablarch.test.tool.converter.model;

import nablarch.test.core.reader.DataType;

/**
 * NTF の 1 データブロックを表す中間モデルの抽象基底。
 *
 * <p>
 * データ種別（{@link DataType}）・グループ ID・識別子という全ブロック共通の属性を持つ。
 * 具体ブロックは、カラム／行形式（{@link ColumnRowDataBlock}）・ファイル（{@link FileDataBlock}）・
 * メッセージ（{@link MessageDataBlock}）の 3 系統に限定される（sealed）。
 * </p>
 *
 * @author kiyotis
 */
public abstract sealed class TestDataBlock
        permits ColumnRowDataBlock, FileDataBlock, MessageDataBlock {

    private final DataType dataType;
    private final String groupId;
    private final String identifier;

    /**
     * コンストラクタ。
     *
     * @param dataType   データ種別
     * @param groupId    グループ ID（省略時は空文字）
     * @param identifier ブロックの識別子（テーブル名／ファイルパス／LIST_MAP ID／メッセージ ID）
     */
    protected TestDataBlock(DataType dataType, String groupId, String identifier) {
        this.dataType = dataType;
        this.groupId = groupId;
        this.identifier = identifier;
    }

    /** @return データ種別 */
    public DataType getDataType() {
        return dataType;
    }

    /** @return グループ ID（省略時は空文字） */
    public String getGroupId() {
        return groupId;
    }

    /** @return ブロックの識別子（テーブル名／ファイルパス／LIST_MAP ID／メッセージ ID） */
    public String getIdentifier() {
        return identifier;
    }
}
