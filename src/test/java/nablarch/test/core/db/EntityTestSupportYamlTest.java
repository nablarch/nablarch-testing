package nablarch.test.core.db;

import nablarch.test.core.reader.yaml.YamlModeTestBase;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.BeforeClass;
import org.junit.Rule;

/** {@link EntityTestSupportTest} を YAML モードで再実行する等価性確認テスト。 */
public class EntityTestSupportYamlTest extends EntityTestSupportTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    @BeforeClass
    public static void prepareYaml() {
        YamlModeTestBase.prepareYamlData(EntityTestSupportYamlTest.class, EntityTestSupportTest.class);
    }
}
