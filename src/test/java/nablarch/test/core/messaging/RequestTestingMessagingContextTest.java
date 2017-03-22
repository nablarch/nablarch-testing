package nablarch.test.core.messaging;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import nablarch.test.Trap;
import nablarch.test.core.batch.BatchRequestTestSupport;
import nablarch.test.core.messaging.RequestTestingMessagingProvider.RequestTestingMessagingContext;
import nablarch.test.support.SystemRepositoryResource;

import org.junit.Rule;
import org.junit.Test;

/**
 * {@link RequestTestingMessagingContext}のテストクラス。
 *
 * @author T.Kawasaki
 */
public class RequestTestingMessagingContextTest extends BatchRequestTestSupport {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    /**
     * EXPECTED_REQUEST_HEADER_MESSAGESがテストデータに存在しない場合、
     * executeメソッド呼び出しで例外が発生すること。
     */
    @Test
    public void testExpectedRequestHeaderMessagesNotFound() {
        new Trap() {
            @Override
            protected void shouldFail() throws Exception {
                execute("testExpectedRequestHeader");
            }
        }.capture(IllegalStateException.class)
         .whichMessageContains(
                 "message was not found. message must be set.",
                 "case number=[1]",
                 "data type=[EXPECTED_REQUEST_HEADER_MESSAGES]"
         );
    }

    /**
     * EXPECTED_REQUEST_BODY_MESSAGESがテストデータに存在しない場合、
     * executeメソッド呼び出しで例外が発生すること。
     */
    @Test
    public void testExpectedRequestBodyMessagesNotFound() {

        new Trap() {
            @Override
            protected void shouldFail() throws Exception {
                execute("testExpectedRequestBody");
            }
        }.capture(IllegalStateException.class)
         .whichMessageContains(
                 "message was not found. message must be set.",
                 "case number=[1]",
                 "data type=[EXPECTED_REQUEST_BODY_MESSAGES]"
         );

    }

    /**
     * RESPONSE_HEADER_MESSAGESがテストデータに存在しない場合、
     * 同期応答処理中に例外が発生しログ出力されること。
     */
    @Test
    public void testResponseHeaderMessagesNotFound() {
        execute("testResponseHeader");
    }

    /**
     * RESPONSE_BODY_MESSAGESがテストデータに存在しない場合、
     * 同期応答処理中に例外が発生しログ出力されること。
     */
    @Test
    public void testResponseBodyMessagesNotFound() {
        execute("testResponseBody");
    }

    /**
     * expectedMessage, responseMessageが指定されていない場合には、メッセージのアサートを行わないこと。
     */
    @Test
    public void testNoAssertion() {
        execute("testNoAssertion");
    }

    /**
     * ボディに対応するヘッダーが定義されていない場合には、テストが失敗すること。
     */
    @Test
    public void testNoMatchingHeader() {
        try {
            execute("testNoMatchingHeader");
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("not expected header message was send."));
            assertThat(e.getMessage(), containsString("case no=[1]"));
            assertThat(e.getMessage(), containsString("message id=[case1]"));
            assertThat(e.getMessage(), containsString("request id=[RM21AA0104_02]"));
        }
    }

    /**
     * ヘッダーに対応するボディが定義されていない場合には、テストが失敗すること。
     */
    @Test
    public void testNoMatchingBody() {
        try {
            execute("testNoMatchingBody");
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("not expected body message was send."));
            assertThat(e.getMessage(), containsString("case no=[1]"));
            assertThat(e.getMessage(), containsString("message id=[case1]"));
            assertThat(e.getMessage(), containsString("request id=[RM21AA0104_02]"));
        }
    }
}
