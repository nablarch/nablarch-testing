package nablarch.test.core.reader;

import java.sql.Types;

import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.TableData;

/**
 * 変換ツールの DB レスな読み込み経路で本体 Parser／ビルダの配線にのみ用いるスタブの{@link DbInfo}。
 * <p>
 * {@link TestCoreReaderAdapter}（Excel 経路）と {@link YamlTestCoreAdapter}（YAML 経路）の両方が、
 * 値加工を伴わない生の本体器を取り出すために共有する。
 * </p>
 * <p>
 * 読み込み（parse→getResult／ビルダ走査）経路で実際に呼ばれるのは
 * {@link #getColumnType(String, String)}のみ（{@link TableData#addRow(java.util.List)}でのカラム型取得）。
 * 値は型に依存せず生のまま格納されるため、一律で{@link java.sql.Types#VARCHAR}を返す。
 * </p>
 * <p>
 * その他のメソッドは DB 書き込み経路（insertData/replaceData）専用で、変換ツールの
 * DB レスな読み込み経路からは呼ばれない。これらは「呼ばれてはならない」ことを表明する
 * <b>番人コード</b>であり、万一呼ばれた場合は前提崩れとして{@link UnsupportedOperationException}で
 * 即座に失敗させる（誤った DB メタ情報で変換結果を静かに歪めない）。
 * </p>
 *
 * @author kiyobot
 */
final class StubDbInfo implements DbInfo {

    /** 読み込み経路から呼ばれてはならないメソッドが呼ばれた場合の例外を生成する。 */
    private static UnsupportedOperationException notOnReadPath(String method) {
        return new UnsupportedOperationException(
                "DbInfo#" + method + " must not be called on the DB-less converter read path.");
    }

    @Override
    public int getColumnType(String tabName, String columnName) {
        return Types.VARCHAR;
    }

    @Override
    public String[] getPrimaryKeys(String tabName) {
        throw notOnReadPath("getPrimaryKeys");
    }

    @Override
    public String[] getColumns(String tabName) {
        throw notOnReadPath("getColumns");
    }

    @Override
    public boolean isUniqueIndex(String tabName, String colName) {
        throw notOnReadPath("isUniqueIndex");
    }

    @Override
    public int getColumnLength(String tabName, String colName) {
        throw notOnReadPath("getColumnLength");
    }

    @Override
    public boolean isComputedColumn(String tabName, String colName) {
        throw notOnReadPath("isComputedColumn");
    }

    @Override
    public boolean isNumberTypeColumn(String tableName, String columnName) {
        throw notOnReadPath("isNumberTypeColumn");
    }

    @Override
    public boolean isDateTypeColumn(String tableName, String columnName) {
        throw notOnReadPath("isDateTypeColumn");
    }

    @Override
    public boolean isBinaryTypeColumn(String tableName, String columnName) {
        throw notOnReadPath("isBinaryTypeColumn");
    }

    @Override
    public boolean isBooleanTypeColumn(String tableName, String columnName) {
        throw notOnReadPath("isBooleanTypeColumn");
    }
}
