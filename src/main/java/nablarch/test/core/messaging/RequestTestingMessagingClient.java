package nablarch.test.core.messaging;

import static nablarch.core.util.Builder.concat;
import static nablarch.core.util.StringUtil.isNullOrEmpty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.core.dataformat.SimpleDataConvertResult;
import nablarch.core.dataformat.SimpleDataConvertUtil;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.FilePathSetting;
import nablarch.core.util.StringUtil;
import nablarch.fw.messaging.InterSystemMessage;
import nablarch.fw.messaging.MessageSenderClient;
import nablarch.fw.messaging.MessageSenderSettings;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.fw.messaging.SyncMessage;
import nablarch.fw.messaging.logging.MessagingLogUtil;
import nablarch.fw.messaging.realtime.http.client.HttpMessagingClient;
import nablarch.fw.messaging.realtime.http.exception.HttpMessagingInvalidDataFormatException;
import nablarch.fw.messaging.realtime.http.exception.HttpMessagingTimeoutException;
import nablarch.test.Assertion;
import nablarch.test.NablarchTestUtils;
import nablarch.test.core.file.DataFileFragment;
import nablarch.test.core.reader.DataType;

/**
 * リクエスト単体テストの際に、テストデータの内容にもとづき、要求電文のアサートおよび応答電文の返却を行うMessageSenderClient。
 * <p>
 * 本クラスを使用する場合、メッセージ送信は行われない。
 * </p>
 * @author TIS
 *
 */
public class RequestTestingMessagingClient implements MessageSenderClient {

    /** メッセージングログを出力するロガー */
    private static final Logger LOGGER = LoggerManager.get("MESSAGING");
    
    /** 呼び出し元から送信された要求電文をキャッシュするstatic領域 */
    private static final List<SyncMessage> SENDING_MESSAGE_CACHE =
        new ArrayList<SyncMessage>();

    /** Excelシート名を取得する */
    private static String sheetName = null;
    /** テストケースNoを取得する */
    private static Integer caseNo = null;
    /** テストクラス名 */
    private static Class<?> testClass = null;
    /** 応答電文のID */
    private static String responseMessageId = null;
    /** 要求電文（期待値）のID */
    private static String expectedRequestMessageId = null;
    /** 本機能を有効にするかどうか */
    private static boolean isMockEnable = false;
    
    /** 応答電文のデータフォーマット定義ファイル名パターン  */
    private String responseMessageFormatFileNamePattern = "%s" + "_RECEIVE";
    
    /** 要求電文のフォーマット定義ファイル名のパターン */
    private static String requestMessageFormatFileNamePattern = "%s" + "_SEND";

    /** SystemRepositoryに設定するデータレコードとしてアサートを行うファイルタイプを管理するためのキー */
    private static final String ASSERT_AS_MAP_KEY = "messaging.assertAsMapFileType";

    /** 文字セット */
    private Charset charset = Charset.defaultCharset();

    /**
     * リクエスト単体テスト時の初期化処理を行う。
     * <p>
     * 本機能のリクエスト単体テストを動作させるために必要なプロパティをstatic領域に設定する。
     * また、要求電文のキャッシュをクリアする。
     * </p>
     * @param clazz テストケースのクラス
     * @param sheetName シート名
     * @param no テストケース番号
     * @param responseMessageId 応答電文のID
     * @param expectedMessageId 要求電文のID
     */
    public static void initializeForRequestUnitTesting(Class<?> clazz,
            String sheetName, String no, String responseMessageId, String expectedMessageId) {
        if (StringUtil.isNullOrEmpty(expectedMessageId)
                && StringUtil.isNullOrEmpty(responseMessageId)) {
            return;
        }
        RequestTestingMessagingClient.isMockEnable = true;
        RequestTestingMessagingClient.testClass = clazz;
        RequestTestingMessagingClient.sheetName = sheetName;
        RequestTestingMessagingClient.caseNo = Integer.parseInt(no);
        RequestTestingMessagingClient.responseMessageId = responseMessageId;
        RequestTestingMessagingClient.expectedRequestMessageId = expectedMessageId;
    }
    
    /**
     * リクエスト単体テスト時に使用する要求電文のキャッシュをクリアする。
     * <p>
     * 本機能のリクエスト単体テストで使用する要求電文のキャッシュをクリアする。
     * </p>
     */
    public static void clearSendingMessageCache() {
        RequestTestingMessagingClient.SENDING_MESSAGE_CACHE.clear();
        isMockEnable = false;
    }
    
