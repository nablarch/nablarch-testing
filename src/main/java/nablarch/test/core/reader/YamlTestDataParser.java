package nablarch.test.core.reader;

import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.db.TableData;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.messaging.RequestTestingMessagePool;
import nablarch.test.core.reader.yaml.YamlFileBuilder;
import nablarch.test.core.reader.yaml.YamlLoader;
import nablarch.test.core.reader.yaml.YamlMessageBuilder;
import nablarch.test.core.reader.yaml.YamlSection;
import nablarch.test.core.reader.yaml.YamlTableDataBuilder;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * YAML 形式のテストデータを読み込むパーサ。
 *
 * <p>
 * {@link BasicTestDataParser} を継承し、各 getter を YAML ファイルから直接構築するようオーバーライドする。
 * {@link YamlLoader#load} が返す順序保持 Map を、データ種別ごとのビルダ
 * （{@link YamlTableDataBuilder}／{@link YamlFileBuilder}／{@link YamlMessageBuilder}）が走査し、
 * 構造の写し取りと値加工（特殊記法の解釈・デフォルト値補完・メッセージ長の {@code -} 注入等）を行って
 * 本体の器（{@link TableData}／{@link DataFile}／{@link MessagePool}）を直接組み立てる（設計書 判断 B）。
 * </p>
 * <p>
 * {@link TestDataReader} は使用しない（{@link #setTestDataReader} は {@link UnsupportedOperationException} をスローする）。
 * </p>
 *
 * @author kiyotis
 */
public class YamlTestDataParser extends BasicTestDataParser {

    private DbInfo dbInfo;
    private DefaultValues defaultValues = new BasicDefaultValues();
    private List<TestDataInterpreter> interpreters;

    /** データ種別ごとのビルダ（dbInfo・defaultValues・interpreters 設定時に再構築する）。 */
    private YamlTableDataBuilder tableBuilder;
    private YamlFileBuilder fileBuilder;
    private YamlMessageBuilder messageBuilder;

    /** デフォルトコンストラクタ。ビルダをデフォルト設定で初期化する。 */
    public YamlTestDataParser() {
        rebuildBuilders();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * {@code YamlTestDataParser} は {@link TestDataReader} を使用しない。
     * YAML ファイルはファイルシステムから直接ロードするため、このメソッドを呼ぶ必要はない。
     * DI 設定で本クラスを使用する場合は {@code setTestDataReader} を設定しないこと。
     * </p>
     *
     * @throws UnsupportedOperationException 常にスローされる
     */
    @Override
    public void setTestDataReader(TestDataReader testDataReader) {
        throw new UnsupportedOperationException(
                "YamlTestDataParser does not use TestDataReader. "
                        + "YAML files are loaded directly from the file system.");
    }

    /** {@inheritDoc} */
    @Override
    public void setDbInfo(DbInfo dbInfo) {
        this.dbInfo = dbInfo;
        super.setDbInfo(dbInfo);
        rebuildBuilders();
    }

    /** {@inheritDoc} */
    @Override
    public void setInterpreters(List<TestDataInterpreter> interpretersPrototype) {
        this.interpreters = interpretersPrototype;
        super.setInterpreters(interpretersPrototype);
        rebuildBuilders();
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultValues(DefaultValues defaultValues) {
        this.defaultValues = defaultValues;
        super.setDefaultValues(defaultValues);
        rebuildBuilders();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isResourceExisting(String basePath, String resourceName) {
        return YamlLoader.isResourceExisting(basePath, resourceName);
    }

    /** {@inheritDoc} */
    @Override
    public List<TableData> getSetupTableData(String path, String resourceName, String... groupId) {
        if (!isResourceExisting(path, resourceName)) {
            return Collections.emptyList();
        }
        Map<String, Object> yaml = YamlLoader.load(path, resourceName);
        String gid = formatGroupId(groupId);
        return tableBuilder.buildTableDataList(yaml, YamlSection.KEY_SETUP_TABLES, gid, false, path);
    }

    /** {@inheritDoc} */
    @Override
    public List<TableData> getExpectedTableData(String path, String resourceName, String... groupId) {
        Map<String, Object> yaml = YamlLoader.load(path, resourceName);
        String gid = formatGroupId(groupId);
        List<TableData> expected = tableBuilder.buildTableDataList(
                yaml, YamlSection.KEY_EXPECTED_TABLES, gid, false, path);
        List<TableData> completed = tableBuilder.buildTableDataList(
                yaml, YamlSection.KEY_EXPECTED_COMPLETE_TABLES, gid, true, path);
        expected.addAll(completed);
        return expected;
    }

    /** {@inheritDoc} */
    @Override
    public List<Map<String, String>> getListMap(String path, String resourceName, String id) {
        Map<String, Object> yaml = YamlLoader.load(path, resourceName);
        return tableBuilder.buildListMapRows(yaml, id, path);
    }

    /** {@inheritDoc} */
    @Override
    public List<DataFile> getSetupFile(String path, String resourceName, String... groupId) {
        Map<String, Object> yaml = YamlLoader.load(path, resourceName);
        String gid = formatGroupId(groupId);
        return fileBuilder.buildDataFileList(yaml, YamlSection.KEY_SETUP_FILES, gid, path);
    }

    /** {@inheritDoc} */
    @Override
    public List<DataFile> getExpectedFile(String path, String resourceName, String... groupId) {
        Map<String, Object> yaml = YamlLoader.load(path, resourceName);
        String gid = formatGroupId(groupId);
        return fileBuilder.buildDataFileList(yaml, YamlSection.KEY_EXPECTED_FILES, gid, path);
    }

    /** {@inheritDoc} */
    @Override
    public MessagePool getMessage(String path, String resourceName, String id) {
        Map<String, Object> yaml = YamlLoader.load(path, resourceName);
        return messageBuilder.buildMessagePool(yaml, YamlSection.KEY_MESSAGES, id, true, path);
    }

    /** {@inheritDoc} */
    @Override
    public MessagePool getMessageWithoutCache(String path, String resourceName, DataType dataType, String id) {
        Map<String, Object> yaml = YamlLoader.load(path, resourceName);
        String sectionKey = YamlSection.dataTypeToSectionKey(dataType);
        boolean useFwHeader = YamlSection.KEY_MESSAGES.equals(sectionKey);
        return messageBuilder.buildMessagePool(yaml, sectionKey, id, useFwHeader, path);
    }

    /** {@inheritDoc} */
    @Override
    public List<RequestTestingMessagePool> getSendSyncMessage(String path, String resourceName,
                                                               String id, DataType dataType) {
        Map<String, Object> yaml = YamlLoader.load(path, resourceName);
        String sectionKey = YamlSection.dataTypeToSectionKey(dataType);
        return messageBuilder.buildSendSyncList(yaml, sectionKey, id, path);
    }

    /**
     * テスト専用: YAML キャッシュをクリアする。
     * テスト間のキャッシュ汚染を防ぐために {@code @After} メソッドから呼ぶこと。
     */
    static void clearCacheForTest() {
        YamlLoader.clearCacheForTest();
    }

    private void rebuildBuilders() {
        tableBuilder = new YamlTableDataBuilder(dbInfo, defaultValues, interpreters);
        fileBuilder = new YamlFileBuilder(interpreters);
        messageBuilder = new YamlMessageBuilder(interpreters);
    }
}
