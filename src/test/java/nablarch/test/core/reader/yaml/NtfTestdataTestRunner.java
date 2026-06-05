package nablarch.test.core.reader.yaml;

import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.messaging.RequestTestingMessagingClient;
import nablarch.test.core.messaging.RequestTestingMessagingProvider;
import nablarch.test.core.messaging.RequestTestingSendSyncSupport;
import nablarch.test.core.messaging.SendSyncSupport;
import nablarch.test.core.reader.BasicTestDataParser;
import nablarch.test.core.util.interpreter.TestDataInterpreter;
import nablarch.test.tool.converter.ConversionRequest;
import nablarch.test.tool.converter.DataFormat;
import nablarch.test.tool.converter.TestDataConverter;
import nablarch.test.support.db.helper.DatabaseTestRunner;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     *
     * <p>{@code @TargetDb} 等により1回目が Ignored となったメソッドは2回目の YAML 実行もスキップする。
     * これにより Ignored が二重集計されるレポート汚染を防ぐ。</p>
     */
    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        // 1回目: Excel 入力（既存設定のまま）。Ignored かどうかを検知する。
        IgnoreDetectingNotifier detect = new IgnoreDetectingNotifier(notifier);
        super.runChild(method, detect);

        // @TargetDb 不一致等で Ignored になった場合は YAML 実行もスキップする
        if (detect.ignored) {
            return;
        }

        // Run1（Excel）の static キャッシュをクリアしてから Run2（YAML）を実行する。
        // Run1 で消費済みのイテレータを持つ pool などがキャッシュに残ったまま
        // Run2 に持ち越されるのを防ぐ。
        clearAllStaticCaches();
        YAML_MODE.set(Boolean.TRUE);
        try {
            super.runChild(method, notifier);
        } finally {
            YAML_MODE.remove();
        }
    }

    /**
     * テストデータ読み込み系の全 static キャッシュをクリアする。
     *
     * <p>Run1（Excel）と Run2（YAML）の間でキャッシュが汚染されるのを防ぐため、
     * 各 Run 開始前に呼び出す。消費済みイテレータ（MessagePool 等）を含む
     * キャッシュがそのまま次の Run に使われるバグを防ぐ。</p>
     */
    private static void clearAllStaticCaches() {
        // XLS/Excel パーサ系キャッシュ（package-private クラスへのアクセスはファサード経由）
        BasicTestDataParser.clearAllParseCachesForTest();
        // YAML パーサキャッシュ
        YamlLoader.clearCacheForTest();
        // メッセージング系キャッシュ（消費済みイテレータを含む pool が再利用されるのを防ぐ）
        SendSyncSupport.clearCacheForTest();
        RequestTestingSendSyncSupport.clearCacheForTest();
        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingProvider.RequestTestingMessagingContext.clearSendingMessageCache();
    }

    /**
     * 1回目の {@code runChild} 呼び出しで {@code fireTestIgnored} が呼ばれたかを検知する RunNotifier ラッパー。
     * Ignored 検知以外のイベントはすべて委譲先 {@link RunNotifier} に転送する。
     */
    private static final class IgnoreDetectingNotifier extends RunNotifier {

        private final RunNotifier delegate;
        boolean ignored = false;

        IgnoreDetectingNotifier(RunNotifier delegate) {
            this.delegate = delegate;
        }

        @Override
        public void fireTestIgnored(Description description) {
            ignored = true;
            delegate.fireTestIgnored(description);
        }

        @Override
        public void fireTestStarted(Description description) throws org.junit.runner.notification.StoppedByUserException {
            delegate.fireTestStarted(description);
        }

        @Override
        public void fireTestFailure(Failure failure) {
            delegate.fireTestFailure(failure);
        }

        @Override
        public void fireTestFinished(Description description) {
            delegate.fireTestFinished(description);
        }

        @Override
        public void fireTestAssumptionFailed(Failure failure) {
            delegate.fireTestAssumptionFailed(failure);
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
     *
     * <p>{@code nablarch.test.resource-root} がセミコロン区切りで複数パスを持つ場合、
     * 先頭パスのみを Excel 変換対象とする。</p>
     *
     * <p>変換はテストメソッドごとに実行されるが、{@code overwrite(true)} により冪等であるため
     * 複数回実行しても副作用はない。STEP6 適用後にビルド時間が問題になった場合は
     * クラス単位に最適化することを検討する。</p>
     */
    static final class YamlSetupRule implements TestRule {

        static final String PARSER_KEY = "testDataParser";
        static final String RESOURCE_ROOT_KEY = "nablarch.test.resource-root";
        private static final String DEFAULT_RESOURCE_ROOT = "src/test/java/";
        /** XLS 変換対象外として YAML 出力ルートにコピーするファイル拡張子 */
        private static final Set<String> BINARY_EXTENSIONS = new HashSet<>(
                Arrays.asList(".png", ".jpg", ".jpeg", ".gif", ".bmp", ".pdf", ".bin", ".zip"));

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

                    int exitCode = TestDataConverter.convert(new ConversionRequest.Builder()
                            .sourceFormat(DataFormat.XLS)
                            .targetFormat(DataFormat.YAML)
                            .inputPath(Paths.get(xlsRoot))
                            .outputPath(yamlOutputRoot)
                            .overwrite(true)
                            .build());
                    if (exitCode != 0) {
                        throw new AssertionError(
                                "Excel→YAML 変換に失敗しました（exitCode=" + exitCode
                                + ", xlsRoot=" + xlsRoot + "）");
                    }

                    // BinaryFileInterpreter が参照するバイナリファイルを YAML 出力ルートにコピーする。
                    // YAML モードでは resource-root が yamlOutputRoot に切り替わるため、
                    // xlsRoot 以下のバイナリファイルが見つからなくなることを防ぐ。
                    copyBinaryFiles(Paths.get(xlsRoot), yamlOutputRoot);

                    Object savedParser = SystemRepository.getObject(PARSER_KEY);
                    Object savedResourceRoot = SystemRepository.getObject(RESOURCE_ROOT_KEY);

                    try {
                        YamlTestDataParser yamlParser = new YamlTestDataParser();
                        DbInfo dbInfo = SystemRepository.get("dbInfo");
                        if (dbInfo != null) {
                            yamlParser.setDbInfo(dbInfo);
                        }
                        @SuppressWarnings("unchecked")
                        List<TestDataInterpreter> interps = SystemRepository.get("interpreters");
                        if (interps != null) {
                            yamlParser.setInterpreters(interps);
                        }
                        loadComponent(PARSER_KEY, yamlParser);
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

        /**
         * xlsRoot 以下のバイナリファイルを yamlOutputRoot 以下の対応するパスにコピーする。
         * すでに存在するファイルは上書きしない（冪等）。
         */
        private void copyBinaryFiles(Path xlsRootPath, Path yamlOutputRootPath) throws IOException {
            if (!Files.exists(xlsRootPath)) {
                return;
            }
            Files.walk(xlsRootPath).forEach(source -> {
                String fileName = source.getFileName().toString().toLowerCase();
                boolean isBinary = BINARY_EXTENSIONS.stream().anyMatch(fileName::endsWith);
                if (!isBinary) {
                    return;
                }
                Path relative = xlsRootPath.relativize(source);
                Path target = yamlOutputRootPath.resolve(relative);
                if (!Files.exists(target)) {
                    try {
                        Files.createDirectories(target.getParent());
                        try (InputStream in = Files.newInputStream(source);
                             OutputStream out = Files.newOutputStream(target)) {
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = in.read(buf)) != -1) {
                                out.write(buf, 0, n);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy binary file: " + source, e);
                    }
                }
            });
        }

        private void loadComponent(String key, Object value) {
            SystemRepository.load(new ObjectLoader() {
                @Override
                public Map<String, Object> load() {
                    return Collections.singletonMap(key, value);
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
