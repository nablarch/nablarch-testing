package nablarch.test.core.db;

import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.connection.TransactionManagerConnection;
import nablarch.core.db.statement.SqlLogFormatter;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.StringUtil;
import nablarch.test.NablarchTestUtils;
import nablarch.test.event.TestEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static nablarch.core.util.Builder.concat;

/**
 * マスタデータ復旧クラス。<br/>
 * <p>
 * テスト内でマスタデータの書き換えが発生した場合、その復旧を行う。
 * テスト実行中、{@link SqlLogWatchingFormatter}から発行されたSQL文の通知を受ける。
 * マスタデータの書き換えが発生していた場合、次のテストメソッドが実行される前に、
 * マスタデータテーブルの復旧をTableDuplicatorに依頼する。
 * 事前に準備されたマスタデータ復旧用のスキーマからデータをコピーすることにより、
 * マスタデータの復旧を行う。<br/>
 * マスタデータを復旧する順序はテーブルの依存関係（FK)に則り行われるが、
 * コンポーネント定義に{@literal nablarch.suppress-table-sort}というキーで
 * 真偽値{@code true}が設定されていた場合はtablesTobeWatchedに設定された順序でマスタデータのインサート、逆順でデリートを行う。
 * </p>
 * <p>
 * 更新SQLかどうかの判定するために厳密なSQL解析は行っていない。
 * 更新SQLと見做せる条件のうちいずれかを満たしていればマスタデータ復旧を行う。
 * これは以下の理由による。
 * <ul>
 * <li>厳密なSQL解析はコストが高くテスト実行速度を低下させる。</li>
 * <li>マスタデータが更新されることは稀であり、誤検知による復旧が発生してもコストが低い。</li>
 * </ul>
 * </p>
 *
 * @author T.Kawasaki
 */
public class MasterDataRestorer extends TestEventListener.Template {

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(MasterDataRestorer.class);

    /** デフォルトの更新SQLキーワード */
    private static final List<String> DEFAULT_KEYWORDS = Arrays.asList(
            "INSERT", "DELETE", "UPDATE", "MERGE", "TRUNCATE"
    );

    /** 更新とみなされるSQLキーワード */
    private List<String> updateSqlKeywords = DEFAULT_KEYWORDS;

    /** バックアップ元のスキーマ名 */
    private String backupSchema;

    /** 監視対象テーブル一覧 */
    private List<String> tablesTobeWatched;

    /**
     * 更新されたテーブル一覧を取得する。<br/>
     * テスト中に発行されたSQLに、監視対象テーブルを更新した可能性のある
     * SQL文が存在した場合、そのテーブルの一覧をtablesTobeWatchedの順でソートし、返却する。
     * このソート処理はテーブルの依存関係によるソートをしない場合に、
     * テーブルの復旧順序を制御するために行っている。
     *
     * @param executedSql 実行されたSQL文
     * @return 更新されたテーブル一覧
     */
    Set<String> getUpdatedTables(List<String> executedSql) {
        Set<String> updated = new HashSet<String>(tablesTobeWatched.size());
        // 発行されたSQL全件チェック
        for (String sql : executedSql) {
            String upperSql = sql.toUpperCase();
            LOGGER.logTrace(upperSql);
            // 監視対象テーブル全件チェック
            for (String table : tablesTobeWatched) {
                if (updated.contains(table)) {
                    continue;  // 既に更新を検知したテーブルはチェック不要
                }
                if (containsUpdateSqlKeyword(table, upperSql)) {
                    LOGGER.logInfo(concat(
                            "Master table modification detected. ",
                            "table=[", table, "], sql=[", upperSql, "]"));
                    updated.add(table);
                }
            }
        }
        return sortTableByTablesTobeWatched(updated);
    }

    /**
     * setの中身をtablesTobeWatchedの順でソートしたものを返す。
     *
     * @param originTable 未ソートのテーブルセット
     * @return tablesTobeWatched順でソートされたテーブルセット
     */
    private Set<String> sortTableByTablesTobeWatched(Set<String> originTable) {
        Set<String> sortedUpdatedTables = new LinkedHashSet<String>();
        for(String table: tablesTobeWatched){
            if(originTable.contains(table)){
                sortedUpdatedTables.add(table);
            }
        }
        return sortedUpdatedTables;
    }

    /** マスタデータ復旧クラスのキー */
    static final String MASTER_DATA_RESTORER_KEY = "masterDataRestorer";

    /**
     * {@inheritDoc}
     * <p/>
     * 実行されたSQLでマスタデータを更新するものがあれば、
     * マスタデータ復旧を行う。
     */
    @Override
    public void afterTestMethod() {
        List<String> executedSql = SqlLogWatchingFormatter.getExecuted();
        if (executedSql.isEmpty()) {
            return;
        }
        Set<String> updatedTables = getUpdatedTables(executedSql);
        TableDuplicator duplicator = new TableDuplicator(updatedTables, backupSchema);
        duplicator.restoreAll();
        SqlLogWatchingFormatter.begin();
    }


