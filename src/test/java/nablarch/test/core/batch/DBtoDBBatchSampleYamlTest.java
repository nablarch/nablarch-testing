package nablarch.test.core.batch;

import nablarch.test.RepositoryInitializer;
import nablarch.test.core.reader.yaml.YamlModeTestBase;
import nablarch.test.core.standalone.TestShot;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

/**
 * {@link DBtoDBBatchSampleTest} を YAML モードで再実行する等価性確認テスト。
 *
 * <p>
 * 親クラスが {@code @ClassRule} を使っているため、同名フィールドを再宣言して差し替える。
 * </p>
 */
public class DBtoDBBatchSampleYamlTest extends DBtoDBBatchSampleTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource =
            new SystemRepositoryResource("unit-test-yaml.xml");

    @BeforeClass
    public static void prepareYaml() {
        YamlModeTestBase.prepareYamlData(DBtoDBBatchSampleYamlTest.class, DBtoDBBatchSampleTest.class);
    }

    @Before
    public void switchToYaml() {
        RepositoryInitializer.reInitializeRepository("unit-test-yaml.xml");
    }

    @Override
    protected TestShot.TestShotAround createTestShotAround(Class<?> testClass) {
        return YamlModeTestBase.wrapForYaml(super.createTestShotAround(testClass), "unit-test-yaml.xml");
    }
}
