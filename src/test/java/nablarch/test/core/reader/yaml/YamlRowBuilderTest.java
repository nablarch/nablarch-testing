package nablarch.test.core.reader.yaml;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * {@link YamlRowBuilder} のテスト。
 * YAML ドキュメント全体の行シーケンス組み立てを検証する。
 * 仕様ID参照: {@code docs/ntf-impl-spec-list.md}
 */
public class YamlRowBuilderTest {

    private final YamlRowBuilder sut = new YamlRowBuilder();

    // -------------------------------------------------------------------
    // 空の YAML: 行シーケンスが空
    // -------------------------------------------------------------------

    /**
     * Given: 空の YAML マップ
     * When:  build を呼び出す
     * Then:  空の行シーケンスを返す
     */
    @Test
    public void build_emptyYaml_returnsEmptyList() {
        // Given
        Map<String, Object> yaml = Collections.emptyMap();

        // When
        List<List<String>> result = sut.build(yaml);

        // Then
        assertThat(result.isEmpty(), is(true));
    }

    // -------------------------------------------------------------------
    // setup_tables が変換される
    // -------------------------------------------------------------------

    /**
     * Given: setup_tables に1エントリ（table="USER"、1行）
     * When:  build を呼び出す
     * Then:  セクションヘッダ "SETUP_TABLE=USER" が先頭行に出力される
     */
    @Test
    public void build_setupTables_outputsHeader() {
        // Given
        Map<String, Object> entry = buildTableEntry(null, "USER",
                Collections.singletonList(buildRow("ID", "1")));
        Map<String, Object> yaml = new LinkedHashMap<String, Object>();
        yaml.put("setup_tables", Collections.singletonList(entry));

        // When
        List<List<String>> result = sut.build(yaml);

        // Then
        assertThat(result.get(0).get(0), is("SETUP_TABLE=USER"));
    }

    // -------------------------------------------------------------------
    // 複数セクションが YAML キー順で変換される
    // -------------------------------------------------------------------

    /**
     * Given: setup_tables と list_maps の両方が存在
     * When:  build を呼び出す
     * Then:  setup_tables が list_maps より先に出力される（YAML トップレベルキー順）
     */
    @Test
    public void build_multipleSection_outputInYamlKeyOrder() {
        // Given
        Map<String, Object> tableEntry = buildTableEntry(null, "USER",
                Collections.singletonList(buildRow("ID", "1")));
        Map<String, Object> listMapEntry = new LinkedHashMap<String, Object>();
        listMapEntry.put("id", "params");
        listMapEntry.put("rows", Collections.singletonList(buildRow("K", "v")));

        Map<String, Object> yaml = new LinkedHashMap<String, Object>();
        yaml.put("setup_tables", Collections.singletonList(tableEntry));
        yaml.put("list_maps", Collections.singletonList(listMapEntry));

        // When
        List<List<String>> result = sut.build(yaml);

        // Then: setup_tables が先
        assertThat("先頭行は SETUP_TABLE", result.get(0).get(0), is("SETUP_TABLE=USER"));
        boolean foundListMap = false;
        for (List<String> row : result) {
            if (!row.isEmpty() && row.get(0).startsWith("LIST_MAP=")) {
                foundListMap = true;
                break;
            }
        }
        assertThat("LIST_MAP セクションが存在する", foundListMap, is(true));
    }

    // -------------------------------------------------------------------
    // response_header_messages が変換される
    // -------------------------------------------------------------------

    /**
     * Given: response_header_messages に1エントリ（group_id="g1"、id="resp"）
     * When:  build を呼び出す
     * Then:  セクションヘッダ "RESPONSE_HEADER_MESSAGES[g1]=resp" が出力される（DT-07）
     */
    @Test
    public void build_responseHeaderMessages_outputsHeader() {
        // Given
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("group_id", "g1");
        entry.put("id", "resp");
        entry.put("records", Collections.emptyList());

        Map<String, Object> yaml = new LinkedHashMap<String, Object>();
        yaml.put("response_header_messages", Collections.singletonList(entry));

        // When
        List<List<String>> result = sut.build(yaml);

        // Then
        assertThat(result.get(0).get(0), is("RESPONSE_HEADER_MESSAGES[g1]=resp"));  // DT-07
    }

    // -------------------------------------------------------------------
    // 存在しないキーはスキップされる
    // -------------------------------------------------------------------

    /**
     * Given: expected_tables のみ（setup_tables なし）
     * When:  build を呼び出す
     * Then:  expected_tables のヘッダが先頭に出力される（setup_tables はスキップ）
     */
    @Test
    public void build_missingSection_skipped() {
        // Given
        Map<String, Object> entry = buildTableEntry(null, "ORDERS",
                Collections.singletonList(buildRow("ID", "1")));
        Map<String, Object> yaml = new LinkedHashMap<String, Object>();
        yaml.put("expected_tables", Collections.singletonList(entry));

        // When
        List<List<String>> result = sut.build(yaml);

        // Then: 先頭行は expected_tables のヘッダ
        assertThat(result.get(0).get(0), is("EXPECTED_TABLE=ORDERS"));
    }

    // -----------------------------------------------------------------------
    // ヘルパー
    // -----------------------------------------------------------------------

    private Map<String, Object> buildTableEntry(String groupId, String table,
            List<Map<String, Object>> rows) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        if (groupId != null) {
            entry.put("group_id", groupId);
        }
        entry.put("table", table);
        entry.put("rows", rows);
        return entry;
    }

    private Map<String, Object> buildRow(String key, String value) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put(key, value);
        return row;
    }
}
