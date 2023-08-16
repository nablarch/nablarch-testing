package nablarch.test.core.entity;

import nablarch.core.message.Message;
import nablarch.core.repository.SystemRepository;
import nablarch.core.validation.ValidationContext;
import nablarch.core.validation.ValidationManager;
import nablarch.core.validation.ValidationUtil;
import nablarch.test.Assertion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nablarch.core.util.Builder.concat;

public class NablarchValidationTestStrategy implements ValidationTestStrategy {

    private final Assertion.EquivCondition<Message, Message> condition;

    public NablarchValidationTestStrategy() {
        condition = new AsMessageID();
    }

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
            throw new RuntimeException(concat("unexpected exception occurred. ",
                    "target entity=[", entityClass.getName(), "] property=[", targetPropertyName, "] parameter=[", paramValues, "]"
            ), e);
        }

        return new ValidationTestContext(ctx.getMessages());
    }

    @Override
    public ValidationTestContext validateParameters(String prefix, Class<?> entityClass, Class<?> notUse, String validateFor, Map<String, String[]> params) {
            ValidationContext<?> ctx =
                    ValidationUtil.validateAndConvertRequest(prefix, entityClass, params, validateFor);
        return new ValidationTestContext(ctx.getMessages());
    }

    /**
     * {@inheritDoc}
     * Nablarch Validationではグループを使用しないため、常にnullを返却する。
     */
    @Override
    public Class<?> getGroupFromTestCase(String groupKey, List<Map<String, String>> groupListMap) {
        return null;
    }

    /**
     * {@inheritDoc}
     * Nablarch Validationでは、メッセージIDが同一であるかを検証する。
     */
    @Override
    public Assertion.EquivCondition<Message, Message> getEquivCondition() {
        return condition;
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
    /**
     * {@link Message}のメッセージIDが同一であるか判定する{@link Assertion.EquivCondition}実装クラス。
     */
    private static class AsMessageID implements Assertion.EquivCondition<Message, Message> {

        /** {@inheritDoc} */
        public boolean isEquivalent(Message expected, Message actual) {
            final String expectedMessageId = expected.getMessageId();
            final String actualMessageId = actual.getMessageId();
            return expectedMessageId.equals(actualMessageId);
        }
    }
}
