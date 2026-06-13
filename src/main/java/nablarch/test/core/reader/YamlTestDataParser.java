package nablarch.test.core.reader;

import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.db.TableData;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.messaging.RequestTestingMessagePool;
import nablarch.test.core.reader.yaml.YamlFileStructureMapper;
import nablarch.test.core.reader.yaml.YamlLoader;
import nablarch.test.core.reader.yaml.YamlMessageStructureMapper;
import nablarch.test.core.reader.yaml.YamlSection;
import nablarch.test.core.reader.yaml.YamlTableStructureMapper;
import nablarch.test.core.reader.yaml.YamlValueProcessor;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * YAML 形式のテストデータを読み込むパーサ。
 *
 * <p>
 * {@link BasicTestDataParser} を継承し、各 getter を YAML ファイルから直接構築するようオーバーライドする。
 * 読み込みは <b>2 層</b>で構成する（設計書 判断 B / option C）。
 * </p>
 * <ol>
 *   <li><b>構造マッピング層</b>（{@code Yaml*StructureMapper}）: YAML Map を値未加工の生の構造レコード
 *       （{@code Raw*}）へ写し取る。変換ツールと共有する公開 API。</li>
 *   <li><b>値加工＋組み立て層</b>（{@link YamlValueProcessor}）: 生の構造レコードに特殊記法の解釈・
 *       デフォルト値補完・メッセージ長の {@code -} 注入を施し、本体の器を組み立てる。</li>
 * </ol>
 * <p>
 * 2 層を明示的に順に呼び出すため、旧実装の「空の interpreters を渡すと加工が外れる」暗黙の切り替えは持たない。
 * {@link TestDataReader} は使用しない（{@link #setTestDataReader} は {@link UnsupportedOperationException} をスローする）。
 * </p>
 *
 * @author kiyotis
 */
public class YamlTestDataParser extends BasicTestDataParser {

    private DbInfo dbInfo;
    private DefaultValues defaultValues = new BasicDefaultValues();
    private List<TestDataInterpreter> interpreters;

    /** 構造マッピング層（状態を持たないため使い回す）。 */
    private final YamlTableStructureMapper tableMapper = new YamlTableStructureMapper();
    private final YamlFileStructureMapper fileMapper = new YamlFileStructureMapper();
    private final YamlMessageStructureMapper messageMapper = new YamlMessageStructureMapper();

    /** 値加工＋組み立て層（dbInfo・defaultValues・interpreters 設定時に再構築する）。 */
    private YamlValueProcessor valueProcessor;

    /** デフォルトコンストラクタ。値加工層をデフォルト設定で初期化する。 */
    public YamlTestDataParser() {
        rebuildValueProcessor();
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
        rebuildValueProcessor();
    }

    /** {@inheritDoc} */
    @Override
    public void setInterpreters(List<TestDataInterpreter> interpretersPrototype) {
        this.interpreters = interpretersPrototype;
        super.setInterpreters(interpretersPrototype);
        rebuildValueProcessor();
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultValues(DefaultValues defaultValues) {
        this.defaultValues = defaultValues;
        super.setDefaultValues(defaultValues);
        rebuildValueProcessor();
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
        return valueProcessor.toTableDataList(
                tableMapper.mapTables(yaml, YamlSection.KEY_SETUP_TABLES),
                YamlSection.KEY_SETUP_TABLES, gid, false, path);
    }

    /** {@inheritDoc} */
    @Override
    public List<TableData> getExpectedTableData(String path, String resourceName, String... groupId) {
        Map<String, Object> yaml = YamlLoader.load(path, resourceName);
        String gid = formatGroupId(groupId);
        List<TableData> expected = valueProcessor.toTableDataList(
                tableMapper.mapTables(yaml, YamlSection.KEY_EXPECTED_TABLES),
                YamlSection.KEY_EXPECTED_TABLES, gid, false, path);
        List<TableData> completed = valueProcessor.toTableDataList(
                tableMapper.mapTables(yaml, YamlSection.KEY_EXPECTED_COMPLETE_TABLES),
                YamlSection.KEY_EXPECTED_COMPLETE_TABLES, gid, true, path);
        expected.addAll(completed);
        return expected;
    }

    /** {@inheritDoc} */
    @Override
    public List<Map<String, String>> getListMap(String path, String resourceName, String id) {
        Map<String, Object> yaml = YamlLoader.load(path, resourceName);
        return valueProcessor.toListMapRows(tableMapper.mapListMaps(yaml), id, path);
    }

    /** {@inheritDoc} */
    @Override
    public List<DataFile> getSetupFile(String path, String resourceName, String... groupId) {
        Map<String, Object> yaml = YamlLoader.load(path, resourceName);
        String gid = formatGroupId(groupId);
        return valueProcessor.toDataFileList(
                fileMapper.mapFiles(yaml, YamlSection.KEY_SETUP_FILES),
                YamlSection.KEY_SETUP_FILES, gid, path);
    }

    /** {@inheritDoc} */
    @Override
    public List<DataFile> getExpectedFile(String path, String resourceName, String... groupId) {
        Map<String, Object> yaml = YamlLoader.load(path, resourceName);
        String gid = formatGroupId(groupId);
        return valueProcessor.toDataFileList(
                fileMapper.mapFiles(yaml, YamlSection.KEY_EXPECTED_FILES),
                YamlSection.KEY_EXPECTED_FILES, gid, path);
    }

    /** {@inheritDoc} */
    @Override
    public MessagePool getMessage(String path, String resourceName, String id) {
        Map<String, Object> yaml = YamlLoader.load(path, resourceName);
        return valueProcessor.toMessagePool(
                messageMapper.mapMessages(yaml, YamlSection.KEY_MESSAGES), id, true, path);
    }

    /** {@inheritDoc} */
    @Override
    public MessagePool getMessageWithoutCache(String path, String resourceName, DataType dataType, String id) {
        Map<String, Object> yaml = YamlLoader.load(path, resourceName);
        String sectionKey = YamlSection.dataTypeToSectionKey(dataType);
        boolean useFwHeader = YamlSection.KEY_MESSAGES.equals(sectionKey);
        return valueProcessor.toMessagePool(
                messageMapper.mapMessages(yaml, sectionKey), id, useFwHeader, path);
    }

    /** {@inheritDoc} */
    @Override
    public List<RequestTestingMessagePool> getSendSyncMessage(String path, String resourceName,
                                                               String id, DataType dataType) {
        Map<String, Object> yaml = YamlLoader.load(path, resourceName);
        String sectionKey = YamlSection.dataTypeToSectionKey(dataType);
        return valueProcessor.toSendSyncList(messageMapper.mapMessages(yaml, sectionKey), id, path);
    }

    /**
     * テスト専用: YAML キャッシュをクリアする。
     * テスト間のキャッシュ汚染を防ぐために {@code @After} メソッドから呼ぶこと。
     */
    static void clearCacheForTest() {
        YamlLoader.clearCacheForTest();
    }

    private void rebuildValueProcessor() {
        valueProcessor = new YamlValueProcessor(dbInfo, defaultValues, interpreters);
    }
}
