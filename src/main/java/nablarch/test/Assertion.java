package nablarch.test;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.db.statement.SqlRow;
import nablarch.core.message.Message;
import nablarch.core.util.ObjectUtil;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

import nablarch.test.core.db.TableData;
import nablarch.test.core.util.ByteArrayAwareMap;
import nablarch.test.core.util.MapCollector;
import org.junit.Assert;
import org.junit.ComparisonFailure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static nablarch.core.util.Builder.concat;


/**
 * 表明クラス。<br/>
 * テスト実行結果が期待通りであることを確認するのに使用する。
 *
 * @author Tsuyoshi Kawasaki
 * @author Koichi Asano
 */
@Published
public final class Assertion {

    /**
     * 複数のTableDataの比較を行う。<br/>
     * 期待値として与えられたTableDataと、それに対応するテーブルの状態が等しいことを表明する。
     *
     * @param expectedTables 期待値
     */
    public static void assertTableEquals(List<TableData> expectedTables) {
        assertTableEquals("", expectedTables);
    }

    /**
     * 複数のTableDataの比較を行う。<br/>
     * 期待値として与えられたTableDataと、それに対応するテーブルの状態が等しいことを表明する。
     *
     * @param message        比較失敗時のメッセージ
     * @param expectedTables 期待値
     */
    public static void assertTableEquals(String message, List<TableData> expectedTables) {
        for (TableData expected : expectedTables) {
            assertTableEquals(message, expected);
        }
    }

    /**
     * TableDataの比較を行う。<br/>
     * 期待値として与えられたTableDataと、それに対応するテーブルの状態が等しいことを表明する。
     *
     * @param expected 期待値
     */
    public static void assertTableEquals(TableData expected) {

        assertTableEquals("", expected);
    }


    /**
     * TableDataの比較を行う。<br/>
     * 期待値として与えられたTableDataと、それに対応するテーブルの状態が等しいことを表明する。
     *
     * @param message  比較失敗時のメッセージ
     * @param expected 期待値
     */
    public static void assertTableEquals(String message, TableData expected) {
        TableData actual = expected.getClone();
        actual.loadData();
        assertTableEquals(message, expected, actual);
    }

    /**
     * TableDataの比較を行う。<br/>
     * 引数で与えられたtableDataが等価であることを表明する。
     *
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static void assertTableEquals(TableData expected, TableData actual) {
        assertTableEquals("", expected, actual);
    }


    /**
     * SqlResultSetの比較を行う。<br/>
     * 引数で与えられたSqlResultSetが等価であることを表明する。
     *
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static void assertSqlResultSetEquals(List<Map<String, String>> expected, SqlResultSet actual) {
        assertSqlResultSetEquals("", expected, actual);
    }

    /**
     * SqlResultSetの比較を行う。<br/>
     * 引数で与えられたSqlResultSetが等価であることを表明する。
     *
     * @param message  メッセージ
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static void assertSqlResultSetEquals(String message, List<Map<String, String>> expected,
                                                SqlResultSet actual) {
        assertListMapEquals(message, expected, actual);
    }

    /**
     * {@literal List<Map>}の比較を行う。<br/>
     *
     * @param expected 期待する値
     * @param actual   実際の値
     * @see #assertListMapEquals(String, List, List)
     */
    public static void assertListMapEquals(List<Map<String, String>> expected,
                                           List<? extends Map<String, ?>> actual) {
        assertListMapEquals("", expected, actual);
    }

