package nablarch.test.core.entity;

import nablarch.core.validation.ValidationResultMessage;

import static nablarch.core.util.Builder.concat;

/**
 * {@link ValidationResultMessage}同士の比較をメッセージ本文とプロパティ名で実行するためのクラス。
 */
public class BeanValidationResultMessage extends ValidationResultMessage {

    /**
     * メッセージ
     */
    private final ValidationResultMessage message;

    /**
     * コンストラクタ。
     * @param message {@link ValidationResultMessage}
     */
    public BeanValidationResultMessage(ValidationResultMessage message) {
        super(null,null,null);
        this.message = message;
    }

    @Override
    public String getPropertyName() {
        return message.getPropertyName();
    }

    @Override
    public String formatMessage() {
        return message.formatMessage();
    }

    /**
     * このオブジェクトと等価であるかを返す。
     *
     * <p>
     * {@code obj}が以下の条件を全て満たす場合{@code true}を返す。
     * <ul>
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
        return message.formatMessage().equals(another.formatMessage())
                && message.getPropertyName().equals(another.getPropertyName());
    }

    /**
     * このオブジェクトのハッシュコード値を返す。
     *
     * <p>
     * 委譲先のメッセージのハッシュコード値を返却するのみ。
     *
     * @return ハッシュコード値。
     */
    @Override
    public int hashCode() {
        return message != null ? message.hashCode() : 0;
    }

    /**
     * このオブジェクトの文字列表現を返す。
     *
     * @return メッセージIDとバリデーション対象プロパティを記載した文字列
     */
    @Override
    public String toString() {
        return concat(
                "messageContent=[", formatMessage(), "] ",
                "propertyName=[", getPropertyName(), "]"
                );
    }
}
