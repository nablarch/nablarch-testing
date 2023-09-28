package nablarch.test.core.entity;


import javax.validation.ConstraintTarget;
import javax.validation.ConstraintValidator;
import javax.validation.MessageInterpolator;
import javax.validation.Payload;
import javax.validation.metadata.ConstraintDescriptor;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bean Validationの補完用属性を保持する{@link MessageInterpolator.Context}の実装クラス。
 */
public class MockMessageInterpolatorContext implements MessageInterpolator.Context {

    /**
     * 制約記述子。
     */
    private final ConstraintDescriptor<?> constraintDescriptor;

    /**
     * サポート外メソッドを呼び出したときのエラーメッセージ。
     */
    private static final String UNSUPPORTED_METHOD_WAS_CALLED = "Unsupported method was called.";

    /**
     * コンストラクタ。
     *
     * @param interpolationMap 補完用属性のマップ
     */
    public MockMessageInterpolatorContext(Map<String, Object> interpolationMap){
        this.constraintDescriptor = new MockConstraintDescriptor<Annotation>(interpolationMap);
    }

    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return constraintDescriptor;
    }

    @Override
    public Object getValidatedValue() {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD_WAS_CALLED);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD_WAS_CALLED);
    }

    /**
     * 補完用属性を保持する{@link ConstraintDescriptor}の実装クラス。
     *
     * @param <T> 制約アノテーションの型
     */
    private static class MockConstraintDescriptor<T extends Annotation> implements ConstraintDescriptor<T> {

        /**
         * 補完用属性のマップ。
         */
        private final Map<String, Object> interpolateMap;

        /**
         * コンストラクタ。
         *
         * @param interpolateMap 補完用属性のマップ
         */
        public MockConstraintDescriptor(Map<String, Object> interpolateMap) {
            this.interpolateMap = interpolateMap;
        }

        @Override
        public T getAnnotation() {
            throw new UnsupportedOperationException(UNSUPPORTED_METHOD_WAS_CALLED);
        }

        @Override
        public String getMessageTemplate() {
            throw new UnsupportedOperationException(UNSUPPORTED_METHOD_WAS_CALLED);
        }

        @Override
        public Set<Class<?>> getGroups() {
            throw new UnsupportedOperationException(UNSUPPORTED_METHOD_WAS_CALLED);
        }

        @Override
        public Set<Class<? extends Payload>> getPayload() {
            throw new UnsupportedOperationException(UNSUPPORTED_METHOD_WAS_CALLED);
        }

        @Override
        public ConstraintTarget getValidationAppliesTo() {
            throw new UnsupportedOperationException(UNSUPPORTED_METHOD_WAS_CALLED);
        }

        @Override
        public List<Class<? extends ConstraintValidator<T, ?>>> getConstraintValidatorClasses() {
            throw new UnsupportedOperationException(UNSUPPORTED_METHOD_WAS_CALLED);
        }

        @Override
        public Map<String, Object> getAttributes() {
            return interpolateMap;
        }

        @Override
        public Set<ConstraintDescriptor<?>> getComposingConstraints() {
            throw new UnsupportedOperationException(UNSUPPORTED_METHOD_WAS_CALLED);
        }

        @Override
        public boolean isReportAsSingleViolation() {
            throw new UnsupportedOperationException(UNSUPPORTED_METHOD_WAS_CALLED);
        }
    }
}
