package nablarch.test.core.reader.yaml;

import java.util.List;
import java.util.Map;

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
        if (id == null) {
            throw new IllegalArgumentException(
                    "list_maps entry is missing required key 'id'. entry=" + entry);
        }

        // セクションヘッダ行
        out.add(YamlValueConverter.singletonRow("LIST_MAP=" + id));

        List<Map<String, Object>> rows = YamlValueConverter.asMapList(entry.get("rows"));
        if (rows.isEmpty()) {
            return;
        }

        TableSectionConverter.addKeyValueRows(rows, out);
    }
}
