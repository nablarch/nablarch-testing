package nablarch.test.core.db;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import nablarch.core.db.connection.AppDbConnection;
import nablarch.test.event.TestEventDispatcher;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import nablarch.test.support.log.app.OnMemoryLogWriter;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link MasterDataRestorer}のテスト
 *
 * @author T.Kawasaki
 */
@RunWith(DatabaseTestRunner.class)
public class MasterDataRestorerTest extends TestEventDispatcher {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    /** 初期化処理 */
    @BeforeClass
    public static void initialize() {

        VariousDbTestHelper.createTable(HogeTable.class);
        VariousDbTestHelper.createTable(HogeTableSsdMaster.class);

        VariousDbTestHelper.setUpTable(
                new HogeTable("00001", 1L, "あ", 12345L, new BigDecimal("1234.123"), new Date(0L), new Timestamp(0L), null,
                        null, null),
                new HogeTable("00002", 2L, "い", 2L, new BigDecimal("22.123"), new Date(0L), new Timestamp(0L), "12345",
                        null, null));

        VariousDbTestHelper.setUpTable(
                new HogeTableSsdMaster("00001", 1L, "あ", 12345L, new BigDecimal("1234.123"), new Date(0L), new Timestamp(
                        0L), null, null, null),
                new HogeTableSsdMaster("00002", 2L, "い", 2L, new BigDecimal("22.123"), new Date(0L), new Timestamp(0L),
                        "12345", null, null));

        VariousDbTestHelper.createTable(Granpa.class);
        VariousDbTestHelper.createTable(GranpaSsdMaster.class);
        VariousDbTestHelper.createTable(Father.class);
        VariousDbTestHelper.createTable(FatherSsdMaster.class);
        VariousDbTestHelper.createTable(Daughter.class);
        VariousDbTestHelper.createTable(DaughterSsdMaster.class);
        VariousDbTestHelper.createTable(Family.class);
        VariousDbTestHelper.createTable(FamilySsdMaster.class);
        VariousDbTestHelper.createTable(Son.class);
        VariousDbTestHelper.createTable(SonSsdMaster.class);

        Granpa granpa1 = new Granpa("1");
        Granpa granpa2 = new Granpa("2");
        VariousDbTestHelper.setUpTable(granpa1, granpa2);

        GranpaSsdMaster granpa1b = new GranpaSsdMaster("1");
        GranpaSsdMaster granpa2b = new GranpaSsdMaster("2");
        VariousDbTestHelper.setUpTable(granpa1b, granpa2b);

        Father father1 = new Father("3", granpa1);
        Father father2 = new Father("4", granpa2);
        VariousDbTestHelper.setUpTable(father1, father2);

        FatherSsdMaster father1b = new FatherSsdMaster("3", granpa1b);
        FatherSsdMaster father2b = new FatherSsdMaster("4", granpa2b);
        VariousDbTestHelper.setUpTable(father1b, father2b);

        Daughter daughter1 = new Daughter("5", father1);
        Daughter daughter2 = new Daughter("6", father2);
        VariousDbTestHelper.setUpTable(daughter1, daughter2);

        DaughterSsdMaster daughter1b = new DaughterSsdMaster("5", father1b);
        DaughterSsdMaster daughter2b = new DaughterSsdMaster("6", father2b);
        VariousDbTestHelper.setUpTable(daughter1b, daughter2b);

        Family family1 = new Family("7", father1, daughter1);
        Family family2 = new Family("8", father2, daughter2);
        VariousDbTestHelper.setUpTable(family1, family2);

        FamilySsdMaster family1b = new FamilySsdMaster("7", father1b, daughter1b);
        FamilySsdMaster family2b = new FamilySsdMaster("8", father2b, daughter2b);
        VariousDbTestHelper.setUpTable(family1b, family2b);
    }

    /** {@link MasterDataRestorer#afterTestMethod()}のテスト */
    @Test
    public void testAfterTestMethod() {
        MasterDataRestorer target = getFromRepository();
        //        target.beforeTestSuite(); // SQLログに登録

        // 2件データがある
        assertEquals(2, VariousDbTestHelper.findAll(HogeTable.class)
                .size());

        // 全件削除する。
        new TransactionTemplate(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
                conn.prepareStatement("DELETE /* テスト */ FROM HOGE_TABLE")
                        .execute();
            }
        }.execute();

        // データは0件
        assertEquals(0, VariousDbTestHelper.findAll(HogeTable.class)
                .size());

        // テスト対象実行
        target.afterTestMethod();

        OnMemoryLogWriter.clear();
        target.afterTestMethod();

        assertThat(OnMemoryLogWriter.getMessages("writer.memlog").size(), is(0));

