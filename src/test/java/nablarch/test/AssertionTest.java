package nablarch.test;


import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.db.statement.SqlRow;
import nablarch.core.util.BinaryUtil;
import nablarch.test.core.db.DbAccessTestSupport;
import nablarch.test.core.db.TableData;
import nablarch.test.core.db.TestTable;
import nablarch.test.core.db.TransactionTemplate;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DatabaseTestRunner.class)
public class AssertionTest extends AssertionTestSupport {

    @Test
    public void testAssertTableEqualsStringListOfTableData() {
        // INSERT文実行
        VariousDbTestHelper.setUpTable(
                new TestTable("00001", 1L, "あ", 12345L, new BigDecimal("1234.123"), new Date(0L), new Timestamp(0L),
                        null, "CLOBです1".toCharArray(), "BLOBです1".getBytes(), true),
                new TestTable("00002", 2L, "い", 2L, new BigDecimal("22.123"), new Date(0L), new Timestamp(0L), "12345",
                        "CLOBです2".toCharArray(), "BLOBです2".getBytes(), true));

        TableData e = new TableData();
        e.setTableName("test_table");
        e.setDbInfo(dbInfo);
        e.setColumnNames(new String[] {"PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL",
                "NUMBER_COL2", "DATE_COL", "TIMESTAMP_COL", "NULL_COL", "CLOB_COL", "BLOB_COL", "BOOL_COL"});
        List<String> row = new ArrayList<String>();
        row.add("00001");
        row.add("1");
        row.add("あ");
        row.add("12345");
        row.add("1234.123");
        row.add("1970-01-01");
        row.add("1970-01-01 09:00:00.0");
        row.add(null);
        row.add("CLOBです1");
        row.add(BinaryUtil.convertToHexString("BLOBです1".getBytes()));
        row.add("true");
        e.addRow(row);
        row = new ArrayList<String>();
        row.add("00002");
        row.add("2");
        row.add("い");
        row.add("2");
        row.add("22.123");
        row.add("1970-01-01");
        row.add("1970-01-01 09:00:00.0");
        row.add("12345");
        row.add("CLOBです2");
        row.add(BinaryUtil.convertToHexString("BLOBです2".getBytes()));
        row.add("true");
        e.addRow(row);

        List<TableData> tables = new ArrayList<TableData>();
        tables.add(e);
        Assertion.assertTableEquals(tables);
        Assertion.assertTableEquals(e);
    }

