package nablarch.test.core.db;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hamcrest.CoreMatchers;

import nablarch.core.util.BinaryUtil;
import nablarch.test.RepositoryInitializer;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.TargetDb;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link nablarch.test.core.db.TableData}のテストクラス。
 *
 * @author Hisaaki Sioiri
 */
@RunWith(DatabaseTestRunner.class)
@TargetDb(exclude = {TargetDb.Db.POSTGRE_SQL, TargetDb.Db.DB2})
public class TableDataTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    @BeforeClass
    public static void beforeClass() {
        VariousDbTestHelper.createTable(TestTable.class);

    }

    @Before
    public void before() {
        VariousDbTestHelper.delete(TestTable.class);

    }

    /**
     * {@link TableData#loadData()} のテスト。<br>
     *
     */
    @Test
    public void testLoadDataOneData() throws Exception {

        // テスト用日付
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd HHmmss");
        java.util.Date testDate = format.parse("20100830 012345");
        long longDateTime = testDate.getTime();

        // INSERT文実行
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyyMMdd");
        long longDate = formatDate.parse("20100830").getTime();
        VariousDbTestHelper.setUpTable(
                new TestTable("00001", 1L, "あいうえお", 1234567890L, new BigDecimal("1234567.123"), new Date(longDate),
                        new Timestamp(longDateTime), null, "CLOBです0".toCharArray(), new byte[100], true));

        // ターゲット実行
        TableData table = new TableData();
        table.setDbInfo(repositoryResource.getComponentByType(DbInfo.class));
        table.setTableName("TEST_TABLE");
        table.loadData();

        // 結果確認
        assertThat("1件ロードされていること。", table.size(), is(1));
        assertThat("PK_COL1", (String) table.getValue(0, "pk_col1"), is("00001"));
        assertThat("PK_COL2", ((Number) table.getValue(0, "pk_col2")).intValue(), is(1));
        assertThat("VARCHAR2_COL", (String) table.getValue(0, "varchar2_col"), is("あいうえお"));
        assertThat("NUMBER_COL", ((Number) table.getValue(0, "number_col")).longValue(), is(1234567890L));
        assertThat("NUMBER_COL2", (BigDecimal) table.getValue(0, "number_col2"), is(new BigDecimal("1234567.123")));

        if (VariousDbTestHelper.getTargetDatabase() == TargetDb.Db.ORACLE) {
            assertThat("DATE_COL", (java.sql.Timestamp) table.getValue(0, "date_col"), is(java.sql.Timestamp.valueOf("2010-08-30 00:00:00.000")));
        } else {
            assertThat("DATE_COL", (java.sql.Date) table.getValue(0, "date_col"), is(java.sql.Date.valueOf("2010-08-30")));
        }
        assertThat("TIMESTAMP_COL", (java.sql.Timestamp) table.getValue(0, "timestamp_col"), is(Timestamp.valueOf(
                "2010-08-30 01:23:45")));
        assertThat("NULL_COL", table.getValue(0, "null_col"), nullValue());
        assertThat("CLOB_COL", (String) table.getValue(0, "clob_col"), is("CLOBです0"));
        assertThat("BLOB_COL", (String) table.getValue(0, "blob_col"), is(BinaryUtil.convertToHexString(new byte[100])));
        assertThat("BOOL_COL", table.getValue(0, "bool_col"), anyOf(
                CoreMatchers.<Object>is(new BigDecimal(1)), CoreMatchers.<Object>is(true)));
    }

    @Test
    public void testLoadDataManyData() throws ParseException {
        VariousDbTestHelper.setUpTable(
                new TestTable("00002", 3L, "さしすせそ", 1L, new BigDecimal(10), new Date(0L), new Timestamp(0L), null,
                        "CLOBです1".toCharArray(), "BLOBです1".getBytes(), true),
                new TestTable("00002", 1L, "かきくけこ", 2L, new BigDecimal(20), new Date(0L), new Timestamp(0L), "12345",
                        "CLOBです2".toCharArray(), "BLOBです2".getBytes(), true));

        // ターゲット実行
        TableData table = new TableData();
        table.setTableName("test_table");
        table.setDbInfo(repositoryResource.getComponentByType(DbInfo.class));
        table.loadData();

        // 主キーでソートされていること
        assertThat("2件ロードされていること。", table.size(), is(2));

        assertThat("1件目:PK_COL1", (String) table.getValue(0, "pk_col1"), is("00002"));
        assertThat("1件目:PK_COL2", ((Number) table.getValue(0, "pk_col2")).intValue(), is(1));
        assertThat("1件目:VARCHAR2_COL", (String) table.getValue(0, "varchar2_col"), is("かきくけこ"));
        assertThat("1件目:CLOB_COL", (String) table.getValue(0, "clob_col"), is("CLOBです2"));
        assertThat("1件目:BLOB_COL", (String) table.getValue(0, "blob_col"), is(BinaryUtil.convertToHexString("BLOBです2".getBytes())));
        assertThat("1件目:BOOL_COL", table.getValue(0, "bool_col"), anyOf(
                CoreMatchers.<Object>is(new BigDecimal(1)), CoreMatchers.<Object>is(true)));

        assertThat("2件目:PK_COL1", (String) table.getValue(1, "pk_col1"), is("00002"));
        assertThat("2件目:PK_COL2", ((Number) table.getValue(1, "pk_col2")).intValue(), is(3));
        assertThat("2件目:VARCHAR2_COL", (String) table.getValue(1, "varchar2_col"), is("さしすせそ"));
        assertThat("2件目:CLOB_COL", (String) table.getValue(1, "clob_col"), is("CLOBです1"));
        assertThat("2件目:BLOB_COL", (String) table.getValue(1, "blob_col"), is(BinaryUtil.convertToHexString("BLOBです1".getBytes())));
        assertThat("2件目:BOOL_COL", table.getValue(1, "bool_col"), anyOf(
                CoreMatchers.<Object>is(new BigDecimal(1)), CoreMatchers.<Object>is(true)));
    }

    /**
     * {@link nablarch.test.core.db.TableData#replaceData()} のテスト。<br>
     * <ul>
     * <li>1件データを登録(未設定項目には初期値が設定されること)</li>
     * <li>複数件データを登録</li>
     * </ul>
     */
    @Test
    public void testReplaceData() {
        TableData table = new TableData();
        table.setTableName("test_table");
        table.setColumnNames(new String[] {"PK_COL1", "PK_COL2"});
        table.setDefaultValues(new MockDefaultValues());
        table.setDbInfo(repositoryResource.getComponentByType(DbInfo.class));

        // 1件登録で、主キーのみを設定
        List<String> row = new ArrayList<String>();
        row.add("00001");
        row.add("02");
        table.addRow(row);
        table.replaceData();

        // DBデータの確認
        List<TestTable> result = VariousDbTestHelper.findAll(TestTable.class);
        assertThat("1件登録されていること", result.size(), is(1));
        assertThat(result.get(0).pkCol1, is("00001"));
        assertThat(result.get(0).pkCol2, is(2L));
        assertThat("文字列項目の初期値は、ブランク", result.get(0).varchar2Col, is(" "));
        assertThat("数値項目の初期値は、0", result.get(0).numberCol, is(0L));
        assertThat("数値項目の初期値は、0", result.get(0).numberCol2.compareTo(new BigDecimal("0.000")), is(0));
        assertThat("日付項目の初期値はepoc", result.get(0).timestampCol.getTime(), is(0L));
        assertThat("Clob型項目の初期値は、ブランク", new String(result.get(0).clobCol), equalToIgnoringWhiteSpace(""));
        assertThat("Blob型項目の初期値は、byte[10]", result.get(0).blobCol.length, is(10));
        assertThat("Boolean型項目の初期値は、false", result.get(0).boolCol, is(false));

        // 複数件登録を実行
        table = new TableData();
        table.setTableName("test_table");
        table.setDbInfo(repositoryResource.getComponentByType(DbInfo.class));
        table.setColumnNames(new String[] {
                "PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL", "NUMBER_COL2", "DATE_COL", "TIMESTAMP_COL",
                "CLOB_COL", "BLOB_COL", "BOOL_COL"
        });
        ArrayList<String> row2 = new ArrayList<String>();
        row2.add("00001");
        row2.add("01");
        row2.add("あ");
        row2.add("12345");
        row2.add("1234.123");
        row2.add("20100831");
        row2.add("20100831123456");
        row2.add("CLOBです1");
        row2.add(BinaryUtil.convertToHexString("BLOBです1".getBytes()));
        row2.add("1");
        table.addRow(row2);
        row2 = new ArrayList<String>();
        row2.add("00001");
        row2.add("02");
        row2.add("い");
        row2.add("111");
        row2.add("222.123");
        row2.add("20110831");
        row2.add("20120831123456");
        row2.add("CLOBです2");
        row2.add(null);
        row2.add("1");
        table.addRow(row2);
        row2 = new ArrayList<String>();
        row2.add("00001");
        row2.add("03");
        row2.add("う");
        row2.add("1111");
        row2.add("333.123");
        row2.add("20110831");
        row2.add("20120831123456");
        row2.add("CLOBです2");
        row2.add("");
        row2.add("1");
        table.addRow(row2);
        table.replaceData();

        // DBデータの確認
        result = VariousDbTestHelper.findAll(TestTable.class, "pkCol1", "pkCol2");
        assertThat("3件登録されていること", result.size(), is(3));

        assertThat(result.get(0).pkCol1, is("00001"));
        assertThat(result.get(0).pkCol2, is(1L));
        assertThat(result.get(0).varchar2Col, is("あ"));
        assertThat(result.get(0).numberCol, is(12345L));
        assertThat(result.get(0).numberCol2, is(new BigDecimal("1234.123")));
        assertThat(result.get(0).dateCol, is(Date.valueOf("2010-08-31")));
        assertThat(result.get(0).timestampCol, is(Timestamp.valueOf("2010-08-31 12:34:56.0")));
        assertThat(new String(result.get(0).clobCol), is("CLOBです1"));
        assertThat(result.get(0).blobCol.length, is("BLOBです1".getBytes().length));
        assertThat(result.get(0).boolCol, is(true));

        assertThat(result.get(1).pkCol1, is("00001"));
        assertThat(result.get(1).pkCol2, is(2L));
        assertThat(result.get(1).varchar2Col, is("い"));
        assertThat(result.get(1).numberCol, is(111L));
        assertThat(result.get(1).numberCol2, is(new BigDecimal("222.123")));
        assertThat(result.get(1).dateCol, is(Date.valueOf("2011-08-31")));
        assertThat(result.get(1).timestampCol, is(Timestamp.valueOf("2012-08-31 12:34:56.0")));
        assertThat(new String(result.get(1).clobCol), is("CLOBです2"));
        assertThat(result.get(1).blobCol, is(nullValue()));
        assertThat(result.get(1).boolCol, is(true));

        assertThat(result.get(2).pkCol1, is("00001"));
        assertThat(result.get(2).pkCol2, is(3L));
        assertThat(result.get(2).varchar2Col, is("う"));
        assertThat(result.get(2).numberCol, is(1111L));
        assertThat(result.get(2).numberCol2, is(new BigDecimal("333.123")));
        assertThat(result.get(2).dateCol, is(Date.valueOf("2011-08-31")));
        assertThat(result.get(2).timestampCol, is(Timestamp.valueOf("2012-08-31 12:34:56.0")));
        assertThat(new String(result.get(2).clobCol), is("CLOBです2"));
        assertThat(result.get(2).blobCol, is(nullValue()));
        assertThat(result.get(2).boolCol, is(true));
    }

    /** クローンが失敗した場合、非チェック例外（{@link RuntimeException}）がスローされること。 */
    @Test
    public void testCloneFail() {
        TableData target = new TableData() {
            @Override
            protected Object clone() throws CloneNotSupportedException {
                throw new CloneNotSupportedException("for test.");
            }
        };
        try {
            target.getClone();
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getCause(), is(instanceOf(CloneNotSupportedException.class)));
        }
    }

    /**
     * 省略されたカラムにデフォルト値の設定ができること。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testFillDefaultValues() throws Exception {
        RepositoryInitializer.initializeDefaultRepository();
        TableData target = new TableData();
        DbInfo dbInfo = repositoryResource.getComponentByType(DbInfo.class);
        assertNotNull(dbInfo);
        target.setDbInfo(dbInfo);
        target.setTableName("test_table");
        target.setColumnNames(new String[] {"pk_col1", "pk_col2"});
        target.setDefaultValues(new MockDefaultValues());
        target.addRow(Arrays.asList("00001", "01"));
        // 省略されたカラムはnullである
        assertThat(target.getValue(0, "varchar2col"), nullValue());
        // デフォルト値を設定
        target.fillDefaultValues();

        // 省略したカラムにデフォルト値が設定されていること
        assertEquals(" ", target.getValue(0, "varchar2col"));
        assertEquals("0", target.getValue(0, "number_col"));
        assertEquals("0", target.getValue(0, "number_col2"));
        assertEquals("1970-01-01 09:00:00.0", String.valueOf(target.getValue(0, "date_col")));
        assertEquals("1970-01-01 09:00:00.0", String.valueOf(target.getValue(0, "timestamp_col")));
        assertThat(String.valueOf(target.getValue(0, "clob_col")), equalToIgnoringWhiteSpace(""));
        assertEquals(BinaryUtil.convertToHexString(new byte[10]), target.getValue(0, "blob_col"));
        Assert.assertThat(target.getValue(0, "bool_col"), anyOf(
                CoreMatchers.<Object>is("0"), CoreMatchers.<Object>is(false)));
    }

    @Test
    public void testReplaceNullValue() {
        RepositoryInitializer.initializeDefaultRepository();
        TableData target = new TableData();
        DbInfo dbInfo = repositoryResource.getComponentByType(DbInfo.class);
        assertNotNull(dbInfo);
        target.setDbInfo(dbInfo);
        target.setTableName("test_table");
        target.setColumnNames(new String[] {"PK_COL1", "PK_COL2", "NULL_COL"});
        target.setDefaultValues(new BasicDefaultValues());
        target.addRow(Arrays.asList("00001", "01", null));
        target.replaceData();

        // DBデータの確認
        List<TestTable> result = VariousDbTestHelper.findAll(TestTable.class, "pkCol1", "pkCol2");
        assertThat("1件登録されていること", result.size(), is(1));
        assertThat(result.get(0).pkCol1, is("00001"));
        assertThat(result.get(0).pkCol2, is(1L));
        assertThat(result.get(0).nullCol, is(nullValue()));
    }

    /** JDBCタイムスタンプエスケープ形式で記載したデータをインサートできること。 */
    @Test
    public void testInsertJdbcTimestampEscape() {
        TableData target = new TableData(repositoryResource.getComponentByType(DbInfo.class), "test_table",
                new String[] {"pk_col1", "pk_col2", "timestamp_col"});
        target.setDefaultValues(new MockDefaultValues());
        target.addRow(Arrays.asList("00001", "1", "2000-01-01 12:34:56.0"));
        target.replaceData();
        // DBデータの確認
        List<TestTable> result = VariousDbTestHelper.findAll(TestTable.class, "pkCol1", "pkCol2");
        assertThat("1件登録されていること", result.size(), is(1));
        assertThat(result.get(0).pkCol1, is("00001"));
        assertThat(result.get(0).pkCol2, is(1L));
        assertThat(result.get(0).timestampCol, is(Timestamp.valueOf("2000-01-01 12:34:56.0")));
    }

    @Test
    public void testInsertyyyyMMddhhmmssS() {
        TableData target = new TableData(repositoryResource.getComponentByType(DbInfo.class), "test_table",
                new String[] {"pk_col1", "pk_col2", "timestamp_col"});
        target.setDefaultValues(new MockDefaultValues());
        target.addRow(Arrays.asList("00001", "1", "200001011234560"));
        target.replaceData();
        // DBデータの確認
        List<TestTable> result = VariousDbTestHelper.findAll(TestTable.class, "pkCol1", "pkCol2");
        assertThat("1件登録されていること", result.size(), is(1));
        assertThat(result.get(0).pkCol1, is("00001"));
        assertThat(result.get(0).pkCol2, is(1L));
        assertThat(result.get(0).timestampCol, is(Timestamp.valueOf("2000-01-01 12:34:56.0")));
    }

    /**
     * 数値型カラムにNULLを設定できること。
     * 
     */
    @Test
    public void testInsertNull() {
        VariousDbTestHelper.createTable(InsNullTestTable.class);
        try {
            TableData target = new TableData(repositoryResource.getComponentByType(DbInfo.class),
                                             "INS_NULL_TEST_TABLE",
                                             new String[]{"PK_COL", "NULLABLE_NUMBER"});
            target.setDefaultValues(new MockDefaultValues());
            target.addRow(Arrays.asList("00001", null));
            target.replaceData();

            // DBデータの確認
            List<InsNullTestTable> result = VariousDbTestHelper.findAll(InsNullTestTable.class);
            assertThat("1件登録されていること", result.size(), is(1));
            InsNullTestTable table = result.get(0);
            assertThat(table.pkCol, is("00001"));
            assertThat(table.nullableNumber, is(nullValue()));
        } finally {
            VariousDbTestHelper.delete(InsNullTestTable.class);
        }
    }
    
    /**
     * 数値型カラムにNULLを設定できること。
     *
     */
    @Test
    public void testInsertExponentNotation() {
        VariousDbTestHelper.createTable(InsNullTestTable.class);
        try {
            TableData target = new TableData(repositoryResource.getComponentByType(DbInfo.class),
                    "INS_NULL_TEST_TABLE",
                    new String[]{"PK_COL", "NULLABLE_NUMBER"});
            target.setDefaultValues(new MockDefaultValues());
            target.addRow(Arrays.asList("00001", "1.E-1"));
            target.replaceData();

            // DBデータの確認
            List<InsNullTestTable> result = VariousDbTestHelper.findAll(InsNullTestTable.class);
            
            assertThat("1件登録されていること", result.size(), is(1));
            InsNullTestTable table = result.get(0);
            assertThat(table.pkCol, is("00001"));
            assertThat("指数表記のデータでも正しく登録されること", table.nullableNumber.compareTo(new BigDecimal("0.1")), is(0));
        } finally {
            VariousDbTestHelper.delete(InsNullTestTable.class);
        }
    }

    @Entity
    @Table(name = "INS_NULL_TEST_TABLE")
    public static class InsNullTestTable {

        public InsNullTestTable() {
        }

        public InsNullTestTable(String pkCol, BigDecimal nullableNumber) {
            this.pkCol = pkCol;
            this.nullableNumber = nullableNumber;
        }

        @Id
        @Column(name = "PK_COL", length = 5, nullable = false)
        public String pkCol;


        @Column(name = "NULLABLE_NUMBER", precision = 10, scale = 3, nullable = true)
        public BigDecimal nullableNumber;
    }
}
