package nablarch.test.tool.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nablarch.test.core.reader.DataType;
import nablarch.test.core.reader.yaml.YamlLoader;
import nablarch.test.tool.converter.model.TableDataBlock;
import nablarch.test.tool.converter.model.TestDataBlock;
import nablarch.test.tool.converter.model.TestDataContainer;
import nablarch.test.tool.converter.model.TestDataSection;
import nablarch.test.tool.converter.xls.XlsFormatReader;
import nablarch.test.tool.converter.xls.XlsFormatWriter;
import nablarch.test.tool.converter.yaml.YamlFormatReader;
import nablarch.test.tool.converter.yaml.YamlFormatWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@link TestDataConverter} の入口（4 方向変換）テストクラス。
 *
 * <p>
 * 実 Reader／Writer を用いた end-to-end 検証を中心に据える。入力フィクスチャは Writer で生成し、変換後に
 * Reader で読み戻して中間モデルの一致（＝意味不変）を確認することで、入口が「リソース走査 → 集約 → 形式選択 →
 * パス解決 → 出力」を 4 方向（XLS→YAML／YAML→XLS／XLS→XLS／YAML→YAML）で正しく束ねていることを示す。
 * あわせて複数リソースの 1 コンテナ集約・ネスト構造の保持・上書き可否・include／exclude・入力不在の異常系を検証する。
 * </p>
 *
 * @author kiyobot
 */
public class TestDataConverterTest {

    /** 一時ディレクトリ */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** 入力ルート */
    private Path in;

    /** 出力ルート */
    private Path out;

    /**
     * 入出力ルートを初期化する。
     *
     * @throws IOException 入出力エラー
     */
    @Before
    public void setUp() throws IOException {
        in = folder.newFolder("in").toPath();
        out = folder.newFolder("out").toPath();
    }

    /**
     * YamlLoader のファイルキャッシュをクリアし、テスト間の干渉を防ぐ。
     */
    @After
    public void clearLoaderCache() {
        YamlLoader.clearCacheForTest();
    }

    // ---- ヘルパ ----

    /**
     * カラム名・データ行から行リストを作る。
     *
     * @param cells セル値
     * @return 1 行
     */
    private static List<String> row(String... cells) {
        return Arrays.asList(cells);
    }

    /**
     * 検証用の SETUP_TABLE ブロックを作る（{@code ${...}} を含み未加工を確認できる）。
     *
     * @param name テーブル名
     * @return テーブルブロック
     */
    private static TableDataBlock sampleTable(String name) {
        return new TableDataBlock(DataType.SETUP_TABLE_DATA, "", name,
                row("USER_NAME", "ROLE"),
                Arrays.asList(row("${userName}", "admin"), row("alice", "user")));
    }

    /**
     * 1 セクション 1 テーブルのコンテナを作る。
     *
     * @param containerName コンテナ名（ブック名／ディレクトリ名）
     * @param sectionName   セクション名（シート名／ファイル名）
     * @param table         テーブルブロック
     * @return コンテナ
     */
    private static TestDataContainer oneTable(String containerName, String sectionName, TableDataBlock table) {
        TestDataSection section = new TestDataSection(sectionName, Collections.<TestDataBlock>singletonList(table));
        return new TestDataContainer(containerName, Collections.singletonList(section));
    }

    /**
     * 入力 Excel ブックを生成する。
     *
     * @param container コンテナ
     * @param baseDir   出力先ディレクトリ
     */
    private static void writeXls(TestDataContainer container, Path baseDir) {
        new XlsFormatWriter().write(container, baseDir.toString());
    }

    /**
     * 入力 YAML コンテナ（ディレクトリ）を生成する。
     *
     * @param container コンテナ
     * @param baseDir   コンテナディレクトリ
     */
    private static void writeYaml(TestDataContainer container, Path baseDir) {
        new YamlFormatWriter().write(container, baseDir.toString());
    }

    /**
     * コンテナから唯一のブロックを取り出す。
     *
     * @param container コンテナ
     * @return 唯一のブロック
     */
    private static TestDataBlock onlyBlock(TestDataContainer container) {
        assertThat(container.getSections().size(), is(1));
        List<TestDataBlock> blocks = container.getSections().get(0).getBlocks();
        assertThat(blocks.size(), is(1));
        return blocks.get(0);
    }

