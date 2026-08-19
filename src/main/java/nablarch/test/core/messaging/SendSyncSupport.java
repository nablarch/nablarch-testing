package nablarch.test.core.messaging;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.core.dataformat.RecordDefinition;
import nablarch.core.dataformat.VariableLengthDataRecordFormatter;
import nablarch.core.dataformat.convertor.datatype.CharacterStreamDataString;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.FilePathSetting;
import nablarch.core.util.annotation.Published;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.test.core.reader.BasicTestDataParser;
import nablarch.test.core.reader.DataType;
import nablarch.test.core.reader.SendSyncMessageParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * テストで必要なメッセージング操作をサポートするクラス。
 *
 * @author Masato Inoue
 */
@Published
public class SendSyncSupport {

    /** 要求電文の内容をMapの形式でログに出力するロガー */
    private static final Logger LOGGER_MAP = LoggerManager.get("MESSAGING_MAP");

    /** 要求電文の内容をCSVの形式でログに出力するロガー */
    private static final Logger LOGGER_CSV = LoggerManager.get("MESSAGING_CSV");
    
    /** レスポンスメッセージが記載されるシートの名前 */
    private static final String RESPONSE_MESSAGES_SHEET_NAME = "message";

    /** テストデータが格納されるディレクトリ名 */
    public static final String SEND_SYNC_TEST_DATA_BASE_PATH = "sendSyncTestData";
    
    /** Excel情報のキャッシュ */
    private static Map<String, TestDataInfo> fileCache = new HashMap<String, TestDataInfo>();
    
    
    /**
     * 要求電文のヘッダと本文をログに出力する。
     * @param requestId リクエストID
     * @param sendingMessage 送信メッセージ
     */
    public void parseRequestMessage(String requestId, SendingMessage sendingMessage) {

        // 要求電文を読み込むために使用するReceivedMessageを生成する
        ReceivedMessage requestMessage = new ReceivedMessage(
                sendingMessage.getBodyBytes());

        // 要求電文（ヘッダ）の内容を読み込む
        TestDataInfo headerInfo = createTestDataInfo(DataType.EXPECTED_REQUEST_HEADER_MESSAGES, requestId);
        ReceivedMessage headerMessage = requestMessage.setFormatter(
                headerInfo.messagePool.getFormatter());
        DataRecord headerRecord;
        try {
            headerRecord = headerMessage.readRecord();
        } catch (InvalidDataFormatException e) {
            throw new RuntimeException(String.format("failed to parse message header. "
                  + "format of the header did not match the expected format. data type=[%s], "
                    + "request id=[%s], no=[%s], path=[%s], resource name=[%s].", 
                    DataType.EXPECTED_REQUEST_HEADER_MESSAGES, requestId, 
                    headerInfo.no, headerInfo.basePath, headerInfo.resourceName), e);
        }
        
        // 要求電文（ボディ）の内容を読み込む
        TestDataInfo bodyInfo = createTestDataInfo(DataType.EXPECTED_REQUEST_BODY_MESSAGES, requestId);
        ReceivedMessage bodyMessage = requestMessage.setFormatter(
                bodyInfo.messagePool.getFormatter());
        
        List<DataRecord> bodyRecords;
        try {
            bodyRecords = bodyMessage.readRecords();
        } catch (InvalidDataFormatException e) {
            throw new RuntimeException(String.format("failed to parse message body. "
                  + "format of the header or body did not match the expected format. "
                    + "request id=[%s], no=[%s], path=[%s], resource name=[%s].", 
                    requestId, bodyInfo.no, bodyInfo.basePath, bodyInfo.resourceName), e);
        }
        
        outputRequestLog(requestId, headerRecord, bodyRecords);

    }

    /**
     * 要求電文のログ出力を行う。
     * @param requestId リクエストID
     * @param headerRecord 要求電文（ヘッダ）のデータレコード
     * @param bodyRecords 要求電文（本文）のデータレコード
     */
    protected void outputRequestLog(String requestId, DataRecord headerRecord, List<DataRecord> bodyRecords) {
        
        // 要求電文の内容をCSV形式でログに出力する
        outputRequestLogAsMap(requestId, headerRecord, bodyRecords);
        // 要求電文の内容をCSV形式でログに出力する
        outputRequestLogAsCsv(requestId, headerRecord, bodyRecords);
        
    }

