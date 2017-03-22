package nablarch.fw.web;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * {@link MockHttpCookie}のテストクラス。
 */
public class MockHttpCookieTest {

    /**
     * 単一のCookieの場合の{@link MockHttpCookie#toString()}のテスト。
     * <p/>
     * headerに埋め込む形式のCookieオブジェクトが生成できること
     */
    @Test
    public void testToString_SingleCookie() throws Exception {
        final MockHttpCookie sut = new MockHttpCookie();
        sut.put("key", "value");

        assertThat(sut.toString(), is("key=value"));
    }

    /**
     * 複数のCookieの場合の{@link MockHttpCookie#toString()}のテスト。
     * <p/>
     * headerに埋め込む形式のCookieオブジェクトが生成できること
     */
    @Test
    public void testToString_MultiCookie() throws Exception {
        final MockHttpCookie sut = new MockHttpCookie();
        sut.put("key-1", "value-1");
        sut.put("key-2", "value-2");

        assertThat(sut.toString(), containsString("key-1=value-1"));
        assertThat(sut.toString(), containsString("; "));
        assertThat(sut.toString(), containsString("key-2=value-2"));
    }

    /**
     * Cookieが空の場合の{@link MockHttpCookie#toString()}のテスト。
     * <p/>
     * 空文字列が返却されること。
     */
    @Test
    public void testToString_emptyCookie() throws Exception {
        final MockHttpCookie sut = new MockHttpCookie();
        assertThat(sut.toString(), is(""));
    }

    /**
     * 単一のCookieの場合の{@link MockHttpCookie#valueOf(String)}のテスト。
     * <p/>
     * Cookieのキーと値が正しく取り出せること
     */
    @Test
    public void testValueOf_SingleCookie() throws Exception {
        final HttpCookie sut = MockHttpCookie.valueOf("key=value");

        assertThat(sut.size(), is(1));
        assertThat(sut.get("key"), is("value"));
    }

    /**
     * 複数のCookieの場合の{@link MockHttpCookie#valueOf(String)}のテスト。
     * <p/>
     * Cookieのキーと値が正しく取り出せること
     */
    @Test
    public void testValueOf_MultiCookie() throws Exception {
        final HttpCookie sut = MockHttpCookie.valueOf("key-1=value-1; key2=!#$%&'()*+-./0123456789:<=");
        assertThat(sut.size(), is(2));
        assertThat(sut.get("key-1"), is("value-1"));
        assertThat(sut.get("key2"), is("!#$%&'()*+-./0123456789:<="));
    }

    /**
     * Cookieが空の場合の{@link MockHttpCookie#valueOf(String)}のテスト。
     * <p/>
     * 空のHttpCookieが返却されること
     */
    @Test
    public void testValueOf_EmptyCookie() throws Exception {
        final HttpCookie sut = MockHttpCookie.valueOf("");
        assertThat(sut.isEmpty(), is(true));
    }

    /**
     * nullを指定した場合の{@link MockHttpCookie#valueOf(String)}のテスト。
     *
     * 空のHttpCookieが返却されること
     */
    @Test
    public void testValueOf_Null() throws Exception {
        final HttpCookie sut = MockHttpCookie.valueOf(null);
        assertThat(sut.isEmpty(), is(true));
    }

    /**
     * 不正なフォーマットを指定した場合の{@link MockHttpCookie#valueOf(String)}のテスト。
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValueOf_invalidFormat() throws Exception {
        MockHttpCookie.valueOf("hoge");
    }
}
