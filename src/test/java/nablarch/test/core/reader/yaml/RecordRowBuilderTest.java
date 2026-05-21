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
 * {@link RecordRowBuilder} のテスト。
 * 固定長・可変長の record_fragment から行シーケンスが正しく生成されることを検証する。
 * RS-06 の末尾補完も検証する。
 * 仕様ID参照: {@code docs/ntf-impl-spec-list.md}
 */
public class RecordRowBuilderTest {

    // -------------------------------------------------------------------
    // 固定長（isFixed=true）: フィールド名行・型行・長さ行・値行が出力される
    // -------------------------------------------------------------------

    /**
     * Given: record_type="DATA", 2フィールド（USER_ID/X/10, USER_NAME/N/20）, 1値行
     * When:  addRecordRows(record, true, out) を呼び出す
     * Then:  フィールド名行・型行・長さ行・値行の4行が出力される
     */
    @Test
    public void addRecordRows_fixed_outputsFourRows() {
        // Given
        Map<String, Object> record = buildRecord("DATA",
                Arrays.asList(
                        buildField("USER_ID", "X", "10"),
                        buildField("USER_NAME", "N", "20")
                ),
                Arrays.asList(
                        Arrays.asList((Object) "001", "山田太郎")
                )
        );

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        RecordRowBuilder.addRecordRows(record, true, out);

        // Then: 4行（フィールド名行 + 型行 + 長さ行 + 値行1）
        assertThat("行数", out.size(), is(4));

        // フィールド名行: ["DATA", "USER_ID", "USER_NAME"]
        assertThat(out.get(0), is(Arrays.asList("DATA", "USER_ID", "USER_NAME")));
        // 型行: ["", "X", "N"]
        assertThat(out.get(1), is(Arrays.asList("", "X", "N")));
        // 長さ行: ["", "10", "20"]
        assertThat(out.get(2), is(Arrays.asList("", "10", "20")));
        // 値行: ["", "001", "山田太郎"]
        assertThat(out.get(3), is(Arrays.asList("", "001", "山田太郎")));
    }

    // -------------------------------------------------------------------
    // 可変長（isFixed=false）: 長さ行が出力されない
    // -------------------------------------------------------------------

    /**
     * Given: isFixed=false（可変長）
     * When:  addRecordRows を呼び出す
     * Then:  フィールド名行・型行・値行の3行のみ出力される（長さ行なし）
     */
    @Test
    public void addRecordRows_variable_outputsThreeRows() {
        // Given
        Map<String, Object> record = buildRecord("DATA",
                Arrays.asList(
                        buildField("COL1", "X", null),
                        buildField("COL2", "N", null)
                ),
                Arrays.asList(
                        Arrays.asList((Object) "a", "b")
                )
        );

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        RecordRowBuilder.addRecordRows(record, false, out);

        // Then: 3行（フィールド名行 + 型行 + 値行1、長さ行なし）
        assertThat("行数", out.size(), is(3));
        assertThat("フィールド名行", out.get(0), is(Arrays.asList("DATA", "COL1", "COL2")));
        assertThat("型行", out.get(1), is(Arrays.asList("", "X", "N")));
        assertThat("値行", out.get(2), is(Arrays.asList("", "a", "b")));
    }

    // -------------------------------------------------------------------
    // 複数値行
    // -------------------------------------------------------------------

    /**
     * Given: 2値行
     * When:  addRecordRows を呼び出す
     * Then:  2つの値行が出力される
     */
    @Test
    public void addRecordRows_multipleValueRows() {
        // Given
        Map<String, Object> record = buildRecord("DATA",
                Arrays.asList(buildField("COL", "X", "5")),
                Arrays.asList(
                        Arrays.asList((Object) "AAA"),
                        Arrays.asList((Object) "BBB")
                )
        );

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        RecordRowBuilder.addRecordRows(record, true, out);

        // Then: 5行（フィールド名行 + 型行 + 長さ行 + 値行1 + 値行2）
        assertThat("行数", out.size(), is(5));
        assertThat("値行1", out.get(3), is(Arrays.asList("", "AAA")));
    }

    // -------------------------------------------------------------------
    // RS-06: 値行の末尾省略補完
    // -------------------------------------------------------------------

    /**
     * Given: 値行が2フィールド定義に対し1要素のみ（末尾省略）
     * When:  addRecordRows を呼び出す
     * Then:  省略されたフィールドは "" で補完される（RS-06）
     */
    @Test
    public void addRecordRows_valueRowTrailingOmitted_paddedWithEmpty() {
        // Given
        Map<String, Object> record = buildRecord("DATA",
                Arrays.asList(
                        buildField("COL1", "X", "5"),
                        buildField("COL2", "X", "5")
                ),
                Arrays.asList(
                        Arrays.<Object>asList("AAA")  // COL2 省略
                )
        );

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        RecordRowBuilder.addRecordRows(record, true, out);

        // Then: 値行 ["", "AAA", ""] (RS-06)
        List<String> valueRow = out.get(3);
        assertThat("値行の列数", valueRow.size(), is(3));
        assertThat("COL1", valueRow.get(1), is("AAA"));
        assertThat("COL2(省略→補完)", valueRow.get(2), is(""));  // RS-06
    }

    // -------------------------------------------------------------------
    // record_type が null の場合
    // -------------------------------------------------------------------

    /**
     * Given: record_type が指定されていない
     * When:  addRecordRows を呼び出す
     * Then:  フィールド名行の先頭セルは ""
     */
    @Test
    public void addRecordRows_noRecordType_firstCellIsEmpty() {
        // Given
        Map<String, Object> record = buildRecord(null,
                Arrays.asList(buildField("COL", "X", "5")),
                Arrays.asList(Arrays.asList((Object) "val"))
        );

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        RecordRowBuilder.addRecordRows(record, true, out);

        // Then: フィールド名行先頭は ""
        assertThat(out.get(0).get(0), is(""));
    }

    // -------------------------------------------------------------------
    // QA-10: フィールド0件のエッジケース
    // -------------------------------------------------------------------

    /**
     * Given: fields が空リスト（フィールド0件）
     * When:  addRecordRows を呼び出す
     * Then:  フィールド名行（先頭セルのみ）・型行（先頭セルのみ）・長さ行（先頭セルのみ）が3行出力される
     */
    @Test
    public void addRecordRows_noFields_outputsHeaderRowsOnly() {
        // Given
        Map<String, Object> record = buildRecord("DATA",
                Collections.<Map<String, Object>>emptyList(),
                Collections.<List<Object>>emptyList()
        );

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        RecordRowBuilder.addRecordRows(record, true, out);

        // Then: フィールド名行（["DATA"]）・型行（[""]）・長さ行（[""]）の3行
        assertThat("行数（固定長・フィールド0件）", out.size(), is(3));
        assertThat("フィールド名行", out.get(0), is(Arrays.asList("DATA")));
        assertThat("型行", out.get(1), is(Arrays.asList("")));
        assertThat("長さ行", out.get(2), is(Arrays.asList("")));
    }

    // -----------------------------------------------------------------------
    // ヘルパー
    // -----------------------------------------------------------------------

    private Map<String, Object> buildRecord(String recordType,
            List<Map<String, Object>> fields,
            List<List<Object>> rows) {
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