    /**
     * {@literal List<Map>}の比較を行う。<br/>
     * 引数で与えられた{@literal List<Map>}が等価であることを表明する。
     * 実際の値のMapのvalueは文字列に変換されて比較される。
     *
     * @param message  メッセージ
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static void assertListMapEquals(String message,
                                           List<Map<String, String>> expected,
                                           List<? extends Map<String, ?>> actual) {
        if (expected == null && actual == null) {
            return;
        }
        // 片方だけnullでないこと
        assertNotXorNull(message, expected, actual);

        // サイズが異なれば等価でない
        if (expected.size() != actual.size()) {
            failComparing(message + " size differs. expected size=[" + expected.size()
                                  + "], actual size=[" + actual.size() + "]", expected, actual);
        }

        for (int rowNum = 0; rowNum < expected.size(); rowNum++) {
            assertMapEquals(message + " line no=[" + (rowNum + 1) + "]",
                            expected.get(rowNum),
                            actual.get(rowNum));
        }
    }

    /**
     * SqlRowの比較を行う。<br/>
     * 引数で与えられたSqlResultSetが等価であることを表明する。<br/>
     * なお、バイナリカラムが含まれている場合は、比較できない。
     *
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static void assertSqlRowEquals(Map<String, String> expected, SqlRow actual) {
        assertSqlRowEquals("", expected, actual);
    }

    /**
     * SqlRowの比較を行う。<br/>
     * 引数で与えられたSqlResultSetが等価であることを表明する。<br/>
     * なお、バイナリカラムが含まれている場合は、比較できない。
     *
     * @param message  メッセージ
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static void assertSqlRowEquals(String message, Map<String, String> expected, SqlRow actual) {
        assertMapEquals(message, expected, actual);
    }

    /**
     * Mapの比較を行う。<br/>
     *
     * @param expected 期待する値
     * @param actual   実際の値
     * @see #assertMapEquals(String, Map, Map)
     */
    public static void assertMapEquals(Map<String, String> expected, Map<String, ?> actual) {
        assertMapEquals("", expected, actual);
    }

    /**
     * Mapの比較を行う。<br/>
     * 引数で与えられたSqlResultSetが等価であることを表明する。
     * 実際の値のMapのvalueは文字列に変換されて比較される。
     *
     * @param message  比較失敗時のメッセージ
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static void assertMapEquals(String message, Map<String, String> expected, Map<String, ?> actual) {
        if (expected == null && actual == null) {
            return;
        }
        // 片方だけnullでないこと
        assertNotXorNull(message, expected, actual);
        // 文字列に寄せて比較する。
        Map<String, String> actualAsString = copyValuesAsString(actual);
        if (!actualAsString.equals(expected)) {
            // 目視による結果確認を容易にするためTreeMapを使用
            expected = new TreeMap<String, String>(expected);
            failComparing(message, expected, actualAsString);
        }
    }

    /**
     * 格納された値を文字列に変換してコピーを行う。
     *
     * @param orig 元のオブジェクト
     * @return 変換後のオブジェクト
     */
    private static Map<String, String> copyValuesAsString(Map<String, ?> orig) {
        @SuppressWarnings("unchecked")
        Map<String, Object> o = (Map<String, Object>) orig;
        // 目視による結果確認を容易にするためTreeMapを使用
        return (new MapCollector<String, String, Object>(new TreeMap<String, String>()) {
            protected String evaluate(String key, Object value) {
                return value == null ? null : StringUtil.toString(value);
            }
        }).collect(o);
    }

