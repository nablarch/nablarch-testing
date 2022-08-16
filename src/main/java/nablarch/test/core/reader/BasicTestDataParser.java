package nablarch.test.core.reader;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.StringUtil;
import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.db.TableData;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.file.VariableLengthFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.messaging.RequestTestingMessagePool;
import nablarch.test.core.util.interpreter.BinaryFileInterpreter;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static nablarch.core.util.Builder.concat;


/**
 * テストデータを読み込み、各オブジェクトにparseするクラス。
 *
 * @author Hisaaki Sioiri
 * @version 1.0
 */
public class BasicTestDataParser implements TestDataParser {

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(BasicTestDataParser.class);
    
    /** テストデータリーダ */
    private TestDataReader testDataReader;

    /** データベース情報 */
    private DbInfo dbInfo;

    /** データベースデフォルト値 */
    private DefaultValues defaultValues = new BasicDefaultValues();

    /** 委譲先の{@link nablarch.test.core.util.interpreter.TestDataInterpreter} */
    private List<TestDataInterpreter> interpreters;

    /** {@inheritDoc} */
    public List<TableData> getSetupTableData(String path, String resourceName, String... groupId) {
        // DBの事前準備が必要無ければ空シートを作成しなくてもいいようにしておく
        if (!testDataReader.isDataExisting(path, resourceName)) {
            LOGGER.logDebug("Skipping table data initialization because preparation data is not found. resource=[" + resourceName + "]");
            return Collections.emptyList();
        }
        return getTableData(path, resourceName, DataType.SETUP_TABLE_DATA, formatGroupId(groupId));
    }

    /** {@inheritDoc} */
    public List<Map<String, String>> getListMap(String path, String resourceName, String id) {
        ListMapParser agent = new ListMapParser(testDataReader, addBinaryFileInterpreter(path));
        agent.parse(path, resourceName, id);
        return agent.getResult();
    }

    /** {@inheritDoc} */
    public List<DataFile> getSetupFile(String path, String resourceName, String... groupId) {
        List<DataFile> result = new ArrayList<DataFile>();
        result.addAll(getFixedLengthFile(DataType.SETUP_FIXED, path, resourceName, groupId));
        result.addAll(getVariableLengthFile(DataType.SETUP_VARIABLE, path, resourceName, groupId));
        return result;
    }

    /** {@inheritDoc} */
    public List<DataFile> getExpectedFile(String path, String resourceName, String... groupId) {
        List<DataFile> result = new ArrayList<DataFile>();
        result.addAll(getFixedLengthFile(DataType.EXPECTED_FIXED, path, resourceName, groupId));
        result.addAll(getVariableLengthFile(DataType.EXPECTED_VARIABLE, path, resourceName, groupId));
        return result;
    }
    /** {@inheritDoc} */
    public MessagePool getMessage(String path, String resourceName, String id) {
        MessageParser agent = new MessageParser(testDataReader, addBinaryFileInterpreter(path), DataType.MESSAGE);
        agent.parse(path, resourceName, id);
        return agent.getResult();
    }
    
    /**
     * メッセージを取得する。
     * <p>
     * Excelファイルのキャッシュは行わない。
     * </p>
     * @param path         取得元パス
     * @param resourceName 取得元データリソース名
     * @param dataType     データタイプ
     * @param id           ID
     * @return メッセージ
     */
    public MessagePool getMessageWithoutCache(String path, String resourceName, DataType dataType, String id) {
        SendSyncMessageParser agent = new SendSyncMessageParser(testDataReader, addBinaryFileInterpreter(path), dataType);
        agent.parse(path, resourceName, id, false);
        return agent.getResult();
    }
    
    /** 
     * メッセージ同期送信処理の場合のメッセージを取得する
     * @param path         ファイルパス
     * @param resourceName リソース名
     * @param id      グループID
     * @param dataType データタイプ
     * @return メッセージのリスト
     */
    public List<RequestTestingMessagePool> getSendSyncMessage(String path, String resourceName, String id, DataType dataType) {
        GroupMessageParser agent = new GroupMessageParser(testDataReader, addBinaryFileInterpreter(path), dataType);
        agent.parse(path, resourceName, id);
        return agent.getResult();
    }
    
    /**
     * 固定長ファイルを取得する。
     *
     * @param type         データ型
     * @param path         ファイルパス
     * @param resourceName リソース名
     * @param groupId      グループID
     * @return グループIDで指定された固定長ファイル一覧
     */
    private List<FixedLengthFile> getFixedLengthFile(
            DataType type, String path, String resourceName, String... groupId) {

        FixedLengthFileParser agent = new FixedLengthFileParser(testDataReader, addBinaryFileInterpreter(path), type);
        return getFile(agent, path, resourceName, groupId);
    }


