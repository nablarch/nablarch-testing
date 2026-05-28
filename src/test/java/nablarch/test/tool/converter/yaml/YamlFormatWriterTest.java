package nablarch.test.tool.converter.yaml;

import nablarch.test.core.reader.DataType;
import nablarch.test.tool.converter.ConverterException;
import nablarch.test.tool.converter.model.FieldDef;
import nablarch.test.tool.converter.model.FileDataBlock;
import nablarch.test.tool.converter.model.ListMapBlock;
import nablarch.test.tool.converter.model.MessageDataBlock;
import nablarch.test.tool.converter.model.RecordLayout;
import nablarch.test.tool.converter.model.TableDataBlock;
import nablarch.test.tool.converter.model.TestDataBlock;
import nablarch.test.tool.converter.model.TestDataContainer;
import nablarch.test.tool.converter.model.TestDataSection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

/**
 * {@link YamlFormatWriter} のテスト。
 *
 * <p>
 * YAML 出力仕様（7.4節）を検証する。
 * 出力先ディレクトリ構成: outputPath/containerName/sectionName.yaml
 * </p>
 */
public class YamlFormatWriterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final YamlFormatWriter sut = new YamlFormatWriter();

    // -------------------------------------------------------------------------
    // テーブルデータブロック
    // -------------------------------------------------------------------------

    /**
     * [Given] SETUP_TABLE ブロック（groupId なし）
     * [When]  write() を呼び出す
     * [Then]  setup_tables セクションに table/rows 形式で出力される
     */
    @Test
    public void writeSetupTable() throws Exception {
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "USER_MASTER",
                Arrays.asList("USER_ID", "NAME"),
                Arrays.asList(
                        Arrays.asList("001", "taro"),
                        Arrays.asList("002", "jiro")
                )
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("setup_tables:"));
        assertThat(yaml, containsString("table: \"USER_MASTER\""));
        assertThat(yaml, containsString("USER_ID: \"001\""));
        assertThat(yaml, containsString("NAME: \"taro\""));
        assertThat(yaml, containsString("USER_ID: \"002\""));
    }

    /**
     * [Given] EXPECTED_TABLE ブロック（groupId あり）
     * [When]  write() を呼び出す
     * [Then]  group_id が table の前に出力される
     */
    @Test
    public void writeExpectedTableWithGroupId() throws Exception {
        TestDataBlock block = new TableDataBlock(
                DataType.EXPECTED_TABLE_DATA, "case01", "ORDERS",
                Arrays.asList("ORDER_ID"),
                Arrays.asList(Arrays.asList("ORD001"))
        );
        TestDataContainer container = container("sheet1", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "sheet1");
        assertThat(yaml, containsString("expected_tables:"));
        assertThat(yaml, containsString("group_id: \"case01\""));
        assertThat(yaml, containsString("table: \"ORDERS\""));
    }

    /**
     * [Given] EXPECTED_COMPLETE_TABLE ブロック
     * [When]  write() を呼び出す
     * [Then]  expected_complete_tables セクションとして出力される
     */
    @Test
    public void writeExpectedCompleteTable() throws Exception {
        TestDataBlock block = new TableDataBlock(
                DataType.EXPECTED_COMPLETED, "", "ITEMS",
                Arrays.asList("ITEM_ID"),
                Collections.singletonList(Arrays.asList("I001"))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("expected_complete_tables:"));
    }

    /**
     * [Given] LIST_MAP ブロック
     * [When]  write() を呼び出す
     * [Then]  list_maps セクションに id/rows 形式で出力される
     */
    @Test
    public void writeListMap() throws Exception {
        TestDataBlock block = new ListMapBlock(
                "", "myList",
                Arrays.asList("KEY1", "KEY2"),
                Arrays.asList(Arrays.asList("a", "b"))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("list_maps:"));
        assertThat(yaml, containsString("id: \"myList\""));
        assertThat(yaml, containsString("KEY1: \"a\""));
    }

    /**
     * [Given] マーカーカラム（[FLAG]）を含む TABLE ブロック
     * [When]  write() を呼び出す
     * [Then]  "[FLAG]" がそのままキーとして出力される（HC-01）
     */
    @Test
    public void markerColumnPreserved() throws Exception {
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "TBL",
                Arrays.asList("[FLAG]", "NAME"),
                Arrays.asList(Arrays.asList("X", "foo"))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("\"[FLAG]\": \"X\""));
    }

    // -------------------------------------------------------------------------
    // 値の書き出し規則（7.4.1節）
    // -------------------------------------------------------------------------

    /**
     * [Given] null 値を含む行
     * [When]  write() を呼び出す
     * [Then]  アンクォートの null として出力される
     */
    @Test
    public void nullValueIsUnquoted() throws Exception {
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "TBL",
                Arrays.asList("COL"),
                Arrays.asList(Collections.singletonList(null))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("COL: null"));
    }

    /**
     * [Given] 空文字値を含む行
     * [When]  write() を呼び出す
     * [Then]  ダブルクォートで "" として出力される
     */
    @Test
    public void emptyStringIsQuoted() throws Exception {
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "TBL",
                Arrays.asList("COL"),
                Arrays.asList(Collections.singletonList(""))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("COL: \"\""));
    }

    /**
     * [Given] "null" という文字列値を含む行
     * [When]  write() を呼び出す
     * [Then]  ダブルクォートで "null" として出力される（YAML null と区別）
     */
    @Test
    public void stringNullIsQuoted() throws Exception {
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "TBL",
                Arrays.asList("COL"),
                Arrays.asList(Collections.singletonList("null"))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("COL: \"null\""));
    }

    /**
     * [Given] "001" のような先頭ゼロ付き文字列値
     * [When]  write() を呼び出す
     * [Then]  ダブルクォートで出力される
     */
    @Test
    public void leadingZeroStringIsQuoted() throws Exception {
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "TBL",
                Arrays.asList("COL"),
                Arrays.asList(Collections.singletonList("001"))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("COL: \"001\""));
    }

    // -------------------------------------------------------------------------
    // ファイルデータブロック（7.4.3節）
    // -------------------------------------------------------------------------

    /**
     * [Given] SETUP_FIXED ブロック（ディレクティブあり）
     * [When]  write() を呼び出す
     * [Then]  setup_files: type: fixed / directives / records が出力される
     */
    @Test
    public void writeSetupFixed() throws Exception {
        Map<String, String> directives = new LinkedHashMap<>();
        directives.put("text-encoding", "MS932");
        List<FieldDef> fields = Arrays.asList(
                new FieldDef("USER_ID", "X", "10"),
                new FieldDef("AMOUNT", "Z", "10")
        );
        RecordLayout record = new RecordLayout("DATA", fields,
                Arrays.asList(Arrays.asList("001", "5000")));
        FileDataBlock block = new FileDataBlock(
                DataType.SETUP_FIXED, "", "input/data.dat",
                FileDataBlock.FileType.FIXED, directives,
                Arrays.asList(record)
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("setup_files:"));
        assertThat(yaml, containsString("path: \"input/data.dat\""));
        assertThat(yaml, containsString("type: fixed"));
        assertThat(yaml, containsString("text-encoding: \"MS932\""));
        assertThat(yaml, containsString("record_type: \"DATA\""));
        assertThat(yaml, containsString("name: \"USER_ID\""));
        assertThat(yaml, containsString("type: \"X\""));
        assertThat(yaml, containsString("length: \"10\""));
        assertThat(yaml, containsString("[\"001\", \"5000\"]"));
    }

    /**
     * [Given] SETUP_VARIABLE ブロック（可変長: フィールドに length=null）
     * [When]  write() を呼び出す
     * [Then]  type: variable かつ length キーが省略される（7.4.3節）
     */
    @Test
    public void writeSetupVariableOmitsLength() throws Exception {
        List<FieldDef> fields = Arrays.asList(
                new FieldDef("NAME", "X", null)
        );
        RecordLayout record = new RecordLayout("DATA", fields,
                Arrays.asList(Arrays.asList("taro")));
        FileDataBlock block = new FileDataBlock(
                DataType.SETUP_VARIABLE, "", "out.csv",
                FileDataBlock.FileType.VARIABLE, new LinkedHashMap<>(),
                Arrays.asList(record)
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("type: variable"));
        assertThat(yaml, not(containsString("length:")));
    }

    /**
     * [Given] records が空リストのファイルデータブロック
     * [When]  write() を呼び出す
     * [Then]  records: [] として出力される（7.4.3節）
     */
    @Test
    public void writeFileBlockWithEmptyRecords() throws Exception {
        Map<String, String> directives = new LinkedHashMap<>();
        directives.put("text-encoding", "UTF-8");
        FileDataBlock block = new FileDataBlock(
                DataType.SETUP_VARIABLE, "", "empty.csv",
                FileDataBlock.FileType.VARIABLE, directives,
                Collections.emptyList()
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("records: []"));
    }

    // -------------------------------------------------------------------------
    // メッセージングデータブロック（7.4.4節）
    // -------------------------------------------------------------------------

    /**
     * [Given] MESSAGE ブロック（FW ヘッダあり）
     * [When]  write() を呼び出す
     * [Then]  messages: / FW_HEADER レコード / 通常レコードが出力される
     */
    @Test
    public void writeMessage() throws Exception {
        Map<String, String> fwHeaders = new LinkedHashMap<>();
        fwHeaders.put("requestId", "REQ001");
        fwHeaders.put("userId", "usr001");
        List<FieldDef> bodyFields = Arrays.asList(
                new FieldDef("FIELD1", "X", null),
                new FieldDef("FIELD2", "X", null)
        );
        RecordLayout bodyRecord = new RecordLayout("default", bodyFields,
                Arrays.asList(Arrays.asList("req1", "data1")));
        MessageDataBlock block = new MessageDataBlock(
                DataType.MESSAGE, "", "sendSyncTestData/REQ001/message",
                fwHeaders, Arrays.asList(bodyRecord)
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("messages:"));
        assertThat(yaml, containsString("id: \"sendSyncTestData/REQ001/message\""));
        assertThat(yaml, containsString("record_type: \"FW_HEADER\""));
        assertThat(yaml, containsString("name: \"requestId\""));
        assertThat(yaml, containsString("name: \"userId\""));
        assertThat(yaml, containsString("[\"REQ001\", \"usr001\"]"));
        assertThat(yaml, containsString("record_type: \"default\""));
        assertThat(yaml, containsString("name: \"FIELD1\""));
    }

    // -------------------------------------------------------------------------
    // ディレクトリ構成・複数セクション
    // -------------------------------------------------------------------------

    /**
     * [Given] 複数セクションを持つ TestDataContainer
     * [When]  write() を呼び出す
     * [Then]  各セクションが別 YAML ファイルとして出力される
     */
    @Test
    public void multipleSectionsWrittenToSeparateFiles() throws Exception {
        TestDataBlock b1 = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "T1",
                Arrays.asList("C1"),
                Arrays.asList(Arrays.asList("v1"))
        );
        TestDataBlock b2 = new TableDataBlock(
                DataType.EXPECTED_TABLE_DATA, "", "T2",
                Arrays.asList("C2"),
                Arrays.asList(Arrays.asList("v2"))
        );
        List<TestDataSection> sections = Arrays.asList(
                new TestDataSection("case01", Arrays.asList(b1)),
                new TestDataSection("case02", Arrays.asList(b2))
        );
        TestDataContainer container = new TestDataContainer("FooTest", sections);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        assertTrue(new File(outputDir, "FooTest/case01.yaml").exists());
        assertTrue(new File(outputDir, "FooTest/case02.yaml").exists());
    }

    /**
     * [Given] 既存ファイルがあり overwrite=false
     * [When]  write() を呼び出す
     * [Then]  ConverterException がスローされる
     */
    @Test(expected = ConverterException.class)
    public void overwriteFalseThrowsWhenFileExists() throws Exception {
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "T1",
                Arrays.asList("C1"),
                Arrays.asList(Arrays.asList("v1"))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);
        // 2回目で例外
        sut.write(container, outputDir.toPath(), false);
    }

    /**
     * [Given] 既存ファイルがあり overwrite=true
     * [When]  write() を呼び出す
     * [Then]  例外なく上書きされる
     */
    @Test
    public void overwriteTrueOverwritesExistingFile() throws Exception {
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "T1",
                Arrays.asList("C1"),
                Arrays.asList(Arrays.asList("v1"))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);
        sut.write(container, outputDir.toPath(), true);  // no exception
    }

    // -------------------------------------------------------------------------
    // 追加テスト（カバレッジ拡充）
    // -------------------------------------------------------------------------

    /**
     * [Given] containerName と同名のファイルが outputDir 直下に既に存在する
     * [When]  write() を呼び出す（Files.createDirectories がファイルパスで IOException を送出）
     * [Then]  ConverterException がスローされる
     */
    @Test(expected = ConverterException.class)
    public void iOExceptionOnContainerDirectoryCreationThrowsConverterException() throws Exception {
        // Given: "FooTest" という名前のファイルを作成（ディレクトリとして作れない）
        File outputDir = temporaryFolder.newFolder("out");
        new File(outputDir, "FooTest").createNewFile();

        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "T1",
                Arrays.asList("C1"), Arrays.asList(Arrays.asList("v1"))
        );
        TestDataContainer container = container("case01", block);

        // When: FooTest がファイルなので createDirectories が失敗する
        sut.write(container, outputDir.toPath(), false);
    }

    /**
     * [Given] TableDataBlock にカラム名はあるが rows が空
     * [When]  write() を呼び出す
     * [Then]  YAML 出力に "rows: []" が含まれる
     */
    @Test
    public void tableDataBlockWithEmptyRowsWritesEmptyRows() throws Exception {
        // Given
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "EMPTY_TBL",
                Arrays.asList("COL1", "COL2"),
                Collections.emptyList()
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        // Then
        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("rows: []"));
    }

    /**
     * [Given] FileDataBlock に group_id が設定されている
     * [When]  write() を呼び出す
     * [Then]  YAML 出力に "group_id: \"grpA\"" が含まれる
     */
    @Test
    public void fileDataBlockWithGroupIdWritesGroupId() throws Exception {
        // Given
        FileDataBlock block = new FileDataBlock(
                DataType.SETUP_FIXED, "grpA", "data.dat",
                FileDataBlock.FileType.FIXED, new LinkedHashMap<>(),
                Collections.emptyList()
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        // Then
        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("group_id: \"grpA\""));
    }

    /**
     * [Given] MessageDataBlock に group_id が設定されている
     * [When]  write() を呼び出す
     * [Then]  YAML 出力に "group_id: \"msgGrp\"" が含まれる
     */
    @Test
    public void messageDataBlockWithGroupIdWritesGroupId() throws Exception {
        // Given
        MessageDataBlock block = new MessageDataBlock(
                DataType.MESSAGE, "msgGrp", "req/msg",
                new LinkedHashMap<>(),
                Collections.emptyList()
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        // Then
        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("group_id: \"msgGrp\""));
    }

    /**
     * [Given] RecordLayout のフィールドに type=null
     * [When]  write() を呼び出す
     * [Then]  YAML 出力が "{name: \"FIELD1\"}" となり type キーを含まない
     */
    @Test
    public void fieldWithNullTypeWritesNameOnly() throws Exception {
        // Given: type=null のフィールドを持つ MessageDataBlock
        List<FieldDef> fields = Arrays.asList(new FieldDef("FIELD1", null, null));
        RecordLayout record = new RecordLayout("default", fields,
                Arrays.asList(Arrays.asList("val")));
        MessageDataBlock block = new MessageDataBlock(
                DataType.MESSAGE, "", "req/msg",
                new LinkedHashMap<>(),
                Arrays.asList(record)
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        // Then: name のみの形式で出力され、, type: ... が field 定義に含まれない
        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("{name: \"FIELD1\"}"));
        // FieldDef の type が null の場合、field 行に ", type:" が含まれない
        assertThat(yaml, not(containsString(", type:")));
    }

    /**
     * [Given] EXPECTED_REQUEST_HEADER_MESSAGES ブロック
     * [When]  write() を呼び出す
     * [Then]  YAML 出力に "expected_request_header_messages:" が含まれる
     */
    @Test
    public void writeExpectedRequestHeaderMessages() throws Exception {
        // Given
        MessageDataBlock block = new MessageDataBlock(
                DataType.EXPECTED_REQUEST_HEADER_MESSAGES, "", "req/hdr",
                new LinkedHashMap<>(),
                Collections.emptyList()
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("expected_request_header_messages:"));
    }

    /**
     * [Given] EXPECTED_REQUEST_BODY_MESSAGES ブロック
     * [When]  write() を呼び出す
     * [Then]  YAML 出力に "expected_request_body_messages:" が含まれる
     */
    @Test
    public void writeExpectedRequestBodyMessages() throws Exception {
        // Given
        MessageDataBlock block = new MessageDataBlock(
                DataType.EXPECTED_REQUEST_BODY_MESSAGES, "", "req/body",
                new LinkedHashMap<>(),
                Collections.emptyList()
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("expected_request_body_messages:"));
    }

    /**
     * [Given] RESPONSE_HEADER_MESSAGES ブロック
     * [When]  write() を呼び出す
     * [Then]  YAML 出力に "response_header_messages:" が含まれる
     */
    @Test
    public void writeResponseHeaderMessages() throws Exception {
        // Given
        MessageDataBlock block = new MessageDataBlock(
                DataType.RESPONSE_HEADER_MESSAGES, "", "res/hdr",
                new LinkedHashMap<>(),
                Collections.emptyList()
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("response_header_messages:"));
    }

    /**
     * [Given] RESPONSE_BODY_MESSAGES ブロック
     * [When]  write() を呼び出す
     * [Then]  YAML 出力に "response_body_messages:" が含まれる
     */
    @Test
    public void writeResponseBodyMessages() throws Exception {
        // Given
        MessageDataBlock block = new MessageDataBlock(
                DataType.RESPONSE_BODY_MESSAGES, "", "res/body",
                new LinkedHashMap<>(),
                Collections.emptyList()
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        String yaml = readYaml(outputDir, "FooTest", "case01");
        assertThat(yaml, containsString("response_body_messages:"));
    }

    /**
     * [Given] Files.newBufferedWriter が IOException をスローする状況
     * [When]  write() を呼び出す
     * [Then]  ConverterException がスローされる
     */
    @Test(expected = ConverterException.class)
    public void iOExceptionOnWriterThrowsConverterException() throws Exception {
        // Given
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "T1",
                Arrays.asList("C1"), Arrays.asList(Arrays.asList("v1"))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            // createDirectories は実際に実行させる
            filesMock.when(() -> Files.createDirectories(any(Path.class)))
                    .thenCallRealMethod();
            // exists は実際に実行させる
            filesMock.when(() -> Files.exists(any(Path.class)))
                    .thenCallRealMethod();
            // newBufferedWriter は IOException をスロー
            filesMock.when(() -> Files.newBufferedWriter(any(Path.class), any(java.nio.charset.Charset.class)))
                    .thenThrow(new IOException("Simulated write failure"));

            sut.write(container, outputDir.toPath(), false);
        }
    }

    // -------------------------------------------------------------------------
    // ヘルパー
    // -------------------------------------------------------------------------

    private TestDataContainer container(String sectionName, TestDataBlock block) {
        return new TestDataContainer("FooTest",
                Arrays.asList(new TestDataSection(sectionName, Arrays.asList(block))));
    }

    private String readYaml(File outputDir, String containerName, String sectionName) throws Exception {
        File yaml = new File(outputDir, containerName + "/" + sectionName + ".yaml");
        return new String(Files.readAllBytes(yaml.toPath()), StandardCharsets.UTF_8);
    }
}
