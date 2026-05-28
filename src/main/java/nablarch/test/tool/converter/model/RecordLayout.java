package nablarch.test.tool.converter.model;

import java.util.List;

/**
 * ファイルデータブロック・メッセージングデータブロックのレコードレイアウト。
 */
public class RecordLayout {

    private final String recordType;
    private final List<FieldDef> fields;
    private final List<List<String>> rows;

    public RecordLayout(String recordType, List<FieldDef> fields, List<List<String>> rows) {
        this.recordType = recordType;
        this.fields = fields;
        this.rows = rows;
    }

    public String getRecordType() {
        return recordType;
    }

    public List<FieldDef> getFields() {
        return fields;
    }

    public List<List<String>> getRows() {
        return rows;
    }
}
