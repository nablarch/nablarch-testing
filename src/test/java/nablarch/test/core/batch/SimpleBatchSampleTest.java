package nablarch.test.core.batch;


import nablarch.test.core.db.HogeTable;
import nablarch.test.core.db.HogeTableSsdMaster;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** {@link SimpleBatchSample}のテストクラス。 */
@RunWith(DatabaseTestRunner.class)
public class SimpleBatchSampleTest extends BatchRequestTestSupport {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    @BeforeClass
    public static void setUpClass() throws Exception {
        VariousDbTestHelper.createTable(HogeTable.class);
        VariousDbTestHelper.createTable(HogeTableSsdMaster.class);
    }

    /** テストを実行する。 */
    @Test
    public void testExecute() {
        execute("testExecute");
    }

}