    /** ログの改行コード */
    private static final String LOG_SEPARATOR = "\n";
    /** タブ */
    private static final String TAB = "\t";
    /** ログメッセージのヘッダ */
    private static final String LOG_MESSAGE_HEADER = 
        "request id=[%s]. following message has been sent: " + LOG_SEPARATOR;
    /** デフォルトのレコードタイプ */
    private static final String DEFAULT_RECORD_TYPE = "csv";

    /**
     * 要求電文のログをMap形式で出力する。
     * @param requestId リクエストID
     * @param headerRecord 要求電文（ヘッダ）のデータレコード
     * @param bodyRecords 要求電文（本文）のデータレコード
     */
    private void outputRequestLogAsMap(String requestId,
            DataRecord headerRecord, List<DataRecord>  bodyRecords) {
        if (LOGGER_MAP.isDebugEnabled()) {
            StringBuilder builder = new StringBuilder(String.format(
                    LOG_MESSAGE_HEADER,
                    requestId));
            // ヘッダ部
            // 要求電文の内容をMap形式でログに出力する
            builder.append(TAB).append("message fw header = ");
            builder.append(headerRecord).append(LOG_SEPARATOR);
            builder.append(TAB).append("message body      = ");

            for (int i = 0; i < bodyRecords.size(); i++) {
                if (i > 0) {
                    builder.append(LOG_SEPARATOR);
                }
                // 要求電文の内容をMAP形式でログに出力する
                builder.append(bodyRecords.get(i).toString());
            }
            LOGGER_MAP.logDebug(builder.toString());
        }
    }

    /**
     * 要求電文のログをCSV形式で出力する。
     * @param requestId リクエストID
     * @param headerRecord 要求電文（ヘッダ）のデータレコード
     * @param bodyRecords 要求電文（本文）のデータレコード
     */
    protected void outputRequestLogAsCsv(String requestId,
            DataRecord headerRecord, List<DataRecord> bodyRecords) {
        if (LOGGER_CSV.isDebugEnabled()) {

            LayoutDefinition definition = createLogLayout();
            RecordDefinition recordDefinition = new RecordDefinition();
            recordDefinition.setTypeName(DEFAULT_RECORD_TYPE);
            Map<String, String> fieldNameMap = createFieldNameMap(headerRecord,
                    recordDefinition);
            definition.addRecord(recordDefinition);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataRecordFormatter formatter;
            try {
                formatter = new VariableLengthDataRecordFormatter().setDefinition(definition).setOutputStream(outputStream).initialize();
            } catch (InvalidDataFormatException e) {
                throw new RuntimeException(e); // can not happen
            } 
            
            try {
                // ヘッダと本文をストリームに出力する
                formatter.writeRecord(DEFAULT_RECORD_TYPE, fieldNameMap);
                formatter.writeRecord(DEFAULT_RECORD_TYPE, headerRecord);
            } catch (IOException e) {
                throw new RuntimeException(e); // can not happen
            }
            formatter.close();
            
            StringBuilder builder = new StringBuilder(String.format(
                    LOG_MESSAGE_HEADER,
                    requestId));

            // ヘッダ部
            // 要求電文の内容をMap形式でログに出力する
            builder.append("message header = ").append(LOG_SEPARATOR);
            builder.append(new String(outputStream.toByteArray(), Charset.forName("UTF-8")));
            builder.append("message body   = ").append(LOG_SEPARATOR);

            outputStream = new ByteArrayOutputStream();
            
            for (DataRecord bodyRecord : bodyRecords) {
                
                Map<String, String> bodyFieldNameMap;
                
                recordDefinition = new RecordDefinition();
                recordDefinition.setTypeName(DEFAULT_RECORD_TYPE);
                
                bodyFieldNameMap = createFieldNameMap(bodyRecord, recordDefinition);

                definition = createLogLayout().addRecord(recordDefinition);  

                try {
                    formatter = formatter.setDefinition(definition)
                            .setOutputStream(outputStream).initialize();
                } catch (InvalidDataFormatException e) {
                    throw new RuntimeException(e); // can not happen
                }

                try {
                    // ヘッダと本文をストリームに出力する
                    formatter.writeRecord(DEFAULT_RECORD_TYPE, bodyFieldNameMap);
                    formatter.writeRecord(DEFAULT_RECORD_TYPE, bodyRecord);
                } catch (IOException e) {
                    throw new RuntimeException(e); // can not happen
                }
                
                // 要求電文の内容をMAP形式でログに出力する
                builder.append(new String(outputStream.toByteArray(), Charset.forName("UTF-8")));
            }


            LOGGER_CSV.logDebug(builder.toString());
            formatter.close();
        }
    }

