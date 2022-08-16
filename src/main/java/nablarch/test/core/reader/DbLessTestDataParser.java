package nablarch.test.core.reader;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.TableData;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * DBデータ以外のテストデータを読み込み、各オブジェクトにparseするクラス。
 *
 * @author Shinya Hijiri
 */
public class DbLessTestDataParser implements TestDataParser {

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(BasicTestDataParser.class);

    /** 委譲先のテストデータリーダ */
    private final TestDataParser testDataParser = new BasicTestDataParser();

    @Override
    public List<TableData> getExpectedTableData(String path, String resourceName, String... groupId) {
        throw new UnsupportedOperationException("Parsing of table data is not supported.");
    }

    @Override
    public List<TableData> getSetupTableData(String path, String resourceName, String... groupId) {
        LOGGER.logDebug("Skip table data initialization as it is not supported.");
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, String>> getListMap(String path, String resourceName, String id) {
        return testDataParser.getListMap(path, resourceName, id);
    }

    @Override
    public List<DataFile> getSetupFile(String path, String resourceName, String... groupId) {
        return testDataParser.getSetupFile(path, resourceName, groupId);
    }

    @Override
    public List<DataFile> getExpectedFile(String path, String resourceName, String... groupId) {
        return testDataParser.getExpectedFile(path, resourceName, groupId);
    }
    
    @Override
    public MessagePool getMessage(String path, String resourceName, String id) {
        return testDataParser.getMessage(path, resourceName, id);
    }

    @Override
    public void setTestDataReader(TestDataReader testDataReader) {
        testDataParser.setTestDataReader(testDataReader);
    }

    @Override
    public void setDbInfo(DbInfo dbInfo) {
        // 使用禁止にするため例外を投げたいが、テスト等でコンポーネントのプロパティ定義から自動インジェクションされる場合があり、
        // その際にエラーとなってしまう。回避手段が無いため、呼び出されても例外は投げずに空振りするようにしておく。
    }

    @Override
    public void setInterpreters(List<TestDataInterpreter> interpreter) {
        testDataParser.setInterpreters(interpreter);
    }

    @Override
    public boolean isResourceExisting(String basePath, String resourceName) {
        return testDataParser.isResourceExisting(basePath, resourceName);
    }
}
