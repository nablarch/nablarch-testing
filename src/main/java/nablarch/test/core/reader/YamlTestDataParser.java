package nablarch.test.core.reader;

import nablarch.core.repository.SystemRepository;
import nablarch.test.NablarchTestUtils;
import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.db.TableData;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.DataFileFragment;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.file.MockMessages;
import nablarch.test.core.file.VariableLengthFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.messaging.RequestTestingMessagePool;
import nablarch.test.core.util.interpreter.BinaryFileInterpreter;
import nablarch.test.core.util.interpreter.InterpretationContext;
import nablarch.test.core.util.interpreter.TestDataInterpreter;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static nablarch.core.util.StringUtil.isNullOrEmpty;

/**
 * YAML 形式のテストデータを読み込むパーサ。
 *
 * <p>
 * {@link BasicTestDataParser} を継承し、各 getter を YAML ファイルから直接構築するよう
 * オーバーライドする。{@link TestDataReader} は使用しない（{@link #setTestDataReader} は
 * {@link UnsupportedOperationException} をスローする）。
 * </p>
 *
 * <p>
 * YAML ファイルは {@code {path}/{resourceName}.yaml} として配置すること（RS-01）。
 * SnakeYAML 2.x を使用し、{@link SafeConstructor} で型変換を制限して安全にロードする。
 * </p>
 *
 * @author kiyotis
 */
public class YamlTestDataParser extends BasicTestDataParser {

    /** YAML ファイルの拡張子 */
    private static final String YAML_EXTENSION = ".yaml";

    /** setup_tables セクションキー */
    private static final String KEY_SETUP_TABLES = "setup_tables";

    /** expected_tables セクションキー */
    private static final String KEY_EXPECTED_TABLES = "expected_tables";

    /** expected_complete_tables セクションキー */
    private static final String KEY_EXPECTED_COMPLETE_TABLES = "expected_complete_tables";

    /** list_maps セクションキー */
    private static final String KEY_LIST_MAPS = "list_maps";

    /** setup_files セクションキー */
    private static final String KEY_SETUP_FILES = "setup_files";

    /** expected_files セクションキー */
    private static final String KEY_EXPECTED_FILES = "expected_files";

    /** messages セクションキー */
    private static final String KEY_MESSAGES = "messages";

    /** expected_request_header_messages セクションキー */
    private static final String KEY_EXPECTED_REQUEST_HEADER_MESSAGES = "expected_request_header_messages";

    /** expected_request_body_messages セクションキー */
    private static final String KEY_EXPECTED_REQUEST_BODY_MESSAGES = "expected_request_body_messages";

    /** response_header_messages セクションキー */
    private static final String KEY_RESPONSE_HEADER_MESSAGES = "response_header_messages";

    /** response_body_messages セクションキー */
    private static final String KEY_RESPONSE_BODY_MESSAGES = "response_body_messages";

    /** group_id フィールドキー */
    private static final String FIELD_GROUP_ID = "group_id";

    /** id フィールドキー */
    private static final String FIELD_ID = "id";

    /** table フィールドキー */
    private static final String FIELD_TABLE = "table";

    /** rows フィールドキー */
    private static final String FIELD_ROWS = "rows";

    /** path フィールドキー */
    private static final String FIELD_PATH = "path";

    /** type フィールドキー（"fixed" / "variable" またはフィールド型） */
    private static final String FIELD_TYPE = "type";

    /** directives フィールドキー */
    private static final String FIELD_DIRECTIVES = "directives";

    /** records フィールドキー */
    private static final String FIELD_RECORDS = "records";

    /** record_type フィールドキー */
    private static final String FIELD_RECORD_TYPE = "record_type";

    /** fields フィールドキー */
    private static final String FIELD_FIELDS = "fields";

    /** name フィールドキー */
    private static final String FIELD_NAME = "name";

    /** length フィールドキー */
    private static final String FIELD_LENGTH = "length";

    /** ファイル種別: 固定長 */
    private static final String FILE_TYPE_FIXED = "fixed";

