package nablarch.test.core.reader.yaml;

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
import nablarch.test.core.reader.yaml.model.RawDataFile;
import nablarch.test.core.reader.yaml.model.RawFieldDef;
import nablarch.test.core.reader.yaml.model.RawListMap;
import nablarch.test.core.reader.yaml.model.RawMessage;
import nablarch.test.core.reader.yaml.model.RawRecordLayout;
import nablarch.test.core.reader.yaml.model.RawTableData;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static nablarch.test.core.reader.yaml.YamlSection.DEFAULT_RECORD_TYPE;
import static nablarch.test.core.reader.yaml.YamlSection.FILE_TYPE_FIXED;
import static nablarch.test.core.reader.yaml.YamlSection.FW_HEADER_RECORD_TYPE;
import static nablarch.test.core.reader.yaml.YamlSection.addBinaryFileInterpreter;
import static nablarch.test.core.reader.yaml.YamlSection.interpret;
import static nablarch.test.core.reader.yaml.YamlSection.objectToString;

/**
 * 構造マッピング層が返した生の構造レコード（{@code Raw*}）に値加工を施し、本体の器
 * （{@link TableData}／{@link DataFile}／{@link MessagePool}）を組み立てる値加工＋組み立て層。
 *
 * <p>
 * 本クラスは <b>本体テスト読み込み専用</b>である。特殊記法（{@code ${...}}）の解釈・
 * {@code ${binaryFile:}} の basePath 解決・デフォルト値補完・メッセージ長の {@code -} 注入・
 * マーカーカラム除外・グループ ID 絞り込みといった「値・組み立て」に関する責務をすべて担う。
 * 構造マッピング層（{@code Yaml*StructureMapper}）は本クラスの存在を一切知らない（純粋）。
 * </p>
 *
 * <p>
 * 変換ツールは本クラスを経由せず、構造マッピング層が返す {@code Raw*} を直接利用する。
 * </p>
 *
 * @author kiyotis
 */
public final class YamlValueProcessor {

    private final DbInfo dbInfo;
    private final DefaultValues defaultValues;
    private final List<TestDataInterpreter> interpreters;

    /**
     * コンストラクタ。
     *
     * @param dbInfo        DB 情報（テーブル構築に使用）
     * @param defaultValues デフォルト値設定（{@code fillDefaultValues} に使用）
     * @param interpreters  インタープリタプロトタイプ（{@code ${binaryFile:}} は basePath 付きで都度先頭に積む）
     */
    public YamlValueProcessor(DbInfo dbInfo, DefaultValues defaultValues,
                              List<TestDataInterpreter> interpreters) {
        this.dbInfo = dbInfo;
        this.defaultValues = defaultValues;
        this.interpreters = interpreters;
    }

    // ========================================================================
    // テーブル
    // ========================================================================

    /**
     * {@link RawTableData} 群から、指定グループの {@link TableData} を組み立てる。
     *
     * @param raws         構造マッピング層が返した生のテーブルデータ群
     * @param sectionKey   セクションキー（例外メッセージ用）
     * @param groupId      整形済みグループ ID（例: {@code "[case01]"} または {@code ""}）
     * @param fillDefaults true の場合 {@link TableData#fillDefaultValues()} を適用する
     * @param basePath     インタープリタ用ベースパス
     * @return TableData リスト
     */
    public List<TableData> toTableDataList(List<RawTableData> raws, String sectionKey, String groupId,
                                           boolean fillDefaults, String basePath) {
        List<TableData> result = new ArrayList<TableData>();
        List<TestDataInterpreter> interps = addBinaryFileInterpreter(basePath, interpreters);
        for (RawTableData raw : raws) {
            if (!groupMatches(raw.getGroupId(), groupId)) {
                continue;
            }
            if (raw.getTableName() == null) {
                throw new IllegalStateException(
                        "Missing required field 'table' in " + sectionKey + " entry. groupId=" + groupId
                                + ", basePath=" + basePath);
            }
            if (raw.getRows().isEmpty()) {
                continue;
            }
            result.add(buildTableData(raw, fillDefaults, interps));
        }
        return result;
    }

