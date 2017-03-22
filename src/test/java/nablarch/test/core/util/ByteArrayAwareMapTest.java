package nablarch.test.core.util;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * {@link ByteArrayAwareMap}のテストクラス。
 *
 * @author T.Kawasaki
 */
public class ByteArrayAwareMapTest {

    /** テスト対象 */
    private ByteArrayAwareMap<String, Object> target;

    /** 前準備 */
    @Before
    public void setUp() {
        Map<String, Object> map = new HashMap<String, Object>();
        byte[] bytes = new byte[] {0x30, 0x31};
        map.put("hoge", bytes);
        target = new ByteArrayAwareMap<String, Object>(map);
    }

    /**
     * 各要素が適切に文字列に変換されること。
     */
    @Test
    public void testToString() {
        target.put("self", target);
        target.put("decimal", new BigDecimal("0.0000000001"));
        target.put("nullValue", null);

        String str = target.toString();
        assertThat("16進数文字列に変換されること", str, containsString("hoge=0x3031"));
        assertThat("自身の参照を表す文字列に変換されること", str, containsString("self=(this Map)"));
        assertThat("指数表記されずに文字列に変換されること", str, containsString("decimal=0.0000000001"));
        assertThat("null文字列に変換されること", str, containsString("nullValue=null"));
    }

    /** バイト配列の内容が同じである場合、異なるインスタンスでも等価とみなされること */
    @Test
    public void testEquals() {
        byte[] bytes = {0x30, 0x31};

        Map<String, Object> one = new HashMap<String, Object>();
        Map<String, Object> another = new HashMap<String, Object>();
        one.put("hoge", bytes); // 別のバイト配列インスタンス
        another.put("hoge", bytes); // oneと同一のバイト配列インスタンス
        // 等価なMapから生成されたByteArrayAwareMapは等価とみなされること。（カバレッジ対応。）
        assertTrue(new ByteArrayAwareMap<String, Object>(one).equals(new ByteArrayAwareMap<String, Object>(another)));

        assertTrue(target.equals(new ByteArrayAwareMap<String, Object>(another)));
    }

    @Test
    public void testEqualsFail() {
        assertFalse(target.equals(null));        // null
        assertFalse(target.equals(new Object()));       // 型が違う
        assertFalse(target.equals(Collections.<String, Object>emptyMap()));  // サイズが違う

        Map<String, Object> one = new ByteArrayAwareMap<String, Object>(new LinkedHashMap<String, Object>());
        Map<String, Object> another = new ByteArrayAwareMap<String, Object>(new LinkedHashMap<String, Object>());
        byte[] bytes = {0x30, 0x31};
        one.put("hoge", bytes); // 別のバイト配列インスタンス
        another.put("hoge", bytes); // oneと同一のバイト配列インスタンス
        one.put("notEqualArray", new byte[] {0x30, 0x31});
        another.put("notEqualArray", new byte[] {0x30, 0x32});
        assertFalse(one.equals(another));

        one = new ByteArrayAwareMap<String, Object>(new LinkedHashMap<String, Object>());
        another = new ByteArrayAwareMap<String, Object>(new LinkedHashMap<String, Object>());
        one.put("notArray", "Not An Array Object");
        another.put("notArray", bytes);
        assertFalse(one.equals(another));

        one = new ByteArrayAwareMap<String, Object>(new LinkedHashMap<String, Object>());
        another = new ByteArrayAwareMap<String, Object>(new LinkedHashMap<String, Object>());
        one.put("aKey", new byte[] {0x30, 0x31});
        another.put("notSameKey", new byte[] {0x30, 0x31});
        assertFalse(one.equals(another));
    }

    /** バイト配列の内容が異なる場合、等価とみなされないこと */
    @Test
    public void testEqualsNot() {
        Map<String, Object> another = new HashMap<String, Object>();
        another.put("hoge", new byte[]{0x30, 0x32}); // 値が違う
        assertFalse(target.equals(new ByteArrayAwareMap<String, Object>(another)));
    }

    /** "hashCodeは、元MapのhashCodeを利用していること。" */
    @Test
    public void testHashCode() {
        Map<String, Object> orig = new HashMap<String, Object>();
        orig.put("hoge", new byte[] {0x30, 0x31});
        ByteArrayAwareMap<String, Object> byteArrayAwareMap = new ByteArrayAwareMap<String, Object>(orig);

        assertTrue(byteArrayAwareMap.hashCode() == orig.hashCode());
    }

}
