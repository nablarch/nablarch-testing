package nablarch.test.core.entity;

import nablarch.core.message.*;
import nablarch.core.repository.SystemRepository;
import nablarch.core.validation.validator.AsciiChar;
import nablarch.core.validation.validator.Length;
import nablarch.core.validation.validator.NumberChar;
import nablarch.core.validation.validator.Required;
import nablarch.test.Assertion;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.*;

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
                sut.invokeValidation(SampleEntity.class, "ascii", null, paramValues);

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
                sut.invokeValidation(SampleEntity.class, "ascii", null, paramValues);

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
                sut.invokeValidation(SampleEntity.class, null, null, paramValues);

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
        sut.invokeValidation(SampleEntity.class, "ascii", null, paramValues);
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
        ValidationTestContext context = sut.validateParameters("", SampleEntity.class, null, null, httpParams);

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
        ValidationTestContext context = sut.validateParameters("form", SampleEntity.class, null, null, httpParams);

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
        ValidationTestContext context = sut.validateParameters("", SampleEntity.class, null, null, httpParams);

        // verify
        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
        assertEquals("asciiは半角英数字または記号で入力せなあかんで", context.getMessages().get(0).formatMessage());
    }

    /**
     * グループ名、パッケージ名マップの両者が空の場合は、nullを返却すること。
     */
    @Test
    public void getGroupFromTestCase() {
        assertNull(sut.getGroupFromTestCase(null, null));
    }

    /**
     * グループ名、パッケージ名が両者とも正しく指定されている場合は、nullを返却すること。
     */
    @Test
    public void getGroupClassFromTestCase() {
        Map<String, String> packageMap = new HashMap<String, String>();
        packageMap.put("PACKAGE_NAME", "nablarch.test.core.entity");
        List<Map<String, String>> packageListMap = new ArrayList<Map<String, String>>();
        packageListMap.add(packageMap);

        // execute, verify
        assertNull(sut.getGroupFromTestCase("TestBean$Test1", packageListMap));
    }

    /**
     * グループ名のみ指定されている場合は、nullを返却すること。
     */
    @Test
    public void thrownWhenOnlyGroupNameSpecified() {
        // execute, verify
        assertNull(sut.getGroupFromTestCase("TestBean$Test1", new ArrayList<Map<String, String>>()));
    }

    /**
     * パッケージリストマップのみ指定されている場合は、nullを返却すること。
     */
    @Test
    public void thrownWhenOnlyPackageListMapSpecified() {
        // setup
        Map<String, String> packageMap = new HashMap<String, String>();
        packageMap.put("PACKAGE_NAME", "nablarch.test.core.entity");
        List<Map<String, String>> packageListMap = new ArrayList<Map<String, String>>();
        packageListMap.add(packageMap);

        // execute, verify
        assertNull(sut.getGroupFromTestCase(null, packageListMap));
    }

    /**
     * 不正なフォーマットのリストマップが指定された場合、nullを返却すること。
     */
    @Test
    public void thrownWhenInvalidListMapSpecified() {
        // setup
        Map<String, String> packageMap = new HashMap<String, String>();
        packageMap.put("HOGE_NAME", "nablarch.test.core.entity");
        List<Map<String, String>> packageListMap = new ArrayList<Map<String, String>>();
        packageListMap.add(packageMap);

        // execute, verify
        assertNull(sut.getGroupFromTestCase("TestBean$Test1", packageListMap));
    }

    /**
     * 存在しないクラスのFQCNを指定した場合、nullを返却すること。
     */
    @Test
    public void thrownWhenNonExistentClassSpecified() {
        // setup
        Map<String, String> packageMap = new HashMap<String, String>();
        packageMap.put("PACKAGE_NAME", "foo.bar.baz.qux");
        List<Map<String, String>> packageListMap = new ArrayList<Map<String, String>>();
        packageListMap.add(packageMap);

        // execute, verify
        assertNull(sut.getGroupFromTestCase("TestBean$Test1", packageListMap));
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
        assertTrue(condition.isEquivalent(message1, message3));
        assertFalse(condition.isEquivalent(message2, message3));
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