    /**
     * TableDataの比較を行う。<br/>
     * 引数で与えられたtableDataが等価であることを表明する。
     *
     * @param message  失敗時のメッセージ
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static void assertTableEquals(String message, TableData expected, TableData actual) {
        if (expected == actual) {
            return;
        }
        assertNotXorNull(message, expected, actual);

        String[] primaryKeys = expected.getPrimaryKeys();
        String[] columns = expected.getColumnNames();

        // DBにあってExcelにないデータをチェックするための配列
        boolean[] dbDataFound = createArray(actual.size(), false);

        // 主キーが同じであるレコードを探す。
        // 発見した場合は、その他のカラムが等価であるか確認する
        for (int expIdx = 0; expIdx < expected.size(); expIdx++) {
            // 主キーが同じレコードを示すインデックス
            int samePkIdx = -1;

            pkMatching:
            for (int actIdx = 0; actIdx < actual.size(); actIdx++) {
                // 全ての主キーがマッチするか
                for (String primaryKey : primaryKeys) {
                    final Object expPk = expected.getValue(expIdx, primaryKey);
                    final Object actPk = actual.getValue(actIdx, primaryKey);
                    final String expPkStr = expPk == null ? "null" : StringUtil.toString(expPk);
                    final String actPkStr = actPk == null ? "null" : StringUtil.toString(actPk);
                    if (!expPkStr.equals(actPkStr)) {
                        continue pkMatching;    // 次のレコードへ
                    }
                }
                // 全ての主キーが同じデータを発見
                samePkIdx = actIdx;
                // ExcelにあったDBデータの消しこみ
                dbDataFound[samePkIdx] = true;
                break;
            }

            // 主キーがマッチするレコードが見つからない
            if (samePkIdx == -1) {
                fail(message, 
                     " the table of [", expected.getTableName(),
                     "] is expected to have a record whose PK is [",
                     expected.getPkValues(expIdx),
                     "], but there is no such record in the table.",
                     " row number=[", expIdx + 1, "]");
            }

            // １カラムづつ比較
            for (String column : columns) {
                Object expData = expected.getValue(expIdx, column);
                Object actData = actual.getValue(samePkIdx, column);
                String msg = concat(message, " table=", expected.getTableName(), " line=",
                                    (expIdx + 1), " column=", column);
                assertEqualsAsString(msg, expData, actData); // 文字列として比較
            }
        }

        // DBにあってExcelになかったデータのチェック
        for (int i = 0; i < dbDataFound.length; i++) {
            boolean found = dbDataFound[i];
            if (!found) {
                fail(message, " an unexpected record is included in the table of [",  actual.getTableName(), "].",
                     " PK=[", actual.getPkValues(i), "]");
            }
        }
    }

    /**
     * 配列を作成する。
     *
     * @param size         サイズ
     * @param initialValue 初期値
     * @return 配列
     */
    private static boolean[] createArray(int size, boolean initialValue) {
        boolean[] array = new boolean[size];
        for (int i = 0; i < size; i++) {
            array[i] = initialValue;
        }
        return array;
    }

    /**
     * 要素の順序を考慮しないで、２つの配列が等価な要素を保持していることを表明する。<br/>
     *
     * @param <T>      配列の総称型
     * @param message  メッセージ
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static <T> void assertEqualsIgnoringOrder(String message, T[] expected, T[] actual) {
        assertEqualsIgnoringOrder(message, Arrays.asList(expected), Arrays.asList(actual));
    }


    /**
     * 要素の順序を考慮しないで、２つの配列が等価な要素を保持していることを表明する。<br/>
     *
     * @param <T>      配列の総称型
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static <T> void assertEqualsIgnoringOrder(T[] expected, T[] actual) {
        assertEqualsIgnoringOrder("", expected, actual);
    }


    /**
     * 要素の順序を考慮しないで、２つのコレクションが等価な要素を保持していることを表明する。<br/>
     *
     * @param <T>      コレクションの総称型
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static <T> void assertEqualsIgnoringOrder(Collection<T> expected, Collection<T> actual) {
        assertEqualsIgnoringOrder("", expected, actual);
    }

    /**
     * 要素の順序を考慮しないで、２つのコレクションが等価な要素を保持していることを表明する。<br/>
     *
     * @param <T>      コレクションの総称型
     * @param message  メッセージ
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static <T> void assertEqualsIgnoringOrder(String message, Collection<T> expected, Collection<T> actual) {
        assertEqualsIgnoringOrder(new AsIdentical<T, T>(), message, expected, actual);
    }

    /**
     * 要素の順序を考慮しないで、２つのコレクションが等価な要素を保持していることを表明する。<br/>
     *
     * @param <T>      コレクションの総称型
     * @param message  メッセージ
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static <T> void assertEqualsIgnoringOrder(EquivCondition<T,T> equivCondition, String message,
                                                     Collection<T> expected, Collection<T> actual) {
        new AssertionIgnoringOrder<T, T>(message, expected, actual).doAssert(equivCondition);
    }

    /**
     * 要素の順序を考慮しないで、２つのコレクションが等価な要素を保持していることを表明する。<br/>
     * 等価かであるかどうかは、比較対象要素の文字列表現が等しいかどうかで判定する。
     *
     * @param message  メッセージ
     * @param expected 期待する値
     * @param actual   実際の値
     * @param <E> 期待する値の型
     * @param <A> 実際の値の型
     */
    @SuppressWarnings("unchecked")
    public static <E, A> void assertEqualsIgnoringOrder(String message,
                                                        List<? extends Map<String, E>> expected,
                                                        List<? extends Map<String, A>> actual) {

        List<Map<String, E>> e = (List<Map<String, E>>) expected;
        List<Map<String, A>> a = (List<Map<String, A>>) actual;
        new AssertionIgnoringOrder<Map<String, E>, Map<String, A>>(message, e, a)
                .doAssert(new AsString<E, A>());
    }

