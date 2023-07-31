package nablarch.test.core.entity;

import nablarch.common.web.validator.BeanValidationFormFactory;
import nablarch.common.web.validator.SimpleReflectionBeanValidationFormFactory;
import nablarch.core.beans.BeanUtil;
import nablarch.core.beans.CopyOptions;
import nablarch.core.validation.ee.ConstraintViolationConverterFactory;
import nablarch.core.validation.ee.ValidatorUtil;

import javax.jms.Message;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeanValidationTestStrategy implements ValidationTestStrategy{


    /**
     * フォームファクトリ。
     * （デフォルトでは単純にリフレクションでインスタンスを生成する）
     */
    private BeanValidationFormFactory formFactory = new SimpleReflectionBeanValidationFormFactory();

    @Override
    public ValidationTestContext invokeValidation(Class<?> entityClass, String targetPropertyName, String[] paramValues) {

        // 入力値（1項目分のみ）
        Map<String, String[]> params = new HashMap<String, String[]>(1);
        params.put(targetPropertyName, paramValues);

        Object bean = formFactory.create(entityClass);
        BeanUtil.copy(entityClass, bean, params, CopyOptions.empty());
        Validator validator = ValidatorUtil.getValidator();
        Set<ConstraintViolation<Object>> results = validator.validate(bean);
        if (!results.isEmpty()) {
            List<Message> messages = new ConstraintViolationConverterFactory().create(annotation.prefix()).convert(results);
            throw new ApplicationException(sortMessages(messages, context, annotation));
        }

        return null;
    }

    @Override
    public ValidationTestContext validate(String prefix, Class<?> targetClass, Map<String, ?> params, String validateFor) {
        return null;
    }
}
