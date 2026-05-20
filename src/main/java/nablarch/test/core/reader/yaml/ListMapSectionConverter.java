package nablarch.test.core.reader.yaml;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code list_maps} セクションのエントリを行シーケンスに変換する {@link SectionConverter} 実装。
 *
 * <p>
 * セクションヘッダ行の形式: {@code "LIST_MAP=id"}
 * （SS-19 参照: {@code docs/ntf-impl-spec-list.md}）
 * </p>
 */
class ListMapSectionConverter implements SectionConverter {

    /** {@inheritDoc} */
    @Override
    public void convert(Map<String, Object> entry, List<List<String>> out) {
        String id = YamlValueConverter.asString(entry.get("id"));

        // セクションヘッダ行
        out.add(singletonRow("LIST_MAP=" + id));

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
