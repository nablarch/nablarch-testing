package nablarch.test.tool.converter.yaml;

import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import nablarch.test.tool.converter.model.MessageDataBlock;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * YAML テストデータディレクトリを検証し、{@link ValidationError} のリストを返すバリデータ。
 *
 * <p>検証ルール:</p>
 * <ul>
 *   <li>V-COL: fields 件数と各 rows 配列長が一致すること</li>
 *   <li>V-DIR: fw_header にディレクティブ名が含まれないこと</li>
 *   <li>V-SCH: ntf-testdata-yaml-schema.json に適合していること</li>
 * </ul>
 */
public class YamlTestDataValidator {

    private static final String SCHEMA_RESOURCE = "/nablarch/test/ntf-testdata-yaml-schema.json";

    private static final Set<String> MESSAGE_SECTION_KEYS = Set.of(
            "messages",
            "expected_request_header_messages",
            "expected_request_body_messages",
            "response_header_messages",
            "response_body_messages"
    );

    private static final Set<String> FILE_AND_MESSAGE_SECTION_KEYS;

    static {
        Set<String> s = new HashSet<>(MESSAGE_SECTION_KEYS);
        s.add("setup_files");
        s.add("expected_files");
        FILE_AND_MESSAGE_SECTION_KEYS = Collections.unmodifiableSet(s);
    }

    /**
     * 指定ディレクトリ内の全 YAML ファイルを検証する。
     *
     * @param dirPath 検証対象の YAML ディレクトリ
     * @return 検証エラーのリスト（エラーなしは空リスト）
     */
    public List<ValidationError> validate(Path dirPath) {
        File dir = dirPath.toFile();
        File[] yamlFiles = dir.listFiles(f -> f.getName().endsWith(".yaml"));
        if (yamlFiles == null || yamlFiles.length == 0) {
            return Collections.emptyList();
        }
        Arrays.sort(yamlFiles, (a, b) -> a.getName().compareTo(b.getName()));

        Schema schema = loadSchema();

        List<ValidationError> errors = new ArrayList<>();
        for (File yamlFile : yamlFiles) {
            errors.addAll(validateFile(yamlFile, schema));
        }
        return errors;
    }

    private List<ValidationError> validateFile(File yamlFile, Schema schema) {
        String filePath = yamlFile.getAbsolutePath();
        List<ValidationError> errors = new ArrayList<>();

        String yamlText;
        try {
            yamlText = new String(Files.readAllBytes(yamlFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            errors.add(new ValidationError(filePath, "", "ファイル読み込みエラー: " + e.getMessage()));
            return errors;
        }

        // V-SCH: スキーマ適合検証
        if (schema != null) {
            List<com.networknt.schema.Error> schemaErrors = schema.validate(yamlText, InputFormat.YAML);
            for (com.networknt.schema.Error schemaError : schemaErrors) {
                errors.add(new ValidationError(filePath, schemaError.getInstanceLocation().toString(), "[V-SCH] スキーマ非適合: " + schemaError.getMessage()));
            }
        }

        // YAML をパース
        Map<String, Object> yaml = parseYaml(yamlText);
        if (yaml == null) {
            return errors;
        }

        // V-COL / V-DIR: 構造検証
        errors.addAll(validateStructure(filePath, yaml));

        return errors;
    }

    private List<ValidationError> validateStructure(String filePath, Map<String, Object> yaml) {
        List<ValidationError> errors = new ArrayList<>();

        for (Map.Entry<String, Object> entry : yaml.entrySet()) {
            String sectionKey = entry.getKey();
            List<Object> blocks = castList(entry.getValue());

            for (int blockIdx = 0; blockIdx < blocks.size(); blockIdx++) {
                Map<String, Object> block = castMap(blocks.get(blockIdx));
                String blockLocation = sectionKey + "[" + blockIdx + "]";

                // V-COL: file 系・message 系のみ適用。setup_tables / expected_tables / list_maps は rows がオブジェクト配列のため対象外
                if (FILE_AND_MESSAGE_SECTION_KEYS.contains(sectionKey)) {
                    List<Object> records = castList(block.get("records"));
                    for (int recIdx = 0; recIdx < records.size(); recIdx++) {
                        Map<String, Object> rec = castMap(records.get(recIdx));
                        String recLocation = blockLocation + ".records[" + recIdx + "]";
                        errors.addAll(validateColumnCount(filePath, recLocation, rec));
                    }
                }

                // V-DIR: messages 系の fw_header チェック
                if (MESSAGE_SECTION_KEYS.contains(sectionKey)) {
                    errors.addAll(validateFwHeader(filePath, blockLocation, block));
                }
            }
        }

        return errors;
    }

    /** V-COL: fields 件数と各 rows 配列長の一致を検証する。 */
    private List<ValidationError> validateColumnCount(String filePath, String recLocation, Map<String, Object> rec) {
        List<ValidationError> errors = new ArrayList<>();
        List<Object> fields = castList(rec.get("fields"));
        List<Object> rows = castList(rec.get("rows"));
        int fieldCount = fields.size();

        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
            List<Object> row = castList(rows.get(rowIdx));
            if (row.size() != fieldCount) {
                String location = recLocation + ".rows[" + rowIdx + "]";
                errors.add(new ValidationError(
                        filePath,
                        location,
                        "[V-COL] 列数不一致: fields=" + fieldCount + " 件に対して rows=" + row.size() + " 要素"
                ));
            }
        }
        return errors;
    }

    /** V-DIR: fw_header にディレクティブ名が含まれていないかを検証する。 */
    private List<ValidationError> validateFwHeader(String filePath, String blockLocation, Map<String, Object> block) {
        List<ValidationError> errors = new ArrayList<>();
        if (!block.containsKey("fw_header")) {
            return errors;
        }
        Map<String, Object> fwHeader = castMap(block.get("fw_header"));
        for (String key : fwHeader.keySet()) {
            if (MessageDataBlock.KNOWN_DIRECTIVE_NAMES.contains(key)) {
                errors.add(new ValidationError(
                        filePath,
                        blockLocation + ".fw_header",
                        "[V-DIR] 構造境界違反: fw_header にディレクティブ名 \"" + key + "\" が含まれています。directives: に移動してください"
                ));
            }
        }
        return errors;
    }

    private Schema loadSchema() {
        try (InputStream in = getClass().getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                System.err.println("WARN: YamlTestDataValidator: スキーマリソースが見つかりません: " + SCHEMA_RESOURCE + " — V-SCH 検証をスキップします");
                return null;
            }
            SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
            return registry.getSchema(in, InputFormat.JSON);
        } catch (IOException e) {
            System.err.println("WARN: YamlTestDataValidator: スキーマのロードに失敗しました — V-SCH 検証をスキップします: " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYaml(String yamlText) {
        LoadSettings settings = LoadSettings.builder().setAllowDuplicateKeys(false).build();
        Load loader = new Load(settings);
        Object loaded = loader.loadFromString(yamlText);
        if (loaded instanceof Map) {
            return (Map<String, Object>) loaded;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Object> castList(Object obj) {
        if (obj instanceof List) return (List<Object>) obj;
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        if (obj instanceof Map) return (Map<String, Object>) obj;
        return Collections.emptyMap();
    }
}
