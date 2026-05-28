package nablarch.test.tool.converter.yaml;

import nablarch.test.core.reader.DataType;
import nablarch.test.tool.converter.ConverterException;
import nablarch.test.tool.converter.TestDataFormatReader;
import nablarch.test.tool.converter.model.FieldDef;
import nablarch.test.tool.converter.model.FileDataBlock;
import nablarch.test.tool.converter.model.ListMapBlock;
import nablarch.test.tool.converter.model.MessageDataBlock;
import nablarch.test.tool.converter.model.RecordLayout;
import nablarch.test.tool.converter.model.TableDataBlock;
import nablarch.test.tool.converter.model.TestDataBlock;
import nablarch.test.tool.converter.model.TestDataContainer;
import nablarch.test.tool.converter.model.TestDataSection;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML ディレクトリを読み込んで {@link TestDataContainer} に変換する Reader。
 *
 * <p>containerName ディレクトリ内の *.yaml ファイルを TestDataSection として読み込む。</p>
 */
public class YamlFormatReader implements TestDataFormatReader {

    private static final List<String> SECTION_KEY_ORDER = Arrays.asList(
            "setup_tables", "expected_tables", "expected_complete_tables",
            "list_maps", "setup_files", "expected_files",
            "messages", "expected_request_header_messages",
            "expected_request_body_messages",
            "response_header_messages", "response_body_messages"
    );

    @Override
    public TestDataContainer read(Path sourcePath) throws ConverterException {
        File dir = sourcePath.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            throw new ConverterException("Directory not found: " + sourcePath);
        }

        String name = dir.getName();
        File[] yamlFiles = dir.listFiles(f -> f.getName().endsWith(".yaml"));
        if (yamlFiles == null) {
            throw new ConverterException("Failed to list files in: " + sourcePath);
        }
        Arrays.sort(yamlFiles, (a, b) -> a.getName().compareTo(b.getName()));

