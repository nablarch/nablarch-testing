package nablarch.test.core.entity;

import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.StringResource;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.StringUtil;
import nablarch.core.validation.ValidationContext;
import nablarch.core.validation.ValidationManager;
import nablarch.core.validation.ValidationResultMessage;
import nablarch.core.validation.ValidationUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static nablarch.core.util.Builder.concat;

/**
 * Nablarch Validationを使用するときの{@link ValidationTestStrategy}実装クラス。
 */
public class NablarchValidationTestStrategy implements ValidationTestStrategy {

    /** {@link ValidationManager}を取得する為のキー */
    private static final String VALIDATION_MANAGER_NAME = "validationManager";

    @Override
    public ValidationTestContext invokeValidation(Class<?> entityClass, String targetPropertyName, String[] paramValues, Class<?> notUse) {
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

        List<Message> convertedMessages = new ArrayList<Message>();
        for(Message message : ctx.getMessages()) {
            convertedMessages.add(new MessageComparedById(message));
        }
        return new ValidationTestContext(convertedMessages);
    }

    @Override
    public ValidationTestContext validateParameters(String prefix, Class<?> entityClass, Map<String, String[]> params, String validateFor, Class<?> notUse) {
            ValidationContext<?> ctx =
                    ValidationUtil.validateAndConvertRequest(prefix, entityClass, params, validateFor);
        return new ValidationTestContext(ctx.getMessages());
    }

    /**
     * {@inheritDoc}
     * Nablarch Validationではグループを使用しないため、常にnullを返却する。
     */
    @Override
    public Class<?> getGroupFromName(String groupKey) {
        return null;
    }

    @Override
    public Message createExpectedValidationResultMessage(String propertyName, String messageString, Object[] options) {
        if(StringUtil.isNullOrEmpty(messageString)) {
            throw new IllegalArgumentException("messageString must be neither null nor empty.");
        }
        StringResource stringResource = new MockStringResource(messageString);
        return new ValidationResultMessage(propertyName, stringResource, options);
    }

    @Override
    public Message createExpectedMessage(MessageLevel level, String messageString, Object[] options) {
        if(StringUtil.isNullOrEmpty(messageString)) {
            throw new IllegalArgumentException("messageString must be neither null nor empty.");
        }
        StringResource stringResource = new MockStringResource(messageString);
        return new MessageComparedById(new Message(level, stringResource, options));
    }

    /**
     * {@link ValidationManager}を取得する。
     *
     * @return {@link ValidationManager}
     */
    private static ValidationManager getValidationManager() {
        ValidationManager validationManager = SystemRepository.get(VALIDATION_MANAGER_NAME);
        if (validationManager == null) {
            throw new IllegalStateException("can't get ValidationManager instance from System Repository."
                    + "check configuration. key=[" + VALIDATION_MANAGER_NAME + "]");
        }
        return validationManager;
    }

    /**
     * メッセージ構築用モックの{@link StringResource}。
     */
    private static class MockStringResource implements StringResource {

        private final String id;

        public MockStringResource(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getValue(Locale locale) {
            throw new UnsupportedOperationException();
        }
    }
}
