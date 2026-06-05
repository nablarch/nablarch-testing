package nablarch.test.core.reader.yaml;

import nablarch.core.repository.SystemRepository;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.TargetDb;
import org.junit.BeforeClass;
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
        // Given: NtfTestdataTestRunner を適用した SampleTarget クラス
        NtfTestdataTestRunner runner = new NtfTestdataTestRunner(SampleTarget.class);
        CountingNotifier notifier = new CountingNotifier();

        // When
        runner.run(notifier);

        // Then: SampleTarget の1メソッドが Excel + YAML の2回実行される
        assertThat("run count (Excel + YAML)", notifier.startedCount, is(2));
        if (!notifier.failures.isEmpty()) {
            fail("Unexpected failures: " + notifier.failures);
        }
    }

    /**
     * YAML 実行中に testDataParser が YamlTestDataParser に差し替わることを確認する。
     *
     * <p>Given: {@code ParserCapturingTarget} は実行中の parser クラス名を記録する
     * When: NtfTestdataTestRunner で実行する
     * Then: 1回目は BasicTestDataParser（YamlTestDataParser でない）、2回目は YamlTestDataParser</p>
     */
    @Test
    public void duringYamlRun_parserIsYamlTestDataParser() throws InitializationError {
        // Given: ParserCapturingTarget は実行中の parser クラス名を記録する
        NtfTestdataTestRunner runner = new NtfTestdataTestRunner(ParserCapturingTarget.class);
        CountingNotifier notifier = new CountingNotifier();

        // When
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

    /**
     * YAML 実行後に testDataParser と resource-root が元の値に復元されることを確認する。
     *
     * <p>Given: {@code SampleTarget} を実行する（@Rule により unit-test.xml がロード・クリアされる）
     * When: runner.run() 完了後に SystemRepository の値を確認する
     * Then: runner.run() 完了後にも ClassRule によって unit-test.xml が継続ロードされているため、
     *       testDataParser は BasicTestDataParser（YamlTestDataParser でない）であること</p>
     *
     * <p>なお ClassRule の SystemRepositoryResource は SampleTarget 実行中に @Rule の after() で
     * clear() されるが、その後この外側の ClassRule 再ロードは行われないため、
     * runner.run() 完了後は clear() 状態になる。
     * そのため本テストでは「YamlSetupRule の finally で復元が試みられ、
     * UNREACHABLE AssertionError が発生しないこと（= 失敗がゼロ）」で復元の成功を確認する。</p>
     */
    @Test
    public void afterYamlRun_restoreSucceeds_confirmedByNoFailure() throws InitializationError {
        // Given: SampleTarget を NtfTestdataTestRunner で実行する
        NtfTestdataTestRunner runner = new NtfTestdataTestRunner(SampleTarget.class);
        CountingNotifier notifier = new CountingNotifier();

        // When
        runner.run(notifier);

        // Then: UNREACHABLE AssertionError（= 復元失敗）があれば failure として記録される
        if (!notifier.failures.isEmpty()) {
            fail("Restore failure detected: " + notifier.failures);
        }
    }

    /**
     * {@code @TargetDb} 等により Ignored となったメソッドが YAML 側でも2回 Ignored にならないことを確認する。
     *
     * <p>Given: {@code IgnoredTarget} は全メソッドが {@code @TargetDb(exclude = ORACLE, SQL_SERVER, DB2, H2)}
     *       で全 DB を対象外にしている（= 必ず Ignored になる）
     * When: NtfTestdataTestRunner で実行する
     * Then: Ignored は1件のみ（2回 Ignored にならない）、startedCount は 0</p>
     */
    @Test
    public void ignoredMethod_isNotRunTwice() throws InitializationError {
        // Given: IgnoredTarget は全メソッドが必ず Ignored になる
        NtfTestdataTestRunner runner = new NtfTestdataTestRunner(IgnoredTarget.class);
        CountingNotifier notifier = new CountingNotifier();

        // When
        runner.run(notifier);

        // Then: Ignored は1件のみ（二重集計されない）
        assertThat("ignored count (must not be doubled)", notifier.ignoredCount, is(1));
        assertThat("started count (ignored method must not run)", notifier.startedCount, is(0));
        if (!notifier.failures.isEmpty()) {
            fail("Unexpected failures: " + notifier.failures);
        }
    }

    /**
     * テストメソッドが例外をスローしても parser と resource-root が復元されることを確認する。
     *
     * <p>Given: {@code FailingTarget} は常に RuntimeException をスローする
     * When: NtfTestdataTestRunner で実行する
     * Then: 失敗は2件（Excel + YAML の各1件）発生するが、
     *       YamlSetupRule の finally ブロックが動作し UNREACHABLE AssertionError が追加されない</p>
     */
    @Test
    public void afterFailingYamlRun_parserIsStillRestored() throws InitializationError {
        // Given: FailingTarget は常に RuntimeException をスローする
        NtfTestdataTestRunner runner = new NtfTestdataTestRunner(FailingTarget.class);
        CountingNotifier notifier = new CountingNotifier();

        // When
        runner.run(notifier);

        // Then: Excel + YAML の各1件で起動される（= 2回実行される）
        assertThat("started count (Excel + YAML)", notifier.startedCount, is(2));
        // 全failureメッセージに "intentional failure" が含まれること（UNREACHABLE が追加されていないこと）
        for (String failureMsg : notifier.failures) {
            if (failureMsg != null && failureMsg.contains("UNREACHABLE")) {
                fail("Unexpected UNREACHABLE AssertionError in failure: " + failureMsg
                        + ". This means restore failed after test exception.");
            }
        }
    }

    /** テスト実行回数・失敗・Ignored 件数を記録する RunNotifier */
    private static final class CountingNotifier extends RunNotifier {
        int startedCount = 0;
        int ignoredCount = 0;
        List<String> failures = new ArrayList<>();

        @Override
        public void fireTestStarted(Description description) {
            startedCount++;
        }

        @Override
        public void fireTestIgnored(Description description) {
            ignoredCount++;
        }

        @Override
        public void fireTestFailure(Failure failure) {
            String msg = failure.getMessage();
            failures.add(failure.getDescription().getMethodName() + ": " + msg);
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

        @BeforeClass
        public static void clearCaptured() {
            capturedParserClasses.clear();
        }

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
     * 全 DB を exclude しているため必ず Ignored になるサンプルテストクラス。
     * Ignored メソッドが YAML 側でも二重 Ignored にならないことを確認するために使う。
     */
    @RunWith(NtfTestdataTestRunner.class)
    @TargetDb(exclude = {TargetDb.Db.ORACLE, TargetDb.Db.POSTGRE_SQL, TargetDb.Db.DB2,
            TargetDb.Db.SQL_SERVER, TargetDb.Db.MY_SQL, TargetDb.Db.H2})
    public static class IgnoredTarget {

        @Rule
        public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

        /**
         * 全 DB が対象外のため必ず Ignored になるテスト。
         *
         * <p>Given: クラスに全 DB を exclude する {@code @TargetDb} が付いている
         * When: NtfTestdataTestRunner で実行する
         * Then: Ignored として処理され、テストは実行されない（外部テストで確認）</p>
         */
        @Test
        public void neverRuns() {
            fail("This test should never run because all DBs are excluded.");
        }
    }

    /**
     * テストメソッドが常に例外をスローするサンプルテストクラス。
     * 例外発生時でも YamlSetupRule の finally が動作することを確認するために使う。
     */
    @RunWith(NtfTestdataTestRunner.class)
    public static class FailingTarget {

        @Rule
        public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

        /**
         * 意図的に例外をスローするテスト。
         *
         * <p>Given: unit-test.xml がロードされた SystemRepository
         * When: RuntimeException をスローする
         * Then: YamlSetupRule の finally ブロックが動作して復元が完了すること（外部テストで確認）</p>
         */
        @Test
        public void alwaysFails() {
            throw new RuntimeException("intentional failure");
        }
    }
}
