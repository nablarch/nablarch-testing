package nablarch.test.core.messaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.messaging.MessageSendSyncTimeoutException;
import nablarch.fw.messaging.MessageSender;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.messaging.SendingMessage;
import nablarch.fw.messaging.SyncMessage;
import nablarch.fw.messaging.logging.MessagingLogUtil;
import nablarch.test.core.log.LogVerifier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * DealUnitMockMessagingContextの異常系テスト。
 * @author Masato Inoue
 */
public class MockMessagingContextTest {

    /** 初期化を行う */
    @BeforeClass
    public static void loadRepository() {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader("nablarch/test/core/messaging/web/web" +
                                                                                       "-component-configuration.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
    }

    /**
     * テストデータが見つからない場合のテスト。
     */
    @Test
    public void test1() {
        DataRecord dataRecord = new DataRecord();
        try {
            MessageSender
                    .sendSync(new SyncMessage("RM11AD0103")
                            .addDataRecord(dataRecord));
            fail();
        
        }
        catch (IllegalStateException e) {
            
            assertTrue(e.getMessage().contains("test data file was not found. "));
            assertTrue(e.getMessage(), e.getMessage().contains("resource name=[RM11AD0103/message]"));
        }     
    }
    

    /**
     * EXPECTED_REQUEST_HEADER_MESSAGESが見つからない場合のテスト。
     */
    @Test
    public void test2() {
        DataRecord dataRecord = new DataRecord();
        try {
            MessageSender
                    .sendSync(new SyncMessage("RM11AD0104")
                            .addDataRecord(dataRecord));
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("message was not found. message must be setting. " +
                    "data type=[EXPECTED_REQUEST_HEADER_MESSAGES], request id=[RM11AD0104],"));
            assertTrue(e.getMessage().contains("resource name=[RM11AD0104/message]."));
        }     
    }
    
