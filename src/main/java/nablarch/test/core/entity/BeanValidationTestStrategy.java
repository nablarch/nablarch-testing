package nablarch.test.core.entity;

import nablarch.common.web.validator.BeanValidationFormFactory;
import nablarch.common.web.validator.SimpleReflectionBeanValidationFormFactory;
import nablarch.core.beans.BeanUtil;
import nablarch.core.beans.CopyOptions;
import nablarch.core.util.StringUtil;
import nablarch.core.validation.ee.ConstraintViolationConverterFactory;
import nablarch.core.validation.ee.ValidatorUtil;
import nablarch.test.TestSupport;

import javax.validation.Constraint;
import javax.validation.ConstraintViolation;
import javax.validation.groups.Default;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeanValidationTestStrategy implements ValidationTestStrategy{

    @Override
    public ValidationTestContext invokeValidation(Class<?> entityClass, String targetPropertyName, Class<?> group, String[] paramValues) {

        // 入力値（1項目分のみ）
        Map<String, String[]> params = new HashMap<String, String[]>(1);
        params.put(targetPropertyName, paramValues);

        Object bean = formFactory.create(entityClass);
        BeanUtil.copy(entityClass, bean, params, CopyOptions.empty());

        Set<ConstraintViolation<Object>> result =
                ValidatorUtil.getValidator().validate(bean, group != null ? group : Default.class);

        return new ValidationTestContext(
                new ConstraintViolationConverterFactory()
                        .create("")
                        .convert(result));
    }


    private static final String PACKAGE_LIST_MAP_ID = "packages";

    @Override
    public Class<?> getGroupFromTestSheet(Class<?> entityClass, String sheetName, String packageKey, String groupName) {
        if (StringUtil.isNullOrEmpty(groupName) || StringUtil.isNullOrEmpty(packageKey)) {
            return null;
        }

        Map<String, String> packageNameMap = getListMapRequired(entityClass, sheetName, PACKAGE_LIST_MAP_ID).get(0);
        if(!packageNameMap.containsKey(packageKey)) {
            throw new IllegalArgumentException("");
        }

        String packageName = packageNameMap.get(packageKey);
        Class<?> group;
        try {
            group = Class.forName(packageName + "." + groupName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return group;
    }

    /**
     * 必須のList-Mapデータを取得する。<br/>
     * 結果が空の場合は指定したIDに合致するデータが存在しないとみなし、例外を発生させる。
     *
     * @param entityClass テスト対象クラス名
     * @param sheetName シート名
     * @param id        ID
     * @return List-Map形式のデータ
     * @throws IllegalArgumentException 指定したIDのデータが存在しない場合
     */
    private List<Map<String, String>> getListMapRequired(Class<?> entityClass, String sheetName, String id) {
        List<Map<String, String>> ret = new TestSupport(entityClass).getListMap(sheetName, id);
        if (ret.isEmpty()) {
            throw new IllegalArgumentException("data [" + id + "] not found in sheet [" + sheetName + "].");
        }
        return ret;
    }

    /** フォームファクトリ。 */
    private BeanValidationFormFactory formFactory = new SimpleReflectionBeanValidationFormFactory();

}
