package nablarch.test.core.db;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link MasterDataSetUpper}のテスト
 *
 * @author T.Kawasaki
 */
@RunWith(DatabaseTestRunner.class)
public class MasterDataSetUpperTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    /** バックアップ用スキーマ名 */
    private static final String BACKUP_SCHEMA = "SSD_MASTER";

    @BeforeClass
    public static void beforeClass() {
        VariousDbTestHelper.createTable(TestTable.class);
        VariousDbTestHelper.createTable(TestTableSsdMaster.class);
    }

    @Before
    public void setUp() throws Exception {
        VariousDbTestHelper.dropTable(Stranger.class);
        VariousDbTestHelper.dropTable(Son.class);
        VariousDbTestHelper.dropTable(Family.class);
        VariousDbTestHelper.dropTable(Daughter.class);
        VariousDbTestHelper.dropTable(Father.class);
        VariousDbTestHelper.dropTable(Granpa.class);

        VariousDbTestHelper.dropTable(StrangerSsdMaster.class);
        VariousDbTestHelper.dropTable(FamilySsdMaster.class);
        VariousDbTestHelper.dropTable(SonSsdMaster.class);
        VariousDbTestHelper.dropTable(DaughterSsdMaster.class);
        VariousDbTestHelper.dropTable(FatherSsdMaster.class);
        VariousDbTestHelper.dropTable(GranpaSsdMaster.class);
    }

    /** {@link nablarch.test.core.db.MasterDataSetUpper#setUpMasterData()} のテスト */
    @Test
    public void testSetUpMasterData() {

        List<File> files = new ArrayList<File>();
        files.add(new File("src/test/java/MASTER_DATA.xls"));
        files.add(new File("src/test/java/MASTER_DATA2.xls"));
        MasterDataSetUpper target = new MasterDataSetUpper(files, BACKUP_SCHEMA);

        // テーブルレコード削除
        VariousDbTestHelper.delete(TestTable.class);
        VariousDbTestHelper.delete(TestTableSsdMaster.class);

        assertEquals(0, VariousDbTestHelper.findAll(TestTable.class)
                .size());
        assertEquals(0, VariousDbTestHelper.findAll(TestTableSsdMaster.class)
                .size());

        // マスタデータ投入
        target.setUpMasterData();

        // 結果確認
        List<TestTable> defaultResult = VariousDbTestHelper.findAll(TestTable.class);
        List<TestTableSsdMaster> backupResult = VariousDbTestHelper.findAll(TestTableSsdMaster.class);

        assertEquals(4, defaultResult.size());
        assertEquals(4, backupResult.size());

        assertEquals("まみむめも", defaultResult.get(0).varchar2Col);
        assertEquals("やゆよ", defaultResult.get(1).varchar2Col);
        assertEquals("あいうえお", defaultResult.get(2).varchar2Col);
        assertEquals("かきくけこ", defaultResult.get(3).varchar2Col);

        for (int i = 0; i < defaultResult.size(); i++) {
            assertEquals(defaultResult.get(i).pkCol1, backupResult.get(i).pkCol1);
            assertEquals(defaultResult.get(i).pkCol2, backupResult.get(i).pkCol2);
            assertEquals(defaultResult.get(i).varchar2Col, backupResult.get(i).varchar2Col);
            assertEquals(defaultResult.get(i).numberCol, backupResult.get(i).numberCol);
            assertEquals(defaultResult.get(i).numberCol2, backupResult.get(i).numberCol2);
        }
    }

    /** {@link MasterDataSetUpper#main(String...)}のテスト(最大引数) */
    @Test
    public void testMain() {

        // テーブルレコード削除
        VariousDbTestHelper.delete(TestTable.class);
        assertEquals(0, VariousDbTestHelper.findAll(TestTable.class)
                .size());

        // マスタデータ投入
        MasterDataSetUpper.main("unit-test.xml", "src/test/java/MASTER_DATA.xls", "src/test/java/MASTER_DATA2.xls",
                "--backUpSchema:" + BACKUP_SCHEMA);

        // 結果確認
        List<TestTable> defaultResult = VariousDbTestHelper.findAll(TestTable.class);
        assertEquals(4, defaultResult.size());

        assertEquals("まみむめも", defaultResult.get(0).varchar2Col);
        assertEquals("やゆよ", defaultResult.get(1).varchar2Col);
        assertEquals("あいうえお", defaultResult.get(2).varchar2Col);
        assertEquals("かきくけこ", defaultResult.get(3).varchar2Col);
    }

    /** {@link MasterDataSetUpper#main(String...)}のテスト(引数不足) */
    @Test(expected = IllegalArgumentException.class)
    public void testMainWithoutArgs() {
        MasterDataSetUpper.main("too few args");
    }

    /** {@link MasterDataSetUpper#main(String...)}のテスト(引数過多) */
    @Test(expected = IllegalArgumentException.class)
    public void testMainTooManyArgs() {
        MasterDataSetUpper.main("too", "many", "ar", "gs");
    }

    @Test
    public void testSetUpInOrder() throws SQLException {
        VariousDbTestHelper.createTable(Granpa.class);
        VariousDbTestHelper.createTable(Father.class);
        VariousDbTestHelper.createTable(Daughter.class);
        VariousDbTestHelper.createTable(Son.class);
        VariousDbTestHelper.createTable(Stranger.class);
        // CREATE TABLE直後
        doSetUp();
        // テーブルにレコードがある状態で実行（削除ができること）
        doSetUp();
    }

    @Test
    public void testSetUpInOrderWithBackUp() throws SQLException {
        VariousDbTestHelper.createTable(Granpa.class);
        VariousDbTestHelper.createTable(Father.class);
        VariousDbTestHelper.createTable(Daughter.class);
        VariousDbTestHelper.createTable(Son.class);
        VariousDbTestHelper.createTable(Stranger.class);
        VariousDbTestHelper.createTable(GranpaSsdMaster.class);
        VariousDbTestHelper.createTable(FatherSsdMaster.class);
        VariousDbTestHelper.createTable(DaughterSsdMaster.class);
        VariousDbTestHelper.createTable(SonSsdMaster.class);
        VariousDbTestHelper.createTable(StrangerSsdMaster.class);
        // CREATE TABLE直後
        doSetUpWithBackUp();
        // テーブルにレコードがある状態で実行（削除ができること）
        doSetUpWithBackUp();
    }

    private void doSetUp() {
        final String prefix = "src/test/resources/nablarch/test/core/db/masterdata/";
        MasterDataSetUpper.main("unit-test.xml",
                prefix + "MASTER_DATA.xls",
                prefix + "MASTER_DATA2.xls");
    }

    private void doSetUpWithBackUp() {
        final String prefix = "src/test/resources/nablarch/test/core/db/masterdata/";
        MasterDataSetUpper.main("unit-test.xml",
                prefix + "MASTER_DATA.xls",
                prefix + "MASTER_DATA2.xls",
                "--backUpSchema:" + BACKUP_SCHEMA);
    }


}
