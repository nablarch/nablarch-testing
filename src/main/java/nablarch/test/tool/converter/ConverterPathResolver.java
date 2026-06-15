package nablarch.test.tool.converter;

import java.nio.file.Path;

/**
 * 入力リソースの位置から出力先ディレクトリ（Writer に渡す {@code basePath}）を算出する純関数群。
 *
 * <p>
 * 入力ディレクトリ配下の相対構造を出力ディレクトリ側へそのまま写す。Excel↔YAML の粒度差
 * （Excel は 1 ブック＝1 ファイル／YAML は 1 コンテナ＝1 ディレクトリ）を吸収する。
 * </p>
 *
 * <ul>
 *   <li>Excel → YAML: ブック {@code foo/bar.xlsx} は YAML ディレクトリ {@code foo/bar/} へ。
 *       Writer は当該ディレクトリへ各シート（セクション）を {@code <シート名>.yaml} で出力する。</li>
 *   <li>YAML → Excel: ディレクトリ {@code foo/bar/} はブック {@code foo/bar.xlsx} へ。
 *       Writer は親ディレクトリ {@code foo/} へ {@code <コンテナ名>.xlsx} を出力する。</li>
 * </ul>
 *
 * @author kiyobot
 */
public final class ConverterPathResolver {

    /** ユーティリティクラスにつきインスタンス化不可。 */
    private ConverterPathResolver() {
        throw new AssertionError("ConverterPathResolver は static 専用です");
    }

    /**
     * YAML 出力時の Writer 用 basePath を算出する。
     *
     * <p>出力は {@code <出力ルート>/<入力ルートからの相対ディレクトリ>/<ブック名>/} となり、
     * その配下へ各シートが YAML ファイルとして書き出される。</p>
     *
     * @param inputRoot  入力ルートディレクトリ
     * @param bookFile   変換対象の Excel ブックファイル（{@code inputRoot} 配下）
     * @param outputRoot 出力ルートディレクトリ
     * @return Writer に渡す出力先ディレクトリ
     */
    public static Path outputBaseForYaml(Path inputRoot, Path bookFile, Path outputRoot) {
        Path relative = inputRoot.relativize(bookFile);
        Path relativeDir = relative.getParent();
        Path dir = relativeDir == null ? outputRoot : outputRoot.resolve(relativeDir);
        return dir.resolve(stripExtension(bookFile.getFileName().toString()));
    }

    /**
     * Excel 出力時の Writer 用 basePath を算出する。
     *
     * <p>出力は {@code <出力ルート>/<入力ルートからの相対親ディレクトリ>/} となり、その配下へ
     * {@code <コンテナ名>.xlsx} が書き出される。</p>
     *
     * @param inputRoot  入力ルートディレクトリ
     * @param yamlDir    変換対象の YAML コンテナディレクトリ（{@code inputRoot} 配下）
     * @param outputRoot 出力ルートディレクトリ
     * @return Writer に渡す出力先ディレクトリ
     */
    public static Path outputBaseForXls(Path inputRoot, Path yamlDir, Path outputRoot) {
        Path relative = inputRoot.relativize(yamlDir);
        Path relativeParent = relative.getParent();
        return relativeParent == null ? outputRoot : outputRoot.resolve(relativeParent);
    }

    /**
     * ファイル名から拡張子（最後の {@code "."} 以降）を取り除く。
     *
     * <p>同一パッケージの入口（{@link TestDataConverter}）も共有するため package-private とする。</p>
     *
     * @param fileName ファイル名
     * @return 拡張子を除いた名前。{@code "."} を含まない場合はそのまま
     */
    static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }
}
