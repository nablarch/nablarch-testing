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
 * {@link GroupMessageSectionConverter} のテスト。
 * DT-07 / MS-06 の response_*_messages セクション変換を検証する。
 * 仕様ID参照: {@code docs/ntf-impl-spec-list.md}
 */
public class GroupMessageSectionConverterTest {

    // -------------------------------------------------------------------
    // group_id なし: "RESPONSE_HEADER_MESSAGES=id"
    // -------------------------------------------------------------------

    /**
     * Given: group_id なし、id="respHeader"
     * When:  convert を呼び出す
     * Then:  セクションヘッダ "RESPONSE_HEADER_MESSAGES=respHeader"（DT-07）
     */
    @Test
    public void convert_noGroupId_headerFormat() {
        // Given
        GroupMessageSectionConverter sut =
                new GroupMessageSectionConverter("RESPONSE_HEADER_MESSAGES");
        Map<String, Object> entry = buildEntry(null, "respHeader",
                Collections.<Map<String, Object>>emptyList());

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        assertThat(out.get(0).get(0), is("RESPONSE_HEADER_MESSAGES=respHeader"));  // DT-07
    }

    // -------------------------------------------------------------------
    // group_id あり: "RESPONSE_BODY_MESSAGES[g1]=id"
    // -------------------------------------------------------------------

    /**
     * Given: group_id="g1"、id="respBody"
     * When:  convert を呼び出す
     * Then:  セクションヘッダ "RESPONSE_BODY_MESSAGES[g1]=respBody"（MS-06）
     */
    @Test
    public void convert_withGroupId_headerFormat() {
        // Given
        GroupMessageSectionConverter sut =
                new GroupMessageSectionConverter("RESPONSE_BODY_MESSAGES");
        Map<String, Object> entry = buildEntry("g1", "respBody",
                Collections.<Map<String, Object>>emptyList());

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        assertThat(out.get(0).get(0), is("RESPONSE_BODY_MESSAGES[g1]=respBody"));  // MS-06
    }

    // -------------------------------------------------------------------
    // records がある場合: 固定長として行シーケンスが生成される
    // -------------------------------------------------------------------

    /**
     * Given: records にレコード1件
     * When:  convert を呼び出す
     * Then:  フィールド名行・型行・長さ行・値行が出力される
     */
    @Test
    public void convert_withRecord_fixedLengthRows() {
        // Given
        GroupMessageSectionConverter sut =
                new GroupMessageSectionConverter("RESPONSE_HEADER_MESSAGES");
        Map<String, Object> record = buildRecord("DATA",
                Arrays.asList(buildField("STATUS", "X", "3")),
                Arrays.asList(Arrays.asList((Object) "200"))
        );
        Map<String, Object> entry = buildEntry(null, "resp",
                Collections.singletonList(record));

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then: ヘッダ(0) + フィールド名行(1) + 型行(2) + 長さ行(3) + 値行(4)
        assertThat("行数", out.size(), is(5));
        assertThat("フィールド名行", out.get(1).get(1), is("STATUS"));
        assertThat("値行", out.get(4).get(1), is("200"));
    }

    // -----------------------------------------------------------------------
    // ヘルパー
    // -----------------------------------------------------------------------

    private Map<String, Object> buildEntry(String groupId, String id,
            List<Map<String, Object>> records) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        if (groupId != null) {
            entry.put("group_id", groupId);
        }
        entry.put("id", id);
        entry.put("records", records);
        return entry;
    }

    private Map<String, Object> buildRecord(String recordType,
            List<Map<String, Object>> fields, List<List<Object>> rows) {
        Map<String, Object> record = new LinkedHashMap<String, Object>();
        if (recordType != null) {
            record.put("record_type", recordType);
        }
        record.put("fields", fields);
        record.put("rows", rows);
        return record;
    }

    private Map<String, Object> buildField(String name, String type, String length) {
        Map<String, Object> field = new LinkedHashMap<String, Object>();
        field.put("name", name);
        field.put("type", type);
        if (length != null) {
            field.put("length", length);
        }
        return field;
    }
}
