package nablarch.test.core.db;

import nablarch.core.db.connection.ConnectionFactory;
import nablarch.core.db.connection.TransactionManagerConnection;
import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.db.statement.SqlRow;
import nablarch.core.db.transaction.SimpleDbTransactionManager;
import nablarch.core.repository.SystemRepository;
import nablarch.core.transaction.TransactionFactory;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.test.Assertion;
import nablarch.test.TestSupport;
import nablarch.test.event.TestEventDispatcher;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nablarch.core.util.Builder.concat;
import static nablarch.core.util.StringUtil.isNullOrEmpty;


/**
 * データベースアクセス自動テスト用基底クラス。<br/>
 * データベースアクセスクラスの自動テストを行う場合には、本クラスを継承しテストクラスを作成する。<br/>
 * 本クラス以外の基底クラスを継承しなければならない場合は、<br/>
 * 本クラスのインスタンスを生成し処理を委譲することで代替可能である。
 *
 * @author Tsuyoshi Kawasaki
 */
@Published
public class DbAccessTestSupport extends TestEventDispatcher {

    /** データベーストランザクション名を取得する為のキー */
    public static final String TRANSACTIONS_KEY = "dbAccessTest.dbTransactionName";

    /** テストクラス用トランザクション名 */
    public static final String DB_TRANSACTION_FOR_TEST = "testTran";

    /** テスティングフレームワーク用トランザクション名 */
    public static final String DB_TRANSACTION_FOR_TEST_FW = "testFwTran";

    /** トランザクションマネージャ */
    private final List<SimpleDbTransactionManager> transactionManagers = new ArrayList<SimpleDbTransactionManager>();

    /** テストサポート */
    private final TestSupport testSupport;

    /**
     * デフォルトコンストラクタ<br/>
     * サブクラスからの呼び出しを想定している。<br/>
     * サブクラス以外から本クラスを使用する場合は、{@link DbAccessTestSupport#DbAccessTestSupport(Class)}を使用すること。
     */
    protected DbAccessTestSupport() {
        testSupport = new TestSupport(getClass());
    }


    /**
     * コンストラクタ
     *
     * @param testClass テストクラス（テスト対象クラスではない）
     */
    public DbAccessTestSupport(Class<?> testClass) {
        testSupport = new TestSupport(testClass);
    }

    /**
     * コンストラクタ
     *
     * @param testSupport テストサポート
     */
    public DbAccessTestSupport(TestSupport testSupport) {
        this.testSupport = testSupport;
    }

    /**
     * データベースアクセスクラスのテスト用にトランザクションを開始する。<br/>
     * 開始対象のトランザクション名は、設定ファイルより取得する。<br/>
     * 複数のトランザクションを開始する場合には、カンマ(",")区切りで複数のトランザクション名を設定する。<br/>
     * 設定ファイル例:<br/>
     * <p/>
     * <pre>
     * dbAccessTest.dbTransactionName = transaction-name1,transaction-name2
     * </pre>
     * <p/>
     * デフォルトのトランザクション(nablarch.core.db.connection.DbConnectionContext#getConnection()で取得されるトランザクション)は、<br/>
     * 設定ファイルの記述の有無に関わらず開始される。<br/>
     * デフォルトのトランザクションのみを使用する場合は、設定ファイルへの記述は不要である。
     */
    @Before
    public void beginTransactions() {
        // デフォルトトランザクション
        transactionManagers.add(getDefaultManager());

        // 設定ファイルに登録されたトランザクション
        String tranKeys = SystemRepository.getString(TRANSACTIONS_KEY);
        if (!isNullOrEmpty(tranKeys)) {
            for (String key : tranKeys.split(",")) {
                SimpleDbTransactionManager manager = (SimpleDbTransactionManager) SystemRepository
                        .getObject(key);
                if (manager == null) {
                    throw new IllegalStateException("can't get from repository name=[" + key + "]");
                }
                transactionManagers.add(manager);
            }
        }

        // トランザクション開始
        for (SimpleDbTransactionManager manager : transactionManagers) {
            manager.beginTransaction();
        }
    }

    /**
     * デフォルトのトランザクションマネージャを取得する。
     *
     * @return デフォルトのトランザクションマネージャ
     */
    private SimpleDbTransactionManager getDefaultManager() {
        SimpleDbTransactionManager defaultManager = new SimpleDbTransactionManager();
        defaultManager.setConnectionFactory((ConnectionFactory) SystemRepository.getObject("connectionFactory"));
        defaultManager.setTransactionFactory((TransactionFactory) SystemRepository.getObject("transactionFactory"));
        return defaultManager;
    }

