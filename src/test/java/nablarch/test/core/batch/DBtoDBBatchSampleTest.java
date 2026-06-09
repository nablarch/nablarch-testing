package nablarch.test.core.batch;


import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.sql.SQLException;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import nablarch.test.core.db.HogeTable;
import nablarch.test.core.db.HogeTableSsdMaster;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** {@link DBtoDBBatchSample}のテストクラス。 */
@RunWith(DatabaseTestRunner.class)
public class DBtoDBBatchSampleTest extends BatchRequestTestSupport {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    /**
     * テスト用のテーブルを作成する。
     *
     * @throws SQLException 予期しない例外
     */
    @BeforeClass
    public static void createTable() throws SQLException {
        VariousDbTestHelper.createTable(BatchSample.class);
        VariousDbTestHelper.createTable(HogeTable.class);
        VariousDbTestHelper.createTable(HogeTableSsdMaster.class);
    }

    /** テストを実行する。 */
    @Test
    public void testExecute() {
        execute("testExecute");
        // マスタデータにテストショット数分、レコードが登録されていること。
        assertThat(VariousDbTestHelper.findAll(HogeTable.class)
                .size(), is(2));
    }

    @AfterClass
    public static void afterClass() throws SQLException {
        // マスタデータ復旧機能によりテーブルがクリアされていること
        assertThat(VariousDbTestHelper.findAll(HogeTable.class)
                .size(), is(0));
    }


    @Entity
    @Table(name = "BATCH_SAMPLE")
    public static class BatchSample {

        public BatchSample() {
        }

        public BatchSample(String id, Long counter, String message,
                Timestamp updateDate) {
            this.id = id;
            this.counter = counter;
            this.message = message;
            this.updateDate = updateDate;
        }

        @Id
        @Column(name = "ID", length = 5, nullable = false)
        public String id;

        @Column(name = "COUNTER", length = 5)
        public Long counter;

        @Column(name = "MESSAGE", length = 50)
        public String message;

        @Column(name = "UPDATE_DATE")
        public Timestamp updateDate;
    }
}
