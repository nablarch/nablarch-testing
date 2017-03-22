package nablarch.test.core.db;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.util.StringUtil;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link GenericJdbcDbInfo}のテスト。
 *
 * @author Hisaaki Sioiri
 */
@RunWith(DatabaseTestRunner.class)
public abstract class GenericJdbcDbInfoTestBase {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    protected GenericJdbcDbInfo dbInfo;

    private static Set<String> created = new HashSet<String>();

    @Before
    public void setUp() throws SQLException {
        dbInfo = repositoryResource.getComponentByType(GenericJdbcDbInfo.class);

        String name = getClass().getName();
        if (!created.contains(name)) {
            created.add(name);
            prepareTable();
        }
    }

    protected abstract void prepareTable();

    /**
     * getPrimaryKeysのテスト
     *
     * @see nablarch.test.core.db.GenericJdbcDbInfo#getPrimaryKeys(String)
     */
    @Test
    public void testGetPrimaryKeys() {
        assertEqualsToIgnoreCase(dbInfo.getPrimaryKeys("non_pk"), new String[] {});

        // 主キーが1カラムの場合
        assertEqualsToIgnoreCase(dbInfo.getPrimaryKeys("one_pk"), new String[] {"PK_COL"});
        // 主キーが1カラムの場合(キャッシュのテスト）
        assertEqualsToIgnoreCase(dbInfo.getPrimaryKeys("one_pk"), new String[] {"PK_COL"});

        // 主キーが複数カラムの場合
        assertEqualsToIgnoreCase(dbInfo.getPrimaryKeys("multi_pk"), new String[] {"PK_COL1", "PK_COL2", "PK_COL3"});
    }

    /**
     * {@link nablarch.test.core.db.GenericJdbcDbInfo#getColumns(String)}のテスト
     */
    @Test
    public void testGetColumns() {
        // カラム名が取得されること
        String[] expectedColumnNames = new String[] {"CHAR_COL", "VARCHAR_COL", "NUMBER_COL", "NUMBER_COL2", "BLOB_COL",
                "DATE_COL", "TIMESTAMP_COL"};
        assertEqualsToIgnoreCase(dbInfo.getColumns("non_pk"), expectedColumnNames);

        // 主キーが存在した場合、主キーからむも含めて取得されること。
        assertEqualsToIgnoreCase(dbInfo.getColumns("one_pk"), new String[] {"PK_COL", "NOT_PK1", "NOT_PK2"});
        // キャッシュのテスト
        assertEqualsToIgnoreCase(dbInfo.getColumns("one_pk"), new String[] {"PK_COL", "NOT_PK1", "NOT_PK2"});
    }

    @Test
    public void testIsComputedColumn() {
        assertThat(dbInfo.isComputedColumn("", ""), is(false)); // 何を指定してもfalse
    }