    /**
     * テーブルブロックが期待値と一致することを表明する（データ種別・グループ・名前・カラム・行）。
     *
     * @param expected 期待テーブル
     * @param actual   実テーブルブロック
     */
    private static void assertTable(TableDataBlock expected, TestDataBlock actual) {
        assertThat(actual, is(org.hamcrest.CoreMatchers.instanceOf(TableDataBlock.class)));
        TableDataBlock table = (TableDataBlock) actual;
        assertThat(table.getDataType(), is(expected.getDataType()));
        assertThat(table.getGroupId(), is(expected.getGroupId()));
        assertThat(table.getIdentifier(), is(expected.getIdentifier()));
        assertThat(table.getColumnNames(), is(expected.getColumnNames()));
        assertThat(table.getRows(), is(expected.getRows()));
    }

    // ---- 4 方向 end-to-end ----

    /**
     * Given: 入力ルートに Excel ブック 1 件。
     * When : XLS→YAML 変換。
     * Then : {@code <out>/<ブック名>/<シート名>.yaml} が生成され、読み戻すと元のテーブルと一致する。
     */
    @Test
    public void convertsXlsToYaml() {
        TableDataBlock table = sampleTable("USERS");
        writeXls(oneTable("BookA", "data", table), in);

        int count = TestDataConverter.convert(DataFormat.XLS, DataFormat.YAML, in, out);

        assertThat(count, is(1));
        Path yaml = out.resolve("BookA/data.yaml");
        assertThat(Files.exists(yaml), is(true));
        TestDataContainer read = new YamlFormatReader().read(out.resolve("BookA").toString(), "data");
        assertTable(table, onlyBlock(read));
    }

    /**
     * Given: 入力ルートに YAML コンテナ 1 件。
     * When : YAML→XLS 変換。
     * Then : {@code <out>/<コンテナ名>.xlsx} が生成され、読み戻すと元のテーブルと一致する。
     */
    @Test
    public void convertsYamlToXls() {
        TableDataBlock table = sampleTable("USERS");
        writeYaml(oneTable("BookB", "data", table), in.resolve("BookB"));

        int count = TestDataConverter.convert(DataFormat.YAML, DataFormat.XLS, in, out);

        assertThat(count, is(1));
        Path xlsx = out.resolve("BookB.xlsx");
        assertThat(Files.exists(xlsx), is(true));
        TestDataContainer read = new XlsFormatReader().read(out.toString(), "BookB/data");
        assertTable(table, onlyBlock(read));
    }

    /**
     * Given: 入力ルートに Excel ブック 1 件。
     * When : XLS→XLS 変換（同一形式・往復要件）。
     * Then : {@code <out>/<ブック名>.xlsx} が生成され、読み戻すと元のテーブルと一致する。
     */
    @Test
    public void convertsXlsToXls() {
        TableDataBlock table = sampleTable("USERS");
        writeXls(oneTable("BookC", "data", table), in);

        int count = TestDataConverter.convert(DataFormat.XLS, DataFormat.XLS, in, out);

        assertThat(count, is(1));
        assertThat(Files.exists(out.resolve("BookC.xlsx")), is(true));
        TestDataContainer read = new XlsFormatReader().read(out.toString(), "BookC/data");
        assertTable(table, onlyBlock(read));
    }

    /**
     * Given: 入力ルートに YAML コンテナ 1 件。
     * When : YAML→YAML 変換（同一形式・往復要件）。
     * Then : {@code <out>/<コンテナ名>/<ファイル名>.yaml} が生成され、読み戻すと元のテーブルと一致する。
     */
    @Test
    public void convertsYamlToYamlIgnoringNonYamlEntries() throws IOException {
        TableDataBlock table = sampleTable("USERS");
        writeYaml(oneTable("BookD", "data", table), in.resolve("BookD"));
        // YAML コンテナ内の非 YAML ファイル・サブディレクトリは無視される
        Files.createFile(in.resolve("BookD/notes.txt"));
        Files.createDirectories(in.resolve("BookD/nested"));

        int count = TestDataConverter.convert(DataFormat.YAML, DataFormat.YAML, in, out);

        assertThat(count, is(1));
        Path yaml = out.resolve("BookD/data.yaml");
        assertThat(Files.exists(yaml), is(true));
        assertThat(Files.exists(out.resolve("BookD/notes.txt")), is(false));
        TestDataContainer read = new YamlFormatReader().read(out.resolve("BookD").toString(), "data");
        assertTable(table, onlyBlock(read));
    }

    // ---- 集約・構造・絞り込み・上書き・異常系 ----