        List<TestDataSection> sections = new ArrayList<>();
        for (File yamlFile : yamlFiles) {
            String sectionName = yamlFile.getName();
            sectionName = sectionName.substring(0, sectionName.length() - 5); // strip .yaml
            try {
                Map<String, Object> yaml = loadYaml(yamlFile);
                List<TestDataBlock> blocks = parseBlocks(yaml);
                sections.add(new TestDataSection(sectionName, blocks));
            } catch (IOException e) {
                throw new ConverterException("Failed to read YAML file: " + yamlFile, e);
            }
        }
        return new TestDataContainer(name, sections);
    }

    private Map<String, Object> loadYaml(File file) throws IOException, ConverterException {
        LoadSettings settings = LoadSettings.builder().setAllowDuplicateKeys(false).build();
        Load loader = new Load(settings);
        FileInputStream in = new FileInputStream(file);
        try {
            Object loaded = loader.loadFromInputStream(in);
            if (loaded == null) {
                return Collections.emptyMap();
            }
            if (!(loaded instanceof Map)) {
                throw new ConverterException("YAML root must be a mapping: " + file);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) loaded;
            return result;
        } finally {
            in.close();
        }
    }

    private List<TestDataBlock> parseBlocks(Map<String, Object> yaml) {
        List<TestDataBlock> blocks = new ArrayList<>();
        for (String sectionKey : SECTION_KEY_ORDER) {
            if (!yaml.containsKey(sectionKey)) {
                continue;
            }
            List<Object> entries = castList(yaml.get(sectionKey));
            DataType dataType = sectionKeyToDataType(sectionKey);
            for (Object entry : entries) {
                Map<String, Object> map = castMap(entry);
                blocks.add(parseBlock(dataType, sectionKey, map));
            }
        }
        return blocks;
    }

    private TestDataBlock parseBlock(DataType dataType, String sectionKey, Map<String, Object> map) {
        String groupId = toStr(map.get("group_id"), "");

        if (isTableType(dataType)) {
            return parseTableBlock(dataType, groupId, map);
        } else if (sectionKey.equals("list_maps")) {
            return parseListMapBlock(groupId, map);
        } else if (isFileType(sectionKey)) {
            return parseFileBlock(sectionKey, groupId, map);
        } else {
            return parseMessageBlock(dataType, groupId, map);
        }
    }

    private TableDataBlock parseTableBlock(DataType dataType, String groupId, Map<String, Object> map) {
        String identifier = toStr(map.get("table"), "");
        List<Object> rowEntries = castList(map.get("rows"));
        List<String> columnNames = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        for (Object rowObj : rowEntries) {
            Map<String, Object> rowMap = castMap(rowObj);
            if (columnNames.isEmpty()) {
                columnNames.addAll(rowMap.keySet());
            }
            List<String> row = new ArrayList<>();
            for (String col : columnNames) {
                row.add(objectToString(rowMap.get(col)));
            }
            rows.add(row);
        }
        return new TableDataBlock(dataType, groupId, identifier, columnNames, rows);
    }

    private ListMapBlock parseListMapBlock(String groupId, Map<String, Object> map) {
        String identifier = toStr(map.get("id"), "");
        List<Object> rowEntries = castList(map.get("rows"));
        List<String> columnNames = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        for (Object rowObj : rowEntries) {
            Map<String, Object> rowMap = castMap(rowObj);
            if (columnNames.isEmpty()) {
                columnNames.addAll(rowMap.keySet());
            }
            List<String> row = new ArrayList<>();
            for (String col : columnNames) {
                row.add(objectToString(rowMap.get(col)));
            }
            rows.add(row);
        }
        return new ListMapBlock(groupId, identifier, columnNames, rows);
    }

    private FileDataBlock parseFileBlock(String sectionKey, String groupId, Map<String, Object> map) {
        String identifier = toStr(map.get("path"), "");
        String typeStr = toStr(map.get("type"), "variable");
        FileDataBlock.FileType fileType = "fixed".equals(typeStr)
                ? FileDataBlock.FileType.FIXED : FileDataBlock.FileType.VARIABLE;
        DataType dataType = resolveFileDataType(sectionKey, fileType);

        Map<String, String> directives = new LinkedHashMap<>();
        if (map.containsKey("directives") && map.get("directives") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> directivesMap = (Map<String, Object>) map.get("directives");
            for (Map.Entry<String, Object> entry : directivesMap.entrySet()) {
                directives.put(entry.getKey(), toStr(entry.getValue(), ""));
            }
        }

        List<RecordLayout> records = new ArrayList<>();
        List<Object> recordEntries = castList(map.get("records"));
        for (Object recObj : recordEntries) {
            Map<String, Object> recMap = castMap(recObj);
            records.add(parseRecordLayout(recMap, fileType == FileDataBlock.FileType.FIXED));
        }

        return new FileDataBlock(dataType, groupId, identifier, fileType, directives, records);
    }

    private DataType resolveFileDataType(String sectionKey, FileDataBlock.FileType fileType) {
        boolean isSetup = sectionKey.equals("setup_files");
        if (fileType == FileDataBlock.FileType.FIXED) {
            return isSetup ? DataType.SETUP_FIXED : DataType.EXPECTED_FIXED;
        } else {
            return isSetup ? DataType.SETUP_VARIABLE : DataType.EXPECTED_VARIABLE;
        }
    }

    private MessageDataBlock parseMessageBlock(DataType dataType, String groupId, Map<String, Object> map) {
        String identifier = toStr(map.get("id"), "");
        List<Object> recordEntries = castList(map.get("records"));

        Map<String, String> fwHeaderFields = new LinkedHashMap<>();
        List<RecordLayout> records = new ArrayList<>();

        for (Object recObj : recordEntries) {
            Map<String, Object> recMap = castMap(recObj);
            String recordType = toStr(recMap.get("record_type"), "");
            if ("FW_HEADER".equals(recordType)) {
                // Extract fwHeaderFields from fields + rows[0]
                List<Object> fieldEntries = castList(recMap.get("fields"));
                List<Object> rowEntries = castList(recMap.get("rows"));
                List<Object> firstRow = rowEntries.isEmpty() ? Collections.emptyList() : castList(rowEntries.get(0));
                for (int i = 0; i < fieldEntries.size(); i++) {
                    Map<String, Object> fieldMap = castMap(fieldEntries.get(i));
                    String fieldName = toStr(fieldMap.get("name"), "");
                    String value = i < firstRow.size() ? toStr(firstRow.get(i), "") : "";
                    fwHeaderFields.put(fieldName, value);
                }
            } else {
                records.add(parseRecordLayout(recMap, false));
            }
        }

        return new MessageDataBlock(dataType, groupId, identifier, fwHeaderFields, records);
    }

    private RecordLayout parseRecordLayout(Map<String, Object> recMap, boolean includeLength) {
        String recordType = toStr(recMap.get("record_type"), "");
        List<Object> fieldEntries = castList(recMap.get("fields"));
        List<FieldDef> fields = new ArrayList<>();
        for (Object fieldObj : fieldEntries) {
            Map<String, Object> fieldMap = castMap(fieldObj);
            String name = toStr(fieldMap.get("name"), "");
            String type = toStr(fieldMap.get("type"), null);
            String length = includeLength ? toStr(fieldMap.get("length"), null) : null;
            fields.add(new FieldDef(name, type, length));
        }
        List<List<String>> dataRows = new ArrayList<>();
        List<Object> rowEntries = castList(recMap.get("rows"));
        for (Object rowObj : rowEntries) {
            List<Object> rawRow = castList(rowObj);
            List<String> row = new ArrayList<>();
            for (Object cell : rawRow) {
                row.add(objectToString(cell));
            }
            dataRows.add(row);
        }
        return new RecordLayout(recordType, fields, dataRows);
    }

    private boolean isTableType(DataType dt) {
        return dt == DataType.SETUP_TABLE_DATA || dt == DataType.EXPECTED_TABLE_DATA
                || dt == DataType.EXPECTED_COMPLETED;
    }

    private boolean isFileType(String sectionKey) {
        return sectionKey.equals("setup_files") || sectionKey.equals("expected_files");
    }

    private DataType sectionKeyToDataType(String key) {
        switch (key) {
            case "setup_tables": return DataType.SETUP_TABLE_DATA;
            case "expected_tables": return DataType.EXPECTED_TABLE_DATA;
            case "expected_complete_tables": return DataType.EXPECTED_COMPLETED;
            case "list_maps": return DataType.LIST_MAP;
            case "setup_files": return DataType.SETUP_FIXED; // refined in parseFileBlock
            case "expected_files": return DataType.EXPECTED_FIXED; // refined in parseFileBlock
            case "messages": return DataType.MESSAGE;
            case "expected_request_header_messages": return DataType.EXPECTED_REQUEST_HEADER_MESSAGES;
            case "expected_request_body_messages": return DataType.EXPECTED_REQUEST_BODY_MESSAGES;
            case "response_header_messages": return DataType.RESPONSE_HEADER_MESSAGES;
            case "response_body_messages": return DataType.RESPONSE_BODY_MESSAGES;
            default: throw new IllegalArgumentException("Unknown section key: " + key);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> castList(Object obj) {
        if (obj == null) return Collections.emptyList();
        if (obj instanceof List) return (List<Object>) obj;
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        if (obj instanceof Map) return (Map<String, Object>) obj;
        return Collections.emptyMap();
    }

    private String toStr(Object obj, String defaultValue) {
        if (obj == null) return defaultValue;
        return obj.toString();
    }

    /** YAML 値を TestDataBlock 用文字列に変換。null は Java null として保持。 */
    private String objectToString(Object obj) {
        if (obj == null) return null;
        return obj.toString();
    }
}
