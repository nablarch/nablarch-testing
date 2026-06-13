package nablarch.test.core.messaging;

import nablarch.test.core.reader.yaml.YamlModeTestBase;
import org.junit.Ignore;
import org.junit.BeforeClass;

/**
 * {@link RequestTestingMessagingClientTest} を YAML モードで再実行する等価性確認テスト。
 *
 * <p>
 * 親クラスは {@code RepositoryInitializer} で直接 SystemRepository をロードするため、
 * {@code repositoryResource} ベースの差し替えは使えない。
 * YAML データのみ生成し、YAML 版リポジトリ設定が存在しないため本クラスは YAML モードでは動作しない可能性がある。
 * 詳細調査が完了するまでは prepareYaml のみ実施する。
 * </p>
 *
 * TODO: RepositoryInitializer 経由のリポジトリに YamlTestDataParser を注入する方法を検討する。
 */
@Ignore("Phase 2 (#3): 変換ツール再構築中につき一時無効化。#13 で再有効化する。")
public class RequestTestingMessagingClientYamlTest extends RequestTestingMessagingClientTest {

    @BeforeClass
    public static void prepareYaml() {
        YamlModeTestBase.prepareYamlData(
                RequestTestingMessagingClientYamlTest.class,
                RequestTestingMessagingClientTest.class);
    }
}
