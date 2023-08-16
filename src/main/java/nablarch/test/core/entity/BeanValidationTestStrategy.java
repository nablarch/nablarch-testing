package nablarch.test.core.entity;

import nablarch.common.web.validator.BeanValidationFormFactory;
import nablarch.common.web.validator.SimpleReflectionBeanValidationFormFactory;
import nablarch.core.beans.BeanUtil;
import nablarch.core.beans.CopyOptions;
import nablarch.core.message.Message;
import nablarch.core.util.StringUtil;
import nablarch.core.validation.ee.ConstraintViolationConverterFactory;
import nablarch.core.validation.ee.ValidatorUtil;
import nablarch.test.Assertion;

import javax.validation.ConstraintViolation;
import javax.validation.groups.Default;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeanValidationTestStrategy implements ValidationTestStrategy{

    private final Assertion.EquivCondition<Message, Message> condition;

    public BeanValidationTestStrategy() {
        condition = new AsMessageContent();
    }

    @Override
    public ValidationTestContext invokeValidation(Class<?> entityClass, String targetPropertyName, Class<?> group, String[] paramValues) {

        // 入力値（1項目分のみ）
        Map<String, String[]> params = new HashMap<String, String[]>(1);
        params.put(targetPropertyName, paramValues);

        Object bean = formFactory.create(entityClass);
        BeanUtil.copy(entityClass, bean, params, CopyOptions.empty());

        Set<ConstraintViolation<Object>> result =
                ValidatorUtil.getValidator().validateProperty(bean, targetPropertyName, group != null ? group : Default.class);

        List<Message> messages = new ConstraintViolationConverterFactory().create().convert(result);

        return new ValidationTestContext(messages);
    }

    @Override
    public ValidationTestContext validateParameters(String prefix, Class<?> entityClass, Class<?> group, String notUse, Map<String, String[]> params) {
        // 入力値 (キーがprefixから始まるもののみ)
        Map<String, String[]> convertedParams;

        if(StringUtil.isNullOrEmpty(prefix)) {
            convertedParams = params;
        } else {
            String innerPrefix = prefix + ".";
            convertedParams = new HashMap<String, String[]>();
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                if (entry.getKey().startsWith(innerPrefix)) {
                    convertedParams.put(entry.getKey().substring(innerPrefix.length()), entry.getValue());
                }
            }
        }

        Object bean = formFactory.create(entityClass);
        BeanUtil.copy(entityClass, bean, convertedParams, CopyOptions.empty());

        Set<ConstraintViolation<Object>> result =
                ValidatorUtil.getValidator().validate(bean, group != null ? group : Default.class);

        List<Message> messages = new ConstraintViolationConverterFactory().create().convert(result);

        return new ValidationTestContext(messages);

    }

    /** BeanValidationのグループが属するパッケージ名のキー */
    private static final String GROUP_NAME = "GROUP_NAME";

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getGroupFromTestCase(String groupKey, List<Map<String, String>> groupListMap) {

        if (!StringUtil.isNullOrEmpty(groupKey) && groupListMap.isEmpty()) {
            throw new IllegalArgumentException("Group LIST_MAP was not found. name = [" + groupKey + "]");
        }

        // groupKeyが空なら、グループ情報が指定されなかったということなのでnullを返却する。
        if (StringUtil.isNullOrEmpty(groupKey)) {
            return null;
        }

        // 上記以外の場合は、必ずgroupListMapは空ではない。
        Map<String, String> packageMap = groupListMap.get(0);
        if(!packageMap.containsKey(GROUP_NAME)) {
            throw new IllegalArgumentException("GROUP_NAME is required for the group name LIST_MAP.");
        }

        String FQCN = packageMap.get(GROUP_NAME);
        try {
            return Class.forName(FQCN);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Non-existent class is specified for bean validation group. Specified FQCN is " + FQCN);
        }
    }

    /**
     * {@inheritDoc}
     * Bean ValidationではメッセージIDによる同一性の確認ができないため、メッセージ本文が同一であるかを検証する。
     */
    @Override
    public Assertion.EquivCondition<Message, Message> getEquivCondition() {
        return condition;
    }

    /** フォームファクトリ。 */
    private final BeanValidationFormFactory formFactory = new SimpleReflectionBeanValidationFormFactory();

    /**
     * {@link Message}のメッセージ本文が同一であるか判定する{@link Assertion.EquivCondition}実装クラス。
     */
    private static class AsMessageContent implements Assertion.EquivCondition<Message, Message> {

        /** {@inheritDoc} */
        public boolean isEquivalent(Message expected, Message actual) {
            final String expectedContent = expected.formatMessage();
            final String actualContent = actual.formatMessage();
            return expectedContent.equals(actualContent);
        }
    }
}
