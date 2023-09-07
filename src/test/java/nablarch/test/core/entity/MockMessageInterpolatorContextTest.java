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

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetValidatedValue(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("No use of this method is intended.");

        sut.getValidatedValue();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testUnwrap(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("No use of this method is intended.");

        sut.unwrap(this.getClass());
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetAnnotation(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("No use of this method is intended.");

        sut.getConstraintDescriptor().getAnnotation();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetMessageTemplate(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("No use of this method is intended.");

        sut.getConstraintDescriptor().getMessageTemplate();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetGroup(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("No use of this method is intended.");

        sut.getConstraintDescriptor().getGroups();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetPayload(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("No use of this method is intended.");

        sut.getConstraintDescriptor().getPayload();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetValidationAppliesTo(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("No use of this method is intended.");

        sut.getConstraintDescriptor().getValidationAppliesTo();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetConstraintValidatorClasses(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("No use of this method is intended.");

        sut.getConstraintDescriptor().getConstraintValidatorClasses();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testGetComposingConstraints(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("No use of this method is intended.");

        sut.getConstraintDescriptor().getComposingConstraints();
    }

    /**
     * {@link UnsupportedOperationException}が送出されることを確認する。
     */
    @Test
    public void testIsReportAsSingleViolation(){

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("No use of this method is intended.");

        sut.getConstraintDescriptor().isReportAsSingleViolation();
    }
}