    private TableData buildTableData(RawTableData raw, boolean fillDefaults, List<TestDataInterpreter> interps) {
        List<String> cols = raw.getColumnNames();
        List<String> dataColumns = new ArrayList<String>();
        List<Integer> dataColumnIndexes = new ArrayList<Integer>();
        for (int i = 0; i < cols.size(); i++) {
            if (!isMarker(cols.get(i))) {
                dataColumns.add(cols.get(i));
                dataColumnIndexes.add(i);
            }
        }
        TableData td = new TableData(dbInfo, raw.getTableName(),
                dataColumns.toArray(new String[0]), defaultValues);
        for (List<String> rawRow : raw.getRows()) {
            if (rawRow.isEmpty()) {
                // 空マッピング（{}）行はデータ行として扱わない（旧 buildTableDataList の rowMap.isEmpty() スキップ相当）。
                continue;
            }
            List<String> values = new ArrayList<String>(dataColumnIndexes.size());
            for (int idx : dataColumnIndexes) {
                values.add(interpret(rawRow.get(idx), interps));
            }
            td.addRow(values);
        }
        if (fillDefaults) {
            td.fillDefaultValues();
        }
        return td;
    }

    // ========================================================================
    // list_maps
    // ========================================================================

    /**
     * {@link RawListMap} 群から、指定 ID の行リストを組み立てる。
     *
     * <p>
     * 出力 Map のキー順は従来どおり {@link TreeMap} でソートする（本体読み込みの振る舞い不変）。
     * YAML 記述順は構造マッピング層の {@link RawListMap} が保持しており、変換ツールはそちらを使う。
     * マーカーカラム（{@code [COL]}）は DB 操作対象外として除外する。
     * </p>
     *
     * @param raws     構造マッピング層が返した生の list_maps 群
     * @param id       list_maps エントリの id
     * @param basePath インタープリタ用ベースパス
     * @return 行リスト（見つからない場合は空リスト）
     */
    public List<Map<String, String>> toListMapRows(List<RawListMap> raws, String id, String basePath) {
        List<TestDataInterpreter> interps = addBinaryFileInterpreter(basePath, interpreters);
        for (RawListMap raw : raws) {
            if (id.equals(raw.getId())) {
                return buildListMapRows(raw, interps);
            }
        }
        return Collections.emptyList();
    }

    private List<Map<String, String>> buildListMapRows(RawListMap raw, List<TestDataInterpreter> interps) {
        List<String> cols = raw.getColumnNames();
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        for (List<String> rawRow : raw.getRows()) {
            Map<String, String> row = new TreeMap<String, String>();
            // 空マッピング（{}）行は空の行として保持する（旧 buildRows は空マップを空行として追加した）。
            if (!rawRow.isEmpty()) {
                for (int i = 0; i < cols.size(); i++) {
                    String col = cols.get(i);
                    if (isMarker(col)) {
                        continue;
                    }
                    row.put(col, interpret(rawRow.get(i), interps));
                }
            }
            result.add(row);
        }
        return result;
    }

    // ========================================================================
    // ファイル
    // ========================================================================

    /**
     * {@link RawDataFile} 群から、指定グループの {@link DataFile} を組み立てる。
     *
     * @param raws       構造マッピング層が返した生のファイルデータ群
     * @param sectionKey セクションキー（例外メッセージ用）
     * @param groupId    整形済みグループ ID
     * @param basePath   インタープリタ用ベースパス
     * @return DataFile リスト
     */
    public List<DataFile> toDataFileList(List<RawDataFile> raws, String sectionKey,
                                         String groupId, String basePath) {
        List<DataFile> result = new ArrayList<DataFile>();
        List<TestDataInterpreter> interps = addBinaryFileInterpreter(basePath, interpreters);
        for (RawDataFile raw : raws) {
            if (!groupMatches(raw.getGroupId(), groupId)) {
                continue;
            }
            if (raw.getPath() == null) {
                throw new IllegalStateException(
                        "Missing required field 'path' in " + sectionKey + " entry. groupId=" + groupId
                                + ", basePath=" + basePath);
            }
            DataFile file = FILE_TYPE_FIXED.equals(raw.getFileType())
                    ? new FixedLengthFile(raw.getPath())
                    : new VariableLengthFile(raw.getPath());
            applyDirectives(file, raw.getDirectives());
            buildFragments(file, raw.getRecords(), false, interps);
            result.add(file);
        }
        return result;
    }

