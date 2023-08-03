package nablarch.test.core.entity;

import nablarch.common.web.validator.BeanValidationFormFactory;
import nablarch.common.web.validator.SimpleReflectionBeanValidationFormFactory;
import nablarch.core.beans.BeanUtil;
import nablarch.core.beans.CopyOptions;
import nablarch.core.util.StringUtil;
import nablarch.core.validation.ee.ConstraintViolationConverterFactory;
import nablarch.core.validation.ee.ValidatorUtil;

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

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getGroupFromTestCase(String packageKey, String groupName, List<Map<String, String>> packageListMap) {

        if (StringUtil.isNullOrEmpty(groupName) || StringUtil.isNullOrEmpty(packageKey) || packageListMap.isEmpty()) {
            return null;
        }

        Map<String, String> packageMap = packageListMap.get(0);
        if(!packageMap.containsKey(packageKey)) {
            throw new IllegalArgumentException("not contained.");
        }

        String packageName = packageMap.get(packageKey);
        Class<?> group;
        try {
            group = Class.forName(packageName + "." + groupName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return group;
    }

    /** フォームファクトリ。 */
    private BeanValidationFormFactory formFactory = new SimpleReflectionBeanValidationFormFactory();

}
