package nablarch.test.core.db;

import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.connection.TransactionManagerConnection;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlRow;
import nablarch.core.util.BinaryUtil;
import nablarch.core.util.Builder;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.test.NablarchTestUtils;

import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static nablarch.core.util.Builder.join;

/**
 * テーブルデータ保持クラス。<br/>
 * テストデータのテーブルデータを保持する。
 *
 * @author Hisaaki Sioiri
 */
@Published(tag = "architect")
public class TableData implements Cloneable {

    /** JDBCタイムスタンプエスケープ形式 */
    private static final String JDBC_TIMESTAMP_ESCAPE = "yyyy-MM-dd HH:mm:ss.SSS";

    /** デフォルトの日付フォーマット */
    private static final String DEFAULT_DATE_FORMAT = "yyyyMMddHHmmssSSS";

    /** データベース情報 */
    private DbInfo dbInfo;

    /** テーブルのデータ */
    private List<SqlRow> contents = new ArrayList<SqlRow>(0);

    /** テーブル名 */
    private String tableName;

    /** カラム名一覧 */
    private String[] columnNames;

    /** 日付型のフォーマット用 */
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat(DEFAULT_DATE_FORMAT);

    /** JDBCタイムスタンプエスケープ形式のDateFormat */
    private final DateFormat jdbcTimestamp = new SimpleDateFormat(JDBC_TIMESTAMP_ESCAPE);

    /** データベースデフォルト値実装クラス */
    private DefaultValues defaultValues = new BasicDefaultValues();

    /** デフォルトコンストラクタ */
    public TableData() {
    }

    /**
     * コンストラクタ
     *
     * @param tableName   テーブル名
     * @param columnNames カラム名
     * @param dbInfo      データベース情報
     */
    public TableData(DbInfo dbInfo, String tableName, String[] columnNames) {
        setTableName(tableName);
        setColumnNames(columnNames);
        this.dbInfo = dbInfo;
    }

    /**
     * コンストラクタ
     *
     * @param tableName     テーブル名
     * @param columnNames   カラム名
     * @param defaultValues デフォルト値
     * @param dbInfo        データベース情報
     */
    public TableData(DbInfo dbInfo, String tableName, String[] columnNames,
                     DefaultValues defaultValues) {
        this(dbInfo, tableName, columnNames);
        this.defaultValues = defaultValues;
    }

    /**
     * テーブル名を設定する。<br/>
     *
     * @param name テーブル名
     */
    public void setTableName(String name) {
        tableName = name.trim().toUpperCase();
    }

    /** 本オブジェクトが保持するデータでＤＢを更新する。 */
    public void replaceData() {

        (new TransactionTemplateInternal(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(TransactionManagerConnection conn) {
                replaceDataInTransaction(conn);
            }
        }).execute();
    }

    /**
     * 本オブジェクトが保持するデータでＤＢを更新する。<br/>
     * このメソッド使用する場合、呼び出し前にメソッド外部でトランザクションが開始されていなければならない。
     *
     * @param conn コネクション
     */
    void replaceDataInTransaction(AppDbConnection conn) {
        deleteData(conn);
        insertData(conn);
    }

    /**
     * テーブルのデータを削除する。
     *
     * @param connection コネクション
     */
    void deleteData(AppDbConnection connection) {
        SqlPStatement delete = connection.prepareStatement("DELETE FROM " + tableName);
        delete.executeUpdate();
    }