    // ========================================================================
    // メッセージ
    // ========================================================================

    /**
     * {@link RawMessage} 群から、指定 ID の {@link MessagePool} を組み立てる。
     *
     * @param raws        構造マッピング層が返した生のメッセージ群
     * @param id          メッセージ ID
     * @param useFwHeader {@code fw_header:} を使用するか（{@code messages} 経路のみ true。その他は空 Map）
     * @param basePath    インタープリタ用ベースパス
     * @return {@link RequestTestingMessagePool}、または存在しない場合 null
     */
    public MessagePool toMessagePool(List<RawMessage> raws, String id, boolean useFwHeader, String basePath) {
        List<TestDataInterpreter> interps = addBinaryFileInterpreter(basePath, interpreters);
        for (RawMessage raw : raws) {
            if (id.equals(raw.getId())) {
                FixedLengthFile file = new FixedLengthFile(id);
                applyDirectives(file, raw.getDirectives());
                buildFragments(file, raw.getRecords(), true, interps);
                Map<String, String> fwHeader = useFwHeader
                        ? convertFwHeader(raw.getFwHeader(), id)
                        : Collections.<String, String>emptyMap();
                return new RequestTestingMessagePool(file, fwHeader);
            }
        }
        return null;
    }

    /**
     * {@link RawMessage} 群から、指定グループの SendSync 用メッセージリストを組み立てる。
     *
     * @param raws     構造マッピング層が返した生のメッセージ群
     * @param groupId  グループ ID（生値で一致比較する）
     * @param basePath インタープリタ用ベースパス
     * @return {@link RequestTestingMessagePool} リスト、または存在しない場合 null
     */
    public List<RequestTestingMessagePool> toSendSyncList(List<RawMessage> raws, String groupId, String basePath) {
        List<TestDataInterpreter> interps = addBinaryFileInterpreter(basePath, interpreters);
        List<RequestTestingMessagePool> result = new ArrayList<RequestTestingMessagePool>();
        for (RawMessage raw : raws) {
            if (raw.getGroupId() != null && raw.getGroupId().equals(groupId)) {
                MockMessages file = new MockMessages(raw.getId() != null ? raw.getId() : "");
                applyDirectives(file, raw.getDirectives());
                buildFragments(file, raw.getRecords(), true, interps);
                RequestTestingMessagePool pool = new RequestTestingMessagePool(file, Collections.<String, String>emptyMap());
                if (raw.getId() != null) {
                    pool.setRequestId(raw.getId());
                }
                result.add(pool);
            }
        }
        return result.isEmpty() ? null : result;
    }

    // ========================================================================
    // 共通: フラグメント組み立て
    // ========================================================================

