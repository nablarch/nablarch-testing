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
import static nablarch.test.core.reader.yaml.YamlSection.addBinaryFileInterpreter;
import static nablarch.test.core.reader.yaml.YamlSection.castMap;
import static nablarch.test.core.reader.yaml.YamlSection.getList;
import static nablarch.test.core.reader.yaml.YamlSection.interpret;
import static nablarch.test.core.reader.yaml.YamlSection.objectToString;
import static nablarch.test.core.reader.yaml.YamlSection.toStr;

/**
 * YAML から {@link TableData} および ListMap を構築するビルダー。
 *
 * <p>
 * {@code nablarch.test.core.reader.yaml} パッケージ内のビルダークラスおよび
 * {@link nablarch.test.core.reader.YamlTestDataParser} から使用する。
 * </p>
 */
public final class YamlTableDataBuilder {

    private final DbInfo dbInfo;
    private final DefaultValues defaultValues;
    private final List<TestDataInterpreter> interpreters;

    public YamlTableDataBuilder(DbInfo dbInfo, DefaultValues defaultValues,
                          List<TestDataInterpreter> interpreters) {
        this.dbInfo = dbInfo;
        this.defaultValues = defaultValues;
        this.interpreters = interpreters;
    }

    /**
     * 指定セクションの TableData リストを構築する。
     *
     * @param yaml         YAML トップレベル Map
     * @param sectionKey   セクションキー（例: "setup_tables"）
     * @param groupId      整形済みグループ ID（例: "[case01]" または ""）
     * @param fillDefaults true の場合 {@link TableData#fillDefaultValues()} を呼ぶ
     * @param path         インタープリタ用ベースパス
     * @return TableData リスト
     */
    public List<TableData> buildTableDataList(Map<String, Object> yaml, String sectionKey,
                                        String groupId, boolean fillDefaults, String path) {
        List<Object> entries = getList(yaml, sectionKey);
        List<TableData> result = new ArrayList<TableData>();
        List<TestDataInterpreter> interps = addBinaryFileInterpreter(path, interpreters);
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String entryGroupId = toStr(map.get(FIELD_GROUP_ID));
            String formattedEntryGid = entryGroupId != null ? "[" + entryGroupId + "]" : "";
            if (!groupId.equals(formattedEntryGid)) {
                continue;
            }
            String tableName = toStr(map.get(FIELD_TABLE));
            List<Object> rows = getList(map, FIELD_ROWS);
            if (rows.isEmpty()) {
                continue;
            }

            Map<String, Object> firstRow = castMap(rows.get(0));
            // SnakeYAML はマッピングを LinkedHashMap としてロードするため、keySet() の順序は YAML の記述順と一致する。
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
     * 指定 ID の list_maps 行リストを構築する。
     *
     * @param yaml YAML トップレベル Map
     * @param id   list_maps エントリの id
     * @param path インタープリタ用ベースパス
     * @return 行リスト（見つからない場合は空リスト）
     */
    public List<Map<String, String>> buildListMapRows(Map<String, Object> yaml, String id, String path) {
        List<Object> entries = getList(yaml, KEY_LIST_MAPS);
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String entryId = toStr(map.get(FIELD_ID));
            if (id.equals(entryId)) {
                return buildRows(map, path);
            }
        }
        return Collections.emptyList();
    }

    private List<Map<String, String>> buildRows(Map<String, Object> listMapEntry, String path) {
        List<Object> rows = getList(listMapEntry, FIELD_ROWS);
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        List<TestDataInterpreter> interps = addBinaryFileInterpreter(path, interpreters);
        for (Object rowObj : rows) {
            Map<String, Object> rowMap = castMap(rowObj);
            Map<String, String> row = new TreeMap<String, String>();
            for (Map.Entry<String, Object> e : rowMap.entrySet()) {
                String key = e.getKey();
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
}
