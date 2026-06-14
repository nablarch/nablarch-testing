package nablarch.test.core.file;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import nablarch.test.core.file.TestCoreFileAdapter.FileView;
import nablarch.test.core.file.TestCoreFileAdapter.FragmentView;

import org.junit.Test;

/**
 * {@link TestCoreFileAdapter}のテストクラス。
 * <p>
 * 本体ファイル器（{@link DataFile}／{@link DataFileFragment}）の protected な内部を
 * {@link FileView}／{@link FragmentView}へ無損失に写すこと、および「器が正規化した値」
 * （型記法のフレームワーク表記化・省略長の実バイト長上書き）がそのまま現れること
 * （＝原文復元は本クラスの責務でないこと）を検証する。
 * </p>
 *
 * @author kiyobot
 */
public class TestCoreFileAdapterTest {

    /**
     * Given: 名前・型・長さ・データ行を持つ固定長ファイル器（2 断片）。
     * When : {@code read} する。
     * Then : パス・断片数・各断片の名前／値が無損失に写される。
     */
    @Test
    public void readsFixedLengthFileFragments() {
        FixedLengthFile file = new FixedLengthFile("test.dat");
        DataFileFragment frag1 = file.getNewFragment();
        frag1.setNames(Arrays.asList("USER_NAME", "AGE"));
        frag1.setTypes(Arrays.asList("半角英字", "半角数字"));
        frag1.setLengths(Arrays.asList("10", "3"));
        frag1.addValue(Arrays.asList("alice", "20"));
        DataFileFragment frag2 = file.getNewFragment();
        frag2.setNames(Arrays.asList("ROLE"));
        frag2.setTypes(Arrays.asList("半角英字"));
        frag2.setLengths(Arrays.asList("5"));
        frag2.addValue(Arrays.asList("admin"));

        FileView view = TestCoreFileAdapter.read(file);

        assertThat(view.getPath(), is("test.dat"));
        assertThat(view.getFragments().size(), is(2));
        FragmentView v1 = view.getFragments().get(0);
        assertThat(v1.getNames(), is(Arrays.asList("USER_NAME", "AGE")));
        assertThat(v1.getValues().size(), is(1));
        assertThat(v1.getValues().get(0).get("USER_NAME"), is("alice"));
        assertThat(v1.getValues().get(0).get("AGE"), is("20"));
        FragmentView v2 = view.getFragments().get(1);
        assertThat(v2.getNames(), is(Arrays.asList("ROLE")));
        assertThat(v2.getValues().get(0).get("ROLE"), is("admin"));
    }

    /**
     * Given: 設計記法（{@code 半角英字}）の型を持つ固定長ファイル器。
     * When : {@code read} する。
     * Then : 型は器が正規化したフレームワーク表記（{@code X}）で現れる
     *        （＝原文復元は本クラスの責務でない）。
     */
    @Test
    public void exposesNormalizedTypeSymbolsAsIs() {
        FixedLengthFile file = new FixedLengthFile("t.dat");
        DataFileFragment frag = file.getNewFragment();
        frag.setNames(Arrays.asList("f1", "f2"));
        frag.setTypes(Arrays.asList("半角英字", "全角"));
        frag.setLengths(Arrays.asList("10", "4"));

        FileView view = TestCoreFileAdapter.read(file);

        // 器は 半角英字→X・全角→N へ正規化する。原文（半角英字/全角）は現れない。
        assertThat(view.getFragments().get(0).getTypes(), is(Arrays.asList("X", "N")));
    }

    /**
     * Given: 長さ省略（{@code -}）フィールドを持つ固定長ファイル器に値を追加する。
     * When : {@code read} する。
     * Then : 省略長は器が実バイト長へ上書きしたものが現れ、原文の {@code -} は残らない
     *        （＝原文復元は本クラスの責務でない）。
     */
    @Test
    public void exposesOndemandCalculatedLengthAsIs() {
        FixedLengthFile file = new FixedLengthFile("t.dat");
        file.setDirective("text-encoding", "UTF-8");
        DataFileFragment frag = file.getNewFragment();
        frag.setNames(Arrays.asList("f1", "f2"));
        frag.setTypes(Arrays.asList("半角英字", "半角英字"));
        frag.setLengths(Arrays.asList("-", "5"));
        frag.addValue(Arrays.asList("abcd", "x"));

        FileView view = TestCoreFileAdapter.read(file);

        List<String> lengths = view.getFragments().get(0).getLengths();
        // 省略長 "-" は実バイト長 "4" へ上書きされている（原文は失われる）。
        assertThat(lengths.get(0), is("4"));
        assertThat(lengths.get(1), is("5"));
    }

    /**
     * Given: 長さ行を持たない可変長ファイル器。
     * When : {@code read} する。
     * Then : 長さは {@code null}（可変長は長さを持たない）。
     */
    @Test
    public void readsVariableLengthFileWithoutLengths() {
        VariableLengthFile file = new VariableLengthFile("in.csv");
        DataFileFragment frag = file.getNewFragment();
        frag.setNames(Arrays.asList("f1"));
        frag.setTypes(Arrays.asList("半角英字"));
        frag.addValue(Arrays.asList("v"));

        FileView view = TestCoreFileAdapter.read(file);

        assertThat(view.getFragments().get(0).getLengths(), is(nullValue()));
        assertThat(view.getFragments().get(0).getValues().get(0).get("f1"), is("v"));
    }

    /**
     * Given: ディレクティブを設定した固定長ファイル器。
     * When : {@code read} する。
     * Then : ディレクティブが写される（{@code file-type} は器が既定で持つ）。
     */
    @Test
    public void readsDirectives() {
        FixedLengthFile file = new FixedLengthFile("t.dat");

        FileView view = TestCoreFileAdapter.read(file);

        assertThat(view.getDirectives().get("file-type"), is((Object) "Fixed"));
    }

    /**
     * Given: 読み取ったビュー。
     * When : 断片リストを変更しようとする。
     * Then : 読み取り専用（{@link UnsupportedOperationException}）。
     */
    @Test
    public void viewsAreReadOnly() {
        FixedLengthFile file = new FixedLengthFile("t.dat");
        DataFileFragment frag = file.getNewFragment();
        frag.setNames(Arrays.asList("f1"));
        FileView view = TestCoreFileAdapter.read(file);
        try {
            view.getFragments().clear();
            fail("UnsupportedOperationException が送出されるべき");
        } catch (UnsupportedOperationException e) {
            // OK
        }
    }

    /**
     * Given: 読み取り後に元の器の値を変更する。
     * When : ビューを参照する。
     * Then : ビューは独立（防御的コピー）で影響を受けない。
     */
    @Test
    public void viewIsDefensivelyCopied() {
        FixedLengthFile file = new FixedLengthFile("t.dat");
        DataFileFragment frag = file.getNewFragment();
        frag.setNames(Arrays.asList("f1"));
        frag.setTypes(Arrays.asList("半角英字"));
        frag.setLengths(Arrays.asList("3"));
        frag.addValue(Arrays.asList("abc"));

        FileView view = TestCoreFileAdapter.read(file);
        Map<String, String> originalRow = frag.values.get(0);
        originalRow.put("f1", "MUTATED");

        assertThat(view.getFragments().get(0).getValues().get(0).get("f1"), is("abc"));
    }
}
