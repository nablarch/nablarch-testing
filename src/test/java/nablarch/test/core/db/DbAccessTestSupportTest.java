package nablarch.test.core.db;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import nablarch.core.db.transaction.SimpleDbTransactionManager;
import nablarch.test.Trap;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.TargetDb;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DatabaseTestRunner.class)
public class DbAccessTestSupportTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
            "nablarch/test/core/db/DbAccessTestSupportTest.xml");

    private DbAccessTestSupport target = new DbAccessTestSupport(getClass());

    @BeforeClass
    public static void createTable() {
        VariousDbTestHelper.createTable(TestTable.class);
    }

    @After
    public void tearDown() throws Throwable {
        try {
            target.endTransactions();
        } catch (Exception ignored) {
        }
    }

    @Test
    public void testBeginTransactions() {


        target.beginTransactions();

        // トランザクションは既に開始されているはず
        SimpleDbTransactionManager managerA = repositoryResource.getComponent("tranA");
        try {
            managerA.beginTransaction();
            fail("期待する例外が発生しませんでした。");
        } catch (IllegalStateException e) {
            String actual = e.getMessage();
            assertThat(actual, containsString("specified database connection name is already used."));
            assertThat(actual, containsString("tran-a"));
        }
        SimpleDbTransactionManager managerB = repositoryResource.getComponent("tranB");
        try {
            managerB.beginTransaction();
            fail("期待する例外が発生しませんでした。");
        } catch (IllegalStateException e) {
            String actual = e.getMessage();
            assertThat(actual, containsString("specified database connection name is already used."));
            assertThat(actual, containsString("tran-b"));
        }

    }

    @Test
    public void testBeginTransactionsFail() {
        repositoryResource.addComponent("dbAccessTest.dbTransactionName", "TranC");
        try {
            target.beginTransactions();
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("can't get from repository name=[TranC]"));
        }
    }


    @Test
    public void testAssertTableEquals() {

        VariousDbTestHelper.setUpTable(
                new TestTable("00001", 3L, "さしすせそ", 1L, new BigDecimal("1.1"), new Date(0L), new Timestamp(0L), "0    ",
                        "CLOBです1".toCharArray(), "aaaaa".getBytes(), true),
                new TestTable("00002", 1L, "\"かきくけこ", 2L, new BigDecimal("1.11"), new Date(0L), new Timestamp(0L),
                        "1    ", "CLOBです2".toCharArray(), "bbbbb".getBytes(), true),
                new TestTable("00003", 1L, "たちつてと", 2L, new BigDecimal("1.11"), new Date(0L), new Timestamp(0L), null,
                        "CLOBです3".toCharArray(), "ccccc".getBytes(), true),
                new TestTable("00004", 2L, "”あいう", 2L, new BigDecimal("1.11"), new Date(0L), new Timestamp(0L), null,
                        "CLOBです4".toCharArray(), "ddddd".getBytes(), true),
                new TestTable("00005", 0L, "null", 2L, new BigDecimal("1.11"), new Date(0L), new Timestamp(0L), "12345",
                        "CLOBです5".toCharArray(), null, true));

        target.assertTableEquals("testAssertTableEquals");
    }

    /**
     * データタイプ EXPECTED_COMPLETE_TABLE が正常に動作することを確認する。
     * アサート時に、指定しなかったカラムにデフォルト値が補われて比較されること。
     */
    @Test
    public void testExpectedCompleteTable() {
        final String sheetName = "testExpectedCompleteTable";

        target.setUpDb(sheetName);
        target.assertTableEquals(sheetName);

    }

    /**
     * データタイプ EXPECTED_COMPLETE_TABLE が正常に動作することを確認する。
     * アサート時に、指定しなかったカラムにデフォルト値が補われて比較されること。
     */
    @Test
    public void testExpectedCompleteTableFail() {

        final String sheetName = "testExpectedCompleteTableFail";
        target.setUpDb(sheetName);     // 一部のカラムをデフォルト値と異なる値でセットアップ
        try {
            target.assertTableEquals(sheetName); // 省略されたカラムをデフォルト値としてアサート
            fail("比較に失敗するはず。");
        } catch (ComparisonFailure e) {
            assertThat(e.getMessage(), containsString(
                    "table=TEST_TABLE line=1 column=VARCHAR2_COL expected:<[ ]> but was:<[a]>"));
        }
    }

    @Test
    public void testSetUpDbTimestamp() {
        final String sheetName = "testSetUpDb";
        final List<String> expected = Arrays.asList(
                "2001-01-01 12:34:56.0",
                "2002-02-02 12:34:56.123",
                "2003-03-03 00:00:00.0",
                "2004-04-04 12:34:56.787");
        target.setUpDb(sheetName);

        List<TestTable> result = VariousDbTestHelper.findAll(TestTable.class, "pkCol1", "pkCol2");
        assertThat(result.size(), is(expected.size()));
        for (int i = 0; i < expected.size(); i++) {
            assertEquals("no." + (i + 1), expected.get(i), result.get(i).timestampCol.toString());
        }
    }

    /**
     * BLOB型のカラムに着目したテスト。
     */
    @Test
    public void testSetUpDbBlob() {
        final byte[][] expected = {
                new byte[] {0x31},
                null,
                new byte[] {0x33},
                null
        };
        target.setUpDb("testSetUpDb");

        List<TestTable> result = VariousDbTestHelper.findAll(TestTable.class, "pkCol1", "pkCol2");
        assertThat(result.size(), is(expected.length));
        assertEquals(1, result.get(0).blobCol.length);
        assertEquals(0x31, result.get(0).blobCol[0]);
        assertEquals(null, result.get(1).blobCol);
        assertEquals(1, result.get(2).blobCol.length);
        assertEquals(0x33, result.get(2).blobCol[0]);
        assertEquals(null, result.get(3).blobCol);
    }

    /**
     * トランザクションが開始した状態でトランザクションを再度開始しようとした場合、
     * 例外が発生すること。
     */
    @Test
    public void testBeginTransactionTwice() {
        target.beginTransactions();
        new Trap() {
            @Override
            protected void shouldFail() throws Exception {
                target.beginTransactions();
            }
        }.capture(IllegalStateException.class)
                .whichMessageContains("specified database connection name is already used");
    }


    /** beginとendを連続して起動できること。 */
    @Test
    public void testBeginAndEndTransactionTwice() {
        target.beginTransactions();
        target.endTransactions();
        target.beginTransactions();
        target.endTransactions();
    }

    /**
     * {@link DbAccessTestSupport#assertTableEquals(String, String)} で
     * グループIDにマッチするテーブルデータが１つも存在しない場合、例外が発生すること。
     */
    @Test
    public void testAssertTableEqualsWithInvalidGroupId() {
        new Trap("存在しないグループIDを指定した場合、例外が発生する。") {
            @Override
            protected void shouldFail() throws Exception {
                target.assertTableEquals("testAssertTableEquals", "notExistsGroupId");
            }
        }.capture(IllegalArgumentException.class)
                .whichMessageStartsWith(" no table data found in the specified sheet.")
                .whichMessageContains("sheet=[testAssertTableEquals")
                .whichMessageContains("groupId=[notExistsGroupId]");
    }

    /**
     * 外部キー制約順にテーブルのセットアップができること。
     *
     * @throws SQLException 予期しない例外
     */
    @Test
    public void testSetUpDbInOrder() throws SQLException {
        TableDataSorterTest.createFKTables();

        // CREATE TABLE直後
        target.setUpDb("testSetUpDbInOrder");

        // テーブルにレコードがある状態で実行（削除ができること）
        target.setUpDb("testSetUpDbInOrder");
    }

    /**
     * テストデータのソート機能をOFF(nablarch.suppress-table-sort=true)にし、
     * Excelの記載順がFKと合致している場合。
     *
     * Excelに記載順でテーブルのセットアップが行われ、
     * FK違反エラーが発生しないこと。
     *
     * @throws SQLException 予期しない例外
     */
    @Test
    public void testSetUpDbOnExcel() throws SQLException {

        repositoryResource.addComponent("nablarch.suppress-table-sort", "true");
        TableDataSorterTest.createFKTables();

        // CREATE TABLE直後
        target.setUpDb("testSetUpDbInOrder");

        // テーブルにレコードがある状態で実行（削除ができること）
        target.setUpDb("testSetUpDbInOrder");
    }

    /**
     * テストデータのソート機能をOFF(nablarch.suppress-table-sort=true)にし、
     * Excelの記載順がFKと合致していない場合。
     *
     * Excelに記載順でテーブルのセットアップが行われ、
     * FK違反エラーが発生すること。
     */
    @Test
    @TargetDb(include = TargetDb.Db.ORACLE)
    public void testSetUpDbOnInvalidExcel_Oracle() {

        repositoryResource.addComponent("nablarch.suppress-table-sort", "true");
        TableDataSorterTest.createFKTables();

        // CREATE TABLE直後
        try {
            target.setUpDb("testSetUpDbOnInvalidExcel");
            fail("FK違反エラーが発生する");
        } catch (RuntimeException e) {
            SQLException cause = (SQLException) e.getCause();
            assertThat(cause.getErrorCode(), is(2291));
        }
    }
    
    @Test
    @TargetDb(include = TargetDb.Db.H2)
    public void testSetUpDbOnInvalidExcel_H2() {

        repositoryResource.addComponent("nablarch.suppress-table-sort", "true");
        TableDataSorterTest.createFKTables();

        // CREATE TABLE直後
        try {
            target.setUpDb("testSetUpDbOnInvalidExcel");
            fail("FK違反エラーが発生する");
        } catch (RuntimeException e) {
            SQLException cause = (SQLException) e.getCause();
            assertThat(cause.getErrorCode(), is(23506));
        }
    }
    
    @Test
    public void testSetUpDbOnInvalidExcel_Postgres() {

        repositoryResource.addComponent("nablarch.suppress-table-sort", "true");
        TableDataSorterTest.createFKTables();

        // CREATE TABLE直後
        try {
            target.setUpDb("testSetUpDbOnInvalidExcel");
            fail("FK違反エラーが発生する");
        } catch (RuntimeException e) {
            SQLException cause = (SQLException) e.getCause();
            assertThat(cause.getSQLState(), is("23503"));
        }
    }
}
