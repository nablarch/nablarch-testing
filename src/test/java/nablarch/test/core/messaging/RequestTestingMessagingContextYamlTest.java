package nablarch.test.core.messaging;

import nablarch.test.core.reader.yaml.YamlModeTestBase;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.BeforeClass;
import org.junit.Rule;

/** {@link RequestTestingMessagingContextTest} を YAML モードで再実行する等価性確認テスト。 */
public class RequestTestingMessagingContextYamlTest extends RequestTestingMessagingContextTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    @BeforeClass
    public static void prepareYaml() {
        YamlModeTestBase.prepareYamlData(
                RequestTestingMessagingContextYamlTest.class,
                RequestTestingMessagingContextTest.class);
    }
}
