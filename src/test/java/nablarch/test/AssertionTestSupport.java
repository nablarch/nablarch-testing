package nablarch.test;


import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.db.statement.SqlRow;
import nablarch.core.util.BinaryUtil;
import nablarch.test.Assertion.AsString;
import nablarch.test.core.db.BigDecimalTable;
import nablarch.test.core.db.DbAccessTestSupport;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.TableData;
import nablarch.test.core.db.TestTable;
import nablarch.test.core.db.TransactionTemplate;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DatabaseTestRunner.class)
public class AssertionTestSupport {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    protected DbInfo dbInfo;

    /**
     * 共通の準備処理
     *
     * @throws Exception 予期しない例外
     */
    @BeforeClass
    public static void beforeClass() {
        VariousDbTestHelper.createTable(TestTable.class);
        VariousDbTestHelper.createTable(BigDecimalTable.class);
    }

    @Before
    public void before() {
        VariousDbTestHelper.delete(TestTable.class);
        VariousDbTestHelper.delete(BigDecimalTable.class);
        dbInfo = repositoryResource.getComponentByType(DbInfo.class);
    }

    /**
     * {@link Assertion#assertTableEquals(TableData, TableData)}のテスト<br/>
     * null同士は等価と見なされることを確認
     */
    @Test
    public void assertTableEqualsNull() {
        Assertion.assertTableEquals((TableData) null, null);
        TableData t = new TableData();
        Assertion.assertTableEquals(t, t);
    }

    /**
     * {@link Assertion#assertTableEquals(TableData, TableData)}のテスト <br/>
     * 同一インスタンスは等価と見なされることを確認
     */
    @Test
    public void assertTableEqualsSameObject() {
        TableData t = new TableData();
        Assertion.assertTableEquals(t, t);
    }

