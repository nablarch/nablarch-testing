package nablarch.test.core.messaging;

import nablarch.test.RepositoryInitializer;
import nablarch.test.core.reader.yaml.YamlModeTestBase;
import nablarch.test.core.standalone.TestShot;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * {@link MessagingReceiveTestSupportTest} を YAML モードで再実行する等価性確認テスト。
 *
 * <p>
 * 親クラスが {@code @ClassRule} を使っているため、同名フィールドを再宣言して差し替える。
 * </p>
 */
public class MessagingReceiveTestSupportYamlTest extends MessagingReceiveTestSupportTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource =
            new SystemRepositoryResource("unit-test-yaml.xml");

    @BeforeClass
    public static void prepareYaml() {
        YamlModeTestBase.prepareYamlData(
                MessagingReceiveTestSupportYamlTest.class,
                MessagingReceiveTestSupportTest.class);
    }

    @Override
    protected TestShot.TestShotAround createTestShotAround(Class<?> testClass) {
        return YamlModeTestBase.wrapForYaml(
                super.createTestShotAround(testClass), "unit-test-yaml.xml");
    }

    @Test
    @Override
    public void testUnExtends() {
        MessagingReceiveTestSupport support = new MessagingReceiveTestSupport(getClass()) {
            @Override
            protected TestShot.TestShotAround createTestShotAround(Class<?> testClass) {
                return YamlModeTestBase.wrapForYaml(
                        super.createTestShotAround(testClass), "unit-test-yaml.xml");
            }
        };
        support.execute("testUnExtends");
    }

    @Before
    public void switchToYaml() {
        RepositoryInitializer.reInitializeRepository("unit-test-yaml.xml");
    }
}
