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

public class MockMessageInterpolatorContext implements MessageInterpolator.Context {

    private final ConstraintDescriptor<?> constraintDescriptor;

    public MockMessageInterpolatorContext(Map<String, Object> interpolateMap){
        this.constraintDescriptor = new MockConstraintDescriptor<Annotation>(interpolateMap);
    }

    @Override
    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return constraintDescriptor;
    }

    @Override
    public Object getValidatedValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        throw new UnsupportedOperationException();
    }

    private class MockConstraintDescriptor<T extends Annotation> implements ConstraintDescriptor<T> {

        private final Map<String, Object> interpolateMap;

        public MockConstraintDescriptor(Map<String, Object> interpolateMap) {
            this.interpolateMap = interpolateMap;
        }

        @Override
        public T getAnnotation() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getMessageTemplate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Class<?>> getGroups() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Class<? extends Payload>> getPayload() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConstraintTarget getValidationAppliesTo() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Class<? extends ConstraintValidator<T, ?>>> getConstraintValidatorClasses() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return interpolateMap;
        }

        @Override
        public Set<ConstraintDescriptor<?>> getComposingConstraints() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isReportAsSingleViolation() {
            throw new UnsupportedOperationException();
        }
    }
}