    /** フレームワーク制御ヘッダのレコードタイプ識別子 */
    private static final String FW_HEADER_RECORD_TYPE = "FW_HEADER";

    /** YAML キャッシュの最大保持エントリ数 */
    private static final int YAML_CACHE_MAX_SIZE = 8;

    /**
     * フレームワーク制御ヘッダフィールド名を SystemRepository から読み込むためのキー。
     * {@link nablarch.test.core.reader.MessageParser} と同じキーを参照することで、
     * DI 設定による FW ヘッダフィールドの変更が YAML 実装にも反映される。
     */
    private static final String FW_HEADER_KEY = "reader.fwHeaderfields";

    /**
     * フレームワーク制御ヘッダフィールド名セット。
     * {@value #FW_HEADER_KEY} が SystemRepository に設定されている場合はその値を使用し、
     * 設定がない場合はデフォルト値 {@code {requestId, userId, resendFlag, resultCode}} を使用する。
     */
    private final Set<String> fwHeaderFields =
            isNullOrEmpty(SystemRepository.getString(FW_HEADER_KEY))
            ? NablarchTestUtils.asSet("requestId", "userId", "resendFlag", "resultCode")
            : NablarchTestUtils.asSet(NablarchTestUtils.makeArray(SystemRepository.getString(FW_HEADER_KEY)));

    /** YAML キャッシュ（path → 解析済み Map）。アクセス順 LRU で最大 {@value #YAML_CACHE_MAX_SIZE} エントリを保持する。 */
    private static final Map<String, Map<String, Object>> YAML_CACHE =
            Collections.synchronizedMap(NablarchTestUtils.<String, Map<String, Object>>createLRUMap(YAML_CACHE_MAX_SIZE));

    /** DbInfo */
    private DbInfo dbInfo;

    /** デフォルト値 */
    private DefaultValues defaultValues = new BasicDefaultValues();

    /** Interpreter リスト */
    private List<TestDataInterpreter> interpreters;

