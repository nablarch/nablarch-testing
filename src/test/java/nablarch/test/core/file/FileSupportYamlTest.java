package nablarch.test.core.file;

import nablarch.test.core.reader.yaml.YamlModeTestBase;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.Ignore;
import org.junit.BeforeClass;
import org.junit.Rule;

/** {@link FileSupportTest} を YAML モードで再実行する等価性確認テスト。 */
@Ignore("Phase 2 (#3): 変換ツール再構築中につき一時無効化。#13 で再有効化する。")
public class FileSupportYamlTest extends FileSupportTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    @BeforeClass
    public static void prepareYaml() {
        YamlModeTestBase.prepareYamlData(FileSupportYamlTest.class, FileSupportTest.class);
    }
}
