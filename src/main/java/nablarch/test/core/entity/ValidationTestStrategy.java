package nablarch.test.core.entity;

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
     * テストシートからBean Validationのグループを検索する。
     *
     * @param entityClass テスト対象クラス名
     * @param sheetName シート名
     * @param packageKey BeanValidationのグループが属するパッケージ名のキー
     * @param groupName BeanValidationのグループのクラス名
     * @return Bean Validationのグループ
     */
    Class<?> getGroupFromTestSheet(Class<?> entityClass, String sheetName, String packageKey, String groupName);
}
