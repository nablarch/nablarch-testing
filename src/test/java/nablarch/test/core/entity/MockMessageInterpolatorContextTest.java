package nablarch.test.core.entity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;

public class MockMessageInterpolatorContextTest {

    @SuppressWarnings("deprecation")
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final MockMessageInterpolatorContext sut = new MockMessageInterpolatorContext(new HashMap<String, Object>());

    private static final String EXPECTED_MSG = "Unsupported method was called.";

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetValidatedValue(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MSG);

        sut.getValidatedValue();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testUnwrap(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MSG);

        sut.unwrap(this.getClass());
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetAnnotation(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MSG);

        sut.getConstraintDescriptor().getAnnotation();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetMessageTemplate(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MSG);

        sut.getConstraintDescriptor().getMessageTemplate();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetGroup(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MSG);

        sut.getConstraintDescriptor().getGroups();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetPayload(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MSG);

        sut.getConstraintDescriptor().getPayload();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetValidationAppliesTo(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MSG);

        sut.getConstraintDescriptor().getValidationAppliesTo();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetConstraintValidatorClasses(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MSG);

        sut.getConstraintDescriptor().getConstraintValidatorClasses();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetComposingConstraints(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MSG);

        sut.getConstraintDescriptor().getComposingConstraints();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testIsReportAsSingleViolation(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MSG);

        sut.getConstraintDescriptor().isReportAsSingleViolation();
    }
}