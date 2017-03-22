package nablarch.test.core.db;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

import org.junit.Test;

/**
 * {@link EntityDependencyParser}のテストクラス。
 *
 * @author T.Kawasaki
 */
public class EntityDependencyParserTest {

    private final EntityDependencyParser parser = new EntityDependencyParser();

    /** 循環参照のテスト。*/
    @Test
    public void testCircularReference() {
        parser.associate("A", "B");
        parser.associate("B", "C");
        parser.associate("C", "A");
        try {
            parser.getTableList();
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("ルートとなるテーブルが見つかりません。循環参照になっていると思います！"));
        }
    }

    /** 自己参照のテスト。 */
    @Test
    public void testSelfReference() {
        parser.associate("C", "C");
        parser.associate("A", "B");

        List<String> tableList = parser.getTableList();

        assertThat(tableList.get(0), anyOf(is("A"), is("C")));
        assertThat(tableList.get(1), anyOf(is("A"), is("C")));
        assertThat(tableList.get(2), is("B"));

    }

    @Test(expected = RuntimeException.class)
    public void testParseFail() {
        parser.parse(new MockConnection() {
            @Override
            public DatabaseMetaData getMetaData() throws SQLException {
                throw new SQLException("for test.");
            }
        }, "");
    }
}
