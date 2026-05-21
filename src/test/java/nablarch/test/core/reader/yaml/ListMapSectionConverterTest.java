package nablarch.test.core.reader.yaml;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * {@link ListMapSectionConverter} のテスト。
 * SS-19 の LIST_MAP セクション変換を検証する。
 * 仕様ID参照: {@code docs/ntf-impl-spec-list.md}
 */
public class ListMapSectionConverterTest {

    private final ListMapSectionConverter sut = new ListMapSectionConverter();

    // -------------------------------------------------------------------
    // セクションヘッダ: "LIST_MAP=id"
    // -------------------------------------------------------------------

    /**
     * Given: id="params"、1行
     * When:  convert を呼び出す
     * Then:  セクションヘッダ "LIST_MAP=params" が最初の行に出力される（SS-19）
     */
    @Test
    public void convert_headerFormat() {
        // Given
        Map<String, Object> entry = buildEntry("params",
                Collections.singletonList(buildRow("KEY", "val")));

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        assertThat("セクションヘッダ", out.get(0).get(0), is("LIST_MAP=params"));  // SS-19
        assertThat("カラムヘッダ先頭セル", out.get(1).get(0), is(""));
        assertThat("データ行先頭セル", out.get(2).get(0), is(""));
        assertThat("KEY の値", out.get(2).get(out.get(1).indexOf("KEY")), is("val"));
    }

    // -------------------------------------------------------------------
    // rows 空: ヘッダのみ
    // -------------------------------------------------------------------

    /**
     * Given: rows が空
     * When:  convert を呼び出す
     * Then:  セクションヘッダ行1行のみ出力される
     */
    @Test
    public void convert_emptyRows_headerOnly() {
        // Given
        Map<String, Object> entry = buildEntry("empty", Collections.<Map<String, Object>>emptyList());

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        assertThat("行数", out.size(), is(1));
    }

    // -------------------------------------------------------------------
    // 複数行
    // -------------------------------------------------------------------

    /**
     * Given: 2行
     * When:  convert を呼び出す
     * Then:  ヘッダ + カラムヘッダ + 2データ行 = 4行出力される
     */
    @Test
    public void convert_multipleRows_allRowsOutput() {
        // Given
        Map<String, Object> row1 = buildRow("K1", "v1");
        Map<String, Object> row2 = buildRow("K1", "v2");
        Map<String, Object> entry = buildEntry("id", listOf(row1, row2));

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then: ヘッダ + カラムヘッダ + 行1 + 行2
        assertThat("行数", out.size(), is(4));
        assertThat("データ行1", out.get(2).get(1), is("v1"));
        assertThat("データ行2", out.get(3).get(1), is("v2"));
    }

    // -------------------------------------------------------------------
    // QA-14: null 値を含む行の変換（RS-03）
    // -------------------------------------------------------------------

    /**
     * Given: YAML ネイティブ null 値（COL_B が null）を含む行
     * When:  convert を呼び出す
     * Then:  null 値は文字列 "null" に変換される（RS-03）
     */
    @Test
    public void convert_rowWithNullValue_nullConvertedToString() {
        // Given
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("COL_A", "val_a");
        row.put("COL_B", null);  // YAML ネイティブ null
        Map<String, Object> entry = buildEntry("id", Collections.singletonList(row));

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then: null は "null" に変換される（RS-03）
        List<String> colHeader = out.get(1);
        List<String> dataRow   = out.get(2);
        int colBIdx = colHeader.indexOf("COL_B");
        assertThat("null → \"null\"", dataRow.get(colBIdx), is("null"));  // RS-03
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
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("rows", Collections.<Map<String, Object>>emptyList());

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

    private Map<String, Object> buildEntry(String id, List<Map<String, Object>> rows) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("id", id);
        entry.put("rows", rows);
        return entry;
    }

    private Map<String, Object> buildRow(String key, String value) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put(key, value);
        return row;
    }

    @SafeVarargs
    private static <T> List<T> listOf(T... items) {
        List<T> list = new ArrayList<T>();
        for (T item : items) {
            list.add(item);
        }
        return list;
    }
}