    /**
     * {@inheritDoc}
     * YAML 実装は {@link TestDataReader} を使用しない。
     *
     * @throws UnsupportedOperationException 常にスローされる
     */
    @Override
    public void setTestDataReader(TestDataReader testDataReader) {
        throw new UnsupportedOperationException(
                "YamlTestDataParser does not use TestDataReader. "
                        + "YAML files are loaded directly from the file system.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * YAML 実装は {@code dbInfo} を独自フィールドに保持する。{@code super.setDbInfo()} も呼ぶことで、
     * 親クラスの内部処理（{@code fillDefaultValues} などの委譲先となるメソッド）が正しく機能するようにする。
     * </p>
     */
    @Override
    public void setDbInfo(DbInfo dbInfo) {
        this.dbInfo = dbInfo;
        super.setDbInfo(dbInfo);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * YAML 実装は {@code interpreters} を独自フィールドに保持する。{@code super.setInterpreters()} も呼ぶことで、
     * 親クラスに依存する処理が正しく動作するようにする。
     * </p>
     */
    @Override
    public void setInterpreters(List<TestDataInterpreter> interpretersPrototype) {
        this.interpreters = interpretersPrototype;
        super.setInterpreters(interpretersPrototype);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * YAML 実装は {@code defaultValues} を独自フィールドに保持する。{@code super.setDefaultValues()} も呼ぶことで、
     * 親クラスに依存する処理が正しく動作するようにする。
     * </p>
     */
    @Override
    public void setDefaultValues(DefaultValues defaultValues) {
        this.defaultValues = defaultValues;
        super.setDefaultValues(defaultValues);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isResourceExisting(String basePath, String resourceName) {
        return new File(basePath + resourceName + YAML_EXTENSION).exists();
    }

    /** {@inheritDoc} */
    @Override
    public List<TableData> getSetupTableData(String path, String resourceName, String... groupId) {
        if (!isResourceExisting(path, resourceName)) {
            return Collections.emptyList();
        }
        Map<String, Object> yaml = loadYaml(path, resourceName);
        String gid = formatGroupId(groupId);
        return buildTableDataList(yaml, KEY_SETUP_TABLES, gid, false, path);
    }

    /** {@inheritDoc} */
    @Override
    public List<TableData> getExpectedTableData(String path, String resourceName, String... groupId) {
        Map<String, Object> yaml = loadYaml(path, resourceName);
        String gid = formatGroupId(groupId);
        List<TableData> expected = buildTableDataList(yaml, KEY_EXPECTED_TABLES, gid, false, path);
        List<TableData> completed = buildTableDataList(yaml, KEY_EXPECTED_COMPLETE_TABLES, gid, true, path);
        expected.addAll(completed);
        return expected;
    }

    /** {@inheritDoc} */
    @Override
    public List<Map<String, String>> getListMap(String path, String resourceName, String id) {
        Map<String, Object> yaml = loadYaml(path, resourceName);
        List<Object> entries = getList(yaml, KEY_LIST_MAPS);
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String entryId = toString(map.get(FIELD_ID));
            if (id.equals(entryId)) {
                return buildListMapRows(map, path);
            }
        }
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<DataFile> getSetupFile(String path, String resourceName, String... groupId) {
        Map<String, Object> yaml = loadYaml(path, resourceName);
        String gid = formatGroupId(groupId);
        return buildFileList(yaml, KEY_SETUP_FILES, gid, path);
    }

    /** {@inheritDoc} */
    @Override
    public List<DataFile> getExpectedFile(String path, String resourceName, String... groupId) {
        Map<String, Object> yaml = loadYaml(path, resourceName);
        String gid = formatGroupId(groupId);
        return buildFileList(yaml, KEY_EXPECTED_FILES, gid, path);
    }

    /** {@inheritDoc} */
    @Override
    public MessagePool getMessage(String path, String resourceName, String id) {
        Map<String, Object> yaml = loadYaml(path, resourceName);
        FixedLengthFile file = buildMessageFile(yaml, KEY_MESSAGES, id, false, path);
        if (file == null) {
            return null;
        }
        Map<String, String> fwHeader = extractFwHeader(yaml, KEY_MESSAGES, id);
        return new RequestTestingMessagePool(file, fwHeader);
    }

    /** {@inheritDoc} */
    @Override
    public MessagePool getMessageWithoutCache(String path, String resourceName, DataType dataType, String id) {
        Map<String, Object> yaml = loadYaml(path, resourceName);
        String sectionKey = dataTypeToSectionKey(dataType);
        FixedLengthFile file = buildMessageFile(yaml, sectionKey, id, false, path);
        if (file == null) {
            return null;
        }
        Map<String, String> fwHeader = extractFwHeader(yaml, sectionKey, id);
        return new RequestTestingMessagePool(file, fwHeader);
    }

    /** {@inheritDoc} */
    @Override
    public List<RequestTestingMessagePool> getSendSyncMessage(String path, String resourceName, String id, DataType dataType) {
        Map<String, Object> yaml = loadYaml(path, resourceName);
        String sectionKey = dataTypeToSectionKey(dataType);
        List<Object> entries = getList(yaml, sectionKey);
        List<RequestTestingMessagePool> result = new ArrayList<RequestTestingMessagePool>();
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String groupId = toString(map.get(FIELD_GROUP_ID));
            if (groupId != null && groupId.equals(id)) {
                MockMessages file = buildMockMessages(map, path);
                Map<String, String> emptyHeader = Collections.emptyMap();
                RequestTestingMessagePool pool = new RequestTestingMessagePool(file, emptyHeader);
                String entryId = toString(map.get(FIELD_ID));
                if (entryId != null) {
                    pool.setRequestId(entryId);
                }
                result.add(pool);
            }
        }
        return result.isEmpty() ? null : result;
    }

    // ========================================================================
    // テスト専用
    // ========================================================================

    /**
     * テスト専用: YAML キャッシュをクリアする。
     * テスト間のキャッシュ汚染を防ぐために {@code @After} メソッドから呼ぶこと。
     */
    static void clearCacheForTest() {
        YAML_CACHE.clear();
    }

    // ========================================================================
    // private helpers
    // ========================================================================

    /**
     * YAML ファイルをロードする（キャッシュあり）。
     *
     * @param basePath     ベースパス
     * @param resourceName リソース名
     * @return YAML トップレベル Map
     */
    private Map<String, Object> loadYaml(String basePath, String resourceName) {
        String filePath = basePath + resourceName + YAML_EXTENSION;
        Map<String, Object> cached = YAML_CACHE.get(filePath);
        if (cached != null) {
            return cached;
        }
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        InputStream in = null;
        try {
            in = new FileInputStream(new File(filePath));
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) yaml.load(in);
            if (result == null) {
                result = Collections.emptyMap();
            }
            YAML_CACHE.put(filePath, result);
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load YAML file: " + filePath, e);
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignore) { /* ignore */ }
            }
        }
    }

    /**
     * TableData のリストを構築する。
     *
     * @param yaml          YAML Map
     * @param sectionKey    セクションキー
     * @param groupId       整形済みグループ ID（例: "[case01]" または ""）
     * @param fillDefaults  true の場合 {@link TableData#fillDefaultValues()} を呼ぶ
     * @param path          インタープリタ用ベースパス
     * @return TableData リスト
     */
    private List<TableData> buildTableDataList(Map<String, Object> yaml, String sectionKey,
                                                String groupId, boolean fillDefaults, String path) {
        List<Object> entries = getList(yaml, sectionKey);
        List<TableData> result = new ArrayList<TableData>();
        List<TestDataInterpreter> interps = addBinaryFileInterpreter(path);
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String entryGroupId = toString(map.get(FIELD_GROUP_ID));
            String formattedEntryGid = entryGroupId != null ? "[" + entryGroupId + "]" : "";
            if (!groupId.equals(formattedEntryGid)) {
                continue;
            }
            String tableName = toString(map.get(FIELD_TABLE));
            List<Object> rows = getList(map, FIELD_ROWS);
            if (rows.isEmpty()) {
                continue;
            }

            // 1行目のキーからカラム名を決定
            Map<String, Object> firstRow = castMap(rows.get(0));
            String[] columnNames = firstRow.keySet().toArray(new String[0]);

            TableData td = new TableData(dbInfo, tableName, columnNames, defaultValues);

            for (Object rowObj : rows) {
                Map<String, Object> rowMap = castMap(rowObj);
                List<String> rowValues = new ArrayList<String>(columnNames.length);
                for (String col : columnNames) {
                    Object rawVal = rowMap.get(col);
                    String strVal = objectToString(rawVal);
                    String interpreted = interpret(strVal, interps);
                    rowValues.add(interpreted);
                }
                td.addRow(rowValues);
            }

            if (fillDefaults) {
                td.fillDefaultValues();
            }
            result.add(td);
        }
        return result;
    }

