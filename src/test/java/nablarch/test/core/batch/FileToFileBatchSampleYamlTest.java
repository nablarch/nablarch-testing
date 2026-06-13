package nablarch.test.core.batch;

import nablarch.test.RepositoryInitializer;
import nablarch.test.core.reader.yaml.YamlModeTestBase;
import nablarch.test.core.standalone.TestShot;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.Ignore;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;

/** {@link FileToFileBatchSampleTest} を YAML モードで再実行する等価性確認テスト。 */
@Ignore("Phase 2 (#3): 変換ツール再構築中につき一時無効化。#13 で再有効化する。")
public class FileToFileBatchSampleYamlTest extends FileToFileBatchSampleTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    @BeforeClass
    public static void prepareYaml() {
        YamlModeTestBase.prepareYamlData(FileToFileBatchSampleYamlTest.class, FileToFileBatchSampleTest.class);
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
