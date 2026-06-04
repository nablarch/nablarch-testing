package nablarch.test.core.reader.yaml;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * NTF テストデータ YAML が JSON Schema に適合していることを検証するテスト。
 *
 * <p>スキーマファイル: {@code nablarch/test/ntf-testdata-yaml-schema.json}（クラスパス）</p>
 * <p>検証対象: {@code YamlTestDataParserTest/} 以下の全 YAML ファイル</p>
 */
public class YamlSchemaValidationTest {

    private static final String YAML_DIR =
            "src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/";

    private static final String SCHEMA_RESOURCE =
            "/nablarch/test/ntf-testdata-yaml-schema.json";

    private Schema loadSchema() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Schema not found: " + SCHEMA_RESOURCE);
            }
            SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
            return registry.getSchema(in, InputFormat.JSON);
        }
    }

    /**
     * [Given] YamlTestDataParserTest/ 以下の各 YAML ファイル
     * [When]  JSON Schema でバリデーションする
     * [Then]  バリデーションエラーが0件であること
     */
    @Test
    public void allTestYamlFilesConformToSchema() throws Exception {
        Schema schema = loadSchema();

        for (Path yamlFile : Files.list(Paths.get(YAML_DIR))
                .filter(p -> p.toString().endsWith(".yaml"))
                // nativeTypes.yaml はクォートなし boolean/integer/float でパーサーの型変換動作を検証する特殊ファイル（スキーマ準拠外）
                .filter(p -> !p.getFileName().toString().equals("nativeTypes.yaml"))
                .sorted()
                .collect(Collectors.toList())) {

            String yaml = new String(Files.readAllBytes(yamlFile), StandardCharsets.UTF_8);
            List<Error> errors = schema.validate(yaml, InputFormat.YAML);

            assertThat(
                    yamlFile.getFileName() + ": " + errors,
                    errors.size(),
                    is(0)
            );
        }
    }
}