    /**
     * List-Map の行リストを構築する。
     *
     * @param listMapEntry list_maps の 1 エントリ
     * @param path         インタープリタ用ベースパス
     * @return rows として構築した Map リスト
     */
    private List<Map<String, String>> buildListMapRows(Map<String, Object> listMapEntry, String path) {
        List<Object> rows = getList(listMapEntry, FIELD_ROWS);
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        List<TestDataInterpreter> interps = addBinaryFileInterpreter(path);
        for (Object rowObj : rows) {
            Map<String, Object> rowMap = castMap(rowObj);
            Map<String, String> row = new TreeMap<String, String>();
            for (Map.Entry<String, Object> e : rowMap.entrySet()) {
                String key = e.getKey();
                // マーカーカラム（[COLNAME] 形式）は除外
                if (key.startsWith("[") && key.endsWith("]")) {
                    continue;
                }
                String val = objectToString(e.getValue());
                String interpreted = interpret(val, interps);
                row.put(key, interpreted);
            }
            result.add(row);
        }
        return result;
    }

    /**
     * DataFile のリストを構築する。
     *
     * @param yaml       YAML Map
     * @param sectionKey セクションキー
     * @param groupId    整形済みグループ ID
     * @param basePath   ファイルパス基点
     * @return DataFile リスト
     */
    private List<DataFile> buildFileList(Map<String, Object> yaml, String sectionKey,
                                          String groupId, String basePath) {
        List<Object> entries = getList(yaml, sectionKey);
        List<DataFile> result = new ArrayList<DataFile>();
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String entryGroupId = toString(map.get(FIELD_GROUP_ID));
            String formattedEntryGid = entryGroupId != null ? "[" + entryGroupId + "]" : "";
            if (!groupId.equals(formattedEntryGid)) {
                continue;
            }
            String filePath = toString(map.get(FIELD_PATH));
            String fileType = toString(map.get(FIELD_TYPE));
            DataFile dataFile = buildDataFile(filePath, fileType, map, basePath);
            result.add(dataFile);
        }
        return result;
    }

    /**
     * DataFile を構築する。
     *
     * @param filePath ファイルパス
     * @param fileType ファイル種別（"fixed" or "variable"）
     * @param map      セクション Map
     * @param basePath ファイルパス基点
     * @return DataFile
     */
    private DataFile buildDataFile(String filePath, String fileType, Map<String, Object> map, String basePath) {
        DataFile file;
        if (FILE_TYPE_FIXED.equals(fileType)) {
            file = new FixedLengthFile(filePath);
        } else {
            file = new VariableLengthFile(filePath);
        }
        applyDirectives(file, map);
        buildFragments(file, map, basePath);
        return file;
    }

    /**
     * ファイルのディレクティブを設定する。
     *
     * @param file ファイル
     * @param map  セクション Map
     */
    private void applyDirectives(DataFile file, Map<String, Object> map) {
        Object directivesObj = map.get(FIELD_DIRECTIVES);
        if (directivesObj == null) {
            return;
        }
        Map<String, Object> directives = castMap(directivesObj);
        for (Map.Entry<String, Object> e : directives.entrySet()) {
            file.setDirective(e.getKey(), toString(e.getValue()));
        }
    }

    /**
     * DataFileFragment を構築してファイルに追加する（ファイルデータ用）。
     *
     * <p>
     * FW_HEADER レコードは除外せず、record_type はそのまま使用する。
     * </p>
     *
     * @param file     ファイル
     * @param map      セクション Map
     * @param basePath インタープリタ用ベースパス
     */
    private void buildFragments(DataFile file, Map<String, Object> map, String basePath) {
        buildFragmentsCore(file, map, false, addBinaryFileInterpreter(basePath));
    }

    /**
     * DataFileFragment を構築してファイルに追加する（共通実装）。
     *
     * @param file         ファイル
     * @param map          セクション Map
     * @param skipFwHeader true の場合 FW_HEADER レコードをスキップし、record_type を "default" に固定する
     * @param interps      使用するインタープリタリスト
     */
    private void buildFragmentsCore(DataFile file, Map<String, Object> map,
                                     boolean skipFwHeader, List<TestDataInterpreter> interps) {
        List<Object> records = getList(map, FIELD_RECORDS);
        for (Object recordObj : records) {
            Map<String, Object> record = castMap(recordObj);
            String recordType = toString(record.get(FIELD_RECORD_TYPE));

            if (skipFwHeader && FW_HEADER_RECORD_TYPE.equals(recordType)) {
                // FW_HEADER レコードはフラグメントに含めない（fwHeader として分離）
                continue;
            }

            DataFileFragment fragment = file.getNewFragment();
            // メッセージ系は record_type を "default" に固定（MessageParser の仕様）
            fragment.setRecordType(skipFwHeader ? "default" : (recordType != null ? recordType : "default"));

            List<Object> fields = getList(record, FIELD_FIELDS);
            List<String> names = new ArrayList<String>(fields.size());
            List<String> types = new ArrayList<String>(fields.size());
            List<String> lengths = new ArrayList<String>(fields.size());
            boolean hasLength = false;

            for (Object fieldObj : fields) {
                Map<String, Object> field = castMap(fieldObj);
                names.add(toString(field.get(FIELD_NAME)));
                types.add(toString(field.get(FIELD_TYPE)));
                Object len = field.get(FIELD_LENGTH);
                if (len != null) {
                    hasLength = true;
                    lengths.add(toString(len));
                } else {
                    lengths.add(null);
                }
            }

            fragment.setNames(names);
            fragment.setTypes(types);

            if (skipFwHeader || hasLength) {
                // メッセージ系: 常に lengths を設定。ファイル系: 少なくとも 1 つの length が指定された場合のみ設定
                List<String> cleanedLengths = new ArrayList<String>(lengths.size());
                for (String l : lengths) {
                    cleanedLengths.add(l != null ? l : "");
                }
                fragment.setLengths(cleanedLengths);
            }

            List<Object> rows = getList(record, FIELD_ROWS);
            for (Object rowObj : rows) {
                if (rowObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> rowList = (List<Object>) rowObj;
                    List<String> rowValues = new ArrayList<String>(rowList.size());
                    for (Object val : rowList) {
                        String strVal = objectToString(val);
                        rowValues.add(interpret(strVal, interps));
                    }
                    fragment.addValue(rowValues);
                }
            }
        }
    }

    /**
     * メッセージファイル（FixedLengthFile）を構築する。
     *
     * @param yaml       YAML Map
     * @param sectionKey セクションキー
     * @param id         メッセージ ID
     * @param isMock     MockMessages を使う場合 true
     * @param basePath   インタープリタ用ベースパス
     * @return FixedLengthFile、または存在しない場合 null
     */
    private FixedLengthFile buildMessageFile(Map<String, Object> yaml, String sectionKey,
                                              String id, boolean isMock, String basePath) {
        List<Object> entries = getList(yaml, sectionKey);
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String entryId = toString(map.get(FIELD_ID));
            if (id.equals(entryId)) {
                FixedLengthFile file = isMock ? new MockMessages(id) : new FixedLengthFile(id);
                applyDirectives(file, map);
                buildFragmentsCore(file, map, true, addBinaryFileInterpreter(basePath));
                return file;
            }
        }
        return null;
    }

    /**
     * MockMessages を構築する（getSendSyncMessage 用）。
     *
     * @param map      セクション Map
     * @param basePath ファイルパス基点
     * @return MockMessages
     */
    private MockMessages buildMockMessages(Map<String, Object> map, String basePath) {
        String entryId = toString(map.get(FIELD_ID));
        MockMessages file = new MockMessages(entryId != null ? entryId : "");
        applyDirectives(file, map);
        buildFragments(file, map, basePath);
        return file;
    }

    /**
     * FW 制御ヘッダを抽出する。
     *
     * @param yaml       YAML Map
     * @param sectionKey セクションキー
     * @param id         ID
     * @return FW 制御ヘッダ Map
     */
    private Map<String, String> extractFwHeader(Map<String, Object> yaml, String sectionKey, String id) {
        List<Object> entries = getList(yaml, sectionKey);
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String entryId = toString(map.get(FIELD_ID));
            if (id.equals(entryId)) {
                Map<String, String> fwHeader = new LinkedHashMap<String, String>();
                List<Object> records = getList(map, FIELD_RECORDS);
                for (Object recordObj : records) {
                    Map<String, Object> record = castMap(recordObj);
                    List<Object> fields = getList(record, FIELD_FIELDS);
                    List<Object> rows = getList(record, FIELD_ROWS);
                    for (Object fieldObj : fields) {
                        Map<String, Object> field = castMap(fieldObj);
                        String fieldName = toString(field.get(FIELD_NAME));
                        if (fwHeaderFields.contains(fieldName)) {
                            // 最初の行の値を FW ヘッダとして取得
                            if (!rows.isEmpty()) {
                                @SuppressWarnings("unchecked")
                                List<Object> firstRow = (List<Object>) rows.get(0);
                                int fieldIndex = fieldIndexOf(fields, fieldName);
                                if (fieldIndex >= 0 && fieldIndex < firstRow.size()) {
                                    fwHeader.put(fieldName, objectToString(firstRow.get(fieldIndex)));
                                }
                            }
                        }
                    }
                }
                return fwHeader;
            }
        }
        return Collections.emptyMap();
    }

    /**
     * フィールド一覧の中で指定フィールド名のインデックスを返す。
     *
     * @param fields    フィールド一覧
     * @param fieldName 検索するフィールド名
     * @return インデックス（見つからない場合は -1）
     */
    private int fieldIndexOf(List<Object> fields, String fieldName) {
        for (int i = 0; i < fields.size(); i++) {
            Map<String, Object> field = castMap(fields.get(i));
            if (fieldName.equals(toString(field.get(FIELD_NAME)))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * DataType から YAML セクションキーへ変換する。
     *
     * @param dataType DataType
     * @return セクションキー
     */
    private String dataTypeToSectionKey(DataType dataType) {
        switch (dataType) {
            case MESSAGE:                          return KEY_MESSAGES;
            case EXPECTED_REQUEST_HEADER_MESSAGES: return KEY_EXPECTED_REQUEST_HEADER_MESSAGES;
            case EXPECTED_REQUEST_BODY_MESSAGES:   return KEY_EXPECTED_REQUEST_BODY_MESSAGES;
            case RESPONSE_HEADER_MESSAGES:         return KEY_RESPONSE_HEADER_MESSAGES;
            case RESPONSE_BODY_MESSAGES:           return KEY_RESPONSE_BODY_MESSAGES;
            default:
                throw new IllegalArgumentException("Unsupported DataType for messaging: " + dataType);
        }
    }

    /**
     * YAML オブジェクトを文字列に変換する（RS-03〜RS-05）。
     *
     * <ul>
     * <li>null → null（RS-03: SnakeYAML が YAML ネイティブ null を Java null に変換し、そのまま返す）</li>
     * <li>Boolean → "true" / "false"（RS-04）</li>
     * <li>Integer / Long / Double 等の数値 → 数字文字列（RS-05）</li>
     * <li>その他 → {@code toString()}</li>
     * </ul>
     *
     * @param value YAML オブジェクト
     * @return 文字列表現（null の場合は null）
     */
    private String objectToString(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * インタープリタチェーンを適用して値を変換する。
     *
     * @param value   変換前の値（null 可）
     * @param interps 使用するインタープリタリスト
     * @return 変換後の値
     */
    private String interpret(String value, List<TestDataInterpreter> interps) {
        if (value == null) {
            return null;
        }
        if (interps == null || interps.isEmpty()) {
            return value;
        }
        InterpretationContext ctx = new InterpretationContext(value, interps);
        return ctx.invokeNext();
    }

    /**
     * {@link BinaryFileInterpreter} をインタープリタリストの先頭に積んで返す。
     *
     * @param path ベースパス
     * @return BinaryFileInterpreter を先頭に追加したリスト
     */
    private List<TestDataInterpreter> addBinaryFileInterpreter(String path) {
        BinaryFileInterpreter fileInterpreter = new BinaryFileInterpreter(path);
        List<TestDataInterpreter> newInterpreters = new ArrayList<TestDataInterpreter>(
                (interpreters != null ? interpreters.size() : 0) + 1);
        newInterpreters.add(fileInterpreter);
        if (interpreters != null) {
            newInterpreters.addAll(interpreters);
        }
        return newInterpreters;
    }

    /**
     * YAML Map から指定キーのリストを取得する。
     * キーが存在しない場合や値が null の場合は空リストを返す。
     *
     * @param map YAML Map
     * @param key キー
     * @return リスト
     */
    @SuppressWarnings("unchecked")
    private List<Object> getList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List) {
            return (List<Object>) val;
        }
        return Collections.emptyList();
    }

    /**
     * Object を {@code Map<String, Object>} にキャストする。
     *
     * @param obj キャスト対象
     * @return Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return Collections.emptyMap();
    }

    /**
     * Object を文字列に変換する（null の場合は null）。
     *
     * @param value 変換対象
     * @return 文字列
     */
    private String toString(Object value) {
        return value != null ? value.toString() : null;
    }
}
