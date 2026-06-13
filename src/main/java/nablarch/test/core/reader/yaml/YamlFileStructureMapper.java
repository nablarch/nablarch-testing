package nablarch.test.core.reader.yaml;

import nablarch.core.util.annotation.Published;
import nablarch.test.core.reader.yaml.model.RawDataFile;
import nablarch.test.core.reader.yaml.model.RawFieldDef;
import nablarch.test.core.reader.yaml.model.RawRecordLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static nablarch.test.core.reader.yaml.YamlSection.FIELD_DIRECTIVES;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_FIELDS;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_GROUP_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_LENGTH;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_NAME;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_PATH;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_RECORDS;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_RECORD_TYPE;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_ROWS;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_TYPE;
import static nablarch.test.core.reader.yaml.YamlSection.castMap;
import static nablarch.test.core.reader.yaml.YamlSection.getList;
import static nablarch.test.core.reader.yaml.YamlSection.objectToString;
import static nablarch.test.core.reader.yaml.YamlSection.toStr;

/**
 * YAML のファイル系セクション（{@code setup_files}／{@code expected_files}）を、値を一切加工せずに
 * 生の構造レコード（{@link RawDataFile}）へ写し取る構造マッピング層。
 *
 * <p>
 * 本クラスは <b>構造のみ</b>を扱う。特殊記法（{@code ${...}}）の解釈・グループ ID 絞り込み・
 * 必須項目チェック・メッセージ長の {@code -} 注入は行わず、それらは値加工層
 * （{@link YamlValueProcessor}）の責務とする。レコードレイアウトとデータ行（位置指定リスト）を
 * 構築する処理は、メッセージ系（{@link YamlMessageStructureMapper}）からも再利用する。
 * </p>
 *
 * @author kiyotis
 */
@Published(tag = "architect")
public final class YamlFileStructureMapper {

    /**
     * ファイル系セクションを全エントリ分 {@link RawDataFile} へ写し取る（グループ絞り込みなし）。
     *
     * @param yaml       YAML トップレベル Map
     * @param sectionKey セクションキー（例: {@code "setup_files"}）
     * @return 全エントリの {@link RawDataFile}（記述順）
     */
    public List<RawDataFile> mapFiles(Map<String, Object> yaml, String sectionKey) {
        List<Object> entries = getList(yaml, sectionKey);
        List<RawDataFile> result = new ArrayList<RawDataFile>();
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String groupId = toStr(map.get(FIELD_GROUP_ID));
            String path = toStr(map.get(FIELD_PATH));
            String fileType = toStr(map.get(FIELD_TYPE));
            Map<String, String> directives = mapDirectives(map);
            List<RawRecordLayout> records = mapRecords(map);
            result.add(new RawDataFile(groupId, path, fileType, directives, records));
        }
        return result;
    }

    /**
     * エントリの {@code directives:} マップを未加工で写し取る（YAML 順保持）。
     */
    static Map<String, String> mapDirectives(Map<String, Object> entry) {
        Map<String, String> directives = new LinkedHashMap<String, String>();
        Object directivesObj = entry.get(FIELD_DIRECTIVES);
        if (directivesObj == null) {
            return directives;
        }
        Map<String, Object> directivesMap = castMap(directivesObj);
        for (Map.Entry<String, Object> e : directivesMap.entrySet()) {
            directives.put(e.getKey(), toStr(e.getValue()));
        }
        return directives;
    }

    /**
     * エントリの {@code records:} を未加工で {@link RawRecordLayout} 群へ写し取る。
     * FW_HEADER レコードもスキップせずそのまま保持する。
     *
     * @param entry セクションエントリ Map
     * @return レコードレイアウト群（YAML 順）
     */
    static List<RawRecordLayout> mapRecords(Map<String, Object> entry) {
        List<Object> records = getList(entry, FIELD_RECORDS);
        List<RawRecordLayout> result = new ArrayList<RawRecordLayout>();
        for (Object recordObj : records) {
            Map<String, Object> record = castMap(recordObj);
            String recordType = toStr(record.get(FIELD_RECORD_TYPE));
            List<RawFieldDef> fields = mapFields(record);
            List<List<String>> rows = mapPositionalRows(record);
            result.add(new RawRecordLayout(recordType, fields, rows));
        }
        return result;
    }

    /**
     * レコードの {@code fields:} を未加工で {@link RawFieldDef} 群へ写し取る（長さ省略は {@code null}）。
     */
    private static List<RawFieldDef> mapFields(Map<String, Object> record) {
        List<Object> fields = getList(record, FIELD_FIELDS);
        List<RawFieldDef> result = new ArrayList<RawFieldDef>(fields.size());
        for (Object fieldObj : fields) {
            Map<String, Object> field = castMap(fieldObj);
            String name = toStr(field.get(FIELD_NAME));
            String type = toStr(field.get(FIELD_TYPE));
            String length = toStr(field.get(FIELD_LENGTH));
            result.add(new RawFieldDef(name, type, length));
        }
        return result;
    }

    /**
     * レコードの {@code rows:}（位置指定リスト）を未加工で写し取る。リストでない行は除外する。
     */
    private static List<List<String>> mapPositionalRows(Map<String, Object> record) {
        List<Object> rows = getList(record, FIELD_ROWS);
        List<List<String>> result = new ArrayList<List<String>>();
        for (Object rowObj : rows) {
            if (!(rowObj instanceof List)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Object> rowList = (List<Object>) rowObj;
            List<String> row = new ArrayList<String>(rowList.size());
            for (Object cell : rowList) {
                row.add(objectToString(cell));
            }
            result.add(row);
        }
        return result;
    }
}