        // データが復旧していること
        assertEquals(2, VariousDbTestHelper.findAll(HogeTable.class)
                .size());
    }

    private MasterDataRestorer getFromRepository() {
        return repositoryResource.getComponent(MasterDataRestorer.MASTER_DATA_RESTORER_KEY);
    }

    /** {@link MasterDataRestorer#getUpdatedTables(java.util.List)}のテスト */
    @Test
    public void testGetUpdatedTables() {

        MasterDataRestorer target = new MasterDataRestorer();
        // 監視対象テーブルを設定
        target.setTablesTobeWatched(Arrays.asList("TARGET1_TBL", "TARGET2_TBL", "TARGET3_TBL"));
        // 発行されたSQL文を登録
        List<String> executed = Arrays.asList(
                "UPDATE TARGET1_TBL SET NAME='john' WHERE ID = ?",
                "DELETE TARGET2_TBL",
                "SELECT * FROM TARGET3_TBL",
                "INSERT INTO ANOTHER_TBL SET (ID = ?, NAME = ?)");

        // 更新されたテーブル名が取得されること
        Set<String> actual = target.getUpdatedTables(executed);
        assertEquals(new LinkedHashSet<String>(Arrays.asList("TARGET1_TBL", "TARGET2_TBL")), actual);
    }

    /** {@link MasterDataRestorer#isSuspiciousSql(String, String, String)}のテスト */
    @Test
    public void testIsSuspiciousSql() {
        MasterDataRestorer target = new MasterDataRestorer();
        target.setUpdateSqlKeywords(Arrays.asList("update"));
        target.setTablesTobeWatched(Arrays.asList("TARGET_TBL"));
        // 更新SQL
        assertTrue(target.containsUpdateSqlKeyword(
                "TARGET_TBL",
                "UPDATE TARGET_TBL SET NAME='john' WHERE ID = ?"));

        // テーブルが異なる（更新SQLでない）
        assertFalse(target.containsUpdateSqlKeyword(
                "TARGET_TBL",
                "UPDATE ANOTHER_TBL SET NAME='john' WHERE ID = ?"));

        // キーワードが現れない（デフォルトには含まれるが、設定した更新SQLキーワードには含まれない）
        assertFalse(target.containsUpdateSqlKeyword(
                "TARGET_TBL",
                "MERGE * FROM TARGET_TBL"));
        // 出現順序が逆
        assertFalse(target.containsUpdateSqlKeyword(
                "TARGET_TBL",
                "TARGET_TBL UPDATE"));
    }

    @Test
    public void testAfterTestMethodWithReferenceTables() {
        MasterDataRestorer target = getFromRepository();

        // 2件データがある
        assertEquals(2, VariousDbTestHelper.findAll(Father.class).size());
        assertEquals(2, VariousDbTestHelper.findAll(FatherSsdMaster.class).size());
        assertEquals(2, VariousDbTestHelper.findAll(Daughter.class).size());
        assertEquals(2, VariousDbTestHelper.findAll(DaughterSsdMaster.class).size());
        assertEquals(2, VariousDbTestHelper.findAll(Family.class).size());
        assertEquals(2, VariousDbTestHelper.findAll(FamilySsdMaster.class).size());

        // システムリポジトリを初期化する。
        repositoryResource.addComponent(MasterDataRestorer.MASTER_DATA_RESTORER_KEY, null);

        assertThat(getFromRepository(), is(nullValue()));

        // 全件削除する。
        new TransactionTemplate(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
                conn.prepareStatement("DELETE FROM FAMILY")
                        .execute();
                conn.prepareStatement("DELETE FROM DAUGHTER")
                        .execute();
                conn.prepareStatement("DELETE FROM FATHER")
                        .execute();
            }
        }.execute();

        // データは0件
        assertEquals(0, VariousDbTestHelper.findAll(Father.class)
                .size());
        assertEquals(0, VariousDbTestHelper.findAll(Daughter.class)
                .size());
        assertEquals(0, VariousDbTestHelper.findAll(Family.class)
                .size());

        // システムリポジトリを再初期化する。
        repositoryResource.addComponent(MasterDataRestorer.MASTER_DATA_RESTORER_KEY, target);

        // テスト対象実行 正しい順序で復元されること
        target = getFromRepository();
        assertThat(target, is(notNullValue()));
        target.afterTestMethod();

        // データが復旧していること
        assertEquals(2, VariousDbTestHelper.findAll(Father.class)
                .size());
        assertEquals(2, VariousDbTestHelper.findAll(Daughter.class)
                .size());
        assertEquals(2, VariousDbTestHelper.findAll(Family.class)
                .size());
    }
}
