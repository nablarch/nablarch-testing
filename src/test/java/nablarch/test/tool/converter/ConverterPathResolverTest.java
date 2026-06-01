package nablarch.test.tool.converter;

import org.junit.Test;

import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link ConverterPathResolver} のテスト（6.5節）。
 */
public class ConverterPathResolverTest {

    /**
     * [Given] inputRoot=src/test/resources, XLS=src/test/resources/foo/FooTest.xls, outputRoot=out
     * [When]  xlsToYamlDir() を呼び出す
     * [Then]  out/foo/FooTest が返される
     */
    @Test
    public void xlsToYamlDir() {
        java.nio.file.Path result = ConverterPathResolver.xlsToYamlDir(
                Paths.get("src/test/resources"),
                Paths.get("src/test/resources/foo/FooTest.xls"),
                Paths.get("out")
        );
        assertThat(result, is(Paths.get("out/foo/FooTest")));
    }

    /**
     * [Given] inputRoot=src, YAML dir=src/foo/FooTest, outputRoot=out
     * [When]  yamlDirToXls() を呼び出す
     * [Then]  out/foo/FooTest.xls が返される
     */
    @Test
    public void yamlDirToXls() {
        java.nio.file.Path result = ConverterPathResolver.yamlDirToXls(
                Paths.get("src"),
                Paths.get("src/foo/FooTest"),
                Paths.get("out")
        );
        assertThat(result, is(Paths.get("out/foo/FooTest.xls")));
    }

    /**
     * [Given] inputRoot == XLS の直接親ディレクトリ（サブディレクトリなし）
     * [When]  xlsToYamlDir() を呼び出す
     * [Then]  outputRoot/FooTest が返される
     */
    @Test
    public void xlsToYamlDirFlat() {
        java.nio.file.Path result = ConverterPathResolver.xlsToYamlDir(
                Paths.get("src"),
                Paths.get("src/FooTest.xls"),
                Paths.get("out")
        );
        assertThat(result, is(Paths.get("out/FooTest")));
    }

    /**
     * [Given] inputRoot=src/test/resources, XLSX=src/test/resources/foo/FooTest.xlsx, outputRoot=out
     * [When]  xlsToYamlDir() を呼び出す
     * [Then]  out/foo/FooTest が返される（.xlsx 拡張子も除去される）
     */
    @Test
    public void xlsToYamlDirXlsx() {
        java.nio.file.Path result = ConverterPathResolver.xlsToYamlDir(
                Paths.get("src/test/resources"),
                Paths.get("src/test/resources/foo/FooTest.xlsx"),
                Paths.get("out")
        );
        assertThat(result, is(Paths.get("out/foo/FooTest")));
    }

    /**
     * [Given] XLS ファイルが .xls 拡張子を持たないファイル名
     * [When]  xlsToYamlDir() を呼び出す
     * [Then]  ファイル名そのままが YAML ディレクトリ名になる
     */
    @Test
    public void xlsToYamlDirNoXlsExtension() {
        java.nio.file.Path result = ConverterPathResolver.xlsToYamlDir(
                Paths.get("src"),
                Paths.get("src/foo/FooTestNoExt"),
                Paths.get("out")
        );
        assertThat(result, is(Paths.get("out/foo/FooTestNoExt")));
    }
}
