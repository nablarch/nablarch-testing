package nablarch.test.tool.converter;

import java.nio.file.Path;

/**
 * 入力パスと出力パスの対応関係を計算するユーティリティクラス。
 */
public final class ConverterPathResolver {

    private ConverterPathResolver() {
    }

    /**
     * XLS ファイルパスから YAML 出力ディレクトリパスを計算する。
     *
     * <p>例: inputRoot=src, xls=src/foo/FooTest.xls, outputRoot=out → out/foo/FooTest</p>
     *
     * @param inputRoot  入力ルートディレクトリ
     * @param xlsFile    XLS ファイルパス
     * @param outputRoot 出力ルートディレクトリ
     * @return YAML ディレクトリパス
     */
    public static Path xlsToYamlDir(Path inputRoot, Path xlsFile, Path outputRoot) {
        Path relative = inputRoot.relativize(xlsFile);
        String fileName = relative.getFileName().toString();
        String baseName;
        if (fileName.endsWith(".xlsx")) {
            baseName = fileName.substring(0, fileName.length() - 5);
        } else if (fileName.endsWith(".xls")) {
            baseName = fileName.substring(0, fileName.length() - 4);
        } else {
            baseName = fileName;
        }
        Path parent = relative.getParent();
        if (parent != null) {
            return outputRoot.resolve(parent).resolve(baseName);
        }
        return outputRoot.resolve(baseName);
    }

    /**
     * YAML ディレクトリパスから XLS 出力ファイルパスを計算する。
     *
     * <p>例: inputRoot=src, yamlDir=src/foo/FooTest, outputRoot=out → out/foo/FooTest.xls</p>
     *
     * @param inputRoot  入力ルートディレクトリ
     * @param yamlDir    YAML ディレクトリパス
     * @param outputRoot 出力ルートディレクトリ
     * @return XLS ファイルパス
     */
    public static Path yamlDirToXls(Path inputRoot, Path yamlDir, Path outputRoot) {
        Path relative = inputRoot.relativize(yamlDir);
        String dirName = relative.getFileName().toString();
        Path parent = relative.getParent();
        if (parent != null) {
            return outputRoot.resolve(parent).resolve(dirName + ".xls");
        }
        return outputRoot.resolve(dirName + ".xls");
    }
}
