package nablarch.test.core.db;

import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.statement.exception.SqlStatementException;
import nablarch.core.db.transaction.SimpleDbTransactionManager;
import nablarch.test.support.SystemRepositoryResource;

import org.junit.Rule;
import org.junit.Test;

public class TransactionTemplateTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");
    
    @Test(expected=IllegalArgumentException.class)
    public void testTransactionTemplateString() {
        new TransactionTemplate("hoge") {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
            }
        };
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testTransactionTemplateInvalidString() {
        new TransactionTemplate("") {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
            }
        };
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testTransactionTemplateManager() {
        new TransactionTemplate((SimpleDbTransactionManager) null) {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
            }
        };
    }

    @Test(expected=IllegalArgumentException.class)
    public void testTransactionTemplateSimpleDbTransactionManager() {
        new TransactionTemplate((SimpleDbTransactionManager) null) {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
            }
        };
    }

    @Test(expected=SqlStatementException.class)
    public void testExecuteFail() {
        new TransactionTemplate(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(AppDbConnection conn) {
                throw new SqlStatementException("異常系テスト用に例外を発生させます。", null);
            }
        }.execute();
    }

}
