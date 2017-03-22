package nablarch.test.core.messaging;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.DataRecordFormatterSupport;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.SimpleDataConvertResult;
import nablarch.core.dataformat.SimpleDataConvertUtil;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.messaging.InterSystemMessage;
import nablarch.fw.messaging.MessageSenderClient;
import nablarch.fw.messaging.MessageSenderSettings;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.fw.messaging.SyncMessage;
import nablarch.fw.messaging.logging.MessagingLogUtil;
import nablarch.fw.messaging.realtime.http.client.HttpMessagingClient;
import nablarch.fw.messaging.realtime.http.exception.HttpMessagingInvalidDataFormatException;
import nablarch.fw.messaging.realtime.http.exception.HttpMessagingTimeoutException;
import nablarch.test.core.reader.DataType;

/**
 * テストデータの内容にもとづき、任意の応答電文を返却するMessageSenderClient。
 * @author Masaya Seko
 */
public class MockMessagingClient implements MessageSenderClient {
    /** メッセージングログを出力するロガー */
    private static final Logger LOGGER = LoggerManager.get("MESSAGING");

    /** 応答電文のデータフォーマット定義ファイル名パターン  */
    private String requestMessageFormatFileNamePattern = "%s" + "_SEND";

    /** 応答電文のデータフォーマット定義ファイル名パターン  */
    private String responseMessageFormatFileNamePattern = "%s" + "_RECEIVE";

    /** 文字セット */
    private Charset charset = Charset.forName("UTF-8");

