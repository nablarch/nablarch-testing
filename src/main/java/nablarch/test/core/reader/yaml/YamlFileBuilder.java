package nablarch.test.core.reader.yaml;

import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.DataFileFragment;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.file.VariableLengthFile;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static nablarch.test.core.reader.yaml.YamlSection.FIELD_DIRECTIVES;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_FIELDS;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_GROUP_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_LENGTH;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_NAME;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_PATH;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_RECORD_TYPE;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_RECORDS;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_ROWS;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_TYPE;
import static nablarch.test.core.reader.yaml.YamlSection.FILE_TYPE_FIXED;
import static nablarch.test.core.reader.yaml.YamlSection.FW_HEADER_RECORD_TYPE;
import static nablarch.test.core.reader.yaml.YamlSection.addBinaryFileInterpreter;
import static nablarch.test.core.reader.yaml.YamlSection.castMap;
import static nablarch.test.core.reader.yaml.YamlSection.getList;
import static nablarch.test.core.reader.yaml.YamlSection.interpret;
import static nablarch.test.core.reader.yaml.YamlSection.objectToString;
import static nablarch.test.core.reader.yaml.YamlSection.toStr;

/**
 * YAML から {@link DataFile} を構築するビルダー。
 *
 * <p>
 * パッケージプライベート。{@code nablarch.test.core.reader.yaml} パッケージ内からのみ使用する。
 * </p>
 */
public final class YamlFileBuilder {

    private final List<TestDataInterpreter> interpreters;

    public YamlFileBuilder(List<TestDataInterpreter> interpreters) {
        this.interpreters = interpreters;
    }

    /**
     * 指定セクションの DataFile リストを構築する（ファイルデータ用）。
     *
     * @param yaml       YAML トップレベル Map
     * @param sectionKey セクションキー
     * @param groupId    整形済みグループ ID
     * @param basePath   ファイルパス基点
     * @return DataFile リスト
     */
    public List<DataFile> buildFileList(Map<String, Object> yaml, String sectionKey,
                                  String groupId, String basePath) {
        List<Object> entries = getList(yaml, sectionKey);
        List<DataFile> result = new ArrayList<DataFile>();
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String entryGroupId = toStr(map.get(FIELD_GROUP_ID));
            String formattedEntryGid = entryGroupId != null ? "[" + entryGroupId + "]" : "";
            if (!groupId.equals(formattedEntryGid)) {
                continue;
            }
            String filePath = toStr(map.get(FIELD_PATH));
            String fileType = toStr(map.get(FIELD_TYPE));
            DataFile dataFile = buildDataFile(filePath, fileType, map, basePath);
            result.add(dataFile);
        }
        return result;
    }

    /**
     * メッセージファイル（FixedLengthFile）を構築する（メッセージ系用）。
     *
     * <p>
     * FW_HEADER レコードを除外し、record_type を "default" に固定する。
     * </p>
     *
     * @param yaml       YAML トップレベル Map
     * @param sectionKey セクションキー
     * @param id         メッセージ ID
     * @param basePath   インタープリタ用ベースパス
     * @return FixedLengthFile、または存在しない場合 null
     */
    public FixedLengthFile buildMessageFile(Map<String, Object> yaml, String sectionKey,
                                      String id, String basePath) {
        List<Object> entries = getList(yaml, sectionKey);
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String entryId = toStr(map.get(YamlSection.FIELD_ID));
            if (id.equals(entryId)) {
                FixedLengthFile file = new FixedLengthFile(id);
                applyDirectives(file, map);
                buildFragmentsCore(file, map, true, addBinaryFileInterpreter(basePath, interpreters));
                return file;
            }
        }
        return null;
    }

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

    private void applyDirectives(DataFile file, Map<String, Object> map) {
        Object directivesObj = map.get(FIELD_DIRECTIVES);
        if (directivesObj == null) {
            return;
        }
        Map<String, Object> directives = castMap(directivesObj);
        for (Map.Entry<String, Object> e : directives.entrySet()) {
            file.setDirective(e.getKey(), toStr(e.getValue()));
        }
    }

    private void buildFragments(DataFile file, Map<String, Object> map, String basePath) {
        buildFragmentsCore(file, map, false, addBinaryFileInterpreter(basePath, interpreters));
    }

    /**
     * DataFileFragment を構築してファイルに追加する（共通実装）。
     *
     * @param file         ファイル
     * @param map          セクション Map
     * @param skipFwHeader true の場合 FW_HEADER レコードをスキップし、record_type を "default" に固定する
     * @param interps      使用するインタープリタリスト
     */
    void buildFragmentsCore(DataFile file, Map<String, Object> map,
                             boolean skipFwHeader, List<TestDataInterpreter> interps) {
        List<Object> records = getList(map, FIELD_RECORDS);
        for (Object recordObj : records) {
            Map<String, Object> record = castMap(recordObj);
            String recordType = toStr(record.get(FIELD_RECORD_TYPE));

            if (skipFwHeader && FW_HEADER_RECORD_TYPE.equals(recordType)) {
                continue;
            }

            DataFileFragment fragment = file.getNewFragment();
            fragment.setRecordType(skipFwHeader ? "default" : (recordType != null ? recordType : "default"));

            List<Object> fields = getList(record, FIELD_FIELDS);
            List<String> names = new ArrayList<String>(fields.size());
            List<String> types = new ArrayList<String>(fields.size());
            List<String> lengths = new ArrayList<String>(fields.size());
            boolean hasLength = false;

            for (Object fieldObj : fields) {
                Map<String, Object> field = castMap(fieldObj);
                names.add(toStr(field.get(FIELD_NAME)));
                types.add(toStr(field.get(FIELD_TYPE)));
                Object len = field.get(FIELD_LENGTH);
                if (len != null) {
                    hasLength = true;
                    lengths.add(toStr(len));
                } else {
                    lengths.add(null);
                }
            }

            fragment.setNames(names);
            fragment.setTypes(types);

            if (skipFwHeader || hasLength) {
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
}
