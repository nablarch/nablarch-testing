package nablarch.test.tool.converter.model;

/**
 * ファイルデータブロックのフィールド定義。
 * 不変オブジェクト。
 */
public final class FieldDef {

    private final String name;
    /** データ型記号（"X", "N", "Z" 等）。可変長 FW_HEADER では null。 */
    private final String type;
    /**
     * フィールド長。固定長のみ。可変長は null。
     * "-"（SS-17: 自動拡張指示）を含むためリテラルとして String で保持する。
     */
    private final String length;

    public FieldDef(String name, String type, String length) {
        this.name = name;
        this.type = type;
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getLength() {
        return length;
    }
}
