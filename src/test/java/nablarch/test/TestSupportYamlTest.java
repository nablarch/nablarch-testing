package nablarch.test;

import nablarch.test.core.reader.yaml.YamlModeTestBase;
import nablarch.test.support.SystemRepositoryResource;
import org.junit.Ignore;
import org.junit.BeforeClass;
import org.junit.Rule;

/**
 * {@link TestSupportTest} を YAML モードで再実行する等価性確認テスト。
 *
 * <p>
 * {@code repositoryResource} を {@code unit-test-yaml.xml} ベースに差し替えることで、
 * テストデータパーサーを {@link nablarch.test.core.reader.YamlTestDataParser} に切り替える。
 * </p>
 */
@Ignore("Phase 2 (#3): 変換ツール再構築中につき一時無効化。#13 で再有効化する。")
public class TestSupportYamlTest extends TestSupportTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    @BeforeClass
    public static void prepareYaml() {
        YamlModeTestBase.prepareYamlData(TestSupportYamlTest.class, TestSupportTest.class);
    }
}