    @Override
    public SyncMessage sendSync(MessageSenderSettings settings,
            SyncMessage requestMessage) {
        // 要求電文をログに出力する
        emitLog(getSendingMessage(requestMessage));

        SendSyncSupport support = new SendSyncSupport();

        // Excelファイルから応答電文を取得する
        byte[] bodyBytes = support.getResponseMessageBinaryByRequestId(DataType.RESPONSE_BODY_MESSAGES, requestMessage.getRequestId());
        if (bodyBytes == null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.logInfo("response timeout: could not receive a reply to the message."
                        + MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(requestMessage),charset)
                        );
            }
            throw new HttpMessagingTimeoutException(String.format(
                "caused by timeout, failed to send message. requestId = [%s]", requestMessage.getRequestId()));
        }
        // Excelファイルからヘッダのレコードを取得する
        Map<String, Object> headerRecord;
        try {
            headerRecord = support.getResponseMessageByRequestId(DataType.RESPONSE_HEADER_MESSAGES, requestMessage.getRequestId());
        } catch (IllegalStateException e) {
            // ヘッダレコードが読み取れない場合は、空のオブジェクトを使用する。
            headerRecord = new HashMap<String, Object>();
        }
        
        if (!headerRecord.containsKey(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE)) {
            //ステータスコードが未設定の場合は、正常時の応答とみなして、200を設定する。
            headerRecord.put(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE, "200");
        }
        
        SimpleDataConvertResult bodyResult = bodyStringToMap(settings.getUri(), headerRecord, requestMessage, bodyBytes);
        
        // 応答電文の作成
        SyncMessage responseSyncMessage = new SyncMessage(requestMessage.getRequestId());
        responseSyncMessage.addDataRecord(bodyResult.getResultMap());
        responseSyncMessage.setHeaderRecord(headerRecord);

        // 応答電文をログに出力する
        emitLog(getReceivedMessage(bodyBytes));
        return responseSyncMessage;
    }

    /**
     * 返信のボディ部分を解析し、応答電文に設定するデータを生成する。
     * @param uri 接続先先のURI
     * @param headerRecord ヘッダ
     * @param requestMessage 要求電文
     * @param bodyBytes 応答電文のバイト列
     * @return 解析後のMap
     */
    private SimpleDataConvertResult bodyStringToMap(String uri, Map<String, Object> headerRecord, SyncMessage requestMessage, byte[] bodyBytes) {
        SimpleDataConvertResult ret = null;

        //フォーマッタから取得した文字コードでエンコードをする。
        String formatName = String.format(responseMessageFormatFileNamePattern, requestMessage.getRequestId());
        DataRecordFormatter formatter = getFormatter(formatName);
        Charset charset = getCharset(formatter);
        
        String data = new String(bodyBytes, charset);

        try {
            ret = SimpleDataConvertUtil.parseData(formatName, data);
        } catch (InvalidDataFormatException e) {
            //ステータスコードの数値表現(ヘッダから取得できなかった場合の初期として0を設定している)
            Integer statusCode = Integer.valueOf(0);

            //ステータスコードの文字列表現
            String statusCodeString = (String) headerRecord.get(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE);
            if (isNumber(statusCodeString)) {
                //ステータスコードがヘッダに適切に設定されている場合は、例外にステータスコードを含める。
                statusCode = Integer.parseInt(statusCodeString);
            }

            String message = "Invalid receive message format. requestId=[" + requestMessage.getRequestId() + "].";
            throw new HttpMessagingInvalidDataFormatException(message, uri, statusCode, new HashMap<String, List<String>>(), data, e);
        }
        return ret;
    }

    /**
     * 引数が数値に変換可能か否か検証する。
     * @param val 検証対象
     * @return trueの場合、変換可能
     */
    private boolean isNumber(String val) {
        try {
            Integer.parseInt(val);
            return true;
        } catch (NumberFormatException nfex) {
            return false;
        }
    }

    /**
     * メッセージングの証跡ログを出力する。
     * 
     * @param message メッセージオブジェクト
     */
    private void emitLog(InterSystemMessage<?> message) {
        String log = (message instanceof ReceivedMessage)
                ? MessagingLogUtil.getHttpReceivedMessageLog((ReceivedMessage) message, charset)
                : MessagingLogUtil.getHttpSentMessageLog((SendingMessage) message, charset);
        LOGGER.logInfo(log);
    }

    /**
     * 電文送信時に出力するログの内容を返す。
     * 
     * @param message 送信電文オブジェクト
     * @return フォーマット済みのメッセージ
     */
    private SendingMessage getSendingMessage(SyncMessage message) {
        SendingMessage sendingMessage = new SendingMessage();
        String formatName = String.format(requestMessageFormatFileNamePattern, message.getRequestId());
        sendingMessage.setFormatter(
                FormatterFactory.getInstance().createFormatter(FilePathSetting.getInstance().getFile("format", formatName)));
        for (Map<String, Object> rec : message.getDataRecords()) {
            sendingMessage.addRecord(rec);
        }
        sendingMessage.setHeaderMap(message.getHeaderRecord());
        return sendingMessage;
    }

    /**
     * 応答電文を返却する。
     * @param bodyBytes 応答電文のデータ
     * @return 応答電文
     */
    private ReceivedMessage getReceivedMessage(byte[] bodyBytes) {
        // 本文のバイナリからReceivedMessageを生成する
        ByteBuffer buffer = ByteBuffer.allocate(bodyBytes.length);
        buffer.put(bodyBytes);
        return new ReceivedMessage(buffer.array());
    }

    /**
     * フォーマット名に対応したフォーマッタを取得する。
     * 
     * @param formatName フォーマット名
     * @return フォーマッタ
     */
    private DataRecordFormatter getFormatter(String formatName) {
        // フォーマットファイルを論理パスから取得
        File formatFile = FilePathSetting
                .getInstance()
                .getFileWithoutCreate("format", formatName);

        // フォーマッタを生成・初期化
        DataRecordFormatter formatter = FormatterFactory
                .getInstance()
                .createFormatter(formatFile);

        formatter.initialize();

        return formatter;
    }

    /**
     * フォーマッタに定義されている文字セットを取得する。
     * 取得できない場合はプラットフォームのデフォルト文字セットを取得する。
     * @param formatter フォーマッタ
     * @return 文字セット
     */
    private Charset getCharset(DataRecordFormatter formatter) {
        Charset charset = Charset.defaultCharset();
        if (formatter instanceof DataRecordFormatterSupport) {
            charset = ((DataRecordFormatterSupport) formatter).getDefaultEncoding();
        }
        return charset;
    }

    /**
     * 文字セット名から文字セットを設定する。
     * @param charset 文字セット名
     */
    public void setCharset(String charset) {
        this.charset = Charset.forName(charset);
    }
}
