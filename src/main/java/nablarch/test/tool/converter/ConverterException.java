package nablarch.test.tool.converter;

/**
 * テストデータ変換ツール専用の検査例外。
 */
public class ConverterException extends Exception {

    public ConverterException(String message) {
        super(message);
    }

    public ConverterException(String message, Throwable cause) {
        super(message, cause);
    }
}
