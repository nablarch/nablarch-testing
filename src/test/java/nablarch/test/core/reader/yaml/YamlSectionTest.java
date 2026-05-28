package nablarch.test.core.reader.yaml;

import nablarch.test.core.reader.DataType;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * {@link YamlSection} の静的ユーティリティメソッドに対する単体テスト。
 *
 * <p>
 * DB 不要 — @RunWith なし。純粋なロジック検証のみ。
 * </p>
 */
public class YamlSectionTest {

    // ========================================================================
    // castMap: 非 Map を渡した場合は空 Map が返ること
    // ========================================================================

    /**
     * [YamlSection] castMap: String を渡した場合は空 Map が返ること。
     *
     * <p>
     * Given: castMap に String "hello" を渡す（Map ではない）<br>
     * When:  castMap("hello") を呼ぶ<br>
     * Then:  空 Map が返ること
     * </p>
     */
    @Test
    public void testCastMap_nonMapReturnsEmptyMap() {
        // When
        java.util.Map<String, Object> result = YamlSection.castMap("hello");

        // Then
        assertTrue("非 Map を渡した場合は空 Map が返ること", result.isEmpty());
    }

    /**
     * [YamlSection] castMap: Integer を渡した場合は空 Map が返ること。
     *
     * <p>
     * Given: castMap に Integer 42 を渡す（Map ではない）<br>
     * When:  castMap(42) を呼ぶ<br>
     * Then:  空 Map が返ること
     * </p>
     */
    @Test
    public void testCastMap_integerReturnsEmptyMap() {
        // When
        java.util.Map<String, Object> result = YamlSection.castMap(42);

        // Then
        assertTrue("Integer を渡した場合は空 Map が返ること", result.isEmpty());
    }

    // ========================================================================
    // dataTypeToSectionKey: MESSAGE → "messages"
    // ========================================================================

    /**
     * [YamlSection] dataTypeToSectionKey: DataType.MESSAGE → "messages" が返ること。
     *
     * <p>
     * Given: DataType.MESSAGE<br>
     * When:  YamlSection.dataTypeToSectionKey(DataType.MESSAGE) を呼ぶ<br>
     * Then:  "messages" が返ること
     * </p>
     */
    @Test
    public void testDataTypeToSectionKey_messageMapsToMessages() {
        // When
        String key = YamlSection.dataTypeToSectionKey(DataType.MESSAGE);

        // Then
        assertThat("DataType.MESSAGE は 'messages' キーにマップされること", key, is("messages"));
    }

    // ========================================================================
    // toStr: 非 null 値の toString が返ること
    // ========================================================================

    /**
     * [YamlSection] toStr: 非 null 値を渡した場合はその toString が返ること。
     *
     * <p>
     * Given: 非 null の String "hello"<br>
     * When:  YamlSection.toStr("hello") を呼ぶ<br>
     * Then:  "hello" が返ること
     * </p>
     */
    @Test
    public void testToStr_nonNullReturnsToString() {
        // When
        String result = YamlSection.toStr("hello");

        // Then
        assertThat("非 null 値の toString が返ること", result, is("hello"));
    }

    /**
     * [YamlSection] toStr: Integer を渡した場合はその文字列表現が返ること。
     *
     * <p>
     * Given: Integer 42<br>
     * When:  YamlSection.toStr(42) を呼ぶ<br>
     * Then:  "42" が返ること
     * </p>
     */
    @Test
    public void testToStr_integerReturnsStringRepresentation() {
        // When
        String result = YamlSection.toStr(42);

        // Then
        assertThat("Integer の toStr は '42' を返すこと", result, is("42"));
    }

    /**
     * [YamlSection] toStr: null を渡した場合は null が返ること。
     *
     * <p>
     * Given: null<br>
     * When:  YamlSection.toStr(null) を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void testToStr_nullReturnsNull() {
        // When
        String result = YamlSection.toStr(null);

        // Then
        assertThat("null を渡した場合は null が返ること", result, nullValue());
    }

    // ========================================================================
    // interpret: interps が null/空のとき value がそのまま返ること
    // ========================================================================

    /**
     * [YamlSection] interpret: interpreters リストが null の場合は value がそのまま返ること。
     *
     * <p>
     * Given: value="test", interps=null<br>
     * When:  YamlSection.interpret("test", null) を呼ぶ<br>
     * Then:  "test" がそのまま返ること
     * </p>
     */
    @Test
    public void testInterpret_nullInterpretersReturnsValueAsIs() {
        // When
        String result = YamlSection.interpret("test", null);

        // Then
        assertThat("interpreters が null のとき value がそのまま返ること", result, is("test"));
    }

    /**
     * [YamlSection] interpret: interpreters リストが空の場合は value がそのまま返ること。
     *
     * <p>
     * Given: value="hello", interps=空リスト<br>
     * When:  YamlSection.interpret("hello", emptyList) を呼ぶ<br>
     * Then:  "hello" がそのまま返ること
     * </p>
     */
    @Test
    public void testInterpret_emptyInterpretersReturnsValueAsIs() {
        // Given
        List<nablarch.test.core.util.interpreter.TestDataInterpreter> emptyList = Collections.emptyList();

        // When
        String result = YamlSection.interpret("hello", emptyList);

        // Then
        assertThat("interpreters が空のとき value がそのまま返ること", result, is("hello"));
    }

    /**
     * [YamlSection] interpret: value が null の場合は null が返ること。
     *
     * <p>
     * Given: value=null, interps=何らかのリスト<br>
     * When:  YamlSection.interpret(null, emptyList) を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void testInterpret_nullValueReturnsNull() {
        // When
        String result = YamlSection.interpret(null, Collections.<nablarch.test.core.util.interpreter.TestDataInterpreter>emptyList());

        // Then
        assertThat("value が null のとき null が返ること", result, nullValue());
    }

    // ========================================================================
    // addBinaryFileInterpreter: interpreters が null の場合も BinaryFileInterpreter のみのリストが返ること
    // ========================================================================

    /**
     * [YamlSection] addBinaryFileInterpreter: interpreters が null の場合、
     * BinaryFileInterpreter のみを含む 1 件のリストが返ること。
     *
     * <p>
     * Given: interpreters=null<br>
     * When:  YamlSection.addBinaryFileInterpreter("somePath", null) を呼ぶ<br>
     * Then:  サイズ 1 のリストが返り、先頭要素が BinaryFileInterpreter であること
     * </p>
     */
    @Test
    public void testAddBinaryFileInterpreter_nullInterpretersReturnsSingletonList() {
        // When
        List<nablarch.test.core.util.interpreter.TestDataInterpreter> result =
                YamlSection.addBinaryFileInterpreter("src/test/java/", null);

        // Then
        assertThat("interpreters が null でも BinaryFileInterpreter を 1 件含むリストが返ること",
                result.size(), is(1));
        assertTrue("先頭要素が BinaryFileInterpreter であること",
                result.get(0) instanceof nablarch.test.core.util.interpreter.BinaryFileInterpreter);
    }
}
