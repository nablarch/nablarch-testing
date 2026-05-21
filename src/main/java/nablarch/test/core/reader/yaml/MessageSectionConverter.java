package nablarch.test.core.reader.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code messages} / {@code expected_request_header_messages} /
 * {@code expected_request_body_messages} セクションのエントリを
 * 行シーケンスに変換する {@link SectionConverter} 実装。
 *
 * <p>
 * セクションヘッダ行の形式: {@code "MESSAGE=id"}（MS-01〜MS-03 参照: {@code docs/ntf-impl-spec-list.md}）
 * </p>
 */
class MessageSectionConverter implements SectionConverter {

    /** セクションヘッダに使用する DataType 名（例: "MESSAGE"） */
    private final String dataTypeName;

    /**
     * コンストラクタ。
     *
     * @param dataTypeName DataType 名（例: {@code "MESSAGE"}）
     */
    MessageSectionConverter(String dataTypeName) {
        this.dataTypeName = dataTypeName;
    }

    /** {@inheritDoc} */
    @Override
    public void convert(Map<String, Object> entry, List<List<String>> out) {
        String id = YamlValueConverter.asString(entry.get("id"));
        out.add(YamlValueConverter.singletonRow(dataTypeName + "=" + id));

        // ディレクティブ行
        Map<String, Object> directives = YamlValueConverter.asMap(entry.get("directives"));
        for (Map.Entry<String, Object> d : directives.entrySet()) {
            List<String> row = new ArrayList<String>();
            row.add(d.getKey());
            row.add(YamlValueConverter.toCell(d.getValue(), false));
            out.add(row);
        }

        // messages は固定長のみ
        List<Object> records = YamlValueConverter.asList(entry.get("records"));
        for (Object rec : records) {
            RecordRowBuilder.addRecordRows(YamlValueConverter.asMap(rec), true, out);
        }
    }
}
