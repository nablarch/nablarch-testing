package nablarch.test.core.batch;


import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import nablarch.test.support.SystemRepositoryResource;

import org.junit.Rule;
import org.junit.Test;

/**
 * @author T.Kawasaki
 */
public class BatchRequestTestSupportTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    private BatchRequestTestSupport target = new BatchRequestTestSupport(getClass());


    /** 引数がnullまたは空文字のときに例外が発生すること。 */
    @Test
    public void testExecuteNull() {
        try {
            target.execute(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("sheetName must not be null or empty."));
        }
    }

    /** テストケース（LIST_MAP=testCases）が見つからない場合、例外が発生すること。 */
    @Test
    public void testTestCasesNotFound() {
        try {
            target.execute("testTestCasesNotFound");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("no test shot found."));
        }
    }

    /** テストケース（LIST_MAP=testCases）が見つからない場合、例外が発生すること。 */
    @Test
    public void testExpectedLogNotFound() {
        try {
            target.execute("testExpectedLogNotFound");
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("expected log data must be set. expected log id = [notFound]"));
        }
    }

    /** 終了ステータスが期待値と異なる場合、例外が発生すること。 */
    @Test
    public void testCompareStatus() throws Exception {
        try {
            target.execute("testCompareStatus");
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("expected status code is [100].but was [0]"));
        }
    }
}
