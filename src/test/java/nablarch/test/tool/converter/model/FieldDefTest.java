package nablarch.test.tool.converter.model;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link FieldDef} のテスト。
 *
 * <p>中間モデルのフィールド定義が、形式の記法を加工せずそのまま保持することを検証する。</p>
 */
public class FieldDefTest {

    @Test
    public void 名称型長さを記法のまま保持する() {
        // Given: 日本語型名称（設計書 §5）と固定長
        // When
        FieldDef sut = new FieldDef("emp_name", "半角英字", "10");

        // Then: 大文字化・型変換なくそのまま
        assertThat(sut.getName(), is("emp_name"));
        assertThat(sut.getType(), is("半角英字"));
        assertThat(sut.getLength(), is("10"));
    }

    @Test
    public void 長さ省略のハイフンをリテラルとして保持する() {
        // Given: 自動拡張指示 "-"
        // When
        FieldDef sut = new FieldDef("body", "全角", "-");

        // Then: 数値化せず "-" のまま
        assertThat(sut.getLength(), is("-"));
    }

    @Test
    public void 大文字混在の名称を大文字化せず保持する() {
        // Given/When: 本体器は大文字化しうるが中間モデルは記法のまま
        FieldDef sut = new FieldDef("EmpName", "半角英字", "10");

        // Then
        assertThat(sut.getName(), is("EmpName"));
    }

    @Test
    public void 型と長さの省略をnullで保持する() {
        // Given: 可変長 FW_HEADER 等、型・長さなし
        // When
        FieldDef sut = new FieldDef("dataKbn", null, null);

        // Then
        assertThat(sut.getName(), is("dataKbn"));
        assertThat(sut.getType(), is(nullValue()));
        assertThat(sut.getLength(), is(nullValue()));
    }
}
