package nablarch.test.core.reader.yaml;

import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.db.TableData;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static nablarch.test.core.reader.yaml.YamlSection.FIELD_GROUP_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_ROWS;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_TABLE;
import static nablarch.test.core.reader.yaml.YamlSection.KEY_LIST_MAPS;
import static nablarch.test.core.reader.yaml.YamlSection.castMap;
import static nablarch.test.core.reader.yaml.YamlSection.getList;
import static nablarch.test.core.reader.yaml.YamlSection.interpret;
import static nablarch.test.core.reader.yaml.YamlSection.isMarker;
import static nablarch.test.core.reader.yaml.YamlSection.objectToString;
import static nablarch.test.core.reader.yaml.YamlSection.resolveColumns;
import static nablarch.test.core.reader.yaml.YamlSection.toStr;

/**
 * YAML のテーブル系セクション（{@code setup_tables}／{@code expected_tables}／
 * {@code expected_complete_tables}／{@code list_maps}）から、本体の器（{@link TableData} および
 * list_maps 行リスト）を直接組み立てるビルダ。
 *
 * <p>
 * YAML トップレベル Map（{@link YamlLoader#load} が返す順序保持 Map）を走査し、構造の写し取りと
 * 値加工（特殊記法 {@code ${...}} の解釈・{@code ${binaryFile:}} の basePath 解決・マーカーカラム除外・
 * デフォルト値補完・グループ ID 絞り込み）を一括で行う。カラム名は各エントリ先頭行のキー
 * （マーカー含む・YAML 記述順）から決定する。
 * </p>
 *
 * @author kiyotis
 */
public final class YamlTableDataBuilder {

    private final DbInfo dbInfo;
    private final DefaultValues defaultValues;
    private final InterpreterResolver interpreterResolver;

    /**
     * コンストラクタ。
     *
     * @param dbInfo              DB 情報（テーブル構築に使用）
     * @param defaultValues       デフォルト値設定（{@code fillDefaultValues} に使用）
     * @param interpreterResolver basePath ごとに値加工インタープリタチェーンを解決する戦略
     */
    public YamlTableDataBuilder(DbInfo dbInfo, DefaultValues defaultValues,
                                InterpreterResolver interpreterResolver) {
        this.dbInfo = dbInfo;
        this.defaultValues = defaultValues;
        this.interpreterResolver = interpreterResolver;
    }

    /**
     * テーブル系セクションから指定グループの {@link TableData} 群を組み立てる。
     *
     * @param yaml         YAML トップレベル Map
     * @param sectionKey   セクションキー（例: {@code "setup_tables"}）
     * @param groupId      整形済みグループ ID（例: {@code "[case01]"} または {@code ""}）
     * @param fillDefaults true の場合 {@link TableData#fillDefaultValues()} を適用する
     * @param basePath     インタープリタ用ベースパス
     * @return TableData リスト
     */
    public List<TableData> buildTableDataList(Map<String, Object> yaml, String sectionKey, String groupId,
                                              boolean fillDefaults, String basePath) {
        List<TableData> result = new ArrayList<TableData>();
        List<TestDataInterpreter> interps = interpreterResolver.resolve(basePath);
        for (Object entry : getList(yaml, sectionKey)) {
            Map<String, Object> map = castMap(entry);
            if (!groupMatches(toStr(map.get(FIELD_GROUP_ID)), groupId)) {
                continue;
            }
            String tableName = toStr(map.get(FIELD_TABLE));
            if (tableName == null) {
                throw new IllegalStateException(
                        "Missing required field 'table' in " + sectionKey + " entry. groupId=" + groupId
                                + ", basePath=" + basePath);
            }
            List<Object> rows = getList(map, FIELD_ROWS);
            List<String> columnNames = resolveColumns(rows);
            List<List<String>> rawRows = extractRows(rows, columnNames);
            if (rawRows.isEmpty()) {
                continue;
            }
            result.add(buildTableData(tableName, columnNames, rawRows, fillDefaults, interps));
        }
        return result;
    }

    private TableData buildTableData(String tableName, List<String> cols, List<List<String>> rawRows,
                                     boolean fillDefaults, List<TestDataInterpreter> interps) {
        List<String> dataColumns = new ArrayList<String>();
        List<Integer> dataColumnIndexes = new ArrayList<Integer>();
        for (int i = 0; i < cols.size(); i++) {
            if (!isMarker(cols.get(i))) {
                dataColumns.add(cols.get(i));
                dataColumnIndexes.add(i);
            }
        }
        TableData td = new TableData(dbInfo, tableName, dataColumns.toArray(new String[0]), defaultValues);
        for (List<String> rawRow : rawRows) {
            if (rawRow.isEmpty()) {
                // 空マッピング（{}）行はデータ行として扱わない。
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

    /**
     * {@code list_maps} から指定 ID の行リストを組み立てる。
     *
     * <p>
     * 出力 Map のキー順は従来どおり {@link TreeMap} でソートする（本体読み込みの振る舞い不変）。
     * マーカーカラム（{@code [COL]}）は DB 操作対象外として除外する。
     * </p>
     *
     * @param yaml     YAML トップレベル Map
     * @param id       list_maps エントリの id
     * @param basePath インタープリタ用ベースパス
     * @return 行リスト（見つからない場合は空リスト）
     */
    public List<Map<String, String>> buildListMapRows(Map<String, Object> yaml, String id, String basePath) {
        List<TestDataInterpreter> interps = interpreterResolver.resolve(basePath);
        for (Object entry : getList(yaml, KEY_LIST_MAPS)) {
            Map<String, Object> map = castMap(entry);
            if (id.equals(toStr(map.get(FIELD_ID)))) {
                List<Object> rows = getList(map, FIELD_ROWS);
                List<String> columnNames = resolveColumns(rows);
                return buildListMapRows(columnNames, extractRows(rows, columnNames), interps);
            }
        }
        return Collections.emptyList();
    }

    private List<Map<String, String>> buildListMapRows(List<String> cols, List<List<String>> rawRows,
                                                       List<TestDataInterpreter> interps) {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        for (List<String> rawRow : rawRows) {
            Map<String, String> row = new TreeMap<String, String>();
            // 空マッピング（{}）行は空の行として保持する。
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

    /**
     * 各行をカラム名に揃えた未加工値リストへ写す。
     *
     * <p>
     * マッピングでない行（スカラ等）は構造を持たないため除外する。空マッピング（{@code {}}）は
     * 空リストとして保持する（行の有無を後段が判別できるようにするため）。
     * </p>
     */
    private static List<List<String>> extractRows(List<Object> rows, List<String> columnNames) {
        List<List<String>> rawRows = new ArrayList<List<String>>();
        for (Object rowObj : rows) {
            if (!(rowObj instanceof Map)) {
                continue;
            }
            Map<String, Object> rowMap = castMap(rowObj);
            if (rowMap.isEmpty()) {
                rawRows.add(new ArrayList<String>());
                continue;
            }
            List<String> rowValues = new ArrayList<String>(columnNames.size());
            for (String col : columnNames) {
                rowValues.add(objectToString(rowMap.get(col)));
            }
            rawRows.add(rowValues);
        }
        return rawRows;
    }

    /**
     * 整形済みグループ ID（{@code "[xxx]"} または {@code ""}）と生のグループ ID が一致するか。
     */
    private static boolean groupMatches(String rawGroupId, String requestedFormatted) {
        String formatted = rawGroupId != null ? "[" + rawGroupId + "]" : "";
        return requestedFormatted.equals(formatted);
    }
}
