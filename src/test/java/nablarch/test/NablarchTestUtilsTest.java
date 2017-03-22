package nablarch.test;


import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * {@link NablarchTestUtils}のテスト
 *
 * @author T.Kawasaki
 */
public class NablarchTestUtilsTest {

    /** {@link NablarchTestUtils#makeArray(String)}のテスト */
    @Test
    public void testMakeArray() {
        // null -> 空の配列
        assertArrayEquals(new String[0], NablarchTestUtils.makeArray(null));
        // 空文字 -> からの配列
        assertArrayEquals(new String[0], NablarchTestUtils.makeArray(""));
        // カンマ区切り ->　複数の配列の要素に分割
        assertArrayEquals(new String[]{"foo", "bar"}, NablarchTestUtils.makeArray("foo,bar"));
        // カンマ区切りでない文字列 -> 変更なし
        assertArrayEquals(new String[]{"foobar"}, NablarchTestUtils.makeArray("foobar"));
    }

    @Test
    public void testCreateLRUMap() {
        Map<Integer, String> lru = NablarchTestUtils.createLRUMap(1);
        lru.put(1, "one");
        assertEquals("one", lru.get(1));
        lru.put(2, "two");
        assertEquals(1, lru.size());
        assertEquals("two", lru.get(2));
        assertNull(lru.get(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateLRUMapFail() {
        NablarchTestUtils.createLRUMap(0);
    }

    @Test
    public void testInvokePrivateConstructor() {
        NablarchTestUtils.invokePrivateDefaultConstructor(HavingDefaultConstructor.class);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testInvokePrivateConstructorFail() {
        NablarchTestUtils.invokePrivateDefaultConstructor(WithoutDefaultConstructor.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testInvokePrivateConstructorFail2() {
        NablarchTestUtils.invokePrivateDefaultConstructor(InvalidConstructor.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvokePrivateConstructorFail3() {
        NablarchTestUtils.invokePrivateDefaultConstructor(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvokePrivateConstructorFail4() {
        NablarchTestUtils.invokePrivateDefaultConstructor(HavingPublicConstructor.class);
    }

    @Test
    public void testConstructor() {
        NablarchTestUtils.invokePrivateDefaultConstructor(NablarchTestUtils.class);
    }

    @Test
    public void testToUpperCase() {
        assertNull(NablarchTestUtils.toUpperCase(null));
        assertTrue(NablarchTestUtils.toUpperCase(new ArrayList<String>(0)).isEmpty());
        assertEquals(
                Arrays.asList("FOO", "BAR", "BUZ"),
                NablarchTestUtils.toUpperCase(Arrays.asList("foo", "bAr", "BUZ")));

    }

    @Test
    public void testGetFileNameWithoutSuffix() {
        assertEquals("foo", NablarchTestUtils.getFileNameWithoutSuffix("foo.java"));
        assertEquals("bar", NablarchTestUtils.getFileNameWithoutSuffix("bar"));
        assertEquals(null, NablarchTestUtils.getFileNameWithoutSuffix(null));
    }


    @Test
    public void testAsSet() {
        Set<String> actual = NablarchTestUtils.asSet("hoge", "fuga");
        assertEquals(2, actual.size());
        assertTrue(actual.contains("hoge"));
        assertTrue(actual.contains("fuga"));
        String[] array = null;
        assertEquals(null, NablarchTestUtils.asSet(array));
    }

    private static class HavingDefaultConstructor {
        private HavingDefaultConstructor() {
        }
    }

    private static class WithoutDefaultConstructor {
        private WithoutDefaultConstructor(String str) {
        }
    }

    private static class InvalidConstructor {
        private InvalidConstructor() {
            throw new RuntimeException("invalid.");
        }
    }
    public static class HavingPublicConstructor {
        public HavingPublicConstructor() {
        }
    }


    /**
     * {@link NablarchTestUtils.ReflectionOperation}のテスト。<br/>
     * テンプレート内で例外が発生しない場合は、例外がスローされず処理が終了すること。
     */
    @Test
    public void testTemplate() {
        new NablarchTestUtils.ReflectionOperation() {
            @Override
            protected void operate() throws Exception {
            }
        }.execute();
    }

    /**
     * {@link NablarchTestUtils.ReflectionOperation}のテスト。<br/>
     * テンプレート内で{@link InvocationTargetException}が発生した場合、その原因となった例外が
     * 非チェック例外であれば、その原因の例外がスローされることを確認する。
     */
    @Test
    public void testTemplateInvocationTargetExceptionWithUnchecked() {
        try {
            new NablarchTestUtils.ReflectionOperation() {
                @Override
                protected void operate() throws Exception {
                    throw new InvocationTargetException(new IllegalArgumentException("for test"));
                }
            }.execute();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("for test", e.getMessage());
        }
    }

    /**
     * {@link NablarchTestUtils.ReflectionOperation}のテスト。<br/>
     * テンプレート内で{@link InvocationTargetException}が発生した場合、
     * その原因となった例外がチェック例外であれば、その原因の例外が
     * RuntimeExceptionにラップされてスローされることを確認する。
     */
    @Test
    public void testTemplateInvocationTargetExceptionWithChecked() {
        try {
            new NablarchTestUtils.ReflectionOperation() {
                @Override
                protected void operate() throws Exception {
                    throw new InvocationTargetException(new Exception("for test"));
                }
            }.execute();
            fail();
        } catch (RuntimeException e) {
            assertEquals("for test", e.getCause().getMessage());
        }
    }

    /**
     * {@link NablarchTestUtils.ReflectionOperation}のテスト。<br/>
     * テンプレート内で非チェック例外が発生した場合、
     * その例外がスローされることを確認する。
     */
    @Test
    public void testTemplateRuntimeException() {
        try {
            new NablarchTestUtils.ReflectionOperation() {
                @Override
                protected void operate() throws Exception {
                    throw new RuntimeException("for test");
                }
            }.execute();
            fail();
        } catch (RuntimeException e) {
            assertEquals("for test", e.getMessage());
        }
    }

    /**
     * {@link NablarchTestUtils.ReflectionOperation}のテスト。<br/>
     * テンプレート内で{@link InvocationTargetException}以外のチェック例外が発生した場合、
     * その例外がRuntimeExceptionにラップされてスローされることを確認する。
     */
    @Test
    public void testTemplateException() {
        try {
            new NablarchTestUtils.ReflectionOperation() {
                @Override
                protected void operate() throws Exception {
                    throw new Exception("for test");
                }
            }.execute();
            fail();
        } catch (RuntimeException e) {
            assertEquals("for test", e.getCause().getMessage());
        }
    }

    @Test
    public void testPrivateConstructor() {
        NablarchTestUtils.invokePrivateDefaultConstructor(NablarchTestUtils.class);
    }

    /**
     * 正規化されたパスが取得されること。
     * @throws IOException 予期しない例外
     */
    @Test
    public void testToCanonicalPath() throws IOException {
        String result = NablarchTestUtils.toCanonicalPath("hoge.txt");
        assertThat(result, is(not("hoge.txt")));
        assertThat(result, is(new File("hoge.txt").getCanonicalPath()));
    }

    /**
     * 正規化されたパスが取得されること。
     */
    @Test
    public void testToCanonical() {
        File orig = new File("hoge.txt");
        File result = NablarchTestUtils.toCanonical(orig);
        assertThat(result.getAbsolutePath(), is(orig.getAbsolutePath()));
        assertThat(result.getPath(), is(not(orig.getPath())));
    }

    /**
     * 引数がnullのとき例外が発生すること
     */
    @Test(expected = IllegalArgumentException.class)
    public void testTOCanonicalWithNull() {
        NablarchTestUtils.toCanonical(null);
    }

    /**
     * パス正規化の際にIOExceptionが発生した場合、RuntimeExceptionにwrapされること
     */
    @Test
    public void testToCanonicalFail() {
        try {
            NablarchTestUtils.toCanonical(new File("error") {
                @Override
                public File getCanonicalFile() throws IOException {
                    throw new IOException("for test.");
                }
            });
            fail();
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            assertNotNull(cause);
            assertEquals(IOException.class, cause.getClass());
        }
    }

    /**
     * 存在しないファイルを削除指定した場合、何も起こらないこと
     */
    @Test
    public void testDeleteFileNotExists() {
        NablarchTestUtils.deleteFile(null);
        NablarchTestUtils.deleteFile(new File("not exists"));
    }

    /**
     * 指定したファイルが削除されること
     * @throws IOException 予期しない例外
     */
    @Test
    public void testDeleteFileSingle() throws IOException {
        File f = File.createTempFile(getClass().getName(), "tmp");
        f.deleteOnExit();
        assertTrue(f.exists());
        NablarchTestUtils.deleteFile(f);
        assertFalse(f.exists());
    }

    /**
     * 指定したディレクトリと配下のファイルが削除されること
     * @throws IOException 予期しない例外
     */
    @Test
    public void testDeleteFileDir() throws IOException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), "tmpdir");
        tmpDir.mkdir();
        File tmpFile = new File(tmpDir, "tmp.txt");
        tmpFile.createNewFile();
        assertTrue(tmpDir.exists());
        assertTrue(tmpFile.exists());
        NablarchTestUtils.deleteFile(tmpDir);
        assertFalse(tmpFile.exists());
        assertFalse(tmpDir.exists());
    }

    @Test
    public void testDeleteFileNull() {
        NablarchTestUtils.deleteFile(null);
    }

    @Test
    public void testTrimTail() {
        assertThat(NablarchTestUtils.trimTail(list("foo", "bar")), is(list("foo", "bar")));
        assertThat(NablarchTestUtils.trimTail(list("foo", "", "bar", "", "")) , is(list("foo", "", "bar")));
        assertThat(NablarchTestUtils.trimTail(list("", "")).size() , is(0));
        assertThat(NablarchTestUtils.trimTail(Collections.<String>emptyList()).size() , is(0));
        assertThat(NablarchTestUtils.trimTail(null), nullValue());

    }

    @Test
    public void testTrimTailCopy() {
        List<String> orig = list("foo", "bar", "", "");
        assertThat("末尾の空要素が取り除かれること。",
                   NablarchTestUtils.trimTailCopy(orig), is(list("foo", "bar")));
        assertThat("元のリストは変更されていないこと",
                   orig, is(list("foo", "bar", "", "")));
    }

    @Test
    public void testTrimTailCopyNull() {
        assertThat("引数がnullのとき、戻り値もnullであること。",
                   NablarchTestUtils.trimTailCopy(null) , is(nullValue()));
    }

    /**
     * {@link NablarchTestUtils#limit(String, int)} のテスト。<br/>
     * 正常系
     */
    @Test
    public void testLimit() {
        assertThat(NablarchTestUtils.limit("hoge", 0), is(""));
        assertThat(NablarchTestUtils.limit("hoge", 3), is("hog"));
        assertThat(NablarchTestUtils.limit("hoge", 4), is("hoge"));
        assertThat(NablarchTestUtils.limit("hoge", 5), is("hoge"));
    }

    /**
     * {@link NablarchTestUtils#limit(String, int)} のテスト。<br/>
     * 閾値に負数が指定された場合、例外が発生すること。
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLimitNegative() {
        NablarchTestUtils.limit("hoge", -1);
    }

    /**
     * {@link NablarchTestUtils#limit(String, int)} のテスト。<br/>
     * 対象文字列にnullが指定された場合、例外が発生すること。
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLimitNull() {
        NablarchTestUtils.limit(null, 1);
    }

    /** エスケープのテスト */
    @Test
    public void testEscape() {
        assertThat(NablarchTestUtils.escape("\\"), is("\\\\"));
        assertThat(NablarchTestUtils.escape("\\\\"), is("\\\\\\\\"));
        assertThat(NablarchTestUtils.escape("\\ho\\ge\\"), is("\\\\ho\\\\ge\\\\"));
        assertThat(NablarchTestUtils.escape("\thoge\n"), is("\thoge\n"));
        assertThat(NablarchTestUtils.escape(""), is(""));
    }

    /** エスケープ解除のテスト */
    @Test
    public void testUnescape() {
        assertThat(NablarchTestUtils.unescapeYen("\\\\"), is("\\"));
        assertThat(NablarchTestUtils.unescapeYen("\\\\\\\\"), is("\\\\"));
        assertThat(NablarchTestUtils.unescapeYen("\\\\ho\\\\ge\\\\"), is("\\ho\\ge\\"));
        assertThat(NablarchTestUtils.unescapeYen("\thoge\n"), is("\thoge\n"));
        assertThat(NablarchTestUtils.unescapeYen(""), is(""));
    }

    @Test
    public void testAssertContainsRequiredKeys() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        NablarchTestUtils.assertContainsRequiredKeys("", map, Collections.<String>emptySet());
        NablarchTestUtils.assertContainsRequiredKeys("", map, NablarchTestUtils.asSet("one", "two"));
        NablarchTestUtils.assertContainsRequiredKeys("", map, NablarchTestUtils.asSet("one", "two", "three"));

    }

    @Test
    public void testAssertContainsRequiredKeysFail() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("one", 1);
        map.put("three", 3);
        try {
            NablarchTestUtils.assertContainsRequiredKeys("hoge", map, NablarchTestUtils.asSet("one", "two", "four"));
            fail();
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            assertThat(msg, containsString("hoge"));
            assertThat(msg, containsString("two"));
            assertThat(msg, containsString("four"));
        }

    }

    @Test
    public void testGetMessages() {
        // 単一の例外
        Exception single = new Exception("single.");
        assertThat(NablarchTestUtils.getMessages(single), is("single."));

        // ネストした例外
        Exception nested = new Exception("third.", new Throwable("second.", new Error("first.")));
        assertThat(NablarchTestUtils.getMessages(nested), is("third. ; second. ; first."));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMessagesNull() {
        // null
        NablarchTestUtils.getMessages(null);
    }

    /** {@link NablarchTestUtils#isNullOrEmpty(Collection)} のテスト。*/
    @Test
    public void testIsNullOrEmpty() {
        assertThat(NablarchTestUtils.isNullOrEmpty(null), is(true));
        assertThat(NablarchTestUtils.isNullOrEmpty(Collections.<Object>emptyList()), is(true));
        assertThat(NablarchTestUtils.isNullOrEmpty(Arrays.asList("hoge")), is(false));
        List<?> list = new ArrayList();
        list.add(null);
        assertThat(NablarchTestUtils.isNullOrEmpty(list), is(false)); // 1要素あるので空ではない
    }

    /** Listインスタンスを作成する。 **/
    private List<String> list(String... strings) {
        return new ArrayList<String>(Arrays.asList(strings));
    }
}
