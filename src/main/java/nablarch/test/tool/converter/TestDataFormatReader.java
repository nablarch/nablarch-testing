package nablarch.test.tool.converter;

import java.nio.file.Path;

/**
 * テストデータを読み込んで {@link TestDataContainer} に変換するインターフェース。
 */
public interface TestDataFormatReader {

    /**
     * 指定されたパスを読み込み、TestDataContainer として返す。
     *
     * @param sourcePath 読み込み元パス（Excel ファイル / YAML ディレクトリ）
     * @return 変換結果の TestDataContainer
     * @throws ConverterException IO エラーまたは書式エラーが発生した場合
     */
    TestDataContainer read(Path sourcePath) throws ConverterException;
}
