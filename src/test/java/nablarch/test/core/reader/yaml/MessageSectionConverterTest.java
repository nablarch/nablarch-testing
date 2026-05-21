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
 * {@link MessageSectionConverter} のテスト。
 * MS-01〜MS-03 の messages / expected_request_*_messages セクション変換を検証する。
 * 仕様ID参照: {@code docs/ntf-impl-spec-list.md}
 */
public class MessageSectionConverterTest {

    // -------------------------------------------------------------------
    // MESSAGE セクション: ヘッダ "MESSAGE=id"
    // -------------------------------------------------------------------

    /**
     * Given: MESSAGE セクション、id="req001"
     * When:  convert を呼び出す
     * Then:  セクションヘッダ "MESSAGE=req001" が最初の行に出力される（MS-01）
     */
    @Test
    public void convert_messageSection_headerFormat() {
        // Given
        MessageSectionConverter sut = new MessageSectionConverter("MESSAGE");
        Map<String, Object> entry = buildEntry("req001",
                Collections.<String, Object>emptyMap(),
                Collections.<Map<String, Object>>emptyList());

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        assertThat(out.get(0).get(0), is("MESSAGE=req001"));  // MS-01
    }

    // -------------------------------------------------------------------
    // EXPECTED_REQUEST_HEADER_MESSAGES セクション
    // -------------------------------------------------------------------

    /**
     * Given: EXPECTED_REQUEST_HEADER_MESSAGES セクション、id="headerMsg"
     * When:  convert を呼び出す
     * Then:  セクションヘッダ "EXPECTED_REQUEST_HEADER_MESSAGES=headerMsg"（MS-02）
     */
    @Test
    public void convert_expectedRequestHeaderMessages_headerFormat() {
        // Given
        MessageSectionConverter sut = new MessageSectionConverter("EXPECTED_REQUEST_HEADER_MESSAGES");
        Map<String, Object> entry = buildEntry("headerMsg",
                Collections.<String, Object>emptyMap(),
                Collections.<Map<String, Object>>emptyList());

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        assertThat(out.get(0).get(0), is("EXPECTED_REQUEST_HEADER_MESSAGES=headerMsg"));  // MS-02
    }

    // -------------------------------------------------------------------
    // EXPECTED_REQUEST_BODY_MESSAGES セクション（R-8）
    // -------------------------------------------------------------------

    /**
     * Given: EXPECTED_REQUEST_BODY_MESSAGES セクション、id="bodyMsg"
     * When:  convert を呼び出す
     * Then:  セクションヘッダ "EXPECTED_REQUEST_BODY_MESSAGES=bodyMsg"（MS-03 / R-8）
     */
    @Test
    public void convert_expectedRequestBodyMessages_headerFormat() {
        // Given
        MessageSectionConverter sut = new MessageSectionConverter("EXPECTED_REQUEST_BODY_MESSAGES");
        Map<String, Object> entry = buildEntry("bodyMsg",
                Collections.<String, Object>emptyMap(),
                Collections.<Map<String, Object>>emptyList());

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        assertThat(out.get(0).get(0), is("EXPECTED_REQUEST_BODY_MESSAGES=bodyMsg"));  // MS-03
    }

    // -------------------------------------------------------------------
    // ディレクティブ行が出力される
    // -------------------------------------------------------------------

    /**
     * Given: directives に "text-encoding"="UTF-8" が設定されている
     * When:  convert を呼び出す
     * Then:  ディレクティブ行が出力される
     */
    @Test
    public void convert_directive_outputDirectiveRow() {
        // Given
        MessageSectionConverter sut = new MessageSectionConverter("MESSAGE");
        Map<String, Object> directives = new LinkedHashMap<String, Object>();
        directives.put("text-encoding", "UTF-8");
        Map<String, Object> entry = buildEntry("id", directives,
                Collections.<Map<String, Object>>emptyList());

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then: ヘッダ(0) + ディレクティブ(1)
        assertThat("ディレクティブキー", out.get(1).get(0), is("text-encoding"));
        assertThat("ディレクティブ値",  out.get(1).get(1), is("UTF-8"));
    }

    // -------------------------------------------------------------------
    // record があり、固定長として処理される（長さ行が出力される）
    // -------------------------------------------------------------------

    /**
     * Given: レコード1件（2フィールド）
     * When:  convert を呼び出す
     * Then:  フィールド名行・型行・長さ行・値行が出力される（messages は固定長のみ）
     */
    @Test
    public void convert_withRecord_fixedLengthRows() {
        // Given
        MessageSectionConverter sut = new MessageSectionConverter("MESSAGE");
        Map<String, Object> record = buildRecord("DATA",
                Arrays.asList(
                        buildField("ID", "X", "5"),
                        buildField("NAME", "N", "10")
                ),
                Arrays.asList(Arrays.asList((Object) "A0001", "テスト"))
        );
        Map<String, Object> entry = buildEntry("id",
                Collections.<String, Object>emptyMap(),
                Collections.singletonList(record));

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then: ヘッダ(0) + フィールド名行(1) + 型行(2) + 長さ行(3) + 値行(4)
        assertThat("行数", out.size(), is(5));
        assertThat("長さ行が出力される", out.get(3).get(1), is("5"));
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
        MessageSectionConverter sut = new MessageSectionConverter("MESSAGE");
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("directives", Collections.<String, Object>emptyMap());
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

    private Map<String, Object> buildEntry(String id, Map<String, Object> directives,
            List<Map<String, Object>> records) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("id", id);
        entry.put("directives", directives);
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
