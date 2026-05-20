package nablarch.test.core.reader;

import nablarch.core.util.StringUtil;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * YAMLファイルからテストデータを読み込む {@link TestDataReader} 実装。
 *
 * <p>
 * {@link #open(String, String)} に指定された {@code dataName} に対して
 * {@code {path}/{dataName}.yaml} を検索して読み込む。
 * </p>
 *
 * <p>
 * YAML ネイティブ型の変換ルール:
 * <ul>
 *   <li>{@code null} → 文字列 {@code "null"}</li>
 *   <li>{@code true}/{@code false} → 文字列 {@code "true"}/{@code "false"}</li>
 *   <li>整数/浮動小数点 → 数字文字列</li>
 *   <li>各行の末尾が省略された列は {@code ""} で補完</li>
 * </ul>
 * </p>
 */
public class YamlTestDataReader implements TestDataReader {

    /** DataType 名と YAML トップレベルキーのマッピング */
    private static final List<SectionType> SECTION_TYPES = buildSectionTypes();

    /** 読み込んだ行シーケンス */
    private List<List<String>> rows;

    /** 現在の読み込み位置 */
    private int index;

    @Override
    public void open(String path, String dataName) {
        if (StringUtil.isNullOrEmpty(dataName)) {
            throw new IllegalArgumentException("dataName must not be null or empty.");
        }

        File file = new File(path, dataName + ".yaml");
        if (!file.exists()) {
            throw new RuntimeException("YAML test data file not found: " + file.getAbsolutePath());
        }

        Map<String, Object> yaml = loadYaml(file);
        rows = buildRows(yaml);
        index = 0;
    }

    @Override
    public void close() {
        rows = null;
        index = 0;
    }

    @Override
    public List<String> readLine() {
        if (rows == null || index >= rows.size()) {
            return null;
        }
        return rows.get(index++);
    }

    @Override
    public boolean isResourceExisting(String basePath, String resourceName) {
        return new File(basePath, resourceName + ".yaml").exists();
    }

    @Override
    public boolean isDataExisting(String basePath, String resourceName) {
        return new File(basePath, resourceName + ".yaml").exists();
    }

    // -----------------------------------------------------------------------
    // YAML ロード
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(File file) {
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(options));
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Object result = yaml.load(reader);
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            }
            return Collections.emptyMap();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YAML file: " + file.getAbsolutePath(), e);
        }
    }

    // -----------------------------------------------------------------------
    // YAML → 行シーケンス変換
    // -----------------------------------------------------------------------

    private List<List<String>> buildRows(Map<String, Object> yaml) {
        List<List<String>> result = new ArrayList<List<String>>();
        for (SectionType st : SECTION_TYPES) {
            Object entries = yaml.get(st.yamlKey);
            if (entries == null) {
                continue;
            }
            for (Object entry : asList(entries)) {
                Map<String, Object> entryMap = asMap(entry);
                st.converter.convert(entryMap, result);
            }
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // セクション種別定義
    // -----------------------------------------------------------------------

    private interface RowConverter {
        void convert(Map<String, Object> entry, List<List<String>> out);
    }

    private static class SectionType {
        final String yamlKey;
        final RowConverter converter;

        SectionType(String yamlKey, RowConverter converter) {
            this.yamlKey = yamlKey;
            this.converter = converter;
        }
    }

    private static List<SectionType> buildSectionTypes() {
        List<SectionType> list = new ArrayList<SectionType>();

        // テーブル系（GroupData）
        list.add(new SectionType("setup_tables",
                new TableRowConverter("SETUP_TABLE")));
        list.add(new SectionType("expected_tables",
                new TableRowConverter("EXPECTED_TABLE")));
        list.add(new SectionType("expected_complete_tables",
                new TableRowConverter("EXPECTED_COMPLETE_TABLE")));

        // LIST_MAP（SingleData）
        list.add(new SectionType("list_maps",
                new ListMapRowConverter()));

        // ファイル系（GroupData）
        list.add(new SectionType("setup_files",
                new FileRowConverter("setup_files")));
        list.add(new SectionType("expected_files",
                new FileRowConverter("expected_files")));

        // メッセージ系（SingleData）
        list.add(new SectionType("messages",
                new MessageRowConverter("MESSAGE")));
        list.add(new SectionType("expected_request_header_messages",
                new MessageRowConverter("EXPECTED_REQUEST_HEADER_MESSAGES")));
        list.add(new SectionType("expected_request_body_messages",
                new MessageRowConverter("EXPECTED_REQUEST_BODY_MESSAGES")));

        // GroupMessage 系（GroupData）
        list.add(new SectionType("response_header_messages",
                new GroupMessageRowConverter("RESPONSE_HEADER_MESSAGES")));
        list.add(new SectionType("response_body_messages",
                new GroupMessageRowConverter("RESPONSE_BODY_MESSAGES")));

        return Collections.unmodifiableList(list);
    }

    // -----------------------------------------------------------------------
    // テーブル系コンバータ
    // -----------------------------------------------------------------------

    private static class TableRowConverter implements RowConverter {
        private final String dataTypeName;

        TableRowConverter(String dataTypeName) {
            this.dataTypeName = dataTypeName;
        }

        @Override
        public void convert(Map<String, Object> entry, List<List<String>> out) {
            String groupId = asString(entry.get("group_id"));
            String tableName = asString(entry.get("table"));

            // セクションヘッダ行: ["SETUP_TABLE[groupId]=TABLE_NAME"] or ["SETUP_TABLE=TABLE_NAME"]
            String header = groupId == null
                    ? dataTypeName + "=" + tableName
                    : dataTypeName + "[" + groupId + "]=" + tableName;
            out.add(singletonRow(header));

            List<Map<String, Object>> rows = asMapList(entry.get("rows"));
            if (rows.isEmpty()) {
                return;
            }

            // 全行の全キーを union して列順を決定（挿入順を保持）
            Set<String> allKeys = collectAllKeys(rows);

            // カラムヘッダ行: ["", col1, col2, ...]
            List<String> colHeader = new ArrayList<String>();
            colHeader.add("");
            colHeader.addAll(allKeys);
            out.add(colHeader);

            // データ行: ["", val1, val2, ...]
            for (Map<String, Object> row : rows) {
                List<String> dataRow = new ArrayList<String>();
                dataRow.add("");
                for (String key : allKeys) {
                    Object val = row.get(key);
                    // キーが存在しない（省略）場合は null 扱い → "" 補完（RS-06）
                    dataRow.add(toCell(val, !row.containsKey(key)));
                }
                out.add(dataRow);
            }
        }
    }

    // -----------------------------------------------------------------------
    // LIST_MAP コンバータ
    // -----------------------------------------------------------------------

    private static class ListMapRowConverter implements RowConverter {
        @Override
        public void convert(Map<String, Object> entry, List<List<String>> out) {
            String id = asString(entry.get("id"));

            // セクションヘッダ行
            out.add(singletonRow("LIST_MAP=" + id));

            List<Map<String, Object>> rows = asMapList(entry.get("rows"));
            if (rows.isEmpty()) {
                return;
            }

            Set<String> allKeys = collectAllKeys(rows);

            // カラムヘッダ行
            List<String> colHeader = new ArrayList<String>();
            colHeader.add("");
            colHeader.addAll(allKeys);
            out.add(colHeader);

            // データ行
            for (Map<String, Object> row : rows) {
                List<String> dataRow = new ArrayList<String>();
                dataRow.add("");
                for (String key : allKeys) {
                    dataRow.add(toCell(row.get(key), !row.containsKey(key)));
                }
                out.add(dataRow);
            }
        }
    }

    // -----------------------------------------------------------------------
    // ファイル系コンバータ（固定長・可変長）
    // -----------------------------------------------------------------------

    private static class FileRowConverter implements RowConverter {
        private final String yamlKey;

        FileRowConverter(String yamlKey) {
            this.yamlKey = yamlKey;
        }

        @Override
        public void convert(Map<String, Object> entry, List<List<String>> out) {
            String groupId = asString(entry.get("group_id"));
            String path    = asString(entry.get("path"));
            String type    = asString(entry.get("type")); // "fixed" or "variable"

            String dataTypeName = resolveFileDataType(yamlKey, type);

            // セクションヘッダ行
            String header = groupId == null
                    ? dataTypeName + "=" + path
                    : dataTypeName + "[" + groupId + "]=" + path;
            out.add(singletonRow(header));

            // ディレクティブ行
            Map<String, Object> directives = asMap(entry.get("directives"));
            for (Map.Entry<String, Object> d : directives.entrySet()) {
                out.add(Arrays.asList(d.getKey(), toCell(d.getValue(), false)));
            }

            // records
            List<Object> records = asList(entry.get("records"));
            for (Object rec : records) {
                Map<String, Object> recMap = asMap(rec);
                addRecordRows(recMap, type, out);
            }
        }

        private static String resolveFileDataType(String yamlKey, String type) {
            boolean isSetup = yamlKey.startsWith("setup");
            if ("variable".equals(type)) {
                return isSetup ? "SETUP_VARIABLE" : "EXPECTED_VARIABLE";
            }
            return isSetup ? "SETUP_FIXED" : "EXPECTED_FIXED";
        }
    }

    // -----------------------------------------------------------------------
    // メッセージ系コンバータ（SingleData）
    // -----------------------------------------------------------------------

    private static class MessageRowConverter implements RowConverter {
        private final String dataTypeName;

        MessageRowConverter(String dataTypeName) {
            this.dataTypeName = dataTypeName;
        }

        @Override
        public void convert(Map<String, Object> entry, List<List<String>> out) {
            String id = asString(entry.get("id"));
            out.add(singletonRow(dataTypeName + "=" + id));

            // ディレクティブ
            Map<String, Object> directives = asMap(entry.get("directives"));
            for (Map.Entry<String, Object> d : directives.entrySet()) {
                out.add(Arrays.asList(d.getKey(), toCell(d.getValue(), false)));
            }

            List<Object> records = asList(entry.get("records"));
            for (Object rec : records) {
                Map<String, Object> recMap = asMap(rec);
                // messages は固定長のみ
                addRecordRows(recMap, "fixed", out);
            }
        }
    }

    // -----------------------------------------------------------------------
    // GroupMessage コンバータ（RESPONSE_HEADER/BODY_MESSAGES）
    // -----------------------------------------------------------------------

    private static class GroupMessageRowConverter implements RowConverter {
        private final String dataTypeName;

        GroupMessageRowConverter(String dataTypeName) {
            this.dataTypeName = dataTypeName;
        }

        @Override
        public void convert(Map<String, Object> entry, List<List<String>> out) {
            String groupId = asString(entry.get("group_id"));
            String id = asString(entry.get("id"));

            // グループIDがある場合は GroupData 経路
            String header;
            if (groupId != null) {
                header = dataTypeName + "[" + groupId + "]=" + id;
            } else {
                header = dataTypeName + "=" + id;
            }
            out.add(singletonRow(header));

            List<Object> records = asList(entry.get("records"));
            for (Object rec : records) {
                Map<String, Object> recMap = asMap(rec);
                addRecordRows(recMap, "fixed", out);
            }
        }
    }

    // -----------------------------------------------------------------------
    // レコード行生成（固定長・可変長共通）
    // -----------------------------------------------------------------------

    /**
     * record_fragment から行シーケンスを生成する。
     * <pre>
     * フィールド名行: [recordType, field1, field2, ...]
     * 型行:         ["", type1, type2, ...]
     * 長さ行(固定長のみ): ["", len1, len2, ...]
     * 値行:         ["", val1, val2, ...] (rows の各配列)
     * </pre>
     */
    private static void addRecordRows(Map<String, Object> record, String fileType, List<List<String>> out) {
        String recordType = asString(record.get("record_type"));
        List<Object> fields = asList(record.get("fields"));

        List<String> names  = new ArrayList<String>();
        List<String> types  = new ArrayList<String>();
        List<String> lengths = new ArrayList<String>();

        for (Object f : fields) {
            Map<String, Object> field = asMap(f);
            names.add(asString(field.get("name")));
            types.add(asString(field.get("type")));
            Object len = field.get("length");
            lengths.add(len == null ? null : toCell(len, false));
        }

        // フィールド名行: [recordType, name1, name2, ...]
        List<String> namesRow = new ArrayList<String>();
        namesRow.add(recordType != null ? recordType : "");
        namesRow.addAll(names);
        out.add(namesRow);

        // 型行: ["", type1, type2, ...]
        List<String> typesRow = new ArrayList<String>();
        typesRow.add("");
        typesRow.addAll(types);
        out.add(typesRow);

        // 長さ行（固定長のみ）: ["", len1, len2, ...]
        boolean isFixed = !"variable".equals(fileType);
        if (isFixed) {
            List<String> lengthsRow = new ArrayList<String>();
            lengthsRow.add("");
            for (String len : lengths) {
                lengthsRow.add(len != null ? len : "");
            }
            out.add(lengthsRow);
        }

        // 値行: ["", val1, val2, ...]
        List<Object> rowsList = asList(record.get("rows"));
        for (Object rowObj : rowsList) {
            List<Object> valueList = asList(rowObj);
            List<String> valueRow = new ArrayList<String>();
            valueRow.add("");
            int colCount = fields.size();
            for (int i = 0; i < colCount; i++) {
                if (i < valueList.size()) {
                    valueRow.add(toCell(valueList.get(i), false));
                } else {
                    valueRow.add("");  // RS-06: 末尾補完
                }
            }
            out.add(valueRow);
        }
    }

    // -----------------------------------------------------------------------
    // ユーティリティ
    // -----------------------------------------------------------------------

    /**
     * YAML から取得した値を文字列セルに変換する。
     *
     * @param value     YAML 値（null / Boolean / Integer / Long / Double / String）
     * @param isMissing キーが存在しない（省略）場合は true → "" を返す（RS-06 末尾補完）
     */
    private static String toCell(Object value, boolean isMissing) {
        if (isMissing) {
            return "";  // RS-06: 省略キーは空文字
        }
        if (value == null) {
            return "null";  // RS-03: YAML ネイティブ null → "null"
        }
        return String.valueOf(value);  // RS-04/RS-05: boolean/integer/float → 数字文字列
    }

    /** キー1つの行リストを生成する。 */
    private static List<String> singletonRow(String value) {
        List<String> row = new ArrayList<String>(1);
        row.add(value);
        return row;
    }

    /** 全行の全キーを挿入順で収集する（union）。 */
    private static Set<String> collectAllKeys(List<Map<String, Object>> rows) {
        Set<String> keys = new LinkedHashSet<String>();
        for (Map<String, Object> row : rows) {
            keys.addAll(row.keySet());
        }
        return keys;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return new LinkedHashMap<String, Object>();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asMapList(Object obj) {
        if (obj instanceof List) {
            return (List<Map<String, Object>>) obj;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object obj) {
        if (obj instanceof List) {
            return (List<Object>) obj;
        }
        return Collections.emptyList();
    }

    private static String asString(Object obj) {
        if (obj == null) {
            return null;
        }
        return String.valueOf(obj);
    }
}
