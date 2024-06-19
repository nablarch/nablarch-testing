package nablarch.test.event;

import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import nablarch.core.repository.SystemRepository;
import nablarch.core.util.annotation.Published;
import nablarch.test.RepositoryInitializer;

/**
 * テストイベントディスパッチャクラス。<br/>
 * テスト実行時における各種イベントを検知し、リポジトリに登録されたリスナーに通知する。
 * テストクラスは本クラスまたは本クラスのサブクラスを継承することで、
 * リスナーに自動的にイベント通知を行えるようになる。
 */
public abstract class TestEventDispatcher {

    /** テストイベントリスナを取得するためのキー */
    static final String TEST_EVENT_LISTENERS_KEY = "testEventListeners";

    /** 初回のテストかどうか */
    private static boolean first = true;

    /**
     * 静的初期化子で発生した例外。<br/>
     * この例外が設定されている場合、すなわち静的初期化子で例外が発生した場合は、
     * 本クラスのサブクラスは全てエラー（テスト失敗）となる。
     *
     * @see #checkErrorInStaticInitializer()
     */
    private static Throwable errorInStaticInitializer = null;

    static {
        initializeDefaultRepository(); // リポジトリ初期化
    }

    /**
     * ブートストラップの為のリポジトリ初期化を行う。<br/>
     * 本メソッドはクラスの静的初期化子から起動される。
     * <p>
     * {@link BeforeClass}でリポジトリの初期化を行うと、
     * 他の{@link BeforeClass}メソッドが先に呼ばれた場合に
     * リポジトリが初期化されていないという事態が発生しうる。
     * これを回避するために静的初期化子にてリポジトリの初期化を行う。
     * </p>
     * <p>
     * 静的初期化子起動時に例外・エラーが発生すると、初回クラスロード時には
     * {@link java.lang.ExceptionInInitializerError}が発生する。しかし、
     * 2回目以降のクラス生成時には{@link java.lang.NoClassDefFoundError}が発生するので、
     * 単独ではエラーになった理由がわからない。
     * 原因究明が容易になるよう、静的初期化子では例外・エラーを発生させないようにしている。
     * 例外・エラーが発生した場合は、これをキャッチして内部的に保持しておき、
     * 静的初期化子の実行は正常に終了させる。
     * テスト実行前に呼び出される{@link #dispatchEventOfBeforeTestClassAndBeforeSuit()}にて、
     * 保持しておいた発生した例外・エラーをスローする
     * （{@link #checkErrorInStaticInitializer()}）。
     * </p>
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-12.html#jls-12.4.2">
     *      The Java® Language Specification Java SE 17 Edition 12.4.2. Detailed Initialization Procedure</a>
     */
    private static void initializeDefaultRepository() {
        try {
            RepositoryInitializer.initializeDefaultRepository();
        } catch (Throwable e) {
            errorInStaticInitializer = e;
        }
    }

    /**
     * イベントリスナーの初期化を行う。
     *
     * @return 初期化されたリスナー
     */
    private static List<TestEventListener> getListeners() {
        @SuppressWarnings("unchecked")
        List<TestEventListener> registeredListeners =
                (List<TestEventListener>)
                        SystemRepository.getObject(TEST_EVENT_LISTENERS_KEY);
        return (registeredListeners == null)
                ? Collections.<TestEventListener>emptyList()
                : registeredListeners;

    }

    /** テスト名 */
    @Rule
    public final TestName testName = new TestName(); // SUPPRESS CHECKSTYLE  JUnit4の制約によりpublic

    /**
     * テストメソッド名を取得する。
     * サブクラスは、テストメソッド内で本メソッドを起動することで、
     * 実行中のテストメソッド名を取得できる。
     * <pre>
     * {@code
     *     @Test
     *     public void testSomething() {
     *         getTestName();   // returns "testSomething"
     *     }
     * }
     * </pre>
     *
     * @return 実行中のテストメソッド名
     */
    @Published
    protected final String getMethodName() {
        String name = testName.getMethodName();
        return (name == null) ? "" : name;
    }

    /** テストクラス前とテストスイート前のイベントをディスパッチする。 */
    @BeforeClass
    public static void dispatchEventOfBeforeTestClassAndBeforeSuit() {
        // 静的初期化子が成功したか確認する。
        checkErrorInStaticInitializer();

        if (first) {
            for (TestEventListener listener : getListeners()) {
                listener.beforeTestSuite();
            }
            first = false;
        }
        for (TestEventListener listener : getListeners()) {
            listener.beforeTestClass();
        }
    }

    /** テストメソッド前のイベントをディスパッチする。 */
    @Before
    public final void dispatchEventOfBeforeTestMethod() {
        for (TestEventListener listener : getListeners()) {
            listener.beforeTestMethod();
        }
    }

    /** テストメソッド後のイベントをディスパッチする。 */
    @After
    public final void dispatchEventOfAfterTestMethod() {
        List<TestEventListener> listeners = getListeners();
        for (int i = listeners.size() - 1; i >= 0; i--) {
            listeners.get(i).afterTestMethod();
        }
    }

    /** テストクラス終了後のイベントをディスパッチする。 */
    @AfterClass
    public static void dispatchEventOfAfterTestClass() {
        List<TestEventListener> listeners = getListeners();
        for (int i = listeners.size() - 1; i >= 0; i--) {
            listeners.get(i).afterTestClass();
        }
    }

    /**
     * 静的初期化子でエラーが発生していないかどうか確認する。
     *
     * @throws Error 静的初期化子でエラーが発生していた場合
     * @see #initializeDefaultRepository
     */
    private static void checkErrorInStaticInitializer() throws Error {
        if (errorInStaticInitializer == null) {
            return;
        }
        String additionalErrorMessage
                = "failed initializing repository. see nested exception message for detail. "
                + "and check configuration files.";
        throw new Error(additionalErrorMessage, errorInStaticInitializer);
    }
}
