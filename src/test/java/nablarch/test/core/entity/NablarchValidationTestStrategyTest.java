package nablarch.test.core.entity;

import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.MockStringResourceHolder;
import nablarch.core.message.StringResource;
import nablarch.core.repository.SystemRepository;
import nablarch.core.validation.ValidationResultMessage;
import nablarch.core.validation.validator.AsciiChar;
import nablarch.core.validation.validator.Length;
import nablarch.core.validation.validator.NumberChar;
import nablarch.core.validation.validator.Required;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

public class NablarchValidationTestStrategyTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
            "nablarch/test/core/entity/NablarchValidationTestStrategyTest.xml");

    @SuppressWarnings("deprecation")
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final ValidationTestStrategy sut = new NablarchValidationTestStrategy();

    private static final String[][] MESSAGES = {
            {"MSG00012", "ja", "{0}は半角英数字または記号で入力してください。"},
            {"MSG00024", "ja", "{0}は半角英数字または記号で入力せなあかんで"},
    };

    @Before
    public void before() {
        repositoryResource.getComponentByType(MockStringResourceHolder.class)
                          .setMessages(MESSAGES);
    }

    /**
     * 妥当な値を投入した場合、バリデーションが成功すること。
     */
    @Test
    public void testInvokeValidationWithValidParam() {
        // setup
        String[] paramValues = new String[]{"abc"};

        // execute
        ValidationTestContext context =
                sut.invokeValidation(SampleEntity.class, "ascii", paramValues, null);

        // verify
        assertTrue(context.isValid());
    }

    /**
     * 不正な値を投入した場合、バリデーションは失敗すること。
     */
    @Test
    public void testInvokeValidationWithInvalidParam() {
        // setup
        String[] paramValues = new String[]{"あいう"}; // ASCIIでない

        // execute
        ValidationTestContext context =
                sut.invokeValidation(SampleEntity.class, "ascii", paramValues, null);

        assertFalse(context.isValid()); // バリデーションはNGのはず
    }

    /**
     * 検証対象のプロパティ名を指定しない場合、例外が発生すること。
     */
    @Test
    public void testInvokeValidationWithNullTargetPropertyName() {
        // setup
        String[] paramValues = new String[]{"abc"};

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(containsString("unexpected exception occurred. target entity=[nablarch.test.core.entity.NablarchValidationTestStrategyTest$SampleEntity] property=[] parameter=["));
        // execute
        ValidationTestContext context =
                sut.invokeValidation(SampleEntity.class, null, paramValues, null);

        assertTrue(context.isValid());
    }

    /**
     * リポジトリからインスタンス取得に失敗した場合、例外が発生すること。
     */
    @Test(expected = IllegalStateException.class)
    public void testGetValidationManagerFail() {
        // setup
        String[] paramValues = new String[]{"hoge"};
        SystemRepository.clear();  // ValidationManager is removed.

        // execute
        // must throw exception.
        sut.invokeValidation(SampleEntity.class, "ascii", paramValues, null);
    }

    private final String asciis21 = "0123456789abcdefghijk";
    private final String asciis20 = "0123456789abcdefghij";
    private final String digits = "0123456789";

    /**
     * すべてのプロパティの値が妥当であるとき、バリデーションに成功すること。
     */
    @Test
    public void testAllValidateWithValidParameters() {
        // setup
        Map<String, String[]> httpParams = new HashMap<String, String[]>();
        httpParams.put("ascii", new String[]{asciis20}); // ASCII文字, 20文字以下
        httpParams.put("number", new String[]{digits});  // 半角数字, 必須

        // execute
        ValidationTestContext context = sut.validateParameters("", SampleEntity.class, httpParams, null, null);

        System.out.println(context.getMessages());
        // verify
        assertTrue(context.isValid());
    }

    /**
     * すべてのプロパティの値が妥当であるとき、バリデーションに成功すること。
     * プレフィクスを指定したパラメータのみ、Beanに設定されること。
     */
    @Test
    public void testAllValidateWithValidParametersAndPrefix() {
        // setup
        Map<String, String[]> httpParams = new HashMap<String, String[]>();
        httpParams.put("ascii", new String[]{asciis21});      // 不正な値だがプレフィクスがないのでEntityに設定されない
        httpParams.put("form.number", new String[]{digits});  // 半角数字, 必須

        // execute
        ValidationTestContext context = sut.validateParameters("form", SampleEntity.class, httpParams, null, null);

        // verify
        assertTrue(context.isValid());
    }

    /**
     * いずれかのプロパティの値が不正であるとき、バリデーションに失敗すること。
     */
    @Test
    public void testAllValidateWithInvalidParameters() {
        // setup
        Map<String, String[]> httpParams = new HashMap<String, String[]>();
        httpParams.put("ascii", new String[]{asciis21}); // ascii文字, 20文字以下である必要があるが21文字の値を指定
        httpParams.put("number", new String[]{digits});  // 半角数字, 必須

        // execute
        ValidationTestContext context = sut.validateParameters("", SampleEntity.class, httpParams, null, null);

        // verify
        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
        assertEquals("asciiは半角英数字または記号で入力せなあかんで", context.getMessages().get(0).formatMessage());
    }

    /**
     * グループ名が正しく指定されている場合は、nullを返却すること。
     */
    @Test
    public void getGroupClassFromName() {
        // execute
        Class<?> group = sut.getGroupFromName("nablarch.test.core.entity.BeanValidationTestStrategyTest$SampleBean$Test1");

        // verify
        assertNull(group);
    }

    /**
     * グループ名が空の場合は、nullが返却されること。
     */
    @Test
    public void nullWhenGroupNameNotSpecified() {
        // execute, verify
        assertNull(sut.getGroupFromName(null));
    }

    /**
     * 存在しないクラス名を指定した場合、nullが返却されること。
     */
    @Test
    public void thrownWhenNonExistentClassSpecified() {
        // execute
        Class<?> group = sut.getGroupFromName("foo.bar.baz.qux.BeanValidationTestStrategyTest$SampleBean$Test1");

        // verify
        assertNull(group);
    }

     /**
     * {@link ValidationResultMessage}を取得できること。
     */
    @Test
    public void createExpectedValidationResultMessage() {

        Message actual = sut.createExpectedValidationResultMessage("test", "1", new Object[0]);
        Message validationResultMessage1 = new ValidationResultMessage("test", new MockStringResource("2", "message1"), new Object[0]);
        Message validationResultMessage2 = new ValidationResultMessage("test", new MockStringResource("1", "message2"), new Object[0]);
        Message validationResultMessage3 = new ValidationResultMessage("hoge", new MockStringResource("1", "message1"), new Object[0]);
        Message basicMessage = new Message(MessageLevel.ERROR, new MockStringResource("1", "message1"), new Object[0]);

        // equals()の呼び出し方も含めて検証するので、敢えてassertEqualsは使用しない
        //noinspection SimplifiableAssertion
        assertFalse(actual.equals(validationResultMessage1));
        //noinspection SimplifiableAssertion
        assertTrue(actual.equals(validationResultMessage2));
        //noinspection SimplifiableAssertion
        assertFalse(actual.equals(validationResultMessage3));
        //noinspection SimplifiableAssertion
        assertFalse(actual.equals(basicMessage));
    }

    /**
     * {@link MessageComparedById}を取得できること。
     */
    @Test
    public void createExpectedMessage() {

        Message actual = sut.createExpectedMessage(MessageLevel.ERROR, "1", new Object[0]);
        Message message1 = new Message(MessageLevel.ERROR, new MockStringResource("2", "message1"), new Object[0]);
        Message message2 = new Message(MessageLevel.ERROR, new MockStringResource("1", "message2"), new Object[0]);
        Message message3 = new Message(MessageLevel.WARN, new MockStringResource("1", "message1"), new Object[0]);
        Message validationResultMessage = new ValidationResultMessage("test", new MockStringResource("1", "message1"), new Object[0]);
        Message forCompareHashCode = new Message(MessageLevel.ERROR, new MockStringResource("1", "message1"), new Object[0]);

        // equals()の呼び出し方も含めて検証するので、敢えてassertEqualsは使用しない
        //noinspection SimplifiableAssertion
        assertFalse(actual.equals(message1));
        //noinspection SimplifiableAssertion
        assertTrue(actual.equals(message2));
        //noinspection SimplifiableAssertion
        assertFalse(actual.equals(message3));
        //noinspection SimplifiableAssertion
        assertTrue(actual.equals(validationResultMessage));

        assertEquals(forCompareHashCode.hashCode(), actual.hashCode());

        assertEquals("messageId=[1] errorLevel=[ERROR]", actual.toString());
    }

    private static class MockStringResource implements StringResource {

        private final String id;
        private final String value;

        public MockStringResource(String id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getValue(Locale locale) {
            return value;
        }
    }

   public static class SampleEntity {
        @Length(max = 20)
        @AsciiChar
        public void setAscii(String s) {
        }

        @Required
        @NumberChar
        public void setNumber(String s) {
        }
    }
}