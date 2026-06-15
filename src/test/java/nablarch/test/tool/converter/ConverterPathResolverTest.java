package nablarch.test.tool.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

/**
 * {@link ConverterPathResolver} のテストクラス。
 *
 * <p>
 * 入力ルート配下の相対構造を出力側へ写すこと、Excel↔YAML の粒度差（ブック＝ファイル／コンテナ＝ディレクトリ）
 * を吸収することを純関数として検証する。
 * </p>
 *
 * @author kiyobot
 */
public class ConverterPathResolverTest {

    /** 入力ルート */
    private static final Path IN = Paths.get("/in");

    /** 出力ルート */
    private static final Path OUT = Paths.get("/out");

    /**
     * Given: 入力ルート直下のブック。
     * When : outputBaseForYaml。
     * Then : 出力ルート直下のブック名ディレクトリ（ここへ各シートが YAML 出力される）。
     */
    @Test
    public void yamlBaseForTopLevelBook() {
        Path base = ConverterPathResolver.outputBaseForYaml(IN, Paths.get("/in/UsersTest.xlsx"), OUT);
        assertThat(base, is(Paths.get("/out/UsersTest")));
    }

    /**
     * Given: サブディレクトリ配下のブック。
     * When : outputBaseForYaml。
     * Then : 相対ディレクトリを保ったブック名ディレクトリ。
     */
    @Test
    public void yamlBaseForNestedBook() {
        Path base = ConverterPathResolver.outputBaseForYaml(IN, Paths.get("/in/sub/pkg/UsersTest.xlsx"), OUT);
        assertThat(base, is(Paths.get("/out/sub/pkg/UsersTest")));
    }

    /**
     * Given: 拡張子 {@code .xls} のブック。
     * When : outputBaseForYaml。
     * Then : 拡張子を除いた名前のディレクトリ。
     */
    @Test
    public void yamlBaseStripsXlsExtension() {
        Path base = ConverterPathResolver.outputBaseForYaml(IN, Paths.get("/in/Legacy.xls"), OUT);
        assertThat(base, is(Paths.get("/out/Legacy")));
    }

    /**
     * Given: 入力ルート直下の YAML コンテナディレクトリ。
     * When : outputBaseForXls。
     * Then : 出力ルート自身（Writer が {@code <コンテナ名>.xlsx} を直下へ出力する）。
     */
    @Test
    public void xlsBaseForTopLevelDir() {
        Path base = ConverterPathResolver.outputBaseForXls(IN, Paths.get("/in/UsersTest"), OUT);
        assertThat(base, is(OUT));
    }

    /**
     * Given: サブディレクトリ配下の YAML コンテナディレクトリ。
     * When : outputBaseForXls。
     * Then : 相対親ディレクトリを保った出力ディレクトリ。
     */
    @Test
    public void xlsBaseForNestedDir() {
        Path base = ConverterPathResolver.outputBaseForXls(IN, Paths.get("/in/sub/pkg/UsersTest"), OUT);
        assertThat(base, is(Paths.get("/out/sub/pkg")));
    }

    /**
     * Given: 入力ルート自身が YAML コンテナ（直下に YAML を持つ）。
     * When : outputBaseForXls。
     * Then : 出力ルート自身（ブックは出力ルート直下へ）。
     */
    @Test
    public void xlsBaseWhenTargetIsInputRoot() {
        Path base = ConverterPathResolver.outputBaseForXls(IN, IN, OUT);
        assertThat(base, is(OUT));
    }
}
