package nablarch.test.core.entity;

import nablarch.core.repository.SystemRepository;
import nablarch.core.util.Builder;
import nablarch.core.validation.ValidationContext;
import nablarch.core.validation.ValidationManager;
import nablarch.core.validation.ValidationUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NablarchValidationTestStrategy implements ValidationTestStrategy {

    @Override
    public ValidationTestContext invokeValidation(Class<?> entityClass, String targetPropertyName, Class<?> notUse, String[] paramValues) {
        // 入力値（1項目分のみ）
        Map<String, String[]> params = new HashMap<String, String[]>(1);
        params.put(targetPropertyName, paramValues);
        // バリデーション実行
        ValidationContext<?> ctx
                = getValidationManager().createValidationContext(
                entityClass, params, "", null);
        try {
            ValidationUtil.validate(ctx, new String[]{targetPropertyName});
        } catch (RuntimeException e) {
            throw new RuntimeException(Builder.concat("unexpected exception occurred. ",
                    "target entity=[", entityClass.getName(), "] property=[", targetPropertyName, "] parameter=[", paramValues, "]"
            ), e);
        }

        return new ValidationTestContext(ctx.getMessages());
    }

    /**
     * {@inheritDoc}
     * Nablarch Validationではグループを使用しないため、常にnullを返却する。
     */
    @Override
    public Class<?> getGroupFromTestCase(String groupName, List<Map<String, String>> packageListMap) {
        return null;
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
