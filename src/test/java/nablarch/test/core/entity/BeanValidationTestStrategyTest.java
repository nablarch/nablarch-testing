package nablarch.test.core.entity;

import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.MessageUtil;
import nablarch.core.message.MockStringResourceHolder;
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
                sut.invokeValidation(TestBean.class, "numberMax", null, paramValues);

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
                sut.invokeValidation(TestBean.class, "numberMax", null, paramValues);

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
                sut.invokeValidation(TestBean.class, "numberMax", TestBean.Test1.class, paramValues);

        // verify
        assertFalse(context.isValid()); // バリデーションはNGのはず
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
        Class<?> group = sut.getGroupFromTestCase("TestBean$Test1", packageListMap);

        // verify
        assertEquals(TestBean.Test1.class, group);

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
        sut.getGroupFromTestCase("TestBean$Test1", new ArrayList<Map<String, String>>());
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
        sut.getGroupFromTestCase("TestBean$Test1", packageListMap);
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
        expectedException.expectMessage("Non-existent class is specified for bean validation group. Specified FQCN is foo.bar.baz.qux.TestBean$Test1");

        // execute
        sut.getGroupFromTestCase("TestBean$Test1", packageListMap);
    }

    /**
     * 期待するメッセージと実際のメッセージが同一である場合、アサーションOKとなること。
     */
    @Test
    public void assertOKWhenIdenticalMessage() {
        // setup
        Message actualMessage = MessageUtil.createMessage(MessageLevel.ERROR, "nablarch.core.validation.ee.Length.max.message");

        // execute
        sut.assertMessageEquals("", "nablarch.core.validation.ee.Length.max.message", actualMessage);

    }

    /**
     * 期待するメッセージと実際のメッセージが一致しない場合、アサーションNGとなること。
     */
    @Test
    public void assertFailWhenDifferentMessage() {
        // setup
        Message actualMessage = MessageUtil.createMessage(MessageLevel.ERROR, "nablarch.core.validation.ee.Length.max.message");

        expectedException.expect(AssertionError.class);
        expectedException.expectMessage("assertion message.");

        // execute
        sut.assertMessageEquals("assertion message.", "nablarch.core.validation.ee.SystemChar.message", actualMessage);

    }
}