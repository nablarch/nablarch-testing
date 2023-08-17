package nablarch.test.core.entity;

import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.StringResource;

/**
 * {@link Message}同士の比較をメッセージIDで実行するためのクラス。
 * {@link Message}を継承したクラスと比較する際に、メッセージIDとエラーレベルのみで比較するために使用する。
 */
public class MessageComparedById extends Message {

    /**
     * コンストラクタ。
     * スーパークラスのコンストラクタを呼び出すのみ。
     * @param level          メッセージレベル
     * @param stringResource メッセージ
     * @param option         オプションパラメータ
     */
    public MessageComparedById(MessageLevel level, StringResource stringResource, Object[] option) {
        super(level, stringResource, option);
    }

    /**
     * このオブジェクトと等価であるかを返す。
     * <p/>
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
        if (this.getLevel() != another.getLevel()) {
            return false;   // レベルが異なる
        }

        return this.getMessageId().equals(another.getMessageId());
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