    /**
     * テーブルにデータを挿入する。
     *
     * @param connection コネクション
     */
    void insertData(AppDbConnection connection) {

        String[] nonComputedColumns = getNonComputedColumns();
        String insertStatement = createInsertStatement(nonComputedColumns);
        SqlPStatement insert = connection.prepareStatement(insertStatement);
        // データ登録
        for (int rowIndex = 0; rowIndex < contents.size(); rowIndex++) {
            int bindIndex = 1;
            SqlRow row = contents.get(rowIndex);
            for (String columnName : nonComputedColumns) {
                if (dbInfo.isBinaryTypeColumn(tableName, columnName)) {
                    byte[] data = null;
                    if (row.containsKey(columnName)) {
                        Object o = row.get(columnName);
                        if (o != null && o.toString().length() != 0) {
                            data = BinaryUtil.convertHexToBytes(o.toString());
                        }
                    } else {
                        data = BinaryUtil.convertHexToBytes((String) getDefaultValue(columnName));
                    }
                    insert.setBytes(bindIndex++, data);
                } else if (dbInfo.isNumberTypeColumn(tableName, columnName)) {
                    Object value = convert(row, columnName, rowIndex);
                    BigDecimal decimal = value == null ? null : new BigDecimal(value.toString());
                    insert.setBigDecimal(bindIndex++, decimal);
                } else if (dbInfo.isBooleanTypeColumn(tableName, columnName)) {
                        insert.setBoolean(bindIndex++,
                                row.containsKey(columnName) ? row.getBoolean(columnName) : (Boolean) getDefaultValue(columnName));
                }
                else {
                    Object value = convert(row, columnName, rowIndex);
                    insert.setObject(bindIndex++, value);
                }
            }
            insert.addBatch();
            if (insert.getBatchSize() % 100 == 0) {
                insert.executeBatch();
            }
        }
        // 一括してDB登録
        insert.executeBatch();
    }

    /**
     * {@link SqlPStatement#setObject(int, Object)}用に変換する。 元の値が存在しない場合（省略されている場合）、
     * {@link DefaultValues}を用いてデフォルト値を返却する。
     *
     * @param row        変換対象カラムを含む行
     * @param columnName 変換対象カラム名
     * @param index      全レコード中のインデックス番号
     * @return 変換後の値
     */
    private Object convert(SqlRow row, String columnName, int index) {

        if (!row.containsKey(columnName)) {
            // カラムが省略されている場合はデフォルト値を返却
            return getDefaultValue(columnName);
        }

        Object orig = row.get(columnName);
        if (orig == null) {
            return null;
        }
        if (dbInfo.isDateTypeColumn(tableName, columnName)) {
            try {
                return toTimestamp(orig);
            } catch (ParseException e) {
                throw new RuntimeException(Builder.concat(
                        "invalid date format. tableName = [", tableName, "]",
                        ":rowNo = [", (index + 1), "]", ":columnName = [", columnName, "]",
                        ":value = [", orig, "]")
                        , e);
            }
        }
        return orig.toString();
    }

    /**
     * タイムスタンプに変換する。
     *
     * @param orig 元のオブジェクト
     * @return 変換後のタイムスタンプ
     * @throws ParseException 解析に失敗した場合。
     */
    private Timestamp toTimestamp(Object orig) throws ParseException {
        String s = orig.toString();
        if (StringUtil.isNullOrEmpty(s)) {
            return null;
        }
        if (isJdbcTimestampFormat(s)) {
            return asJdbcTimestampEscape(s);
        }
        return asYyyyMMddHHmmssSSS(s);
    }

    /**
     * JDBCタイムスタンプエスケープ形式であるか判定する。
     *
     * @param str 判定対象文字列
     * @return 判定結果
     */
    private boolean isJdbcTimestampFormat(String str) {
        return str.length() > 4 && str.charAt(4) == '-';
    }

    /**
     * yyyyMMddHHmmssSSS形式でタイムスタンプに変換する。
     *
     * @param orig 変換対象文字列
     * @return タイムスタンプ
     * @throws ParseException 対象文字列がyyyyMMddHHmmssSSS形式に合致しない場合
     */
    private Timestamp asYyyyMMddHHmmssSSS(String orig) throws ParseException {
        Date d = dateFormatter.parse((orig + "00000000000000000").substring(0, 17));
        return new Timestamp(d.getTime());
    }

    /**
     * JDBCタイムスタンプエスケープ形式でタイムスタンプに変換する。
     * <p/>
     * JDBCタイムスタンプエスケープ形式としては、以下の形式をサポートする。
     * <ul>
     * <li>yyyy-[M]M-[d]d</li>
     * <li>yyyy-[M]M-[d]d HH:mm:ss[.SSS]</li>
     * </ul>
     * 
     * @param orig 変換対象文字列
     * @return タイムスタンプ
     * @throws ParseException 対象文字列がJDBCタイムスタンプエスケープ形式に合致しない場合
     */
    private Timestamp asJdbcTimestampEscape(String orig) throws ParseException {
        StringBuilder sb = new StringBuilder(orig);
        if (orig.indexOf(':') == -1) {
            // yyyy-MM-ddの場合に時刻を付与して正常に変換できるようにする
            sb.append(" 00:00:00.000");
        }
        return Timestamp.valueOf(sb.toString());
    }

