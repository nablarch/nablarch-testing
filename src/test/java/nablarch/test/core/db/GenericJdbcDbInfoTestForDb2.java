package nablarch.test.core.db;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;


/**
 * {@link nablarch.test.core.db.GenericJdbcDbInfo}のテスト。
 *
 * @author Hisaaki Sioiri
 */
public class GenericJdbcDbInfoTestForDb2 extends GenericJdbcDbInfoTestBase {

    /** テーブルを準備する。 */
    @Override
    protected void prepareTable() {

        try {
            executeQuietly("drop table non_pk");
            executeQuietly("drop table one_pk");
            executeQuietly("drop table multi_pk");
            executeQuietly("drop table unique_index");
            executeQuietly("drop index idx_unq_1");
            executeQuietly("drop index idx_unq_2");
        } catch (Exception e) {
            // NOP
        }

        executeQuietly(
                "create table non_pk ("
                        + " char_col char(10) not null,"
                        + " varchar_col varchar(2000) not null,"
                        + " number_col decimal(20) not null,"
                        + " number_col2 decimal(10,3) not null,"
                        + " blob_col blob not null,"
                        + " date_col date not null,"
                        + " timestamp_col timestamp not null)");
        executeQuietly(
                "create table one_pk ("
                        + " pk_col char(10) not null,"
                        + " not_pk1 char(1) not null,"
                        + " not_pk2 smallint not null,"
                        + " primary key(pk_col))");
        executeQuietly(
                "create table multi_pk ("
                        + " pk_col1 char(10) not null,"
                        + " pk_col2 char(5) not null,"
                        + " pk_col3 char(1) not null,"
                        + " primary key(pk_col1, pk_col2, pk_col3))");
        executeQuietly(
                "create table unique_index ("
                        + " pk_col char(1) not null,"
                        + " unq_1 char(10) not null,"
                        + " unq_2_1 char(5) not null,"
                        + " unq_2_2 char(1) not null,"
                        + " primary key(pk_col))");
        executeQuietly("create unique index idx_unq_1 on unique_index (unq_1)");
        executeQuietly("create unique index idx_unq_2 on unique_index (unq_2_1, unq_2_2)");
    }

    /**
     * {@link nablarch.test.core.db.OracleDbInfo#getColumnType}のテスト。
     *
     * @see nablarch.test.core.db.OracleDbInfo#getColumnType(String, String)
     */
    @Test
    public void testGetColumnType() {

        // char
        assertThat(dbInfo.getColumnType("non_pk", "char_col"), is(java.sql.Types.CHAR));
        // varchar
        assertThat(dbInfo.getColumnType("non_pk", "varchar_col"), is(java.sql.Types.VARCHAR));
        // number
        assertThat(dbInfo.getColumnType("non_pk", "number_col"), is(java.sql.Types.DECIMAL));
        // blob
        assertThat(dbInfo.getColumnType("non_pk", "blob_col"), is(java.sql.Types.BLOB));
        // date
        // oracle5　からDATE型はjava.sql.Timestampにマッピングされる。
        assertThat(dbInfo.getColumnType("non_pk", "date_col"), is(java.sql.Types.DATE));
        // timestamp
        assertThat(dbInfo.getColumnType("non_pk", "timestamp_col"), is(java.sql.Types.TIMESTAMP));
    }
}