    /**
     * {@link RawRecordLayout} 群から {@link DataFileFragment} を組み立ててファイルに追加する。
     *
     * @param file         ファイル
     * @param records      生のレコードレイアウト群
     * @param skipFwHeader true の場合 FW_HEADER レコードをスキップし、record_type を {@code "default"} に固定し、
     *                     長さ未指定フィールドを {@code "-"}（動的計算）として扱う（メッセージ系）
     * @param interps      使用するインタープリタリスト
     */
    private void buildFragments(DataFile file, List<RawRecordLayout> records,
                                boolean skipFwHeader, List<TestDataInterpreter> interps) {
        for (RawRecordLayout record : records) {
            String recordType = record.getRecordType();
            if (skipFwHeader && FW_HEADER_RECORD_TYPE.equals(recordType)) {
                continue;
            }

            DataFileFragment fragment = file.getNewFragment();
            fragment.setRecordType(skipFwHeader
                    ? DEFAULT_RECORD_TYPE
                    : (recordType != null ? recordType : DEFAULT_RECORD_TYPE));

            List<RawFieldDef> fields = record.getFields();
            List<String> names = new ArrayList<String>(fields.size());
            List<String> types = new ArrayList<String>(fields.size());
            List<String> lengths = new ArrayList<String>(fields.size());
            boolean hasLength = false;
            for (RawFieldDef field : fields) {
                names.add(field.getName());
                types.add(field.getType());
                if (field.getLength() != null) {
                    hasLength = true;
                    lengths.add(field.getLength());
                } else {
                    lengths.add(null);
                }
            }

            fragment.setNames(names);
            fragment.setTypes(types);

            // メッセージファイル（skipFwHeader=true）は常に固定長のため setLengths が必要。
            // それ以外は length フィールドが 1 件以上ある場合のみ setLengths を呼ぶ。
            if (skipFwHeader || hasLength) {
                List<String> cleanedLengths = new ArrayList<String>(lengths.size());
                for (String l : lengths) {
                    // skipFwHeader=true（メッセージ）の場合 length 未指定フィールドを "-"（動的計算）として扱う。
                    cleanedLengths.add(l != null ? l : (skipFwHeader ? "-" : ""));
                }
                fragment.setLengths(cleanedLengths);
            }

            for (List<String> rawRow : record.getRows()) {
                List<String> rowValues = new ArrayList<String>(rawRow.size());
                for (String cell : rawRow) {
                    rowValues.add(interpret(cell, interps));
                }
                fragment.addValue(rowValues);
            }
        }
    }

    /**
     * 生の {@code fw_header} 値を検証・文字列化して {@code Map<String,String>} へ変換する（{@code messages} 経路のみ呼ばれる）。
     *
     * <p>
     * 値は文字列化のみで解釈（interpret）はしない。マップ以外が指定された場合は ID 付きで
     * {@link IllegalStateException} を投げる。検証は読み出すメッセージに対してのみ遅延実行される。
     * </p>
     *
     * @param fwHeaderObj 生の fw_header 値（マップ／その他／null）
     * @param id          メッセージ ID（例外メッセージ用）
     * @return FW 制御ヘッダ Map（省略時・null 時は空 Map）
     */
    private Map<String, String> convertFwHeader(Object fwHeaderObj, String id) {
        if (fwHeaderObj == null) {
            return Collections.emptyMap();
        }
        if (!(fwHeaderObj instanceof Map)) {
            throw new IllegalStateException(
                    "fw_header in message entry id='" + id + "' must be a map, "
                            + "but was: " + fwHeaderObj.getClass().getSimpleName());
        }
        Map<String, String> fwHeader = new LinkedHashMap<String, String>();
        Map<?, ?> rawMap = (Map<?, ?>) fwHeaderObj;
        for (Map.Entry<?, ?> kv : rawMap.entrySet()) {
            fwHeader.put(objectToString(kv.getKey()), objectToString(kv.getValue()));
        }
        return fwHeader;
    }

    /**
     * ディレクティブ Map を {@link DataFile} に適用する。
     */
    private void applyDirectives(DataFile file, Map<String, String> directives) {
        for (Map.Entry<String, String> e : directives.entrySet()) {
            file.setDirective(e.getKey(), e.getValue());
        }
    }

    /**
     * 整形済みグループ ID（{@code "[xxx]"} または {@code ""}）と生のグループ ID が一致するか。
     */
    private boolean groupMatches(String rawGroupId, String requestedFormatted) {
        String formatted = rawGroupId != null ? "[" + rawGroupId + "]" : "";
        return requestedFormatted.equals(formatted);
    }

    /**
     * マーカーカラム（{@code [COL]} 形式）か判定する。
     */
    private boolean isMarker(String column) {
        return column != null && column.startsWith("[") && column.endsWith("]");
    }
}
