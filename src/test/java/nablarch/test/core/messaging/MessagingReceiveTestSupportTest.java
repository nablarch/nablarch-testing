package nablarch.test.core.messaging;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link MessagingReceiveTestSupport}のテストクラス。
 *
 * @author hisaaki sioiri
 */
@RunWith(DatabaseTestRunner.class)
public class MessagingReceiveTestSupportTest extends MessagingReceiveTestSupport {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    @BeforeClass
    public static void setupClass() {
        VariousDbTestHelper.createTable(IdGenerate.class);
        VariousDbTestHelper.createTable(ReceiveTest.class);
        VariousDbTestHelper.setUpTable(new IdGenerate("00", 0L));
    }

    @Test
    public void testExtends() {
        execute();
    }

    @Test
    public void testUnExtends() {
        MessagingReceiveTestSupport support = new MessagingReceiveTestSupport(
                getClass());
        support.execute("testUnExtends");
    }

    @Entity
    @Table(name = "ID_GENERATE")
    public static class IdGenerate {

        public IdGenerate() {
        }

        ;

        public IdGenerate(String id, Long no) {
            this.id = id;
            this.no = no;
        }

        @Id
        @Column(name = "ID", length = 2, nullable = false)
        public String id;

        @Column(name = "NO", length = 10, nullable = false)
        public Long no;
    }

    @Entity
    @Table(name = "RECEIVE_TEST")
    public static class ReceiveTest {

        public ReceiveTest() {
        }

        ;

        public ReceiveTest(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Id
        @Column(name = "ID", length = 10, nullable = false)
        public String id;

        @Column(name = "NAME", length = 100)
        public String name;
    }
}
