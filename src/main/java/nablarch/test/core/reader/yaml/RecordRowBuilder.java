package nablarch.test.core.reader.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * YAML の record_fragment から行シーケンスを生成するビルダ。
 *
 * <p>
 * 固定長・可変長ファイルおよびメッセージ系で共用する。
 * 生成する行シーケンスは以下の順:
 * <ol>
 *   <li>フィールド名行: [recordType, field1, field2, ...]</li>
 *   <li>型行: ["", type1, type2, ...]</li>
 *   <li>長さ行（固定長のみ）: ["", len1, len2, ...]</li>
 *   <li>値行: ["", val1, val2, ...] （rows の各要素）</li>
 * </ol>
 * </p>
 */
class RecordRowBuilder {

    private RecordRowBuilder() {
    }

    /**
     * record_fragment から行シーケンスを生成して {@code out} に追加する。
     *
     * @param record   YAML の record_fragment マップ
     * @param isFixed  {@code true} の場合は固定長（長さ行を出力する）
     * @param out      変換結果を追記する行シーケンス
     */
    static void addRecordRows(Map<String, Object> record, boolean isFixed, List<List<String>> out) {
        String recordType = YamlValueConverter.asString(record.get("record_type"));
        List<Object> fields = YamlValueConverter.asList(record.get("fields"));

        List<String> names   = new ArrayList<String>();
        List<String> types   = new ArrayList<String>();
        List<String> lengths = new ArrayList<String>();

        for (Object f : fields) {
            Map<String, Object> field = YamlValueConverter.asMap(f);
            names.add(YamlValueConverter.asString(field.get("name")));
            types.add(YamlValueConverter.asString(field.get("type")));
            Object len = field.get("length");
            lengths.add(len == null ? null : YamlValueConverter.toCell(len, false));
        }

        // フィールド名行: [recordType, name1, name2, ...]
        List<String> namesRow = new ArrayList<String>();
        namesRow.add(recordType != null ? recordType : "");
        namesRow.addAll(names);
        out.add(namesRow);

        // 型行: ["", type1, type2, ...]
        List<String> typesRow = new ArrayList<String>();
        typesRow.add("");
        typesRow.addAll(types);
        out.add(typesRow);

        // 長さ行（固定長のみ）: ["", len1, len2, ...]
        if (isFixed) {
            List<String> lengthsRow = new ArrayList<String>();
            lengthsRow.add("");
            for (String len : lengths) {
                lengthsRow.add(len != null ? len : "");
            }
            out.add(lengthsRow);
        }

        // 値行: ["", val1, val2, ...]
        List<Object> rowsList = YamlValueConverter.asList(record.get("rows"));
        for (Object rowObj : rowsList) {
            List<Object> valueList = YamlValueConverter.asList(rowObj);
            List<String> valueRow = new ArrayList<String>();
            valueRow.add("");
            int colCount = fields.size();
            for (int i = 0; i < colCount; i++) {
                if (i < valueList.size()) {
                    valueRow.add(YamlValueConverter.toCell(valueList.get(i), false));
                } else {
                    valueRow.add("");  // RS-06: 末尾補完
                }
            }
            out.add(valueRow);
        }
    }
}
