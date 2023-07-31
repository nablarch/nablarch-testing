package nablarch.test.core.entity;

import java.util.Map;

public interface ValidationTestStrategy {

    ValidationTestContext invokeValidation(Class<?> entityClass, String targetPropertyName, String[] paramValues);

    ValidationTestContext validate(String prefix, Class<?> targetClass, Map<String, ?> params, String validateFor);
}