    /**
     * 順序、型が異なるSqlResultSetでも比較できること。
     */
    @Test
    public void testAssertEqualsIgnoringOrderSqlResultSet() {
        // 期待値（格納順序がことなる）
        final List<Map<String, String>> expected = new ListMapBuilder<String, String>()
                .put("PK_COL1", "00002")
                .put("PK_COL2", "2")
                .put("VARCHAR2_COL", "い")
                .put("NUMBER_COL", "2")
                .put("NUMBER_COL2", "22.123")
                .put("NULL_COL", "00001")
                .put("TIMESTAMP_COL", "1970-01-01 09:00:00.0")
                .put("DATE_COL", "1970-01-01")
                .put("BLOB_COL", "null")
                .put("CLOB_COL", "null")
                .put("BOOL_COL", "true")
                .newRow()
                .put("PK_COL1", "00001")
                .put("PK_COL2", "1")
                .put("VARCHAR2_COL", "あ")
                .put("NUMBER_COL", "12345")
                .put("NUMBER_COL2", "1234.123")
                .put("NULL_COL", "null")
                .put("TIMESTAMP_COL", "1970-01-01 09:00:00.0")
                .put("DATE_COL", "1970-01-01")
                .put("BLOB_COL", "null")
                .put("CLOB_COL", "null")
                .put("BOOL_COL", "true")
                .build();

        // INSERT文実行
        VariousDbTestHelper.setUpTable(
                new TestTable("00001", 1L, "あ", 12345L, new BigDecimal("1234.123"), new Date(0L), new Timestamp(0L),
                        null, null, null, true),
                new TestTable("00002", 2L, "い", 2L, new BigDecimal("22.123"), new Date(0L), new Timestamp(0L), "00001",
                        null, null, true));

        new TransactionTemplate(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
                SqlResultSet resultSet = conn.prepareStatement("SELECT * FROM TEST_TABLE")
                        .retrieve();
                Assertion.assertEqualsIgnoringOrder("", expected, resultSet);
            }
        }.execute();
    }

    @Test
    public void testAssertSqlRowEquals() {

        // INSERT文実行
        VariousDbTestHelper.setUpTable(
                new TestTable("00001", 1L, "あ", 12345L, new BigDecimal("1234.123"), new Date(0L), new Timestamp(0L),
                        null, null, null, true));

        // expected

        final Map<String, String> expMap = new TreeMap<String, String>();
        expMap.put("NUMBER_COL", "12345");
        expMap.put("VARCHAR2_COL", "あ");
        expMap.put("PK_COL1", "00001");
        expMap.put("PK_COL2", "1");
        expMap.put("NUMBER_COL", "12345");
        expMap.put("NUMBER_COL2", "1234.123");
        expMap.put("DATE_COL", "1970-01-01");
        expMap.put("TIMESTAMP_COL", "1970-01-01 09:00:00.0");
        expMap.put("NULL_COL", null);

        new TransactionTemplate(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
                String stmt = "SELECT "
                        + "NUMBER_COL, "
                        + "VARCHAR2_COL, "
                        + "PK_COL1, "
                        + "PK_COL2, "
                        + "NUMBER_COL, "
                        + "NUMBER_COL2, "
                        + "DATE_COL, "
                        + "TIMESTAMP_COL, "
                        + "NULL_COL "
                        + "FROM "
                        + "TEST_TABLE";
                SqlPStatement s = conn.prepareStatement(stmt);
                SqlResultSet actual = s.retrieve();
                assertEquals(1, actual.size());
                SqlRow actualRow = actual.get(0);
                Assertion.assertSqlRowEquals(expMap, actualRow);
            }
        }.execute();
    }

    @Test
    public void testAssertSqlResultSetEquals() {
        // INSERT文実行
        VariousDbTestHelper.setUpTable(
                new TestTable("00001", 1L, "あ", 12345L, new BigDecimal("1234.123"), new Date(0L), new Timestamp(0L),
                        null, null, null, true),
                new TestTable("00002", 2L, "い", 2L, new BigDecimal("22.123"), new Date(0L), new Timestamp(0L), "00001",
                        null, null, true));

        // expected
        final List<Map<String, String>> expList = new ArrayList<Map<String, String>>();
        Map<String, String> expMap = null;
        expMap = new TreeMap<String, String>();
        expMap.put("NUMBER_COL", "12345");
        expMap.put("VARCHAR2_COL", "あ");
        expMap.put("PK_COL1", "00001");
        expMap.put("PK_COL2", "1");
        expMap.put("NUMBER_COL", "12345");
        expMap.put("NUMBER_COL2", "1234.123");
        expMap.put("DATE_COL", "1970-01-01");
        expMap.put("TIMESTAMP_COL", "1970-01-01 09:00:00.0");
        expMap.put("NULL_COL", null);
        expMap.put("BLOB_COL", null);
        expMap.put("CLOB_COL", null);
        expMap.put("BOOL_COL", "true");
        expList.add(expMap);
        expMap = new TreeMap<String, String>();
        expMap.put("NUMBER_COL", "2");
        expMap.put("VARCHAR2_COL", "い");
        expMap.put("PK_COL1", "00002");
        expMap.put("PK_COL2", "2");
        expMap.put("NUMBER_COL", "2");
        expMap.put("NUMBER_COL2", "22.123");
        expMap.put("DATE_COL", "1970-01-01");
        expMap.put("TIMESTAMP_COL", "1970-01-01 09:00:00.0");
        expMap.put("NULL_COL", "00001");
        expMap.put("BLOB_COL", null);
        expMap.put("CLOB_COL", null);
        expMap.put("BOOL_COL", "true");
        expList.add(expMap);

        new TransactionTemplate(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
                String stmt = "SELECT * FROM TEST_TABLE ORDER BY PK_COL1";
                SqlPStatement s = conn.prepareStatement(stmt);
                SqlResultSet actual = s.retrieve();
                Assertion.assertSqlResultSetEquals(expList, actual);
            }
        }.execute();
    }
}
