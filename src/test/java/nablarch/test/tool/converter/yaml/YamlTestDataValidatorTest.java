package nablarch.test.tool.converter.yaml;

import nablarch.core.dataformat.DataRecordFormatterSupport.Directive;
import nablarch.core.dataformat.FixedLengthDataRecordFormatter.FixedLengthDirective;
import nablarch.core.dataformat.VariableLengthDataRecordFormatter.VariableLengthDirective;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * {@link YamlTestDataValidator} のテスト。
 *
 * <p>YAML テストデータディレクトリのリンタ。検証ルール V-COL / V-DIR / V-SCH / V-FNAME / V-DKEY /
 * V-YAML を、意味のある最小フィクスチャ（対象ルールのみが発火する schema 適合データ）で実証する。
 * TDD: RED → GREEN。</p>
 */
public class YamlTestDataValidatorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final YamlTestDataValidator sut = new YamlTestDataValidator();

    // =========================================================================
    // V-COL: fields 件数と各 rows 配列長の一致
    // =========================================================================

    /**
     * [Given] setup_files の record_fragment で fields 2 件に対し rows が 3 要素
     * [When]  validate
     * [Then]  V-COL エラーが 1 件（schema 適合ゆえ他ルールは発火しない）
     */
    @Test
    public void vcol_setupFiles_mismatch_reportsSingleError() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "          - {name: col2, type: 半角英字, length: \"3\"}\n" +
                "        rows:\n" +
                "          - [a, b, c]\n");

        List<ValidationError> errors = sut.validate(dir);

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), containsString("[V-COL]"));
        assertThat(errors.get(0).getLocation(), is("setup_files[0].records[0].rows[0]"));
    }

    /**
     * [Given] messages の record_fragment で fields 2 件に対し rows が 1 要素
     * [When]  validate
     * [Then]  V-COL エラーが報告される
     */
    @Test
    public void vcol_messages_mismatch_reportsError() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "messages:\n" +
                "  - id: msg01\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "          - {name: f2, type: 半角英字}\n" +
                "        rows:\n" +
                "          - [x]\n");

        List<ValidationError> errors = sut.validate(dir);

        assertThat(hasRule(errors, "[V-COL]"), is(true));
    }

    /**
     * [Given] fields 件数と全 rows 要素数が一致
     * [When]  validate
     * [Then]  エラーなし
     */
    @Test
    public void vcol_match_noError() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "          - {name: col2, type: 半角英字, length: \"3\"}\n" +
                "        rows:\n" +
                "          - [a, b]\n" +
                "          - [c, d]\n");

        assertThat(sut.validate(dir).size(), is(0));
    }

    /**
     * [Given] 複数 rows のうち 1 行だけ列数不一致
     * [When]  validate
     * [Then]  当該行のみ V-COL エラー（location が rows[1]）
     */
    @Test
    public void vcol_onlyOffendingRowReported() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "        rows:\n" +
                "          - [a]\n" +
                "          - [b, c]\n");

        List<ValidationError> errors = sut.validate(dir);

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getLocation(), is("setup_files[0].records[0].rows[1]"));
    }

    /**
     * [Given] setup_tables（テーブル系・rows はオブジェクト配列）
     * [When]  validate
     * [Then]  V-COL は適用されない（テーブルの rows はマップで列数概念がない）
     */
    @Test
    public void vcol_notAppliedToTables() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "setup_tables:\n" +
                "  - table: USERS\n" +
                "    rows:\n" +
                "      - {USER_ID: \"001\", NAME: foo}\n");

        assertThat(hasRule(sut.validate(dir), "[V-COL]"), is(false));
    }

    // =========================================================================
    // V-FNAME: 同一 record_fragment 内のフィールド名重複
    // =========================================================================

    /**
     * [Given] 同一 record_fragment の fields に同名フィールドが 2 つ
     * [When]  validate
     * [Then]  V-FNAME エラー 1 件（schema は名前重複を禁止しないため単独発火）
     */
    @Test
    public void vfname_duplicateInSameFragment_reportsError() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: dup, type: 半角英字, length: \"3\"}\n" +
                "          - {name: dup, type: 半角英字, length: \"3\"}\n" +
                "        rows: []\n");

        List<ValidationError> errors = sut.validate(dir);

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), containsString("[V-FNAME]"));
        assertThat(errors.get(0).getMessage(), containsString("dup"));
    }

    /**
     * [Given] 同名フィールドが N(=3) 回出現
     * [When]  validate
     * [Then]  (N-1)=2 件の V-FNAME エラー
     */
    @Test
    public void vfname_tripleDuplicate_reportsTwoErrors() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: dup, type: 半角英字, length: \"3\"}\n" +
                "          - {name: dup, type: 半角英字, length: \"3\"}\n" +
                "          - {name: dup, type: 半角英字, length: \"3\"}\n" +
                "        rows: []\n");

        assertThat(rulesOf(sut.validate(dir), "[V-FNAME]").size(), is(2));
    }

    /**
     * [Given] 異なる record_type 間で同名（種別内では一意）
     * [When]  validate
     * [Then]  V-FNAME は発火しない（重複判定は record_fragment 単位）
     */
    @Test
    public void vfname_sameNameAcrossDifferentFragments_noError() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: A\n" +
                "        fields:\n" +
                "          - {name: shared, type: 半角英字, length: \"3\"}\n" +
                "        rows: []\n" +
                "      - record_type: B\n" +
                "        fields:\n" +
                "          - {name: shared, type: 半角英字, length: \"3\"}\n" +
                "        rows: []\n");

        assertThat(hasRule(sut.validate(dir), "[V-FNAME]"), is(false));
    }

    // =========================================================================
    // V-DKEY: directives キーが既知ディレクティブ名
    // =========================================================================

    /**
     * [Given] setup_files の directives に未知キー
     * [When]  validate
     * [Then]  V-DKEY エラーが報告される（schema 側 V-SCH も発火しうるため anyMatch で確認）
     */
    @Test
    public void vdkey_unknownKey_setupFiles_reportsError() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    directives:\n" +
                "      bogus-directive: value\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "        rows: []\n");

        assertThat(hasRule(sut.validate(dir), "[V-DKEY]"), is(true));
    }

    /**
     * [Given] directives に既知キーのみ
     * [When]  validate
     * [Then]  V-DKEY は発火しない
     */
    @Test
    public void vdkey_knownKeysOnly_noError() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
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
                "        rows: []\n");

        assertThat(hasRule(sut.validate(dir), "[V-DKEY]"), is(false));
    }

    /**
     * [Given] directives セクションを持たないブロック
     * [When]  validate
     * [Then]  エラーなし（directives 未指定は許容）
     */
    @Test
    public void vdkey_noDirectivesSection_noError() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n" +
                "        rows: []\n");

        assertThat(sut.validate(dir).size(), is(0));
    }

    /**
     * [Given] テーブル系セクション（directives 概念を持たない）
     * [When]  validate
     * [Then]  V-DKEY は適用されない
     */
    @Test
    public void vdkey_notAppliedToTables() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "setup_tables:\n" +
                "  - table: USERS\n" +
                "    rows:\n" +
                "      - {USER_ID: \"001\"}\n");

        assertThat(hasRule(sut.validate(dir), "[V-DKEY]"), is(false));
    }

    // =========================================================================
    // V-DIR: 構造境界 — fw_header にディレクティブ名を含めない
    // =========================================================================

    /**
     * [Given] messages の fw_header にディレクティブ名（text-encoding）が混入
     * [When]  validate
     * [Then]  V-DIR エラーが報告される（directives へ移すよう促す）
     */
    @Test
    public void vdir_directiveNameInFwHeader_reportsError() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "messages:\n" +
                "  - id: msg01\n" +
                "    fw_header:\n" +
                "      requestId: RM11AC0202\n" +
                "      text-encoding: UTF-8\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows: []\n");

        List<ValidationError> errors = sut.validate(dir);

        assertThat(hasRule(errors, "[V-DIR]"), is(true));
        assertThat(rulesOf(errors, "[V-DIR]").get(0).getMessage(), containsString("text-encoding"));
    }

    /**
     * [Given] fw_header に通常の制御ヘッダのみ
     * [When]  validate
     * [Then]  V-DIR は発火しない
     */
    @Test
    public void vdir_cleanFwHeader_noError() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "messages:\n" +
                "  - id: msg01\n" +
                "    fw_header:\n" +
                "      requestId: RM11AC0202\n" +
                "      userId: \"0000000001\"\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows: []\n");

        assertThat(hasRule(sut.validate(dir), "[V-DIR]"), is(false));
    }

    // =========================================================================
    // V-SCH: スキーマ適合
    // =========================================================================

    /**
     * [Given] スキーマ未定義のトップレベルキー（additionalProperties:false 違反）
     * [When]  validate
     * [Then]  V-SCH エラーが報告される
     */
    @Test
    public void vsch_unknownTopLevelKey_reportsError() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "unknown_section:\n" +
                "  - foo: bar\n");

        assertThat(hasRule(sut.validate(dir), "[V-SCH]"), is(true));
    }

    /**
     * [Given] record_fragment に必須キー rows が欠落
     * [When]  validate
     * [Then]  V-SCH エラーが報告される
     */
    @Test
    public void vsch_missingRequiredKey_reportsError() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: col1, type: 半角英字, length: \"3\"}\n");

        assertThat(hasRule(sut.validate(dir), "[V-SCH]"), is(true));
    }

    /**
     * [Given] スキーマ適合の正常データ
     * [When]  validate
     * [Then]  エラーなし
     */
    @Test
    public void vsch_conformant_noError() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "setup_tables:\n" +
                "  - table: USERS\n" +
                "    rows:\n" +
                "      - {USER_ID: \"001\", NAME: foo}\n");

        assertThat(sut.validate(dir).size(), is(0));
    }

    // =========================================================================
    // V-DIR の適用スコープ（fw_header を持つセクションのみ）
    // =========================================================================

    /**
     * [Given] response_*_messages（group_message_data＝fw_header 非対応）に fw_header らしき記述
     * [When]  validate
     * [Then]  V-DIR は適用されない（混入は V-SCH が捕捉する責務分担）
     */
    @Test
    public void vdir_notAppliedToResponseMessages() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "response_header_messages:\n" +
                "  - group_id: g1\n" +
                "    id: r1\n" +
                "    fw_header:\n" +
                "      text-encoding: UTF-8\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {name: f1, type: 半角英字}\n" +
                "        rows: []\n");

        // V-DIR は発火せず、fw_header 自体の不正は V-SCH が拾う
        List<ValidationError> errors = sut.validate(dir);
        assertThat(hasRule(errors, "[V-DIR]"), is(false));
        assertThat(hasRule(errors, "[V-SCH]"), is(true));
    }

    // =========================================================================
    // V-YAML: 解析エラー（不正構文・キー重複）でも例外停止せず報告する
    // =========================================================================

    /**
     * [Given] 不正な YAML 構文（未終端フローシーケンス）
     * [When]  validate
     * [Then]  例外を投げず V-YAML エラーを報告する
     */
    @Test
    public void vyaml_malformedSyntax_reportsErrorNotThrow() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "setup_tables:\n" +
                "  - table: USERS\n" +
                "    rows: [\n");  // 未終端フローシーケンス

        assertThat(hasRule(sut.validate(dir), "[V-YAML]"), is(true));
    }

    /**
     * [Given] キーが重複した YAML
     * [When]  validate
     * [Then]  例外を投げず V-YAML エラーを報告する
     */
    @Test
    public void vyaml_duplicateKey_reportsErrorNotThrow() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "setup_tables:\n" +
                "  - table: USERS\n" +
                "    rows: []\n" +
                "setup_tables:\n" +
                "  - table: ROLES\n" +
                "    rows: []\n");

        assertThat(hasRule(sut.validate(dir), "[V-YAML]"), is(true));
    }

    // =========================================================================
    // KNOWN_DIRECTIVE_NAMES と NTF 本体ディレクティブの整合
    // =========================================================================

    /**
     * [Given] NTF 本体の Directive / FixedLengthDirective / VariableLengthDirective 全名称
     * [When]  YamlTestDataValidator.KNOWN_DIRECTIVE_NAMES と比較
     * [Then]  完全一致（本体側の増減を即検知）
     */
    @Test
    @SuppressWarnings("unchecked")
    public void knownDirectiveNames_matchesNtfDirectives() throws Exception {
        Set<String> ntf = new HashSet<>();

        // 基底 Directive.VALUES は private なのでリフレクションで取得
        Field baseValues = Directive.class.getDeclaredField("VALUES");
        baseValues.setAccessible(true);
        Map<String, Directive> baseMap = (Map<String, Directive>) baseValues.get(null);
        baseMap.values().forEach(d -> ntf.add(d.getName()));

        FixedLengthDirective.VALUES.values().forEach(d -> ntf.add(d.getName()));
        VariableLengthDirective.VALUES.values().forEach(d -> ntf.add(d.getName()));

        assertThat(YamlTestDataValidator.KNOWN_DIRECTIVE_NAMES, is(ntf));
    }

    /**
     * [Given] スキーマ {@code $defs/directives/properties}（additionalProperties:false で列挙）のキー集合
     * [When]  YamlTestDataValidator.KNOWN_DIRECTIVE_NAMES と比較
     * [Then]  完全一致（スキーマ↔本体↔バリデータ定数の三者ずれを検知）
     */
    @Test
    @SuppressWarnings("unchecked")
    public void knownDirectiveNames_matchesSchemaDirectiveKeys() throws Exception {
        Object root;
        try (InputStream in = getClass().getResourceAsStream("/nablarch/test/ntf-testdata-yaml-schema.json")) {
            // JSON は YAML のサブセットなので snakeyaml で読める（JSON 依存を増やさない）
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            root = new Load(LoadSettings.builder().build()).loadFromString(json);
        }
        Map<String, Object> defs = (Map<String, Object>) ((Map<String, Object>) root).get("$defs");
        Map<String, Object> directives = (Map<String, Object>) defs.get("directives");
        Map<String, Object> props = (Map<String, Object>) directives.get("properties");

        assertThat(props.keySet(), is(YamlTestDataValidator.KNOWN_DIRECTIVE_NAMES));
    }

    // =========================================================================
    // 入出力・走査のエッジ
    // =========================================================================

    /**
     * [Given] 空ディレクトリ
     * [When]  validate
     * [Then]  空リスト
     */
    @Test
    public void emptyDir_returnsEmpty() throws Exception {
        assertThat(sut.validate(newCaseDir()).isEmpty(), is(true));
    }

    /**
     * [Given] .yaml 以外のファイルのみ
     * [When]  validate
     * [Then]  空リスト（.yaml だけを対象にする）
     */
    @Test
    public void nonYamlFilesIgnored() throws Exception {
        Path dir = newCaseDir();
        writeFile(dir, "readme.txt", "not yaml");
        assertThat(sut.validate(dir).isEmpty(), is(true));
    }

    /**
     * [Given] 複数 .yaml が存在
     * [When]  validate
     * [Then]  ファイル名昇順で走査し、各ファイルのエラーが filePath で区別される
     */
    @Test
    public void multipleFiles_sortedAndAttributed() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "b.yaml", "unknown_b:\n  - x: 1\n");
        writeYaml(dir, "a.yaml", "unknown_a:\n  - x: 1\n");

        List<ValidationError> errors = sut.validate(dir);

        List<String> order = errors.stream()
                .map(e -> new File(e.getFilePath()).getName())
                .distinct().collect(Collectors.toList());
        assertThat(order.get(0), is("a.yaml"));
        assertThat(order.get(1), is("b.yaml"));
    }

    /**
     * [Given] トップレベルがマップでない YAML（リストのみ）
     * [When]  validate
     * [Then]  構造検証は走らず V-SCH のみが判定する（例外を投げない）
     */
    @Test
    public void topLevelNotMap_doesNotThrow() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml", "- a\n- b\n");

        // 例外を投げず、構造系（V-COL/V-FNAME/...）エラーは出さない
        List<ValidationError> errors = sut.validate(dir);
        assertThat(hasRule(errors, "[V-COL]"), is(false));
    }

    /**
     * [Given] 存在しないディレクトリ
     * [When]  validate
     * [Then]  空リスト（listFiles が null）
     */
    @Test
    public void nonExistentDir_returnsEmpty() throws Exception {
        Path missing = new File(temporaryFolder.getRoot(), "nope").toPath();
        assertThat(sut.validate(missing).isEmpty(), is(true));
    }

    /**
     * [Given] 読み込めない「.yaml」エントリ（同名のサブディレクトリ）
     * [When]  validate
     * [Then]  ファイル読み込みエラーが報告され、例外を投げない
     */
    @Test
    public void unreadableYamlEntry_reportsReadError() throws Exception {
        Path dir = newCaseDir();
        // 名前が .yaml で終わるディレクトリは listFiles に拾われるが readAllBytes が IOException
        new File(dir.toFile(), "broken.yaml").mkdir();

        List<ValidationError> errors = sut.validate(dir);

        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("ファイル読み込みエラー")));
    }

    /**
     * [Given] field の name が null（schema 違反だが構造検査が落ちないこと）
     * [When]  validate
     * [Then]  V-FNAME は name=null をスキップし例外を投げない
     */
    @Test
    public void vfname_nullFieldName_skipped() throws Exception {
        Path dir = newCaseDir();
        writeYaml(dir, "case.yaml",
                "setup_files:\n" +
                "  - path: test.dat\n" +
                "    type: fixed\n" +
                "    records:\n" +
                "      - record_type: \"\"\n" +
                "        fields:\n" +
                "          - {type: 半角英字, length: \"3\"}\n" +
                "        rows: []\n");

        // name 欠落は schema(V-SCH) で捕捉される。V-FNAME は例外なくスキップ。
        assertThat(hasRule(sut.validate(dir), "[V-FNAME]"), is(false));
    }

    // =========================================================================
    // ヘルパー
    // =========================================================================

    private Path newCaseDir() throws Exception {
        return temporaryFolder.newFolder("TestCase").toPath();
    }

    private void writeYaml(Path dir, String name, String content) throws Exception {
        writeFile(dir, name, content);
    }

    private void writeFile(Path dir, String name, String content) throws Exception {
        File f = new File(dir.toFile(), name);
        try (PrintWriter pw = new PrintWriter(f, "UTF-8")) {
            pw.print(content);
        }
    }

    private static boolean hasRule(List<ValidationError> errors, String tag) {
        return errors.stream().anyMatch(e -> e.getMessage().contains(tag));
    }

    private static List<ValidationError> rulesOf(List<ValidationError> errors, String tag) {
        return errors.stream().filter(e -> e.getMessage().contains(tag)).collect(Collectors.toList());
    }
}
