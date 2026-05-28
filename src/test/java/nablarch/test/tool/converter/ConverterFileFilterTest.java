package nablarch.test.tool.converter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

/**
 * {@link ConverterFileFilter} のテスト（3.2節・6.5節）。
 */
public class ConverterFileFilterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    // -------------------------------------------------------------------------
    // XLS ファイルの列挙
    // -------------------------------------------------------------------------

    /**
     * [Given] ルートディレクトリ直下に .xls ファイルがある
     * [When]  findXlsFiles() を呼び出す
     * [Then]  .xls ファイルが列挙される
     */
    @Test
    public void findXlsFilesInRoot() throws Exception {
        File root = temporaryFolder.newFolder("src");
        touch(root, "FooTest.xls");
        touch(root, "BarTest.xls");

        List<Path> result = ConverterFileFilter.findXlsFiles(root.toPath(),
                Collections.emptyList(), Collections.emptyList());

        assertThat(result.size(), is(2));
    }

    /**
     * [Given] ネストしたディレクトリに .xls ファイルがある
     * [When]  findXlsFiles() を呼び出す
     * [Then]  再帰的に列挙される
     */
    @Test
    public void findXlsFilesRecursively() throws Exception {
        File root = temporaryFolder.newFolder("src");
        File sub = new File(root, "sub");
        sub.mkdir();
        touch(root, "FooTest.xls");
        touch(sub, "BarTest.xls");

        List<Path> result = ConverterFileFilter.findXlsFiles(root.toPath(),
                Collections.emptyList(), Collections.emptyList());

        assertThat(result.size(), is(2));
    }

    /**
     * [Given] .xlsx ファイルがある
     * [When]  findXlsFiles() を呼び出す
     * [Then]  .xlsx は列挙されない
     */
    @Test
    public void xlsxFilesExcluded() throws Exception {
        File root = temporaryFolder.newFolder("src");
        touch(root, "FooTest.xlsx");
        touch(root, "BarTest.xls");

        List<Path> result = ConverterFileFilter.findXlsFiles(root.toPath(),
                Collections.emptyList(), Collections.emptyList());

        assertThat(result.size(), is(1));
    }

    /**
     * [Given] --exclude パターンに合致するファイルがある
     * [When]  findXlsFiles() を呼び出す
     * [Then]  合致するファイルが除外される（3.2節）
     */
    @Test
    public void excludePatternFilters() throws Exception {
        File root = temporaryFolder.newFolder("src");
        touch(root, "FooTest.xls");
        touch(root, "template.xls");

        List<Path> result = ConverterFileFilter.findXlsFiles(root.toPath(),
                Collections.emptyList(),
                Collections.singletonList("template.xls"));

        assertThat(result.size(), is(1));
        assertThat(fileNames(result), hasItem("FooTest.xls"));
        assertThat(fileNames(result), not(hasItem("template.xls")));
    }

    /**
     * [Given] --include パターンが指定されている
     * [When]  findXlsFiles() を呼び出す
     * [Then]  パターンに合致するファイルのみ列挙される（3.2節）
     */
    @Test
    public void includePatternFilters() throws Exception {
        File root = temporaryFolder.newFolder("src");
        touch(root, "FooTest.xls");
        touch(root, "BarTest.xls");

        List<Path> result = ConverterFileFilter.findXlsFiles(root.toPath(),
                Collections.singletonList("Foo*.xls"),
                Collections.emptyList());

        assertThat(result.size(), is(1));
        assertThat(fileNames(result), hasItem("FooTest.xls"));
    }

    // -------------------------------------------------------------------------
    // YAML ディレクトリの列挙
    // -------------------------------------------------------------------------

    /**
     * [Given] .yaml ファイルを直下に含むディレクトリがある
     * [When]  findYamlDirs() を呼び出す
     * [Then]  そのディレクトリが列挙される（3.3節 YAML ディレクトリの定義）
     */
    @Test
    public void findYamlDirsWithYamlFiles() throws Exception {
        File root = temporaryFolder.newFolder("src");
        File fooDir = new File(root, "FooTest");
        fooDir.mkdir();
        touch(fooDir, "case01.yaml");

        List<Path> result = ConverterFileFilter.findYamlDirs(root.toPath(),
                Collections.emptyList(), Collections.emptyList());

        assertThat(result.size(), is(1));
        assertThat(result.get(0).getFileName().toString(), is("FooTest"));
    }

    /**
     * [Given] .yaml ファイルを持つサブディレクトリが .yaml を持つサブディレクトリを含む
     * [When]  findYamlDirs() を呼び出す
     * [Then]  最下位の YAML ディレクトリのみ列挙される（3.3節）
     */
    @Test
    public void findYamlDirsOnlyLeafDirs() throws Exception {
        File root = temporaryFolder.newFolder("src");
        File parent = new File(root, "FooTest");
        parent.mkdir();
        File child = new File(parent, "subcase");
        child.mkdir();
        touch(child, "case01.yaml");

        List<Path> result = ConverterFileFilter.findYamlDirs(root.toPath(),
                Collections.emptyList(), Collections.emptyList());

        // parent has no direct .yaml files and has a subdir with .yaml -> not a YAML dir
        // child has .yaml files and no subdir with .yaml -> YAML dir
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getFileName().toString(), is("subcase"));
    }

    /**
     * [Given] --exclude パターンに合致する YAML ディレクトリがある
     * [When]  findYamlDirs() を呼び出す
     * [Then]  合致するディレクトリが除外される
     */
    @Test
    public void excludePatternFiltersYamlDirs() throws Exception {
        File root = temporaryFolder.newFolder("src");
        File fooDir = new File(root, "FooTest");
        fooDir.mkdir();
        touch(fooDir, "case01.yaml");
        File templateDir = new File(root, "templateDir");
        templateDir.mkdir();
        touch(templateDir, "data.yaml");

        List<Path> result = ConverterFileFilter.findYamlDirs(root.toPath(),
                Collections.emptyList(),
                Collections.singletonList("templateDir"));

        assertThat(result.size(), is(1));
        assertThat(result.get(0).getFileName().toString(), is("FooTest"));
    }

    /**
     * [Given] .yaml ファイルを持つが .yaml を含むサブディレクトリも持つディレクトリ（isYamlDir が false）
     * [When]  findYamlDirs() を呼び出す
     * [Then]  そのディレクトリは列挙されず、子ディレクトリが列挙される
     */
    @Test
    public void yamlDirWithSubdirContainingYamlIsNotListed() throws Exception {
        File root = temporaryFolder.newFolder("src");
        File parent = new File(root, "FooTest");
        parent.mkdir();
        // parent 直下にも .yaml を置く
        touch(parent, "top.yaml");
        // さらにサブディレクトリにも .yaml を置く（isYamlDir → false になる）
        File child = new File(parent, "sub");
        child.mkdir();
        touch(child, "case01.yaml");

        List<Path> result = ConverterFileFilter.findYamlDirs(root.toPath(),
                Collections.emptyList(), Collections.emptyList());

        // parent は .yaml を含む subdir を持つため isYamlDir=false
        // child が YAML ディレクトリとして列挙される
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getFileName().toString(), is("sub"));
    }

    /**
     * [Given] .yaml を含まないサブディレクトリのみを持つディレクトリがある
     * [When]  findYamlDirs() を呼び出す
     * [Then]  containsYaml が false を返しそのディレクトリは isYamlDir で除外されない
     */
    @Test
    public void containsYamlReturnsFalseForDirWithNoYaml() throws Exception {
        File root = temporaryFolder.newFolder("src");
        File parent = new File(root, "FooTest");
        parent.mkdir();
        // parent 直下に .yaml
        touch(parent, "case01.yaml");
        // parent のサブディレクトリには .yaml がない（containsYaml → false → L160）
        File emptySubDir = new File(parent, "noYamlDir");
        emptySubDir.mkdir();
        touch(emptySubDir, "data.xls"); // .yaml でないファイル

        List<Path> result = ConverterFileFilter.findYamlDirs(root.toPath(),
                Collections.emptyList(), Collections.emptyList());

        // emptySubDir は .yaml を含まないため containsYaml=false
        // parent は .yaml を持ちかつ .yaml を含む subdir がない → isYamlDir=true
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getFileName().toString(), is("FooTest"));
    }

    /**
     * [Given] .yaml を含むサブディレクトリのサブディレクトリ（再帰 containsYaml）がある
     * [When]  findYamlDirs() を呼び出す
     * [Then]  containsYaml の再帰パス（L158）が通る
     */
    @Test
    public void containsYamlRecursiveSubdir() throws Exception {
        // 構造: root/FooTest/sub/deep/case01.yaml
        // FooTest 直下に .yaml なし、sub 直下に .yaml なし、deep に .yaml あり
        File root = temporaryFolder.newFolder("src");
        File fooTest = new File(root, "FooTest");
        fooTest.mkdir();
        // FooTest 直下にも .yaml を置く（containsYaml チェック対象にするため isYamlDir の呼び出しが必要）
        touch(fooTest, "top.yaml");
        File sub = new File(fooTest, "sub");
        sub.mkdir();
        // sub 直下に .yaml なし
        File deep = new File(sub, "deep");
        deep.mkdir();
        touch(deep, "case01.yaml");

        List<Path> result = ConverterFileFilter.findYamlDirs(root.toPath(),
                Collections.emptyList(), Collections.emptyList());

        // FooTest: .yaml あり かつ sub が containsYaml(sub) → containsYaml(deep) → true
        // → isYamlDir(FooTest) = false
        // deep が YAML ディレクトリとして列挙される
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getFileName().toString(), is("deep"));
    }

    /**
     * [Given] YAML ディレクトリの --include パターンに合致しないディレクトリがある
     * [When]  findYamlDirs() を呼び出す
     * [Then]  合致しないディレクトリはスキップカウントが増加し除外される
     */
    @Test
    public void includePatternFiltersYamlDirs() throws Exception {
        File root = temporaryFolder.newFolder("src");
        File fooDir = new File(root, "FooTest");
        fooDir.mkdir();
        touch(fooDir, "case01.yaml");
        File barDir = new File(root, "BarTest");
        barDir.mkdir();
        touch(barDir, "case01.yaml");

        int[] skipCount = {0};
        List<Path> result = ConverterFileFilter.findYamlDirs(root.toPath(),
                Collections.singletonList("FooTest"),
                Collections.emptyList(),
                skipCount);

        assertThat(result.size(), is(1));
        assertThat(result.get(0).getFileName().toString(), is("FooTest"));
        assertThat(skipCount[0], is(1));
    }

    /**
     * [Given] Files.walkFileTree が IOException をスローする状況（findXlsFiles）
     * [When]  findXlsFiles() を呼び出す
     * [Then]  ConverterException がスローされる
     */
    @Test(expected = ConverterException.class)
    public void findXlsFilesThrowsOnIoException() throws Exception {
        File root = temporaryFolder.newFolder("src");
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.walkFileTree(any(Path.class), any()))
                    .thenThrow(new IOException("Simulated walk failure"));

            ConverterFileFilter.findXlsFiles(root.toPath(),
                    Collections.emptyList(), Collections.emptyList());
        }
    }

    /**
     * [Given] Files.walkFileTree が IOException をスローする状況（findYamlDirs）
     * [When]  findYamlDirs() を呼び出す
     * [Then]  ConverterException がスローされる
     */
    @Test(expected = ConverterException.class)
    public void findYamlDirsThrowsOnIoException() throws Exception {
        File root = temporaryFolder.newFolder("src");
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.walkFileTree(any(Path.class), any()))
                    .thenThrow(new IOException("Simulated walk failure"));

            ConverterFileFilter.findYamlDirs(root.toPath(),
                    Collections.emptyList(), Collections.emptyList());
        }
    }

    // -------------------------------------------------------------------------
    // ヘルパー
    // -------------------------------------------------------------------------

    private void touch(File dir, String name) throws Exception {
        new File(dir, name).createNewFile();
    }

    private List<String> fileNames(List<Path> paths) {
        List<String> names = new java.util.ArrayList<>();
        for (Path p : paths) names.add(p.getFileName().toString());
        return names;
    }
}
