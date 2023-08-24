package nablarch.test.core.entity;

import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.MockStringResourceHolder;
import nablarch.core.message.StringResource;
import nablarch.core.validation.ValidationResultMessage;
import nablarch.core.validation.ee.Length;
import nablarch.core.validation.ee.Required;
import nablarch.core.validation.ee.Size;
import nablarch.core.validation.ee.SystemChar;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.validation.groups.Default;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeanValidationTestStrategyTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
            "nablarch/test/core/entity/BeanValidationTestStrategyTest.xml");

    @SuppressWarnings("deprecation")
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final ValidationTestStrategy sut = new BeanValidationTestStrategy();

    private static final String[][] MESSAGES = {
            {"nablarch.core.validation.ee.Length.max.message", "ja", "{0}は{2}文字以下で入力して下さい。"},
            {"nablarch.core.validation.ee.SystemChar.message", "ja", "{0}を入力してください。"},
            {"nablarch.core.validation.ee.Size.max.message", "ja", "不正なサイズです"},
            {"MSG00024", "ja", "{0}は{2}文字以下でないとあかんで"},
            {"MSGTEST", "ja", "message1"},
            {"MSGINTERPOLATION", "ja", "message_{interpolate}_interpolated"}
    };

    @Before
    public void before() {
        repositoryResource.getComponentByType(MockStringResourceHolder.class)
                          .setMessages(MESSAGES);
    }

    /**
     * Defaultグループにおける妥当な値を投入した場合、バリデーションが成功すること。
     */
    @Test
    public void testInvokeValidationWithValidParam() {
        // setup
        String[] paramValues = new String[]{"01234567890123456789"};

        // execute
        ValidationTestContext context =
                sut.invokeValidation(SampleBean.class, "number", paramValues, Default.class);

        // verify
        assertTrue(context.isValid());
    }

    /**
     * Defaultグループにおける不正な値を投入した場合、バリデーションは失敗すること。
     */
    @Test
    public void testInvokeValidationWithInvalidParam() {
        // setup
        String[] paramValues = new String[]{"あいう"}; // 半角数字ではない

        // execute
        ValidationTestContext context =
                sut.invokeValidation(SampleBean.class, "number", paramValues, Default.class);

        // verify
        assertFalse(context.isValid()); // バリデーションはNGのはず
    }

    /**
     * Defaultグループにおける妥当な値を投入した場合、バリデーションが成功すること。
     * 配列の要素数が妥当な場合の検証を実施する。
     */
    @Test
    public void testInvokeValidationWithValidSizedArrayParam() {
        // setup
        String[] paramValues = new String[]{"0","1","2","3","4"};

        // execute
        ValidationTestContext context =
                sut.invokeValidation(SampleBean.class, "sizedArray", paramValues, Default.class);

        // verify
        assertTrue(context.isValid());
    }

    /**
     * Defaultグループにおける不正な値を投入した場合、バリデーションが失敗すること。
     * 配列の要素数が上限超過した場合を検証する。
     */
    @Test
    public void testInvokeValidationWithInvalidSizedArrayParam() {
        // setup
        String[] paramValues = new String[]{"0","1","2","3","4","5","6"};

        // execute
        ValidationTestContext context =
                sut.invokeValidation(SampleBean.class, "sizedArray", paramValues, Default.class);

        // verify
        assertFalse(context.isValid()); // バリデーションはNGのはず
    }

    /**
     * Defaultグループにおける妥当な値を投入した場合、バリデーションが成功すること。
     * リストの要素数が妥当な場合の検証を実施する。
     */
    @Test
    public void testInvokeValidationWithValidSizedListParam() {
        // setup
        String[] paramValues = new String[]{"0","1","2","3","4"};

        // execute
        ValidationTestContext context =
                sut.invokeValidation(SampleBean.class, "sizedList", paramValues, Default.class);

        // verify
        assertTrue(context.isValid());
    }

    /**
     * Defaultグループにおける不正な値を投入した場合、バリデーションが失敗すること。
     * 配列の要素数が上限超過した場合を検証する。
     */
    @Test
    public void testInvokeValidationWithInvalidSizedListParam() {
        // setup
        String[] paramValues = new String[]{"0","1","2","3","4","5","6"};

        // execute
        ValidationTestContext context =
                sut.invokeValidation(SampleBean.class, "sizedList", paramValues, Default.class);

        // verify
        assertFalse(context.isValid()); // バリデーションはNGのはず
    }

    /**
     * Test1グループにおける不正な値を投入した場合、バリデーションは失敗すること。
     */
    @Test
    public void testInvokeValidationWithInvalidParamOnTest1Group() {
        // setup
        // Defaultグループでは妥当な値だが、Test1グループでは不正な値
        String[] paramValues = new String[]{"01234567890123456789"};

        // execute
        ValidationTestContext context =
                sut.invokeValidation(SampleBean.class, "number", paramValues, SampleBean.Test1.class);

        // verify
        assertFalse(context.isValid()); // バリデーションはNGのはず
    }


    /**
     * すべてのプロパティの値が妥当であるとき、バリデーションに成功すること。
     */
    @Test
    public void testAllValidateWithValidParameters() {
        // setup
        Map<String, String[]> httpParams = new HashMap<String, String[]>();
        String digits20 = "01234567890123456789";
        httpParams.put("number", new String[]{digits20});  // 半角数字, 20文字以下
        httpParams.put("ascii", new String[]{"abcdef"});   // ascii文字

        // execute
        ValidationTestContext context = sut.validateParameters("", SampleBean.class, httpParams, null, Default.class);

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
        String digits50 = "01234567890123456789012345678901234567890123456789";
        httpParams.put("number", new String[]{digits50});      // 不正な値だがプレフィクスがないのでBeanに設定されない
        httpParams.put("form.ascii", new String[]{"abcdef"});  // ascii文字

        // execute
        ValidationTestContext context = sut.validateParameters("form", SampleBean.class, httpParams, null, Default.class);

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
        String digits50 = "01234567890123456789012345678901234567890123456789";
        httpParams.put("number", new String[]{digits50});  // 半角数字, 20文字以下である必要があるが50文字の値を指定
        httpParams.put("ascii", new String[]{"abcdef"});   // ascii文字

        // execute
        ValidationTestContext context = sut.validateParameters("", SampleBean.class, httpParams, null, Default.class);

        // verify
        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
        assertEquals("{0}は{2}文字以下で入力して下さい。", context.getMessages().get(0).formatMessage());
    }

    /**
     * プレフィクスを指定したパラメータが不正のとき、バリデーションに失敗すること。
     */
    @Test
    public void testAllValidateWithInvalidValidParametersAndPrefix() {
        // setup
        Map<String, String[]> httpParams = new HashMap<String, String[]>();
        String digits50 = "01234567890123456789012345678901234567890123456789";
        httpParams.put("number", new String[]{digits50});      // 不正な値だがプレフィクスがないのでBeanに設定されない
        httpParams.put("form.ascii", new String[]{"あいうえお"});  // 不正な値（ascii文字以外）

        // execute
        ValidationTestContext context = sut.validateParameters("form", SampleBean.class, httpParams, null, Default.class);

        // verify
        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
        assertEquals("messageContent=[{0}を入力してください。] propertyName=[form.ascii]", context.getMessages().get(0).toString());
    }

    /**
     * Test1グループですべてのプロパティの値が妥当であるとき、バリデーションに成功すること。
     */
    @Test
    public void testAllValidateWithValidParametersOnTest1Group() {
        // setup
        Map<String, String[]> httpParams = new HashMap<String, String[]>();
        String digits10 = "0123456789";
        httpParams.put("number", new String[]{digits10});    // 半角数字, 10文字以下
        httpParams.put("ascii", new String[]{"abcdef"});     // 半角英字

        // execute
        ValidationTestContext context = sut.validateParameters("", SampleBean.class, httpParams, null, SampleBean.Test1.class);

        // verify
        assertTrue(context.isValid());
    }

    /**
     * グループ名が正しく指定されている場合は、グループクラスを返却すること。
     */
    @Test
    public void getGroupClassFromName() {
        // execute
        Class<?> group = sut.getGroupFromName("nablarch.test.core.entity.BeanValidationTestStrategyTest$SampleBean$Test1");

        // verify
        assertEquals(SampleBean.Test1.class, group);
    }

    /**
     * グループ名が空の場合は、{@link Default}が返却されること。
     */
    @Test
    public void getDefaultGroupWhenGroupNameNotSpecified() {
        // execute, verify
        assertEquals(Default.class, sut.getGroupFromName(null));
    }

    /**
     * 存在しないクラス名を指定した場合、例外が送出されること。
     */
    @Test
    public void thrownWhenNonExistentClassSpecified() {
        // setup
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Non-existent class is specified for bean validation group. Specified FQCN is foo.bar.baz.qux.BeanValidationTestStrategyTest$SampleBean$Test1");

        // execute
        sut.getGroupFromName("foo.bar.baz.qux.BeanValidationTestStrategyTest$SampleBean$Test1");
    }

    /**
     * メッセージ本文から{@link BeanValidationResultMessage}を取得できること。
     */
    @Test
    public void createExpectedValidationResultMessageFromContent() {

        // setup
        Message actual = sut.createExpectedValidationResultMessage("test", "message1", null);
        Message validationResultMessage1 = new ValidationResultMessage("test", new MockStringResource("2", "message1"), null);
        Message validationResultMessage2 = new ValidationResultMessage("test", new MockStringResource("1", "message2"), null);
        Message validationResultMessage3 = new ValidationResultMessage("hoge", new MockStringResource("1", "message1"), null);
        Message basicMessage = new Message(MessageLevel.ERROR, new MockStringResource("1", "message1"), null);
        Message forCompareHashCode = new ValidationResultMessage("test", new MockStringResource("dummy", "message1"), null);

        // equals()の呼び出し方も含めて検証するので、敢えてassertEqualsは使用しない
        //noinspection SimplifiableAssertion
        assertTrue(actual.equals(validationResultMessage1));
        //noinspection SimplifiableAssertion
        assertFalse(actual.equals(validationResultMessage2));
        //noinspection SimplifiableAssertion
        assertFalse(actual.equals(validationResultMessage3));
        //noinspection SimplifiableAssertion
        assertFalse(actual.equals(basicMessage));

        assertEquals(forCompareHashCode.hashCode(), actual.hashCode());

        assertEquals("messageContent=[message1] propertyName=[test]", actual.toString());
    }

    /**
     * メッセージIDから{@link BeanValidationResultMessage}を取得できること。
     */
    @Test
    public void createExpectedValidationResultMessageFromId() {

        // setup
        Message actual = sut.createExpectedValidationResultMessage("test", "{MSGTEST}", null);
        Message validationResultMessage1 = new ValidationResultMessage("test", new MockStringResource("2", "message1"), null);
        Message validationResultMessage2 = new ValidationResultMessage("test", new MockStringResource("1", "message2"), null);
        Message validationResultMessage3 = new ValidationResultMessage("hoge", new MockStringResource("1", "message1"), null);
        Message basicMessage = new Message(MessageLevel.ERROR, new MockStringResource("1", "message1"), null);
        Message forCompareHashCode = new ValidationResultMessage("test", new MockStringResource("dummy", "message1"), null);

        // equals()の呼び出し方も含めて検証するので、敢えてassertEqualsは使用しない
        //noinspection SimplifiableAssertion
        assertTrue(actual.equals(validationResultMessage1));
        //noinspection SimplifiableAssertion
        assertFalse(actual.equals(validationResultMessage2));
        //noinspection SimplifiableAssertion
        assertFalse(actual.equals(validationResultMessage3));
        //noinspection SimplifiableAssertion
        assertFalse(actual.equals(basicMessage));

        assertEquals(forCompareHashCode.hashCode(), actual.hashCode());

        assertEquals("messageContent=[message1] propertyName=[test]", actual.toString());
    }
    /**
     * メッセージ本文から{@link MessageComparedByContent}を取得できること。
     */
    @Test
    public void createExpectedMessageFromContent() {

        Message actual = sut.createExpectedMessage(MessageLevel.ERROR, "message1", null);
        Message message1 = new Message(MessageLevel.ERROR, new MockStringResource("2", "message1"), null);
        Message message2 = new Message(MessageLevel.ERROR, new MockStringResource("1", "message2"), null);
        Message message3 = new Message(MessageLevel.WARN, new MockStringResource("1", "message1"), null);
        Message validationResultMessage = new ValidationResultMessage("test", new MockStringResource("1", "message1"), null);
        Message forCompareHashCode = new Message(MessageLevel.ERROR, new MockStringResource("dummy", "message1"), null);
        Map<String, Object> options = new HashMap<String, Object>(){{put("interpolate", "test");}};
        Message actualInterpolated = sut.createExpectedMessage(MessageLevel.ERROR, "message_{interpolate}_interpolated", new Object[]{options});

        // equals()の呼び出し方も含めて検証するので、敢えてassertEqualsは使用しない
        //noinspection SimplifiableAssertion
        assertTrue(actual.equals(message1));
        //noinspection SimplifiableAssertion
        assertFalse(actual.equals(message2));
        //noinspection SimplifiableAssertion
        assertFalse(actual.equals(message3));
        //noinspection SimplifiableAssertion
        assertTrue(actual.equals(validationResultMessage));

        assertEquals(forCompareHashCode.hashCode(), actual.hashCode());

        assertEquals("messageContent=[message1] errorLevel=[ERROR]", actual.toString());

        assertEquals("messageContent=[message_test_interpolated] errorLevel=[ERROR]", actualInterpolated.toString());
    }

    /**
     * メッセージIDから{@link MessageComparedByContent}を取得できること。
     */
    @Test
    public void createExpectedMessageFromId() {

        Message actual = sut.createExpectedMessage(MessageLevel.ERROR, "{MSGTEST}", null);
        Message message1 = new Message(MessageLevel.ERROR, new MockStringResource("2", "message1"), null);
        Message message2 = new Message(MessageLevel.ERROR, new MockStringResource("1", "message2"), null);
        Message message3 = new Message(MessageLevel.WARN, new MockStringResource("1", "message1"), null);
        Message validationResultMessage = new ValidationResultMessage("test", new MockStringResource("1", "message1"), null);
        Message forCompareHashCode = new Message(MessageLevel.ERROR, new MockStringResource("dummy", "message1"), null);
        Map<String, Object> options = new HashMap<String, Object>(){{put("interpolate", "test");}};
        Message actualInterpolated = sut.createExpectedMessage(MessageLevel.ERROR, "{MSGINTERPOLATION}", new Object[]{options});


        // equals()の呼び出し方も含めて検証するので、敢えてassertEqualsは使用しない
        //noinspection SimplifiableAssertion
        assertTrue(actual.equals(message1));
        //noinspection SimplifiableAssertion
        assertFalse(actual.equals(message2));
        //noinspection SimplifiableAssertion
        assertFalse(actual.equals(message3));
        //noinspection SimplifiableAssertion
        assertTrue(actual.equals(validationResultMessage));

        assertEquals(forCompareHashCode.hashCode(), actual.hashCode());

        assertEquals("messageContent=[message1] errorLevel=[ERROR]", actual.toString());

        assertEquals("messageContent=[message_test_interpolated] errorLevel=[ERROR]", actualInterpolated.toString());
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

    public static class SampleBean {
        @Length.List({
                @Length(max = 20),
                @Length(max = 10, groups = Test1.class, message = "{MSG00024}")
        })
        @SystemChar(charsetDef = "半角数字")
        private String number;

        @Required.List({
                @Required,
                @Required(groups = Test1.class, message = "{MSG00010}")
        })
        @SystemChar(charsetDef = "ASCII文字")
        private String ascii;

        @Size(max = 6)
        private String[] sizedArray;

        @Size(max = 6)
        private List<String> sizedList;

        public SampleBean() {
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getAscii() {
            return ascii;
        }

        public void setAscii(String ascii) {
            this.ascii = ascii;
        }

        public String[] getSizedArray() {
            return sizedArray;
        }

        public void setSizedArray(String[] sizedArray) {
            this.sizedArray = sizedArray;
        }

        public List<String> getSizedList() {
            return sizedList;
        }

        public void setSizedList(List<String> sizedList) {
            this.sizedList = sizedList;
        }

        public interface Test1 {
        }

    }
}