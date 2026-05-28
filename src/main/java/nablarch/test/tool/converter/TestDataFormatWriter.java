package nablarch.test.tool.converter;

import nablarch.test.tool.converter.model.TestDataContainer;

import java.nio.file.Path;

/**
 * {@link TestDataContainer} を指定された形式で書き出すインターフェース。
 */
public interface TestDataFormatWriter {

    /**
     * TestDataContainer を指定されたパスに書き出す。
     *
     * @param container  書き出す TestDataContainer
     * @param outputPath 書き出し先の基底パス（Excel ファイル / YAML ディレクトリの親）
     * @param overwrite  既存ファイルを上書きするか
     * @throws ConverterException IO エラーまたは上書き禁止エラーが発生した場合
     */
    void write(TestDataContainer container, Path outputPath, boolean overwrite) throws ConverterException;
}
