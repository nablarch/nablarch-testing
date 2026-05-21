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
    // records がある場合: 固定長として行シーケンスが生成される（QA-15強化）
    // -------------------------------------------------------------------

    /**
     * Given: records にレコード1件（STATUS/X/3、値 "200"）
     * When:  convert を呼び出す
     * Then:  フィールド名行・型行・長さ行・値行が出力される（型行・長さ行のアサートを含む）（QA-15）
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
        assertThat("フィールド名行の先頭セル", out.get(1).get(0), is("DATA"));
        assertThat("フィールド名行のフィールド名", out.get(1).get(1), is("STATUS"));
        assertThat("型行の先頭セルは空", out.get(2).get(0), is(""));
        assertThat("型行の型", out.get(2).get(1), is("X"));          // QA-15: 型行アサート
        assertThat("長さ行の先頭セルは空", out.get(3).get(0), is(""));
        assertThat("長さ行の長さ", out.get(3).get(1), is("3"));       // QA-15: 長さ行アサート
        assertThat("値行の値", out.get(4).get(1), is("200"));
    }

    // -------------------------------------------------------------------
    // records が複数件の場合
    // -------------------------------------------------------------------

    /**
     * Given: records に2件のレコード
     * When:  convert を呼び出す
     * Then:  2件分の行シーケンス（各4行）が出力される（MS-06）
     */
    @Test
    public void convert_multipleRecords_allRecordsOutput() {
        // Given
        GroupMessageSectionConverter sut =
                new GroupMessageSectionConverter("RESPONSE_BODY_MESSAGES");
        Map<String, Object> record1 = buildRecord("REQ",
                Arrays.asList(buildField("ID", "X", "5")),
                Arrays.asList(Arrays.asList((Object) "R0001"))
        );
        Map<String, Object> record2 = buildRecord("RSP",
                Arrays.asList(buildField("STATUS", "X", "3")),
                Arrays.asList(Arrays.asList((Object) "200"))
        );
        Map<String, Object> entry = buildEntry("g1", "resp",
                Arrays.asList(record1, record2));

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then: ヘッダ(0) + record1の4行(1-4) + record2の4行(5-8) = 9行
        assertThat("行数", out.size(), is(9));
        assertThat("record1 フィールド名行", out.get(1).get(1), is("ID"));
        assertThat("record2 フィールド名行", out.get(5).get(1), is("STATUS"));
    }

    // -------------------------------------------------------------------
    // E-1: 'id' キー欠落時に IllegalArgumentException
    // -------------------------------------------------------------------

    /**
     * Given: 'id' キーが欠落したエントリ
     * When:  convert を呼び出す
     * Then:  IllegalArgumentException がスローされ、メッセージにキー名が含まれる（E-1）
     */
    @Test
    public void convert_missingId_throwsIllegalArgumentException() {
        // Given: id キーなし
        GroupMessageSectionConverter sut =
                new GroupMessageSectionConverter("RESPONSE_HEADER_MESSAGES");
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("records", Collections.<Map<String, Object>>emptyList());

        // When / Then
        List<List<String>> out = new ArrayList<List<String>>();
        try {
            sut.convert(entry, out);
            fail("IllegalArgumentException が期待される");
        } catch (IllegalArgumentException e) {
            assertThat("例外メッセージに 'id' が含まれること", e.getMessage(), containsString("id"));  // E-1
        }
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