    /**
     * 要素の順序を考慮しないで、２つのコレクションが等価な要素を保持していることを表明するクラス。<br/>
     *
     * @param <E> 期待値の要素の型
     * @param <A> 実際の値の要素の型
     * @author Tsuyoshi Kawasaki
     */
    private static final class AssertionIgnoringOrder<E, A> {

        /** 比較失敗時のメッセージ */
        private String message;

        /** 期待する値 */
        private Collection<E> expectedOriginal;

        /** 実際の値 */
        private Collection<A> actualOriginal;

        /**
         * コンストラクタ
         *
         * @param message  比較失敗時のメッセージ
         * @param expected 期待する値
         * @param actual   実際の値
         */
        private AssertionIgnoringOrder(String message, Collection<E> expected,
                                       Collection<A> actual) {
            this.message = message;
            this.expectedOriginal = expected;
            this.actualOriginal = actual;
        }

        /**
         * 等価判定クラスを用いてアサートする。
         * @param comparator 比較用クラス等価判定クラス
         */
        void doAssert(EquivCondition<E, A> comparator) {
            if (expectedOriginal == actualOriginal) {
                return;        // 等値であれば等価
            }
            assertNotXorNull(message, expectedOriginal, actualOriginal);
            // 元の値を破壊しないようコピーを作成して比較
            doAssertEqualsIgnoringOrder(new LinkedList<E>(expectedOriginal),
                                        new LinkedList<A>(actualOriginal),
                                        comparator);
        }

        /** アサートを実行する。 */
        void doAssert() {
            doAssert(new AsIdentical<E, A>());
        }

        /**
         * 要素の順序を考慮しないで、２つの連結リストが等価な要素を保持していることを表明する。<br/>
         * 引数で与えられたリストの内容は保証されない（破壊的メソッド）。
         *
         * @param expected 期待する値
         * @param actual   実際の値
         * @param eq       比較ロジック
         */
        private void doAssertEqualsIgnoringOrder(LinkedList<E> expected,
                                                 LinkedList<A> actual,
                                                 EquivCondition<E, A> eq) {

            // サイズが異なれば、順番に関係なく等価でない
            if (expected.size() != actual.size()) {
                failComparing(message, expected, actual);
            }

            // 両方空であれば等価（最終的な終了条件、 前段でサイズチェック済みなのでexpectedのみ）
            if (expected.isEmpty()) {
                return;
            }

            // 等価な値を探索
            for (E e : expected) {
                for (A a : actual) {
                    if (eq.isEquivalent(e, a)) {
                        // 等価な値を発見したら、その要素を取り除き再帰呼び出し
                        expected.remove(e);
                        actual.remove(a);
                        doAssertEqualsIgnoringOrder(expected, actual, eq);
                        return; // 全ての要素が等価
                    }
                    // この時点で等価でないことが判明しているが、可能な限り比較を行う(行番号表示ができないため）。
                }
            }
            // 等価でなかった要素を表示する。
            failComparing(
                    message + " different element(s) found. expected has " + expected
                            + ", actual has " + actual + ". "
                    , expectedOriginal, actualOriginal);
        }   // failがあるので到達しない。

    }

