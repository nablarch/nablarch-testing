package nablarch.test.core.reader;

import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.db.TableData;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.file.MockMessages;
import nablarch.test.core.file.VariableLengthFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.messaging.RequestTestingMessagePool;
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
 * @author NTF YAML 実装フェーズ
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

    /** type フィールドキー（"fixed" / "variable"） */
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

    /** type（フィールド型）フィールドキー */
    private static final String FIELD_FIELD_TYPE = "type";

    /** length フィールドキー */
    private static final String FIELD_LENGTH = "length";

    /** ファイル種別: 固定長 */
    private static final String FILE_TYPE_FIXED = "fixed";

    /** フレームワーク制御ヘッダのレコードタイプ識別子 */
    private static final String FW_HEADER_RECORD_TYPE = "FW_HEADER";

    /** フレームワーク制御ヘッダフィールド名セット */
    private static final java.util.Set<String> FW_HEADER_FIELDS;
    static {
        java.util.Set<String> s = new java.util.HashSet<String>();
        s.add("requestId");
        s.add("userId");
        s.add("resendFlag");
        s.add("resultCode");
        FW_HEADER_FIELDS = Collections.unmodifiableSet(s);
    }

    /** DbInfo */
    private DbInfo dbInfo;

    /** デフォルト値 */
    private DefaultValues defaultValues;

    /** Interpreter リスト */
    private List<TestDataInterpreter> interpreters;

    /** YAML キャッシュ（path → 解析済み Map） */
    private static final Map<String, Map<String, Object>> YAML_CACHE =
            Collections.synchronizedMap(new java.util.LinkedHashMap<String, Map<String, Object>>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Map<String, Object>> eldest) {
                    return size() > 8;
                }
            });

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

    /** {@inheritDoc} */
    @Override
    public void setDbInfo(DbInfo dbInfo) {
        this.dbInfo = dbInfo;
        super.setDbInfo(dbInfo);
    }

    /** {@inheritDoc} */
    @Override
    public void setInterpreters(List<TestDataInterpreter> interpretersPrototype) {
        this.interpreters = interpretersPrototype;
        super.setInterpreters(interpretersPrototype);
    }

    /** {@inheritDoc} */
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
        return buildTableDataList(yaml, KEY_SETUP_TABLES, gid, false);
    }

    /** {@inheritDoc} */
    @Override
    public List<TableData> getExpectedTableData(String path, String resourceName, String... groupId) {
        Map<String, Object> yaml = loadYaml(path, resourceName);
        String gid = formatGroupId(groupId);
        List<TableData> expected = buildTableDataList(yaml, KEY_EXPECTED_TABLES, gid, false);
        List<TableData> completed = buildTableDataList(yaml, KEY_EXPECTED_COMPLETE_TABLES, gid, true);
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
                return buildListMapRows(map);
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
        FixedLengthFile file = buildMessageFile(yaml, KEY_MESSAGES, id, false);
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
        FixedLengthFile file = buildMessageFile(yaml, sectionKey, id, false);
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
            String expectedGid = "[" + id + "]";
            if (expectedGid.equals("[" + (groupId != null ? groupId : "") + "]") && groupId != null && groupId.equals(id)) {
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
        if (YAML_CACHE.containsKey(filePath)) {
            return YAML_CACHE.get(filePath);
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
     * @return TableData リスト
     */
    private List<TableData> buildTableDataList(Map<String, Object> yaml, String sectionKey,
                                                String groupId, boolean fillDefaults) {
        List<Object> entries = getList(yaml, sectionKey);
        List<TableData> result = new ArrayList<TableData>();
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
                    String interpreted = interpret(strVal);
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
     * @return rows として構築した Map リスト
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, String>> buildListMapRows(Map<String, Object> listMapEntry) {
        List<Object> rows = getList(listMapEntry, FIELD_ROWS);
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        for (Object rowObj : rows) {
            Map<String, Object> rowMap = castMap(rowObj);
            Map<String, String> row = new java.util.TreeMap<String, String>();
            for (Map.Entry<String, Object> e : rowMap.entrySet()) {
                String key = e.getKey();
                // マーカーカラム（[COLNAME] 形式）は除外
                if (key.startsWith("[") && key.endsWith("]")) {
                    continue;
                }
                String val = objectToString(e.getValue());
                String interpreted = interpret(val);
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
     * DataFileFragment を構築してファイルに追加する。
     *
     * @param file     ファイル
     * @param map      セクション Map
     * @param basePath ファイルパス基点
     */
    private void buildFragments(DataFile file, Map<String, Object> map, String basePath) {
        List<Object> records = getList(map, FIELD_RECORDS);
        for (Object recordObj : records) {
            Map<String, Object> record = castMap(recordObj);
            nablarch.test.core.file.DataFileFragment fragment = file.getNewFragment();

            String recordType = toString(record.get(FIELD_RECORD_TYPE));
            fragment.setRecordType(recordType != null ? recordType : "default");

            List<Object> fields = getList(record, FIELD_FIELDS);
            List<String> names = new ArrayList<String>(fields.size());
            List<String> types = new ArrayList<String>(fields.size());
            List<String> lengths = new ArrayList<String>(fields.size());
            boolean hasLength = false;

            for (Object fieldObj : fields) {
                Map<String, Object> field = castMap(fieldObj);
                names.add(toString(field.get(FIELD_NAME)));
                types.add(toString(field.get(FIELD_FIELD_TYPE)));
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
            if (hasLength) {
                // null を含む場合は空文字に変換
                List<String> cleanedLengths = new ArrayList<String>(lengths.size());
                for (String l : lengths) {
                    cleanedLengths.add(l != null ? l : "");
                }
                fragment.setLengths(cleanedLengths);
            }

            // データ行を追加
            List<Object> rows = getList(record, FIELD_ROWS);
            for (Object rowObj : rows) {
                if (rowObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> rowList = (List<Object>) rowObj;
                    List<String> rowValues = new ArrayList<String>(rowList.size());
                    for (Object val : rowList) {
                        String strVal = objectToString(val);
                        rowValues.add(interpret(strVal));
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
     * @return FixedLengthFile、または存在しない場合 null
     */
    private FixedLengthFile buildMessageFile(Map<String, Object> yaml, String sectionKey,
                                              String id, boolean isMock) {
        List<Object> entries = getList(yaml, sectionKey);
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String entryId = toString(map.get(FIELD_ID));
            if (id.equals(entryId)) {
                FixedLengthFile file = isMock ? new MockMessages(id) : new FixedLengthFile(id);
                applyDirectives(file, map);
                buildFragmentsForMessage(file, map);
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
     * メッセージ系フラグメントを構築する（record_type を "default" に固定）。
     *
     * @param file ファイル
     * @param map  セクション Map
     */
    private void buildFragmentsForMessage(FixedLengthFile file, Map<String, Object> map) {
        List<Object> records = getList(map, FIELD_RECORDS);
        for (Object recordObj : records) {
            Map<String, Object> record = castMap(recordObj);
            // FW_HEADER レコードはフラグメントに含めない（fwHeader として分離）
            String recordType = toString(record.get(FIELD_RECORD_TYPE));
            if (FW_HEADER_RECORD_TYPE.equals(recordType)) {
                continue;
            }
            nablarch.test.core.file.DataFileFragment fragment = file.getNewFragment();
            // MessageParser は record_type を "default" に上書きする
            fragment.setRecordType("default");

            List<Object> fields = getList(record, FIELD_FIELDS);
            List<String> names = new ArrayList<String>(fields.size());
            List<String> types = new ArrayList<String>(fields.size());
            List<String> lengths = new ArrayList<String>(fields.size());

            for (Object fieldObj : fields) {
                Map<String, Object> field = castMap(fieldObj);
                String fieldName = toString(field.get(FIELD_NAME));
                // FW 制御ヘッダはフラグメントに含めない（fwHeader として分離）
                // ただし YAML 実装では全フィールドをフラグメントに含める
                names.add(fieldName);
                types.add(toString(field.get(FIELD_FIELD_TYPE)));
                Object len = field.get(FIELD_LENGTH);
                lengths.add(len != null ? toString(len) : "0");
            }

            fragment.setNames(names);
            fragment.setTypes(types);
            fragment.setLengths(lengths);

            List<Object> rows = getList(record, FIELD_ROWS);
            for (Object rowObj : rows) {
                if (rowObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> rowList = (List<Object>) rowObj;
                    List<String> rowValues = new ArrayList<String>(rowList.size());
                    for (Object val : rowList) {
                        String strVal = objectToString(val);
                        rowValues.add(interpret(strVal));
                    }
                    fragment.addValue(rowValues);
                }
            }
        }
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
                        if (FW_HEADER_FIELDS.contains(fieldName)) {
                            // 最初の行の値を FW ヘッダとして取得
                            if (!rows.isEmpty()) {
                                @SuppressWarnings("unchecked")
                                List<Object> firstRow = (List<Object>) rows.get(0);
                                int fieldIndex = fieldIndexOf(fields, fieldName);
                                if (fieldIndex < firstRow.size()) {
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
     * <li>null → null（RS-03: Java null として返す）</li>
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
     * @param value 変換前の値（null 可）
     * @return 変換後の値
     */
    private String interpret(String value) {
        if (value == null) {
            return null;
        }
        if (interpreters == null || interpreters.isEmpty()) {
            return value;
        }
        InterpretationContext ctx = new InterpretationContext(value, interpreters);
        return ctx.invokeNext();
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
