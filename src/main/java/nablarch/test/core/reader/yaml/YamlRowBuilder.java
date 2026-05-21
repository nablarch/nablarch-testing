package nablarch.test.core.reader.yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * YAML ドキュメント全体から行シーケンスを組み立てるディスパッチャ。
 *
 * <p>
 * YAML トップレベルキーと {@link SectionConverter} のマッピングを保持し、
 * キー順に各エントリを変換して行シーケンスを返す。
 * </p>
 *
 * <p>
 * {@code YamlTestDataReader} が内部で使用するクラスであり、フレームワーク外部からの使用は想定していない。
 * </p>
 */
public class YamlRowBuilder {

    /**
     * セクション種別定義リスト（YAML トップレベルキー順）。<br/>
     * 各コンバータはステートレスであるため、インスタンス間で共有しても安全。
     */
    private static final List<SectionEntry> SECTION_ENTRIES = buildSectionEntries();

    // -----------------------------------------------------------------------
    // パブリック API
    // -----------------------------------------------------------------------

    /**
     * YAML ドキュメントを行シーケンスに変換する。
     *
     * @param yaml YAML トップレベルマップ
     * @return 行シーケンス
     */
    public List<List<String>> build(Map<String, Object> yaml) {
        List<List<String>> result = new ArrayList<List<String>>();
        for (SectionEntry se : SECTION_ENTRIES) {
            Object entries = yaml.get(se.yamlKey);
            if (entries == null) {
                continue;
            }
            for (Object entry : YamlValueConverter.asList(entries)) {
                se.converter.convert(YamlValueConverter.asMap(entry), result);
            }
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // セクション種別定義
    // -----------------------------------------------------------------------

    private static class SectionEntry {
        final String yamlKey;
        final SectionConverter converter;

        SectionEntry(String yamlKey, SectionConverter converter) {
            this.yamlKey = yamlKey;
            this.converter = converter;
        }
    }

    private static List<SectionEntry> buildSectionEntries() {
        return Collections.unmodifiableList(Arrays.asList(
                new SectionEntry("setup_tables",
                        new TableSectionConverter("SETUP_TABLE")),
                new SectionEntry("expected_tables",
                        new TableSectionConverter("EXPECTED_TABLE")),
                new SectionEntry("expected_complete_tables",
                        new TableSectionConverter("EXPECTED_COMPLETE_TABLE")),
                new SectionEntry("list_maps",
                        new ListMapSectionConverter()),
                new SectionEntry("setup_files",
                        new FileSectionConverter("setup_files")),
                new SectionEntry("expected_files",
                        new FileSectionConverter("expected_files")),
                new SectionEntry("messages",
                        new MessageSectionConverter("MESSAGE")),
                new SectionEntry("expected_request_header_messages",
                        new MessageSectionConverter("EXPECTED_REQUEST_HEADER_MESSAGES")),
                new SectionEntry("expected_request_body_messages",
                        new MessageSectionConverter("EXPECTED_REQUEST_BODY_MESSAGES")),
                new SectionEntry("response_header_messages",
                        new GroupMessageSectionConverter("RESPONSE_HEADER_MESSAGES")),
                new SectionEntry("response_body_messages",
                        new GroupMessageSectionConverter("RESPONSE_BODY_MESSAGES"))
        ));
    }
}
