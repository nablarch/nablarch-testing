package nablarch.test.core.messaging;

import java.nio.ByteBuffer;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.fw.messaging.MessageSenderSettings;
import nablarch.fw.messaging.MessagingContext;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.fw.messaging.logging.MessagingLogUtil;
import nablarch.test.core.reader.DataType;

/**
 * テストデータの内容にもとづき、要求電文のログ出力および、任意の応答電文を返却するMessagingContext。
 * <p>
 * 本クラスは、画面オンライン処理方式の取引単体テストのように、VMを立ち上げたままで連続してテストを行う場面での使用を想定している。
 * よって、テストデータのExcelファイルのタイムスタンプが更新された場合に、応答電文のカウンタを更新する（テストデータを再読み込みする）機能を提供する。
 * </p>
 * <p>
 * 本クラスを使用する場合、キューへのアクセスは行われない。
 * </p>
 * @author Masato Inoue
 */
public class MockMessagingContext extends MessagingContext {

    /** メッセージングログを出力するロガー */
    private static final Logger LOGGER = LoggerManager.get("MESSAGING");

    /** sendSyncメソッドが呼ばれるたびに発行するメッセージIDのカウント */
    private static int sendSyncCount = 0;

    /**
     * {@inheritDoc}<br/>
     * この実装では、sendSyncメソッドをオーバーライドし、要求電文のアサートおよび、応答電文の返却を行う。
     * <p>
     * 本メソッドは、要求電文ヘッダに「requestId」という名前のフィールドがある前提で動作する。
     * </p>
     */
    @Override
    public ReceivedMessage sendSync(SendingMessage message, long timeout) {

        String messageId = "messageId" + ++sendSyncCount;
        message.setMessageId(messageId);

        String requestId = (String) message.getRecords().get(0)
                .get("requestId");

        // 要求電文をログに出力する
        emitLog(message);

        SendSyncSupport support = new SendSyncSupport();

        // 要求電文（ヘッダ）および本文をログに出力するためパースする（アサートは行わない）
        support.parseRequestMessage(requestId, message);
        
        // 応答メッセージを生成し、返却する
        ReceivedMessage reply = createReceivedMessage(requestId);

        if (reply == null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.logInfo("response timeout: could not receive a reply to the message below within "
                        + timeout
                        + "msec. "
                        + MessagingLogUtil.getSentMessageLog(message));
            }
            return null;
        }
        reply.setMessageId(messageId);

        // 受信電文をログに出力する
        // 応答電文としては作成前であり、フォーマッタ等はまだ未設定の状態であるため、設定される想定のフォーマッタを使用しておく
        // ここでのフォーマッタ設定はあくまでログ用であるため、返却する受信電文には影響しないようにコピーを利用する
        MessageSenderSettings settings = new MessageSenderSettings(requestId);
        ReceivedMessage replyForLogging = new ReceivedMessage(reply);
        replyForLogging.setFormatter(settings.getReceivedDataFormatter());
        emitLog(replyForLogging);

        return reply;
    }

    
    /**
     * 任意の応答電文を返却する。
     * <p>
     * 返却する応答電文は、Excelから読み込む。
     * </p>
     * @param requestId リクエストID
     * @return 応答電文
     */
    private ReceivedMessage createReceivedMessage(String requestId) {

        SendSyncSupport support = new SendSyncSupport();

        // 応答電文を取得する
        byte[] headerBytes = support
                .getResponseMessageBinaryByRequestId(DataType.RESPONSE_HEADER_MESSAGES, requestId);
        byte[] bodyBytes = support
                .getResponseMessageBinaryByRequestId(DataType.RESPONSE_BODY_MESSAGES, requestId);

        // リトライの場合はnullを返却する
        if (bodyBytes == null) {
            return null;
        }

        // ヘッダと本文のバイナリからReceivedMessageを生成する
        int bufferSize = (headerBytes != null ? headerBytes.length : 0) + bodyBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        if (headerBytes != null) {
            buffer.put(headerBytes);
        }
        buffer.put(bodyBytes);
        return new ReceivedMessage(buffer.array());
    }


    /** {@inheritDoc}<br/>
     * この実装では何も行わない。
     */
    @Override
    public void close() {
        // nop
    }

    /** {@inheritDoc}<br/> */
    @Override
    public String send(
            SendingMessage message) throws UnsupportedOperationException {

        // 要求電文をログに出力する
        emitLog(message);

        return "messageId";
    }

    /** {@inheritDoc}<br/> */
    @Override
    public String sendMessage(
            SendingMessage message) throws UnsupportedOperationException {
        return send(message);
    }

    /** {@inheritDoc}<br/>
     * この実装ではこのメソッドはサポートしない。
     * @throws UnsupportedOperationException このメソッドが呼ばれた場合にスローされる例外
     */
    @Override
    public ReceivedMessage receiveMessage(String receiveQueue,
            String messageId, long timeout) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("this method was unsupported.");
    }

}
