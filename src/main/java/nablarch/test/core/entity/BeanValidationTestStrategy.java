package nablarch.test.core.entity;

import nablarch.common.web.validator.BeanValidationFormFactory;
import nablarch.common.web.validator.SimpleReflectionBeanValidationFormFactory;
import nablarch.core.beans.BeanUtil;
import nablarch.core.beans.CopyOptions;
import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.MessageUtil;
import nablarch.core.util.StringUtil;
import nablarch.core.validation.ee.ConstraintViolationConverterFactory;
import nablarch.core.validation.ee.ValidatorUtil;

import javax.validation.ConstraintViolation;
import javax.validation.groups.Default;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class BeanValidationTestStrategy implements ValidationTestStrategy{

    @Override
    public ValidationTestContext invokeValidation(Class<?> entityClass, String targetPropertyName, Class<?> group, String[] paramValues) {

        // 入力値（1項目分のみ）
        Map<String, String[]> params = new HashMap<String, String[]>(1);
        params.put(targetPropertyName, paramValues);

        Object bean = formFactory.create(entityClass);
        BeanUtil.copy(entityClass, bean, params, CopyOptions.empty());

        Set<ConstraintViolation<Object>> result =
                ValidatorUtil.getValidator().validateProperty(bean, targetPropertyName, group != null ? group : Default.class);

        List<Message> messages = new ConstraintViolationConverterFactory().create("").convert(result);

        return new ValidationTestContext(messages);
    }

    /** BeanValidationのグループが属するパッケージ名のキー */
    private static final String PACKAGE_NAME = "PACKAGE_NAME";

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getGroupFromTestCase(String groupName, List<Map<String, String>> packageListMap) {

        if ((!StringUtil.isNullOrEmpty(groupName) && packageListMap.isEmpty())
                || (StringUtil.isNullOrEmpty(groupName) && !packageListMap.isEmpty())) {
            throw new IllegalArgumentException("Both the groupName and packageKey must be specified. Otherwise, both must be unspecified.");
        }

        // 両方空ならnullを返す
        if (packageListMap.isEmpty()) {
            return null;
        }

        Map<String, String> packageMap = packageListMap.get(0);
        if(!packageMap.containsKey(PACKAGE_NAME)) {
            throw new IllegalArgumentException("PACKAGE_NAME is required for the package name LIST_MAP.");
        }

        String FQCN = packageMap.get(PACKAGE_NAME) + "." + groupName;
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
    public void assertMessageEquals(String msgOnFail, String expectedMessageId, Message actualMessage) {
        String expectedFormatMessage = MessageUtil.createMessage(MessageLevel.ERROR, expectedMessageId).formatMessage();
        String actualFormatMessage = actualMessage.formatMessage();

        assertEquals(msgOnFail, expectedFormatMessage, actualFormatMessage);
    }

    /** フォームファクトリ。 */
    private final BeanValidationFormFactory formFactory = new SimpleReflectionBeanValidationFormFactory();

}