    /**
     * デフォルト値を取得する。
     *
     * @param columnName カラム名
     * @return デフォルト値
     */
    private Object getDefaultValue(String columnName) {
        int columnType = dbInfo.getColumnType(tableName, columnName);
        int length = dbInfo.getColumnLength(tableName, columnName);
        return defaultValues.get(columnType, length);
    }

    /**
     * INSERT文を生成する。
     *
     * @param nonComputedColumns 自動計算以外のカラム
     * @return INSERT文
     */
    private String createInsertStatement(String[] nonComputedColumns) {
        // INSERT文生成
        StringBuilder sb = new StringBuilder(256);

        sb.append("INSERT INTO ").append(tableName).append('(');

        boolean firstColumn = true;
        for (String columnName : nonComputedColumns) {
            sb.append(firstColumn ? "" : ",");
            firstColumn = false;
            sb.append(columnName);

        }
        sb.append(") VALUES (");

        firstColumn = true;
        for (String columnName : nonComputedColumns) {

            sb.append(firstColumn ? "" : ",");
            firstColumn = false;
            sb.append('?');
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * 自動計算カラム以外のカラムを取得する。
     *
     * @return 自動計算カラム以外のカラム
     */
    private String[] getNonComputedColumns() {
        String[] columns = dbInfo.getColumns(tableName);
        List<String> nonComputed = new ArrayList<String>(columns.length);
        for (String e : columns) {
            if (!dbInfo.isComputedColumn(tableName, e)) {
                nonComputed.add(e);
            }
        }
        return nonComputed.toArray(new String[nonComputed.size()]);
    }

    /** DBからデータを取得し、本オブジェクトにセットする。 */
    public void loadData() {

        String[] colNames = getColumnNames();

        // 取得対象カラムが1つも存在しない場合は
        // data_に空のListをセットして終了する
        if (colNames.length == 0) {
            contents = new ArrayList<SqlRow>(0);
            return;
        }

        final String sql = createSelectStatement(tableName, colNames, getPrimaryKeys());
        (new TransactionTemplateInternal(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(TransactionManagerConnection conn) {
                SqlPStatement statement = conn.prepareStatement(sql);
                contents = statement.retrieve();
            }
        }).execute();

        // データ内のバイナリカラムは HexStringに差し替える。
        convertBinary();

        // カラムの中身を置き換える。
        convertSqlRow();
    }


    /**
     * カラムの中身のデータ型変換などを行う。
     *
     * <ul>
     *     <li>CLOB型の値が存在した場合には、文字列に変換する。
     * 基本的にデータベースのCLOB値は、ヒープを大量に消費する可能性があり（サイズの大きいデータが格納されるカラムのため)
     * プロダクション環境でヒープにCLOBの値をのせることは想定されない。
     * しかしUnit Test時には該当カラムの値を比較したい事があるため、Stringに変換し保持する。
     * ヒープ不足に関しては、VMオプションにて対応をしてもらう。
     * （ギガ単位のデータをテストでセットアップすることは出来ないので、基本的にヒープが足りなくなることはない）
     *     </li>
     *     <li>BigDecimal型の小数部の末尾0を削る
     * JDBC実装によっては、scaleのサイズの固定長でBigDecimalを構築するものがある。
     * この場合、「1.100」のような値が返却されるため、末尾の0を削除してアサートする。
     *     </li>
     * </ul>
     */
    private void convertSqlRow() {
        for (SqlRow content : contents) {
            for (Map.Entry<String, Object> entry : content.entrySet()) {
                final Object value = entry.getValue();
                if (value instanceof Clob) {
                    final String string = clob2String((Clob) value);
                    content.put(entry.getKey(), string);
                }
                if (value instanceof BigDecimal) {
                    final BigDecimal ret = trimScale((BigDecimal) value);
                    content.put(entry.getKey(), ret);
                }
            }
        }
    }

    /**
     * BigDecimalの小数部をトリム(末尾の0削除)する
     * @param value BigDecimal
     * @return トリムした値
     */
    private BigDecimal trimScale(BigDecimal value) {
        final DecimalFormat format = new DecimalFormat("#.#");
        format.setMaximumFractionDigits(value.scale());
        return new BigDecimal(format.format(value));
    }

    /**
     * CLOBを文字列に変換する。
     * @param value CLOB
     * @return 文字列
     */
    private String clob2String(Clob value) {
        final Clob clob = value;
        final String string;
        try {
            string = clob.getSubString(1, (int) clob.length());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return string;
    }

    /** 取得したデータのバイナリカラムをHexStringに変換する。 */
    private void convertBinary() {
        if (contents.isEmpty()) {
            return;
        }
        Set<String> binaryColumns = getBinaryColumns(contents.get(0));
        for (SqlRow content : contents) {
            convertBinary(content, binaryColumns);
        }

    }

    /**
     * レコードのバイナリカラムをHexStringに置き換える。
     *
     * @param target        対象レコード
     * @param binaryColumns 対象レコード内のバイナリカラム
     */
    private void convertBinary(SqlRow target, Set<String> binaryColumns) {
        for (String binaryColumn : binaryColumns) {
            byte[] bytes = target.getBytes(binaryColumn);
            target.put(binaryColumn, BinaryUtil.convertToHexString(bytes));
        }
    }

    /**
     * 対象レコード内のバイナリカラムを取得する。
     *
     * @param row レコード
     * @return 対象レコード内のバイナリカラム
     */
    private Set<String> getBinaryColumns(SqlRow row) {
        Set<String> binaryColumns = new HashSet<String>();
        for (String colName : row.keySet()) {
            if (dbInfo.isBinaryTypeColumn(tableName, colName)) {
                binaryColumns.add(colName);
            }
        }
        return binaryColumns;
    }

    /**
     * 本オブジェクトが保持するデータ件数を返却する。
     *
     * @return データ件数
     */
    public int size() {
        return contents.size();
    }

    /**
     * プライマリーキーを取得する。
     *
     * @return プライマリーキーの配列
     */
    public String[] getPrimaryKeys() {
        return dbInfo.getPrimaryKeys(tableName);
    }

    /**
     * カラム名を設定する。
     *
     * @param columnNames カラム名の配列
     */
    public void setColumnNames(String[] columnNames) {
        this.columnNames = new String[columnNames.length];
        for (int i = 0; i < this.columnNames.length; i++) {
            this.columnNames[i] = columnNames[i].toUpperCase();
        }
    }

    /**
     * カラム名を取得する。
     *
     * @return カラム名の配列
     */
    public String[] getColumnNames() {
        if (columnNames == null) {
            columnNames = dbInfo.getColumns(tableName);
        }
        return columnNames.clone();
    }

    /**
     * テーブル名を取得する。
     *
     * @return テーブル名
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * データを追加する。<br/>
     *
     * @param row 1件分のデータ
     */
    public void addRow(List<String> row) {

        Map<String, Integer> colTypes = getColumnTypes();
        SqlRow map = new SqlRow(new HashMap<String, Object>(), colTypes,
                                new HashMap<String, String>());

        for (int i = 0; i < columnNames.length; i++) {
            String value = row.get(i);
            map.put(columnNames[i].toUpperCase(), value);
        }
        contents.add(map);
    }

    /**
     * カラム型一覧を取得する。
     *
     * @return カラム型一覧
     */
    private Map<String, Integer> getColumnTypes() {
        Map<String, Integer> colTypes = new HashMap<String, Integer>();
        for (String colName : columnNames) {
            String key = colName.toUpperCase();
            Integer val = dbInfo.getColumnType(tableName, colName);
            colTypes.put(key, val);
        }
        return colTypes;
    }

    /**
     * 値を取得する。
     *
     * @param row    取得対象の行数。
     * @param column 取得対象のカラム名
     * @return 取得した値
     */
    public Object getValue(int row, String column) {
        return contents.get(row).get(column);
    }

    /**
     * 値をバイト配列で取得する。
     *
     * @param row    取得対象の行数。
     * @param column 取得対象のカラム名
     * @return 取得した値（バイト配列）
     */
    public byte[] getBytes(int row, String column) {
        return contents.get(row).getBytes(column);
    }

    /**
     * 本オブジェクトのクローンを取得する。
     *
     * @return クローン
     */
    public TableData getClone() {
        try {
            return (TableData) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("unexpected exception.", e);
        }
    }

    /**
     * 本オブジェクトのカラム値を変更する。
     *
     * @param idx   変更対象レコードインデックス（Excelで定義した順）
     * @param name  カラム名
     * @param value 変更する値
     */
    public void alterColumnValue(int idx, String name, String value) {
        contents.get(idx).put(name, value);
    }

    /** {@inheritDoc} */
    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("\n");
        sb.append("table name:").append(tableName).append("\n");
        sb.append("primarykey list:");

        String[] primaryKeys = this.getPrimaryKeys();

        for (String primaryKey : primaryKeys) {
            sb.append(primaryKey).append(" ");
        }

        sb.append("\n");
        sb.append("contents size:").append(contents.size()).append("\n");
        sb.append("column list:");

        String[] columns = dbInfo.getPrimaryKeys(tableName);

        for (String column : columns) {
            sb.append(column).append(" ");
        }

        sb.append("\n");

        for (int i = 0; i < contents.size(); i++) {
            for (String column : columns) {
                sb.append(getValue(i, column)).append(", ");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 値を設定する。
     *
     * @param row        行数
     * @param columnName カラム名
     * @param value      値
     */
    void setValue(int row, String columnName, Object value) {
        contents.get(row).put(columnName, value);
    }

    /**
     * データベース情報を設定する。
     *
     * @param dbInfo データベース情報
     */
    public void setDbInfo(DbInfo dbInfo) {
        this.dbInfo = dbInfo;
    }

    /**
     * テーブルのデータを取得する為のSELECT文を作成する。
     *
     * @param tableName   テーブル名
     * @param colNames    カラム名
     * @param primaryKeys 主キー
     * @return SELECT文
     */
    private String createSelectStatement(String tableName, String[] colNames, String[] primaryKeys) {

        StringBuilder sb = new StringBuilder(256);
        // SELECT句を構築する
        sb.append("select ");
        sb.append(join(colNames, ","));

        // FROM句を構築する
        sb.append(" from ").append(tableName);

        // ORDER BY句を構築する
        if (primaryKeys.length > 0) {
            sb.append(" order by ");
            sb.append(join(primaryKeys, ","));
        }
        return sb.toString();
    }

    /**
     * PKの値を文字列として取得する。
     *
     * @param rowIndex 取得対象の行番号
     * @return PKの文字列表現
     */
    public String getPkValues(int rowIndex) {
        String[] primaryKeys = getPrimaryKeys();
        List<String> pkValues = new ArrayList<String>();
        for (String pkColumnName : primaryKeys) {
            String value = pkColumnName + '=' + getValue(rowIndex, pkColumnName);
            pkValues.add(value);
        }
        return join(pkValues, ",");
    }

    /**
     * データベースデフォルト値実装クラスを設定する。
     *
     * @param defaultValues データベースデフォルト値実装クラスを設定する。
     */
    public void setDefaultValues(DefaultValues defaultValues) {
        this.defaultValues = defaultValues;
    }

    /**
     * 省略されたカラムにデフォルト値を埋める。<br/>
     * 自インスタンスには無いが実際のDBには存在するカラムにデフォルト値を設定する。 （破壊的メソッド）
     */
    public void fillDefaultValues() {
        // DB上の全カラム
        String[] allColumns = dbInfo.getColumns(tableName);
        // 省略されたカラム
        Set<String> omittedColumns = NablarchTestUtils.asSet(toUpperCase(allColumns));
        omittedColumns.removeAll(NablarchTestUtils.asSet(toUpperCase(columnNames)));

        // 全レコードに対し、省略されたカラムをデフォルト値で埋める
        for (int i = 0; i < size(); i++) {
            for (String column : omittedColumns) {
                Object defaultValue = getDefaultValue(column);
                setValue(i, column, defaultValue);
            }
        }
        // カラム名を変更
        setColumnNames(allColumns);
    }

    /**
     * 大文字に変換する。
     * @param original 変換前
     * @return 大文字に変換した値
     */
    private static String[] toUpperCase(String[] original) {
        String[] result = new String[original.length];
        for (int i = 0; i < original.length; i++) {
            result[i] = original[i].toUpperCase();
        }
        return result;
    }

    /**
     * データベース情報を取得する。
     *
     * @return dbInfo データベース情報
     */
    public DbInfo getDbInfo() {
        return dbInfo;
    }
}
