package nablarch.test.core.entity;

import nablarch.common.web.validator.BeanValidationFormFactory;
import nablarch.common.web.validator.SimpleReflectionBeanValidationFormFactory;
import nablarch.core.beans.BeanUtil;
import nablarch.core.beans.CopyOptions;
import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.MessageUtil;
import nablarch.core.message.StringResource;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.StringUtil;
import nablarch.core.validation.ValidationResultMessage;
import nablarch.core.validation.ee.ConstraintViolationConverterFactory;
import nablarch.core.validation.ee.NablarchMessageInterpolator;
import nablarch.core.validation.ee.ValidatorUtil;

import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.groups.Default;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bean Validationを使用するときの{@link ValidationTestStrategy}実装クラス。
 */
public class BeanValidationTestStrategy implements ValidationTestStrategy{

    /** フォームファクトリ。 */
    private final BeanValidationFormFactory formFactory = new SimpleReflectionBeanValidationFormFactory();

    /**
     * デフォルトの言語。
     */
    private static final Locale DEFAULT_LOCALE = new Locale(Locale.getDefault().getLanguage());

    /**
     * メッセージIDのパターン。
     */
    private static final Pattern MESSAGE_ID = Pattern.compile("\\{(.+)\\}");

    @Override
    public ValidationTestContext invokeValidation(Class<?> entityClass, String targetPropertyName, String[] paramValues, Class<?> group) {
        // 入力値（1項目分のみ）
        Map<String, String[]> params = new HashMap<String, String[]>(1);
        params.put(targetPropertyName, paramValues);

        Object bean = formFactory.create(entityClass);
        BeanUtil.copy(entityClass, bean, params, CopyOptions.empty());

        Set<ConstraintViolation<Object>> result =
                ValidatorUtil.getValidator().validateProperty(bean, targetPropertyName, group);

        List<Message> messages = new ConstraintViolationConverterFactory().create().convert(result);

        List<Message> convertedMessages = new ArrayList<Message>();
        for(Message message : messages) {
            convertedMessages.add(new MessageComparedByContent(message));
        }
        return new ValidationTestContext(convertedMessages);
    }

    @Override
    public ValidationTestContext validateParameters(String prefix, Class<?> entityClass, Map<String, String[]> params, String notUse, Class<?> group) {
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

        List<Message> convertedMessages = new ArrayList<Message>();
        for(Message message : messages) {
            convertedMessages.add(new BeanValidationResultMessage((ValidationResultMessage) message));
        }

        return new ValidationTestContext(convertedMessages);
    }

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

    @Override
    public Message createExpectedValidationResultMessage(String propertyName, String messageString, Object[] options) {
        StringResource stringResource = getStringResource(messageString, options);
        return new BeanValidationResultMessage(new ValidationResultMessage(propertyName, stringResource, null));
    }

    @Override
    public Message createExpectedMessage(MessageLevel level, String messageString, Object[] options) {
        StringResource stringResource = getStringResource(messageString, options);
        return new MessageComparedByContent(new Message(level, stringResource));
    }

    /**
     * 期待するメッセージの{@link StringResource}を取得する。
     *
     * @param messageString メッセージを特定する文字列
     * @param options       Bean Validationのメッセージ補完用属性のリスト
     * @return              期待するメッセージの {@link StringResource}
     */
    private StringResource getStringResource(String messageString, Object[] options) {
        String messageContent = messageString;

        Matcher m = MESSAGE_ID.matcher(messageString);
        if (m.matches()) {
            messageContent = MessageUtil.getStringResource(m.group(1)).getValue(DEFAULT_LOCALE);
        }

        // オプションがMapの場合は、MessageInterpolatorによる補完を試みる。
        if(options != null && options.length == 1 && options[0] instanceof Map) {
            //noinspection unchecked
            Map<String, Object> interpolationMap = (Map<String, Object>) options[0];
            MessageInterpolator.Context context = new MockMessageInterpolatorContext(interpolationMap);
            MessageInterpolator interpolator = getMessageInterpolator();
            messageContent = interpolator.interpolate(messageContent, context, DEFAULT_LOCALE);
        }

        return new StringResourceMock(messageContent);
    }

    /**
     * {@link MessageInterpolator}を取得する。
     *
     * @return システムリポジトリに登録された {@link MessageInterpolator} 、なければ {@link NablarchMessageInterpolator}
     */
    private MessageInterpolator getMessageInterpolator() {
        MessageInterpolator messageInterpolator = SystemRepository.get("messageInterpolator");
        if (messageInterpolator == null) {
            messageInterpolator = new NablarchMessageInterpolator();
        }

        return messageInterpolator;
    }

    /**
     * メッセージ構築用モックの{@link StringResource}。
     */
    private static class StringResourceMock implements StringResource {

        private final String value;

        public StringResourceMock(String value) {
            this.value = value;
        }

        @Override
        public String getId() {
            return "dummy";
        }

        @Override
        public String getValue(Locale locale) {
            return value;
        }
    }
}