    /**
     * EXPECTED_REQUEST_BODY_MESSAGESが見つからない場合のテスト。
     */
    @Test
    public void test3() {
        DataRecord dataRecord = new DataRecord();
        try {
            MessageSender
                    .sendSync(new SyncMessage("RM11AD0105")
                            .addDataRecord(dataRecord));
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("message was not found. message must be setting. " +
                    "data type=[EXPECTED_REQUEST_BODY_MESSAGES], request id=[RM11AD0105],"));
            assertTrue(e.getMessage().contains("resource name=[RM11AD0105/message]."));
        }     
    }
    
    
    /**
     * RESPONSE_HEADER_MESSAGESが見つからない場合のテスト。
     */
    @Test
    public void test4() {
        DataRecord dataRecord = new DataRecord();
        try {
            MessageSender
                    .sendSync(new SyncMessage("RM11AD0106")
                            .addDataRecord(dataRecord));
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("message was not found. message must be setting. " +
                    "data type=[RESPONSE_HEADER_MESSAGES], request id=[RM11AD0106],"));
            assertTrue(e.getMessage().contains("resource name=[RM11AD0106/message]."));
        }      
    }
    
    /**
     * RESPONSE_BODY_MESSAGESが見つからない場合のテスト。
     */
    @Test
    public void test5() {
        DataRecord dataRecord = new DataRecord();
        try {
            MessageSender
                    .sendSync(new SyncMessage("RM11AD0107")
                            .addDataRecord(dataRecord));
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("message was not found. message must be setting. " +
                    "data type=[RESPONSE_BODY_MESSAGES], request id=[RM11AD0107],"));
            assertTrue(e.getMessage().contains("resource name=[RM11AD0107/message]."));
        }
    }

    /**
     * Excelの応答電文の数を上回るリクエストがやってきた場合のテスト。（noの2で落ちたことが分かる）
     */
    @Test
    public void test6() throws Exception {
        
        FilePathSetting filePathSetting = FilePathSetting.getInstance();
        File file = filePathSetting.getFileIfExists(SendSyncSupport.SEND_SYNC_TEST_DATA_BASE_PATH, "RM11AD0108");
        if(file == null) {
            File fileCopy = filePathSetting.getFileIfExists(SendSyncSupport.SEND_SYNC_TEST_DATA_BASE_PATH, "RM11AD0108_timestamp");
            FileInputStream fis = new FileInputStream(fileCopy);
            FileChannel channel = fis.getChannel();
            FileOutputStream fos = new FileOutputStream(filePathSetting.getBaseDirectory(SendSyncSupport.SEND_SYNC_TEST_DATA_BASE_PATH).getAbsolutePath() + "/RM11AD0108.xls");
            FileChannel ofc = fos.getChannel();
            channel.transferTo(0, channel.size(), ofc);
        }
        
        DataRecord dataRecord = new DataRecord();
        SyncMessage sendSync = MessageSender.sendSync(new SyncMessage("RM11AD0108")
        .addDataRecord(dataRecord));
        assertEquals("test2", sendSync.getDataRecord().get("failureCode"));
        try {
            MessageSender.sendSync(new SyncMessage("RM11AD0108")
            .addDataRecord(dataRecord));
            fail();
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("receive message did not exists. data type=[RESPONSE_HEADER_MESSAGES], " +
                    "request id=[RM11AD0108], no=[2], "));
            assertTrue(e.getMessage().contains("resource name=[RM11AD0108/message]."));
        }
        try {
            MessageSender.sendSync(new SyncMessage("RM11AD0108")
            .addDataRecord(dataRecord));
            fail();
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("receive message did not exists. data type=[RESPONSE_HEADER_MESSAGES], " +
                    "request id=[RM11AD0108], no=[3], "));
            assertTrue(e.getMessage().contains("resource name=[RM11AD0108/message]."));
        }
        
        // ファイルのタイムスタンプを書き換える
        assertTrue(file.delete());
        File fileCopy = filePathSetting.getFileIfExists(SendSyncSupport.SEND_SYNC_TEST_DATA_BASE_PATH, "RM11AD0108_timestamp");
        FileInputStream fis = new FileInputStream(fileCopy);
        FileChannel channel = fis.getChannel();
        FileOutputStream fos = new FileOutputStream(file);
        FileChannel ofc = fos.getChannel();
        channel.transferTo(0, channel.size(), ofc);
        
        // 例外が発生せず、一件目から値の取得ができる
        sendSync = MessageSender.sendSync(new SyncMessage("RM11AD0108")
        .addDataRecord(dataRecord));
        assertEquals("test2", sendSync.getDataRecord().get("failureCode"));
        
    }
    
    /**
     * 障害系のテスト。
     */
    @Test
    public void testFailure() {
        DataRecord dataRecord = new DataRecord();

        // タイムアウト
        try {
            MessageSender.sendSync(new SyncMessage(
                    "RM11AD0110").addDataRecord(dataRecord));
            fail();
        } catch (MessageSendSyncTimeoutException e) {
            assertTrue(true);
        }


        // MessagingException
        try {
            MessageSender.sendSync(new SyncMessage("RM11AD0110")
                    .addDataRecord(dataRecord));
            fail();
        } catch (MessagingException e) {
            assertEquals(e.getMessage(), "message exception was happened! this exception was thrown by mock.");
        }     
    }

    /**
     * ヘッダのフォーマットが不正な場合。
     */
    @Test
    public void testInvalidHeaderFormat() {
        DataRecord dataRecord = new DataRecord();

        try {
            MessageSender.sendSync(new SyncMessage(
                    "RM11AD0111").addDataRecord(dataRecord));
            fail();
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof InvalidDataFormatException);
            assertTrue(e.getMessage().contains("failed to parse message header. " +
            		"format of the header did not match the expected format. " +
            		"data type=[EXPECTED_REQUEST_HEADER_MESSAGES], " +
            		"request id=[RM11AD0111], no=[1], "));
            assertTrue(e.getMessage().contains("resource name=[RM11AD0111/message]."));
        }
    }
    /**
     * ボディのフォーマットが不正な場合。
     */
    @Test
    public void testInvalidBodyFormat() {
        DataRecord dataRecord = new DataRecord();

        try {
            MessageSender.sendSync(new SyncMessage(
                    "RM11AD0112").addDataRecord(dataRecord));
            fail();
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof InvalidDataFormatException);
            assertTrue(e.getMessage().contains("failed to parse message body. " +
                    "format of the header or body did not match the expected format. " +
                    "request id=[RM11AD0112], no=[1], "));
            assertTrue(e.getMessage().contains("resource name=[RM11AD0112/message]."));
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReceiveMessage() throws Exception {
        new MockMessagingContext().receiveMessage("dummyQueue", "dummyMessageId", 0);
    }

    @Test
    public void testSend() throws Exception {
        MockMessagingContext context = new MockMessagingContext();
        SendingMessage message = new SendingMessage();

        Map<String, String> logInfo = new HashMap<String, String>();
        logInfo.put("logLevel", "INFO");
        logInfo.put("message1", MessagingLogUtil.getSentMessageLog(message));
        List<Map<String,String>> expectedLog = Arrays.asList(logInfo);

        LogVerifier.setExpectedLogMessages(expectedLog);
        context.send(message);
        LogVerifier.verify("Failed!");

        LogVerifier.clear();

        LogVerifier.setExpectedLogMessages(expectedLog);
        context.sendMessage(message);
        LogVerifier.verify("Failed!");

        context.close();
    }

    /**
     * キャッシュを利用しないテスト
     */
    @Test
    public void test7() throws Exception {

        FilePathSetting filePathSetting = FilePathSetting.getInstance();
        File file = filePathSetting.getFileIfExists(SendSyncSupport.SEND_SYNC_TEST_DATA_BASE_PATH, "RM11AD0113");

        DataRecord dataRecord = new DataRecord();
        SyncMessage sendSync = MessageSender.sendSync(new SyncMessage("RM11AD0113")
                .addDataRecord(dataRecord));
        assertEquals("test2", sendSync.getDataRecord().get("failureCode"));

        // ファイルのタイムスタンプを書き換える
        assertTrue(file.delete());
        File fileCopy = filePathSetting.getFileIfExists(SendSyncSupport.SEND_SYNC_TEST_DATA_BASE_PATH, "RM11AD0113_timestamp");
        FileInputStream fis = new FileInputStream(fileCopy);
        FileChannel channel = fis.getChannel();
        FileOutputStream fos = new FileOutputStream(file);
        FileChannel ofc = fos.getChannel();
        channel.transferTo(0, channel.size(), ofc);

        // 最新のデータを取得できる
        SyncMessage sendSync2 = MessageSender.sendSync(new SyncMessage("RM11AD0113"));
        assertEquals("test4", sendSync2.getDataRecord().get("failureCode"));

    }

}