    /**
     * 可変長ファイルを取得する。
     *
     * @param type         データ型
     * @param path         ファイルパス
     * @param resourceName リソース名
     * @param groupId      グループID
     * @return グループIDで指定された可変長ファイル一覧
     */
    private List<VariableLengthFile> getVariableLengthFile(
            DataType type, String path, String resourceName, String... groupId) {

        VariableLengthFileParser agent = new VariableLengthFileParser(testDataReader, addBinaryFileInterpreter(path), type);
        return getFile(agent, path, resourceName, groupId);
    }

    /**
     * ファイルを取得する。
     *
     * @param agent        ファイル取得に使用するパーサ
     * @param path         ファイルパス
     * @param resourceName リソース名
     * @param groupId      グループID
     * @param <T>          取得するファイルの型
     * @return グループIDで指定されたファイル一覧
     */
    private <T extends DataFile> List<T> getFile(DataFileParser<T> agent, String path, String resourceName,
                                                 String... groupId) {
        String id = formatGroupId(groupId);
        agent.parse(path, resourceName, id);
        return agent.getResult();
    }


    /** {@inheritDoc} */
    public List<TableData> getExpectedTableData(String path, String resourceName, String... groupId) {
        // EXPECTED_TABLEとEXPECTED_COMPLETE_TABLEを収集し、マージして返却する。
        String gid = formatGroupId(groupId);
        List<TableData> expectedTable = getTableData(path, resourceName, DataType.EXPECTED_TABLE_DATA, gid);
        List<TableData> expectedCompleted = getTableData(path, resourceName, DataType.EXPECTED_COMPLETED, gid);
        for (TableData e : expectedCompleted) {
            e.fillDefaultValues();
        }
        expectedTable.addAll(expectedCompleted);
        return expectedTable;
    }

    /**
     * {@link TableData}を取得する。
     * 
     * @param path 取得元パス
     * @param resourceName 取得元リソース名
     * @param targetType 処理対象データ型
     * @param groupId グループIDを
     * @return 取得したデータ
     */
    private List<TableData> getTableData(String path, String resourceName, DataType targetType,
            String groupId) {
        TableDataParser agent = new TableDataParser(testDataReader, addBinaryFileInterpreter(path), dbInfo,
                defaultValues, targetType);
        agent.parse(path, resourceName, groupId);
        return agent.getResult();
    }

    /**
     * 取得元パスをもった{@link BinaryFileInterpreter}を先頭に積んだ新しいInterpreterのリストを作成する。
     * 
     * @param path 取得元パス
     * @return 取得元パスをもった{@link BinaryFileInterpreter}を先頭に積んだ新しいInterpreterのリスト
     */
    private List<TestDataInterpreter> addBinaryFileInterpreter(String path) {
        BinaryFileInterpreter fileInterpreter = new BinaryFileInterpreter(path);
        List<TestDataInterpreter> newInterpreters = new ArrayList<TestDataInterpreter>(
                interpreters.size() + 1);
        newInterpreters.add(fileInterpreter);
        newInterpreters.addAll(interpreters);
        return newInterpreters;
    }

    /** {@inheritDoc} */
    public void setTestDataReader(TestDataReader testDataReader) {
        this.testDataReader = testDataReader;
    }

    /** {@inheritDoc} */
    public void setDbInfo(DbInfo dbInfo) {
        this.dbInfo = dbInfo;
    }

    /**
     * 委譲先の{@link TestDataInterpreter}を設定する。
     *
     * @param interpretersPrototype {@link TestDataInterpreter}
     */
    public void setInterpreters(List<TestDataInterpreter> interpretersPrototype) {
        this.interpreters = interpretersPrototype;
    }

    /**
     * データベースデフォルト値を設定する。
     *
     * @param defaultValues データベースデフォルト値
     */
    public void setDefaultValues(DefaultValues defaultValues) {
        this.defaultValues = defaultValues;
    }

    /**
     * グループIDを整形する。<br/>
     * 引数の要素数が1の場合は、その要素をグループIDとして
     * <pre>[グループID]</pre>形式で返却する。<br/>
     * nullまたは要素数が0の場合は空文字を返却する。<br/>
     * その他の場合は実行時例外が発生する。
     *
     * @param groupIdVarargs グループIDを格納した可変長引数
     * @return 整形後グループID
     */
    String formatGroupId(String[] groupIdVarargs) {
        if (groupIdVarargs == null) {
            return "";
        }
        switch (groupIdVarargs.length) {
            case 0:
                return "";
            case 1:
                String groupId = groupIdVarargs[0];
                return StringUtil.isNullOrEmpty(groupId) ? "" : concat("[", groupId, "]");
            default:
                throw new IllegalArgumentException("argument groupId must be one or zero.");
        }
    }

    @Override
    public boolean isResourceExisting(String basePath, String resourceName) {
        return testDataReader.isResourceExisting(basePath, resourceName);
    }
}
