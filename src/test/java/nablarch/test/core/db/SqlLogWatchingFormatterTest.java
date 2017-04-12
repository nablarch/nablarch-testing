package nablarch.test.core.db;

import nablarch.test.core.db.MasterDataRestorer.SqlLogWatchingFormatter;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/** {@link nablarch.test.core.db.MasterDataRestorer.SqlLogWatchingFormatter}のテスト */
public class SqlLogWatchingFormatterTest {

    /** テスト対象 */
    private MasterDataRestorer.SqlLogWatchingFormatter target = new MasterDataRestorer.SqlLogWatchingFormatter();

    @Before
    public void clearStatement() {
        SqlLogWatchingFormatter.begin();
    }

    /** 終了処理 */
    @AfterClass
    public static void terminate() {
        SqlLogWatchingFormatter.begin();
    }



    /**
     * {@link nablarch.test.core.db.MasterDataRestorer.SqlLogWatchingFormatter#startExecuteUpdate(String, String, String)}のテスト
     */
    @Test
    public void testStartExecuteUpdate() {
        // 発行したSQL文が取得できること。
        target.startExecuteUpdate("testStartExecuteUpdate", "UPDATE hoge", "");
        target.startExecuteUpdate("testStartExecuteUpdate", "UPDATE fuga", "");
        assertThat(SqlLogWatchingFormatter.getExecuted(),
                is(Arrays.asList("UPDATE hoge", "UPDATE fuga")));
    }


    /**
     * {@link nablarch.test.core.db.MasterDataRestorer.SqlLogWatchingFormatter#startExecute(String, String, String)}のテスト
     */
    @Test
    public void testStartExecute() {
        // 発行したSQL文が取得できること。
        target.startExecute("testStartExecuteUpdate", "UPDATE hoge", "");
        target.startExecute("testStartExecuteUpdate", "UPDATE fuga", "");
        assertThat(SqlLogWatchingFormatter.getExecuted(),
                is(Arrays.asList("UPDATE hoge", "UPDATE fuga")));
    }

    /**
     * {@link nablarch.test.core.db.MasterDataRestorer.SqlLogWatchingFormatter#startExecuteBatch(String, String, String)}のテスト
     */
    @Test
    public void testStartExecuteBatch() {
        // 発行したSQL文が取得できること。
        target.startExecuteBatch("testStartExecuteUpdate", "UPDATE hoge", "");
        target.startExecuteBatch("testStartExecuteUpdate", "UPDATE fuga", "");
        assertThat(SqlLogWatchingFormatter.getExecuted(),
                is(Arrays.asList("UPDATE hoge", "UPDATE fuga")));

    }
}
