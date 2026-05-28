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
}
