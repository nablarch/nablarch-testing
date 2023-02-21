package nablarch.test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import nablarch.core.db.connection.ConnectionFactory;
import nablarch.core.db.connection.TransactionManagerConnection;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlRow;
import nablarch.core.transaction.TransactionContext;
import nablarch.fw.DataReader.NoMoreRecord;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.Result;
import nablarch.fw.handler.DataReadHandler;
import nablarch.fw.reader.DatabaseRecordReader;
import nablarch.fw.reader.DatabaseTableQueueReader;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link nablarch.test.OneShotLoopHandler}のテストクラス。
 */
@RunWith(DatabaseTestRunner.class)
public class OneShotLoopHandlerTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("db-default.xml");

    /** テスト用接続 */
    private TransactionManagerConnection connection;

    @BeforeClass
    public static void setUpClass() throws Exception {
        VariousDbTestHelper.createTable(InputTable.class);
    }

    @Before
    public void setUp() throws Exception {
        VariousDbTestHelper.delete(InputTable.class);
        connection = repositoryResource.getComponentByType(ConnectionFactory.class)
                                       .getConnection(TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY);
    }

    @After
    public void tearDown() throws Exception {
        try {
            connection.rollback();
        } finally {
            connection.terminate();
        }
    }

    /**
     * データがある場合のテスト
     */
    @Test
    public void testDataFound() throws Exception {
        createTestData(5);
        DatabaseTableQueueReader queueReader = createDataReader();
        ExecutionContext context = new ExecutionContext();
        context.setDataReader(queueReader);

        final List<Integer> readData = new ArrayList<Integer>();
        context.addHandler(new OneShotLoopHandler());
        context.addHandler(new DataReadHandler());
        context.addHandler(new Handler<SqlRow, Result>() {
            @Override
            public Result handle(SqlRow record, ExecutionContext context) {
                int id = record.getBigDecimal("id").intValue();
                VariousDbTestHelper.update(new InputTable(Long.valueOf(id), "1"));
                readData.add(id);
                return new Result.Success();
            }
        });
        context.handleNext("input");

        assertThat(readData.size(), is(5));
        assertThat(readData, hasItems(1, 2, 3, 4, 5));
    }

    /**
     * データが存在しない場合のテスト
     */
    @Test
    public void testDataNotFound() throws Exception {

        DatabaseTableQueueReader queueReader = createDataReader();
        ExecutionContext context = new ExecutionContext();
        context.setDataReader(queueReader);

        final List<Integer> readData = new ArrayList<Integer>();
        context.addHandler(new OneShotLoopHandler());
        context.addHandler(new DataReadHandler());
        context.addHandler(new Handler<SqlRow, Result>() {
            @Override
            public Result handle(SqlRow record, ExecutionContext context) {
                int id = record.getBigDecimal("id").intValue();
                readData.add(id);
                return new Result.Success();
            }
        });
        context.handleNext("input");

        assertThat("データが存在しないので処理済みデータは無し", readData.size(), is(0));
    }

    /**
     * データリーダが{@link nablarch.fw.reader.DatabaseTableQueueReader}以外のテスト。
     * <p/>
     * {@link nablarch.fw.reader.DatabaseTableQueueReader}以外のリーダの場合には、
     * そのリーダを使用してデータの読み込みができること。
     */
    @Test
    public void testNotDatabaseTableQueueReader() throws Exception {
        createTestData(3);
        DatabaseTableQueueReader queueReader = createDataReader();
        ExecutionContext context = new ExecutionContext();

        // DatabaseTableQueueReaderではなくオリジナルのリーダ(DatabaseRecordReader)を使用する。
        context.setDataReader(queueReader.getOriginalReader());

        final List<Integer> readData = new ArrayList<Integer>();
        context.addHandler(new OneShotLoopHandler());
        context.addHandler(new DataReadHandler());
        context.addHandler(new Handler<SqlRow, Result>() {
            @Override
            public Result handle(SqlRow record, ExecutionContext context) {
                int id = record.getBigDecimal("id").intValue();
                VariousDbTestHelper.update(new InputTable(Long.valueOf(id), "1"));
                readData.add(id);
                return new Result.Success();
            }
        });
        context.handleNext("input");

        assertThat(readData.size(), is(3));
        assertThat(readData, hasItems(1, 2, 3));
    }

    /**
     * 後続のハンドラが{@link NoMoreRecord}を返した場合、処理が終了すること。
     * @throws Exception
     */
    @Test
    public void testNoMoreRecord() throws Exception {
        createTestData(3);
        DatabaseTableQueueReader queueReader = createDataReader();
        ExecutionContext context = new ExecutionContext();

        context.setDataReader(queueReader);

        final List<Integer> readData = new ArrayList<Integer>();
        context.addHandler(new OneShotLoopHandler());
        context.addHandler(new DataReadHandler());
        context.addHandler(new Handler<SqlRow, Result>() {
            @Override
            public Result handle(SqlRow record, ExecutionContext context) {
                int id = record.getBigDecimal("id").intValue();
                readData.add(id);
                return new NoMoreRecord();
            }
        });
        context.handleNext("input");

        assertThat("最初のレコード処理後にNoMoreRecordがかえされるので処理したレコード数は1", readData.size(), is(1));
        assertThat(readData, hasItems(1));
    }

    /**
     * ハンドラでステータスが更新されない場合でも処理が終了することを確認するテスト。
     * <p/>
     * アプリケーション側の不具合でステータスが更新されない場合でも、
     * 初回セットアップデータを一度処理したらバッチ実行が終了することを確認する。
     */
    @Test
    public void testNotUpdateStatus() {

        createTestData(5);

        DatabaseTableQueueReader queueReader = createDataReader();
        ExecutionContext context = new ExecutionContext();
        context.setDataReader(queueReader);

        final List<Integer> readData = new ArrayList<Integer>();
        context.addHandler(new OneShotLoopHandler());
        context.addHandler(new DataReadHandler());
        context.addHandler(new Handler<SqlRow, Result>() {
            @Override
            public Result handle(SqlRow record, ExecutionContext context) {
                int id = record.getBigDecimal("id").intValue();
                readData.add(id);
                return new Result.Success();
            }
        });
        context.handleNext("input");

        assertThat("データが存在しないので処理済みデータは無し", readData, hasItems(1, 2, 3, 4, 5));
    }

    @Test
    public void 後続のハンドラにはこのハンドラのインプットとなるExecutionContextのコピーが渡されること() {

        final TestExecutionContext originalContext = new TestExecutionContext();
        createTestData(1);
        originalContext.setDataReader(createDataReader());

        originalContext.addHandler(new DataReadHandler());
        originalContext.addHandler(new Handler<Object, Object>() {
            @Override
            public Object handle(final Object o, final ExecutionContext context) {
                assertThat("OneShotLoopHandlerに指定したExecutionContextと同じクラスが後続のハンドラに渡されること",
                        context, instanceOf(originalContext.getClass()));
                return null;
            }
        });

        final OneShotLoopHandler sut = new OneShotLoopHandler();
        sut.handle(null, originalContext);
    }
    
    /**
     * テストで使用するデータリーダを生成する。
     *
     * @return 生成したデータリーダ
     */
    private DatabaseTableQueueReader createDataReader() {
        SqlPStatement statement = connection.prepareStatement(
                "SELECT * FROM INPUT_TABLE WHERE STATUS = '0' ORDER BY ID");
        DatabaseRecordReader reader = new DatabaseRecordReader();
        reader.setStatement(statement);
        return new DatabaseTableQueueReader(reader, 1000, "id");
    }

    /**
     * テスト用のデータを作成する。
     *
     * @param count 作成する件数
     */
    private void createTestData(int count) {
        InputTable[] entity = new InputTable[count];
        for (int i = 0; i < count; i++) {
            entity[i] = new InputTable(Long.valueOf(i + 1), "0");
        }
        VariousDbTestHelper.insert((Object[]) entity);
    }

    @Entity
    @Table(name = "INPUT_TABLE")
    public static class InputTable {

        public InputTable() {
        };

        public InputTable(Long id, String status) {
            this.id = id;
            this.status = status;
        }

        @Id
        @Column(name = "ID", length = 10, nullable = false)
        public Long id;

        @Column(name = "STATUS", length = 1, nullable = false)
        public String status;
    }

    /**
     * テスト用の{@link ExecutionContext}実装。
     */
    private static class TestExecutionContext extends ExecutionContext {

        public TestExecutionContext() {
        }

        public TestExecutionContext(final TestExecutionContext original) {
            super(original);
        }

        @Override
        protected ExecutionContext copyInternal() {
            return new TestExecutionContext(this);
        }
    }
}

