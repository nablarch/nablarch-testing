package nablarch.test.core.entity;

import nablarch.core.message.Message;
import nablarch.test.Assertion;

import java.util.List;
import java.util.Map;

/**
 * Form/Entityテストのストラテジ。
 * 実行するバリデーションの種類毎に実装クラスを作成する。
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
    ValidationTestContext invokeValidation(Class<?> entityClass, String targetPropertyName, Class<?> group, String[] paramValues);

    /**
     * 入力全体のバリデーションを実行する。
     *
     * @param entityClass テスト対象対象クラス名
     * @param group Bean Validationのグループ（Bean Validationを使用するときのみ有効）
     * @param params 入力値を表すマップ
     * @return テスト用バリデーションコンテキスト
     */
    ValidationTestContext validateParameters(String prefix, Class<?> entityClass, Class<?> group, String validateFor, Map<String, String[]> params);

    /**
     * Bean Validationのグループを検索し、取得する。
     *
     * {@code groupListMap}は、テストシートより{@code groupKey}をキーとして取得したList-Mapでなければならない。
     *
     * @param groupKey グループ情報のList-Map名
     * @param groupListMap グループ情報のList-Map内容
     * @return Bean Validationのグループ
     */
    Class<?> getGroupFromTestCase(String groupKey, List<Map<String, String>> groupListMap);

    /**
     * {@link Message}の同一性条件を返却する。
     * @return メッセージの同一性条件（{@link Assertion.EquivCondition})
     */
    Assertion.EquivCondition<Message, Message> getEquivCondition() ;

}