    /**
     * 等価の判定を行うインタフェース。
     *
     * @param <E> 期待値の型
     * @param <A> 実際の値の型
     */
    public interface EquivCondition<E, A> {
        /**
         * 等価であるか判定する。
         * @param expected 期待値
         * @param actual 実際の値
         * @return 等価である場合、真
         */
        boolean isEquivalent(E expected, A actual);
    }

    /**
     * Mapが文字列として等価であるか判定する{@link EquivCondition}実装クラス。
     *
     * @param <EV> 期待値のvalueの型
     * @param <AV> 実際の値のvalueの型
     */
    static class AsString<EV, AV> implements EquivCondition<Map<String, EV>, Map<String, AV>> {
        /** {@inheritDoc} */
        public boolean isEquivalent(Map<String, EV> expected, Map<String, AV> actual) {
            Map<?, ?> sortedExpected = wrap(expected);
            Map<?, ?> sortedActual = wrap(actual);
            return sortedExpected.toString().equals(sortedActual.toString());
        }
    }

    /**
     * {@link Message}のメッセージ内容が同一であるか判定する{@link EquivCondition}実装クラス。
     */
    public static class AsIdentical<E, A> implements EquivCondition<E, A> {

        /** {@inheritDoc} */
        public boolean isEquivalent(E expected, A actual) {
            return expected.equals(actual);
        }
    }

    /**
     * 文字列として比較した場合に等価であることを表明する。
     *
     * @param message  メッセージ
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static void assertEqualsAsString(String message, Object expected, Object actual) {
        final String expectedStr = expected == null ? "null" : StringUtil.toString(expected);
        final String actualStr = actual == null ? "null" : StringUtil.toString(actual);
        Assert.assertEquals(message, expectedStr, actualStr);
    }

    /**
     * 比較に失敗したことを通知する。<br/>
     *
     * @param message  メッセージ
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static void failComparing(String message, Object expected, Object actual) {
        final String expectedStr = expected == null ? "null" : StringUtil.toString(expected);
        final String actualStr = actual == null ? "null" : StringUtil.toString(actual);
        throw new ComparisonFailure(message, expectedStr, actualStr);
    }

    /**
     * 片方だけnullでないことを表明する。
     *
     * @param message  メッセージ
     * @param expected 期待する値
     * @param actual   実際の値
     */
    public static void assertNotXorNull(String message, Object expected, Object actual) {
        // 片方だけnullならば比較失敗とする。
        if (expected != null && actual == null || expected == null && actual != null) {
            failComparing(message, expected, actual);
        }
    }

    /**
     * 失敗を表明する。引数で与えられたメッセージを連結して一つのメッセージとする。
     *
     * @param messages メッセージ
     */
    public static void fail(Object... messages) {
        Assert.fail(concat(messages));
    }

    /**
     * Object に設定されたプロパティを表明する。<br />
     * <p>
     * expected に指定した Map の持つキーに対して、 actual のプロパティの値を全てチェックする。 <br />
     * actual のプロパティが文字列ではない場合、プロパティの値を{@link StringUtil#toString(Object)}で比較する。
     * </p>
     *
     * @param expected オブジェクトが持つプロパティに期待される値を設定した Map
     * @param actual   実際の値
     */
    public static void assertProperties(Map<String, String> expected, Object actual) {
        assertProperties("", expected, actual);
    }

