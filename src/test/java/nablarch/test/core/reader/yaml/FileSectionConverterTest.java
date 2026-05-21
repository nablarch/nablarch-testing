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
 * {@link FileSectionConverter} のテスト。
 * SS-08〜SS-11 の setup_files / expected_files セクション変換を検証する。
 * 仕様ID参照: {@code docs/ntf-impl-spec-list.md}
 */
public class FileSectionConverterTest {

    // -------------------------------------------------------------------
    // setup_files / 固定長: "SETUP_FIXED=path"
    // -------------------------------------------------------------------

    /**
     * Given: setup_files エントリ、type="fixed"、group_id なし
     * When:  convert を呼び出す
     * Then:  セクションヘッダ "SETUP_FIXED=path" が最初の行に出力される（SS-08）
     */
    @Test
    public void convert_setupFixed_noGroupId_headerFormat() {
        // Given
        FileSectionConverter sut = new FileSectionConverter("setup_files");
        Map<String, Object> entry = buildEntry(null, "input/data.dat", "fixed",
                Collections.<String, Object>emptyMap(),
                Collections.<Map<String, Object>>emptyList());

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        assertThat(out.get(0).get(0), is("SETUP_FIXED=input/data.dat"));  // SS-08
    }

    // -------------------------------------------------------------------
    // expected_files / 可変長: "EXPECTED_VARIABLE=path"
    // -------------------------------------------------------------------

    /**
     * Given: expected_files エントリ、type="variable"
     * When:  convert を呼び出す
     * Then:  セクションヘッダ "EXPECTED_VARIABLE=path"（SS-09）
     */
    @Test
    public void convert_expectedVariable_headerFormat() {
        // Given
        FileSectionConverter sut = new FileSectionConverter("expected_files");
        Map<String, Object> entry = buildEntry(null, "output/data.dat", "variable",
                Collections.<String, Object>emptyMap(),
                Collections.<Map<String, Object>>emptyList());

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        assertThat(out.get(0).get(0), is("EXPECTED_VARIABLE=output/data.dat"));  // SS-09
    }

    // -------------------------------------------------------------------
    // QA-8: expected_files / 固定長: "EXPECTED_FIXED=path"
    // -------------------------------------------------------------------

    /**
     * Given: expected_files エントリ、type="fixed"（QA-8）
     * When:  convert を呼び出す
     * Then:  セクションヘッダ "EXPECTED_FIXED=path"
     */
    @Test
    public void convert_expectedFixed_headerFormat() {
        // Given
        FileSectionConverter sut = new FileSectionConverter("expected_files");
        Map<String, Object> entry = buildEntry(null, "output/expected.dat", "fixed",
                Collections.<String, Object>emptyMap(),
                Collections.<Map<String, Object>>emptyList());

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        assertThat(out.get(0).get(0), is("EXPECTED_FIXED=output/expected.dat"));  // QA-8
    }

    // -------------------------------------------------------------------
    // QA-9: setup_files / 可変長: "SETUP_VARIABLE=path"
    // -------------------------------------------------------------------

    /**
     * Given: setup_files エントリ、type="variable"（QA-9）
     * When:  convert を呼び出す
     * Then:  セクションヘッダ "SETUP_VARIABLE=path"
     */
    @Test
    public void convert_setupVariable_headerFormat() {
        // Given
        FileSectionConverter sut = new FileSectionConverter("setup_files");
        Map<String, Object> entry = buildEntry(null, "input/var.dat", "variable",
                Collections.<String, Object>emptyMap(),
                Collections.<Map<String, Object>>emptyList());

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        assertThat(out.get(0).get(0), is("SETUP_VARIABLE=input/var.dat"));  // QA-9
    }

    // -------------------------------------------------------------------
    // group_id あり: "SETUP_FIXED[groupId]=path"
    // -------------------------------------------------------------------

    /**
     * Given: group_id="g1"
     * When:  convert を呼び出す
     * Then:  セクションヘッダ "SETUP_FIXED[g1]=path"（SS-10）
     */
    @Test
    public void convert_withGroupId_headerFormat() {
        // Given
        FileSectionConverter sut = new FileSectionConverter("setup_files");
        Map<String, Object> entry = buildEntry("g1", "data.dat", "fixed",
                Collections.<String, Object>emptyMap(),
                Collections.<Map<String, Object>>emptyList());

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        assertThat(out.get(0).get(0), is("SETUP_FIXED[g1]=data.dat"));  // SS-10
    }

    // -------------------------------------------------------------------
    // ディレクティブ行が出力される
    // -------------------------------------------------------------------

