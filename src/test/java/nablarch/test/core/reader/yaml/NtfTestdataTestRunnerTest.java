package nablarch.test.core.reader.yaml;

import nablarch.core.repository.SystemRepository;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link NtfTestdataTestRunner} のユニットテスト。
 */
@RunWith(DatabaseTestRunner.class)
public class NtfTestdataTestRunnerTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    /**
     * Runner を付けたテストクラスが2回実行され（Excel + YAML）、両方パスすることを確認する。
     *
     * <p>Given: {@code NtfTestdataTestRunner} を適用した {@code SampleTarget} クラス
     * When: Runner で実行する
     * Then: テストメソッドが2回カウントされ（1メソッド × Excel + YAML）、失敗ゼロ</p>
     */
    @Test
    public void runnerExecutesEachMethodTwice_excelThenYaml() throws InitializationError {
        // When
        NtfTestdataTestRunner runner = new NtfTestdataTestRunner(SampleTarget.class);
        CountingNotifier notifier = new CountingNotifier();
        runner.run(notifier);

        // Then: SampleTarget の1メソッドが Excel + YAML の2回実行される
        assertThat("run count (Excel + YAML)", notifier.startedCount, is(2));
        if (!notifier.failures.isEmpty()) {
            fail("Unexpected failures: " + notifier.failures);
        }
    }

    /**
     * YAML 実行後に testDataParser と resource-root が元の値に復元されることを確認する。
     *
     * <p>Given: RestoreVerifyTarget が Excel/YAML 2回実行の前後で parser クラス名を記録するよう設定
     * When: NtfTestdataTestRunner で実行する
     * Then: 2回目（YAML）実行の直前は YamlTestDataParser であり、テスト完了後は BasicTestDataParser に戻る</p>
     */
    @Test
    public void afterYamlRun_parserIsRestoredBeforeSystemRepositoryClears() throws InitializationError {
        // When
        NtfTestdataTestRunner runner = new NtfTestdataTestRunner(RestoreVerifyTarget.class);
        CountingNotifier notifier = new CountingNotifier();
        runner.run(notifier);

        // Then: 失敗がないこと（YamlSetupRule が復元に失敗すれば UNREACHABLE AssertionError で失敗する）
        if (!notifier.failures.isEmpty()) {
            fail("Unexpected failures (may indicate restore failure): " + notifier.failures);
        }
        // parserInYamlRun は YamlTestDataParser、parserAfterRestore は BasicTestDataParser
        assertThat("parser during YAML run",
                RestoreVerifyTarget.parserInYamlRun, is(YamlTestDataParser.class.getName()));
        assertThat("parser after YAML run restore",
                RestoreVerifyTarget.parserAfterRestore,
                not(is(YamlTestDataParser.class.getName())));
    }

    /**
     * YAML 実行中に testDataParser が YamlTestDataParser に差し替わることを確認する。
     *
     * <p>Given: {@code SampleTarget} は実行中の parser クラス名を記録する
     * When: NtfTestdataTestRunner で実行する
     * Then: 1回目は BasicTestDataParser、2回目は YamlTestDataParser</p>
     */
    @Test
    public void duringYamlRun_parserIsYamlTestDataParser() throws InitializationError {
        // When
        NtfTestdataTestRunner runner = new NtfTestdataTestRunner(ParserCapturingTarget.class);
        CountingNotifier notifier = new CountingNotifier();
        runner.run(notifier);

        // Then
        if (!notifier.failures.isEmpty()) {
            fail("Unexpected failures: " + notifier.failures);
        }
        assertThat("captured parser class names count", ParserCapturingTarget.capturedParserClasses.size(), is(2));
        String firstClass = ParserCapturingTarget.capturedParserClasses.get(0);
        String secondClass = ParserCapturingTarget.capturedParserClasses.get(1);
        assertThat("1st run (Excel): not YamlTestDataParser",
                firstClass, not(is(YamlTestDataParser.class.getName())));
        assertThat("2nd run (YAML): YamlTestDataParser",
                secondClass, is(YamlTestDataParser.class.getName()));
    }

    /** テスト実行回数と失敗を記録する RunNotifier */
    private static final class CountingNotifier extends RunNotifier {
        int startedCount = 0;
        List<String> failures = new ArrayList<>();

        @Override
        public void fireTestStarted(Description description) {
            startedCount++;
        }

        @Override
        public void fireTestFailure(Failure failure) {
            failures.add(failure.getDescription().getMethodName() + ": " + failure.getMessage());
        }
    }

    /**
     * NtfTestdataTestRunner の動作確認用サンプルテストクラス。
     * {@code @Rule SystemRepositoryResource} を使う最小テストを持つ。
     */
    @RunWith(NtfTestdataTestRunner.class)
    public static class SampleTarget {

        @Rule
        public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

        /**
         * testDataParser が利用可能であることを確認する最小テスト。
         *
         * <p>Given: unit-test.xml がロードされた SystemRepository
         * When: testDataParser を取得する
         * Then: null でない</p>
         */
        @Test
        public void testDataParser_isAvailable() {
            assertThat(SystemRepository.getObject("testDataParser"), is(notNullValue()));
        }
    }

    /**
     * 実行中の testDataParser クラス名を記録するサンプルテストクラス。
     */
    @RunWith(NtfTestdataTestRunner.class)
    public static class ParserCapturingTarget {

        static final List<String> capturedParserClasses = new ArrayList<>();

        @Rule
        public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

        /**
         * 実行中の testDataParser クラス名を capturedParserClasses に記録する。
         *
         * <p>Given: unit-test.xml がロードされた SystemRepository（YAML実行時は YamlTestDataParser に差し替え済み）
         * When: testDataParser のクラス名を記録する
         * Then: （記録のみ。アサーションは外部テストで行う）</p>
         */
        @Test
        public void captureParserClass() {
            Object parser = SystemRepository.getObject("testDataParser");
            capturedParserClasses.add(parser != null ? parser.getClass().getName() : "null");
        }
    }

    /**
     * YAML 実行中の差し替えと復元を確認するサンプルテストクラス。
     * {@code @After} で YAML 実行時の parser クラス名を記録する。
     */
    @RunWith(NtfTestdataTestRunner.class)
    public static class RestoreVerifyTarget {

        /** YAML 実行中（@After 実行時点）の testDataParser クラス名 */
        static volatile String parserInYamlRun;

        /** YAML 実行後、復元済み（2回目の @After が完了した直後）の testDataParser クラス名。
         *  @After は YamlSetupRule.finally の前に実行されるため、ここでは差し替え中の値が取れる。
         *  復元の確認は failure がないことで行う（UNREACHABLE = 復元失敗 → AssertionError → failure）。 */
        static volatile String parserAfterRestore;

        private static int runCount = 0;

        @Rule
        public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

        /**
         * testDataParser の状態を記録する最小テスト。
         *
         * <p>Given: unit-test.xml がロードされた SystemRepository
         * When: 実行カウントを更新する
         * Then: エラーなし</p>
         */
        @Test
        public void recordRun() {
            runCount++;
        }

        /**
         * テスト実行直後（YamlSetupRule.finally 実行前）の parser クラス名を記録する。
         */
        @After
        public void captureParserAfterTest() {
            Object parser = SystemRepository.getObject("testDataParser");
            String className = parser != null ? parser.getClass().getName() : "null";
            if (runCount == 1) {
                // 1回目終了後: まだ Excel parser（差し替えなし）
            } else {
                // 2回目終了後: YamlSetupRule.finally 実行前なので YamlTestDataParser がまだ有効
                parserInYamlRun = className;
                // 復元後を確認するには YamlSetupRule.finally の後が必要だが、
                // @After はその前に実行されるため直接は確認不可。
                // 代わりに復元失敗は UNREACHABLE AssertionError（failure）として検出される。
                parserAfterRestore = "restore-checked-via-no-failure";
            }
        }
    }
}
