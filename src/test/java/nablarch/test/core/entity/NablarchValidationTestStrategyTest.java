package nablarch.test.core.entity;

import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.MessageUtil;
import nablarch.core.message.MockStringResourceHolder;
import nablarch.core.repository.SystemRepository;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            {"MSG90012", "ja", "{0}は半角英数字または記号で入力せんとあかんで"},
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
                sut.invokeValidation(TestEntity.class, "ascii", null, paramValues);

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
                sut.invokeValidation(TestEntity.class, "ascii", null, paramValues);

        assertFalse(context.isValid()); // バリデーションはNGのはず
    }

    @Test
    public void testInvokeValidationWithNullTargetPropertyName() {
        // setup
        String[] paramValues = new String[]{"abc"};

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(containsString("unexpected exception occurred. target entity=[nablarch.test.core.entity.TestEntity] property=[] parameter=["));
        // execute
        ValidationTestContext context =
                sut.invokeValidation(TestEntity.class, null, null, paramValues);

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
        sut.invokeValidation(TestEntity.class, "ascii", null, paramValues);
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
     * 期待するメッセージIDと実際のメッセージIDが同一である場合、アサーションOKとなること。
     */
    @Test
    public void assertOKWhenIdenticalMessageId() {
        // setup
        Message actualMessage = MessageUtil.createMessage(MessageLevel.ERROR, "MSG00012");

        // execute
        sut.assertMessageEquals("", "MSG00012", actualMessage);

    }

    /**
     * 期待するメッセージIDと実際のメッセージIDが一致しない場合、アサーションNGとなること。
     */
    @Test
    public void assertFailWhenDifferentMessageId() {
        // setup
        Message actualMessage = MessageUtil.createMessage(MessageLevel.ERROR, "MSG00012");

        expectedException.expect(AssertionError.class);
        expectedException.expectMessage("assertion message.");

        // execute
        sut.assertMessageEquals("assertion message.", "MSG90012", actualMessage);

    }

}