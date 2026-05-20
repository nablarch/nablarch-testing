package nablarch.test.core.reader.yaml;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code setup_tables} / {@code expected_tables} / {@code expected_complete_tables}
 * セクションのエントリを行シーケンスに変換する {@link SectionConverter} 実装。
 *
 * <p>
 * セクションヘッダ行（SS-01〜SS-03 参照: {@code docs/ntf-impl-spec-list.md}）の形式:
 * <ul>
 *   <li>{@code group_id} なし: {@code "SETUP_TABLE=TABLE_NAME"}</li>
 *   <li>{@code group_id} あり: {@code "SETUP_TABLE[groupId]=TABLE_NAME"}</li>
 * </ul>
 * </p>
 */
class TableSectionConverter implements SectionConverter {

    /** セクションヘッダに使用する DataType 名（例: "SETUP_TABLE"） */
    private final String dataTypeName;

    /**
     * コンストラクタ。
     *
     * @param dataTypeName DataType 名（例: {@code "SETUP_TABLE"}）
     */
    TableSectionConverter(String dataTypeName) {
        this.dataTypeName = dataTypeName;
    }

    /** {@inheritDoc} */
    @Override
    public void convert(Map<String, Object> entry, List<List<String>> out) {
        String groupId   = YamlValueConverter.asString(entry.get("group_id"));
        String tableName = YamlValueConverter.asString(entry.get("table"));

        // セクションヘッダ行
        String header = groupId == null
                ? dataTypeName + "=" + tableName
                : dataTypeName + "[" + groupId + "]=" + tableName;
        out.add(singletonRow(header));

        List<Map<String, Object>> rows = YamlValueConverter.asMapList(entry.get("rows"));
        if (rows.isEmpty()) {
            return;
        }

        Set<String> allKeys = collectAllKeys(rows);

        // カラムヘッダ行: ["", col1, col2, ...]
        List<String> colHeader = new ArrayList<String>();
        colHeader.add("");
        colHeader.addAll(allKeys);
        out.add(colHeader);

        // データ行: ["", val1, val2, ...]
        for (Map<String, Object> row : rows) {
            List<String> dataRow = new ArrayList<String>();
            dataRow.add("");
            for (String key : allKeys) {
                dataRow.add(YamlValueConverter.toCell(row.get(key), !row.containsKey(key)));
            }
            out.add(dataRow);
        }
    }

    /** 全行の全キーを挿入順で収集する（union）。 */
    private static Set<String> collectAllKeys(List<Map<String, Object>> rows) {
        Set<String> keys = new LinkedHashSet<String>();
        for (Map<String, Object> row : rows) {
            keys.addAll(row.keySet());
        }
        return keys;
    }

    private static List<String> singletonRow(String value) {
        List<String> row = new ArrayList<String>(1);
        row.add(value);
        return row;
    }
}
