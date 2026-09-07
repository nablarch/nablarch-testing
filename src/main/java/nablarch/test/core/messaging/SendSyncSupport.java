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
    
    /** 読み込んだテストデータ情報のキャッシュ */
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
     * 読み込むテストデータの最終更新日時が変更されていない場合は、キャッシュからメッセージを取得する。
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
        // 実データを読み込む前にスナップショットを1度だけ取得し、比較にも記録にも同じ値を使用する。
        Map<String, Long> timestampSnapshot = getTimestampSnapshot(file);

        TestDataInfo testDataInfo;
        if (fileCache.containsKey(cacheKey)) {
            TestDataInfo cachedTestDataInfo = fileCache.get(cacheKey);
            // 読み込むテストデータの最終更新日時が変更された場合、再読み込みを行う
            if (!timestampSnapshot.equals(cachedTestDataInfo.timestampSnapshot)) {
                testDataInfo = createTestDataInfo(dataType, requestId, basePath,
                        resourceName, timestampSnapshot, cacheKey);
            } else {
                cachedTestDataInfo.incrementNo(); // 読み込む番号をインクリメントする
                testDataInfo = cachedTestDataInfo;
            }
        } else {
            testDataInfo = createTestDataInfo(dataType, requestId, basePath,
                    resourceName, timestampSnapshot, cacheKey);
        }

        return testDataInfo;
    }

    /**
     * メッセージを生成する。
     * @param dataType データタイプ
     * @param requestId リクエストID
     * @param basePath ベースパス
     * @param resourceName リソース名
     * @param timestampSnapshot テストデータの最終更新日時のスナップショット
     * @param cacheKey キャッシュのキー
     * @return ロードしたメッセージ
     */
    private TestDataInfo createTestDataInfo(DataType dataType, String requestId,
            String basePath, String resourceName, Map<String, Long> timestampSnapshot, String cacheKey) {
        TestDataInfo testDataInfo;
        MessagePool pool = getMessages(basePath, resourceName, dataType, requestId);
        testDataInfo = new TestDataInfo(timestampSnapshot, pool, basePath, resourceName);
        fileCache.put(cacheKey, testDataInfo);
        return testDataInfo;
    }

    /**
     * テストデータの最終更新日時のスナップショットを取得する。
     * <p>
     * ディレクトリ自体の最終更新日時は配下のファイルが書き換えられても変化しないため、
     * ディレクトリ自体と配下の全エントリの最終更新日時を、起点からの相対パスをキーとするMapで採取する。
     * テストデータがファイルの場合（ベースパスに拡張子が設定されている場合）は、
     * そのファイルの最終更新日時だけを起点自身のエントリとして採取する。
     * Mapの等価判定はキーの順序に依存しないため、{@link File#listFiles()}が返す
     * エントリの順序によって比較結果が変わることはない。
     * 保持するサイズと比較のコストはツリーのエントリ数に比例する（ツリーの走査回数は変わらない）。
     * </p>
     * <p>
     * 検知の基準は最終更新日時であり、内容のハッシュではない。
     * このため、最終更新日時を保存したまま内容を書き換えた場合は検知できない。
     * </p>
     * <p>
     * このメソッドの戻り値をテストから検証するため、可視性をpackage privateとしている。
     * </p>
     * @param file テストデータのファイルまたはディレクトリ
     * @return 起点からの相対パスをキー、最終更新日時を値とするスナップショット
     */
    Map<String, Long> getTimestampSnapshot(File file) {
        Map<String, Long> snapshot = new HashMap<String, Long>();
        collectTimestamp(file, "", snapshot);
        return snapshot;
    }

    /**
     * 最終更新日時をスナップショットに採取する。<br/>
     * ディレクトリの場合は、配下のエントリを再帰的に辿る。
     * @param file 対象のファイルまたはディレクトリ
     * @param relativePath 起点からの相対パス（起点自身は空文字）
     * @param snapshot 採取先のスナップショット
     */
    private void collectTimestamp(File file, String relativePath, Map<String, Long> snapshot) {
        snapshot.put(relativePath, file.lastModified());
        // ファイルに対しても listFiles() はnullを返すため次の分岐でも停止するが、
        // ディレクトリでない場合は再帰しないことを明示するための冗長な分岐である。
        if (!file.isDirectory()) {
            return;
        }
        File[] files = file.listFiles();
        if (files == null) {
            // ディレクトリの一覧が取得できない場合は、ディレクトリ自体のエントリのみを採取する
            return;
        }
        // キーは同一JVM内のスナップショット同士の比較にしか使わないため、区切り文字は"/"に固定する
        String prefix = relativePath.isEmpty() ? "" : relativePath + "/";
        for (File child : files) {
            collectTimestamp(child, prefix + child.getName(), snapshot);
        }
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
        
        /** テストデータの最終更新日時のスナップショット */
        private Map<String, Long> timestampSnapshot;
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
         * @param timestampSnapshot テストデータの最終更新日時のスナップショット
         * @param messagePool メッセージ
         * @param basePath ベースパス名
         * @param resourceName リソース名
         */
        TestDataInfo(Map<String, Long> timestampSnapshot, MessagePool messagePool, String basePath, String resourceName) {
            this.timestampSnapshot = timestampSnapshot;
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
