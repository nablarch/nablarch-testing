package nablarch.test.core.entity;

import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.StringResource;
import nablarch.core.validation.ValidationResultMessage;

import java.util.Map;

/**
 * Form/Entityテストで使用するバリデーション毎のストラテジ。
 */
public interface ValidationTestStrategy {

    /**
     * 単一のプロパティについて、バリデーションを実行する。
     *
     * @param entityClass テスト対象対象クラス名
     * @param targetPropertyName バリデーション対象プロパティ名
     * @param group Bean Validationのグループ（Bean Validationを使用するときのみ有効）
     * @param paramValues パラメータとして使用する値
     * @return テスト用バリデーションコンテキスト
     */
    ValidationTestContext invokeValidation(Class<?> entityClass, String targetPropertyName, String[] paramValues, Class<?> group);

    /**
     * 入力全体のバリデーションを実行する。
     *
     * @param entityClass テスト対象対象クラス名
     * @param group Bean Validationのグループ（Bean Validationを使用するときのみ有効）
     * @param params 入力値を表すマップ
     * @return テスト用バリデーションコンテキスト
     */
    ValidationTestContext validateParameters(String prefix, Class<?> entityClass, Map<String, String[]> params, String validateFor, Class<?> group);

    /**
     * Bean Validationのグループを取得する。
     *
     * @param groupName グループ名
     * @return Bean Validationのグループ
     */
    Class<?> getGroupFromName(String groupName);

    /**
     * メッセージ比較用の{@link ValidationResultMessage}を生成する。
     *
     * @param propertyName  プロパティ名
     * @param messageString メッセージを特定する文字列
     * @param options       オプションパラメータ
     * @return {@link ValidationResultMessage}
     */
    Message createExpectedValidationResultMessage(String propertyName, String messageString, Object[] options);

    /**
     * メッセージ比較用の{@link Message}を生成する。
     *
     * @param level         メッセージレベル
     * @param messageString メッセージを特定する文字列
     * @param options       オプションパラメータ
     * @return {@link Message}
     */
    Message createExpectedMessage(MessageLevel level, String messageString, Object[] options);
}
