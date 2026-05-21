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

    /**
     * ファイル系セクションの種類を表す列挙型。<br/>
     * セクション種別ごとの DataType 名プレフィックスを管理する。
     */
    enum FileSection {
        /** setup_files セクション（SETUP_FIXED / SETUP_VARIABLE） */
        SETUP("SETUP_FIXED", "SETUP_VARIABLE"),
        /** expected_files セクション（EXPECTED_FIXED / EXPECTED_VARIABLE） */
        EXPECTED("EXPECTED_FIXED", "EXPECTED_VARIABLE");

        final String fixedDataTypeName;
        final String variableDataTypeName;

        FileSection(String fixedDataTypeName, String variableDataTypeName) {
            this.fixedDataTypeName = fixedDataTypeName;
            this.variableDataTypeName = variableDataTypeName;
        }

        static FileSection of(String yamlKey) {
            if ("setup_files".equals(yamlKey)) {
                return SETUP;
            }
            if ("expected_files".equals(yamlKey)) {
                return EXPECTED;
            }
            throw new IllegalArgumentException("Unknown file section YAML key: " + yamlKey);
        }
    }

    /** ファイル系セクション種別 */
    private final FileSection fileSection;

    /**
     * コンストラクタ。
     *
     * @param yamlKey YAML トップレベルキー（{@code "setup_files"} または {@code "expected_files"}）
     */
    FileSectionConverter(String yamlKey) {
        this.fileSection = FileSection.of(yamlKey);
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
        out.add(YamlValueConverter.singletonRow(header));

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
        if ("variable".equals(type)) {
            return fileSection.variableDataTypeName;
        }
        return fileSection.fixedDataTypeName;
    }
}
