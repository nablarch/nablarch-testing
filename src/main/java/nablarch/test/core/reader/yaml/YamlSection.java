package nablarch.test.core.reader.yaml;

import nablarch.test.core.reader.DataType;
import nablarch.test.core.util.interpreter.BinaryFileInterpreter;
import nablarch.test.core.util.interpreter.InterpretationContext;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * YAML セクションキー定数と共通ヘルパーメソッド。
 *
 * <p>
 * {@code nablarch.test.core.reader.yaml} パッケージ内のビルダ（{@code Yaml*Builder}）・
 * {@link nablarch.test.core.reader.YamlTestDataParser}・{@link nablarch.test.core.reader.YamlTestCoreAdapter} から使用する。
 * </p>
 *
 * @author kiyotis
 */
public final class YamlSection {

    // ========================================================================
    // セクションキー定数
    // ========================================================================

    public static final String KEY_SETUP_TABLES = "setup_tables";
    public static final String KEY_EXPECTED_TABLES = "expected_tables";
    public static final String KEY_EXPECTED_COMPLETE_TABLES = "expected_complete_tables";
    public static final String KEY_LIST_MAPS = "list_maps";
    public static final String KEY_SETUP_FILES = "setup_files";
    public static final String KEY_EXPECTED_FILES = "expected_files";
    public static final String KEY_MESSAGES = "messages";
    public static final String KEY_EXPECTED_REQUEST_HEADER_MESSAGES = "expected_request_header_messages";
    public static final String KEY_EXPECTED_REQUEST_BODY_MESSAGES = "expected_request_body_messages";
    public static final String KEY_RESPONSE_HEADER_MESSAGES = "response_header_messages";
    public static final String KEY_RESPONSE_BODY_MESSAGES = "response_body_messages";

    // ========================================================================
    // フィールドキー定数
    // ========================================================================

    public static final String FIELD_GROUP_ID = "group_id";
    public static final String FIELD_ID = "id";
    public static final String FIELD_TABLE = "table";
    public static final String FIELD_ROWS = "rows";
    public static final String FIELD_PATH = "path";
    /** "fixed" / "variable" またはフィールド型 */
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_DIRECTIVES = "directives";
    public static final String FIELD_RECORDS = "records";
    public static final String FIELD_RECORD_TYPE = "record_type";
    public static final String FIELD_FIELDS = "fields";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_LENGTH = "length";

    // ========================================================================
    // ファイル種別定数
    // ========================================================================

    public static final String FILE_TYPE_FIXED = "fixed";

    // ========================================================================
    // メッセージ系定数
    // ========================================================================

    /**
     * FW制御ヘッダレコード種別名。
     *
     * <p>
     * {@link YamlFileBuilder#buildFragments} が messages セクションの records から FW_HEADER レコードを
     * スキップする際に使用する。FW制御ヘッダ自体は {@link #FIELD_FW_HEADER} マップから取得する。
     * </p>
     */
    public static final String FW_HEADER_RECORD_TYPE = "FW_HEADER";

    /** messages エントリ直下の FW 制御ヘッダマップキー */
    public static final String FIELD_FW_HEADER = "fw_header";

    /** フォールバック時に使用するレコードタイプ名。record_type が未指定の場合および skipFwHeader=true の場合に使用する。 */
    public static final String DEFAULT_RECORD_TYPE = "default";

    // ========================================================================
    // ユーティリティメソッド
    // ========================================================================

    private YamlSection() {
    }

    /**
     * YAML Map から指定キーのリストを取得する。値が null またはキー不在の場合は空リストを返す。
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List) {
            return (List<Object>) val;
        }
        return Collections.emptyList();
    }

    /**
     * Object を {@code Map<String, Object>} にキャストする。Map でない場合は空 Map を返す。
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> castMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return Collections.emptyMap();
    }

    /**
     * YAML Map のキー値（パス・ファイル種別・フィールド名等の設定値）を文字列に変換する（null の場合は null）。
     *
     * <p>
     * 設定値取得用。テストデータのセルデータ変換には {@link #objectToString(Object)} を使うこと。
     * </p>
     */
    public static String toStr(Object value) {
        return value != null ? value.toString() : null;
    }

    /**
     * YAML のテストデータ値を文字列に変換する（RS-03〜RS-05）。
     *
     * <ul>
     * <li>null → null（RS-03）</li>
     * <li>Boolean → "true"/"false"（RS-04）</li>
     * <li>数値 → 数字文字列（RS-05）</li>
     * <li>その他 → {@code toString()}</li>
     * </ul>
     *
     * <p>
     * テストデータのセル値変換用。設定値取得には {@link #toStr(Object)} を使うこと。
     * 現在の実装は {@link #toStr(Object)} と同一だが、将来 YAML ネイティブ型の変換仕様が
     * 変わった場合はこちらのみ変更すること（例: 数値フォーマットの変更、null 表現の変換等）。
     * </p>
     */
    public static String objectToString(Object value) {
        return value != null ? value.toString() : null;
    }

    /**
     * 先頭行のキーをカラム名（マーカー含む・YAML 記述順・大文字小文字保持）として決定する。
     * 行が無い場合は空リストを返す。
     *
     * <p>
     * SnakeYAML はマッピングを {@link java.util.LinkedHashMap} でロードするため、{@code keySet} の
     * 順序は YAML 記述順と一致する。
     * </p>
     */
    public static List<String> resolveColumns(List<Object> rows) {
        if (rows.isEmpty()) {
            return new ArrayList<String>();
        }
        return new ArrayList<String>(castMap(rows.get(0)).keySet());
    }

    /**
     * マーカーカラム（{@code [COL]} 形式）か判定する。
     */
    public static boolean isMarker(String column) {
        return column != null && column.startsWith("[") && column.endsWith("]");
    }

    /**
     * インタープリタチェーンを適用して値を変換する。
     */
    public static String interpret(String value, List<TestDataInterpreter> interps) {
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
     * {@link BinaryFileInterpreter} をリストの先頭に積んで返す。
     */
    public static List<TestDataInterpreter> addBinaryFileInterpreter(String path,
                                                               List<TestDataInterpreter> interpreters) {
        BinaryFileInterpreter fileInterpreter = new BinaryFileInterpreter(path);
        List<TestDataInterpreter> result = new ArrayList<TestDataInterpreter>(
                (interpreters != null ? interpreters.size() : 0) + 1);
        result.add(fileInterpreter);
        if (interpreters != null) {
            result.addAll(interpreters);
        }
        return result;
    }

    /**
     * {@link DataType} から YAML セクションキーへ変換する。
     */
    public static String dataTypeToSectionKey(DataType dataType) {
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
}
