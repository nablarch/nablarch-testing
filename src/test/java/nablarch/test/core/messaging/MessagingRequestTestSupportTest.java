package nablarch.test.core.messaging;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import nablarch.test.Trap;
import nablarch.test.core.db.TestTable;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import junit.framework.AssertionFailedError;

/**
 * @author T.Kawasaki
 */
@RunWith(DatabaseTestRunner.class)
public class MessagingRequestTestSupportTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    /** テスト名 */
    @Rule
    public final TestName testName = new TestName();

    private MessagingRequestTestSupport support = new MessagingRequestTestSupport(getClass());

    @BeforeClass
    public static void beforeClass() {
        VariousDbTestHelper.createTable(TestTable.class);
        VariousDbTestHelper.createTable(SentMessage.class);
        VariousDbTestHelper.createTable(BatchRequest.class);
    }

    /** エラーを期待する場合のターゲット起動処理 */
    private Trap trap = new Trap() {
        @Override
        protected void shouldFail() throws Exception {
            support.execute(testName.getMethodName());
        }
    };

    @Test
    public void testSuccess() {
        support.execute(testName.getMethodName());
    }

    /** XMLの際、構造化データ用フレームワーク制御ヘッダを使用してテストを行えること。 */
    @Test
    public void testUseStructFwHeaderDefXML() {
        support.execute(testName.getMethodName());
    }

    /** XMLの際、構造化データ用フレームワーク制御ヘッダを使用してテストを行えること。 */
    @Test
    public void testUseStructFwHeaderDefJSON() {
        support.execute(testName.getMethodName());
    }

    /** XMLをStringとしてテストを行えること。 */
    @Test
    public void testXmlAsString() {
        support.execute(testName.getMethodName());
    }

    /** XMLをDataRecordとしてテストを行えること。 */
    @Test
    public void testXmlAsDataRecord() {
        support.execute(testName.getMethodName());
    }

    /** 実応答電文が期待電文より長いデータのテストを行えること。 */
    @Test
    public void testLargeResponseData() {
        trap.capture(ComparisonFailure.class)
                .whichMessageContains("02[]", "02[1]");
    }

    /** 実応答電文が期待電文より短いデータのテストを行えること。 */
    @Test
    public void testShortResponseData() {
        trap.capture(ComparisonFailure.class)
                .whichMessageContains("[02]", "[x]");
    }

    /** 実応答電文が期待電文と異なるデータのテストを行えること。 */
    @Test
    public void testDifferentResponseData() {
        trap.capture(ComparisonFailure.class)
                .whichMessageContains("0[5]<", "0[2]");
    }

    /** FWヘッダーのフォーマット定義ファイルをデフォルトから変更して実行できること。 */
    @Test
    public void testUseAnotherFwHeader() {
        support.execute(testName.getMethodName());
    }

    /** ステータスコード比較失敗時に例外が発生すること */
    @Test
    public void testStatusCodeFail() {
        trap.capture(ComparisonFailure.class)
                .whichMessageContains("expected:<[200]>", "but was:<[404]>");
    }

    /** キューに応答電文が存在しない場合、例外が発生すること */
    @Test
    public void testResponseMessageNotFound() {
        trap.capture(AssertionFailedError.class)
                .whichMessageContains("no response message in the queue");
    }

    /** 応答電文の結果が期待通りでない場合、例外が発生すること */
    @Test
    public void testResponseAssertionFailed() {
        trap.capture(ComparisonFailure.class)
                .whichMessageContains("no=[2]",
                        "expected:<[{処理結果コード=Hello, [Hello]}]>",
                        "but was:<[{処理結果コード=Hello, [Tsuyoshi]}]>");
    }

    /** 期待したログメッセージが出力されなかった場合、例外が発生すること */
    @Test
    public void testLogAssertionFailed() {
        trap.capture(AssertionError.class)
                .whichMessageContains("following log message(s) expected.");
    }

    /** testShots表が存在しない場合、例外が発生すること */
    @Test
    public void testNoTestShotFail() {
        trap.capture(IllegalArgumentException.class)
                .whichMessageContains("no test shot found");
    }

    /** データベースの比較に失敗した場合、例外が発生すること */
    @Test
    public void testDbAssertionFailed() {
        trap.capture(ComparisonFailure.class)
                .whichMessageContains("no=[1]",
                        "table=TEST_TABLE line=1 column=VARCHAR2_COL");
    }

    /** 要求電文のテストデータが存在しない場合、例外が発生すること */
    @Test
    public void testSetUpMsgNotFoundFail() {
        trap.capture(IllegalStateException.class)
                .whichMessageContains("no data found",
                        "id=[setUpMessages]");
    }


    /** 要求電文のテストデータが存在しない場合、例外が発生すること */
    @Test
    public void testExpectedMsgNotFoundFail() {
        trap.capture(IllegalStateException.class)
                .whichMessageContains("no data found",
                        "id=[expectedMessages]");
    }


    /** 要求電文の数がテストショット数より不足している場合、例外が発生すること */
    @Test
    public void testSetUpMsgLackingFail() {
        trap.capture(IllegalStateException.class)
                .whichMessageContains("setUpMessage is lacking",
                        "no=[2]");
    }

    /** 要求電文の数がテストショット数より不足している場合、例外が発生すること */
    @Test
    public void testExpectedMsgLackingFail() {
        trap.capture(IllegalStateException.class)
                .whichMessageContains("expectedMessage is lacking",
                        "no=[2]");
    }

    /** デフォルトコンストラクタの起動ができること */
    @Test
    public void testConstructor() {
        new MessagingRequestTestSupport();  // no error occurs.
    }

    @Entity
    @Table(name = "SENT_MESSAGE")
    public static class SentMessage {

        public SentMessage() {
        }

        ;

        public SentMessage(String messageId, String requestId,
                String replyQueue, String statusCode, byte[] bodyData) {
            this.messageId = messageId;
            this.requestId = requestId;
            this.replyQueue = replyQueue;
            this.statusCode = statusCode;
            this.bodyData = bodyData;
        }

        @Id
        @Column(name = "MESSAGE_ID", length = 64, nullable = false)
        public String messageId;

        @Id
        @Column(name = "REQUEST_ID", length = 64, nullable = false)
        public String requestId;

        @Column(name = "REPLY_QUEUE", length = 64, nullable = false)
        public String replyQueue = "";

        @Column(name = "STATUS_CODE", length = 4, nullable = false)
        public String statusCode = "";

        @Lob
        @Column(name = "BODY_DATA")
        public byte[] bodyData;
    }

    @Entity
    @Table(name = "BATCH_REQUEST")
    public static class BatchRequest {

        public BatchRequest() {
        }

        ;

        public BatchRequest(String requestId, String requestName,
                String processHaltFlg, String processActiveFlg,
                String serviceAvailable, String resumePoint) {
            this.requestId = requestId;
            this.requestName = requestName;
            this.processHaltFlg = processHaltFlg;
            this.processActiveFlg = processActiveFlg;
            this.serviceAvailable = serviceAvailable;
            this.resumePoint = resumePoint;
        }

        @Id
        @Column(name = "REQUEST_ID", length = 10, nullable = false)
        public String requestId;

        @Column(name = "REQUEST_NAME", length = 100, nullable = false)
        public String requestName;

        @Column(name = "PROCESS_HALT_FLG", length = 1, nullable = false)
        public String processHaltFlg;

        @Column(name = "PROCESS_ACTIVE_FLG", length = 1, nullable = false)
        public String processActiveFlg;

        @Column(name = "SERVICE_AVAILABLE", length = 1, nullable = false)
        public String serviceAvailable;

        @Column(name = "RESUME_POINT", length = 1, nullable = false)
        public String resumePoint;
    }
}