    /**
     * レコードが異なる（主キーが異なる）。
     */
    @Test
    public void testAssertTableEqualsListOfTableDataFail1() {
        TableData exp = new TableData();
        exp.setTableName("test_table");
        exp.setDbInfo(dbInfo);
        exp.setColumnNames(new String[] {"PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL",
                "NUMBER_COL2", "DATE_COL", "TIMESTAMP_COL", "CLOB_COL", "BLOB_COL", "BOOL_COL"});
        // 1行目
        List<String> row = new ArrayList<String>();
        row.add("00001");
        row.add("01");
        row.add("あ");
        row.add("12345");
        row.add("1234.123");
        row.add("20100831101900");
        row.add("20100831123456789");
        row.add("CLOBです1");
        row.add(BinaryUtil.convertToHexString("BLOBです1".getBytes()));
        row.add("1");
        exp.addRow(row);
        // 2行め
        row = new ArrayList<String>();
        row.add("00002");
        row.add("02");
        row.add("い");
        row.add("111");
        row.add("222.123");
        row.add("20110831101900");
        row.add("20120831123456789");
        row.add("CLOBです2");
        row.add(BinaryUtil.convertToHexString("BLOBです2".getBytes()));
        row.add("1");
        exp.addRow(row);


        TableData act = new TableData();
        act.setTableName("test_table");
        act.setDbInfo(dbInfo);
        act.setColumnNames(new String[] {"PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL",
                "NUMBER_COL2", "DATE_COL", "TIMESTAMP_COL", "CLOB_COL", "BLOB_COL", "BOOL_COL"});
        // 1行目
        row = new ArrayList<String>();
        row.add("00001");
        row.add("01");
        row.add("あ");
        row.add("12345");
        row.add("1234.123");
        row.add("20100831101900");
        row.add("20100831123456789");
        row.add("CLOBです1");
        row.add(BinaryUtil.convertToHexString("BLOBです1".getBytes()));
        row.add("1");
        act.addRow(row);
        // 2行め
        row = new ArrayList<String>();
        row.add("00003");
        row.add("02");
        row.add("い");
        row.add("111");
        row.add("222.123");
        row.add("20110831101900");
        row.add("20120831123456789");
        row.add("CLOBです2");
        row.add(BinaryUtil.convertToHexString("BLOBです2".getBytes()));
        row.add("1");
        act.addRow(row);
        // ターゲット実行
        try {
            Assertion.assertTableEquals("メッセージ", exp, act);
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(),
                    equalToIgnoringCase(
                            "メッセージ the table of [TEST_TABLE] is expected to have a record whose PK is [PK_COL1=00002,PK_COL2=02], but there is no such record in the table. row number=[2]"));
        }
    }

    /**
     * 期待値にないレコードが実際の値に含まれている。
     */
    @Test
    public void testAssertTableEqualsListOfTableDataFail2() {
        TableData exp = new TableData();
        exp.setTableName("test_table");
        exp.setDbInfo(dbInfo);
        exp.setColumnNames(new String[] {"PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL",
                "NUMBER_COL2", "DATE_COL", "TIMESTAMP_COL"});
        // 1行目
        List<String> row = new ArrayList<String>();
        row.add("00001");
        row.add("01");
        row.add("あ");
        row.add("12345");
        row.add("1234.123");
        row.add("20100831101900");
        row.add("20100831123456789");
        exp.addRow(row);

        TableData act = new TableData();
        act.setTableName("test_table");
        act.setDbInfo(dbInfo);
        act.setColumnNames(new String[] {"PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL",
                "NUMBER_COL2", "DATE_COL", "TIMESTAMP_COL"});
        // 1行目
        row = new ArrayList<String>();
        row.add("00001");
        row.add("01");
        row.add("あ");
        row.add("12345");
        row.add("1234.123");
        row.add("20100831101900");
        row.add("20100831123456789");
        act.addRow(row);
        // 2行め
        row = new ArrayList<String>();
        row.add("00002");
        row.add("02");
        row.add("い");
        row.add("111");
        row.add("222.123");
        row.add("20110831101900");
        row.add("20120831123456789");
        act.addRow(row);

        // ターゲット実行
        try {
            Assertion.assertTableEquals("メッセージ", exp, act);
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(),
                    equalToIgnoringCase(
                            "メッセージ an unexpected record is included in the table of [TEST_TABLE]. PK=[PK_COL1=00002,PK_COL2=02]"));
        }
    }

    /**
     * 主キーが同じだが、その他のカラムの値が異なる。
     */
    @Test
    public void testAssertTableEqualsListOfTableDataFail3() {
        TableData exp = new TableData();
        exp.setTableName("test_table");
        exp.setDbInfo(dbInfo);
        exp.setColumnNames(new String[] {"PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL",
                "NUMBER_COL2", "DATE_COL", "TIMESTAMP_COL"});
        // 1行目
        List<String> row = new ArrayList<String>();
        row.add("00001");
        row.add("01");
        row.add("あ");
        row.add("12345");
        row.add("1234.123");
        row.add("20100831101900");
        row.add("20100831123456789");
        exp.addRow(row);

        TableData act = new TableData();
        act.setTableName("test_table");
        act.setDbInfo(dbInfo);
        act.setColumnNames(new String[] {"PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL",
                "NUMBER_COL2", "DATE_COL", "TIMESTAMP_COL"});
        // 1行目
        row = new ArrayList<String>();
        row.add("00001");
        row.add("01");
        row.add("い");    //異なる
        row.add("12345");
        row.add("1234.123");
        row.add("20100831101900");
        row.add("20100831123456789");
        act.addRow(row);

        // ターゲット実行
        try {
            Assertion.assertTableEquals("メッセージ", exp, act);
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is(
                    "メッセージ table=TEST_TABLE line=1 column=VARCHAR2_COL expected:<[あ]> but was:<[い]>"));
        }
    }

    /**
     * 片方がnull
     */
    @Test
    public void testAssertTableEqualsListOfTableDataFail4() {
        TableData tableData = new TableData();
        tableData.setTableName("test_table");
        tableData.setDbInfo(dbInfo);
        tableData.setColumnNames(new String[] {"PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL",
                "NUMBER_COL2", "DATE_COL", "TIMESTAMP_COL"});
        // 1行目
        List<String> row = new ArrayList<String>();
        row.add("00001");
        row.add("01");
        row.add("あ");
        row.add("12345");
        row.add("1234.123");
        row.add("20100831101900");
        row.add("20100831123456789");
        tableData.addRow(row);
        // ターゲット実行
        try {
            Assertion.assertTableEquals("メッセージ", tableData, null);
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage()
                    .startsWith("メッセージ "), is(true));
            assertThat(e.getMessage(), containsString("but was:<[null]>"));
        }
        // ターゲット実行
        try {
            Assertion.assertTableEquals("メッセージ", null, tableData);
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage()
                    .startsWith("メッセージ "), is(true));
            assertThat(e.getMessage(), containsString("expected:<[null]> but was:"));
        }
    }

    /**
     * {@link Assertion#assertTableEquals(TableData, TableData)}のテスト<br/>
     * 等価なインスタンス同士の比較が成功すること。
     */
    @Test
    public void testAssertTableEqualsListOfTableData() {

        TableData e = new TableData();
        e.setTableName("test_table");
        e.setDbInfo(dbInfo);
        e.setColumnNames(new String[] {"PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL",
                "NUMBER_COL2", "DATE_COL", "TIMESTAMP_COL"});
        // 1行目
        List<String> row = new ArrayList<String>();
        row.add("00001");
        row.add("01");
        row.add("あ");
        row.add("12345");
        row.add("1234.123");
        row.add("20100831101900");
        row.add("20100831123456789");
        e.addRow(row);
        // 2行め
        row = new ArrayList<String>();
        row.add("00002");
        row.add("02");
        row.add("い");
        row.add("111");
        row.add("222.123");
        row.add("20110831101900");
        row.add("20120831123456789");
        e.addRow(row);


        // 全く等価なオブジェクト同士で比較
        Assertion.assertTableEquals(e, e.getClone());

        // レコードの順番を入れ替えて比較
        TableData a = new TableData();
        a.setTableName("test_table");
        a.setDbInfo(dbInfo);
        a.setColumnNames(new String[] {"PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL",
                "NUMBER_COL2", "DATE_COL", "TIMESTAMP_COL"});

        // 1行目
        row = new ArrayList<String>();
        row.add("00002");
        row.add("02");
        row.add("い");
        row.add("111");
        row.add("222.123");
        row.add("20110831101900");
        row.add("20120831123456789");
        a.addRow(row);

        // 2行目（レコードの順番を入れ替え）
        row = new ArrayList<String>();
        row.add("00001");
        row.add("01");
        row.add("あ");
        row.add("12345");
        row.add("1234.123");
        row.add("20100831101900");
        row.add("20100831123456789");
        a.addRow(row);

        // ターゲット実行
        Assertion.assertTableEquals(e, a);
    }

    /**
     * テーブルの主キーがBigDecimal型であっても指数表記にならずにアサートできること。
     */
    @Test
    public void testAssertTableEqualsBigDecimalKey() {
        // INSERT文実行
        VariousDbTestHelper.setUpTable(
                new BigDecimalTable(new BigDecimal("0.0000000001")));

        TableData e = new TableData();
        e.setTableName("big_decimal_table");
        e.setDbInfo(dbInfo);
        e.setColumnNames(new String[] {"DECIMAL_PK_COL", "DECIMAL_COL"});
        List<String> row = new ArrayList<String>();
        row.add("0.0000000001");
        row.add(null);
        e.addRow(row);
        List<TableData> tables = new ArrayList<TableData>();
        tables.add(e);
        Assertion.assertTableEquals(tables);
        Assertion.assertTableEquals(e);
    }

    /**
     * テーブルのカラムにBigDecimal型が含まれていても指数表記にならずにアサートできること。
     */
    @Test
    public void testAssertTableEqualsBigDecimalColumn() {
        // INSERT文実行
        VariousDbTestHelper.setUpTable(
                new BigDecimalTable(new BigDecimal("1"), new BigDecimal("0.0000000001")));

        TableData e = new TableData();
        e.setTableName("big_decimal_table");
        e.setDbInfo(dbInfo);
        e.setColumnNames(new String[] {"DECIMAL_PK_COL", "DECIMAL_COL"});
        List<String> row = new ArrayList<String>();
        row.add("1");
        row.add("0.0000000001");
        e.addRow(row);
        List<TableData> tables = new ArrayList<TableData>();
        tables.add(e);
        Assertion.assertTableEquals(tables);
        Assertion.assertTableEquals(e);
    }

    @Test
    public void testAssertEqualsIgnoringOrder1() {

        List<String> e = new LinkedList<String>();
        e.add("foo");
        e.add("bar");
        e.add("baz");
        List<String> a = new ArrayList<String>();
        a.add("bar");
        a.add("baz");
        a.add("foo");
        Assertion.assertEqualsIgnoringOrder(e, a);
    }

    @Test
    public void testAssertEqualsIgnoringOrder2() {
        List<String> e = new LinkedList<String>();
        List<String> a = new LinkedList<String>();
        Assertion.assertEqualsIgnoringOrder(e, a);
    }

    @Test
    public void testAssertEqualsIgnoringOrder3() {
        List<String> e = null;
        List<String> a = null;
        Assertion.assertEqualsIgnoringOrder(e, a);
    }


    @Test
    public void testAssertEqualsIgnoringOrder4() {
        List<String> e = new LinkedList<String>();
        e.add("foo");
        e.add("bar");
        e.add("baz");

        List<String> a = new LinkedList<String>();
        a.add("bar");
        a.add("baz");
        try {
            Assertion.assertEqualsIgnoringOrder(e, a);
            fail("期待したエラーが発生しませんでした。");
        } catch (ComparisonFailure actual) {
            assertThat(actual.getMessage(),
                    is("expected:<[[foo, ]bar, baz]> but was:<[[]bar, baz]>"));
        }
    }

    @Test
    public void testAssertEqualsIgnoringOrder5() {
        List<String> e = new LinkedList<String>();
        e.add("foo");
        e.add("bar");
        e.add("baz");

        List<String> a = new LinkedList<String>();
        a.add("hoge");
        a.add("foo");
        a.add("baz");
        try {
            Assertion.assertEqualsIgnoringOrder(e, a);
            fail("期待したエラーが発生しませんでした。");
        } catch (AssertionError actual) {
            assertEquals(
                    " different element(s) found. expected has [bar], actual has [hoge].  expected:<[[foo, bar], baz]> but was:<[[hoge, foo], baz]>"
                    , actual.getMessage());
        }
    }

    @Test(expected = ComparisonFailure.class)
    public void testAssertEqualsIgnoringOrder6() {
        List<String> e = null;
        List<String> a = new ArrayList<String>();
        Assertion.assertEqualsIgnoringOrder(e, a);
    }

    /**
     * 順序、型が異なるList-Mapでも比較できること。
     */
    @Test
    public void testAssertEqualsIgnoringOrderListMap() {
        // 期待値
        ListMapBuilder<String, String> expected = new ListMapBuilder<String, String>() {
            @Override
            protected Map<String, String> newMap() {
                return new TreeMap<String, String>();  // Map実装クラスを比較対象と違うクラスにする。
            }
        };
        expected.put("name", "satou")
                .put("age", "20")
                .put("no", "1")
                .newRow()
                .put("name", "yamada")
                .put("age", "30")
                .put("no", "2")
                .newRow()
                .put("name", "tanaka")
                .put("age", "40")
                .put("no", "3")
                .build();

        // 実際の値はListの要素の格納順が異なる。
        ListMapBuilder<String, Object> actual = new ListMapBuilder<String, Object>();
        actual.put("name", "satou")
                .put("age", 20)
                .put("no", 1)
                .newRow()
                .put("name", "tanaka")
                .put("age", 40)
                .put("no", 3)
                .newRow()
                .put("name", "yamada")
                .put("age", 30)
                .put("no", 2);

        Assertion.assertEqualsIgnoringOrder("should success.", expected.build(), actual.build());

    }

    @Test
    public void testAsStringTrue() {
        AsString<String, Integer> asString = new AsString<String, Integer>();
        // 期待値
        Map<String, String> expected = new LinkedHashMap<String, String>() {
            {
                put("three", "3");
                put("one", "1");
                put("two", "2");
            }
        };
        // 実際の値
        Map<String, Integer> actual = new TreeMap<String, Integer>() {
            {
                put("one", 1);
                put("two", 2);
                put("three", 3);
            }
        };
        // 順序や型が異なっていても文字列表現が同じであれば等価と見なされること
        assertThat(asString.isEquivalent(expected, actual), is(true));
    }

    @Test
    public void testAsStringFalse() {
        AsString<String, Float> asString = new AsString<String, Float>();
        Map<String, String> expected = new HashMap<String, String>() {
            {
                put("one", "1");
            }
        };

        Map<String, Float> actual = new HashMap<String, Float>() {
            {
                put("one", 1F);
            }
        };
        // 数値としては等価だが、文字列表現が異なる場合は等価と見なされない（"1" と "1.0"）
        assertThat(asString.isEquivalent(expected, actual), is(false));
    }


    public static class ListMapBuilder<K, V> {

        private final List<Map<K, V>> result;

        private Map<K, V> map = newMap();

        public ListMapBuilder() {
            this(new ArrayList<Map<K, V>>());  // default
        }

        public ListMapBuilder(List<Map<K, V>> list) {
            result = list;
        }

        public ListMapBuilder<K, V> put(K key, V value) {
            map.put(key, value);
            return this;
        }

        public ListMapBuilder<K, V> newRow() {
            result.add(map);
            map = newMap();
            return this;
        }

        public List<Map<K, V>> build() {
            if (!map.isEmpty()) {
                newRow();
            }
            return result;
        }

        protected Map<K, V> newMap() {
            return new HashMap<K, V>();
        }
    }


    /**
     * サイズが異なる場合
     */
    @Test(expected = ComparisonFailure.class)
    public void testAssertSqlResultSetEqualsFail1() {
        final List<Map<String, String>> expList = new ArrayList<Map<String, String>>();
        expList.add(new HashMap<String, String>());
        new TransactionTemplate(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
                SqlPStatement s = conn.prepareStatement("SELECT * FROM TEST_TABLE");
                SqlResultSet actual = s.retrieve();
                Assertion.assertSqlResultSetEquals(expList, actual);
            }
        }.execute();
    }

    /**
     * 中身が異なる場合
     */
    @Test(expected = ComparisonFailure.class)
    public void testAssertSqlResultSetEqualsFail2() {
        // INSERT文実行
        VariousDbTestHelper.setUpTable(
                new TestTable("00001", 1L, "あ", 12345L, new BigDecimal("1234.123"), new Date(0L), new Timestamp(0L),
                        null, null, null, true));

        // expected
        final List<Map<String, String>> expList = new ArrayList<Map<String, String>>();
        Map<String, String> expMap = null;
        expMap = new TreeMap<String, String>();
        expMap.put("NUMBER_COL", "12345");
        expMap.put("VARCHAR2_COL", "い");
        expMap.put("PK_COL1", "00001");
        expMap.put("PK_COL2", "01");
        expMap.put("NUMBER_COL", "12345");
        expMap.put("NUMBER_COL2", "1234.123");
        expMap.put("DATE_COL", "1970-01-01 09:00:00.0");
        expMap.put("TIMESTAMP_COL", "1970-01-01 09:00:00.0");
        expMap.put("NULL_COL", null);
        expList.add(expMap);

        new TransactionTemplate(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
                SqlPStatement s = conn.prepareStatement("SELECT * FROM TEST_TABLE");
                SqlResultSet actual = s.retrieve();
                Assertion.assertSqlResultSetEquals(expList, actual);
            }
        }.execute();
    }

    /**
     * テーブルのカラムにBigDecimal型が含まれていても、SQLRowをアサートできること。
     */
    @Test
    public void testAssertSqlRowEqualsBigDecimal() {

        VariousDbTestHelper.setUpTable(new BigDecimalTable(new BigDecimal("0.0000000001"), new BigDecimal("0.0000000002")));

        final Map<String, String> expMap = new TreeMap<String, String>();
        expMap.put("DECIMAL_PK_COL", "0.0000000001");
        expMap.put("DECIMAL_COL", "0.0000000002");

        new TransactionTemplate(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
                String stmt = "SELECT "
                        + "DECIMAL_PK_COL, "
                        + "DECIMAL_COL "
                        + "FROM "
                        + "BIG_DECIMAL_TABLE";
                SqlPStatement s = conn.prepareStatement(stmt);
                SqlResultSet actual = s.retrieve();
                assertEquals(1, actual.size());
                SqlRow actualRow = actual.get(0);
                Assertion.assertSqlRowEquals(expMap, actualRow);
            }
        }.execute();
    }

    @Test(expected = ComparisonFailure.class)
    public void testAssertSqlRowEqualsFail1() {
        // INSERT文実行
        VariousDbTestHelper.setUpTable(
                new TestTable("00001", 1L, "い", 12345L, new BigDecimal("1234.123"), new Date(0L), new Timestamp(0L),
                        null, null, null, true));

        // expected

        final Map<String, String> expMap = new TreeMap<String, String>();
        expMap.put("NUMBER_COL", "12345");
        expMap.put("VARCHAR2_COL", "あ");
        expMap.put("PK_COL1", "00001");
        expMap.put("PK_COL2", "01");
        expMap.put("NUMBER_COL", "12345");
        expMap.put("NUMBER_COL2", "1234.123");
        expMap.put("DATE_COL", "1970-01-01 09:00:00.0");
        expMap.put("TIMESTAMP_COL", "1970-01-01 09:00:00.0");
        expMap.put("NULL_COL", null);

        new TransactionTemplate(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
                SqlPStatement s = conn.prepareStatement("SELECT * FROM TEST_TABLE");
                SqlResultSet actual = s.retrieve();
                assertEquals(1, actual.size());
                SqlRow actualRow = actual.get(0);
                Assertion.assertSqlRowEquals(expMap, actualRow);
            }
        }.execute();
    }


    /**
     * 中身が異なる場合
     */
    @Test(expected = ComparisonFailure.class)
    public void testAssertSqlRowEqualsFail2() {

        // INSERT文実行
        VariousDbTestHelper.setUpTable(
                new TestTable("00001", 1L, "あ", 12345L, new BigDecimal("1234.123"), new Date(0L), new Timestamp(0L),
                        null, null, null, true));

        // expected
        final Map<String, String> expMap = new TreeMap<String, String>();
        expMap.put("NUMBER_COL", "12345");
        expMap.put("VARCHAR2_COL", "い");
        expMap.put("PK_COL1", "00001");
        expMap.put("PK_COL2", "01");
        expMap.put("NUMBER_COL", "12345");
        expMap.put("NUMBER_COL2", "1234.123");
        expMap.put("DATE_COL", "1970-01-01 09:00:00.0");
        expMap.put("TIMESTAMP_COL", "1970-01-01 09:00:00.0");
        expMap.put("NULL_COL", null);


        new TransactionTemplate(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
                SqlPStatement s = conn.prepareStatement("SELECT * FROM TEST_TABLE");
                SqlResultSet actual = s.retrieve();
                assertEquals(1, actual.size());
                SqlRow actualRow = actual.get(0);
                Assertion.assertSqlRowEquals(expMap, actualRow);
            }
        }.execute();
    }

    /**
     * {@link Assertion#assertNotXorNull(String, Object, Object)}のテスト。
     */
    @Test
    public void testAssertNotXorNull() {
        Assertion.assertNotXorNull("両方nullでない", new Object(), new Object());
        Assertion.assertNotXorNull("両方null", null, null);
    }

    /** {@link Assertion#assertNotXorNull(String, Object, Object)}のテスト。 */
    @Test
    public void testAssertNotXorNullFail1() {
        try {
            Assertion.assertNotXorNull("期待値がnull", null, new Object());
            fail();
        } catch (ComparisonFailure e) {
            assertThat(e.getMessage(), containsString("期待値がnull"));
        }
    }

    @Test
    public void testAssertNotXorNullFail2() {
        try {
            Assertion.assertNotXorNull("実際の値がnull", new Object(), null);
            fail();
        } catch (ComparisonFailure e) {
            assertThat(e.getMessage(), containsString("実際の値がnull"));
        }

    }


    @Test
    public void testAssertEqualsIgnoringOrderArray1() {
        String[] e = new String[] {"foo", "bar", "baz"};
        String[] a = new String[] {"bar", "baz", "foo"};
        Assertion.assertEqualsIgnoringOrder(e, a);
    }

    @Test
    public void testAssertEqualsIgnoringOrderArray2() {
        String[] e = new String[] {"foo", "bar", "baz"};
        String[] a = new String[] {"hoge", "foo", "baz"};
        try {
            Assertion.assertEqualsIgnoringOrder(e, a);
            fail("期待したエラーが発生しませんでした。");
        } catch (AssertionError actual) {
            assertEquals(" different element(s) found. expected has [bar], actual has [hoge].  "
                            + "expected:<[[foo, bar], baz]> but was:<[[hoge, foo], baz]>",
                    actual.getMessage());
        }
    }

    @Test
    public void testAssertEqualsIgnoringOrderArray3() {
        String[] e = new String[] {"foo", "bar", "baz"};
        String[] a = new String[] {"hoge", "baz", "fuga"};
        try {
            Assertion.assertEqualsIgnoringOrder(e, a);
            fail("期待したエラーが発生しませんでした。");
        } catch (AssertionError actual) {
            assertEquals(" different element(s) found. expected has [foo, bar], actual has [hoge, fuga].  "
                            + "expected:<[[foo, bar, baz]]> but was:<[[hoge, baz, fuga]]>",
                    actual.getMessage());
        }
    }

    @Test
    public void testPrivateConstructor() {
        NablarchTestUtils.invokePrivateDefaultConstructor(Assertion.class);
    }

    @Test
    public void testAssertProperties() {

        {
            // 全項目チェックOKのテスト
            TestTargetClass actual = new TestTargetClass();
            actual.setName("name1");
            actual.setIntegerVal(2);
            actual.setShortVal((short) 3);
            actual.setIntVal(4);
            actual.setLongVal(5l);
            actual.setFloatVal(1.0f);
            actual.setDoubleVal(1.0);
            actual.setBoolVal(true);
            actual.setDecimalVal(new BigDecimal("0.0000000001"));

            Map<String, String> expected = new HashMap<String, String>();
            expected.put("name", "name1");
            expected.put("integerVal", "2");
            expected.put("shortVal", "3");
            expected.put("intVal", "4");
            expected.put("longVal", "5");
            expected.put("floatVal", "1.0");
            expected.put("doubleVal", "1.0");
            expected.put("boolVal", "true");
            expected.put("decimalVal", "0.0000000001");

            Assertion.assertProperties("test", expected, actual);
        }

        {
            // チェック対象外はテストしないことのテスト
            TestTargetClass actual = new TestTargetClass();
            actual.setName("name");
            actual.setIntegerVal(2);

            Map<String, String> expected = new HashMap<String, String>();
            expected.put("name", "name");

            // integerVal は null だけどチェックされない。
            Assertion.assertProperties("test", expected, actual);

        }

        {
            // Map に put された null はチェック対象になることのテスト
            TestTargetClass actual = new TestTargetClass();
            actual.setName("name");
            actual.setIntegerVal(3);

            Map<String, String> expected = new HashMap<String, String>();
            expected.put("name", "name");
            expected.put("integerVal", null);

            // integerVal は チェックされる。
            try {
                Assertion.assertProperties("test", expected, actual);
                fail("例外が発生するはず。");
            } catch (ComparisonFailure error) {
                // OK
            }
        }

        {
            // Object型のチェックが動作すること
            TestTargetClass actual = new TestTargetClass();
            Map<String, String> expected = new HashMap<String, String>();

            actual.setIntegerVal(3);
            expected.put("integerVal", "3");
            Assertion.assertProperties("test", expected, actual);

            expected.put("integerVal", "2");
            try {
                Assertion.assertProperties("test", expected, actual);
                fail("例外が発生するはず");
            } catch (ComparisonFailure error) {
                // OK
            }
        }

        {
            // short型変数のチェック
            TestTargetClass actual = new TestTargetClass();
            Map<String, String> expected = new HashMap<String, String>();

            actual.setShortVal((short) 3);
            expected.put("shortVal", "3");
            Assertion.assertProperties("test", expected, actual);

            expected.put("shortVal", "4");
            try {
                Assertion.assertProperties("test", expected, actual);
                fail("例外が発生するはず");
            } catch (ComparisonFailure error) {
                // OK
            }
        }
        {
            // int型変数のチェック
            TestTargetClass actual = new TestTargetClass();
            Map<String, String> expected = new HashMap<String, String>();

            actual.setIntVal(4);
            expected.put("intVal", "4");
            Assertion.assertProperties("test", expected, actual);

            expected.put("intVal", "5");
            try {
                Assertion.assertProperties("test", expected, actual);
                fail("例外が発生するはず");
            } catch (ComparisonFailure error) {
                // OK
            }
        }
        {
            // int型変数のチェック
            TestTargetClass actual = new TestTargetClass();
            Map<String, String> expected = new HashMap<String, String>();

            actual.setLongVal(4);
            expected.put("longVal", "4");
            Assertion.assertProperties("test", expected, actual);

            expected.put("longVal", "5");
            try {
                Assertion.assertProperties("test", expected, actual);
                fail("例外が発生するはず");
            } catch (ComparisonFailure error) {
                // OK
            }
        }
        {
            // float型変数のチェック
            TestTargetClass actual = new TestTargetClass();
            Map<String, String> expected = new HashMap<String, String>();

            actual.setFloatVal(4f);
            expected.put("floatVal", "4.0");
            Assertion.assertProperties("test", expected, actual);

            expected.put("floatVal", "5.0");
            try {
                Assertion.assertProperties("test", expected, actual);
                fail("例外が発生するはず");
            } catch (ComparisonFailure error) {
                // OK
            }
        }
        {
            // double型変数のチェック
            TestTargetClass actual = new TestTargetClass();
            Map<String, String> expected = new HashMap<String, String>();

            actual.setDoubleVal(5);
            expected.put("doubleVal", "5.0");
            Assertion.assertProperties("test", expected, actual);

            expected.put("doubleVal", "6.0");
            try {
                Assertion.assertProperties("test", expected, actual);
                fail("例外が発生するはず");
            } catch (ComparisonFailure error) {
                // OK
            }
        }
        {
            // boolean型変数のチェック
            TestTargetClass actual = new TestTargetClass();
            Map<String, String> expected = new HashMap<String, String>();

            // ここはOK
            actual.setBoolVal(true);
            expected.put("boolVal", "true");
            Assertion.assertProperties("test", expected, actual);

            expected.put("boolVal", "false");
            try {
                Assertion.assertProperties("test", expected, actual);
                fail("例外が発生するはず");
            } catch (ComparisonFailure error) {
                // OK
            }

            actual.setBoolVal(false);
            expected.put("boolVal", "false");
            // ここはOK
            Assertion.assertProperties("test", expected, actual);

            expected.put("boolVal", "true");
            try {
                Assertion.assertProperties("test", expected, actual);
                fail("例外が発生するはず");
            } catch (ComparisonFailure error) {
                // OK
            }
        }
        {
            // BigDecimal型変数のチェック
            TestTargetClass actual = new TestTargetClass();
            Map<String, String> expected = new HashMap<String, String>();

            actual.setDecimalVal(new BigDecimal("0.0000000001"));
            expected.put("decimalVal", "0.0000000001");
            Assertion.assertProperties("test", expected, actual);

            expected.put("decimalVal", "0.0000000002");
            try {
                Assertion.assertProperties("test", expected, actual);
                fail("例外が発生するはず");
            } catch (ComparisonFailure error) {
                // OK
            }
        }
        {
            // メッセージ指定なし版のメソッド呼び出し
            TestTargetClass actual = new TestTargetClass();
            Map<String, String> expected = new HashMap<String, String>();

            // ここはOK
            actual.setBoolVal(true);
            expected.put("boolVal", "true");
            Assertion.assertProperties(expected, actual);

            expected.put("boolVal", "false");
            try {
                Assertion.assertProperties(expected, actual);
                fail("例外が発生するはず");
            } catch (ComparisonFailure error) {
                // OK
            }

            actual.setBoolVal(false);
            expected.put("boolVal", "false");
            // ここはOK
            Assertion.assertProperties(expected, actual);

            expected.put("boolVal", "true");
            try {
                Assertion.assertProperties(expected, actual);
                fail("例外が発生するはず");
            } catch (ComparisonFailure error) {
                // OK
            }
        }
        {

            // 実際のオブジェクトに対応するプロパティがない場合
            TestTargetClass actual = new TestTargetClass();

            Map<String, String> expected = new HashMap<String, String>();
            expected.put("hoge", "fuga");  // 存在しないプロパティ
            try {
                Assertion.assertProperties("test", expected, actual);
                fail("期待した例外が発生しませんでした。");
            } catch (AssertionError e) {
                assertThat(e.getMessage(), containsString("does not have property hoge"));
                return;
            }
        }
    }

    /**
     * Mapの比較が成功すること。<br/>
     * null同士の比較は等価と見なされること。
     */
    @Test
    public void testAssertMapEqualsNull() {
        Assertion.assertMapEquals(null, null);
    }

    /**
     * Mapの比較が成功すること。<br/>
     * 空のMap同士の比較は等価とみなされること。
     */
    @Test
    public void testAssertMapEqualsEmpty() {
        Assertion.assertMapEquals(new HashMap<String, String>(0),
                new HashMap<String, Object>(0));
    }

    /** Mapの比較が成功すること。<br/> */
    @Test
    public void testAssertMapEquals() {
        // 期待値
        Map<String, String> expected = new HashMap<String, String>() {
            {
                put("one", "1");
                put("two", "2");
            }
        };
        // 実際の値
        Map<String, Object> actual = new TreeMap<String, Object>() {
            {
                // Integer でも文字列で等価であればOK
                put("one", Integer.valueOf(1));
                // どんな型でも文字列で等価であればOK
                put("two", new Object() {
                    @Override
                    public String toString() {
                        return "2";
                    }
                });
            }
        };
        // OK
        Assertion.assertMapEquals(expected, actual);
    }

    /** Mapの比較に失敗した場合、例外が発生すること。 */
    @Test
    public void testAssertMapEqualsFail() {
        // 期待値
        final Map<String, String> expected = new HashMap<String, String>() {
            {
                put("a", "A");
                put("b", "B");
            }
        };
        // 実際の値
        final Map<String, Object> actual = new HashMap<String, Object>() {
            {
                put("b", "B");
                put("c", "C");
                put("a", "A");
            }
        };

        new Trap("異なる要素を含むMapは等価とみなされないこと。") {
            @Override
            protected void shouldFail() throws Exception {
                Assertion.assertMapEquals(
                        "additional message on failure.",
                        expected,
                        actual);
            }
        }.capture(ComparisonFailure.class)
                .whichMessageContains(
                        "expected:<{a=A, b=B[]}> but was:<{a=A, b=B[, c=C]}>")  // ソートされて比較されること
                .whichMessageStartsWith(
                        "additional message on failure.");     // 引数で渡したメッセージが付加されていること
    }


    /**
     * {@literal List<Map>}の比較が成功すること。<br/>
     * null同士の比較は等価と見なされること。
     */
    @Test
    public void testAssertListMapEqualsNull() {
        Assertion.assertListMapEquals(null, null);
    }

    /**
     * {@literal List<Map>}の比較が成功すること。<br/>
     * 空のList同士の比較は等価とみなされること。
     */
    @Test
    public void testAssertListMapEqualsEmpty() {
        Assertion.assertListMapEquals(new ArrayList<Map<String, String>>(0),
                new ArrayList<Map<String, Object>>(0));
    }

    /** {@literal List<Map>}の比較が成功すること。<br/> */
    @Test
    public void testAssertListMapEquals() {
        List<Map<String, String>> expected = new ArrayList<Map<String, String>>();
        expected.add(new HashMap<String, String>() {
            {
                put("one", "1");
                put("two", "2");
            }
        });
        List<Map<String, Object>> actual = new ArrayList<Map<String, Object>>();
        actual.add(new HashMap<String, Object>() {
            {
                put("one", 1);
                put("two", 2);
            }
        });
        Assertion.assertListMapEquals(expected, actual);
    }

    /** Listの比較に失敗した場合、例外が発生すること。 */
    @Test
    public void testAssertListMapEqualsFail() {
        final List<Map<String, String>> expected = new ArrayList<Map<String, String>>();
        expected.add(new HashMap<String, String>() {
            {
                put("one", "1");
                put("two", "2");
                put("three", "3");
            }
        });
        final List<Map<String, Object>> actual = new ArrayList<Map<String, Object>>();
        actual.add(new HashMap<String, Object>() {
            {
                put("one", 1);
                put("two", 2);
            }
        });
        new Trap("異なる要素を含むListは等価とみなされないこと。") {
            @Override
            protected void shouldFail() throws Exception {
                Assertion.assertListMapEquals("message on failure.", expected, actual);
            }
        }.capture(ComparisonFailure.class)
                .whichMessageStartsWith("message on failure.")     // 引数で渡したメッセージが付加されていること
                .whichMessageContains("line no=[1]");              // 比較失敗した行番号が含まれていること
    }

    /**
     * 内部の型が異なっている場合、文字列として比較されること。
     */
    @Test
    public void testAssertEqualsDataRecords() {
        // 実際の値（数値）
        DataRecord actual = new DataRecord();
        actual.put("one", 1);
        actual.put("two", new BigDecimal(2));
        actual.put("three", 3L);
        actual.put("four", 4.0f);
        // 期待値（文字列）
        DataRecord expected = new DataRecord();
        expected.put("one", "1");
        expected.put("two", "2");
        expected.put("three", "3");
        expected.put("four", "4.0");
        Assertion.assertEquals("等価とみなされる", expected, actual);
        Assertion.assertEquals("等価とみなされる", actual, expected);

        // 電文オブジェクトがnull
        try {
            Assertion.assertEquals("nullじゃない", expected, null);
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof ComparisonFailure);
        }
    }

    @Test
    public void testFailComparingBigDecimal() throws Exception {
        try {
            Assertion.failComparing("test", new BigDecimal("0.0000000001"), new BigDecimal("0.0000000002"));
        } catch (ComparisonFailure e) {
            assertThat(e.getExpected(), is("0.0000000001"));
            assertThat(e.getActual(), is("0.0000000002"));
        }
    }

    public static final class TestTargetClass {

        private String name;

        private Integer integerVal;

        private short shortVal;

        private int intVal;

        private long longVal;

        private float floatVal;

        private double doubleVal;

        private boolean boolVal;

        private BigDecimal decimalVal;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getIntegerVal() {
            return integerVal;
        }

        public void setIntegerVal(Integer integerVal) {
            this.integerVal = integerVal;
        }

        public short getShortVal() {
            return shortVal;
        }

        public void setShortVal(short shortVal) {
            this.shortVal = shortVal;
        }

        public int getIntVal() {
            return intVal;
        }

        public void setIntVal(int intVal) {
            this.intVal = intVal;
        }

        public long getLongVal() {
            return longVal;
        }

        public void setLongVal(long longVal) {
            this.longVal = longVal;
        }

        public float getFloatVal() {
            return floatVal;
        }

        public void setFloatVal(float floatVal) {
            this.floatVal = floatVal;
        }

        public double getDoubleVal() {
            return doubleVal;
        }

        public void setDoubleVal(double doubleVal) {
            this.doubleVal = doubleVal;
        }

        public boolean getBoolVal() {
            return boolVal;
        }

        public void setBoolVal(boolean boolVal) {
            this.boolVal = boolVal;
        }

        public BigDecimal getDecimalVal() {
            return decimalVal;
        }

        public void setDecimalVal(BigDecimal decimalVal) {
            this.decimalVal = decimalVal;
        }
    }

}
