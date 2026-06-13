package nablarch.test.tool.converter.model;

/**
 * ファイル／メッセージのフィールド定義。不変オブジェクト。
 *
 * <p>
 * 名称・型・長さを、形式の記法を加工せずそのまま保持する。型・長さの省略は {@code null}。
 * 名称は大文字化しない。長さは {@code "-"}（自動拡張指示）等の記法もありうるためリテラルとして
 * {@link String} で保持する（数値化しない）。
 * </p>
 *
 * @author kiyotis
 */
public final class FieldDef {

    private final String name;
    private final String type;
    private final String length;

    /**
     * コンストラクタ。
     *
     * @param name   フィールド名称（記述のまま。大文字化なし）
     * @param type   データ型（記述のまま。省略時は {@code null}）
     * @param length フィールド長（記述のまま。{@code "-"} 等もリテラル保持。省略時は {@code null}）
     */
    public FieldDef(String name, String type, String length) {
        this.name = name;
        this.type = type;
        this.length = length;
    }

    /** @return フィールド名称 */
    public String getName() {
        return name;
    }

    /** @return データ型（省略時は {@code null}） */
    public String getType() {
        return type;
    }

    /** @return フィールド長（省略時は {@code null}） */
    public String getLength() {
        return length;
    }
}
