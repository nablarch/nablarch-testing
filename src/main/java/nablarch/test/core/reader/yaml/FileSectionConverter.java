package nablarch.test.core.reader.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code setup_files} / {@code expected_files} セクションのエントリを
 * 行シーケンスに変換する {@link SectionConverter} 実装。
 *
 * <p>
 * セクションヘッダ行の形式（SS-08〜SS-11 参照: {@code docs/ntf-impl-spec-list.md}）:
 * <ul>
 *   <li>固定長: {@code "SETUP_FIXED=path"} / {@code "EXPECTED_FIXED=path"}</li>
 *   <li>可変長: {@code "SETUP_VARIABLE=path"} / {@code "EXPECTED_VARIABLE=path"}</li>
 *   <li>{@code group_id} あり: {@code "SETUP_FIXED[groupId]=path"}</li>
 * </ul>
 * </p>
 */
class FileSectionConverter implements SectionConverter {

    /** YAML トップレベルキー（"setup_files" or "expected_files"） */
    private final String yamlKey;

    /**
     * コンストラクタ。
     *
     * @param yamlKey YAML トップレベルキー
     */
    FileSectionConverter(String yamlKey) {
        this.yamlKey = yamlKey;
    }

    /** {@inheritDoc} */
    @Override
    public void convert(Map<String, Object> entry, List<List<String>> out) {
        String groupId = YamlValueConverter.asString(entry.get("group_id"));
        String path    = YamlValueConverter.asString(entry.get("path"));
        String type    = YamlValueConverter.asString(entry.get("type")); // "fixed" or "variable"

        String dataTypeName = resolveDataTypeName(type);

        // セクションヘッダ行
        String header = groupId == null
                ? dataTypeName + "=" + path
                : dataTypeName + "[" + groupId + "]=" + path;
        out.add(singletonRow(header));

        // ディレクティブ行
        Map<String, Object> directives = YamlValueConverter.asMap(entry.get("directives"));
        for (Map.Entry<String, Object> d : directives.entrySet()) {
            List<String> row = new ArrayList<String>();
            row.add(d.getKey());
            row.add(YamlValueConverter.toCell(d.getValue(), false));
            out.add(row);
        }

        // records
        boolean isFixed = !"variable".equals(type);
        List<Object> records = YamlValueConverter.asList(entry.get("records"));
        for (Object rec : records) {
            RecordRowBuilder.addRecordRows(YamlValueConverter.asMap(rec), isFixed, out);
        }
    }

    private String resolveDataTypeName(String type) {
        boolean isSetup = yamlKey.startsWith("setup");
        if ("variable".equals(type)) {
            return isSetup ? "SETUP_VARIABLE" : "EXPECTED_VARIABLE";
        }
        return isSetup ? "SETUP_FIXED" : "EXPECTED_FIXED";
    }

    private static List<String> singletonRow(String value) {
        List<String> row = new ArrayList<String>(1);
        row.add(value);
        return row;
    }
}
