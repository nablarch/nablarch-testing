package nablarch.test.core.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.core.dataformat.RecordDefinition;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.messaging.MessageSenderSettings;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.messaging.SendingMessage;
import nablarch.fw.messaging.SyncMessage;
import nablarch.fw.messaging.logging.MessagingLogUtil;
import nablarch.fw.messaging.realtime.http.exception.HttpMessagingInvalidDataFormatException;
import nablarch.fw.messaging.realtime.http.exception.HttpMessagingTimeoutException;
import nablarch.test.RepositoryInitializer;
import nablarch.test.core.file.TestDataConverter;
import nablarch.test.core.log.LogVerifier;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * {@link RequestTestingMessagingClient}のテストクラス
 * 
 * @author TIS
 */
public class RequestTestingMessagingClientTest {

    /** UTF-8 文字セット */
    private Charset utf8Charset = Charset.forName("UTF-8");

    /** 初期化を行う */
    @BeforeClass
    public static void loadRepository() {
        new RequestTestingSendSyncSupport(RequestTestingMessagingClientTest.class);
        
        RepositoryInitializer.recreateRepository("nablarch/test/core/messaging/web/web-component-configuration" +
                                                         "-request-testing.xml");
    }
    
    /** テスト実行後にリポジトリを戻す。    */
    @AfterClass
    public static void tearDown() {
       RepositoryInitializer.revertDefaultRepository();
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
                // NOP
            }
        }
        sendingMessage.setHeaderMap(message.getHeaderRecord());
        return sendingMessage;
    }

    /**
     * テスト用のレコードを作成します
     * @return テスト用のレコード
     */
    private Map<String, Object> createTestRecord() {
        Map<String, Object> rec = new HashMap<String, Object>();
        rec.put("userId", "0000000101");
        rec.put("resendFlag", "0");
        rec.put("dataKbn", "0");
        rec.put("loginId", "LOGINID");
        rec.put("kanjiName", "漢字 名１");
        rec.put("kanaName", "カナ メイ");
        rec.put("mailAddress", "mail@mail.com1");
        rec.put("extensionNumber.building", "11");
        rec.put("extensionNumber.personal", "1001");
        rec.put("mobilePhoneNumber.areaCode", "201");
        rec.put("mobilePhoneNumber.cityCode", "3001");
        rec.put("mobilePhoneNumber.sbscrCode", "4001");
        return rec;
    }
    
    /**
     * 構造化データ同期送信の正常系テストを行う。
     */
    @Test
    public void testSendSyncNormal() throws Exception {
        Map<String, Object> reqrec = createTestRecord();

        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);

        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testSendSync", "1", "case1", "RM11AD0201");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        final RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        final SyncMessage reply = client.sendSync(settings, request);
        assertEquals("200", reply.getHeaderRecord().get("STATUS_CODE"));
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec = reply.getDataRecord();
        assertEquals("00000000000000000112", resrec.get("userInfoId"));
        assertEquals("0", resrec.get("dataKbn"));
        assertEquals("200", resrec.get("_nbctlhdr.statusCode"));
        
        RequestTestingMessagingClient.assertSendingMessage(getClass(), "testSendSync", "1", "case1");
    }
    
    /**
     * 固定長データ同期送信の正常系テストを行う。
     */
    @Test
    public void testAsFixedData() throws Exception {
        Map<String, Object> reqrec = new HashMap<String, Object>();
        reqrec.put("title", "title001");
        reqrec.put("publisher", "publisher002");
        reqrec.put("authors", "authors003");
        
        SyncMessage request = new SyncMessage("RM11AD0202");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0202");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);
        
        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testAsFixedData", "1", "case1", "RM11AD0202");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        final RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        final SyncMessage reply = client.sendSync(settings, request);
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec = reply.getDataRecord();
        assertEquals("test1", resrec.get("failureCode"));
        assertEquals("user1", resrec.get("userInfoId"));
        
        RequestTestingMessagingClient.assertSendingMessage(getClass(), "testAsFixedData", "1", "case1");
    }
    
    
    /**
     * Excelにステータスコードが記載されていない場合の、構造化データ同期送信の正常系テストを行う。
     */
    @Test
    public void testSendLessStatusCode() throws Exception {
        Map<String, Object> reqrec = createTestRecord();
        
        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);

        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testLessStatusCode", "1", "case1", "RM11AD0201");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        final RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        final SyncMessage reply = client.sendSync(settings, request);
        //明示的にEXせｌにステータスコードが設定されていない場合は、200が設定されている。
        assertEquals("200", reply.getHeaderRecord().get("STATUS_CODE"));
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec = reply.getDataRecord();
        assertEquals("00000000000000000112", resrec.get("userInfoId"));
        assertEquals("0", resrec.get("dataKbn"));
        assertEquals("200", resrec.get("_nbctlhdr.statusCode"));
        
        RequestTestingMessagingClient.assertSendingMessage(getClass(), "testLessStatusCode", "1", "case1");
    }

    /**
     * テストデータコンバータを使用した場合の正常系テストを行う。
     */
    @Test
    public void testWithTestDataConverter() throws Exception {
        Map<String, Object> reqrec = new HashMap<String, Object>();
        reqrec.put("title", "HOGE");
        reqrec.put("publisher", "HOGE");
        reqrec.put("authors", "HOGE");
        
        SyncMessage request = new SyncMessage("RM11AD0202");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0202");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);
        
        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testWithTestDataConverter", "1", "case1", "RM11AD0202");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        final RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        final SyncMessage reply = client.sendSync(settings, request);
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec = reply.getDataRecord();
        assertEquals("HOGE", resrec.get("failureCode"));
        assertEquals("HOGE", resrec.get("userInfoId"));
        
        RequestTestingMessagingClient.assertSendingMessage(getClass(), "testWithTestDataConverter", "1", "case1");
    }
    
    public static class HogeDataConvertor implements TestDataConverter {
        @Override
        public DataRecord convertData(LayoutDefinition definition,
                DataRecord currentData, Charset encoding) {
            for (String key : currentData.keySet()) {
                currentData.put(key, "HOGE");
            }
            return currentData;
        }
        
        @Override
        public LayoutDefinition createDefinition(
                LayoutDefinition defaultDefinition, DataRecord currentData,
                Charset encoding) {
            
            // 元レイアウト定義の複製
            RecordDefinition rd = new RecordDefinition();
            RecordDefinition orgRd = defaultDefinition.getRecords().get(0);
            rd.setTypeName(orgRd.getTypeName());
            for (FieldDefinition orgFd : orgRd.getFields()) {
                FieldDefinition fd = new FieldDefinition().setName(orgFd.getName()).setPosition(orgFd.getPosition());
                for (Entry<String, Object[]> orgSetting : orgFd.getConvertorSettingList().entrySet()) {
                    fd.addConvertorSetting(orgSetting.getKey(), orgSetting.getValue());
                }
                rd.addField(fd);
            }
            
            LayoutDefinition ld = new LayoutDefinition();
            ld.getDirective().putAll(defaultDefinition.getDirective());
            ld.addRecord(rd);
            
            return ld;
        }
    }
    
    /**
     * 初期化が正しく行われない場合のテストです。
     */
    @Test
    public void testInitializeForRequestUnitTestingError() throws Exception {
        // expectedMessage と responseMessage が null
        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testSendSync", "1", null, null);
        try {
            new RequestTestingMessagingClient().sendSync(null, null);
            fail("例外が発生する");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("expectedMessage and responseMessage was not specified in test data. expectedMessage and responseMessage must be specified."));
        }
        
        // expectedMessage が null
        RequestTestingMessagingClient.assertSendingMessage(getClass(), "testSendSync", "1", null);
        
        Map<String, Object> reqrec = createTestRecord();

        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testSendSync", "1", "case1", null);
        try {
            new RequestTestingMessagingClient().sendSync(settings, request);
            fail("例外が発生する");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("response message was not found in sheet. sheet name=[testSendSync], case no=[1], message id=[case1], data type name=[RESPONSE_BODY_MESSAGES], request id=[RM11AD0201]."));
        }

        // expectedMessage が null の状態でassert
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testSendSync", "1", "case1", null);
        RequestTestingMessagingClient.assertSendingMessage(getClass(), "testSendSync", "1", null);
    }
    
    /**
     * タイムアウトが発生する場合のテストです。
     */
    @Test
    public void testTimeout() throws Exception {
        Map<String, Object> reqrec = createTestRecord();
        
        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testTimeout", "1", "case1", "RM11AD0201");
        
        try {
            new RequestTestingMessagingClient().sendSync(settings, request);
            fail("例外が発生する");
        } catch (HttpMessagingTimeoutException e) {
            assertTrue(e.getMessage().contains("caused by timeout, failed to send message. requestId = [RM11AD0201]"));
        }
    }

    /**
     * 応答データの変換に失敗する場合のテストです。
     */
    @Test
    public void testInvalidResponse() throws Exception {
        Map<String, Object> reqrec = createTestRecord();
        
        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testInvalidResponse", "1", "case1", "RM11AD0201");
        
        try {
            new RequestTestingMessagingClient().sendSync(settings, request);
            fail("例外が発生する");
        } catch (HttpMessagingInvalidDataFormatException e) {
            assertTrue(e.getMessage().contains("Receive message when converting RM11AD0201, format error occurs."));
        }
    }
    
    /**
     * 要求電文のアサートに失敗する場合のテストです。
     *　・期待ヘッダが取得できない場合
     */
    @Test
    public void testAssertFailNoMatchHeader() throws Exception {
        Map<String, Object> reqrec = createTestRecord();
        
        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);

        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testAssertFailNoMatchHeader", "1", "case1", "RM11AD0201");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        final RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        final SyncMessage reply = client.sendSync(settings, request);
        assertEquals("200", reply.getHeaderRecord().get("STATUS_CODE"));
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec = reply.getDataRecord();
        assertEquals("00000000000000000112", resrec.get("userInfoId"));
        assertEquals("0", resrec.get("dataKbn"));
        assertEquals("200", resrec.get("_nbctlhdr.statusCode"));

        try {
            RequestTestingMessagingClient.assertSendingMessage(getClass(), "testAssertFailNoMatchHeader", "1", "case1");
            fail("例外が発生する");
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("not expected header message was send."));
        }
    }

    /**
     * 要求電文のアサートに失敗する場合のテストです。
     *　・期待ボディが取得できない場合
     */
    @Test
    public void testAssertFailNoMatchBody() throws Exception {
        Map<String, Object> reqrec = createTestRecord();
        
        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);

        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testAssertFailNoMatchBody", "1", "case1", "RM11AD0201");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        final RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        final SyncMessage reply = client.sendSync(settings, request);
        assertEquals("200", reply.getHeaderRecord().get("STATUS_CODE"));
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec = reply.getDataRecord();
        assertEquals("00000000000000000112", resrec.get("userInfoId"));
        assertEquals("0", resrec.get("dataKbn"));
        assertEquals("200", resrec.get("_nbctlhdr.statusCode"));

        try {
            RequestTestingMessagingClient.assertSendingMessage(getClass(), "testAssertFailNoMatchBody", "1", "case1");
            fail("例外が発生する");
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("not expected body message was send."));
        }
    }

    /**
     * 要求電文のアサートに失敗する場合のテストです。
     *　・期待ヘッダと期待ボディの数が違う場合
     */
