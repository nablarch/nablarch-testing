package nablarch.test.tool.converter;

/**
 * 変換ツールが扱うテストデータ形式。
 *
 * <p>
 * 入口（{@link TestDataConverter}）が変換元・変換先の形式を指定するために用いる。各形式は
 * コマンドライン引数・設定値として用いる短い識別子（{@link #getArgument() argument}）を持つ。
 * </p>
 *
 * @author kiyobot
 */
public enum DataFormat {

    /** Excel（{@code .xlsx}） */
    XLS("xls"),

    /** YAML（{@code .yaml}） */
    YAML("yaml");

    /** 引数・設定値で用いる識別子 */
    private final String argument;

    /**
     * コンストラクタ。
     *
     * @param argument 引数・設定値で用いる識別子
     */
    DataFormat(String argument) {
        this.argument = argument;
    }

    /**
     * 引数・設定値で用いる識別子を返す。
     *
     * @return 識別子（{@code "xls"} または {@code "yaml"}）
     */
    public String getArgument() {
        return argument;
    }

    /**
     * 識別子（{@code "xls"}／{@code "yaml"}）から形式を解決する。
     *
     * @param argument 識別子（前後空白は許容せず厳密一致）
     * @return 対応する形式
     * @throws IllegalArgumentException 識別子が未知の場合
     */
    public static DataFormat fromArgument(String argument) {
        for (DataFormat format : values()) {
            if (format.argument.equals(argument)) {
                return format;
            }
        }
        throw new IllegalArgumentException("unknown data format: " + argument);
    }
}