    /** コミットを実行する。 */
    public void commitTransactions() {
        for (SimpleDbTransactionManager manager : transactionManagers) {
            manager.commitTransaction();
        }
    }

    /** トランザクションをロールバックする。 */
    public void rollbackTransactions() {
        for (SimpleDbTransactionManager transactionManager : transactionManagers) {
            transactionManager.rollbackTransaction();
        }
    }

    /** トランザクションを終了する。 */
    @After
    public void endTransactions() {
        for (SimpleDbTransactionManager manager : transactionManagers) {
            try {
                manager.endTransaction();
            } catch (RuntimeException t) {
                System.err.println("endTransactions failed.");
                t.printStackTrace();
            }
        }
        // 管理中のトランザクションマネージャをクリア
        transactionManagers.clear();
    }

    /**
     * データベースにデータを投入する。<br/>
     *
     * @param sheetName シート名
     */
    public void setUpDb(String sheetName) {
        setUpDb(sheetName, null);
    }

    /**
     * データベースにデータを投入する。<br/>
     *
     * @param sheetName シート名
     * @param groupId   グループID
     */
    public void setUpDb(String sheetName, String groupId) {

        // 準備データのセットアップ前に、targetクラスで使用するデータベーストランザクションを
        // 全てロールバックする。
        rollbackTransactions();

        // 複数のテーブルにデータを登録する。
        final List<TableData> allTables = testSupport.getSetupTableData(sheetName, groupId);

        if (allTables.isEmpty()) {
            return;
        }

        new TransactionTemplateInternal(DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(TransactionManagerConnection conn) {

                // 子テーブルから削除
                List<TableData> orderedForDeletion = TableDataSorter.reversed(allTables, conn);
                for (TableData tableToDelete : orderedForDeletion) {
                    tableToDelete.deleteData(conn);
                }
                // 親テーブルから挿入
                List<TableData> orderedForInsertion = TableDataSorter.sort(allTables, conn);
                for (TableData tableToInsert : orderedForInsertion) {
                    tableToInsert.insertData(conn);
                }
            }
        }
        .execute();
    }

    /**
     * ThreadContextに値を設定する。<br/>
     *
     * @param sheetName 取得元シート名
     * @param id        取得元ID
     */
    public void setThreadContextValues(String sheetName, String id) {
        testSupport.setThreadContextValues(sheetName, id);
    }

    /**
     * SqlResultSetの値とExcelファイルに記載したデータの比較を行う。<br/>
     * 検索系テスト実行結果の検索結果確認に使用する。<br/>
     *
     * @param message   比較失敗時のメッセージ
     * @param sheetName 期待値を格納したシート名
     * @param id        シート内のデータを特定するためのID
     * @param actual    実際の値
     */
    public void assertSqlResultSetEquals(String message, String sheetName, String id,
                                         SqlResultSet actual) {
        List<Map<String, String>> expected = testSupport.getListMap(sheetName, id);
        Assertion.assertSqlResultSetEquals(message, expected, actual);
    }

    /**
     * SqlRowの値とExcelファイルに記載したデータの比較を行う。<br/>
     * 検索系テスト実行結果の検索結果確認に使用する。<br/>
     *
     * @param message   比較失敗時のメッセージ
     * @param sheetName 期待値を格納したシート名
     * @param id        シート内のデータを特定するためのID
     * @param actual    実際の値
     */
    public void assertSqlRowEquals(String message, String sheetName, String id, SqlRow actual) {
        Map<String, String> expected = testSupport.getMap(sheetName, id);
        Assertion.assertSqlRowEquals(message, expected, actual);
    }

    /**
     * List-Map形式でデータを取得する。<br/>
     *
     * @param sheetName シート名
     * @param id        ID
     * @return List-Map形式のデータ
     */
    public List<Map<String, String>> getListMap(String sheetName, String id) {
        return testSupport.getListMap(sheetName, id);
    }

    /**
     * List-Map形式でデータを取得する。<br/>
     * HTTPパラメータと同じ形式で取得できる（Mapの値がString[]となる）。
     *
     * @param sheetName シート名
     * @param id        ID
     * @return List-Map形式のデータ
     * @see nablarch.fw.web.HttpRequest#getParamMap()
     */
    public List<Map<String, String[]>> getListParamMap(String sheetName, String id) {
        return testSupport.getListParamMap(sheetName, id);
    }

