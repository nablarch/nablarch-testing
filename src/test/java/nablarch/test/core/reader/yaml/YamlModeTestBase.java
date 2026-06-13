package nablarch.test.core.reader.yaml;

import nablarch.fw.ExecutionContext;
import nablarch.fw.launcher.CommandLine;
import nablarch.test.RepositoryInitializer;
import nablarch.test.core.standalone.MainForRequestTesting;
import nablarch.test.core.standalone.TestShot;

/**
 * YAML モードテストを支援するユーティリティ。
 *
 * <p>
 * YAML 版テストクラス（{@code XxxYamlTest extends XxxTest}）から呼び出す static メソッドと定数を提供する。
 * </p>
 *
 * <p>使い方:</p>
 * <pre>
 * public class FooYamlTest extends FooTest {
 *     &#64;Rule
 *     public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");
 *
 *     &#64;BeforeClass
 *     public static void prepareYaml() {
 *         YamlModeTestBase.prepareYamlData(FooYamlTest.class, FooTest.class);
 *     }
 *
 *     &#64;Before
 *     public void switchToYaml() {
 *         repositoryResource.addComponent("testDataParser", new YamlTestDataParser());
 *         repositoryResource.addComponent("nablarch.test.resource-root", YamlModeTestBase.YAML_ROOT);
 *     }
 * }
 * </pre>
 */
public final class YamlModeTestBase {

    /** 変換先ルートディレクトリ（target 配下・.gitignore で管理対象外） */
    public static final String YAML_ROOT = "target/generated-test-yaml";

    private YamlModeTestBase() {
    }

    /**
     * テスト実行前に Excel を YAML に変換し、自クラス名ディレクトリへ複製する。
     *
     * <p>
     * <strong>Phase 2（タスク #3）で変換ツールを一括削除したため、現在は一時的に無効化されている。</strong>
     * 本メソッドを呼ぶ 18 個の {@code *YamlTest} は同フェーズ中 {@code @Ignore} 済み。
     * 設計書どおりに再構築した変換ツールの入口 API（{@code TestDataConverter.convert}）へ
     * タスク #13 で再接続し、本メソッドの Excel→YAML 変換ロジックを復旧する。
     * 復旧時は本タスク（#3）の削除コミット直前 {@code 5a160c4} の実装を起点とする。
     * </p>
     *
     * @param concreteClass YAML 版テストクラス自身（自クラス名ディレクトリ名に使用）
     * @param baseTestClass 変換元 Excel が置かれているテストクラス（Excel 版クラス）
     * @throws UnsupportedOperationException 変換ツール再構築中（#13 で再接続するまで）常に送出
     */
    public static void prepareYamlData(Class<?> concreteClass, Class<?> baseTestClass) {
        throw new UnsupportedOperationException(
                "Phase 2（#3）で変換ツールを一括削除したため一時的に利用不可。"
                        + "#13 で再構築後の TestDataConverter.convert API へ再接続する。");
    }

    /**
     * {@code @ClassRule} を使うテストクラス向けに、{@link TestShot.TestShotAround} をラップする。
     *
     * <p>
     * {@link MainForRequestTesting#handle} の finally で {@code revertDefaultRepository()} が呼ばれ
     * {@code SystemRepository} が unit-test.xml ベースに戻る。このメソッドが返す
     * {@code TestShotAround} は、{@code createMain()} で返す {@code Main} の {@code handle()} が
     * 完了した直後に {@code reInitializeRepository(repositoryXml)} を呼び直すことで、
     * 後続の {@code assertAll()} で YAML 版リポジトリが使われるようにする。
     * </p>
     *
     * @param original      元の {@link TestShot.TestShotAround} 実装
     * @param repositoryXml 差し替え先リポジトリ XML（例: {@code "unit-test-yaml.xml"}）
     * @return ラップされた {@link TestShot.TestShotAround}
     */
    public static TestShot.TestShotAround wrapForYaml(
            TestShot.TestShotAround original, String repositoryXml) {
        return new TestShot.TestShotAround() {
            @Override
            public void setUpInputData(TestShot testShot) {
                original.setUpInputData(testShot);
            }
            @Override
            public void assertOutputData(String msgOnFail, TestShot testShot) {
                original.assertOutputData(msgOnFail, testShot);
            }
            @Override
            public boolean isColumnForTestFramework(String columnName) {
                return original.isColumnForTestFramework(columnName);
            }
            @Override
            public String compareStatus(int actual, TestShot testShot) {
                return original.compareStatus(actual, testShot);
            }
            @Override
            public nablarch.fw.launcher.Main createMain() {
                return new MainForRequestTesting() {
                    @Override
                    public Integer handle(CommandLine commandLine, ExecutionContext context) {
                        try {
                            return super.handle(commandLine, context);
                        } finally {
                            RepositoryInitializer.reInitializeRepository(repositoryXml);
                        }
                    }
                };
            }
        };
    }
}
