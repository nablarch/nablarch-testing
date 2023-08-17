package nablarch.test.core.entity;

import nablarch.core.message.StringResource;
import nablarch.core.validation.ValidationResultMessage;

/**
 * {@link ValidationResultMessage}同士の比較をメッセージ本文とプロパティ名で実行するためのクラス。
 */
public class BeanValidationResultMessage extends ValidationResultMessage {

    /**
     * コンストラクタ。
     * スーパークラスのコンストラクタを呼び出すのみ。
     * @param propertyName 検証対象のプロパティ名
     * @param message      メッセージ
     * @param parameters   オプションパラメータ
     */
    public BeanValidationResultMessage(String propertyName, StringResource message, Object[] parameters) {
        super(propertyName, message, parameters);
    }

    /**
     * このオブジェクトと等価であるかを返す。
     * <p/>
     * {@code obj}が以下の条件を全て満たす場合{@code true}を返す。
     * <ul>
     *     <li>{@code null}ではないこと。</li>
     *     <li>{@link ValidationResultMessage}もしくはそれを継承した型であること</li>
     *     <li>メッセージ本文が同値であること。</li>
     *     <li>バリデーション対象のプロパティ名が同値であること。</li>
     * </ul>
     *
     * @return このオブジェクトと等価である場合{@code true}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ValidationResultMessage)) {
            return false;
        }
        ValidationResultMessage another = (ValidationResultMessage) obj;
        return formatMessage().equals(another.formatMessage())
                && getPropertyName().equals(another.getPropertyName());
    }

    /**
     * このオブジェクトのハッシュコード値を返す。
     * フィールドを保持していないので、スーパークラスのハッシュコード値を返却するのみ。
     * @return ハッシュコード値。
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
