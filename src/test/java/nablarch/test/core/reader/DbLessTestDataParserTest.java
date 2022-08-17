package nablarch.test.core.reader;

import nablarch.test.core.db.TableData;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * {@link DbLessTestDataParser}のテストクラス。<br>
 *
 * @author Hisaaki Sioiri
 */
@RunWith(DatabaseTestRunner.class)
public class DbLessTestDataParserTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-dbless.xml");

    private static final String resourceRoot = "src/test/java/";

    private TestDataParser target;
    
    @Before
    public void before() {
        target = repositoryResource.getComponent("testDataParser");
    }

    /**
     * 期待するテーブルデータの取得がサポートされていないことを確認する。
     */
    @SuppressWarnings("unchecked")
    @Test(expected = UnsupportedOperationException.class)
    public void testExpectedGetTableData() {
        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/withoutGroupId";
        
        target.getExpectedTableData(dir, resource);
    }

    /**
     * テーブルデータの初期化がサポートされていないことを確認する。
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetSetupTableData() {
        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/withoutGroupId";

        List<TableData> actual = target.getSetupTableData(dir, resource);

        assertThat(actual.size(), is(0));
    }

    /**
     * List-Map形式のデータ取得が{@link BasicTestDataParser}と同じく処理されることを確認する
     */
    @Test
    public void testGetListMap() {
        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/getListMap";
        List<Map<String, String>> actual = target.getListMap(dir, resource, "params");
        assertNotNull(actual);
        assertThat(actual.size(), is(2));
    }
}
