package nablarch.test.core.entity;

import nablarch.common.web.validator.BeanValidationFormFactory;
import nablarch.common.web.validator.SimpleReflectionBeanValidationFormFactory;
import nablarch.core.beans.BeanUtil;
import nablarch.core.beans.CopyOptions;
import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.StringResource;
import nablarch.core.util.StringUtil;
import nablarch.core.validation.ValidationResultMessage;
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
    public ValidationTestContext invokeValidation(Class<?> entityClass, String targetPropertyName, String[] paramValues, Class<?> group) {
        assert(null != group);

        // 入力値（1項目分のみ）
        Map<String, String[]> params = new HashMap<String, String[]>(1);
        params.put(targetPropertyName, paramValues);

        Object bean = formFactory.create(entityClass);
        BeanUtil.copy(entityClass, bean, params, CopyOptions.empty());

        Set<ConstraintViolation<Object>> result =
                ValidatorUtil.getValidator().validateProperty(bean, targetPropertyName, group);

        List<Message> messages = new ConstraintViolationConverterFactory().create().convert(result);

        return new ValidationTestContext(messages);
    }

    @Override
    public ValidationTestContext validateParameters(String prefix, Class<?> entityClass, Map<String, String[]> params, String notUse, Class<?> group) {
        assert(null != group);

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
                ValidatorUtil.getValidator().validate(bean, group);

        List<Message> messages = new ConstraintViolationConverterFactory().create().convert(result);

        return new ValidationTestContext(messages);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getGroupFromName(String groupName) {

        // groupNameが空なら、グループ情報が指定されなかったということなのでDefaultを返却する。
        if (StringUtil.isNullOrEmpty(groupName)) {
            return Default.class;
        }

        try {
            return Class.forName(groupName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Non-existent class is specified for bean validation group. Specified FQCN is " + groupName);
        }
    }

    /**
     * {@inheritDoc}
     * Bean Validationで{@link ValidationResultMessage}を検証する際は、メッセージ本文を比較する。
     */
    @Override
    public Message createExpectedValidationResultMessage(String propertyName, StringResource stringResource, Object[] options) {
        return new BeanValidationResultMessage(propertyName, stringResource, options);
    }

    /**
     * {@inheritDoc}
     * Bean Validationで{@link Message}を検証する際は、メッセージ本文を比較する。
     */
    @Override
    public Message createExpectedMessage(MessageLevel level, StringResource stringResource, Object[] options) {
        return new MessageComparedByContent(level, stringResource, options);
    }

    /** フォームファクトリ。 */
    private final BeanValidationFormFactory formFactory = new SimpleReflectionBeanValidationFormFactory();
}
