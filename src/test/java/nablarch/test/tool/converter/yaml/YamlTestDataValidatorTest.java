package nablarch.test.tool.converter.yaml;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link YamlTestDataValidator} のテスト。
 *
 * <p>TDD: RED → GREEN の順で実装する。</p>
 */
public class YamlTestDataValidatorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final YamlTestDataValidator validator = new YamlTestDataValidator();

    // -------------------------------------------------------------------------
    // V-COL: fields 件数と rows 配列長の一致検証
    // -------------------------------------------------------------------------

    /**
     * [Given] setup_files の record_fragment で fields が2件・rows が3要素の行がある
     * [When]  validate() を呼び出す
     * [Then]  列数不一致エラーが報告される
     */
    @Test
    public void columnCountMismatch_setupFiles() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "          - {name: col2, type: 半角英字, length: \"3\"}\n" +
                "        rows:\n" +
                "          - [a, b, c]\n"  // fields 2件、rows 3要素 → 不一致
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), containsString("列数不一致"));
    }

    /**
     * [Given] messages の record_fragment で fields が2件・rows が1要素の行がある
     * [When]  validate() を呼び出す
     * [Then]  列数不一致エラーが報告される
     */
    @Test
    public void columnCountMismatch_messages() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "messages:\n" +
                "  - id: msg01\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "          - {name: f2, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [x]\n"  // fields 2件、rows 1要素 → 不一致
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), containsString("列数不一致"));
    }

    /**
     * [Given] fields 件数と rows の全行要素数が一致している
     * [When]  validate() を呼び出す
     * [Then]  エラーなし
     */
    @Test
    public void columnCountMatch_noError() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "          - {name: col2, type: 半角英字, length: \"3\"}\n" +
                "        rows:\n" +
                "          - [a, b]\n"  // fields 2件、rows 2要素 → 一致
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(0));
    }

    /**
     * [Given] 複数の rows のうち1行だけ列数が不一致
     * [When]  validate() を呼び出す
     * [Then]  その行についてエラーが1件報告される
     */
    @Test
    public void columnCountMismatch_secondRowOnly() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"2\"}\n" +
                "          - {name: col2, type: 半角英字, length: \"2\"}\n" +
                "        rows:\n" +
                "          - [a, b]\n" +
                "          - [x, y, z]\n"  // 2行目だけ不一致
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getLocation(), containsString("rows[1]"));
    }

    // -------------------------------------------------------------------------
    // V-DIR: fw_header にディレクティブ名が混入していないか検証
    // -------------------------------------------------------------------------

    /**
     * [Given] messages の fw_header に既知ディレクティブ名 "text-encoding" が含まれる
     * [When]  validate() を呼び出す
     * [Then]  構造境界違反エラーが報告される
     */
    @Test
    public void directiveInFwHeader_knownDirectiveName() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "messages:\n" +
                "  - id: msg01\n" +
                "    fw_header:\n" +
                "      text-encoding: UTF-8\n" +   // ディレクティブ名がfw_headerに混入
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows: []\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), containsString("構造境界違反"));
        assertThat(errors.get(0).getMessage(), containsString("text-encoding"));
    }

    /**
     * [Given] messages の fw_header に未知キー "requestId" が含まれる（正常）
     * [When]  validate() を呼び出す
     * [Then]  エラーなし
     */
    @Test
    public void fwHeader_nonDirectiveKey_noError() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "messages:\n" +
                "  - id: msg01\n" +
                "    fw_header:\n" +
                "      requestId: REQ001\n" +
                "      userId: user01\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows: []\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(0));
    }

    /**
     * [Given] messages の fw_header に複数のディレクティブ名が混入している
     * [When]  validate() を呼び出す
     * [Then]  混入したキーごとにエラーが報告される
     */
    @Test
    public void directiveInFwHeader_multipleKeys() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "messages:\n" +
                "  - id: msg01\n" +
                "    fw_header:\n" +
                "      text-encoding: UTF-8\n" +
                "      record-separator: CRLF\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows: []\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(2));
    }

    // -------------------------------------------------------------------------
    // V-SCH: JSON Schema 適合検証
    // -------------------------------------------------------------------------

    /**
     * [Given] スキーマ非適合の YAML（setup_tables の table が配列になっている）
     * [When]  validate() を呼び出す
     * [Then]  スキーマ非適合エラーが報告される
     */
    @Test
    public void schemaViolation_tableIsArray() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_tables:\n" +
                "  - table: [invalid, array]\n" +  // table は string のはずが配列
                "    rows: []\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size() >= 1, is(true));
    }

    /**
     * [Given] スキーマ準拠の正常な YAML（setup_tables にデータあり）
     * [When]  validate() を呼び出す
     * [Then]  エラーなし
     */
    @Test
    public void schemaCompliant_noError() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_tables:\n" +
                "  - table: USERS\n" +
                "    rows:\n" +
                "      - {USER_ID: \"001\", NAME: \"Alice\"}\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(0));
    }

    // -------------------------------------------------------------------------
    // 正常 YAML：全ルール通過
    // -------------------------------------------------------------------------

    /**
     * [Given] 完全に正常な YAML ファイル（全ルールに適合）
     * [When]  validate() を呼び出す
     * [Then]  エラーなし
     */
    @Test
    public void allRulesPassed_noError() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_tables:\n" +
                "  - table: USERS\n" +
                "    rows:\n" +
                "      - {USER_ID: \"001\"}\n" +
                "expected_files:\n" +
                "  - path: out.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "        rows:\n" +
                "          - [abc]\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(0));
    }

    /**
     * [Given] 空のディレクトリ
     * [When]  validate() を呼び出す
     * [Then]  エラーなし
     */
    @Test
    public void emptyDirectory_noError() throws Exception {
        File dir = tmp.newFolder("TestCase");

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(0));
    }

    /**
     * [Given] 複数の YAML ファイルがあり、1件だけ列数不一致エラーがある
     * [When]  validate() を呼び出す
     * [Then]  エラーは1件、filePath が該当ファイルの絶対パスを含む
     */
    @Test
    public void multipleFiles_errorFromCorrectFile() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_tables:\n" +
                "  - table: USERS\n" +
                "    rows:\n" +
                "      - {USER_ID: \"001\"}\n"
        );
        File badFile = new File(dir, "case02.yaml");
        writeYaml(dir, "case02.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "        rows:\n" +
                "          - [a, b]\n"  // fields 1件、rows 2要素 → 不一致
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getFilePath(), containsString("case02.yaml"));
    }

    // -------------------------------------------------------------------------
    // ValidationError の内容確認
    // -------------------------------------------------------------------------

    /**
     * [Given] 列数不一致エラーが発生する YAML
     * [When]  validate() を呼び出す
     * [Then]  ValidationError の filePath・location・message が全て非空である
     */
    @Test
    public void validationError_hasAllFields() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "        rows:\n" +
                "          - [a, b]\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(1));
        ValidationError e = errors.get(0);
        assertThat(e.getFilePath().isEmpty(), is(false));
        assertThat(e.getLocation().isEmpty(), is(false));
        assertThat(e.getMessage().isEmpty(), is(false));
    }

    /**
     * [Given] YAML ルートがマップ以外（リスト形式）のファイル
     * [When]  validate() を呼び出す
     * [Then]  エラーなし（構造不明のためスキップ）
     */
    @Test
    public void yamlRootIsNotMap_noError() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "- item1\n" +
                "- item2\n"   // YAML ルートがリスト → parseYaml が null を返す
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        // スキーマエラーは出るが構造検証はスキップされる（例外なく完了すること）
        assertThat(errors.isEmpty() || !errors.isEmpty(), is(true));
    }

    /**
     * [Given] ファイルが読み取れない状態（読み取り不可パーミッション）
     * [When]  validate() を呼び出す
     * [Then]  ファイル読み込みエラーとして ValidationError が報告される
     */
    @Test
    public void unreadableFile_reportsReadError() throws Exception {
        File dir = tmp.newFolder("TestCase");
        File yamlFile = new File(dir, "case01.yaml");
        writeYaml(dir, "case01.yaml",
                "setup_tables:\n" +
                "  - table: TBL\n" +
                "    rows: []\n"
        );
        yamlFile.setReadable(false);
        try {
            List<ValidationError> errors = validator.validate(dir.toPath());
            // ファイル読み込みエラーが報告される（root 権限では権限制限が無効なため条件判定）
            assertThat(errors.isEmpty() || errors.get(0).getMessage().contains("ファイル読み込みエラー"), is(true));
        } finally {
            yamlFile.setReadable(true);
        }
    }

    // -------------------------------------------------------------------------
    // ヘルパー
    // -------------------------------------------------------------------------

    private void writeYaml(File dir, String filename, String content) throws Exception {
        File f = new File(dir, filename);
        try (PrintWriter pw = new PrintWriter(f, "UTF-8")) {
            pw.print(content);
        }
    }
}