    /**
     * Given: 2 シートを持つ Excel ブック。
     * When : XLS→YAML 変換。
     * Then : シートごとに YAML ファイルが出力され、1 コンテナへ集約される。
     */
    @Test
    public void aggregatesMultipleSheetsIntoOneContainer() {
        TableDataBlock t1 = sampleTable("T1");
        TableDataBlock t2 = sampleTable("T2");
        TestDataContainer container = new TestDataContainer("Multi", Arrays.asList(
                new TestDataSection("s1", Collections.<TestDataBlock>singletonList(t1)),
                new TestDataSection("s2", Collections.<TestDataBlock>singletonList(t2))));
        writeXls(container, in);

        int count = TestDataConverter.convert(DataFormat.XLS, DataFormat.YAML, in, out);

        assertThat(count, is(1));
        assertThat(Files.exists(out.resolve("Multi/s1.yaml")), is(true));
        assertThat(Files.exists(out.resolve("Multi/s2.yaml")), is(true));
        assertTable(t1, onlyBlock(new YamlFormatReader().read(out.resolve("Multi").toString(), "s1")));
        assertTable(t2, onlyBlock(new YamlFormatReader().read(out.resolve("Multi").toString(), "s2")));
    }

    /**
     * Given: サブディレクトリ配下の Excel ブック。
     * When : XLS→YAML 変換。
     * Then : 入力の相対構造が出力側に保たれる。
     */
    @Test
    public void preservesNestedStructure() throws IOException {
        Path pkg = Files.createDirectories(in.resolve("pkg/sub"));
        writeXls(oneTable("BookE", "data", sampleTable("USERS")), pkg);

        int count = TestDataConverter.convert(DataFormat.XLS, DataFormat.YAML, in, out);

        assertThat(count, is(1));
        assertThat(Files.exists(out.resolve("pkg/sub/BookE/data.yaml")), is(true));
    }

    /**
     * Given: 2 件の Excel ブックと exclude グロブ。
     * When : ConversionRequest 経由で XLS→YAML 変換。
     * Then : 除外されたブックは変換されない。
     */
    @Test
    public void appliesExcludeFromRequest() {
        writeXls(oneTable("Keep", "data", sampleTable("USERS")), in);
        writeXls(oneTable("Drop", "data", sampleTable("USERS")), in);

        int count = TestDataConverter.convert(new ConversionRequest.Builder()
                .sourceFormat(DataFormat.XLS).targetFormat(DataFormat.YAML)
                .inputPath(in).outputPath(out)
                .exclude("Drop.xlsx")
                .build());

        assertThat(count, is(1));
        assertThat(Files.exists(out.resolve("Keep/data.yaml")), is(true));
        assertThat(Files.exists(out.resolve("Drop")), is(false));
    }

    /**
     * Given: 出力先が既存・overwrite=false。
     * When : 変換。
     * Then : ConverterException で衝突を表面化する。
     */
    @Test
    public void failsOnExistingOutputWhenOverwriteFalse() throws IOException {
        writeXls(oneTable("BookA", "data", sampleTable("USERS")), in);
        Files.createDirectories(out.resolve("BookA"));
        Files.createFile(out.resolve("BookA/data.yaml"));

        try {
            TestDataConverter.convert(DataFormat.XLS, DataFormat.YAML, in, out);
            fail("should throw");
        } catch (ConverterException e) {
            assertThat(e.getMessage().startsWith("output already exists"), is(true));
        }
    }

    /**
     * Given: 出力先が既存・overwrite=true。
     * When : ConversionRequest 経由で変換。
     * Then : 上書きして変換成功。
     */
    @Test
    public void overwritesExistingOutputWhenOverwriteTrue() throws IOException {
        TableDataBlock table = sampleTable("USERS");
        writeXls(oneTable("BookA", "data", table), in);
        Files.createDirectories(out.resolve("BookA"));
        Files.write(out.resolve("BookA/data.yaml"), "stale".getBytes());

        int count = TestDataConverter.convert(new ConversionRequest.Builder()
                .sourceFormat(DataFormat.XLS).targetFormat(DataFormat.YAML)
                .inputPath(in).outputPath(out)
                .overwrite(true)
                .build());

        assertThat(count, is(1));
        assertTable(table, onlyBlock(new YamlFormatReader().read(out.resolve("BookA").toString(), "data")));
    }

    /**
     * Given: 入力ディレクトリが存在しない。
     * When : 変換。
     * Then : ConverterException。
     */
    @Test
    public void failsWhenInputMissing() {
        try {
            TestDataConverter.convert(DataFormat.XLS, DataFormat.YAML, in.resolve("nope"), out);
            fail("should throw");
        } catch (ConverterException e) {
            assertThat(e.getMessage().startsWith("input directory not found:"), is(true));
        }
    }

    /**
     * Given: 変換対象が 0 件（空の入力ディレクトリ）。
     * When : 変換。
     * Then : 0 件で正常終了（出力なし）。
     */
    @Test
    public void convertsNothingWhenNoTargets() {
        int count = TestDataConverter.convert(DataFormat.XLS, DataFormat.YAML, in, out);
        assertThat(count, is(0));
    }
}