    @Test()
    public void testGetColumnTypeFail() {
        try {
            dbInfo.getColumnType("non_pk", "invalid column name");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("can't get column type"));
        }
    }

    /**
     * {@link nablarch.test.core.db.GenericJdbcDbInfo#isUniqueIndex(String, String)}のテスト。
     */
    @Test
    public void testIsUniqueIndex() {
        // ユニークインデックスはtrue
        assertThat(dbInfo.isUniqueIndex("unique_index", "unq_1"), is(true));

        // 複合カラムのユニークインデックスもtrue
        assertThat(dbInfo.isUniqueIndex("unique_index", "unq_2_1"), is(true));
        assertThat(dbInfo.isUniqueIndex("unique_index", "unq_2_2"), is(true));

        // 主キーはfalse
        assertThat(dbInfo.isUniqueIndex("unique_index", "pk_col"), is(false));
    }

    /**
     * {@link nablarch.test.core.db.GenericJdbcDbInfo#getColumnLength(String, String)} のテスト
     */
    @Test
    public void testGetColumnLength() {
        // 各データタイプの桁数確認
        assertThat(dbInfo.getColumnLength("non_pk", "char_col"), is(10));
        assertThat(dbInfo.getColumnLength("non_pk", "varchar_col"), is(2000));
        assertThat(dbInfo.getColumnLength("non_pk", "number_col"), is(20));
        assertThat(dbInfo.getColumnLength("non_pk", "number_col2"), is(10));
    }

    @Test
    public void testIsNumberTypeColumn() {
        assertThat(dbInfo.isNumberTypeColumn("non_pk", "number_col"), is(true));
        assertThat(dbInfo.isNumberTypeColumn("non_pk", "char_col"), is(false));

        assertThat(dbInfo.isNumberTypeColumn(Types.DECIMAL), is(true));
        assertThat(dbInfo.isNumberTypeColumn(Types.DOUBLE), is(true));
        assertThat(dbInfo.isNumberTypeColumn(Types.BIGINT), is(true));
        assertThat(dbInfo.isNumberTypeColumn(Types.FLOAT), is(true));
        assertThat(dbInfo.isNumberTypeColumn(Types.INTEGER), is(true));
        assertThat(dbInfo.isNumberTypeColumn(Types.NUMERIC), is(true));
        assertThat(dbInfo.isNumberTypeColumn(Types.SMALLINT), is(true));
        assertThat(dbInfo.isNumberTypeColumn(Types.REAL), is(true));
        assertThat(dbInfo.isNumberTypeColumn(Types.BINARY), is(false));
    }

    @Test
    public void testIsDateTypeColumn() {
        assertThat(dbInfo.isDateTypeColumn("non_pk", "date_col"), is(true));
        assertThat(dbInfo.isDateTypeColumn("non_pk", "timestamp_col"), is(true));
        assertThat(dbInfo.isDateTypeColumn("non_pk", "varchar_col"), is(false));

        assertThat(dbInfo.isDateTypeColumn(Types.DATE), is(true));
        assertThat(dbInfo.isDateTypeColumn(Types.TIME), is(true));
        assertThat(dbInfo.isDateTypeColumn(Types.TIMESTAMP), is(true));
        assertThat(dbInfo.isDateTypeColumn(Types.CHAR), is(false));
    }

    @Test
    public void testIsBinaryTypeColumn() {
        assertThat(dbInfo.isBinaryTypeColumn("non_pk", "blob_col"), is(true));
        assertThat(dbInfo.isBinaryTypeColumn("non_pk", "timestamp_col"), is(false));
        assertThat(dbInfo.isBinaryTypeColumn("non_pk", "varchar_col"), is(false));

        assertThat(dbInfo.isBinaryTypeColumn(Types.BINARY), is(true));
        assertThat(dbInfo.isBinaryTypeColumn(Types.VARBINARY), is(true));
        assertThat(dbInfo.isBinaryTypeColumn(Types.LONGVARBINARY), is(true));
        assertThat(dbInfo.isBinaryTypeColumn(Types.BLOB), is(true));

        assertThat(dbInfo.isBinaryTypeColumn(Types.BIT), is(false));
    }

    /**
     * SQLを実行する。<br/>
     * 失敗しても処理を続行する。
     *
     * @param sql 実行対象SQL
     */
    protected void executeQuietly(final String sql) {
        new TransactionTemplate(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
                conn.prepareStatement(sql)
                        .execute();
            }
        }.execute();
    }

    /**
     * 大文字・小文字を区別せずに文字列の検証を行う。
     *
     * @param actual 結果
     * @param expected 期待値
     */
    protected void assertEqualsToIgnoreCase(final String actual, final String expected) {
        String upperActual = StringUtil.hasValue(actual) ? actual.toUpperCase() : actual;
        String upperExpected = StringUtil.hasValue(expected) ? expected.toUpperCase() : expected;
        assertThat(upperActual, is(upperExpected));
    }

    /**
     * 大文字・小文字を区別せずに文字列の検証を行う。
     *
     * @param actual 結果
     * @param expected 期待値
     */
    protected void assertEqualsToIgnoreCase(final String[] actual, final String[] expected) {
        if (actual == null || expected == null) {
            fail();
        } else if (actual != null && expected != null) {
            assertThat(actual.length, is(expected.length));
            for (int i = 0; i < expected.length; i++) {
                assertEqualsToIgnoreCase(actual[i], expected[i]);
            }
        }
    }
}
