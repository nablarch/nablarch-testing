package nablarch.test.core.batch;


import nablarch.test.support.SystemRepositoryResource;

import org.junit.Rule;
import org.junit.Test;

/** {@link SimpleBatchSample}のテストクラス。 */
public class SimpleBatchSampleTest extends BatchRequestTestSupport {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");


    /** テストを実行する。 */
    @Test
    public void testExecute() {
        execute("testExecute");
    }

}
