package nablarch.test.core.db;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import nablarch.test.core.db.MasterDataRestorer.SqlLogWatchingFormatter;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link nablarch.test.core.db.MasterDataRestorer.TableDuplicator}のテスト
 *
 * @author T.Kawasaki
 */
@RunWith(DatabaseTestRunner.class)
public class TableRestorerTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    /** 初期化処理 */
    @BeforeClass
    public static void initialize() {
        VariousDbTestHelper.createTable(TestTable.class);
        VariousDbTestHelper.createTable(TestTableSsdMaster.class);

        VariousDbTestHelper.setUpTable(
                new TestTable("00001", 1L, "あ", 12345L, new BigDecimal("1234.123"), new Date(0L), new Timestamp(0L),
                        null, null, null, true),
                new TestTable("00002", 2L, "い", 2L, new BigDecimal("22.123"), new Date(0L), new Timestamp(0L),
                        "12345", null, null, true));

        VariousDbTestHelper.setUpTable(
                new TestTableSsdMaster("00001", 1L, "あ", 12345L, new BigDecimal("1234.123"),
                        new Date(0L), new Timestamp(0L), null, null, null, true),
                new TestTableSsdMaster("00002", 2L, "い", 2L, new BigDecimal("22.123"),
                        new Date(0L), new Timestamp(0L), "12345", null, null, true));
    }

    /** 終了処理 */
    @AfterClass
    public static void terminate() {
        // 他のテストが落ちないようにごみ掃除しておきます。
        SqlLogWatchingFormatter.getExecutedAndClear();
    }

    /** {@link nablarch.test.core.db.MasterDataRestorer.TableDuplicator#restoreAll()}のテスト */
    @Test
    public void testRestoreAll() {
        MasterDataRestorer.TableDuplicator target = new MasterDataRestorer.TableDuplicator(
                new HashSet<String>(Arrays.asList("test_table")),
                "ssd_master");
        target.restoreAll();
    }

    /** {@link nablarch.test.core.db.MasterDataRestorer.TableDuplicator#restoreAll()}のテスト */
    @Test
    public void testRestoreAllEmpty() {
        MasterDataRestorer.TableDuplicator target = new MasterDataRestorer.TableDuplicator(
                Collections.<String>emptySet(),
                "ssd_master");
        target.restoreAll();
    }
}