    @Override
    public SyncMessage sendSync(MessageSenderSettings settings, SyncMessage requestMessage) {
        
        if (!isMockEnable) {
            throw new RuntimeException(
                    "expectedMessage and responseMessage was not specified in test data. expectedMessage and responseMessage must be specified.");
        }

        try {

            String requestId = requestMessage.getRequestId();

            // 要求電文をログに出力する
            emitLog(getSendingMessage(requestMessage));

            // 送信されたメッセージをキャッシュする。ここでキャッシュした内容は、後でアサートに使用される
            SENDING_MESSAGE_CACHE.add(requestMessage);

            // 応答メッセージを生成し、返却する
            ReceivedMessage reply = createReceivedMessage(requestId);
            if (reply == null) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.logInfo("response timeout: could not receive a reply to the message."
                            + MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(requestMessage), charset)
                    );
                }
                throw new HttpMessagingTimeoutException(String.format(
                    "caused by timeout, failed to send message. requestId = [%s]", requestId));
            }

            if (!reply.getHeaderMap().containsKey(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE)) {
                //ステータスコードが未設定の場合は、正常時の応答とみなして、200を設定する。
                reply.setHeader(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE, "200");
            }
            
            // 応答電文をログに出力する
            emitLog(reply);

            SimpleDataConvertResult resBodyDataConvertResult = null;
            try {
                String formatName = String.format(responseMessageFormatFileNamePattern, requestMessage.getRequestId());
                resBodyDataConvertResult = SimpleDataConvertUtil.parseData(formatName, new ByteArrayInputStream(reply.getBodyBytes()));
                
            } catch (IOException e) {
                throw new MessagingException(e); // cannot happen
                
            } catch (InvalidDataFormatException e) {
                //ステータスコードの数値表現(ヘッダから取得できなかった場合の初期として0を設定している)
                Integer statusCode = Integer.valueOf(0);
                
                //ステータスコードの文字列表現
                String statusCodeString = (String) reply.getHeader(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE);
                if (isNumber(statusCodeString)) {
                    //ステータスコードがヘッダに適切に設定されている場合は、例外にステータスコードを含める。
                    statusCode = Integer.parseInt(statusCodeString);
                }
                
                String message = String.format("Receive message when converting %s, format error occurs.", requestMessage.getRequestId());
                String data = new String(reply.getBodyBytes(), Charset.forName("UTF-8"));
                throw new HttpMessagingInvalidDataFormatException(message,
                        settings.getUri(), statusCode,
                        new HashMap<String, List<String>>(), data, e);
            }
            
            Map<String, ?> responseData = resBodyDataConvertResult.getResultMap();
            
            //応答電文を生成
            SyncMessage responseMessage = new SyncMessage(requestMessage.getRequestId());
            responseMessage.addDataRecord(responseData);
            responseMessage.setHeaderRecord(reply.getHeaderMap());

            return responseMessage;
        } catch (MessagingException e) {
            // MessagingExceptionの場合は、アサートを行う必要があるので、isMockEnable属性はtrueのままにしておく
            throw e;
        } catch (RuntimeException e) {
            // MessagingException以外の例外の場合は、この時点でテストに失敗しているので、isMockEnable属性をfalseにし、アサートが走らないようにする
            isMockEnable = false;
            throw e;
        }

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
        
        RequestTestingSendSyncSupport support = new RequestTestingSendSyncSupport(testClass);
        
        // 応答電文を取得する
        RequestTestingMessagePool expectedRequestHeaderMessage = support
                .getResponseMessage(sheetName, requestId,
                        caseNo, responseMessageId, DataType.RESPONSE_HEADER_MESSAGES, true);
        RequestTestingMessagePool expectedRequestBodyMessage = support
                .getResponseMessage(sheetName, requestId,
                        caseNo, responseMessageId, DataType.RESPONSE_BODY_MESSAGES, true);

        // ヘッダのレコードを取得する
        DataRecord headerRecord = expectedRequestHeaderMessage.getRequestTestingReceivedMessage(sheetName,
                caseNo, responseMessageId, DataType.RESPONSE_BODY_MESSAGES, requestId);
        // 本文のバイナリを取得する
        byte[] bodyBytes = expectedRequestBodyMessage.createRequestTestingReceivedMessageBinary(sheetName,
                caseNo, responseMessageId, DataType.RESPONSE_BODY_MESSAGES, requestId);
        
        // リトライの場合はnullを返却する
        if (bodyBytes == null) {
            return null;
        }
        
        // 本文のバイナリからReceivedMessageを生成する
        int bufferSize = bodyBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.put(bodyBytes);
        
        ReceivedMessage reply = new ReceivedMessage(buffer.array());
        
        // ヘッダを生成する
        reply.setHeaderMap(headerRecord);
            
        return reply;
    }

    /**
     * 要求メッセージのアサートを行う。
     * @param testClass テストクラス
     * @param sheetName シート名
     * @param caseNo テストケース番号
     * @param expectedRequestMessageId 要求電文（期待値）のID
     */
    public static void assertSendingMessage(Class<?> testClass, String sheetName, String caseNo, String expectedRequestMessageId) {
        
        if (!isMockEnable) {
            return;
        }
        
        // 要求メッセージ（期待値）のIDが設定されていない場合は、テストは行わない
        if (expectedRequestMessageId == null) {
            return;
        }
        
        RequestTestingSendSyncSupport support = new RequestTestingSendSyncSupport(testClass);
        
        Integer caseNoInt = Integer.parseInt(caseNo);
        
        // メッセージIDに紐付くアサート用の要求電文（ヘッダ）を取得する
        List<RequestTestingMessagePool> expectedHeaderMessageList = 
            support.getExpectedRequestMessage(sheetName, caseNoInt, expectedRequestMessageId, DataType.EXPECTED_REQUEST_HEADER_MESSAGES, true);
        // メッセージIDに紐付くアサート用の要求電文（本文）を取得する
        List<RequestTestingMessagePool> expectedBodyMessageList = 
            support.getExpectedRequestMessage(sheetName, caseNoInt, expectedRequestMessageId, DataType.EXPECTED_REQUEST_BODY_MESSAGES, true);

        // アサート用の要求電文をリクエストID単位にまとめて保持する
        Map<String, Map<String, RequestTestingMessagePool>> expectedHeaderAndBodyMap = buildExpectedHeaderAndBodyMap(
                expectedHeaderMessageList, expectedBodyMessageList);

        // 要求電文のアサート
        assertSendingMessage(testClass, caseNoInt, expectedHeaderAndBodyMap, SENDING_MESSAGE_CACHE);
    }

    /** Mapのヘッダが格納されるキー */
    private static final String KEY_HEADER = "header";
    /** Mapの本文が格納されるキー */
    private static final String KEY_BODY = "body";
    
    /**
     * 要求メッセージのアサートを行う。
     * @param testClass テストクラス
     * @param caseNoInt テストケース番号
     * @param expectedHeaderAndBodyMap 要求電文の期待値を保持するMap
     * @param sendingMessageCache 要求メッセージのキャッシュ
     */
    private static void assertSendingMessage(
            Class<?> testClass, Integer caseNoInt,
            Map<String, Map<String, RequestTestingMessagePool>> expectedHeaderAndBodyMap, List<SyncMessage> sendingMessageCache) {
        
        // データレコードとしてアサートを行うファイルタイプ
        Set<String> assertAsDataRecordFileTypes 
                = isNullOrEmpty(SystemRepository.getString(ASSERT_AS_MAP_KEY))
                ? NablarchTestUtils.asSet("Fixed")
                : NablarchTestUtils.asSet(NablarchTestUtils.makeArray(SystemRepository.getString(ASSERT_AS_MAP_KEY)));

        // リクエストIDの単位でアサートを行う
        for (Map.Entry<String, Map<String, RequestTestingMessagePool>> entry : expectedHeaderAndBodyMap
                .entrySet()) {

            String requestId = entry.getKey();
            
            // エラーメッセージを生成する
            String msgOnFail = concat("case no=[", caseNoInt, "], message id=[" , expectedRequestMessageId , "]"
                    , ", request id=[", requestId , "] test class=[", testClass.getName(), "].");
            
            Map<String, RequestTestingMessagePool> expectedPoolMap = entry.getValue();

            // テストケースに対応するヘッダを取得する
            if (!expectedPoolMap.containsKey(KEY_HEADER)) {
                Assertion.fail("not expected header message was send. ", msgOnFail);
            }
            RequestTestingMessagePool expectedHeaderPool = expectedPoolMap.get(KEY_HEADER);
//            DataRecordFormatter headerFormatter = expectedHeaderPool.getFormatter();

            if (!expectedPoolMap.containsKey(KEY_BODY)) {
                Assertion.fail("not expected body message was send. ", msgOnFail);
            }
            RequestTestingMessagePool expectedBodyPool = expectedPoolMap.get(KEY_BODY);
            DataRecordFormatter bodyFormatter = expectedBodyPool.getFormatter();
            
            // 期待する送信メッセージのヘッダのリストを取得する
            List<DataRecord> expectedHeaderRecords = expectedHeaderPool.getExpectedMessageList();
            
            // 期待する送信メッセージの本文のリストを取得する
            List<DataRecord> expectedBodyRecords = expectedBodyPool.getExpectedMessageList();

            // 期待する送信メッセージのヘッダと、期待する送信メッセージの本文の定義の数が一致しない場合、例外をスローする
            if (expectedHeaderRecords.size() != expectedBodyRecords.size()) {
                throw new IllegalStateException(String
                        .format("number of lines of header and body does not match. "
                                + "number of lines of header=["
                                + expectedHeaderRecords.size()
                                + "], but number of lines of body=[" + expectedBodyRecords.size()
                                + "]. " + msgOnFail));
            }
            
            // リクエストIDに紐付く送信メッセージを取得する
            List<SyncMessage> sendingMessages = getSendingMessage(requestId, sendingMessageCache);

            
            // 送信されるメッセージの回数が、期待しているメッセージのサイズが小さい場合、アサーション例外をスローする
            if (sendingMessages.size() > expectedHeaderRecords.size()) {
                Assertion.fail(String
                        .format("number of send message was invalid. "
                                + "expected number=["
                                + expectedHeaderRecords.size()
                                + "], but actual number=[" + sendingMessages.size()
                                + "]. " + msgOnFail));
            }
            // 送信されたメッセージのサイズより、期待しているメッセージのサイズが大きい場合、アサーション例外をスローする
            if (sendingMessages.size() < expectedHeaderRecords.size()) {
                Assertion.fail(String
                        .format("number of send message was invalid. "
                                + "expected number=["
                                + expectedHeaderRecords.size()
                                + "], but actual number=[" + sendingMessages.size()
                                + "]. " + msgOnFail));
            }
            
            
            ArrayBlockingQueue<SyncMessage> queue = new ArrayBlockingQueue<SyncMessage>(sendingMessages.size(), true, sendingMessages);

            // リクエストID単位でアサートを行う
            for (int i = 0; i < expectedHeaderRecords.size(); i++) {
//                DataRecord expectedHeaderRecord = expectedHeaderRecords.get(i);
                DataRecord expectedBodyRecord = expectedBodyRecords.get(i);

//                String headerNo = (String) expectedHeaderRecord.remove(
//                        DataFileFragment.FIRST_FIELD_NO); // 識別子（連番）情報をリストから削除
                String bodyNo = (String) expectedBodyRecord.remove(
                        DataFileFragment.FIRST_FIELD_NO); // 識別子（連番）情報をリストから削除

                SyncMessage sendingMessage = queue.poll();
                
                SimpleDataConvertResult reqBodyDataConvertResult = null;
                try {
                    String formatName = String.format(requestMessageFormatFileNamePattern, sendingMessage.getRequestId());
                    reqBodyDataConvertResult = SimpleDataConvertUtil.buildData(formatName, sendingMessage.getDataRecord());
                    
                } catch (InvalidDataFormatException e) {
                    String message = String.format("Sending message when converting %s, format error occurs.", sendingMessage.getRequestId());
                    throw new MessagingException(message, e);
                }
                

                // 要求電文を読み込むために使用するReceivedMessageを生成する
                ReceivedMessage requestMessage = null;
                requestMessage = new ReceivedMessage(reqBodyDataConvertResult.getResultText().getBytes(reqBodyDataConvertResult.getCharset()));
                // HttpMessageClient本体がFW制御ヘッダの設定に対応していないため、コメントアウト
//                // ヘッダを読み込む
//                DataRecord actualHeaderRecord = requestMessage.setFormatter(headerFormatter).readRecord();
//                Assertion.assertEquals("header message assertion failed. "
//                        + msgOnFail + " test no=[" + headerNo + "].",
//                        expectedHeaderRecord, actualHeaderRecord);
                
                // テストデータ変換
                expectedBodyRecord = expectedBodyPool.convertByFileType(expectedBodyRecord);
                // 対応するレイアウト定義を生成
                LayoutDefinition ld = expectedBodyPool.createLayoutFromDataRecord(expectedBodyRecord);
                
                String expectedFileType = getFileTypeFromDirective(ld);
                
                if (assertAsDataRecordFileTypes.contains(expectedFileType)) {
                    // データレコードとして項目ごとにアサート
                    
                    // 本文を読み込む
                    DataRecord actualBodyRecord = requestMessage.setFormatter(bodyFormatter.setDefinition(ld)).readRecord();
                    Assertion.assertEquals("body message assertion failed. "
                            + msgOnFail + " test no=[" + bodyNo + "].",
                                           expectedBodyRecord, actualBodyRecord);
                } else {
                    // 文字列として電文全体をアサート
                    Charset expectedCharset = getCharsetFromDirective(ld);
                                            
                    byte[] expectedBodyBytes = new SendingMessage()
                                                .setFormatter(bodyFormatter.setDefinition(ld))
                                                .addRecord(expectedBodyRecord)
                                                .getBodyBytes();
                    
                    String expectedBody = new String(expectedBodyBytes, expectedCharset);
                    String actualBody = new String(requestMessage.getBodyBytes(), expectedCharset);
                    
                    Assertion.assertEquals("body message string assertion failed. "
                            + msgOnFail + " test no=[" + bodyNo + "].",
                                           expectedBody, actualBody);
                }
            }
        }
    }
    
    /**
     * レイアウト定義のディレクティブからファイルタイプを取得。
     * @param ld レイアウト定義
     * @return レイアウト定義に指定されたファイルタイプ
     */
    private static String getFileTypeFromDirective(LayoutDefinition ld) {
        return (String) ld.getDirective().get("file-type");
    }
    
    /**
     * レイアウト定義のディレクティブからエンコーディングを取得。
     * @param ld レイアウト定義
     * @return レイアウト定義に指定されたエンコーディング
     */
    private static Charset getCharsetFromDirective(LayoutDefinition ld) {
        String expectedTextEncoding = (String) ld.getDirective().get("text-encoding");
        return Charset.forName(expectedTextEncoding);
    }
    
    /**
     * リクエストIDに紐付く、送信メッセージのリストを取得する。
     * @param expectedRequestId リクエストID 
     * @param sendingMessageCache 要求メッセージのキャッシュ
     * @return 送信するメッセージのリスト
     */
    private static List<SyncMessage> getSendingMessage(String expectedRequestId,
            List<SyncMessage> sendingMessageCache) {
        
        ArrayList<SyncMessage> list = new ArrayList<SyncMessage>();
        
        for (SyncMessage message : sendingMessageCache) {
            String requestId = message.getRequestId();
            if (expectedRequestId.equals(requestId)) {
                list.add(message);
            }
        }
        
        return list;
    }

    /**
     * 要求電文の期待値を保持するMapを生成する。
     * @param expectedHeaderMessageList ヘッダの期待値のリスト
     * @param expectedBodyMessageList 本文の期待値のリスト
     * @return 要求電文の期待値を保持するMap
     */
    private static Map<String, Map<String, RequestTestingMessagePool>> buildExpectedHeaderAndBodyMap(
            List<RequestTestingMessagePool> expectedHeaderMessageList,
            List<RequestTestingMessagePool> expectedBodyMessageList) {

        Map<String, Map<String, RequestTestingMessagePool>> result = new HashMap<String, Map<String, RequestTestingMessagePool>>();

        for (RequestTestingMessagePool pool : expectedHeaderMessageList) {
            String requestId = pool.getRequestId();
            Map<String, RequestTestingMessagePool> map = new HashMap<String, RequestTestingMessagePool>();
            map.put(KEY_HEADER, pool);
            result.put(requestId, map);
        }
        for (RequestTestingMessagePool pool : expectedBodyMessageList) {
            String requestId = pool.getRequestId();
            if (result.containsKey(requestId)) {
                result.get(requestId).put(KEY_BODY, pool);
            } else {
                Map<String, RequestTestingMessagePool> map = new HashMap<String, RequestTestingMessagePool>();
                map.put(KEY_BODY, pool);
                result.put(requestId, map);
            }
        }
        
        return result;
    }
    

    /**
     * メッセージングの証跡ログを出力する。
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
     * @param message 送信電文オブジェクト
     * @return フォーマット済みのメッセージ
     */
    private SendingMessage getSendingMessage(SyncMessage message) {
        SendingMessage sendingMessage = new SendingMessage();
        sendingMessage.setFormatter(
                FormatterFactory.getInstance().createFormatter(
                        FilePathSetting.getInstance().getFile("format", message.getRequestId() + "_SEND")));
        for (Map<String, Object> rec : message.getDataRecords()) {
            try {
                sendingMessage.addRecord(rec);
            } catch (InvalidDataFormatException e) {
                LOGGER.logInfo(String.format("Sending message convert error occurred. RequestId:%s [%s]", message.getRequestId(), e.getMessage()));
            }
        }
        sendingMessage.setHeaderMap(message.getHeaderRecord());
        return sendingMessage;
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
     * 文字セット名から文字セットを設定する。
     * @param charset 文字セット名
     */
    public void setCharset(String charset) {
        this.charset = Charset.forName(charset);
    }
}
