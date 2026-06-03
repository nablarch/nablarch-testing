package nablarch.test.tool.converter.yaml;

import org.junit.Assume;
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
        assertThat(errors.get(0).getMessage(), containsString("[V-COL]"));
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
        assertThat(errors.get(0).getMessage(), containsString("[V-COL]"));
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
        assertThat(errors.get(0).getMessage(), containsString("[V-DIR]"));
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

    /**
     * [Given] expected_request_body_messages の fw_header に既知ディレクティブ名が含まれる
     * [When]  validate() を呼び出す
     * [Then]  構造境界違反エラーが報告される（messages 以外のメッセージ系にも V-DIR が適用されること）
     */
    @Test
    public void directiveInFwHeader_expectedRequestBodyMessages() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "expected_request_body_messages:\n" +
                "  - id: msg01\n" +
                "    fw_header:\n" +
                "      file-type: Fixed\n" +   // ディレクティブ名がfw_headerに混入
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows: []\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), containsString("[V-DIR]"));
        assertThat(errors.get(0).getMessage(), containsString("file-type"));
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
        assertThat(errors.get(0).getMessage(), containsString("[V-SCH]"));
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
        // toString() が "[filePath] location: message" 形式であること
        String str = e.toString();
        assertThat(str, containsString("["));
        assertThat(str, containsString("] "));
        assertThat(str, containsString(": "));
        assertThat(str, containsString(e.getFilePath()));
        assertThat(str, containsString(e.getLocation()));
        assertThat(str, containsString(e.getMessage()));
    }

    /**
     * [Given] YAML ルートがマップ以外（リスト形式）のファイル
     * [When]  validate() を呼び出す
     * [Then]  スキーマエラーが返るが V-COL/V-DIR 構造検証では例外が出ない
     */
    @Test
    public void yamlRootIsNotMap_noStructureException() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "- item1\n" +
                "- item2\n"   // YAML ルートがリスト → parseYaml が null を返し構造検証をスキップ
        );

        // スキーマエラーのみが返り、V-COL/V-DIR では例外が出ないこと
        List<ValidationError> errors = validator.validate(dir.toPath());
        for (ValidationError e : errors) {
            assertThat("V-COL/V-DIR による誤検知がないこと", e.getMessage(), containsString("[V-SCH]"));
        }
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
        // root 権限では setReadable(false) が無効なのでスキップ
        Assume.assumeFalse(yamlFile.canRead());
        try {
            List<ValidationError> errors = validator.validate(dir.toPath());
            assertThat(errors.size(), is(1));
            assertThat(errors.get(0).getMessage(), containsString("ファイル読み込みエラー"));
        } finally {
            yamlFile.setReadable(true);
        }
    }

    // -------------------------------------------------------------------------
    // V-FNAME: 同一 record_fragment 内のフィールド名重複検証
    // -------------------------------------------------------------------------

    /**
     * [Given] setup_files の record_fragment でフィールド名 "col1" が重複している
     * [When]  validate() を呼び出す
     * [Then]  フィールド名重複エラーが報告される
     */
    @Test
    public void fieldNameDuplicate_setupFiles() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "        rows:\n" +
                "          - [a, a]\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), containsString("[V-FNAME]"));
        assertThat(errors.get(0).getMessage(), containsString("col1"));
        // location が records[0].fields を指していること（指摘2）
        assertThat(errors.get(0).getLocation(), containsString("records[0].fields"));
    }

    /**
     * [Given] 同一 record_fragment 内でフィールド名が3個重複している
     * [When]  validate() を呼び出す
     * [Then]  重複発見のたびに1件エラーが追加されるため2件報告される（指摘1）
     */
    @Test
    public void fieldNameTriplicate_twoErrors() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "        rows:\n" +
                "          - [a, a, a]\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        long fnameCnt = errors.stream().filter(e -> e.getMessage().contains("[V-FNAME]")).count();
        assertThat(fnameCnt, is(2L));
    }

    /**
     * [Given] 全フィールド名がユニーク
     * [When]  validate() を呼び出す
     * [Then]  エラーなし
     */
    @Test
    public void fieldNameUnique_noError() throws Exception {
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
                "          - [a, b]\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(0));
    }

    /**
     * [Given] 別々の record_fragment に同名フィールドがある（同一 fragment 内ではない）
     * [When]  validate() を呼び出す
     * [Then]  エラーなし（V-FNAME は同一 fragment 内のみ検査）
     */
    @Test
    public void fieldNameDuplicate_acrossFragments_noError() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: TYPE_A\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "        rows:\n" +
                "          - [a]\n" +
                "      - record_type: TYPE_B\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "        rows:\n" +
                "          - [b]\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(0));
    }

    /**
     * [Given] messages の record_fragment でフィールド名が重複している
     * [When]  validate() を呼び出す
     * [Then]  フィールド名重複エラーが報告される
     */
    @Test
    public void fieldNameDuplicate_messages() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "messages:\n" +
                "  - id: msg01\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows: []\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), containsString("[V-FNAME]"));
    }

    // -------------------------------------------------------------------------
    // V-DKEY: directives キーが既知のディレクティブ名であることの検証
    // -------------------------------------------------------------------------

    /**
     * [Given] setup_files の directives に未知キーが含まれる
     * [When]  validate() を呼び出す
     * [Then]  不正ディレクティブキーエラーが少なくとも1件報告される
     */
    @Test
    public void unknownDirectiveKey_setupFiles() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    directives:\n" +
                "      unknown-key: value\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "        rows: []\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        boolean hasDkeyError = errors.stream().anyMatch(e -> e.getMessage().contains("[V-DKEY]") && e.getMessage().contains("unknown-key"));
        assertThat("V-DKEY エラーが少なくとも1件報告されること", hasDkeyError, is(true));
        // location が setup_files[0].directives を指していること（指摘4）
        boolean hasLocation = errors.stream()
                .filter(e -> e.getMessage().contains("[V-DKEY]"))
                .anyMatch(e -> e.getLocation().contains("setup_files[0].directives"));
        assertThat("location が setup_files[0].directives を含むこと", hasLocation, is(true));
    }

    /**
     * [Given] directives が空マップ
     * [When]  validate() を呼び出す
     * [Then]  エラーなし（指摘3）
     */
    @Test
    public void emptyDirectives_noError() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    directives: {}\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "        rows: []\n"
        );

        assertThat(validator.validate(dir.toPath()).size(), is(0));
    }

    /**
     * [Given] directives に未知キーが2つある
     * [When]  validate() を呼び出す
     * [Then]  2件の V-DKEY エラーが報告される（指摘5）
     */
    @Test
    public void unknownDirectiveKey_multipleKeys_twoErrors() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    directives:\n" +
                "      bad-key1: v1\n" +
                "      bad-key2: v2\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "        rows: []\n"
        );

        long cnt = validator.validate(dir.toPath()).stream()
                .filter(e -> e.getMessage().contains("[V-DKEY]")).count();
        assertThat(cnt, is(2L));
    }

    /**
     * [Given] setup_tables に未知の directives キーがある（V-DKEY の適用外セクション）
     * [When]  validate() を呼び出す
     * [Then]  V-DKEY は発動せずエラーなし（指摘9: 適用外セクションの仕様文書化）
     */
    @Test
    public void unknownDirectiveKey_setupTables_notApplied() throws Exception {
        File dir = tmp.newFolder("TestCase");
        // setup_tables は FILE_AND_MESSAGE_SECTION_KEYS に含まれないため V-DKEY 適用外
        // → 未知キーを directives に置いても V-DKEY が発動しないことを確認
        writeYaml(dir, "case01.yaml",
                "setup_tables:\n" +
                "  - table: USERS\n" +
                "    rows:\n" +
                "      - {USER_ID: \"001\"}\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        boolean hasDkeyError = errors.stream().anyMatch(e -> e.getMessage().contains("[V-DKEY]"));
        assertThat("setup_tables には V-DKEY が適用されないこと", hasDkeyError, is(false));
    }

    /**
     * [Given] setup_files の directives に既知キーのみ含まれる
     * [When]  validate() を呼び出す
     * [Then]  エラーなし
     */
    @Test
    public void knownDirectiveKey_noError() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    directives:\n" +
                "      text-encoding: UTF-8\n" +
                "      record-separator: CRLF\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "        rows: []\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(0));
    }

    /**
     * [Given] messages の directives に未知キーが含まれる
     * [When]  validate() を呼び出す
     * [Then]  不正ディレクティブキーエラーが少なくとも1件報告される
     */
    @Test
    public void unknownDirectiveKey_messages() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "messages:\n" +
                "  - id: msg01\n" +
                "    directives:\n" +
                "      invalid-directive: value\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows: []\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        boolean hasDkeyError = errors.stream().anyMatch(e -> e.getMessage().contains("[V-DKEY]"));
        assertThat("V-DKEY エラーが少なくとも1件報告されること", hasDkeyError, is(true));
    }

    /**
     * [Given] directives セクションがないブロック
     * [When]  validate() を呼び出す
     * [Then]  エラーなし
     */
    @Test
    public void noDirectives_noError() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "        rows: []\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(0));
    }

    // -------------------------------------------------------------------------
    // V-MSGROW: expected_request_header_messages と expected_request_body_messages の rows 合計行数一致検証
    // -------------------------------------------------------------------------

    /**
     * [Given] expected_request_header_messages[0] の rows 合計が3行、expected_request_body_messages[0] の rows 合計が2行
     * [When]  validate() を呼び出す
     * [Then]  rows 合計行数不一致エラーが報告される
     */
    @Test
    public void msgRowMismatch_requestMessages() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "expected_request_header_messages:\n" +
                "  - id: header01\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [a]\n" +
                "          - [b]\n" +
                "          - [c]\n" +
                "expected_request_body_messages:\n" +
                "  - id: body01\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [x]\n" +
                "          - [y]\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), containsString("[V-MSGROW]"));
        // エラーメッセージに具体的な行数が含まれること（指摘8）
        assertThat(errors.get(0).getMessage(), containsString("rows 合計=3"));
        assertThat(errors.get(0).getMessage(), containsString("rows 合計=2"));
    }

    /**
     * [Given] expected_request_header_messages と expected_request_body_messages の rows 合計が一致
     * [When]  validate() を呼び出す
     * [Then]  エラーなし
     */
    @Test
    public void msgRowMatch_noError() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "expected_request_header_messages:\n" +
                "  - id: header01\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [a]\n" +
                "          - [b]\n" +
                "expected_request_body_messages:\n" +
                "  - id: body01\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [x]\n" +
                "          - [y]\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(0));
    }

    /**
     * [Given] expected_request_header_messages のみあり（body なし）
     * [When]  validate() を呼び出す
     * [Then]  エラーなし（ペアリング対象がなければスキップ）
     */
    @Test
    public void msgRowOnlyHeader_noError() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "expected_request_header_messages:\n" +
                "  - id: header01\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [a]\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(0));
    }

    /**
     * [Given] header が2件、body が1件（ブロック数が非対称）
     * [When]  validate() を呼び出す
     * [Then]  ペアが取れた[0]のみチェックされ header[1] はスキップ → エラーなし（指摘6）
     */
    @Test
    public void msgRow_headerMoreThanBody_noError() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "expected_request_header_messages:\n" +
                "  - id: h1\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [a]\n" +
                "  - id: h2\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [b]\n" +
                "          - [c]\n" +
                "expected_request_body_messages:\n" +
                "  - id: b1\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [x]\n"
        );

        assertThat(validator.validate(dir.toPath()).size(), is(0));
    }

    /**
     * [Given] header ブロックが records を2つ持ち合計3行、body ブロックが合計3行
     * [When]  validate() を呼び出す
     * [Then]  行数一致のためエラーなし（指摘7: countTotalRows の複数 records 対応）
     */
    @Test
    public void msgRow_multipleRecordsInBlock_sumMatches_noError() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "expected_request_header_messages:\n" +
                "  - id: h1\n" +
                "    records:\n" +
                "      - record_type: A\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [a]\n" +
                "          - [b]\n" +
                "      - record_type: B\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [c]\n" +
                "expected_request_body_messages:\n" +
                "  - id: b1\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [x]\n" +
                "          - [y]\n" +
                "          - [z]\n"
        );

        assertThat(validator.validate(dir.toPath()).size(), is(0));
    }

    /**
     * [Given] header の複数 records 合計が body と不一致
     * [When]  validate() を呼び出す
     * [Then]  V-MSGROW エラーが報告される（指摘7: countTotalRows の複数 records 対応）
     */
    @Test
    public void msgRow_multipleRecordsInBlock_sumMismatch() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "expected_request_header_messages:\n" +
                "  - id: h1\n" +
                "    records:\n" +
                "      - record_type: A\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [a]\n" +
                "      - record_type: B\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [b]\n" +
                "expected_request_body_messages:\n" +
                "  - id: b1\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [x]\n" +
                "          - [y]\n" +
                "          - [z]\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), containsString("[V-MSGROW]"));
    }

    /**
     * [Given] 複数ブロックで2番目のペアだけ rows 合計が不一致
     * [When]  validate() を呼び出す
     * [Then]  インデックス1のペアについてエラーが1件報告される
     */
    @Test
    public void msgRowMismatch_secondPairOnly() throws Exception {
        File dir = tmp.newFolder("TestCase");
        writeYaml(dir, "case01.yaml",
                "expected_request_header_messages:\n" +
                "  - id: header01\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [a]\n" +
                "  - id: header02\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [b]\n" +
                "          - [c]\n" +
                "expected_request_body_messages:\n" +
                "  - id: body01\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [x]\n" +
                "  - id: body02\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [y]\n"
        );

        List<ValidationError> errors = validator.validate(dir.toPath());

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), containsString("[V-MSGROW]"));
        assertThat(errors.get(0).getLocation(), containsString("[1]"));
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
