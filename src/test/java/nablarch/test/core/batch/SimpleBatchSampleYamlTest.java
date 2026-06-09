package nablarch.test.core.batch;

import nablarch.test.core.reader.yaml.YamlModeTestBase;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.BeforeClass;
import org.junit.Rule;

/** {@link SimpleBatchSampleTest} を YAML モードで再実行する等価性確認テスト。 */
public class SimpleBatchSampleYamlTest extends SimpleBatchSampleTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    @BeforeClass
    public static void prepareYaml() {
        YamlModeTestBase.prepareYamlData(SimpleBatchSampleYamlTest.class, SimpleBatchSampleTest.class);
    }
}