    /**
     * List-Map形式でデータを取得する。<br/>
     * HTTPパラメータと同じ形式で取得できる（Mapの値がString[]となる）。
     *
     * @param sheetName シート名
     * @param id        ID
     * @return Map形式のデータ
     * @see nablarch.fw.web.HttpRequest#getParamMap()
     */
    public Map<String, String[]> getParamMap(String sheetName, String id) {
        List<Map<String, String[]>> list = getListParamMap(sheetName, id);
        if (list.size() > 1) {
            throw new IllegalArgumentException(concat(
                    "list size must be one. but was [", list.size(), "]",
                    "sheet=[", sheetName, "], id=[", id, "]"));
        }
        if (list.size() == 0) {
            return new HashMap<String, String[]>(0);
        }
        return list.get(0);
    }

    /**
     * データベースのテーブルの値とExcelファイルに記載した値の比較を行う。<br/>
     * 更新系テスト実行後の更新結果確認用に使用する。
     * テストクラスと同一のパッケージに存在するテストデータファイルから、 期待値を読み取り実際のテーブルと比較を行う。
     *
     * @param sheetName 期待値を格納したシート名
     */
    public void assertTableEquals(String sheetName) {
        assertTableEquals("", sheetName, null);
    }

    /**
     * テーブルの比較を行う。<br/>
     * テストクラスと同一のパッケージに存在するテストデータファイルから、
     * 期待値を読み取り実際のテーブルと比較を行う。
     *
     * @param message           比較失敗時のメッセージ
     * @param sheetName         期待値を格納したシート名
     * @param failIfNoDataFound データが存在しない場合に例外とするかどうか
     * @throws IllegalArgumentException 期待値のデータが存在せず、failIfNoDataFoundが真の場合
     */
    public void assertTableEquals(String message,
                                  String sheetName,
                                  boolean failIfNoDataFound) throws IllegalArgumentException {
        assertTableEquals(message, sheetName, null, failIfNoDataFound);
    }

    /**
     * テーブルの比較を行う。<br/>
     * テストクラスと同一のパッケージに存在するテストデータファイルから、
     * 期待値を読み取り実際のテーブルと比較を行う。
     *
     * @param sheetName 期待値を格納したシート名
     * @param groupId   グループID（オプション）
     * @throws IllegalArgumentException 期待値のデータが存在しない場合
     */
    public void assertTableEquals(String sheetName, String groupId) throws IllegalArgumentException {
        assertTableEquals("", sheetName, groupId);
    }

    /**
     * テーブルの比較を行う。<br/>
     * テストクラスと同一のパッケージに存在するテストデータファイルから、
     * 期待値を読み取り実際のテーブルと比較を行う。
     *
     * @param message           比較失敗時のメッセージ
     * @param groupId           グループID（オプション）
     * @param sheetName         期待値を格納したシート名
     * @throws IllegalArgumentException 期待値のデータが存在しない場合
     */
    public void assertTableEquals(String message, String sheetName, String groupId) throws IllegalArgumentException {
        assertTableEquals(message, sheetName, groupId, true);
    }

    /**
     * テーブルの比較を行う。<br/>
     * テストクラスと同一のパッケージに存在するテストデータファイルから、
     * 期待値を読み取り実際のテーブルと比較を行う。
     *
     * @param message           比較失敗時のメッセージ
     * @param groupId           グループID（オプション）
     * @param sheetName         期待値を格納したシート名
     * @param failIfNoDataFound データが存在しない場合に例外とするかどうか
     * @throws IllegalArgumentException 期待値のデータが存在せず、failIfNoDataFoundが真の場合
     */
    public void assertTableEquals(String message,
                                  String sheetName,
                                  String groupId,
                                  boolean failIfNoDataFound) throws IllegalArgumentException {

        List<TableData> expected = testSupport.getExpectedTableData(sheetName, groupId);
        if (expected.isEmpty() && failIfNoDataFound) {
            groupId = StringUtil.nullToEmpty(groupId);
            message = StringUtil.nullToEmpty(message);
            throw new IllegalArgumentException(message
                     + " no table data found in the specified sheet."
                     + " sheet=[" + sheetName + "]. groupId=[" + groupId + "]");
        }
        Assertion.assertTableEquals(message, expected);
    }

    /**
     * テストサポートクラスを返却する。
     *
     * @return テストクラス
     */
    public TestSupport getTestSupport() {
        return testSupport;
    }
}
