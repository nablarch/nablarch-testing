package nablarch.test.core.reader.yaml.model;

/**
 * 構造マッピング層が返すフィールド定義（値未加工）。
 *
 * <p>
 * YAML に記述されたフィールドの名称・型・長さを、解釈・補完を一切施さずに保持する不変オブジェクト。
 * 長さは省略時 {@code null}。{@code "-"}（自動拡張指示）等の記法もリテラルのまま保持する。
 * </p>
 *
 * @author kiyotis
 */
public final class RawFieldDef {

    private final String name;
    private final String type;
    private final String length;

    /**
     * コンストラクタ。
     *
     * @param name   フィールド名称（YAML 記述のまま）
     * @param type   データ型（YAML 記述のまま。省略時 {@code null}）
     * @param length フィールド長（YAML 記述のまま。省略時 {@code null}）
     */
    public RawFieldDef(String name, String type, String length) {
        this.name = name;
        this.type = type;
        this.length = length;
    }

    /** @return フィールド名称 */
    public String getName() {
        return name;
    }

    /** @return データ型（省略時 {@code null}） */
    public String getType() {
        return type;
    }

    /** @return フィールド長（省略時 {@code null}） */
    public String getLength() {
        return length;
    }
}
