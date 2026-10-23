package nablarch.test.core.db;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nablarch.test.RepositoryInitializer;
import nablarch.test.core.reader.TestDataParser;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

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

    /**
     * 非Excel形式（YAML形式相当）のマスタデータファイルを指定した場合、
     * そのファイル内の複数テーブルのデータが投入されること。
     */
    @Test
    public void testSetUpMasterDataFromNonExcelFile() throws SQLException {
        VariousDbTestHelper.createTable(Granpa.class);
        VariousDbTestHelper.createTable(Stranger.class);

        try {
            MasterDataSetUpper.main(
                    "nablarch/test/core/db/masterdata/masterdata-tsv-component-configuration.xml",
                    "src/test/resources/nablarch/test/core/db/masterdata/MASTER_DATA_NON_EXCEL.txt");

            // 1ファイルに含まれる2テーブル分が投入されること
            assertEquals(3, VariousDbTestHelper.findAll(Granpa.class).size());
            assertEquals(2, VariousDbTestHelper.findAll(Stranger.class).size());
        } finally {
            // MasterDataSetUpper.main がリポジトリを差し替えるため、既定の設定に戻す。
            // 戻さないと、同一クラスの後続テストがタブ区切りテキスト用のリーダでExcelを読みに行き失敗する。
            RepositoryInitializer.recreateRepository("unit-test.xml");
        }
    }

    /**
     * 拡張子を持たないマスタデータファイルをExcel形式と誤判定せず、
     * ファイル全体を1リソースとして{@link TestDataParser}へ問い合わせること。
     * <p>
     * ファイル名を{@code xls}にしているのは、{@code MasterDataSetUpper#isExcelFile}の
     * 早期return（拡張子が無ければExcel形式とみなさない）が効くかどうかを判別できる
     * 唯一の入力だからである。早期returnが無いと、区切りの{@code .}が無いファイル名は
     * そのまま拡張子として扱われ、{@code xls}はExcel形式と判定されてしまう。
     * </p>
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testGetAllTableDataFromFileWithNoExtension() throws Exception {
        // Given: 拡張子を持たず、名前がそのまま「xls」であるマスタデータファイル
        File masterDataFile = tmpDir.newFile("xls");
        String dir = masterDataFile.getAbsoluteFile().getParent();

        List<TableData> expected = Collections.singletonList(new TableData());
        TestDataParser parser = mock(TestDataParser.class);
        when(parser.getSetupTableData(dir, "xls")).thenReturn(expected);
        repositoryResource.addComponent("testDataParser", parser);

        try {
            // When
            MasterDataSetUpper target =
                    new MasterDataSetUpper(Arrays.asList(masterDataFile), null);
            List<TableData> actual = target.getAllTableData(masterDataFile);

            // Then: シート単位ではなく、ファイル全体を1リソースとして1回だけ問い合わせている
            verify(parser).getSetupTableData(dir, "xls");
            assertThat(actual, is(expected));
        } finally {
            // @ClassRule のリポジトリへ差し込んだため、既定の設定に戻す。
            RepositoryInitializer.recreateRepository("unit-test.xml");
        }
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
