package nablarch.test.core.reader.yaml;

import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.test.tool.converter.ConversionRequest;
import nablarch.test.tool.converter.DataFormat;
import nablarch.test.tool.converter.TestDataConverter;
import nablarch.test.support.db.helper.DatabaseTestRunner;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * テストデータを使う NTF 本体テストを、Excel 入力と YAML 入力の両方で実行する Runner。
 *
 * <p>各テストメソッドを2回実行する。
 * <ul>
 *   <li>1回目 (Excel入力): 既存設定のまま実行する。</li>
 *   <li>2回目 (YAML入力): {@link YamlSetupRule} が {@code SystemRepositoryResource.before()} の後で
 *       Excel テストデータを YAML に変換し、{@code testDataParser} と
 *       {@code nablarch.test.resource-root} を一時差し替えて実行する。実行後に必ず復元する。</li>
 * </ul>
 * </p>
 *
 * <p>対象テストは {@code @RunWith(NtfTestdataTestRunner.class)} を付けるだけでよい。
 * テストコード本体は変更不要。</p>
 */
public class NtfTestdataTestRunner extends DatabaseTestRunner {

    /** YAML 生成先ルートディレクトリ */
    static final String GENERATED_YAML_ROOT = "target/generated-test-yaml";

    /**
     * YAML モードであることを runChild から getTestRules（同一スレッド）へ受け渡す ThreadLocal。
     * 使用後は必ず remove() すること。
     */
    private static final ThreadLocal<Boolean> YAML_MODE = new ThreadLocal<>();

    /**
     * @param klass テストクラス
     * @throws InitializationError 初期化エラー
     */
    public NtfTestdataTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    /**
     * 各テストメソッドを Excel 入力・YAML 入力の2回実行する。
     *
     * <p>変換処理は {@link YamlSetupRule} 内で {@code SystemRepositoryResource.before()} の後に実行される。
     * これにより {@code nablarch.test.resource-root} が設定済みの状態で変換元パスを解決できる。</p>
     */
    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        // 1回目: Excel 入力（既存設定のまま）
        super.runChild(method, notifier);

        // 2回目: YAML 入力
        YAML_MODE.set(Boolean.TRUE);
        try {
            super.runChild(method, notifier);
        } finally {
            YAML_MODE.remove();
        }
    }

    /**
     * YAML モード時にリスト先頭（最内側）に {@link YamlSetupRule} を追加する。
     *
     * <p>JUnit4 の Rule 実行順序は、リストの後方ほど外側（先に before が呼ばれ、後に after が呼ばれる）。
     * リスト先頭に差し込むことで、{@code SystemRepositoryResource.before()} が XML をロードした後、
     * テスト実行直前に {@link YamlSetupRule} が変換・差し替えを行う。</p>
     */
    @Override
    protected List<TestRule> getTestRules(Object target) {
        List<TestRule> rules = new ArrayList<>(super.getTestRules(target));
        if (Boolean.TRUE.equals(YAML_MODE.get())) {
            rules.add(0, new YamlSetupRule(Paths.get(GENERATED_YAML_ROOT)));
        }
        return rules;
    }

    /**
     * YAML 入力用に変換・差し替え・復元を行う Rule。
     *
     * <p>このクラスは {@link #getTestRules(Object)} からリスト先頭（最内側）に差し込まれるため、
     * {@code SystemRepositoryResource.before()} による XML ロードの後に実行される。
     * この時点で {@code nablarch.test.resource-root} が設定済みであることが保証される。</p>
     */
    static final class YamlSetupRule implements TestRule {

        static final String PARSER_KEY = "testDataParser";
        static final String RESOURCE_ROOT_KEY = "nablarch.test.resource-root";
        private static final String DEFAULT_RESOURCE_ROOT = "test/java/";

        private final Path yamlOutputRoot;

        YamlSetupRule(Path yamlOutputRoot) {
            this.yamlOutputRoot = yamlOutputRoot;
        }

        @Override
        public Statement apply(Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    // SystemRepositoryResource.before() 完了後のため resource-root は設定済み
                    String resourceRootSetting = SystemRepository.get(RESOURCE_ROOT_KEY);
                    if (resourceRootSetting == null || resourceRootSetting.isEmpty()) {
                        resourceRootSetting = DEFAULT_RESOURCE_ROOT;
                    }
                    String xlsRoot = resourceRootSetting.split(";")[0];

                    TestDataConverter.convert(new ConversionRequest.Builder()
                            .sourceFormat(DataFormat.XLS)
                            .targetFormat(DataFormat.YAML)
                            .inputPath(Paths.get(xlsRoot))
                            .outputPath(yamlOutputRoot)
                            .overwrite(true)
                            .build());

                    Object savedParser = SystemRepository.getObject(PARSER_KEY);
                    Object savedResourceRoot = SystemRepository.getObject(RESOURCE_ROOT_KEY);

                    try {
                        loadComponent(PARSER_KEY, new YamlTestDataParser());
                        loadComponent(RESOURCE_ROOT_KEY, yamlOutputRoot.toString());
                        base.evaluate();
                    } finally {
                        // 必ず復元する（例外発生時も含む）
                        restoreComponent(PARSER_KEY, savedParser);
                        restoreComponent(RESOURCE_ROOT_KEY, savedResourceRoot);
                    }
                }
            };
        }

        private void loadComponent(String key, Object value) {
            final String k = key;
            final Object v = value;
            SystemRepository.load(new ObjectLoader() {
                @Override
                public Map<String, Object> load() {
                    return Collections.singletonMap(k, v);
                }
            });
        }

        private void restoreComponent(String key, Object savedValue) {
            if (savedValue != null) {
                loadComponent(key, savedValue);
            } else {
                // NTF の運用上 testDataParser と resource-root は必ず設定されているため
                // null ケースは発生しない。発生したら不正な状態として AssertionError とする。
                throw new AssertionError("UNREACHABLE: " + key + " was null before YAML setup");
            }
        }
    }
}
