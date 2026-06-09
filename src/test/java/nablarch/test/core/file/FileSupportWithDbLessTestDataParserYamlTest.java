package nablarch.test.core.file;

import nablarch.test.core.reader.yaml.YamlModeTestBase;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.BeforeClass;
import org.junit.Rule;

/** {@link FileSupportWithDbLessTestDataParserTest} を YAML モードで再実行する等価性確認テスト。 */
public class FileSupportWithDbLessTestDataParserYamlTest extends FileSupportWithDbLessTestDataParserTest {

    @Rule
    public SystemRepositoryResource repositoryResource =
            new SystemRepositoryResource("unit-test-yaml-dbless.xml");

    @BeforeClass
    public static void prepareYaml() {
        YamlModeTestBase.prepareYamlData(
                FileSupportWithDbLessTestDataParserYamlTest.class,
                FileSupportWithDbLessTestDataParserTest.class);
    }
}
