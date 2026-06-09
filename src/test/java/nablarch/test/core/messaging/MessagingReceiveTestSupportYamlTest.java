package nablarch.test.core.messaging;

import nablarch.test.core.reader.yaml.YamlModeTestBase;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.BeforeClass;
import org.junit.ClassRule;

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
}
