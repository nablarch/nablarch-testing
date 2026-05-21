package nablarch.test.core.reader.yaml;

import java.util.List;
import java.util.Map;

/**
 * {@code response_header_messages} / {@code response_body_messages} セクションのエントリを
 * 行シーケンスに変換する {@link SectionConverter} 実装。
 *
 * <p>
 * セクションヘッダ行の形式（DT-07 / MS-06 参照: {@code docs/ntf-impl-spec-list.md}）:
 * <ul>
 *   <li>{@code group_id} あり: {@code "RESPONSE_HEADER_MESSAGES[groupId]=id"}</li>
 *   <li>{@code group_id} なし: {@code "RESPONSE_HEADER_MESSAGES=id"}</li>
 * </ul>
 * </p>
 */
class GroupMessageSectionConverter implements SectionConverter {

    /** セクションヘッダに使用する DataType 名（例: "RESPONSE_HEADER_MESSAGES"） */
    private final String dataTypeName;

    /**
     * コンストラクタ。
     *
     * @param dataTypeName DataType 名（例: {@code "RESPONSE_HEADER_MESSAGES"}）
     */
    GroupMessageSectionConverter(String dataTypeName) {
        this.dataTypeName = dataTypeName;
    }

    /** {@inheritDoc} */
    @Override
    public void convert(Map<String, Object> entry, List<List<String>> out) {
        String groupId = YamlValueConverter.asString(entry.get("group_id"));
        String id      = YamlValueConverter.asString(entry.get("id"));

        String header = groupId != null
                ? dataTypeName + "[" + groupId + "]=" + id
                : dataTypeName + "=" + id;
        out.add(YamlValueConverter.singletonRow(header));

        List<Object> records = YamlValueConverter.asList(entry.get("records"));
        for (Object rec : records) {
            RecordRowBuilder.addRecordRows(YamlValueConverter.asMap(rec), true, out);
        }
    }
}
