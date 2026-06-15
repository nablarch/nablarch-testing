package nablarch.test.tool.converter;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@link ConverterFileFilter} のテストクラス。
 *
 * <p>
 * 実ディレクトリ（{@link TemporaryFolder}）に対し、Excel ブック／YAML コンテナディレクトリの列挙、
 * 一時ロックファイル・非対象拡張子の除外、include／exclude グロブ、再現性のためのソート、
 * 入力ディレクトリ不在時の例外を検証する。
 * </p>
 *
 * @author kiyobot
 */
public class ConverterFileFilterTest {

    /** 一時ディレクトリ */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** 入力ルート */
    private Path root;

    /**
     * 入力ルートを初期化する。
     */
    @Before
    public void setUp() {
        root = folder.getRoot().toPath();
    }

    /**
     * 指定相対パスに空ファイルを作成する（親ディレクトリは自動生成）。
     *
     * @param relative ルートからの相対パス
     * @return 作成したファイルの絶対パス
     * @throws IOException 入出力エラー
     */
    private Path touch(String relative) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.createFile(file);
        return file;
    }

    // ---- findXlsFiles ----

    /**
     * Given: .xls/.xlsx・非対象・一時ロック・ネストを含むディレクトリ。
     * When : findXlsFiles（フィルタなし）。
     * Then : ブックのみを相対パス辞書順で返す（非対象・{@code ~$} 一時ファイルは除外）。
     */
    @Test
    public void findsXlsBooksSortedIgnoringNonBooks() throws IOException {
        Path a = touch("A.xls");
        Path b = touch("B.xlsx");
        touch("notes.txt");
        touch("~$lock.xlsx");
        Path c = touch("sub/C.xls");

        List<Path> found = ConverterFileFilter.findXlsFiles(root, emptyList(), emptyList());

        assertThat(found, is(Arrays.asList(a, b, c)));
    }

    /**
     * Given: include グロブ。
     * When : findXlsFiles。
     * Then : 合致したブックのみ返す。
     */
    @Test
    public void findXlsFilesAppliesInclude() throws IOException {
        touch("A.xls");
        Path c = touch("sub/C.xls");

        List<Path> found = ConverterFileFilter.findXlsFiles(root, Arrays.asList("sub/**"), emptyList());

        assertThat(found, is(Arrays.asList(c)));
    }

    /**
     * Given: exclude グロブ。
     * When : findXlsFiles。
     * Then : 合致したブックを除外する。
     */
    @Test
    public void findXlsFilesAppliesExclude() throws IOException {
        Path a = touch("A.xls");
        Path b = touch("B.xlsx");
        touch("sub/C.xls");

        List<Path> found = ConverterFileFilter.findXlsFiles(root, emptyList(), Arrays.asList("**/C.xls"));

        assertThat(found, is(Arrays.asList(a, b)));
    }

    /**
     * Given: 入力ディレクトリが存在しない。
     * When : findXlsFiles。
     * Then : ConverterException。
     */
    @Test
    public void findXlsFilesRejectsMissingDirectory() {
        try {
            ConverterFileFilter.findXlsFiles(root.resolve("nope"), emptyList(), emptyList());
            fail("should throw");
        } catch (ConverterException e) {
            assertThat(e.getMessage().startsWith("input directory not found:"), is(true));
        }
    }

    // ---- findYamlDirs ----

    /**
     * Given: 同一ディレクトリ内複数 YAML・ネスト・ルート直下 YAML・YAML 無しディレクトリ。
     * When : findYamlDirs（フィルタなし）。
     * Then : YAML を直下に持つディレクトリのみを重複なく辞書順で返す。
     */
    @Test
    public void findsYamlDirsDedupedAndSorted() throws IOException {
        touch("d1/a.yaml");
        touch("d1/b.yaml");
        touch("d1/readme.txt");
        touch("d2/c.yaml");
        touch("d2/sub/e.yaml");
        touch("top.yaml");
        Files.createDirectories(root.resolve("empty"));

        List<Path> found = ConverterFileFilter.findYamlDirs(root, emptyList(), emptyList());

        assertThat(found, is(Arrays.asList(
                root,
                root.resolve("d1"),
                root.resolve("d2"),
                root.resolve("d2/sub"))));
    }

    /**
     * Given: 複数の include グロブ。
     * When : findYamlDirs。
     * Then : いずれかに合致したディレクトリのみ返す。
     */
    @Test
    public void findYamlDirsAppliesInclude() throws IOException {
        touch("d1/a.yaml");
        touch("d2/c.yaml");
        touch("d2/sub/e.yaml");

        List<Path> found = ConverterFileFilter.findYamlDirs(root, Arrays.asList("d2", "d2/**"), emptyList());

        assertThat(found, is(Arrays.asList(root.resolve("d2"), root.resolve("d2/sub"))));
    }

    /**
     * Given: exclude グロブ。
     * When : findYamlDirs。
     * Then : 合致したディレクトリを除外する。
     */
    @Test
    public void findYamlDirsAppliesExclude() throws IOException {
        touch("d1/a.yaml");
        touch("d2/c.yaml");

        List<Path> found = ConverterFileFilter.findYamlDirs(root, emptyList(), Arrays.asList("d1"));

        assertThat(found, is(Arrays.asList(root.resolve("d2"))));
    }

    /**
     * Given: 入力ディレクトリが存在しない。
     * When : findYamlDirs。
     * Then : ConverterException。
     */
    @Test
    public void findYamlDirsRejectsMissingDirectory() {
        try {
            ConverterFileFilter.findYamlDirs(root.resolve("nope"), emptyList(), emptyList());
            fail("should throw");
        } catch (ConverterException e) {
            assertThat(e.getMessage().startsWith("input directory not found:"), is(true));
        }
    }
}
