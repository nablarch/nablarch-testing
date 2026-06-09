package nablarch.test.core.http;

import nablarch.test.core.reader.yaml.YamlModeTestBase;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.BeforeClass;
import org.junit.Rule;

/** {@link AbstractHttpRequestTestTemplateTest} を YAML モードで再実行する等価性確認テスト。 */
public class AbstractHttpRequestTestTemplateYamlTest extends AbstractHttpRequestTestTemplateTest {

    @Rule
    public SystemRepositoryResource repositoryResource =
            new SystemRepositoryResource("nablarch/test/core/http/http-test-configuration-yaml.xml");

    @BeforeClass
    public static void prepareYaml() {
        YamlModeTestBase.prepareYamlData(
                AbstractHttpRequestTestTemplateYamlTest.class,
                AbstractHttpRequestTestTemplateTest.class);
    }
}
