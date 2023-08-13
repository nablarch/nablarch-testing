package nablarch.test.core.entity;

import nablarch.core.message.*;
import nablarch.core.validation.ee.Length;
import nablarch.core.validation.ee.Required;
import nablarch.core.validation.ee.SystemChar;
import nablarch.test.Assertion;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

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
            {"MSG00024", "ja", "{0}は{2}文字以下でないとあかんで"}
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
                sut.invokeValidation(SampleBean.class, "number", null, paramValues);

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
                sut.invokeValidation(SampleBean.class, "number", null, paramValues);

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
                sut.invokeValidation(SampleBean.class, "number", SampleBean.Test1.class, paramValues);

        // verify
        assertFalse(context.isValid()); // バリデーションはNGのはず
    }

    private final String digits50 = "01234567890123456789012345678901234567890123456789";
    private final String digits20 = "01234567890123456789";
    private final String digits10 = "0123456789";

    /**
     * すべてのプロパティの値が妥当であるとき、バリデーションに成功すること。
     */
    @Test
    public void testAllValidateWithValidParameters() {
        // setup
        Map<String, String[]> httpParams = new HashMap<String, String[]>();
        httpParams.put("number", new String[]{digits20});  // 半角数字, 20文字以下
        httpParams.put("ascii", new String[]{"abcdef"});   // ascii文字

        // execute
        ValidationTestContext context = sut.validateParameters("", SampleBean.class, null, null, httpParams);

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
        httpParams.put("number", new String[]{digits50});      // 不正な値だがプレフィクスがないのでBeanに設定されない
        httpParams.put("form.ascii", new String[]{"abcdef"});  // ascii文字

        // execute
        ValidationTestContext context = sut.validateParameters("form", SampleBean.class, null, null, httpParams);

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
        httpParams.put("number", new String[]{digits50});  // 半角数字, 20文字以下である必要があるが50文字の値を指定
        httpParams.put("ascii", new String[]{"abcdef"});   // ascii文字

        // execute
        ValidationTestContext context = sut.validateParameters("", SampleBean.class, null, null, httpParams);

        // verify
        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
        assertEquals("{0}は{2}文字以下で入力して下さい。", context.getMessages().get(0).formatMessage());
    }

    /**
     * Test1グループですべてのプロパティの値が妥当であるとき、バリデーションに成功すること。
     */
    @Test
    public void testAllValidateWithValidParametersOnTest1Group() {
        // setup
        Map<String, String[]> httpParams = new HashMap<String, String[]>();
        httpParams.put("number", new String[]{digits10});    // 半角数字, 10文字以下
        httpParams.put("ascii", new String[]{"abcdef"});     // 半角英字

        // execute
        ValidationTestContext context = sut.validateParameters("", SampleBean.class, SampleBean.Test1.class, null, httpParams);

        // verify
        assertTrue(context.isValid());
    }

    /**
     * グループ名、パッケージ名マップの両者が空の場合は、nullを返却すること。
     */
    @Test
    public void getNullFromTestCase() {
        assertNull(sut.getGroupFromTestCase(null, new ArrayList<Map<String, String>>()));
    }

    /**
     * グループ名、パッケージ名が両者とも正しく指定されている場合は、グループクラスを返却すること。
     */
    @Test
    public void getGroupClassFromTestCase() {
        Map<String, String> packageMap = new HashMap<String, String>();
        packageMap.put("PACKAGE_NAME", "nablarch.test.core.entity");
        List<Map<String, String>> packageListMap = new ArrayList<Map<String, String>>();
        packageListMap.add(packageMap);

        // execute
        Class<?> group = sut.getGroupFromTestCase("BeanValidationTestStrategyTest$SampleBean$Test1", packageListMap);

        // verify
        assertEquals(SampleBean.Test1.class, group);

    }

    /**
     * グループ名のみ指定されている場合は、例外が送出されること。
     */
    @Test
    public void thrownWhenOnlyGroupNameSpecified() {
        // setup
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Both the groupName and packageKey must be specified. Otherwise, both must be unspecified.");

        // execute
        sut.getGroupFromTestCase("SampleBean$Test1", new ArrayList<Map<String, String>>());
    }

    /**
     * パッケージリストマップのみ指定されている場合は、例外が送出されること。
     */
    @Test
    public void thrownWhenOnlyPackageListMapSpecified() {
        // setup
        Map<String, String> packageMap = new HashMap<String, String>();
        packageMap.put("PACKAGE_NAME", "nablarch.test.core.entity");
        List<Map<String, String>> packageListMap = new ArrayList<Map<String, String>>();
        packageListMap.add(packageMap);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Both the groupName and packageKey must be specified. Otherwise, both must be unspecified.");

        // execute
        sut.getGroupFromTestCase(null, packageListMap);
    }

    /**
     * 不正なフォーマットのリストマップが指定された場合、例外が送出されること。
     */
    @Test
    public void thrownWhenInvalidListMapSpecified() {
        // setup
        Map<String, String> packageMap = new HashMap<String, String>();
        packageMap.put("HOGE_NAME", "nablarch.test.core.entity");
        List<Map<String, String>> packageListMap = new ArrayList<Map<String, String>>();
        packageListMap.add(packageMap);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("PACKAGE_NAME is required for the package name LIST_MAP.");

        // execute
        sut.getGroupFromTestCase("SampleBean$Test1", packageListMap);
    }

    /**
     * 存在しないクラスのFQCNを指定した場合、例外が送出されること。
     */
    @Test
    public void thrownWhenNonExistentClassSpecified() {
        // setup
        Map<String, String> packageMap = new HashMap<String, String>();
        packageMap.put("PACKAGE_NAME", "foo.bar.baz.qux");
        List<Map<String, String>> packageListMap = new ArrayList<Map<String, String>>();
        packageListMap.add(packageMap);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Non-existent class is specified for bean validation group. Specified FQCN is foo.bar.baz.qux.Sample$Test1");

        // execute
        sut.getGroupFromTestCase("Sample$Test1", packageListMap);
    }

    /**
     * Assertion.AsMessageContentのインスタンスを取得できること。
     */
    @Test
    public void isInstanceOfAsMessageContent() {
        // setup, execute
        Assertion.EquivCondition<Message, Message> condition = sut.getEquivCondition();
        Message message1 = new Message(MessageLevel.ERROR, new MockStringResource("msg1", "value1"));
        Message message2 = new Message(MessageLevel.ERROR, new MockStringResource("msg2", "value2"));
        Message message3 = new Message(MessageLevel.ERROR, new MockStringResource("msg1", "value2"));

        // verify
        assertTrue(condition.isEquivalent(message2, message2));
        assertFalse(condition.isEquivalent(message1, message3));
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

        public interface Test1 {
        }

    }
}