    /**
     * Object に設定されたプロパティを表明する。<br />
     * <p>
     * expected に指定した Map の持つキーに対して、 actual のプロパティの値を全てチェックする。 <br />
     * actual のプロパティが文字列ではない場合、プロパティの値を{@link StringUtil#toString(Object)}で比較する。
     * </p>
     *
     * @param message  メッセージ
     * @param expected オブジェクトが持つプロパティに期待される値を設定した Map
     * @param actual   実際の値
     */
    public static void assertProperties(String message, Map<String, String> expected, Object actual) {
        String actualClassName = actual.getClass().getName();

        Map<String, String> actualMap = new TreeMap<String, String>();
        for (String key : expected.keySet()) {

            Object actualValue = null;
            try {
                actualValue = ObjectUtil.getProperty(actual, key);
            } catch (RuntimeException e) {
                // 取得失敗
                fail(message, actualClassName, " does not have property ", key);
            }

            actualMap.put(key, actualValue == null ? null : StringUtil.toString(actualValue));
        }

        Map<String, String> expectedMap = new TreeMap<String, String>(expected);


        // 結果を見やすくするために、toStringした結果を比較
        message += "; class property assertion failed ; target class name = " + actualClassName;
        assertEqualsAsString(message, expectedMap, actualMap);
    }

    /**
     * {@link DataRecord}の比較を行う。
     *
     * @param msg      比較失敗時のメッセージ
     * @param expected 期待値
     * @param actual   実際の値
     */
    public static void assertEquals(String msg, List<DataRecord> expected, List<DataRecord> actual) {
        assertNotXorNull(msg, expected, actual);
        assertEqualsAsString(msg, wrap(expected), wrap(actual));
    }


    /**
     * {@link DataRecord}の比較を行う。
     *
     * @param msg      比較失敗時のメッセージ
     * @param expected 期待値
     * @param actual   実際の値
     */
    public static void assertEquals(String msg, DataRecord expected, DataRecord actual) {
        assertNotXorNull(msg, expected, actual);
        Map<String, ?> exp = wrap(expected);
        Map<String, ?> act = wrap(actual);
        assertEqualsAsString(msg, exp, act);
    }

    /**
     * データレコードを比較用にラップする。
     *
     * @param dataRecords 比較対象のデータレコード
     * @return ラップしたデータレコード
     */
    private static List<Map<String, ?>> wrap(List<? extends Map<String, ?>> dataRecords) {
        List<Map<String, ?>> wrapped = new ArrayList<Map<String, ?>>(dataRecords.size());
        for (Map<String, ?> e : dataRecords) {
            wrapped.add(wrap(e));
        }
        return wrapped;
    }

    /**
     * データレコードを比較用にラップする。<br/>
     *
     * @param dataRecord 比較対象のデータレコード
     * @return ラップしたデータレコード
     */
    private static Map<String, ?> wrap(Map<String, ?> dataRecord) {
        // バイト列を扱えるようにする
        return new ByteArrayAwareMap<String, Object>(
                new TreeMap<String, Object>(dataRecord)   // 比較しやすいようTreeMapを使用
        );
    }

    /**
     * 期待値と実際の値が等価であることを表明する。<br/>
     * 等価でなかった場合は、デバッグを容易にするため
     * AssertionErrorではなくComparisonFailureをスローする。
     *
     * @param msg      比較失敗時のメッセージ
     * @param expected 期待値
     * @param actual   実際の値
     * @throws ComparisonFailure 等価でなかった場合
     */
    public static void assertEquals(String msg, Object expected, Object actual) throws ComparisonFailure {
        assertEquals(new AsIdentical<Object, Object>(), msg, expected, actual);
    }

    /**
     * 期待値と実際の値が等価であることを表明する。<br/>
     * 等価でなかった場合は、デバッグを容易にするため
     * AssertionErrorではなくComparisonFailureをスローする。
     *
     * @param msg      比較失敗時のメッセージ
     * @param expected 期待値
     * @param actual   実際の値
     * @throws ComparisonFailure 等価でなかった場合
     */
    public static <E, A> void assertEquals(EquivCondition<E, A> equivCondition, String msg,
                                           E expected, A actual) throws ComparisonFailure {
        if (expected == actual) {
            return;
        }
        assertNotXorNull(msg, expected, actual);
        if (!equivCondition.isEquivalent(expected, actual)) {
            failComparing(msg, expected, actual);
        }
    }

    /**
     * プライベートコンストラクタ<br/>
     * 本クラスはインスタンス化できない。
     */
    private Assertion() {
    }
}