//    @Test
    public void testAssertFailNoMatchCount() throws Exception {
        Map<String, Object> reqrec = createTestRecord();
        
        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);

        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testAssertFailNoMatchCount", "1", "case1", "RM11AD0201");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        final RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        final SyncMessage reply = client.sendSync(settings, request);
        assertEquals("200", reply.getHeaderRecord().get("STATUS_CODE"));
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec = reply.getDataRecord();
        assertEquals("00000000000000000112", resrec.get("userInfoId"));
        assertEquals("0", resrec.get("dataKbn"));
        assertEquals("200", resrec.get("_nbctlhdr.statusCode"));

        try {
            RequestTestingMessagingClient.assertSendingMessage(getClass(), "testAssertFailNoMatchCount", "1", "case1");
            fail("例外が発生する");
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("number of lines of header and body does not match.　"
                    + "number of lines of header=[1]"
                    + ", but number of lines of body=[2]."
                    ));
        }
    }

    /**
     * 要求電文のアサートに失敗する場合のテストです。
     *　・送信回数が想定より多い場合
     */
    @Test
    public void testAssertFailSendCountMore() throws Exception {
        Map<String, Object> reqrec = createTestRecord();
        
        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);

        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testAssertFailSendCountMore", "1", "case1", "RM11AD0201");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        SyncMessage reply = client.sendSync(settings, request);
        assertEquals("200", reply.getHeaderRecord().get("STATUS_CODE"));
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec = reply.getDataRecord();
        assertEquals("00000000000000000112", resrec.get("userInfoId"));
        assertEquals("0", resrec.get("dataKbn"));
        assertEquals("200", resrec.get("_nbctlhdr.statusCode"));

        // dummy
        client.sendSync(settings, request);
        
        try {
            RequestTestingMessagingClient.assertSendingMessage(getClass(), "testAssertFailSendCountMore", "1", "case1");
            fail("例外が発生する");
        } catch (AssertionError e) {
            assertTrue(e.getMessage(), e.getMessage().contains("number of send message was invalid. "
                    + "expected number=[1], but actual number=[2]."));
        }
    }

    /**
     * 要求電文のアサートに失敗する場合のテストです。
     *　・送信回数が想定より少ない場合
     */
    @Test
    public void testAssertFailSendCountLess() throws Exception {
        Map<String, Object> reqrec = createTestRecord();
        
        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);

        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testAssertFailSendCountLess", "1", "case1", "RM11AD0201");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        SyncMessage reply = client.sendSync(settings, request);
        assertEquals("200", reply.getHeaderRecord().get("STATUS_CODE"));
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec = reply.getDataRecord();
        assertEquals("00000000000000000112", resrec.get("userInfoId"));
        assertEquals("0", resrec.get("dataKbn"));
        assertEquals("200", resrec.get("_nbctlhdr.statusCode"));

        try {
            RequestTestingMessagingClient.assertSendingMessage(getClass(), "testAssertFailSendCountLess", "1", "case1");
            fail("例外が発生する");
        } catch (AssertionError e) {
            assertTrue(e.getMessage(), e.getMessage().contains("number of send message was invalid. "
                    + "expected number=[2], but actual number=[1]."));
        }
    }

    /**
     * 応答電文のアサートに失敗する場合のテストです。
     *　・ステータスコードに例外が発生する値を設定した場合
     */
    @Test
    public void testAssertFailStatusCode() throws Exception {
        Map<String, Object> reqrec = createTestRecord();
        
        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);

        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testAssertFailStatusCode", "1", "case1", "RM11AD0201");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        SyncMessage reply;
        try {
            reply = client.sendSync(settings, request);
            fail("例外が発生する");
        } catch (HttpMessagingInvalidDataFormatException e) {
            assertTrue(e.getMessage().contains("Receive message when converting RM11AD0201, format error occurs."));
        }
    }
    
    /**
     * 要求電文XMLをStringとしてアサート場合のテストです。
     */
    @Test
    public void testAssertAsString() throws Exception {
        // 今回のテスト用リポジトリ
        RepositoryInitializer.recreateRepository("nablarch/test/core/messaging/XmlAssertAsStringTest.xml");
        
        Map<String, Object> reqrec = createTestRecord();
        
        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);

        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testAssertAsString", "1", "case1", "RM11AD0201");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        SyncMessage reply = client.sendSync(settings, request);
        assertEquals("200", reply.getHeaderRecord().get("STATUS_CODE"));
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec = reply.getDataRecord();
        assertEquals("00000000000000000112", resrec.get("userInfoId"));
        assertEquals("0", resrec.get("dataKbn"));
        assertEquals("200", resrec.get("_nbctlhdr.statusCode"));

         RequestTestingMessagingClient.assertSendingMessage(getClass(), "testAssertAsString", "1", "case1");
        
        // リポジトリを元に戻す
        RepositoryInitializer.recreateRepository("nablarch/test/core/messaging/web/web-component-configuration-request-testing.xml");
    }
    
    /**
     * 要求電文XMLをDataRecordとしてアサート場合のテストです。
     */
    @Test
    public void testAssertAsDataRecord() throws Exception {
        // 今回のテスト用リポジトリ
        RepositoryInitializer.recreateRepository("nablarch/test/core/messaging/web/web-component-configuration-request-testing.xml");
        
        Map<String, Object> reqrec = createTestRecord();
        
        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);

        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testAssertAsDataRecord", "1", "case1", "RM11AD0201");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        SyncMessage reply = client.sendSync(settings, request);
        assertEquals("200", reply.getHeaderRecord().get("STATUS_CODE"));
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec = reply.getDataRecord();
        assertEquals("00000000000000000112", resrec.get("userInfoId"));
        assertEquals("0", resrec.get("dataKbn"));
        assertEquals("200", resrec.get("_nbctlhdr.statusCode"));

        RequestTestingMessagingClient.assertSendingMessage(getClass(), "testAssertAsDataRecord", "1", "case1");
        
        // リポジトリを元に戻す
        RepositoryInitializer.recreateRepository("nablarch/test/core/messaging/web/web-component-configuration-request-testing.xml");
    }
    
    /**
     * 要求電文のアサートに失敗する場合のテストです。
     * ・期待電文より送信電文が長い場合
     */
    @Test
    public void testAssertFailLargeBody() throws Exception {
        Map<String, Object> reqrec = createTestRecord();
        
        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);

        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testAssertFailLargeBody", "1", "case1", "RM11AD0201");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        SyncMessage reply = client.sendSync(settings, request);
        assertEquals("200", reply.getHeaderRecord().get("STATUS_CODE"));
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec = reply.getDataRecord();
        assertEquals("00000000000000000112", resrec.get("userInfoId"));
        assertEquals("0", resrec.get("dataKbn"));
        assertEquals("200", resrec.get("_nbctlhdr.statusCode"));

        try {
            RequestTestingMessagingClient.assertSendingMessage(getClass(), "testAssertFailLargeBody", "1", "case1");
            fail("例外が発生する");
        } catch (AssertionError e) {
            assertTrue(e.getMessage(), e.getMessage().contains("body message string assertion failed."));
        }
    }
    
    /**
     * 要求電文のアサートに失敗する場合のテストです。
     * ・期待電文より送信電文が短い場合
     */
    @Test
    public void testAssertFailShortBody() throws Exception {
        Map<String, Object> reqrec = createTestRecord();
        
        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);

        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testAssertFailShortBody", "1", "case1", "RM11AD0201");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        SyncMessage reply = client.sendSync(settings, request);
        assertEquals("200", reply.getHeaderRecord().get("STATUS_CODE"));
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec = reply.getDataRecord();
        assertEquals("00000000000000000112", resrec.get("userInfoId"));
        assertEquals("0", resrec.get("dataKbn"));
        assertEquals("200", resrec.get("_nbctlhdr.statusCode"));

        try {
            RequestTestingMessagingClient.assertSendingMessage(getClass(), "testAssertFailShortBody", "1", "case1");
            fail("例外が発生する");
        } catch (AssertionError e) {
            assertTrue(e.getMessage(), e.getMessage().contains("body message string assertion failed."));
        }
    }
    
    /**
     * 要求電文のアサートに失敗する場合のテストです。
     * ・期待電文と送信電文の長さが同じで内容が異なる場合
     */
    @Test
    public void testAssertFailDifferentBody() throws Exception {
        Map<String, Object> reqrec = createTestRecord();
        reqrec.put("kanjiName", "漢字 名２"); // kanjiNameの値を「漢字 名２」に置き換え(テストデータは「漢字 名１」)
        
        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);

        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testAssertFailDifferentBody", "1", "case1", "RM11AD0201");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        SyncMessage reply = client.sendSync(settings, request);
        assertEquals("200", reply.getHeaderRecord().get("STATUS_CODE"));
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec = reply.getDataRecord();
        assertEquals("00000000000000000112", resrec.get("userInfoId"));
        assertEquals("0", resrec.get("dataKbn"));
        assertEquals("200", resrec.get("_nbctlhdr.statusCode"));

        try {
            RequestTestingMessagingClient.assertSendingMessage(getClass(), "testAssertFailDifferentBody", "1", "case1");
            fail("例外が発生する");
        } catch (AssertionError e) {
            assertTrue(e.getMessage(), e.getMessage().contains("body message string assertion failed."));
        }
    }
    
    /** 
     * 要求データの変換に失敗する場合のテストです。
     */
    @Test
    public void testInvalidRequest() throws Exception {
        Map<String, Object> reqrec = createTestRecord();
        reqrec.remove("loginId");
        
        SyncMessage request = new SyncMessage("RM11AD0201");
        request.addDataRecord(reqrec);
        MessageSenderSettings settings = new MessageSenderSettings("RM11AD0201");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);

        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testInvalidRequest", "1", "case1", "RM11AD0201");
        
        LogVerifier.setExpectedLogMessages(expectedLog);
        final RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        final SyncMessage reply = client.sendSync(settings, request);
        assertEquals("200", reply.getHeaderRecord().get("STATUS_CODE"));
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec = reply.getDataRecord();
        assertEquals("00000000000000000112", resrec.get("userInfoId"));
        assertEquals("0", resrec.get("dataKbn"));
        assertEquals("200", resrec.get("_nbctlhdr.statusCode"));
        
        try {
            RequestTestingMessagingClient.assertSendingMessage(getClass(), "testInvalidRequest", "1", "case1");
            fail("例外が発生する");
        } catch (MessagingException e) {
            assertTrue(e.getMessage().contains("Sending message when converting RM11AD0201, format error occurs."));
        }
    }
    

    /**
     * 異なるリクエストIDのメッセージを送信する場合のテストです。
     */
    @Test
    public void testSendDifferentRequestIds() throws Exception {
        Map<String, Object> reqrec = createTestRecord();
        
        SyncMessage request1 = new SyncMessage("RM11AD0201");
        request1.addDataRecord(reqrec);
        MessageSenderSettings settings1 = new MessageSenderSettings("RM11AD0201");
        
        SyncMessage request2 = new SyncMessage("RM11AD0201_02");
        request2.addDataRecord(reqrec);
        MessageSenderSettings settings2 = new MessageSenderSettings("RM11AD0201_02");
        
        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request1), utf8Charset));
        logInfo.put("message2", MessagingLogUtil.getHttpSentMessageLog(getSendingMessage(request2), utf8Charset));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);
        
        RequestTestingMessagingClient.clearSendingMessageCache();
        RequestTestingMessagingClient.initializeForRequestUnitTesting(getClass(), "testSendDifferentRequestIds", "1", "case1", "RM11AD0201");
        
        RequestTestingMessagingClient client = new RequestTestingMessagingClient();
        client.setCharset(utf8Charset);
        LogVerifier.setExpectedLogMessages(expectedLog);
        SyncMessage reply1 = client.sendSync(settings1, request1);
        assertEquals("200", reply1.getHeaderRecord().get("STATUS_CODE"));
        SyncMessage reply2 = client.sendSync(settings2, request2);
        assertEquals("200", reply2.getHeaderRecord().get("STATUS_CODE"));
        LogVerifier.verify("Failed!");
        
        Map<String, Object> resrec1 = reply1.getDataRecord();
        assertEquals("00000000000000000112", resrec1.get("userInfoId"));
        assertEquals("0", resrec1.get("dataKbn"));
        assertEquals("200", resrec1.get("_nbctlhdr.statusCode"));
        
        Map<String, Object> resrec2 = reply2.getDataRecord();
        assertEquals("00000000000000000112", resrec2.get("userInfoId"));
        assertEquals("0", resrec2.get("dataKbn"));
        assertEquals("200", resrec2.get("_nbctlhdr.statusCode"));
        
        RequestTestingMessagingClient.assertSendingMessage(getClass(), "testSendDifferentRequestIds", "1", "case1");
    }
    
}
