package nablarch.test.core.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import nablarch.test.core.batch.BatchRequestTestSupport;
import nablarch.test.support.SystemRepositoryResource;

import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.Test;

/**
 * バッチ処理でメッセージ同期送信を行うテスト。
 * 
 * @author Masato Inoue
 */
public class RequestTestingSendSyncBatchTest extends BatchRequestTestSupport {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    /**
     * バッチ処理でメッセージ同期送信を行うテスト。正常系。
     */
    @Test
    public void testNormalEnd() {
        execute();
    }

    /**
     * 異常系。要求電文（ボディ部）のアサートエラーが発生する。
     */
    @Test
    public void testAbnormalEnd1() {
        try {
            execute();
            
            fail();
        } catch (ComparisonFailure e) {
            assertTrue(e.getExpected().contains("title=lionERROR"));
            assertTrue(e.getActual().contains("title=lion3"));
        }
    }

    
    /**
     * 異常系。要求電文（ヘッダ部）のアサートエラーが発生する。
     */
    @Test
    public void testAbnormalEnd2() {
        try {
            execute();
            fail();
        } catch (ComparisonFailure e) {
            assertTrue(e.getExpected().contains("requestId=ERROR"));
            assertTrue(e.getActual().contains("requestId=RM21AA0101"));
        }
    }

    /**
     * 異常系。行が多い。
     */
    @Test
    public void testAbnormalEnd3() {
        try {
            execute();
            fail();
        } catch (AssertionError e) {
            assertEquals(
                    "number of send message was invalid. expected number=[3], but actual number=[2]. case no=[1], message id=[case6], request id=[RM21AA0101] test class=[nablarch.test.core.messaging.RequestTestingSendSyncBatchTest].",
                    e.getMessage());
        }
    }

    /**
     * 異常系。ヘッダとボディ行が一致しない。
     */
    @Test
    public void testAbnormalEnd4() {
        try {
            execute();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "number of lines of header and body does not match. number of lines of header=[2], but number of lines of body=[3]. case no=[1], message id=[case6], request id=[RM21AA0101] test class=[nablarch.test.core.messaging.RequestTestingSendSyncBatchTest].",
                    e.getMessage());
        }
    }

    /**
     * 異常系。行が少ない。
     */
    @Test
    public void testAbnormalEnd5() {
        try {
            execute();
            fail();
        } catch (AssertionError e) {
            assertEquals(
                    "number of send message was invalid. expected number=[1], but actual number=[2]. case no=[1], message id=[case6], request id=[RM21AA0101] test class=[nablarch.test.core.messaging.RequestTestingSendSyncBatchTest].",
                    e.getMessage());
        }
    }

    /**
     * 異常系。ヘッダとボディ行が一致しない。
     */
    @Test
    public void testAbnormalEnd6() {
        try {
            execute();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "number of lines of header and body does not match. number of lines of header=[2], but number of lines of body=[1]. case no=[1], message id=[case6], request id=[RM21AA0101] test class=[nablarch.test.core.messaging.RequestTestingSendSyncBatchTest].",
                    e.getMessage());
        }
    }

    /**
     * ヘッダ行が存在しない。
     */
    @Test
    public void testAbnormalEnd7() {
        try {
            execute();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "message was not found. message must be set. case number=[1], message id=[case6], data type=[EXPECTED_REQUEST_HEADER_MESSAGES], path=[src/test/java/nablarch/test/core/messaging], resource name=[RequestTestingSendSyncBatchTest/testAbnormalEnd7].",
                    e.getMessage());
        }
    }

    /**
     * ボディ行が存在しない。
     */
    @Test
    public void testAbnormalEnd8() {
        try {
            execute();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "message was not found. message must be set. case number=[1], message id=[case6], data type=[EXPECTED_REQUEST_BODY_MESSAGES], path=[src/test/java/nablarch/test/core/messaging], resource name=[RequestTestingSendSyncBatchTest/testAbnormalEnd8].",
                    e.getMessage());
        }
    }


    /**
     * expectedMessageの識別子に対応するテーブルが存在しない場合のテスト。
     */
    @Test
    public void testExpectedMessageNotExist1() {

        try {
            execute();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "message was not found. message must be set. " +
                    "case number=[1], message id=[notExist], data type=[EXPECTED_REQUEST_HEADER_MESSAGES], " +
                    "path=[src/test/java/nablarch/test/core/messaging], " +
                    "resource name=[RequestTestingSendSyncBatchTest/testExpectedMessageNotExist1].",
                    e.getMessage());
        }
    }

    /**
     * responseMessageの識別子に対応するテーブルが存在しない場合のテスト。
     */
    @Test
    public void testExpectedMessageNotExist2() {

        try {
            execute();
            fail();
        } catch (IllegalStateException e) {
            assertEquals(
                    "message was not found. message must be set. " +
                    "case number=[1], message id=[notExist], data type=[EXPECTED_REQUEST_BODY_MESSAGES], " +
                    "path=[src/test/java/nablarch/test/core/messaging], resource name=[RequestTestingSendSyncBatchTest/testExpectedMessageNotExist2].",
                    e.getMessage());
        }
    }
    
    /**
     * expectedMessageの識別子に対応するテーブルが存在しない場合のテスト。
     */
    @Test
    public void testResponseMessageNotExist1() {

        execute();
    }

    /**
     * responseMessageの識別子に対応するテーブルが存在しない場合のテスト。
     */
    @Test
    public void testResponseMessageNotExist2() {

        execute();
    }


    @Test
    public void testPaddingRemoved() {
        execute();
    }
}
