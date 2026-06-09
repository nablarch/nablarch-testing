package nablarch.test.core.http;

import nablarch.test.RepositoryInitializer;
import nablarch.test.core.reader.yaml.YamlModeTestBase;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static nablarch.test.Assertion.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

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

    @Test
    @Override
    public void testGetEmptyTestCase() {
        RepositoryInitializer.initializeDefaultRepository();
        RepositoryInitializer.reInitializeRepository(
                "nablarch/test/core/http/http-test-configuration-yaml.xml");

        target = createDefaultMock();
        try {
            target.execute("testGetEmptyTestCase");
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("testShots (LIST_MAP=testShots) must have one or more test " +
                    "shots"));
        }
    }
}
