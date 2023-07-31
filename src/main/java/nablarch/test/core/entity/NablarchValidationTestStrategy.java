package nablarch.test.core.entity;

import nablarch.core.repository.SystemRepository;
import nablarch.core.util.Builder;
import nablarch.core.validation.ValidationContext;
import nablarch.core.validation.ValidationManager;
import nablarch.core.validation.ValidationUtil;

import java.util.HashMap;
import java.util.Map;

public class NablarchValidationTestStrategy implements ValidationTestStrategy {

    @Override
    public ValidationTestContext invokeValidation(Class<?> entityClass, String targetPropertyName, String[] paramValue) {
        // 入力値（1項目分のみ）
        Map<String, String[]> params = new HashMap<String, String[]>(1);
        params.put(targetPropertyName, paramValue);
        // バリデーション実行
        ValidationContext<?> ctx
                = getValidationManager().createValidationContext(
                entityClass, params, "", null);
        try {
            ValidationUtil.validate(ctx, new String[]{targetPropertyName});
        } catch (RuntimeException e) {
            throw new RuntimeException(Builder.concat(
                    "unexpected exception occurred. ", toString(),
                    " parameter=[", paramValue, "]"), e);
        }

        return new ValidationTestContext(ctx.getMessages());
    }

    @Override
    public ValidationTestContext validate(String prefix, Class<?> targetClass, Map<String, ?> params, String validateFor) {
        ValidationContext<?> ctx =
                ValidationUtil.validateAndConvertRequest(prefix, targetClass, params, validateFor);

        return new ValidationTestContext(ctx.getMessages());
    }

    /** {@link ValidationManager}を取得する為のキー */
    private static final String VALIDATION_MANAGER_NAME = "validationManager";

    private static ValidationManager getValidationManager() {
        ValidationManager validationManager = SystemRepository.get(VALIDATION_MANAGER_NAME);
        if (validationManager == null) {
            throw new IllegalStateException("can't get ValidationManager instance from System Repository."
                    + "check configuration. key=[" + VALIDATION_MANAGER_NAME + "]");
        }
        return validationManager;
    }


}
