package nablarch.test.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

import nablarch.test.core.db.DbAccessTestSupport;
import nablarch.test.core.db.TestTable;
import nablarch.test.core.file.FileSupport;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 複数のデータリソースに対してデータをセットアップするテスト。
 *
 * @author hisaaki sioiri
 */
@RunWith(DatabaseTestRunner.class)
public class MultiResourceDataSetUpTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    /** データベースセットアップ */
    private DbAccessTestSupport dbSupport = new DbAccessTestSupport(getClass());

    /** ファイルセットアップ */
    private FileSupport fileSupport = new FileSupport(getClass());

    @BeforeClass
    public static void createTable() {
        VariousDbTestHelper.createTable(TestTable.class);
        final File work = new File("./work");
        if (!work.exists()) {
            work.mkdir();
        }
    }

    @After
    public void tearDown() throws Throwable {
        dbSupport.endTransactions();
    }

    /**
     * ファイル->データベースの順にデータをセットアップする。
     * <p/>
     * Excelに記載された順にデータがセットアップされることを確認する。
     */
    @Test
    public void testFileAndDatabaseSetUp() {

        String sheetName = "testFileAndDatabaseSetUp";
        // setup file
        fileSupport.setUpFile(sheetName);
        // setup database
        dbSupport.setUpDb(sheetName);

        // データベースを操作
        VariousDbTestHelper.setUpTable(new TestTable("00001", 11L, "あいうえお", 100L, new BigDecimal(100), new Date(
                System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()), null, null, toByteArray(
                "./work/テスト1.csv"), true));
        dbSupport.assertTableEquals(sheetName);
    }

    private byte[] toByteArray(String filePath) {
        byte[] b = new byte[1];
        try {
            FileInputStream fis = new FileInputStream(new File(filePath));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (fis.read(b) > 0) {
                baos.write(b);
            }
            baos.close();
            fis.close();
            b = baos.toByteArray();
        } catch (Exception e) {
            // NOP
        }
        return b;
    }
}