    /**
     * 更新とおぼしきSQLキーワードが含まれているか判定する。
     *
     * @param targetTable テーブル名
     * @param logMessage  ログメッセージ
     * @return 判定結果
     */
    boolean containsUpdateSqlKeyword(String targetTable, String logMessage) {
        for (String keyword : updateSqlKeywords) {
            if (isSuspiciousSql(targetTable, logMessage, keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 更新とおぼしきSQLキーワードが含まれているか判定する。
     *
     * @param targetTable テーブル名
     * @param logMessage  ログメッセージ
     * @param keyword     キーワード
     * @return 判定結果
     */
    private boolean isSuspiciousSql(String targetTable, String logMessage, String keyword) {

        int keywordIdx = logMessage.indexOf(keyword);
        if (keywordIdx == -1) {
            return false;  // キーワードが現れていない。
        }

        int tableNameIdx = logMessage.indexOf(targetTable, keywordIdx + keyword.length());

        // キーワード以降にテーブル名が存在していた場合は、リストア対象とする。
        return tableNameIdx != -1;
    }


    /**
     * 監視対象テーブル名を設定する。
     *
     * @param tablesTobeWatched 監視対象テーブル
     */
    public void setTablesTobeWatched(List<String> tablesTobeWatched) {
        this.tablesTobeWatched = NablarchTestUtils.toUpperCase(tablesTobeWatched);
    }


    /**
     * バックアップスキーマを設定する。
     *
     * @param backupSchema バックアップスキーマを設定する。
     */
    public void setBackupSchema(String backupSchema) {
        this.backupSchema = backupSchema;
    }

    /**
     * 更新とみなされるSQLキーワードを設定する。
     * SQLキーワード文字列末尾には半角スペースが付与される。
     *
     * @param updateSqlKeywords 更新とみなされるSQLキーワード
     */
    public void setUpdateSqlKeywords(List<String> updateSqlKeywords) {
        List<String> upper = new ArrayList<String>();
        for (String keyword : updateSqlKeywords) {
            upper.add(keyword.trim().toUpperCase() + " ");
        }
        this.updateSqlKeywords = upper;
    }

    /**
     * 発行されたSQL文を監視するSqlLogFormatterサブクラス<br/>
     * <p>
     * 本クラスへの出力された更新系SQLログは、全てnablarch.test.core.db.MasterDataRestorerに通知される。
     * nablarch.test.core.db.MasterDataRestorerはログ出力からマスタデータ更新を検知する。
     * </p>
     * app-log.properties設定例を以下に示す。
     * <code>
     * <pre>
     * sqlLogFormatter.className=nablarch.test.core.db.MasterDataRestorer$SqlLogWatchingFormatter
     * </pre>
     * </code>
     *
     * @see SqlLogFormatter#startExecuteUpdate(String, String, String)
     * @see SqlLogFormatter#startExecuteBatch(String, String, String)
     * @see SqlLogFormatter#startExecute(String, String, String)
     */
    public static class SqlLogWatchingFormatter extends SqlLogFormatter {

        /** 発行されたSQL */
        private static List<String> executedSql = new ArrayList<String>();

        /** {@inheritDoc} */
        @Override
        public String startExecuteBatch(String methodName, String sql, String additionalInfo) {
            register(sql);
            return super.startExecuteBatch(methodName, sql, additionalInfo);
        }

        /** {@inheritDoc} */
        @Override
        public String startExecuteUpdate(String methodName, String sql, String additionalInfo) {
            register(sql);
            return super.startExecuteUpdate(methodName, sql, additionalInfo);
        }

        /** {@inheritDoc} */
        @Override
        public String startExecute(String methodName, String sql, String additionalInfo) {
            register(sql);
            return super.startExecute(methodName, sql, additionalInfo);
        }

        /**
         * SQL文を登録する。
         * @param sql SQL文
         */
        static void register(String sql) {
            executedSql.add(sql);
        }
        
        /**
         * 発行されたSQL文を取得する。
         * @return 発行されたSQL文
         */
        static List<String> getExecuted() {
            return executedSql;

        }

        /**
         * SQL文の監視を開始する。
         */
        static void begin() {
            executedSql = new ArrayList<String>();
        }

    }

    /**
     * テーブル複製クラス<br/>
     * 指定されたテーブル全てを、コピー元スキーマからコピー先スキーマへコピーする。
     *
     * @author T.Kawasaki
     */
    static class TableDuplicator {

        /** 複製対象テーブル一覧 */
        private final Set<String> targetTableNames;

        /** コピー元スキーマ名 */
        private final String sourceSchema;

        /** コピー先スキーマ名 */
        private final String destinationSchema;

        /**
         * コンストラクタ
         *
         * @param targetTableNames  複製対象テーブル一覧
         * @param sourceSchema      コピー元スキーマ名
         * @param destinationSchema コピー先スキーマ名
         */
        TableDuplicator(Set<String> targetTableNames, String sourceSchema, String destinationSchema) {
            this.targetTableNames = targetTableNames;
            this.sourceSchema = sourceSchema;
            this.destinationSchema = destinationSchema;
        }

        /**
         * コンストラクタ
         *
         * @param targetTableNames 複製対象テーブル一覧
         * @param sourceSchema     コピー元スキーマ名
         */
        TableDuplicator(Set<String> targetTableNames, String sourceSchema) {
            this(targetTableNames, sourceSchema, "");
        }

        /** 指定されたテーブルをバックアップスキーマから複製する。 */
        void restoreAll() {
            if (targetTableNames.isEmpty()) {
                return;
            }
            new TransactionTemplateInternal(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
                @Override
                protected void doInTransaction(TransactionManagerConnection conn) {
                    List<String> orderedForInsertion = getSortTableNames(targetTableNames, conn);
                    List<String> orderedForDeletion = new ArrayList<String>(orderedForInsertion);
                    Collections.reverse(orderedForDeletion);

                    for (String tableName : orderedForDeletion) {
                        delete(tableName, conn);
                    }

                    for (String tableName : orderedForInsertion) {
                        copy(tableName, conn);
                    }
                }
            }
            .execute();
        }

        /**
         * テーブル名一覧をソートする。
         *
         * @param orig ソート対象となるリスト
         * @param conn コネクション
         * @return ソート後のリスト
         */
        List<String> getSortTableNames(Collection<String> orig, TransactionManagerConnection conn) {
            TableDataSorter sorter = new TableDataSorter(conn.getConnection(), destinationSchema);
            return sorter.sortTableNamesByFK(new ArrayList<String>(orig));
        }


        /**
         * テーブルを削除する。
         *
         * @param tableName 削除対象のテーブル名
         * @param conn コネクション
         */
        private void delete(String tableName, AppDbConnection conn) {
            String destination = getDestinationTableName(tableName);
            // 複製先テーブルを全件削除
            deleteTable(destination, conn);
        }

        /**
         * テーブルレコードのコピーを行う。
         * 指定した名前のテーブルのレコードをコピー元のスキーマからコピー先のスキーマへコピーする
         * @param tableName コピー対象となるテーブル名
         * @param conn コネクション
         */
        private void copy(String tableName, AppDbConnection conn) {
            String source = getSourceTableName(tableName);
            String destination = getDestinationTableName(tableName);
            // 複製元テーブルから複製先テーブルへデータをコピー
            copyAllRecords(source, destination, conn);
            LOGGER.logDebug(concat(
                    " table [", destination, "] was overwritten ",
                    "from [", source, "]"));
        }

        /**
         * 複製元テーブル名を取得する。
         *
         * @param tableName テーブル名
         * @return 複製元スキーマ名.テーブル名（デフォルトスキーマの場合はテーブル名のみ）
         */
        private String getSourceTableName(String tableName) {
            return getTableWithSchema(sourceSchema, tableName);
        }

        /**
         * 複製先テーブル名を取得する。
         *
         * @param tableName テーブル名
         * @return 複製先スキーマ名.テーブル名（デフォルトスキーマの場合はテーブル名のみ）
         */
        private String getDestinationTableName(String tableName) {
            return getTableWithSchema(destinationSchema, tableName);
        }

        /**
         * スキーマ名付きテーブル名を取得する。
         *
         * @param schema    スキーマ名
         * @param tableName テーブル名
         * @return スキーマ名.テーブル名（デフォルトスキーマの場合はテーブル名のみ）
         */
        private String getTableWithSchema(String schema, String tableName) {
            return StringUtil.isNullOrEmpty(schema)
                    ? tableName
                    : concat(schema, ".", tableName);
        }


        /**
         * テーブル全件削除をする。
         *
         * @param tableName 削除対象テーブル名
         * @param conn      DB接続
         * @return 削除件数
         */
        private int deleteTable(String tableName, AppDbConnection conn) {
            String deleteSql = concat(
                    "delete ",
                    "from ",
                    tableName);
            return conn.prepareStatement(deleteSql).executeUpdate();
        }

        /**
         * バックアップスキーマからデータを復旧する。
         *
         * @param source 復旧元テーブル名
         * @param dest   復旧対象テーブル名
         * @param conn   DB接続
         * @return 復旧件数
         */
        private int copyAllRecords(String source, String dest, AppDbConnection conn) {
            String restoreSql = concat(
                    "insert into ",
                    dest, " ",
                    "select ",
                    "* ",
                    "from ",
                    source
            );
            return conn.prepareStatement(restoreSql).executeUpdate();
        }
    }
}
