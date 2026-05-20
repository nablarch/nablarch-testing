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
 * {@link TableSectionConverter} のテスト。
 * SS-01〜SS-03 のセクションヘッダ生成・カラム補完を検証する。
 * 仕様ID参照: {@code docs/ntf-impl-spec-list.md}
 */
public class TableSectionConverterTest {

    private final TableSectionConverter sut = new TableSectionConverter("SETUP_TABLE");

    // -------------------------------------------------------------------
    // group_id なし: ヘッダが "SETUP_TABLE=TABLE_NAME" 形式
    // -------------------------------------------------------------------

    /**
     * Given: group_id なし、table="USER"、1行
     * When:  convert を呼び出す
     * Then:  セクションヘッダ "SETUP_TABLE=USER"・カラムヘッダ行・データ行が出力される
     */
    @Test
    public void convert_noGroupId_headerFormat() {
        // Given
        Map<String, Object> entry = buildEntry(null, "USER",
                Collections.singletonList(buildRow("USER_ID", "001")));

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        assertThat("セクションヘッダ", out.get(0).get(0), is("SETUP_TABLE=USER"));  // SS-01
        assertThat("カラムヘッダ先頭セル", out.get(1).get(0), is(""));
        assertThat("データ行先頭セル", out.get(2).get(0), is(""));
    }

    // -------------------------------------------------------------------
    // group_id あり: ヘッダが "SETUP_TABLE[groupId]=TABLE_NAME" 形式
    // -------------------------------------------------------------------

    /**
     * Given: group_id="case1"、table="ORDER"
     * When:  convert を呼び出す
     * Then:  セクションヘッダ "SETUP_TABLE[case1]=ORDER"（SS-02 グループID）
     */
    @Test
    public void convert_withGroupId_headerFormat() {
        // Given
        Map<String, Object> entry = buildEntry("case1", "ORDER",
                Collections.singletonList(buildRow("ORDER_ID", "1001")));

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        assertThat("セクションヘッダ", out.get(0).get(0), is("SETUP_TABLE[case1]=ORDER"));  // SS-02
    }

    // -------------------------------------------------------------------
    // rows 空: ヘッダのみ出力
    // -------------------------------------------------------------------

    /**
     * Given: rows が空
     * When:  convert を呼び出す
     * Then:  セクションヘッダ行1行のみ出力される
     */
    @Test
    public void convert_emptyRows_headerOnly() {
        // Given
        Map<String, Object> entry = buildEntry(null, "EMPTY_TABLE",
                Collections.<Map<String, Object>>emptyList());

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        assertThat("行数", out.size(), is(1));
        assertThat("セクションヘッダのみ", out.get(0).get(0), is("SETUP_TABLE=EMPTY_TABLE"));
    }

    // -------------------------------------------------------------------
    // RS-06: 行ごとに列数が異なる場合に全列の union で補完
    // -------------------------------------------------------------------

    /**
     * Given: 行1は COL_A/COL_B/COL_C、行2は COL_A/COL_B（COL_C 省略）
     * When:  convert を呼び出す
     * Then:  行2の COL_C 位置は "" で補完される（RS-06）
     */
    @Test
    public void convert_missingColumnPaddedWithEmpty() {
        // Given
        Map<String, Object> row1 = new LinkedHashMap<String, Object>();
        row1.put("COL_A", "a1");
        row1.put("COL_B", "b1");
        row1.put("COL_C", "c1");

        Map<String, Object> row2 = new LinkedHashMap<String, Object>();
        row2.put("COL_A", "a2");
        row2.put("COL_B", "b2");
        // COL_C 省略

        Map<String, Object> entry = buildEntry(null, "T", Arrays.asList(row1, row2));

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        sut.convert(entry, out);

        // Then
        List<String> colHeader = out.get(1);
        List<String> dataRow2  = out.get(3);  // ヘッダ + 行1 + 行2
        int colCIdx = colHeader.indexOf("COL_C");
        assertThat("行2 COL_C は補完 ''", dataRow2.get(colCIdx), is(""));  // RS-06
    }

    // -------------------------------------------------------------------
    // EXPECTED_COMPLETE_TABLE ヘッダ
    // -------------------------------------------------------------------

    /**
     * Given: dataTypeName="EXPECTED_COMPLETE_TABLE"
     * When:  convert を呼び出す
     * Then:  セクションヘッダが "EXPECTED_COMPLETE_TABLE=TABLE" 形式
     */
    @Test
    public void convert_expectedCompleteTable_headerFormat() {
        // Given
        TableSectionConverter convEct = new TableSectionConverter("EXPECTED_COMPLETE_TABLE");
        Map<String, Object> entry = buildEntry(null, "TABLE",
                Collections.singletonList(buildRow("ID", "1")));

        // When
        List<List<String>> out = new ArrayList<List<String>>();
        convEct.convert(entry, out);

        // Then
        assertThat(out.get(0).get(0), is("EXPECTED_COMPLETE_TABLE=TABLE"));
    }

    // -----------------------------------------------------------------------
    // ヘルパー
    // -----------------------------------------------------------------------

    private Map<String, Object> buildEntry(String groupId, String table,
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
