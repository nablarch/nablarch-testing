package nablarch.test.core.messaging;

import nablarch.test.core.reader.yaml.YamlModeTestBase;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.BeforeClass;
import org.junit.Rule;

/** {@link RequestTestingSendSyncBatchTest} を YAML モードで再実行する等価性確認テスト。 */
public class RequestTestingSendSyncBatchYamlTest extends RequestTestingSendSyncBatchTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    @BeforeClass
    public static void prepareYaml() {
        YamlModeTestBase.prepareYamlData(
                RequestTestingSendSyncBatchYamlTest.class,
                RequestTestingSendSyncBatchTest.class);
    }
}