    /**
     * Given: directives に "text-encoding"="MS932" が設定されている
     * When:  convert を呼び出す
     * Then:  ディレクティブ行 ["text-encoding", "MS932"] が出力される（DR-01）
     */
    @Test
    public void convert_directive_outputDirectiveRow() {
        // Given
        FileSectionConverter sut = new FileSectionConverter("setup_files");
        Map<String, Object> directives = new LinkedHashMap<String, Object>();
        directives.put("text-encoding", "MS932");
        Map<String, Object> entry = buildEntry(null, "data.dat", "fixed", directives,
                Collections.<Map<String, Object>>emptyList());

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then: ヘッダ行(0) + ディレクティブ行(1)
        assertThat("ディレクティブキー", out.get(1).get(0), is("text-encoding"));
        assertThat("ディレクティブ値",  out.get(1).get(1), is("MS932"));
    }

    // -------------------------------------------------------------------
    // 固定長 record: 長さ行が出力される
    // -------------------------------------------------------------------

    /**
     * Given: 固定長エントリにレコード1件（2フィールド）、値行1行
     * When:  convert を呼び出す
     * Then:  フィールド名行・型行・長さ行・値行が出力される（SS-11）
     */
    @Test
    public void convert_fixedRecord_outputsLengthRow() {
        // Given
        FileSectionConverter sut = new FileSectionConverter("setup_files");
        Map<String, Object> record = buildRecord("DATA",
                Arrays.asList(
                        buildField("ID", "X", "10"),
                        buildField("NAME", "N", "20")
                ),
                Arrays.asList(Arrays.asList((Object) "001", "山田太郎"))
        );
        Map<String, Object> entry = buildEntry(null, "data.dat", "fixed",
                Collections.<String, Object>emptyMap(),
                Collections.singletonList(record));

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then: ヘッダ(0) + フィールド名行(1) + 型行(2) + 長さ行(3) + 値行(4)
        assertThat("行数", out.size(), is(5));
        assertThat("フィールド名行", out.get(1).get(1), is("ID"));
        assertThat("型行", out.get(2).get(1), is("X"));
        assertThat("長さ行", out.get(3).get(1), is("10"));  // SS-11
        assertThat("値行", out.get(4).get(1), is("001"));
    }

    // -------------------------------------------------------------------
    // 可変長 record: 長さ行が出力されない
    // -------------------------------------------------------------------

    /**
     * Given: 可変長エントリにレコード1件
     * When:  convert を呼び出す
     * Then:  長さ行が出力されない（フィールド名行・型行・値行の3行）
     */
    @Test
    public void convert_variableRecord_noLengthRow() {
        // Given
        FileSectionConverter sut = new FileSectionConverter("setup_files");
        Map<String, Object> record = buildRecord("DATA",
                Arrays.asList(buildField("COL", "X", null)),
                Arrays.asList(Arrays.asList((Object) "val"))
        );
        Map<String, Object> entry = buildEntry(null, "data.dat", "variable",
                Collections.<String, Object>emptyMap(),
                Collections.singletonList(record));

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then: ヘッダ(0) + フィールド名行(1) + 型行(2) + 値行(3) = 4行
        assertThat("行数（長さ行なし）", out.size(), is(4));
    }

    // -------------------------------------------------------------------
    // R-5: type=null のとき固定長（SETUP_FIXED）として扱われること（仕様確認）
    // -------------------------------------------------------------------

    /**
     * Given: type が null（未指定）の setup_files エントリ
     * When:  convert を呼び出す
     * Then:  固定長のデフォルト動作として "SETUP_FIXED=path" ヘッダが出力される（R-5 仕様確認）
     */
    @Test
    public void convert_nullType_defaultsToFixed() {
        // Given
        FileSectionConverter sut = new FileSectionConverter("setup_files");
        Map<String, Object> entry = buildEntry(null, "data.dat", null,  // type=null
                Collections.<String, Object>emptyMap(),
                Collections.<Map<String, Object>>emptyList());

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then: type が null の場合は固定長のデフォルト（SETUP_FIXED）として扱われる（R-5）
        assertThat(out.get(0).get(0), is("SETUP_FIXED=data.dat"));  // R-5
    }

    // -----------------------------------------------------------------------
    // ヘルパー
    // -----------------------------------------------------------------------

    private Map<String, Object> buildEntry(String groupId, String path, String type,
            Map<String, Object> directives, List<Map<String, Object>> records) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        if (groupId != null) {
            entry.put("group_id", groupId);
        }
        entry.put("path", path);
        entry.put("type", type);
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
