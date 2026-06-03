package nablarch.test.tool.converter.yaml;

/**
 * YAML テストデータ検証エラーを表す不変の値オブジェクト。
 */
public final class ValidationError {

    private final String filePath;
    private final String location;
    private final String message;

    public ValidationError(String filePath, String location, String message) {
        this.filePath = filePath;
        this.location = location;
        this.message = message;
    }

    /** 検証対象ファイルの絶対パス文字列。 */
    public String getFilePath() {
        return filePath;
    }

    /** セクション・ブロック・行の識別文字列（例: "setup_files[0].records[1].rows[2]"）。 */
    public String getLocation() {
        return location;
    }

    /** エラーの説明。 */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "[" + filePath + "] " + location + ": " + message;
    }
}
