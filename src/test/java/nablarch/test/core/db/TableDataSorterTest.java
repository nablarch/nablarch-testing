package nablarch.test.core.db;


import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import nablarch.core.db.connection.TransactionManagerConnection;
import nablarch.core.repository.SystemRepository;
import nablarch.test.RepositoryInitializer;
import nablarch.test.SystemPropertyResource;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Expectations;
import mockit.Mocked;

/**
 * {@link TableDataSorter}のテストクラス。
 *
 * @author T.Kawasaki
 */
@RunWith(DatabaseTestRunner.class)
public class TableDataSorterTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    @Rule
    public SystemPropertyResource systemPropertyResource = new SystemPropertyResource();
    
    private Connection conn;

    /** 共通の準備処理 */
    @BeforeClass
    public static void createFKTables() {
        VariousDbTestHelper.dropTable(Stranger.class);
        VariousDbTestHelper.dropTable(Son.class);
        VariousDbTestHelper.dropTable(Family.class);
        VariousDbTestHelper.dropTable(Daughter.class);
        VariousDbTestHelper.dropTable(Father.class);
        VariousDbTestHelper.dropTable(Granpa.class);

        VariousDbTestHelper.createTable(Granpa.class);
        VariousDbTestHelper.createTable(Father.class);
        VariousDbTestHelper.createTable(Daughter.class);
        VariousDbTestHelper.createTable(Son.class);
        VariousDbTestHelper.createTable(Stranger.class);
    }

    @Before
    public void before() throws Throwable {
        conn = VariousDbTestHelper.getNativeConnection();
    }

    @After
    public void after() throws Throwable {
        conn.close();
    }

    /**
     * 外部キー制約の依存関係にそってテーブルデータのソートが行われること。
     *
     * @throws SQLException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSortByTableData() throws SQLException {
        TableDataSorter sut = new TableDataSorter(conn, SystemRepository.getString("nablarch.db.schema"));

        List<TableData> tables = load("DAUGHTER", "GRANPA", "FATHER", "SON", "STRANGER");
        List<TableData> sorted = sut.sortTableDataByFK(tables);
        Iterator<TableData> itr = sorted.iterator();
        assertThat(itr.next()
                .getTableName(), anyOf(is("STRANGER"), is("GRANPA")));
        assertThat(itr.next()
                .getTableName(), anyOf(is("STRANGER"), is("GRANPA")));
        assertThat(itr.next()
                .getTableName(), is("FATHER"));
        assertThat(itr.next()
                .getTableName(), anyOf(is("DAUGHTER"), is("SON")));
        assertThat(itr.next()
                .getTableName(), anyOf(is("DAUGHTER"), is("SON")));
    }

    /**
     * 外部キー制約の依存関係にそってテーブル名のソートが行われること。
     *
     * @throws SQLException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSortByTableName() throws SQLException {
        TableDataSorter sut = new TableDataSorter(conn, SystemRepository.getString("nablarch.db.schema"));
        List<String> sorted = sut.sortTableNamesByFK(Arrays.asList(
                "DAUGHTER", "GRANPA", "FATHER", "SON", "STRANGER"));
        Iterator<String> itr = sorted.iterator();
        assertThat(itr.next(), anyOf(is("STRANGER"), is("GRANPA")));
        assertThat(itr.next(), anyOf(is("STRANGER"), is("GRANPA")));
        assertThat(itr.next(), is("FATHER"));
        assertThat(itr.next(), anyOf(is("DAUGHTER"), is("SON")));
        assertThat(itr.next(), anyOf(is("DAUGHTER"), is("SON")));
    }

    /**
     * nablarch.db.schema に値がセットされていなかった場合に例外が発生すること。
     */
    @Test
    public void testSortMethodWithoutSchemaName() {
        RepositoryInitializer.reInitializeRepository("nablarch/test/core/db/no-schema-db-default.xml");

        try {
            TableDataSorter.sort(null, null);
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("nablarch.db.schema"));
        }
    }

    @Mocked private TransactionManagerConnection mockTranConn;

    /** フラグが設定されていた場合、ソートがスキップされること。 */
    @Test
    public void testSuppressSortTable() {

        RepositoryInitializer.reInitializeRepository("nablarch/test/core/db/suppress-sort-table.xml");

        new Expectations() {{
            mockTranConn.getConnection();
            result = conn;
        }};

        final DbInfo unusedDbInfo = null;
        final String[] unusedColumnNames = new String[0];
        final List<TableData> originalOrder = Arrays.asList(
                new TableData(unusedDbInfo, "GRANPA", unusedColumnNames),
                new TableData(unusedDbInfo, "FATHER", unusedColumnNames),
                new TableData(unusedDbInfo, "SON", unusedColumnNames),
                new TableData(unusedDbInfo, "DAUGHTER", unusedColumnNames));

        final List<TableData> sorted = TableDataSorter.sort(originalOrder, mockTranConn);

        assertTrue("インスタンスが異なること", sorted != originalOrder);
        assertThat("元の順番のままであること", sorted, is(originalOrder));

        final List<TableData> reversed = TableDataSorter.reversed(originalOrder, mockTranConn);

        final List<TableData> reversedOriginalOrder = new ArrayList<TableData>(originalOrder);
        Collections.reverse(reversedOriginalOrder);
        assertTrue("インスタンスが異なること", reversed != originalOrder);
        assertThat("元の順番のままであること", reversed, is(reversedOriginalOrder));
    }

    private List<TableData> load(String... tableNames) throws SQLException {
        List<TableData> ret = new ArrayList<TableData>();
        for (String table : tableNames) {
            TableData t = new TableData();
            t.setDbInfo(repositoryResource.getComponentByType(DbInfo.class));
            t.setTableName(table);
            t.loadData();
            ret.add(t);
        }
        return ret;
    }
}