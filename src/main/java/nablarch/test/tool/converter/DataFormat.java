package nablarch.test.tool.converter;

/**
 * テストデータの変換形式を表す enum。
 */
public enum DataFormat {

    XLS("xls"),
    YAML("yaml");

    private final String argument;

    DataFormat(String argument) {
        this.argument = argument;
    }

    /** @return CLI 引数文字列（"xls" または "yaml"） */
    public String toArgument() {
        return argument;
    }

    /**
     * CLI 引数文字列から {@link DataFormat} に変換する。
     *
     * @param value CLI 引数値
     * @return 対応する {@link DataFormat}
     * @throws IllegalArgumentException 未知の値または null の場合
     */
    public static DataFormat fromArgument(String value) {
        if (value == null) {
            throw new IllegalArgumentException("DataFormat argument must not be null");
        }
        for (DataFormat format : values()) {
            if (format.argument.equals(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown DataFormat argument: " + value + ". Must be 'xls' or 'yaml'.");
    }
}
