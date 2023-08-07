package nablarch.test.core.entity;

import nablarch.core.message.Message;

import java.util.List;
import java.util.Map;

/**
 * Form/Entityテストのストラテジ。
 * 実行するバリデーションの種類毎に実装クラスを作成する。
 */
public interface ValidationTestStrategy {

    /**
     * バリデーションを実行する。
     *
     * @param entityClass テスト対象対象クラス名
     * @param targetPropertyName バリデーション対象プロパティ名
     * @param group Bean Validationのグループ（Bean Validationを使用するときのみ有効）
     * @param paramValues パラメータとして使用する値
     * @return テスト用バリデーションコンテキスト
     */
    ValidationTestContext invokeValidation(Class<?> entityClass, String targetPropertyName, Class<?> group, String[] paramValues);

    /**
     * Bean Validationのグループを検索し、取得する。
     *
     * @param groupName BeanValidationのグループのクラス名
     * @param packageListMap パッケージ情報のList-Map
     * @return Bean Validationのグループ
     */
    Class<?> getGroupFromTestCase(String groupName, List<Map<String, String>> packageListMap);

    /**
     * 実際のメッセージが期待するメッセージと等しいかを検証する。
     *
     * @param msgOnFail
     * @param expectedMessageId
     * @param actualMessage
     */
    void assertMessageEquals(String msgOnFail, String expectedMessageId, Message actualMessage);
}
