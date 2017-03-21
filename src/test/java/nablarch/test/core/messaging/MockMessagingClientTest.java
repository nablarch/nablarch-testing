package nablarch.test.core.messaging;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import test.support.SystemRepositoryResource;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.core.dataformat.RecordDefinition;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.messaging.MessageSender;
import nablarch.fw.messaging.SyncMessage;
import nablarch.fw.messaging.SyncMessagingEventHook;
import nablarch.fw.messaging.realtime.http.client.HttpMessagingClient;
import nablarch.fw.messaging.realtime.http.exception.HttpMessagingTimeoutException;
import nablarch.test.core.file.TestDataConverter;
import nablarch.test.support.log.app.OnMemoryLogWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


/**
 * MockMessagingClientTestのテスト。
 * @author Masaya Seko
 */
public class MockMessagingClientTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
            "nablarch/test/core/messaging/web/web-component-configuration.xml");

    /** 初期化を行う */
    @Before
    public void setUp() throws Exception {
        OnMemoryLogWriter.clear();
    }

    /** 終了処理を行う。*/
    @After
    public void tearDown() throws Exception {
        OnMemoryLogWriter.clear();
        SystemRepository.clear();
    }

    /**
     * 正常系のテスト(UTF-8)。
     */
    @Test
    public void testNormal() {
        Map<String, Object> dataRecord = new HashMap<String, Object>();
        SyncMessage responseMessage = null;
        
        dataRecord.put("dataKbn", "1");
        dataRecord.put("loginId", "user01");
        dataRecord.put("kanjiName", "ナブラ太郎");
        dataRecord.put("kanaName", "ナブラタロウ");
        dataRecord.put("mailAddress", "a@a.com");
        dataRecord.put("extensionNumberBuilding", "2");
        dataRecord.put("extensionNumberPersonal", "3");
        
        responseMessage = MessageSender.sendSync(new SyncMessage("RM11AC0202").addDataRecord(dataRecord));
        Map<String, Object> responseDataRecord = responseMessage.getDataRecord();
        //文字化けせずに取得できていることを確認
        assertThat((String)responseDataRecord.get("userInfoId"), is("あ"));
        assertTrue(responseMessage.getRequestId().contains("RM11AC0202"));
        // ログが文字化けしていないこと。
        OnMemoryLogWriter.assertLogContains("writer.memlog",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><request><dataKbn>1</dataKbn>"
                        + "<loginId>user01</loginId><kanjiName>ナブラ太郎</kanjiName><kanaName>ナブラタロウ</kanaName>"
                        + "<mailAddress>a@a.com</mailAddress><extensionNumberBuilding>2</extensionNumberBuilding>"
                        + "<extensionNumberPersonal>3</extensionNumberPersonal></request>");
    }

    
    /**
     * Excelファイルに複数の応答電文が定義されている場合の正常系のテスト。
     */
    @Test
    public void testNormalMultipleRow() {
        String requestId = "RM11AC0203";
        Map<String, Object> dataRecord = null;
        SyncMessage responseMessage = null;
        Map<String, Object> responseDataRecord = null;

        dataRecord = new HashMap<String, Object>();
        dataRecord.put("dataKbn", "1");
        dataRecord.put("loginId", "user01");
        dataRecord.put("kanjiName", "ナブラ太郎");
        dataRecord.put("kanaName", "ナブラタロウ");
        dataRecord.put("mailAddress", "a@a.com");
        dataRecord.put("extensionNumberBuilding", "2");
        dataRecord.put("extensionNumberPersonal", "3");
        responseMessage = MessageSender.sendSync(new SyncMessage(requestId).addDataRecord(dataRecord));
        responseDataRecord = responseMessage.getDataRecord();
        assertThat((String)responseDataRecord.get("userInfoId"), is("00000000000000000112"));

        dataRecord = new HashMap<String, Object>();
        dataRecord.put("dataKbn", "1");
        dataRecord.put("loginId", "user01");
        dataRecord.put("kanjiName", "ナブラ次郎");
        dataRecord.put("kanaName", "ナブラジロウ");
        dataRecord.put("mailAddress", "b@b.com");
        dataRecord.put("extensionNumberBuilding", "2");
        dataRecord.put("extensionNumberPersonal", "3");
        responseMessage = MessageSender.sendSync(new SyncMessage(requestId).addDataRecord(dataRecord));
        responseDataRecord = responseMessage.getDataRecord();
        assertThat((String)responseDataRecord.get("userInfoId"), is("00000000000000000113"));
        // ログが文字化けしていないこと。
        OnMemoryLogWriter.assertLogContains("writer.memlog",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><request><dataKbn>1</dataKbn>"
                        + "<loginId>user01</loginId><kanjiName>ナブラ太郎</kanjiName><kanaName>ナブラタロウ</kanaName>"
                        + "<mailAddress>a@a.com</mailAddress><extensionNumberBuilding>2</extensionNumberBuilding>"
                        + "<extensionNumberPersonal>3</extensionNumberPersonal></request>",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><request><dataKbn>1</dataKbn>"
                        + "<loginId>user01</loginId><kanjiName>ナブラ次郎</kanjiName><kanaName>ナブラジロウ</kanaName>"
                        + "<mailAddress>b@b.com</mailAddress><extensionNumberBuilding>2</extensionNumberBuilding>"
                        + "<extensionNumberPersonal>3</extensionNumberPersonal></request>");
    }

    /**
     * テストデータが見つからない場合のテスト。
     */
    @Test
    public void testNotFoundTestData() {
        Map<String, Object> dataRecord = new HashMap<String, Object>();
        dataRecord.put("dataKbn", "1");
        dataRecord.put("loginId", "user01");
        dataRecord.put("kanjiName", "ナブラ太郎");
        dataRecord.put("kanaName", "ナブラタロウ");
        dataRecord.put("mailAddress", "a@a.com");
        dataRecord.put("extensionNumberBuilding", "2");
        dataRecord.put("extensionNumberPersonal", "3");
        try {
            MessageSender.sendSync(new SyncMessage("RM11AC0291").addDataRecord(dataRecord));
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("test data file was not found. "));
            assertTrue(e.getMessage(), e.getMessage().contains("resource name=[RM11AC0291/message]"));
        }
    }

    /**
     * EXPECTED_REQUEST_BODY_MESSAGESが見つからない場合のテスト。
     */
    @Test
    public void testNotFoundExpectedRequestBodyMessages() {
        Map<String, Object> dataRecord = new HashMap<String, Object>();
        dataRecord.put("dataKbn", "1");
        dataRecord.put("loginId", "user01");
        dataRecord.put("kanjiName", "ナブラ太郎");
        dataRecord.put("kanaName", "ナブラタロウ");
        dataRecord.put("mailAddress", "a@a.com");
        dataRecord.put("extensionNumberBuilding", "2");
        dataRecord.put("extensionNumberPersonal", "3");
        try {
            MessageSender.sendSync(new SyncMessage("RM11AC0292").addDataRecord(dataRecord));
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("message was not found. message must be setting. " +
                    "data type=[RESPONSE_BODY_MESSAGES], request id=[RM11AC0292],"));
            assertTrue(e.getMessage().contains("resource name=[RM11AC0292/message]."));
        }
    }
    
    /**
     * ボディのフォーマットが不正な場合。
     */
    @Test
    public void invalidBodyFormat() {
        Map<String, Object> dataRecord = new HashMap<String, Object>();
        dataRecord.put("dataKbn", "1");
        dataRecord.put("loginId", "user01");
        dataRecord.put("kanjiName", "ナブラ太郎");
        dataRecord.put("kanaName", "ナブラタロウ");
        dataRecord.put("mailAddress", "a@a.com");
        dataRecord.put("extensionNumberBuilding", "2");
        dataRecord.put("extensionNumberPersonal", "3");

        try {
            MessageSender.sendSync(new SyncMessage("RM11AC0293").addDataRecord(dataRecord));
            fail();
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Invalid receive message format. requestId=[RM11AC0293]. URL=[http://localhost:8888/msgaction/ss11AC/RM11AC0299]."));
        }
    }


    /**
     * タイムアウトのテスト。
     */
    @Test
    public void testTimeout() {
        Map<String, Object> dataRecord = new HashMap<String, Object>();
        
        dataRecord.put("dataKbn", "1");
        dataRecord.put("loginId", "user01");
        dataRecord.put("kanjiName", "ナブラ太郎");
        dataRecord.put("kanaName", "ナブラタロウ");
        dataRecord.put("mailAddress", "a@a.com");
        dataRecord.put("extensionNumberBuilding", "2");
        dataRecord.put("extensionNumberPersonal", "3");

        try{
            MessageSender.sendSync(new SyncMessage("RM11AC0294").addDataRecord(dataRecord));
        }catch(Exception e){
            assertThat(e, is(instanceOf(HttpMessagingTimeoutException.class)));
        }
        // 通常ログとエラーログの2回出力されていること。
        assertLogWithCount(createMessagePattern(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><request><dataKbn>1</dataKbn>"
                        + "<loginId>user01</loginId><kanjiName>ナブラ太郎</kanjiName><kanaName>ナブラタロウ</kanaName>"
                        + "<mailAddress>a@a.com</mailAddress><extensionNumberBuilding>2</extensionNumberBuilding>"
                        + "<extensionNumberPersonal>3</extensionNumberPersonal></request>"),
                2);
        //エラーログが文字化けしていないこと。
        assertLogWithCount(createMessagePattern("response timeout: could not receive a reply to the message.",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><request><dataKbn>1</dataKbn>"
                        + "<loginId>user01</loginId><kanjiName>ナブラ太郎</kanjiName><kanaName>ナブラタロウ</kanaName>"
                        + "<mailAddress>a@a.com</mailAddress><extensionNumberBuilding>2</extensionNumberBuilding>"
                        + "<extensionNumberPersonal>3</extensionNumberPersonal></request>"),
                1);
    }


    /**
     * 正常系のテスト(SJIS)。
     */
    @Test
    public void testNormalSJIS() {

        //Shift-JISで設定を読み込む。
        SystemRepository.clear();
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader("nablarch/test/core/messaging/web/web" +
                "-component-configuration-sjis.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);


        Map<String, Object> dataRecord = new HashMap<String, Object>();
        SyncMessage responseMessage = null;
        
        dataRecord.put("dataKbn", "1");
        dataRecord.put("loginId", "user01");
        dataRecord.put("kanjiName", "ナブラ太郎");
        dataRecord.put("kanaName", "ナブラタロウ");
        dataRecord.put("mailAddress", "a@a.com");
        dataRecord.put("extensionNumberBuilding", "2");
        dataRecord.put("extensionNumberPersonal", "3");
        
        responseMessage = MessageSender.sendSync(new SyncMessage("RM11AC0295").addDataRecord(dataRecord));
        Map<String, Object> responseDataRecord = responseMessage.getDataRecord();
        //文字化けせずに取得できていることを確認
        assertThat((String)responseDataRecord.get("userInfoId"), is("あ"));
        assertTrue(responseMessage.getRequestId().contains("RM11AC0295"));
        // ログがShift-JISでも文字化けしていないこと。
        OnMemoryLogWriter.assertLogContains("writer.memlog",
                "<?xml version=\"1.0\" encoding=\"Shift_JIS\"?><request><dataKbn>1</dataKbn>"
                        + "<loginId>user01</loginId><kanjiName>ナブラ太郎</kanjiName><kanaName>ナブラタロウ</kanaName>"
                        + "<mailAddress>a@a.com</mailAddress><extensionNumberBuilding>2</extensionNumberBuilding>"
                        + "<extensionNumberPersonal>3</extensionNumberPersonal></request>");
   }

    /**
     * 正常系のテスト(Charset指定なし)。
     */
    @Test
    public void testNormalNoCharset() throws Exception {

        //Charset指定なしで設定を読み込む。
        SystemRepository.clear();
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader("nablarch/test/core/messaging/web/web" +
                "-component-configuration-nocharset.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);


        Map<String, Object> dataRecord = new HashMap<String, Object>();
        SyncMessage responseMessage = null;

        dataRecord.put("dataKbn", "1");
        dataRecord.put("loginId", "user01");
        dataRecord.put("kanjiName", "ナブラ太郎");
        dataRecord.put("kanaName", "ナブラタロウ");
        dataRecord.put("mailAddress", "a@a.com");
        dataRecord.put("extensionNumberBuilding", "2");
        dataRecord.put("extensionNumberPersonal", "3");

        responseMessage = MessageSender.sendSync(new SyncMessage("RM11AC0298").addDataRecord(dataRecord));
        Map<String, Object> responseDataRecord = responseMessage.getDataRecord();
        //文字化けせずに取得できていることを確認
        assertThat((String)responseDataRecord.get("userInfoId"), is("あ"));
        assertTrue(responseMessage.getRequestId().contains("RM11AC0298"));
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><request><dataKbn>1</dataKbn>"
                + "<loginId>user01</loginId><kanjiName>ナブラ太郎</kanjiName><kanaName>ナブラタロウ</kanaName>"
                + "<mailAddress>a@a.com</mailAddress><extensionNumberBuilding>2</extensionNumberBuilding>"
                + "<extensionNumberPersonal>3</extensionNumberPersonal></request>";
        //Charset指定なしのログがデフォルトでエンコードされることを確認
        String body =  new String(expected.getBytes(Charset.defaultCharset()));
        assertLogWithCount(createMessagePattern(body), 1);
    }

    /**
     * 正常系のテスト(テストデータコンバータで文字コードを設定)。<br>
     * <p>
     * テストデータコンバータの文字コードと、フォーマット定義の文字コードを一致させた状態で読めることを確認する。
     * </p>
     */
    @Test
    public void testNormalWithTestDataConverter() {
        Map<String, Object> dataRecord = new HashMap<String, Object>();
        SyncMessage responseMessage = null;
        
        dataRecord.put("dataKbn", "1");
        dataRecord.put("loginId", "user01");
        dataRecord.put("kanjiName", "ナブラ太郎");
        dataRecord.put("kanaName", "ナブラタロウ");
        dataRecord.put("mailAddress", "a@a.com");
        dataRecord.put("extensionNumberBuilding", "2");
        dataRecord.put("extensionNumberPersonal", "3");
        
        responseMessage = MessageSender.sendSync(new SyncMessage("RM11AC0296").addDataRecord(dataRecord));
        Map<String, Object> responseDataRecord = responseMessage.getDataRecord();
        //文字化けせずに取得できていることを確認
        assertThat((String)responseDataRecord.get("userInfoId"), is("あ"));
        assertTrue(responseMessage.getRequestId().contains("RM11AC0296"));
    }

    /**
     * 異常系のテスト(Excelに不正な文字コードが記載されている場合)。
     */
    @Test(expected=java.lang.IllegalStateException.class)
    public void testInvalidEncode() {
        Map<String, Object> dataRecord = new HashMap<String, Object>();
        
        dataRecord.put("dataKbn", "1");
        dataRecord.put("loginId", "user01");
        dataRecord.put("kanjiName", "ナブラ太郎");
        dataRecord.put("kanaName", "ナブラタロウ");
        dataRecord.put("mailAddress", "a@a.com");
        dataRecord.put("extensionNumberBuilding", "2");
        dataRecord.put("extensionNumberPersonal", "3");
        
        MessageSender.sendSync(new SyncMessage("RM11AC0297").addDataRecord(dataRecord));
    }

    /**
     * ヘッダが指定されていない場合のテスト。
     */
    @Test
    public void testNotFoundExpectedRequestHeaderMessages() {
        Map<String, Object> dataRecord = new HashMap<String, Object>();
        SyncMessage responseMessage = null;
        
        dataRecord.put("dataKbn", "1");
        dataRecord.put("loginId", "user01");
        dataRecord.put("kanjiName", "ナブラ太郎");
        dataRecord.put("kanaName", "ナブラタロウ");
        dataRecord.put("mailAddress", "a@a.com");
        dataRecord.put("extensionNumberBuilding", "2");
        dataRecord.put("extensionNumberPersonal", "3");
        
        responseMessage = MessageSender.sendSync(new SyncMessage("RM11AC0204").addDataRecord(dataRecord));
        assertTrue(responseMessage.getRequestId().contains("RM11AC0204"));
        
        // ヘッダが指定されていない場合、ステータスコードのみ設定されている。
        Map<String, Object> headerRecord = responseMessage.getHeaderRecord();
        String resultStatusCode = (String)headerRecord.get(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE);
        assertEquals(1, headerRecord.size());
        assertEquals("200", resultStatusCode);
    }

    /**
     * 任意のヘッダが登録されている場合のテスト。
     */
    @Test
    public void testNormalHeaderOfany() {
        Map<String, Object> dataRecord = new HashMap<String, Object>();
        SyncMessage responseMessage = null;
        
        dataRecord.put("dataKbn", "1");
        dataRecord.put("loginId", "user01");
        dataRecord.put("kanjiName", "ナブラ太郎");
        dataRecord.put("kanaName", "ナブラタロウ");
        dataRecord.put("mailAddress", "a@a.com");
        dataRecord.put("extensionNumberBuilding", "2");
        dataRecord.put("extensionNumberPersonal", "3");
        
        responseMessage = MessageSender.sendSync(new SyncMessage("RM11AC0205").addDataRecord(dataRecord));
        assertTrue(responseMessage.getRequestId().contains("RM11AC0205"));
        Map<String, Object> headerRecord = responseMessage.getHeaderRecord();
        assertThat((String)headerRecord.get("X-Message-Id"), is("00001"));
    }
    

    /**
     * 、応答の本文がフォーマット定義で変換できない場合、configファイルに適切に実装された{@link SyncMessagingEventHook}が設定されていれば、ハンドリングできることを確認する。
     */
    @Test
    public void testInvalidFormatWith404() {
        Map<String, Object> dataRecord = new HashMap<String, Object>();
        SyncMessage responseMessage = null;
        
        dataRecord.put("dataKbn", "1");
        dataRecord.put("loginId", "user01");
        dataRecord.put("kanjiName", "ナブラ太郎");
        dataRecord.put("kanaName", "ナブラタロウ");
        dataRecord.put("mailAddress", "a@a.com");
        dataRecord.put("extensionNumberBuilding", "2");
        dataRecord.put("extensionNumberPersonal", "3");
        
        responseMessage = MessageSender.sendSync(new SyncMessage("RM11AC0206").addDataRecord(dataRecord));
        assertTrue(responseMessage.getRequestId().contains("RM11AC0206"));
        Map<String, Object> headerRecord = responseMessage.getHeaderRecord();
        assertThat((String)headerRecord.get(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE), is("404"));
    }

    /**
     * 応答の本文がフォーマット定義で変換できない場合かつ、HTTPステータスコードを取得できない場合は、{@link SyncMessagingEventHook}を実装したクラス内でステータスコード0で取り出せることを確認する。
     */
    @Test
    public void testInvalidFormat() {
        Map<String, Object> dataRecord = new HashMap<String, Object>();
        SyncMessage responseMessage = null;
        
        dataRecord.put("dataKbn", "1");
        dataRecord.put("loginId", "user01");
        dataRecord.put("kanjiName", "ナブラ太郎");
        dataRecord.put("kanaName", "ナブラタロウ");
        dataRecord.put("mailAddress", "a@a.com");
        dataRecord.put("extensionNumberBuilding", "2");
        dataRecord.put("extensionNumberPersonal", "3");
        
        responseMessage = MessageSender.sendSync(new SyncMessage("RM11AC0207").addDataRecord(dataRecord));
        assertTrue(responseMessage.getRequestId().contains("RM11AC0207"));
        Map<String, Object> headerRecord = responseMessage.getHeaderRecord();
        assertThat((String)headerRecord.get(HttpMessagingClient.SYNCMESSAGE_STATUS_CODE), is("0"));
    }

    /**
     * テストデータコンバータを使用した場合のテスト。
     */
    @Test
    public void testSendWithTestDataConverter() {
        DataRecord dataRecord = new DataRecord();
        SyncMessage reply =
            MessageSender
                .sendSync(new SyncMessage("RM11AC0208")
                        .addDataRecord(dataRecord));
        
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("failureCode", "HOGE");
        expected.put("userInfoId", "HOGE");
        expected.put("test", "HOGE");
        
        Map<String, Object> actual = reply.getDataRecord();
        
        assertEquals(expected, actual);
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
     * TestDataConverterで文字コードを変更するテストで使用するコンバータ
     */
    public static class Utf8EncodingConvertor implements TestDataConverter {

        @Override
        public LayoutDefinition createDefinition(
                LayoutDefinition defaultDefinition, DataRecord currentData,
                Charset encoding) {
            
            RecordDefinition rd = new RecordDefinition();
            RecordDefinition orgRd = defaultDefinition.getRecords().get(0);
            rd.setTypeName(orgRd.getTypeName());
            int position = 1;
            //フィールド定義
            for (FieldDefinition orgFd : orgRd.getFields()) {
                String string = currentData.get(orgFd.getName()).toString();
                int length = string.getBytes(Charset.forName("utf-8")).length;
                FieldDefinition fd = new FieldDefinition().setName(orgFd.getName()).setPosition(position);
                fd.addConvertorSetting("X", new Object[]{length});
                rd.addField(fd);
                position += length;
            }

            //レイアウト定義を修正
            LayoutDefinition ld = new LayoutDefinition();
            ld.getDirective().putAll(defaultDefinition.getDirective());
            ld.getDirective().put("text-encoding", "utf-8");
            ld.getDirective().put("record-length", position-1);
            ld.addRecord(rd);
            
            return ld;
        }

        @Override
        public DataRecord convertData(LayoutDefinition definition,
                DataRecord currentData, Charset encoding) {
            //特に何もしない
            return currentData;
        }
    }

    /**
     * 正規表現で指定したパターンが指定回数ログに出力されていることを確認する。
     * @param messagePattern テストしたい正規表現のパターン
     * @param expectedCount 期待する回数
     */
    public void assertLogWithCount(String messagePattern, int expectedCount)
    {
        List<String> log = OnMemoryLogWriter.getMessages("writer.memlog");
        System.out.println("log = " + log);
        String msg = messagePattern.replaceAll("\\r|\\n", "");
        Pattern pattern = Pattern.compile(msg);
        int logCount = 0;
        for (String logMessage : log) {
            String str = logMessage.replaceAll("\\r|\\n", "");
            Matcher matcher = pattern.matcher(str);
            if (matcher.matches()) {
                logCount++;
            }
        }
        assertThat( String.format("ログが指定回数だけ出力されていること messagePattern=[%s]",messagePattern), logCount, is(expectedCount));
    }

    /**
     * 引数の配列を順番通りに含む正規表現を生成する。
     * @param args 含めたい文字列の配列
     * @return 引数すべてを含む正規表現
     */
    public String createMessagePattern(final String... args) {
        StringBuilder sb = new StringBuilder();
        sb.append(".*");
        for (String arg: args) {
            sb.append(Pattern.quote(arg));
            sb.append(".*");
        }
        return sb.toString();
    }
}
