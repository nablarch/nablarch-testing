package nablarch.test.core.reader.yaml;

import nablarch.core.util.annotation.Published;
import nablarch.test.core.reader.yaml.model.RawListMap;
import nablarch.test.core.reader.yaml.model.RawTableData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static nablarch.test.core.reader.yaml.YamlSection.FIELD_GROUP_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_ROWS;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_TABLE;
import static nablarch.test.core.reader.yaml.YamlSection.KEY_LIST_MAPS;
import static nablarch.test.core.reader.yaml.YamlSection.castMap;
import static nablarch.test.core.reader.yaml.YamlSection.getList;
import static nablarch.test.core.reader.yaml.YamlSection.objectToString;
import static nablarch.test.core.reader.yaml.YamlSection.toStr;

/**
 * YAML のテーブル系セクション（{@code setup_tables}／{@code expected_tables}／
 * {@code expected_complete_tables}／{@code list_maps}）を、値を一切加工せずに
 * 生の構造レコード（{@link RawTableData}／{@link RawListMap}）へ写し取る構造マッピング層。
 *
 * <p>
 * 本クラスは <b>構造のみ</b>を扱う。特殊記法（{@code ${...}}）の解釈・デフォルト値補完・
 * 大文字化・マーカーカラムの除外・グループ ID 絞り込みは一切行わず、それらは値加工層
 * （{@link YamlValueProcessor}）の責務とする。本体テスト読み込みと変換ツールが共有する公開 API。
 * </p>
 *
 * <p>
 * カラム名は各エントリの先頭行のキー（マーカー含む・YAML 記述順・大文字小文字保持）から決定する。
 * 空マッピング（{@code {}}）およびマッピングでない行は構造を持たないためデータ行から除外する。
 * </p>
 *
 * @author kiyotis
 */
@Published(tag = "architect")
public final class YamlTableStructureMapper {

    /**
     * テーブル系セクションを全エントリ分 {@link RawTableData} へ写し取る（グループ絞り込みなし）。
     *
     * @param yaml       YAML トップレベル Map
     * @param sectionKey セクションキー（例: {@code "setup_tables"}）
     * @return 全エントリの {@link RawTableData}（記述順）
     */
    public List<RawTableData> mapTables(Map<String, Object> yaml, String sectionKey) {
        List<Object> entries = getList(yaml, sectionKey);
        List<RawTableData> result = new ArrayList<RawTableData>();
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String groupId = toStr(map.get(FIELD_GROUP_ID));
            String tableName = toStr(map.get(FIELD_TABLE));
            List<Object> rows = getList(map, FIELD_ROWS);
            List<String> columnNames = resolveColumns(rows);
            List<List<String>> rawRows = extractRows(rows, columnNames);
            result.add(new RawTableData(groupId, tableName, columnNames, rawRows));
        }
        return result;
    }

    /**
     * {@code list_maps} を全エントリ分 {@link RawListMap} へ写し取る（ID 絞り込みなし）。
     *
     * @param yaml YAML トップレベル Map
     * @return 全エントリの {@link RawListMap}（記述順）
     */
    public List<RawListMap> mapListMaps(Map<String, Object> yaml) {
        List<Object> entries = getList(yaml, KEY_LIST_MAPS);
        List<RawListMap> result = new ArrayList<RawListMap>();
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String id = toStr(map.get(FIELD_ID));
            List<Object> rows = getList(map, FIELD_ROWS);
            List<String> columnNames = resolveColumns(rows);
            List<List<String>> rawRows = extractRows(rows, columnNames);
            result.add(new RawListMap(id, columnNames, rawRows));
        }
        return result;
    }

    /**
     * 先頭行のキーをカラム名（マーカー含む・YAML 順）として決定する。行が無い場合は空リスト。
     */
    private List<String> resolveColumns(List<Object> rows) {
        if (rows.isEmpty()) {
            return new ArrayList<String>();
        }
        // SnakeYAML はマッピングを LinkedHashMap でロードするため keySet の順序は YAML 記述順と一致する。
        return new ArrayList<String>(castMap(rows.get(0)).keySet());
    }

    /**
     * 各行をカラム名に揃えた未加工値リストへ写す。
     *
     * <p>
     * マッピングでない行（スカラ等）は構造を持たないため除外する。空マッピング（{@code {}}）は
     * <b>空リスト</b>として保持する（行の有無を後段が判別できるようにするため。テーブルは空行をスキップし、
     * list_maps は空行として残す、という本体読み込みの差異は値加工層が判断する）。
     * </p>
     */
    private List<List<String>> extractRows(List<Object> rows, List<String> columnNames) {
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
}
