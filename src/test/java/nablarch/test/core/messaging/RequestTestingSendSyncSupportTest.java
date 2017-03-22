package nablarch.test.core.messaging;

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import nablarch.core.dataformat.DataRecord;
import nablarch.test.core.reader.DataType;
import nablarch.test.support.SystemRepositoryResource;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link RequestTestingSendSyncSupport}のテストクラス。
 *
 * @author Ryo TANAKA
 */
public class RequestTestingSendSyncSupportTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");


    RequestTestingSendSyncSupport target;

    @Before
    public void setup() {
        target = new RequestTestingSendSyncSupport(this.getClass());
    }

    /**
     * {@link RequestTestingSendSyncSupport#getExpectedRequestMessage(String, Integer, String, DataType, boolean)}のテスト。
     *
     * キャッシュを利用しない場合にも、正常にテストデータを取得できることを確認する。
     */
    @Test
    public void testGetExpectedRequestMessageWithoutCache() {
        List<RequestTestingMessagePool> messages =
                target.getExpectedRequestMessage("testGetExpectedRequestMessage", 1, "case1", DataType.EXPECTED_REQUEST_HEADER_MESSAGES, false);
        assertThat(messages.size(), is(1));
        List<DataRecord> messageList = messages.get(0).getExpectedMessageList();
        assertThat(messageList.size(), is(1));
        DataRecord message = messageList.get(0);
        assertThat(message.getString("requestId"), is("RM21AA0104_01"));
    }
}
