package nablarch.test.core.entity;

import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;

import static nablarch.core.util.Builder.concat;

/**
 * {@link Message}同士の比較をメッセージIDで実行するためのクラス。
 */
public class MessageComparedById extends Message {

    /**
     * メッセージ
     */
    private final Message message;

    /**
     * コンストラクタ。
     *
     * @param message メッセージ
     */
    public MessageComparedById(Message message) {
        super(null, null);
        this.message = message;
    }

    @Override
    public MessageLevel getLevel() {
        return message.getLevel();
    }

    @Override
    public String getMessageId() {
        return message.getMessageId();
    }

    /**
     * このオブジェクトと等価であるかを返す。
     *
     * <p>
     * {@code o}が以下の条件を全て満たす場合{@code true}を返す。
     * <ul>
     *     <li>{@link Message}もしくはそれを継承した型であること</li>
     *     <li>メッセージレベルが同値であること。</li>
     *     <li>メッセージIDが同値であること。</li>
     * </ul>
     *
     * @return このオブジェクトと等価である場合{@code true}
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;   // 同一インスタンス
        }
        if (!(o instanceof Message)) {
            return false;   // クラスが異なる
        }

        Message another = (Message) o;
        if (message.getLevel() != another.getLevel()) {
            return false;   // レベルが異なる
        }

        return message.getMessageId().equals(another.getMessageId());
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
     * @return メッセージIDとエラーレベルを記載した文字列
     */
    @Override
    public String toString() {
        return concat(
                "messageId=[", message.getMessageId(), "] ",
                "errorLevel=[", message.getLevel(), "]");
    }
}
