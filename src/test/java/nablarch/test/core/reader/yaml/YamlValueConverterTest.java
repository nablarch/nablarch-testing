package nablarch.test.core.reader.yaml;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * {@link YamlValueConverter} のテスト。
 * RS-03〜RS-06 の変換ルールを検証する。
 * 仕様ID参照: {@code docs/ntf-impl-spec-list.md}
 */
public class YamlValueConverterTest {

    // -------------------------------------------------------------------
    // toCell: RS-03 YAML null → 文字列 "null"
    // -------------------------------------------------------------------

    /**
     * Given: YAML ネイティブ null 値、isMissing=false
     * When:  toCell を呼び出す
     * Then:  文字列 "null" を返す（RS-03）
     */
    @Test
    public void toCell_nativeNull_returnsStringNull() {
        assertThat(YamlValueConverter.toCell(null, false), is("null"));  // RS-03
    }

    // -------------------------------------------------------------------
    // toCell: RS-04 boolean → "true"/"false"
    // -------------------------------------------------------------------

    /**
     * Given: YAML ネイティブ boolean true、isMissing=false
     * When:  toCell を呼び出す
     * Then:  文字列 "true" を返す（RS-04）
     */
    @Test
    public void toCell_booleanTrue_returnsStringTrue() {
        assertThat(YamlValueConverter.toCell(Boolean.TRUE, false), is("true"));  // RS-04
    }

    /**
     * Given: YAML ネイティブ boolean false、isMissing=false
     * When:  toCell を呼び出す
     * Then:  文字列 "false" を返す（RS-04）
     */
    @Test
    public void toCell_booleanFalse_returnsStringFalse() {
        assertThat(YamlValueConverter.toCell(Boolean.FALSE, false), is("false"));  // RS-04
    }

    // -------------------------------------------------------------------
    // toCell: RS-05 integer/float → 数字文字列
    // -------------------------------------------------------------------

    /**
     * Given: YAML ネイティブ整数 42、isMissing=false
     * When:  toCell を呼び出す
     * Then:  文字列 "42" を返す（RS-05）
     */
    @Test
    public void toCell_integer_returnsNumberString() {
        assertThat(YamlValueConverter.toCell(42, false), is("42"));  // RS-05
    }

    /**
     * Given: YAML ネイティブ float 3.14、isMissing=false
     * When:  toCell を呼び出す
     * Then:  文字列 "3.14" を返す（RS-05）
     */
    @Test
    public void toCell_float_returnsNumberString() {
        assertThat(YamlValueConverter.toCell(3.14, false), is("3.14"));  // RS-05
    }

    /**
     * Given: YAML 科学表記 1.0E10（SnakeYAML が Double として渡す）、isMissing=false
     * When:  toCell を呼び出す
     * Then:  文字列 "1.0E10" を返す（RS-05 境界値）
     */
    @Test
    public void toCell_scientificNotationFloat_returnsString() {
        assertThat(YamlValueConverter.toCell(1.0E10, false), is("1.0E10"));  // RS-05 境界値
    }

    /**
     * Given: 通常の文字列 "hello"、isMissing=false
     * When:  toCell を呼び出す
     * Then:  そのまま "hello" を返す
     */
    @Test
    public void toCell_string_returnsSameString() {
        assertThat(YamlValueConverter.toCell("hello", false), is("hello"));
    }

    // -------------------------------------------------------------------
    // toCell: RS-06 isMissing=true → ""
    // -------------------------------------------------------------------

    /**
     * Given: isMissing=true（キーが省略されている）
     * When:  toCell を呼び出す（value が何であっても）
     * Then:  空文字 "" を返す（RS-06）
     */
    @Test
    public void toCell_isMissing_returnsEmpty() {
        assertThat(YamlValueConverter.toCell(null, true),      is(""));  // RS-06
        assertThat(YamlValueConverter.toCell("something", true), is(""));  // RS-06
    }

    // -------------------------------------------------------------------
    // asMap
    // -------------------------------------------------------------------

    /**
     * Given: Map オブジェクト
     * When:  asMap を呼び出す
     * Then:  そのまま Map として返す
     */
    @Test
    public void asMap_mapObject_returnsSameMap() {
        // Given
        Map<String, Object> input = new java.util.LinkedHashMap<String, Object>();
        input.put("key", "value");
        // When / Then
        assertThat(YamlValueConverter.asMap(input), is(sameInstance(input)));
    }

    /**
     * Given: null オブジェクト
     * When:  asMap を呼び出す
     * Then:  空の LinkedHashMap を返す
     */
    @Test
    public void asMap_nullObject_returnsEmptyMap() {
        Map<String, Object> result = YamlValueConverter.asMap(null);
        assertThat(result, is(notNullValue()));
        assertThat(result.isEmpty(), is(true));
    }

    /**
     * Given: Map でないオブジェクト（String）
     * When:  asMap を呼び出す
     * Then:  空の LinkedHashMap を返す
     */
    @Test
    public void asMap_nonMapObject_returnsEmptyMap() {
        assertThat(YamlValueConverter.asMap("not a map").isEmpty(), is(true));
    }

    // -------------------------------------------------------------------
    // asList
    // -------------------------------------------------------------------

    /**
     * Given: List オブジェクト
     * When:  asList を呼び出す
     * Then:  そのまま List として返す
     */
    @Test
    public void asList_listObject_returnsSameList() {
        List<Object> input = Arrays.asList((Object) "a", "b");
        assertThat(YamlValueConverter.asList(input), is(sameInstance(input)));
    }

    /**
     * Given: null オブジェクト
     * When:  asList を呼び出す
     * Then:  空リストを返す
     */
    @Test
    public void asList_null_returnsEmptyList() {
        assertThat(YamlValueConverter.asList(null).isEmpty(), is(true));
    }

    // -------------------------------------------------------------------
    // asString
    // -------------------------------------------------------------------

    /**
     * Given: null
     * When:  asString を呼び出す
     * Then:  null を返す
     */
    @Test
    public void asString_null_returnsNull() {
        assertThat(YamlValueConverter.asString(null), is(nullValue()));
    }

    /**
     * Given: 整数 42
     * When:  asString を呼び出す
     * Then:  文字列 "42" を返す
     */
    @Test
    public void asString_integer_returnsString() {
        assertThat(YamlValueConverter.asString(42), is("42"));
    }
}
