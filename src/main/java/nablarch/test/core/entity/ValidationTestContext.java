package nablarch.test.core.entity;

import nablarch.core.message.Message;

import java.util.List;

/**
 * Form/Entityテスト実施時のコンテキスト情報を保持するクラス。
 */
public class ValidationTestContext {

    /** バリデーションエラーのメッセージ。 */
    private final List<Message> messages;

    /**
     * コンストラクタ。
     *
     * @param messages バリデーションエラーのメッセージ
     */
    public ValidationTestContext(List<Message> messages) {
        this.messages = messages;
    }

    /**
     * バリデーション結果が正当であるかを返却する。
     *
     * @return {@code true} バリデーション結果が正当である場合
     */
    public boolean isValid() {
        return messages.isEmpty();
    }

    /**
     * バリデーションエラーのメッセージを返却する。
     *
     * @return バリデーションエラーのメッセージ
     */
    public List<Message> getMessages() {
        return messages;
    }
}
