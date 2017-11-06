package nablarch.test.core.batch;

import nablarch.test.core.db.HogeTable;
import nablarch.test.core.db.HogeTableSsdMaster;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;
import nablarch.test.support.tool.Hereis;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * @author T.Kawasaki
 */
@RunWith(DatabaseTestRunner.class)
public class FileToFileBatchSampleTest extends BatchRequestTestSupport {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    @BeforeClass
    public static void setUp() {
        VariousDbTestHelper.createTable(HogeTable.class);
        VariousDbTestHelper.createTable(HogeTableSsdMaster.class);
        
        // データフォーマット定義ファイル
        File formatFile = Hereis.file("./work/layout.txt");
        /**********************************************
         # 文字列型フィールドの文字エンコーディング
         text-encoding: "sjis"
         file-type:     "Fixed"

         # 各レコードの長さ
         record-length: 20

         # データレコード定義
         [Default]
         1    id             X(5)
         6    counter        Z(5)
         11   message        X(10)
         ***************************************************/
        formatFile.deleteOnExit();

    }

    @Test
    public void testHandle() {
        execute();
    }
}
