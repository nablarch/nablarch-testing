package nablarch.test.tool.converter.yaml;

import nablarch.test.core.reader.DataType;
import nablarch.test.tool.converter.ConverterException;
import nablarch.test.tool.converter.model.FileDataBlock;
import nablarch.test.tool.converter.model.ListMapBlock;
import nablarch.test.tool.converter.model.MessageDataBlock;
import nablarch.test.tool.converter.model.RecordLayout;
import nablarch.test.tool.converter.model.TableDataBlock;
import nablarch.test.tool.converter.model.TestDataContainer;
import nablarch.test.tool.converter.model.TestDataSection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * {@link YamlFormatReader} のテスト。
 *
 * <p>YAML IN 仕様（7.3節）を検証する。</p>
 */
public class YamlFormatReaderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final YamlFormatReader sut = new YamlFormatReader();

    // -------------------------------------------------------------------------
    // テーブルデータブロック
    // -------------------------------------------------------------------------

    /**
     * [Given] setup_tables を含む YAML ディレクトリ
     * [When]  read() を呼び出す
     * [Then]  SETUP_TABLE_DATA の TableDataBlock が取得できる
     */
    @Test
    public void readSetupTable() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "setup_tables:",
                "  - table: USER_MASTER",
                "    rows:",
                "      - USER_ID: \"001\"",
                "        NAME: \"taro\""
        );

        TestDataContainer result = sut.read(dir.toPath());

        assertThat(result.getName(), is("FooTest"));
        assertThat(result.getSections().size(), is(1));
        TestDataSection section = result.getSections().get(0);
        assertThat(section.getName(), is("case01"));
        TableDataBlock block = (TableDataBlock) section.getBlocks().get(0);
        assertThat(block.getDataType(), is(DataType.SETUP_TABLE_DATA));
        assertThat(block.getIdentifier(), is("USER_MASTER"));
        assertThat(block.getGroupId(), is(""));
        assertThat(block.getColumnNames(), is(Arrays.asList("USER_ID", "NAME")));
        assertThat(block.getRows().get(0), is(Arrays.asList("001", "taro")));
    }

    /**
     * [Given] expected_tables を含む YAML ディレクトリ
     * [When]  read() を呼び出す
     * [Then]  EXPECTED_TABLE_DATA の TableDataBlock が取得できる
     */
    @Test
    public void readExpectedTable() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "expected_tables:",
                "  - table: ORDERS",
                "    rows:",
                "      - ORDER_ID: \"ORD001\""
        );

        TestDataContainer result = sut.read(dir.toPath());

        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDataType(), is(DataType.EXPECTED_TABLE_DATA));
        assertThat(block.getIdentifier(), is("ORDERS"));
    }

    /**
     * [Given] expected_complete_tables を含む YAML ディレクトリ
     * [When]  read() を呼び出す
     * [Then]  EXPECTED_COMPLETED の TableDataBlock が取得できる
     */
    @Test
    public void readExpectedCompleteTable() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "expected_complete_tables:",
                "  - table: ITEMS",
                "    rows:",
                "      - ID: \"1\""
        );

        TestDataContainer result = sut.read(dir.toPath());

        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDataType(), is(DataType.EXPECTED_COMPLETED));
    }

    /**
     * [Given] group_id フィールドを含む YAML エントリ
     * [When]  read() を呼び出す
     * [Then]  groupId が設定される（7.3.2節）
     */
    @Test
    public void readGroupId() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "setup_tables:",
                "  - group_id: grpA",
                "    table: TBL",
                "    rows:",
                "      - C1: \"v1\""
        );

        TestDataContainer result = sut.read(dir.toPath());

        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getGroupId(), is("grpA"));
    }

    /**
     * [Given] YAML ネイティブ null を含む行
     * [When]  read() を呼び出す
     * [Then]  Java null として保持される（7.3.2節）
     */
    @Test
    public void nativeNullPreservedAsNull() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "setup_tables:",
                "  - table: TBL",
                "    rows:",
                "      - COL: null"
        );

        TestDataContainer result = sut.read(dir.toPath());

        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getRows().get(0).get(0), is(nullValue()));
    }

    /**
     * [Given] list_maps を含む YAML ディレクトリ
     * [When]  read() を呼び出す
     * [Then]  LIST_MAP の ListMapBlock が取得できる
     */
    @Test
    public void readListMap() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "list_maps:",
                "  - id: myList",
                "    rows:",
                "      - KEY1: \"a\"",
                "        KEY2: \"b\""
        );

        TestDataContainer result = sut.read(dir.toPath());

        ListMapBlock block = (ListMapBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDataType(), is(DataType.LIST_MAP));
        assertThat(block.getIdentifier(), is("myList"));
        assertThat(block.getColumnNames(), is(Arrays.asList("KEY1", "KEY2")));
    }

    /**
     * [Given] マーカーカラム "[NO]" を含む YAML
     * [When]  read() を呼び出す
     * [Then]  "[NO]" がそのまま columnNames に保持される
     */
    @Test
    public void markerColumnPreserved() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "list_maps:",
                "  - id: myList",
                "    rows:",
                "      - \"[NO]\": \"1\"",
                "        KEY1: \"a\""
        );

        TestDataContainer result = sut.read(dir.toPath());

        ListMapBlock block = (ListMapBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getColumnNames(), is(Arrays.asList("[NO]", "KEY1")));
    }

    // -------------------------------------------------------------------------
    // ファイルデータブロック
    // -------------------------------------------------------------------------

    /**
     * [Given] setup_files（fixed）を含む YAML ディレクトリ
     * [When]  read() を呼び出す
     * [Then]  SETUP_FIXED の FileDataBlock が取得できる
     */
    @Test
    public void readSetupFixed() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "setup_files:",
                "  - path: input/data.dat",
                "    type: fixed",
                "    directives:",
                "      text-encoding: \"MS932\"",
                "    records:",
                "      - record_type: DATA",
                "        fields:",
                "          - {name: USER_ID, type: X, length: 10}",
                "        rows:",
                "          - [\"001\"]"
        );

        TestDataContainer result = sut.read(dir.toPath());

        FileDataBlock block = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDataType(), is(DataType.SETUP_FIXED));
        assertThat(block.getFileType(), is(FileDataBlock.FileType.FIXED));
        assertThat(block.getIdentifier(), is("input/data.dat"));
        assertThat(block.getDirectives().get("text-encoding"), is("MS932"));
        RecordLayout record = block.getRecords().get(0);
        assertThat(record.getRecordType(), is("DATA"));
        assertThat(record.getFields().get(0).getName(), is("USER_ID"));
        assertThat(record.getFields().get(0).getType(), is("X"));
        assertThat(record.getFields().get(0).getLength(), is("10"));
        assertThat(record.getRows().get(0), is(Collections.singletonList("001")));
    }

    /**
     * [Given] setup_files（variable）を含む YAML ディレクトリ
     * [When]  read() を呼び出す
     * [Then]  SETUP_VARIABLE の FileDataBlock が取得できる（length は null）
     */
    @Test
    public void readSetupVariable() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "setup_files:",
                "  - path: out.csv",
                "    type: variable",
                "    records:",
                "      - record_type: DATA",
                "        fields:",
                "          - {name: NAME, type: X}",
                "        rows:",
                "          - [\"taro\"]"
        );

        TestDataContainer result = sut.read(dir.toPath());

        FileDataBlock block = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDataType(), is(DataType.SETUP_VARIABLE));
        assertThat(block.getFileType(), is(FileDataBlock.FileType.VARIABLE));
        assertThat(block.getRecords().get(0).getFields().get(0).getLength(), is(nullValue()));
    }

    /**
     * [Given] records が空の YAML（records: []）
     * [When]  read() を呼び出す
     * [Then]  records が空リストの FileDataBlock が取得できる
     */
    @Test
    public void readFileBlockWithEmptyRecords() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "setup_files:",
                "  - path: empty.csv",
                "    type: variable",
                "    directives:",
                "      text-encoding: \"UTF-8\"",
                "    records: []"
        );

        TestDataContainer result = sut.read(dir.toPath());

        FileDataBlock block = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertTrue(block.getRecords().isEmpty());
    }

    // -------------------------------------------------------------------------
    // メッセージングデータブロック
    // -------------------------------------------------------------------------

    /**
     * [Given] messages（FW_HEADER + 通常レコード）を含む YAML ディレクトリ
     * [When]  read() を呼び出す
     * [Then]  fwHeaderFields が構築され、通常レコードが records に格納される（7.3.4節）
     */
    @Test
    public void readMessage() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "messages:",
                "  - id: sendSyncTestData/REQ001/message",
                "    fw_header:",
                "      requestId: \"REQ001\"",
                "      userId: \"usr001\"",
                "    records:",
                "      - record_type: default",
                "        fields:",
                "          - {name: FIELD1, type: X}",
                "        rows:",
                "          - [\"req1\"]"
        );

        TestDataContainer result = sut.read(dir.toPath());

        MessageDataBlock block = (MessageDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDataType(), is(DataType.MESSAGE));
        assertThat(block.getIdentifier(), is("sendSyncTestData/REQ001/message"));
        assertThat(block.getFwHeaderFields().get("requestId"), is("REQ001"));
        assertThat(block.getFwHeaderFields().get("userId"), is("usr001"));
        assertThat(block.getRecords().size(), is(1));
        assertThat(block.getRecords().get(0).getRecordType(), is("default"));
        assertThat(block.getRecords().get(0).getFields().get(0).getName(), is("FIELD1"));
    }

    // -------------------------------------------------------------------------
    // 複数セクション・複数ブロック
    // -------------------------------------------------------------------------

    /**
     * [Given] 複数 YAML ファイルを持つコンテナディレクトリ
     * [When]  read() を呼び出す
     * [Then]  各 YAML ファイルが TestDataSection として格納される
     */
    @Test
    public void multipleSectionsFromMultipleYamlFiles() throws Exception {
        File dir = temporaryFolder.newFolder("FooTest");
        writeYaml(new File(dir, "case01.yaml"),
                "setup_tables:",
                "  - table: T1",
                "    rows:",
                "      - C: \"v1\""
        );
        writeYaml(new File(dir, "case02.yaml"),
                "expected_tables:",
                "  - table: T2",
                "    rows:",
                "      - C: \"v2\""
        );

        TestDataContainer result = sut.read(dir.toPath());

        assertThat(result.getName(), is("FooTest"));
        assertThat(result.getSections().size(), is(2));
    }

    /**
     * [Given] 複数のブロックを持つ YAML ファイル
     * [When]  read() を呼び出す
     * [Then]  全ブロックが TestDataSection.blocks に格納される
     */
    @Test
    public void multipleBlocksInOneSection() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "setup_tables:",
                "  - table: T1",
                "    rows:",
                "      - C: \"v1\"",
                "expected_tables:",
                "  - table: T2",
                "    rows:",
                "      - C: \"v2\""
        );

        TestDataContainer result = sut.read(dir.toPath());

        assertThat(result.getSections().get(0).getBlocks().size(), is(2));
    }

    // -------------------------------------------------------------------------
    // エラーケース
    // -------------------------------------------------------------------------

    /**
     * [Given] 存在しないディレクトリパス
     * [When]  read() を呼び出す
     * [Then]  ConverterException がスローされる
     */
    @Test(expected = ConverterException.class)
    public void directoryNotFound() throws Exception {
        sut.read(temporaryFolder.getRoot().toPath().resolve("nonexistent"));
    }

    // -------------------------------------------------------------------------
    // 追加テスト（カバレッジ拡充）
    // -------------------------------------------------------------------------

    /**
     * [Given] 空の YAML ファイル（0バイト）
     * [When]  read() を呼び出す
     * [Then]  正常終了し、0ブロックのセクションが返される
     */
    @Test
    public void emptyYamlFileResultsInEmptySection() throws Exception {
        // Given: 空ファイル
        File dir = temporaryFolder.newFolder("FooTest");
        new File(dir, "case01.yaml").createNewFile();

        // When
        TestDataContainer result = sut.read(dir.toPath());

        // Then
        assertThat(result.getSections().size(), is(1));
        assertThat(result.getSections().get(0).getBlocks().size(), is(0));
    }

    /**
     * [Given] YAML ルートがリスト（マッピングでない）
     * [When]  read() を呼び出す
     * [Then]  ConverterException がスローされる
     */
    @Test(expected = ConverterException.class)
    public void yamlRootIsListThrowsConverterException() throws Exception {
        // Given: ルートがリスト形式の YAML
        File dir = makeDir("FooTest", "case01",
                "- item1",
                "- item2"
        );

        // When
        sut.read(dir.toPath());
    }

    /**
     * [Given] expected_request_header_messages セクションを含む YAML
     * [When]  read() を呼び出す
     * [Then]  DataType が EXPECTED_REQUEST_HEADER_MESSAGES のブロックが取得できる
     */
    @Test
    public void readExpectedRequestHeaderMessages() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "expected_request_header_messages:",
                "  - id: msg1",
                "    records:",
                "      - record_type: default",
                "        fields:",
                "          - {name: F1}",
                "        rows:",
                "          - [\"v1\"]"
        );

        TestDataContainer result = sut.read(dir.toPath());

        assertThat(result.getSections().get(0).getBlocks().size(), is(1));
        assertThat(result.getSections().get(0).getBlocks().get(0).getDataType(),
                is(DataType.EXPECTED_REQUEST_HEADER_MESSAGES));
    }

    /**
     * [Given] expected_request_body_messages セクションを含む YAML
     * [When]  read() を呼び出す
     * [Then]  DataType が EXPECTED_REQUEST_BODY_MESSAGES のブロックが取得できる
     */
    @Test
    public void readExpectedRequestBodyMessages() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "expected_request_body_messages:",
                "  - id: msg2",
                "    records:",
                "      - record_type: default",
                "        fields:",
                "          - {name: F1}",
                "        rows:",
                "          - [\"v1\"]"
        );

        TestDataContainer result = sut.read(dir.toPath());

        assertThat(result.getSections().get(0).getBlocks().get(0).getDataType(),
                is(DataType.EXPECTED_REQUEST_BODY_MESSAGES));
    }

    /**
     * [Given] response_header_messages セクションを含む YAML
     * [When]  read() を呼び出す
     * [Then]  DataType が RESPONSE_HEADER_MESSAGES のブロックが取得できる
     */
    @Test
    public void readResponseHeaderMessages() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "response_header_messages:",
                "  - id: msg3",
                "    records:",
                "      - record_type: default",
                "        fields:",
                "          - {name: F1}",
                "        rows:",
                "          - [\"v1\"]"
        );

        TestDataContainer result = sut.read(dir.toPath());

        assertThat(result.getSections().get(0).getBlocks().get(0).getDataType(),
                is(DataType.RESPONSE_HEADER_MESSAGES));
    }

    /**
     * [Given] response_body_messages セクションを含む YAML
     * [When]  read() を呼び出す
     * [Then]  DataType が RESPONSE_BODY_MESSAGES のブロックが取得できる
     */
    @Test
    public void readResponseBodyMessages() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "response_body_messages:",
                "  - id: msg4",
                "    records:",
                "      - record_type: default",
                "        fields:",
                "          - {name: F1}",
                "        rows:",
                "          - [\"v1\"]"
        );

        TestDataContainer result = sut.read(dir.toPath());

        assertThat(result.getSections().get(0).getBlocks().get(0).getDataType(),
                is(DataType.RESPONSE_BODY_MESSAGES));
    }

    /**
     * [Given] "case01.yaml" という名前のサブディレクトリが存在する
     * [When]  read() を呼び出す
     * [Then]  FileInputStream がディレクトリ上でスローする IOException が ConverterException にラップされる
     */
    @Test(expected = ConverterException.class)
    public void yamlEntryIsDirectoryThrowsConverterException() throws Exception {
        // Given: case01.yaml という名前のディレクトリを作成
        File dir = temporaryFolder.newFolder("FooTest");
        new File(dir, "case01.yaml").mkdir();

        // When: ディレクトリを FileInputStream で開こうとして IOException → ConverterException
        sut.read(dir.toPath());
    }

    /**
     * [Given] expected_files セクションを含む YAML（固定長）
     * [When]  read() を呼び出す
     * [Then]  DataType が EXPECTED_FIXED の FileDataBlock が取得できる
     */
    @Test
    public void readExpectedFilesFixed() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "expected_files:",
                "  - path: \"output/data.dat\"",
                "    type: fixed",
                "    records:",
                "      - record_type: DATA",
                "        fields:",
                "          - {name: FIELD1, type: X, length: \"10\"}",
                "        rows:",
                "          - [\"val1\"]"
        );

        TestDataContainer result = sut.read(dir.toPath());

        FileDataBlock block = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDataType(), is(DataType.EXPECTED_FIXED));
        assertThat(block.getFileType(), is(FileDataBlock.FileType.FIXED));
        assertThat(block.getIdentifier(), is("output/data.dat"));
    }

    /**
     * [Given] expected_files セクションを含む YAML（可変長）
     * [When]  read() を呼び出す
     * [Then]  DataType が EXPECTED_VARIABLE の FileDataBlock が取得できる
     */
    @Test
    public void readExpectedFilesVariable() throws Exception {
        File dir = makeDir("FooTest", "case01",
                "expected_files:",
                "  - path: \"output/data.csv\"",
                "    type: variable",
                "    records:",
                "      - record_type: DATA",
                "        fields:",
                "          - {name: FIELD1, type: X}",
                "        rows:",
                "          - [\"val1\"]"
        );

        TestDataContainer result = sut.read(dir.toPath());

        FileDataBlock block = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDataType(), is(DataType.EXPECTED_VARIABLE));
        assertThat(block.getFileType(), is(FileDataBlock.FileType.VARIABLE));
    }

    /**
     * [Given] YAML に rows: null（リストでない値）が設定されたブロック
     * [When]  read() を呼び出す
     * [Then]  castList が空リストにフォールバックし、例外なく解析できる
     */
    @Test
    public void castListFallbackOnNonListValue() throws Exception {
        // Given: rows に文字列（リストでない）を設定するとcastListが空リストを返す
        // setup_tables ブロックで rows をスカラーにする
        File dir = makeDir("FooTest", "case01",
                "setup_tables:",
                "  - table: TBL",
                "    rows: \"not_a_list\""
        );

        // When: castList が非List値に対して emptyList を返し、例外なく処理される
        TestDataContainer result = sut.read(dir.toPath());

        // Then: ブロックが解析され rows が空（castList フォールバック）
        assertThat(result.getSections().get(0).getBlocks().size(), is(1));
        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getRows().size(), is(0));
    }

    /**
     * [Given] YAML に records に非マッピング値が含まれるブロック
     * [When]  read() を呼び出す
     * [Then]  castMap が空マップにフォールバックし、例外なく解析できる
     */
    @Test
    public void castMapFallbackOnNonMapValue() throws Exception {
        // Given: records リストの要素に文字列（マッピングでない）を設定
        File dir = makeDir("FooTest", "case01",
                "setup_files:",
                "  - path: \"data.dat\"",
                "    type: variable",
                "    records:",
                "      - \"not_a_map\""
        );

        // When: castMap が非Map値に対して emptyMap を返し、例外なく処理される
        TestDataContainer result = sut.read(dir.toPath());

        // Then: ブロックが解析され、不正エントリが空マップとして扱われる
        assertThat(result.getSections().get(0).getBlocks().size(), is(1));
        FileDataBlock block = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        // 非マップエントリは空マップとして処理され、recordType="" の RecordLayout になる
        assertThat(block.getRecords().size(), is(1));
    }

    /**
     * [Given] ディレクトリではなくファイルのパスを渡す
     * [When]  read() を呼び出す
     * [Then]  ConverterException がスローされる
     */
    @Test(expected = ConverterException.class)
    public void pathIsFileNotDirectoryThrowsConverterException() throws Exception {
        File file = temporaryFolder.newFile("notADir.yaml");
        sut.read(file.toPath());
    }

    /**
     * [Given] 読み取り権限のないディレクトリ（listFiles() が null を返す）
     * [When]  read() を呼び出す
     * [Then]  ConverterException がスローされる
     */
    @Test(expected = ConverterException.class)
    public void listFilesReturnsNullThrowsConverterException() throws Exception {
        File dir = temporaryFolder.newFolder("FooTest");
        dir.setReadable(false);
        try {
            sut.read(dir.toPath());
        } finally {
            dir.setReadable(true);
        }
    }

    // -------------------------------------------------------------------------
    // ヘルパー
    // -------------------------------------------------------------------------

    /** 単一 YAML ファイルを含むコンテナディレクトリを作成する。 */
    private File makeDir(String containerName, String sectionName, String... lines) throws Exception {
        File dir = temporaryFolder.newFolder(containerName);
        writeYaml(new File(dir, sectionName + ".yaml"), lines);
        return dir;
    }

    private void writeYaml(File file, String... lines) throws Exception {
        PrintWriter pw = new PrintWriter(file, "UTF-8");
        try {
            for (String line : lines) {
                pw.println(line);
            }
        } finally {
            pw.close();
        }
    }
}