    /**
     * フィールド名のMapを生成する。
     * @param record データレコード 
     * @param recordDefinition レコード定義情報保持クラス
     * @return フィールド名（CSVのタイトル行）のMap
     */
    private Map<String, String> createFieldNameMap(DataRecord record, 
            RecordDefinition recordDefinition) {
        int i = 1;
        Map<String, String> headerMap = new HashMap<String, String>();
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            FieldDefinition fieldDefinition = new FieldDefinition();
            fieldDefinition.setName(entry.getKey());
            fieldDefinition.setDataType(new CharacterStreamDataString());
            fieldDefinition.setPosition(i++);
            recordDefinition.addField(fieldDefinition);
            headerMap.put(entry.getKey(), entry.getKey());
        }
        return headerMap;
    }


    /**
     * CSV形式のログを出力する際のレイアウトを作成する。
     * @return フォーマット定義情報保持クラス
     */
    protected LayoutDefinition createLogLayout() {
        LayoutDefinition definition = new LayoutDefinition();
        definition.getDirective().put("file-type", "Variable");
        definition.getDirective().put("record-separator", "\n");
        definition.getDirective().put("field-separator", ",");
        definition.getDirective().put("quoting-delimiter", "\"");
        definition.getDirective().put("text-encoding", "UTF-8");
        return definition;       
    }

    /**
     * リクエストIDに紐付くメッセージのバイナリを取得する。
     * @param dataType データタイプ
     * @param requestId リクエストID
     * @return メッセージのリスト
     */
    public byte[] getResponseMessageBinaryByRequestId(DataType dataType, String requestId) {

        TestDataInfo info = createTestDataInfo(dataType, requestId);
        
        if (info.no > info.messagePool.getRecords().size()) {
            throw new RuntimeException(String.format(
                    "receive message did not exists. data type=[%s], "
                  + "request id=[%s], no=[%s], path=[%s], resource name=[%s].", 
                  dataType, requestId, info.no, info.basePath, info.resourceName));
        }
        DataRecord dataRecord = info.messagePool.getRecords().get(info.no - 1);
        
        if (dataRecord.containsValue(SendSyncMessageParser.ErrorMode.TIMEOUT.getValue())) {
            // タイムアウトを返却するテストの場合、nullを返却する
            return null;
        } else if (dataRecord.containsValue(SendSyncMessageParser.ErrorMode.MSG_EXCEPTION.getValue())) {
            // MessagingExceptionをスローするテストの場合、nullを返却する
            throw new MessagingException("message exception was happened! this exception was thrown by mock.");
        }
        
        // テストデータ変換
        dataRecord = info.messagePool.convertByFileType(dataRecord);
        // 対応するレイアウト定義を生成
        LayoutDefinition ld = info.messagePool.createLayoutFromDataRecord(dataRecord);
        
        // SendingMessageを使用してメッセージをバイナリ化する
        SendingMessage sendingMessage = new SendingMessage();
        sendingMessage.setFormatter(info.messagePool.getFormatter().setDefinition(ld))
        .addRecord(dataRecord);
        
        return sendingMessage.getBodyBytes();
    }

    /**
     * リクエストIDに紐付くメッセージのバイナリを取得する。
     * @param dataType データタイプ
     * @param requestId リクエストID
     * @return 応答電文レコード
     */
    public DataRecord getResponseMessageByRequestId(DataType dataType, String requestId) {

        TestDataInfo info = createTestDataInfo(dataType, requestId);
        
        if (info.no > info.messagePool.getRecords().size()) {
            throw new RuntimeException(String.format(
                    "receive message did not exists. data type=[%s], "
                  + "request id=[%s], no=[%s], path=[%s], resource name=[%s].", 
                  dataType, requestId, info.no, info.basePath, info.resourceName));
        }
        DataRecord dataRecord = info.messagePool.getRecords().get(info.no - 1);
        
        // テストデータ変換
        dataRecord = info.messagePool.convertByFileType(dataRecord);

        return dataRecord;
    }

    /**
     * メッセージを生成する。
     * <p>
     * 読み込むテストデータのタイムスタンプ署名が変更されていない場合は、キャッシュからメッセージを取得する。
     * </p>
     * @param dataType データタイプ
     * @param requestId リクエストID
     * @return メッセージ
     */
    private TestDataInfo createTestDataInfo(DataType dataType, String requestId) {
        FilePathSetting filePathSetting = FilePathSetting.getInstance();
        String basePath = filePathSetting.getBaseDirectory(SEND_SYNC_TEST_DATA_BASE_PATH).getPath();
        String resourceName = requestId + "/" + RESPONSE_MESSAGES_SHEET_NAME;
        File file = filePathSetting.getFileIfExists(SEND_SYNC_TEST_DATA_BASE_PATH, requestId);
        if (file == null) {
            throw new IllegalStateException(String.format(
                    "test data file was not found. request id=[%s], base path=[%s], resource name=[%s], absolute base path=[%s].",
                    requestId, basePath, resourceName, new File(basePath).getAbsolutePath()));
        }
        
        String cacheKey = createCacheKey(dataType, requestId);

        // テストデータの読み込み中に行われた書き換えを取りこぼさないよう、
        // 実データを読み込む前にタイムスタンプ署名を1度だけ取得し、比較にも記録にも同じ値を使用する。
        long timestampSignature = getTimestampSignature(file);

        TestDataInfo testDataInfo;
        if (fileCache.containsKey(cacheKey)) {
            TestDataInfo cachedTestDataInfo = fileCache.get(cacheKey);
            // 読み込むテストデータのタイムスタンプ署名が変更された場合、再読み込みを行う
            if (timestampSignature != cachedTestDataInfo.timestampSignature) {
                testDataInfo = createTestDataInfo(dataType, requestId, basePath,
                        resourceName, timestampSignature, cacheKey);
            } else {
                cachedTestDataInfo.incrementNo(); // 読み込む番号をインクリメントする
                testDataInfo = cachedTestDataInfo;
            }
        } else {
            testDataInfo = createTestDataInfo(dataType, requestId, basePath,
                    resourceName, timestampSignature, cacheKey);
        }

        return testDataInfo;
    }

    /**
     * メッセージを生成する。
     * @param dataType データタイプ
     * @param requestId リクエストID
     * @param basePath ベースパス
     * @param resourceName リソース名
     * @param timestampSignature テストデータのタイムスタンプ署名
     * @param cacheKey キャッシュのキー
     * @return ロードしたメッセージ
     */
    private TestDataInfo createTestDataInfo(DataType dataType, String requestId,
            String basePath, String resourceName, long timestampSignature, String cacheKey) {
        TestDataInfo testDataInfo;
        MessagePool pool = getMessages(basePath, resourceName, dataType, requestId);
        testDataInfo = new TestDataInfo(timestampSignature, pool, basePath, resourceName);
        fileCache.put(cacheKey, testDataInfo);
        return testDataInfo;
    }

    /**
     * テストデータのタイムスタンプ署名を取得する。
     * <p>
     * テストデータがファイルの場合（ベースパスに拡張子が設定されている場合）は、
     * そのファイルの最終更新日時をそのまま返却する。
     * </p>
     * <p>
     * テストデータがディレクトリの場合（ベースパスに拡張子が設定されていない場合）、
     * ディレクトリ自体の最終更新日時は配下のファイルの内容が書き換えられても変化しない。
     * このため、ディレクトリ自体および配下の全エントリの最終更新日時を畳み込んだ値を署名とする。
     * 最大値を採らないのは、未来日時の最終更新日時を持つファイルが1つでも存在すると、
     * 以降どのファイルを書き換えても値が変化せず、再読み込みが恒久的に行われなくなるためである。
     * </p>
     * <p>
     * {@link File#listFiles()} が返すエントリの順序は保証されないため、
     * 走査順によって署名が変化しないよう、畳み込む前にソートする。
     * </p>
     * <p>
     * なお、畳み込んだ値であるため、内容が異なるにも関わらず署名が一致し
     * 再読み込みが行われない可能性が確率的に残る。
     * </p>
     * <p>
     * このメソッドの戻り値をテストから検証するため、可視性をpackage privateとしている。
     * </p>
     * @param file テストデータのファイルまたはディレクトリ
     * @return タイムスタンプ署名
     */
    long getTimestampSignature(File file) {
        long lastModified = file.lastModified();
        if (!file.isDirectory()) {
            return lastModified;
        }
        File[] files = file.listFiles();
        if (files == null) {
            // ディレクトリの一覧が取得できない場合は、ディレクトリ自体の最終更新日時を使用する
            return lastModified;
        }
        // 走査順に依存しない署名とするため、エントリをソートしてから畳み込む
        Arrays.sort(files);
        long signature = lastModified;
        for (File child : files) {
            signature = signature * 31 + getTimestampSignature(child);
        }
        return signature;
    }

    /**
     * 読み込んだテストデータをキャッシュするためのキーを生成する。
     * @param dataType データタイプ
     * @param requestId リクエストID
     * @return 読み込んだテストデータをキャッシュするためのキー
     */
    private String createCacheKey(DataType dataType, String requestId) {
        return dataType + "_" + requestId;
    }

    /**
     * リクエストIDに紐付くメッセージを生成する。
     * @param path パス
     * @param resourceName リソース名
     * @param dataType データタイプ
     * @param requestId リクエストID
     * @return メッセージのリスト
     */
    private MessagePool getMessages(String path,
            String resourceName, DataType dataType, String requestId) {
        
        BasicTestDataParser testDataParser = SystemRepository.get("messagingTestDataParser");
        if (testDataParser == null) {
            throw new IllegalStateException("can't get TestDataParser. check configuration.");
        }
        
        MessagePool pools = testDataParser.getMessageWithoutCache(path, resourceName, dataType, requestId);

        if (pools == null) {
            throw new IllegalStateException(
                    String.format(
                            "message was not found. message must be setting. "
                          + "data type=[%s], request id=[%s], path=[%s], resource name=[%s].",
                            dataType.getName(), requestId, path, resourceName));
        }
        
        return pools;
    }


    /** テストデータ情報 */
    private static class TestDataInfo {
        
        /** テストデータのタイムスタンプ署名 */
        private long timestampSignature;
        /** メッセージ */
        private MessagePool messagePool;
        /** 読み込む番号 */
        private int no = 1;
        /** ベースパス名 */
        private String basePath;
        /** リソース名 */
        private String resourceName;

        /**
         * コンストラクタ
         * @param timestampSignature テストデータのタイムスタンプ署名
         * @param messagePool メッセージ
         * @param basePath ベースパス名
         * @param resourceName リソース名
         */
        TestDataInfo(long timestampSignature, MessagePool messagePool, String basePath, String resourceName) {
            this.timestampSignature = timestampSignature;
            this.messagePool = messagePool;
            this.basePath = basePath;
            this.resourceName = resourceName;
        }

        /**
         * 番号をインクリメントする 
         * @return このクラス自身のインスタンス
         */
        TestDataInfo incrementNo() {
            no++;
            return this;
        }
    }


}
