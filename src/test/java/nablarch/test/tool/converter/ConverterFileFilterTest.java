package nablarch.test.tool.converter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
