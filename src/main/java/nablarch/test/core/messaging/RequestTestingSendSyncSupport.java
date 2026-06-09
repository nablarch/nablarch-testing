package nablarch.test.core.messaging;

import nablarch.core.util.annotation.Published;
import nablarch.test.TestSupport;
import nablarch.test.core.reader.BasicTestDataParser;
import nablarch.test.core.reader.DataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * リクエスト単体テストで必要なメッセージング操作をサポートするクラス。
 *
 * @author Masato Inoue
 */
@Published
public class RequestTestingSendSyncSupport {

    /** テストサポートクラス(テストデータ読み込み用) */
    private final TestSupport support;
    
    /** Excel情報のキャッシュ */
    private static Map<String, List<RequestTestingMessagePool>> fileCache = new HashMap<String, List<RequestTestingMessagePool>>();
    
    /**
     * コンストラクタ。
     * @param testClass テストクラス
     */
    public RequestTestingSendSyncSupport(Class<?> testClass) {
        support = new TestSupport(testClass);
    }
    
    /**
     * 要求電文の準備を行う。
     * @param sheetName シート名
     * @param caseNo テストケース番号
     * @param expectedRequestMessageId 要求電文（期待値）のID
     * @param dataType データタイプ
     * @param useCache 読み込んだデータのキャッシュ要否
     * @return テストデータ（メッセージ）投入用クラスのインスタンス
     */
    public List<RequestTestingMessagePool> getExpectedRequestMessage(
            String sheetName, Integer caseNo, String expectedRequestMessageId,
            DataType dataType, boolean useCache) {
        List<RequestTestingMessagePool> headerMessages = getMessages(sheetName,
                caseNo, expectedRequestMessageId, dataType, useCache);
        return headerMessages;
    }

    /**
     * 応答電文の準備を行う。
     * @param sheetName シート名
     * @param requestId リクエストID
     * @param caseNo テストケース番号
     * @param responseMessageId 応答電文のID
     * @param dataType データタイプ
     * @param useCache 読み込んだデータのキャッシュ要否
     * @return テストデータ（メッセージ）投入用クラスのインスタンス
     */
    public RequestTestingMessagePool getResponseMessage(String sheetName,
            String requestId, Integer caseNo, String responseMessageId,
            DataType dataType, boolean useCache) {
        RequestTestingMessagePool messages = getResponseMessageByRequestId(sheetName,
                requestId, caseNo, responseMessageId, dataType, useCache);
        return messages;
    }
    
    /**
     * リクエストIDに紐付くメッセージのリストを取得する。
     * @param sheetName シート名
     * @param requestId リクエストID
     * @param caseNo テストケース番号
     * @param responseMessageId 応答電文のID
     * @param dataType データタイプ
     * @param useCache 読み込んだデータのキャッシュ要否
     * @return メッセージのリスト
     */
    private RequestTestingMessagePool getResponseMessageByRequestId(String sheetName, String requestId,
            Integer caseNo, String responseMessageId, DataType dataType, boolean useCache) {

        List<RequestTestingMessagePool> pools = getMessages(sheetName, caseNo, responseMessageId, dataType, useCache);

        // リクエストIDに合致するメッセージを返却
        for (RequestTestingMessagePool pool : pools) {
            if (requestId.equals(pool.getRequestId())) {
                return pool;
            }
        }

        throw new IllegalStateException(
                String.format(
                        "messages that match the request was not found in sheet. "
                      + "sheet name=[%s], case no=[%s], message id=[%s], data type name=[%s], request id=[%s].",
                        sheetName, caseNo, responseMessageId,
                        dataType.getName(), requestId));
    }
    
    /**
     * メッセージのリストを取得する。
     * @param sheetName シート名
     * @param caseNo テストケース番号
     * @param messageId 電文のID
     * @param dataType データタイプ
     * @param useCache 読み込んだデータのキャッシュ要否
     * @return メッセージのリスト
     */
    private List<RequestTestingMessagePool> getMessages(String sheetName, 
            Integer caseNo, String messageId, DataType dataType, boolean useCache) {

        String resourceName = support.getResourceName(sheetName);
        String path = support.getPathOf(resourceName);
        
        List<RequestTestingMessagePool> pools;
        if (useCache) {
            String cacheKey = createCacheKey(messageId, dataType, path, resourceName);
            if (fileCache.containsKey(cacheKey)) {
                return fileCache.get(cacheKey);
            } else {
                pools = getMessages(path, resourceName, caseNo, messageId, dataType);
                fileCache.put(cacheKey, pools);
            }
        } else {
            pools = getMessages(path, resourceName, caseNo, messageId, dataType);
        }
        return pools;
    }

    /**
     * 読み込んだテストデータをキャッシュするためのキーを生成する。
     * @param messageId 電文のID
     * @param dataType データタイプ
     * @param path パス
     * @param resourceName リソース名
     * @return 読み込んだテストデータをキャッシュするためのキー
     */
    private String createCacheKey(String messageId,
            DataType dataType, String path, String resourceName) {
        String cacheKey = path + "_" + resourceName + "_" + messageId + "_" + dataType.getName();
        return cacheKey;
    }

    /**
     * テストケース番号および電文のIDに紐付くメッセージを生成する。
     * @param path パス
     * @param resourceName リソース名
     * @param caseNo テストケース番号
     * @param messageId 電文のID
     * @param dataType データタイプ
     * @return メッセージのリスト
     */
    private List<RequestTestingMessagePool> getMessages(String path,
            String resourceName, Integer caseNo, String messageId, DataType dataType) {

        BasicTestDataParser testDataParser = (BasicTestDataParser) support
                .getTestDataParser();
        List<RequestTestingMessagePool> pools = testDataParser.getSendSyncMessage(path, resourceName, "[" + messageId + "]", dataType);

        if (pools == null) {
            throw new IllegalStateException(
                    String.format(
                            "message was not found. message must be set. case number=[%s], "
                          + "message id=[%s], data type=[%s], path=[%s], resource name=[%s].",
                            caseNo, messageId, dataType.getName(), path, resourceName));
        }

        return pools;
    }



}
