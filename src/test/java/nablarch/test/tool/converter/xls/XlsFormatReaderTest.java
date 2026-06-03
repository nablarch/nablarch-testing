package nablarch.test.tool.converter.xls;

import nablarch.test.core.reader.DataType;
import nablarch.test.tool.converter.ConverterException;
import nablarch.test.tool.converter.model.ColumnRowDataBlock;
import nablarch.test.tool.converter.model.FileDataBlock;
import nablarch.test.tool.converter.model.ListMapBlock;
import nablarch.test.tool.converter.model.MessageDataBlock;
import nablarch.test.tool.converter.model.RecordLayout;
import nablarch.test.tool.converter.model.TableDataBlock;
import nablarch.test.tool.converter.model.TestDataBlock;
import nablarch.test.tool.converter.model.TestDataContainer;
import nablarch.test.tool.converter.model.TestDataSection;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * {@link XlsFormatReader} のテストクラス。
 */
public class XlsFormatReaderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final XlsFormatReader sut = new XlsFormatReader();

    // -------------------------------------------------------------------------
    // テーブルデータブロック（DT-01〜DT-03, SS-01, HC-01, HC-03, HC-04）
    // -------------------------------------------------------------------------

    /**
     * [Given] SETUP_TABLE ブロックを含む XLS ファイル
     * [When]  read() を呼び出す
     * [Then]  TestDataContainer にテーブルデータブロックが格納される
     */
    @Test
    public void readSetupTable() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_TABLE=USER_MASTER", "", ""},
                {"USER_ID", "NAME", "AGE"},
                {"001", "taro", "20"},
                {"002", "jiro", "30"}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        assertThat(result.getName(), is("FooTest"));
        assertThat(result.getSections().size(), is(1));
        TestDataSection section = result.getSections().get(0);
        assertThat(section.getName(), is("case01"));
        assertThat(section.getBlocks().size(), is(1));

        TableDataBlock block = (TableDataBlock) section.getBlocks().get(0);
        assertThat(block.getDataType(), is(DataType.SETUP_TABLE_DATA));
        assertThat(block.getGroupId(), is(""));
        assertThat(block.getIdentifier(), is("USER_MASTER"));
        assertThat(block.getColumnNames(), is(Arrays.asList("USER_ID", "NAME", "AGE")));
        assertThat(block.getRows().size(), is(2));
        assertThat(block.getRows().get(0), is(Arrays.asList("001", "taro", "20")));
        assertThat(block.getRows().get(1), is(Arrays.asList("002", "jiro", "30")));
    }

    /**
     * [Given] EXPECTED_TABLE ブロック
     * [When]  read() を呼び出す
     * [Then]  dataType が EXPECTED_TABLE_DATA になる
     */
    @Test
    public void readExpectedTable() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"EXPECTED_TABLE=ORDERS", ""},
                {"ORDER_ID", "STATUS"},
                {"100", "DONE"}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDataType(), is(DataType.EXPECTED_TABLE_DATA));
        assertThat(block.getIdentifier(), is("ORDERS"));
    }

    /**
     * [Given] EXPECTED_COMPLETE_TABLE ブロック（DT-01）
     * [When]  read() を呼び出す
     * [Then]  dataType が EXPECTED_COMPLETED になる
     */
    @Test
    public void readExpectedCompleteTable() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"EXPECTED_COMPLETE_TABLE=ITEMS", ""},
                {"ITEM_ID", "PRICE"},
                {"A01", "500"}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDataType(), is(DataType.EXPECTED_COMPLETED));
        assertThat(block.getIdentifier(), is("ITEMS"));
    }

    /**
     * [Given] groupId 付き識別行（DT-06）
     * [When]  read() を呼び出す
     * [Then]  groupId が取得できる
     */
    @Test
    public void readGroupId() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_TABLE[case01]=USER_MASTER", ""},
                {"USER_ID", "NAME"},
                {"001", "taro"}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        TestDataBlock block = result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getGroupId(), is("case01"));
        assertThat(block.getIdentifier(), is("USER_MASTER"));
    }

    /**
     * [Given] DataType 判定は前方一致（DT-03）
     * [When]  read() を呼び出す
     * [Then]  先頭セルが DataType 名で前方一致する行が識別行として解析される
     */
    @Test
    public void dataTypeMatchByStartsWith() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_TABLE=T1", ""},
                {"COL1", "COL2"},
                {"v1", "v2"}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        assertThat(result.getSections().get(0).getBlocks().size(), is(1));
        assertThat(result.getSections().get(0).getBlocks().get(0).getDataType(), is(DataType.SETUP_TABLE_DATA));
    }

    /**
     * [Given] ヘッダ末尾に空カラムがある（HC-03）
     * [When]  read() を呼び出す
     * [Then]  末尾の空カラムは除去される
     */
    @Test
    public void headerTrailingEmptyColumnsRemoved() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_TABLE=T1", "", "", ""},
                {"COL1", "COL2", "", ""},
                {"v1", "v2", "", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getColumnNames(), is(Arrays.asList("COL1", "COL2")));
        assertThat(block.getRows().get(0), is(Arrays.asList("v1", "v2")));
    }

    /**
     * [Given] データ行がヘッダより短い（HC-04）
     * [When]  read() を呼び出す
     * [Then]  不足分は空文字で補完される
     */
    @Test
    public void dataRowShorterThanHeader() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_TABLE=T1", "", ""},
                {"COL1", "COL2", "COL3"},
                {"v1"}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getRows().get(0), is(Arrays.asList("v1", "", "")));
    }

    /**
     * [Given] マーカーカラム "[FLAG]" 形式（HC-01）
     * [When]  read() を呼び出す
     * [Then]  "[" "]" を含めてそのまま保持される
     */
    @Test
    public void markerColumnPreserved() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_TABLE=T1", "", ""},
                {"COL1", "[FLAG]", "COL2"},
                {"v1", "X", "v2"}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getColumnNames(), is(Arrays.asList("COL1", "[FLAG]", "COL2")));
    }

    // -------------------------------------------------------------------------
    // LIST_MAP
    // -------------------------------------------------------------------------

    /**
     * [Given] LIST_MAP ブロック
     * [When]  read() を呼び出す
     * [Then]  ListMapBlock として格納される
     */
    @Test
    public void readListMap() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"LIST_MAP=resultSet", ""},
                {"KEY1", "KEY2"},
                {"a", "b"}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        TestDataBlock block = result.getSections().get(0).getBlocks().get(0);
        assertThat(block, instanceOf(ListMapBlock.class));
        assertThat(block.getDataType(), is(DataType.LIST_MAP));
    }

    // -------------------------------------------------------------------------
    // コメント行・空行（HC-05, HC-06, HC-07）
    // -------------------------------------------------------------------------

    /**
     * [Given] コメント行（先頭セルが "//" で始まる行）（HC-05）
     * [When]  read() を呼び出す
     * [Then]  コメント行はスキップされる
     */
    @Test
    public void commentLineSkipped() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"// comment", "", ""},
                {"SETUP_TABLE=T1", "", ""},
                {"COL1", "COL2", ""},
                {"// another comment", "", ""},
                {"v1", "v2", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getRows().size(), is(1));
        assertThat(block.getRows().get(0), is(Arrays.asList("v1", "v2")));
    }

    /**
     * [Given] 行内コメント（先頭以外のセルが "//" で始まる）（HC-06）
     * [When]  read() を呼び出す
     * [Then]  "//" 以降のセルが切り捨てられ HC-04 で補完される
     */
    @Test
    public void inlineCommentTruncated() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_TABLE=T1", "", "", ""},
                {"COL1", "COL2", "COL3", ""},
                {"v1", "// cut here", "should be cut", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then: COL2 以降切り捨て → HC-04 で空文字補完
        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getRows().get(0), is(Arrays.asList("v1", "", "")));
    }

    /**
     * [Given] 全セルが空の行（HC-07）
     * [When]  read() を呼び出す
     * [Then]  空行はスキップされる
     */
    @Test
    public void emptyRowSkipped() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_TABLE=T1", ""},
                {"COL1", "COL2"},
                {"", ""},
                {"v1", "v2"}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getRows().size(), is(1));
        assertThat(block.getRows().get(0), is(Arrays.asList("v1", "v2")));
    }

    // -------------------------------------------------------------------------
    // 複数シート・複数ブロック
    // -------------------------------------------------------------------------

    /**
     * [Given] 複数シートを持つ XLS ファイル
     * [When]  read() を呼び出す
     * [Then]  各シートが TestDataSection として格納される
     */
    @Test
    public void multipleSheetsBecomeSections() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        Workbook wb = new HSSFWorkbook();
        Sheet s1 = wb.createSheet("case01");
        row(s1, 0, "SETUP_TABLE=T1", "");
        row(s1, 1, "COL1", "");
        row(s1, 2, "v1", "");
        Sheet s2 = wb.createSheet("case02");
        row(s2, 0, "EXPECTED_TABLE=T2", "");
        row(s2, 1, "COL2", "");
        row(s2, 2, "v2", "");
        FileOutputStream out = new FileOutputStream(xls);
        try {
            wb.write(out);
        } finally {
            out.close();
        }

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        assertThat(result.getSections().size(), is(2));
        assertThat(result.getSections().get(0).getName(), is("case01"));
        assertThat(result.getSections().get(1).getName(), is("case02"));
    }

    /**
     * [Given] 1シート内に複数ブロック
     * [When]  read() を呼び出す
     * [Then]  各ブロックが順番に格納される
     */
    @Test
    public void multipleBlocksInOneSheet() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_TABLE=T1", ""},
                {"COL1", ""},
                {"v1", ""},
                {"EXPECTED_TABLE=T2", ""},
                {"COL2", ""},
                {"v2", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        List<TestDataBlock> blocks = result.getSections().get(0).getBlocks();
        assertThat(blocks.size(), is(2));
        assertThat(blocks.get(0).getDataType(), is(DataType.SETUP_TABLE_DATA));
        assertThat(blocks.get(1).getDataType(), is(DataType.EXPECTED_TABLE_DATA));
    }

    // -------------------------------------------------------------------------
    // ファイルデータブロック（SS-08〜SS-13, SS-15, SS-17, DR-01, DR-07）
    // -------------------------------------------------------------------------

    /**
     * [Given] SETUP_FIXED ブロック（固定長・ディレクティブあり）
     * [When]  read() を呼び出す
     * [Then]  FileDataBlock として格納される
     */
    @Test
    public void readSetupFixed() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_FIXED=input/data.dat", "", "", ""},
                {"text-encoding", "MS932", "", ""},
                {"DATA", "USER_ID", "AMOUNT", ""},
                {"", "X", "Z", ""},
                {"", "10", "10", ""},
                {"", "001", "5000", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        FileDataBlock block = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDataType(), is(DataType.SETUP_FIXED));
        assertThat(block.getFileType(), is(FileDataBlock.FileType.FIXED));
        assertThat(block.getIdentifier(), is("input/data.dat"));
        assertThat(block.getDirectives().get("text-encoding"), is("MS932"));
        assertThat(block.getRecords().size(), is(1));

        RecordLayout record = block.getRecords().get(0);
        assertThat(record.getRecordType(), is("DATA"));
        assertThat(record.getFields().size(), is(2));
        assertThat(record.getFields().get(0).getName(), is("USER_ID"));
        assertThat(record.getFields().get(0).getType(), is("X"));
        assertThat(record.getFields().get(0).getLength(), is("10"));
        assertThat(record.getFields().get(1).getName(), is("AMOUNT"));
        assertThat(record.getRows().size(), is(1));
        assertThat(record.getRows().get(0), is(Arrays.asList("001", "5000")));
    }

    /**
     * [Given] SETUP_VARIABLE ブロック（可変長・フィールド長行なし）（SS-10）
     * [When]  read() を呼び出す
     * [Then]  FileType.VARIABLE で length が null
     */
    @Test
    public void readSetupVariable() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_VARIABLE=input/var.dat", "", ""},
                {"field-separator", ",", ""},
                {"DATA", "FIELD1", "FIELD2"},
                {"", "X", "X"},
                {"", "aaa", "bbb"}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        FileDataBlock block = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getFileType(), is(FileDataBlock.FileType.VARIABLE));
        assertThat(block.getRecords().get(0).getFields().get(0).getLength(), is(nullValue()));
    }

    /**
     * [Given] フィールド長が "-" のブロック（SS-17）
     * [When]  read() を呼び出す
     * [Then]  "-" がリテラルとして保持される
     */
    @Test
    public void fieldLengthDashPreserved() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_FIXED=data.dat", "", ""},
                {"DATA", "FIELD1", ""},
                {"", "X", ""},
                {"", "-", ""},
                {"", "v1", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        FileDataBlock block = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getRecords().get(0).getFields().get(0).getLength(), is("-"));
    }

    /**
     * [Given] 空ファイル表現（ディレクティブのみ、レコード定義なし）（SS-15）
     * [When]  read() を呼び出す
     * [Then]  records が空リスト
     */
    @Test
    public void emptyFileRepresentation() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_FIXED=empty.dat", "", ""},
                {"text-encoding", "UTF-8", ""},
                {"EXPECTED_TABLE=T1", ""},
                {"COL1", ""},
                {"v1", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        FileDataBlock fileBlock = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(fileBlock.getRecords().size(), is(0));
    }

    /**
     * [Given] 空ファイル表現がシート末尾（EOF）にある（SS-15、Q-1バグ修正確認）
     * [When]  read() を呼び出す
     * [Then]  ディレクティブが directives に格納され records は空リスト
     */
    @Test
    public void emptyFileRepresentationAtEof() throws Exception {
        // Given: SETUP_FIXED ブロックが最後のブロックで、EOF 直前にディレクティブ行がある
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_FIXED=empty.dat", "", ""},
                {"text-encoding", "UTF-8", ""}
                // シート末尾（EOF）
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        FileDataBlock fileBlock = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(fileBlock.getDirectives().size(), is(1));
        assertThat(fileBlock.getDirectives().get("text-encoding"), is("UTF-8"));
        assertThat(fileBlock.getRecords().size(), is(0));
    }

    /**
     * [Given] 複数ディレクティブの最後がシート末尾（EOF）
     * [When]  read() を呼び出す
     * [Then]  全ディレクティブが directives に格納される
     */
    @Test
    public void multipleDirectivesAtEof() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_FIXED=data.dat", "", ""},
                {"text-encoding", "UTF-8", ""},
                {"record-separator", "\\n", ""}
                // EOF（次行なし）
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        FileDataBlock fileBlock = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(fileBlock.getDirectives().size(), is(2));
        assertThat(fileBlock.getDirectives().get("text-encoding"), is("UTF-8"));
        assertThat(fileBlock.getDirectives().get("record-separator"), is("\\n"));
        assertThat(fileBlock.getRecords().size(), is(0));
    }

    /**
     * [Given] 複数レコードレイアウトを持つファイルデータブロック（SS-11）
     * [When]  read() を呼び出す
     * [Then]  複数の RecordLayout が格納される
     */
    @Test
    public void multipleRecordLayouts() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_FIXED=data.dat", "", ""},
                {"REC1", "F1", ""},
                {"", "X", ""},
                {"", "5", ""},
                {"", "aaa", ""},
                {"REC2", "G1", "G2"},
                {"", "N", "X"},
                {"", "3", "10"},
                {"", "123", "bbb"}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        FileDataBlock block = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getRecords().size(), is(2));
        assertThat(block.getRecords().get(0).getRecordType(), is("REC1"));
        assertThat(block.getRecords().get(1).getRecordType(), is("REC2"));
    }

    /**
     * [Given] フィールド名行の構造（先頭列=レコード種別名、2列目以降=フィールド名）（SS-12）
     * [When]  read() を呼び出す
     * [Then]  レコード種別名とフィールド名が正しく分離される
     */
    @Test
    public void fieldNameRowStructure() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_FIXED=data.dat", "", "", ""},
                {"HEADER", "USER_ID", "NAME", ""},
                {"", "X", "X", ""},
                {"", "10", "20", ""},
                {"", "001", "taro", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        RecordLayout record = ((FileDataBlock) result.getSections().get(0).getBlocks().get(0)).getRecords().get(0);
        assertThat(record.getRecordType(), is("HEADER"));
        assertThat(record.getFields().get(0).getName(), is("USER_ID"));
        assertThat(record.getFields().get(1).getName(), is("NAME"));
    }

    // -------------------------------------------------------------------------
    // メッセージングデータブロック（MS-01, MS-02）
    // -------------------------------------------------------------------------

    /**
     * [Given] MESSAGE ブロック（FW ヘッダあり）（MS-01, MS-02）
     * [When]  read() を呼び出す
     * [Then]  MessageDataBlock として格納され FW ヘッダが分離される
     */
    @Test
    public void readMessage() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"MESSAGE=sendSyncTestData/REQ001/message", ""},
                {"requestId", "REQ001"},
                {"userId", "usr001"},
                {"", "FIELD1", "FIELD2"},
                {"", "X", "X"},
                {"", "req1", "data1"}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        MessageDataBlock block = (MessageDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDataType(), is(DataType.MESSAGE));
        assertThat(block.getIdentifier(), is("sendSyncTestData/REQ001/message"));
        assertThat(block.getFwHeaderFields().get("requestId"), is("REQ001"));
        assertThat(block.getFwHeaderFields().get("userId"), is("usr001"));
        assertThat(block.getRecords().size(), is(1));
        assertThat(block.getRecords().get(0).getFields().get(0).getName(), is("FIELD1"));
        assertThat(block.getRecords().get(0).getRows().get(0), is(Arrays.asList("req1", "data1")));
    }

    // -------------------------------------------------------------------------
    // エラーケース
    // -------------------------------------------------------------------------

    /**
     * [Given] 存在しないファイルパス
     * [When]  read() を呼び出す
     * [Then]  ConverterException がスローされる
     */
    @Test(expected = ConverterException.class)
    public void fileNotFound() throws Exception {
        sut.read(Path.of("/nonexistent/path/FooTest.xls"));
    }

    // -------------------------------------------------------------------------
    // 追加テスト（カバレッジ拡充）
    // -------------------------------------------------------------------------

    /**
     * [Given] データ行のうち col 1 のセルが生成されていない（null cell）
     * [When]  read() を呼び出す
     * [Then]  readCells が null セルを "" として扱い、HC-04 で補完される
     */
    @Test
    public void nullCellInRowReadsAsEmptyString() throws Exception {
        // Given: 手動で Row を作成し col 0 のみセルを作成する（col 1 は null）
        File xls = temporaryFolder.newFile("FooTest.xls");
        Workbook wb = new HSSFWorkbook();
        Sheet sheet = wb.createSheet("case01");
        // 識別行
        row(sheet, 0, "SETUP_TABLE=T1", "", "");
        // ヘッダ行
        row(sheet, 1, "COL1", "COL2", "");
        // データ行: col 0 のみ作成、col 1 は作成しない
        Row dataRow = sheet.createRow(2);
        dataRow.createCell(0).setCellValue("v1");
        // col 1 のセルは生成しない → null cell
        // lastCellNum は getLastCellNum() が返す値に依存するため、
        // col 2 に空セルを置いて lastCellNum >= 2 にする
        dataRow.createCell(2).setCellValue("");
        FileOutputStream out = new FileOutputStream(xls);
        try {
            wb.write(out);
        } finally {
            out.close();
        }

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then: col 1 が "" として読まれ HC-04 で補完される
        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getRows().get(0), is(Arrays.asList("v1", "")));
    }

    /**
     * [Given] データ行のセルが数値型（CELL_TYPE_NUMERIC）
     * [When]  read() を呼び出す
     * [Then]  cell.toString() の結果が文字列として読まれ、数値警告ログパスが通る
     */
    @Test
    public void numericCellUsesToString() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        Workbook wb = new HSSFWorkbook();
        Sheet sheet = wb.createSheet("case01");
        row(sheet, 0, "SETUP_TABLE=T1", "");
        row(sheet, 1, "COL1", "");
        // データ行に数値セルを設定
        Row dataRow = sheet.createRow(2);
        dataRow.createCell(0).setCellValue(1.0);
        FileOutputStream out = new FileOutputStream(xls);
        try {
            wb.write(out);
        } finally {
            out.close();
        }

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then: cell.toString() の結果（例: "1.0"）が読まれる
        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getRows().size(), is(1));
        // toString() の結果は空でない
        assertThat(block.getRows().get(0).get(0).isEmpty(), is(false));
    }

    /**
     * [Given] 識別行に "=" が含まれない不正フォーマット（例: "SETUP_TABLE_BAD_FORMAT"）
     * [When]  read() を呼び出す
     * [Then]  ConverterException がスローされる
     */
    @Test(expected = ConverterException.class)
    public void identifierRowMissingEqualsThrows() throws Exception {
        // Given: "SETUP_TABLE" プレフィックスで始まるが "=" が無い
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_TABLE_BAD_FORMAT", ""}
        });

        // When
        sut.read(xls.toPath());
    }

    /**
     * [Given] 認識できない行（DataType プレフィックスに合致しない行）がブロック間に存在する
     * [When]  read() を呼び出す
     * [Then]  不明行はスキップされ、前後のブロックは正常に解析される
     */
    @Test
    public void unknownRowBetweenBlocksIsSkipped() throws Exception {
        // Given: SETUP_TABLE ブロック、不明行 "NOTE: ..." 、EXPECTED_TABLE ブロック
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_TABLE=T1", ""},
                {"COL1", ""},
                {"v1", ""},
                {"NOTE: some text", ""},
                {"EXPECTED_TABLE=T2", ""},
                {"COL2", ""},
                {"v2", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then: 2つのブロックが解析され、"NOTE:" 行はスキップされる
        List<TestDataBlock> blocks = result.getSections().get(0).getBlocks();
        assertThat(blocks.size(), is(2));
        assertThat(blocks.get(0).getDataType(), is(DataType.SETUP_TABLE_DATA));
        assertThat(blocks.get(1).getDataType(), is(DataType.EXPECTED_TABLE_DATA));
    }

    /**
     * [Given] ファイルデータブロックでディレクティブ行なし（識別行直後にフィールド名行）
     * [When]  read() を呼び出す
     * [Then]  directives が空で 1 レコードが解析される
     */
    @Test
    public void fileBlockWithNoDirectives() throws Exception {
        // Given: SETUP_FIXED 直後にフィールド名行（ディレクティブなし）
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_FIXED=data.dat", "", "", ""},
                {"DATA", "FIELD1", "", ""},
                {"", "X", "", ""},
                {"", "5", "", ""},
                {"", "v1", "", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then: directives は空、1レコードが解析される
        FileDataBlock block = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDirectives().isEmpty(), is(true));
        assertThat(block.getRecords().size(), is(1));
        assertThat(block.getRecords().get(0).getRows().get(0), is(Arrays.asList("v1")));
    }

    /**
     * [Given] ファイルデータブロックのデータ行がフィールド数より短い（HC-04 for file blocks）
     * [When]  read() を呼び出す
     * [Then]  不足分は空文字で補完される
     */
    @Test
    public void fileBlockDataRowShorterThanFieldCount() throws Exception {
        // Given: 2フィールドに対してデータ行は1値のみ
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_FIXED=data.dat", "", "", ""},
                {"DATA", "FIELD1", "FIELD2", ""},
                {"", "X", "X", ""},
                {"", "5", "5", ""},
                {"", "only_val", "", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then: データ行が ["only_val", ""] に補完される
        FileDataBlock block = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getRecords().get(0).getRows().get(0), is(Arrays.asList("only_val", "")));
    }

    /**
     * [Given] Row オブジェクトが null（getRow() が null を返す行番号）の XLS ファイル
     * [When]  read() を呼び出す
     * [Then]  null 行はスキップされ、他の行は正常に読み込まれる
     */
    @Test
    public void nullRowInSheetIsSkipped() throws Exception {
        // Given: row 0 を作成せず row 1 のみ作成する（row 0 が null になる）
        File xls = temporaryFolder.newFile("FooTest.xls");
        Workbook wb = new HSSFWorkbook();
        Sheet sheet = wb.createSheet("case01");
        // row 0 を作成しない → sheet.getRow(0) は null
        row(sheet, 1, "SETUP_TABLE=T1", "");
        row(sheet, 2, "COL1", "");
        row(sheet, 3, "v1", "");
        FileOutputStream out = new FileOutputStream(xls);
        try {
            wb.write(out);
        } finally {
            out.close();
        }

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then: null 行がスキップされ 1 ブロックが解析される
        assertThat(result.getSections().get(0).getBlocks().size(), is(1));
        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getRows().get(0), is(Arrays.asList("v1")));
    }

    /**
     * [Given] DataType.DEFAULT プレフィックスに合致しない DataType が識別行に来た場合（未サポート DataType）
     * [When]  read() を呼び出す
     * [Then]  その行はスキップされ後続ブロックが正常に解析される
     *
     * <p>DataType.DEFAULT は detectDataType で除外されるが、
     * isColumnRowType/isFileType/isMessageType のいずれにも該当しない DataType が
     * 将来追加された場合でも else ブランチで i++ スキップされることを確認する。
     * 現状では DEFAULT がその候補だが DataType.DEFAULT は getName() 呼び出し時に
     * startsWith 判定を通過しないため、代わりにヘッダ行（非識別行）連続ケースで
     * parseBlocks の全 null ブランチを確認する。</p>
     */
    @Test
    public void multipleUnknownRowsBetweenBlocksAreSkipped() throws Exception {
        // Given: 不明行が複数続いた後に有効なブロックがある
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"UNKNOWN_TYPE=something", ""},
                {"anotherUnknown", ""},
                {"SETUP_TABLE=T1", ""},
                {"COL1", ""},
                {"v1", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then: SETUP_TABLE ブロックのみが解析される
        List<TestDataBlock> blocks = result.getSections().get(0).getBlocks();
        assertThat(blocks.size(), is(1));
        assertThat(blocks.get(0).getDataType(), is(DataType.SETUP_TABLE_DATA));
    }

    /**
     * [Given] ファイルデータブロックで先頭空行がディレクティブ後に来る（フィールド名行への遷移）
     * [When]  read() を呼び出す
     * [Then]  ディレクティブとレコードレイアウトが正しく解析される
     */
    @Test
    public void fileBlockDirectivesFollowedByEmptyFirstCellRow() throws Exception {
        // Given: ディレクティブ行（非空先頭）の直後に先頭空のフィールド名行がある
        // これにより parseFileBlock ディレクティブループの
        // "nextFirstEmpty → break" ブランチを通過させる
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_VARIABLE=data.csv", "", "", ""},
                {"field-separator", ",", "", ""},
                {"DATA", "FIELD1", "FIELD2", ""},
                {"", "X", "X", ""},
                {"", "aaa", "bbb", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        FileDataBlock block = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDirectives().get("field-separator"), is(","));
        assertThat(block.getRecords().size(), is(1));
        assertThat(block.getRecords().get(0).getRecordType(), is("DATA"));
    }

    /**
     * [Given] ファイルデータブロックで新しいブロックがレコードレイアウト解析中に来る
     * [When]  read() を呼び出す
     * [Then]  ファイルブロック内レコードループが新 DataType で break される
     */
    @Test
    public void fileBlockRecordLoopBreaksOnNextDataType() throws Exception {
        // Given: SETUP_FIXED の後に EXPECTED_TABLE が来る
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_FIXED=data.dat", "", ""},
                {"REC1", "F1", ""},
                {"", "X", ""},
                {"", "5", ""},
                {"", "v1", ""},
                {"EXPECTED_TABLE=T2", ""},
                {"COL2", ""},
                {"v2", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        List<TestDataBlock> blocks = result.getSections().get(0).getBlocks();
        assertThat(blocks.size(), is(2));
        assertThat(blocks.get(0), instanceOf(FileDataBlock.class));
        assertThat(blocks.get(1), instanceOf(TableDataBlock.class));
    }

    /**
     * [Given] メッセージングブロックで FW ヘッダ行の後に新しい DataType ブロックが来る
     * [When]  read() を呼び出す
     * [Then]  メッセージブロックが空レコードで終了し次のブロックが解析される
     */
    @Test
    public void messageBlockFwHeaderBreaksOnNextDataType() throws Exception {
        // Given: MESSAGE の FW ヘッダ行解析中に EXPECTED_TABLE が来る
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"MESSAGE=req/id/msg", ""},
                {"requestId", "REQ001"},
                {"EXPECTED_TABLE=T1", ""},
                {"COL1", ""},
                {"v1", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then: MESSAGEブロックと EXPECTED_TABLE ブロックの2つが解析される
        List<TestDataBlock> blocks = result.getSections().get(0).getBlocks();
        assertThat(blocks.size(), is(2));
        assertThat(blocks.get(0), instanceOf(MessageDataBlock.class));
        assertThat(blocks.get(1), instanceOf(TableDataBlock.class));
    }

    /**
     * [Given] メッセージングブロックで先頭非空行がレコードレイアウト解析中に来る
     * [When]  read() を呼び出す
     * [Then]  レコードレイアウトループが break される
     */
    @Test
    public void messageBlockRecordLoopBreaksOnNonEmptyFirstCell() throws Exception {
        // Given: MESSAGE のレコードレイアウト解析中に先頭非空行（識別子でない）が来る
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"MESSAGE=req/id/msg", ""},
                {"requestId", "REQ001"},
                {"", "FIELD1", "FIELD2"},
                {"", "X", "X"},
                {"", "req1", "data1"},
                {"FW_HEADER_EXTRA", "VALUE"}  // 先頭非空の非識別行
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then: MESSAGEブロックが解析される（後続の非識別先頭非空行でループ break）
        List<TestDataBlock> blocks = result.getSections().get(0).getBlocks();
        assertThat(blocks.size(), is(1));
        assertThat(blocks.get(0), instanceOf(MessageDataBlock.class));
        MessageDataBlock msg = (MessageDataBlock) blocks.get(0);
        assertThat(msg.getRecords().size(), is(1));
    }

    /**
     * [Given] メッセージングデータブロックのデータ行がフィールド数より短い（HC-04 for message blocks）
     * [When]  read() を呼び出す
     * [Then]  不足分は空文字で補完される
     */
    @Test
    public void messageBlockDataRowShorterThanFieldCount() throws Exception {
        // Given: 2フィールドに対してデータ行は1値のみ
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"MESSAGE=req/id/msg", ""},
                {"requestId", "REQ001"},
                {"", "FIELD1", "FIELD2"},
                {"", "X", "X"},
                {"", "only_val", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then: データ行が ["only_val", ""] に補完される
        MessageDataBlock block = (MessageDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getRecords().get(0).getRows().get(0), is(Arrays.asList("only_val", "")));
    }

    /**
     * [Given] メッセージングデータブロックで複数データ行の後に新しいブロックが来る
     * [When]  read() を呼び出す
     * [Then]  データ行の先頭非空チェックによりループが break される
     */
    @Test
    public void messageBlockDataRowBreaksOnNextRecord() throws Exception {
        // Given: MESSAGE のデータ行解析中に先頭非空行（FW ヘッダまたは次のレコード種別）が来る
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"MESSAGE=req/id/msg", ""},
                {"requestId", "REQ001"},
                {"", "FIELD1"},
                {"", "X"},
                {"", "val1"},
                {"", "val2"},  // 2行目データ
                {"EXPECTED_TABLE=T1", ""},
                {"COL1", ""},
                {"v1", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then: MESSAGE と EXPECTED_TABLE の2ブロック
        List<TestDataBlock> blocks = result.getSections().get(0).getBlocks();
        assertThat(blocks.size(), is(2));
        assertThat(blocks.get(0), instanceOf(MessageDataBlock.class));
        assertThat(blocks.get(1), instanceOf(TableDataBlock.class));
    }

    /**
     * [Given] trimTrailingEmpty で末尾に空文字が複数ある行
     * [When]  read() を呼び出す
     * [Then]  末尾の空文字が除去される
     */
    @Test
    public void trimTrailingEmptyRemovesMultipleTrailingEmpty() throws Exception {
        // Given: ヘッダ行の末尾に空カラムが 3 つある
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"SETUP_TABLE=T1", "", "", "", ""},
                {"COL1", "", "", "", ""},
                {"v1", "", "", "", ""}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then: 空のヘッダ列が削除され COL1 のみ残る
        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getColumnNames(), is(Arrays.asList("COL1")));
        assertThat(block.getRows().get(0), is(Arrays.asList("v1")));
    }

    /**
     * [Given] ファイルデータブロックのフィールド名行末尾に空セルがある
     * [When]  read() を呼び出す
     * [Then]  trimTrailingEmpty によって末尾の空フィールド名が除去される
     */
    @Test
    public void fileBlockFieldNameTrailingEmptyRemoved() throws Exception {
        // Given: フィールド名行（subList を通じて trimTrailingEmpty が呼ばれる）で末尾に空がある
        File xls = temporaryFolder.newFile("FooTest.xls");
        Workbook wb = new HSSFWorkbook();
        Sheet sheet = wb.createSheet("case01");
        row(sheet, 0, "SETUP_FIXED=data.dat", "", "", "");
        // フィールド名行: col0=レコード種別, col1=FIELD1, col2=FIELD2, col3="" (末尾空)
        row(sheet, 1, "DATA", "FIELD1", "FIELD2", "");
        // データ型行
        row(sheet, 2, "", "X", "X", "");
        // フィールド長行
        row(sheet, 3, "", "5", "5", "");
        // データ行
        row(sheet, 4, "", "val1", "val2", "");
        FileOutputStream out = new FileOutputStream(xls);
        try {
            wb.write(out);
        } finally {
            out.close();
        }

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then: 末尾の空フィールドが trimTrailingEmpty によって除去される
        FileDataBlock block = (FileDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getRecords().size(), is(1));
        assertThat(block.getRecords().get(0).getFields().size(), is(2));
        assertThat(block.getRecords().get(0).getFields().get(0).getName(), is("FIELD1"));
        assertThat(block.getRecords().get(0).getFields().get(1).getName(), is("FIELD2"));
    }

    /**
     * [Given] MESSAGE ブロックに text-encoding ディレクティブと FW ヘッダが混在
     * [When]  read() を呼び出す
     * [Then]  text-encoding は directives に、requestId/userId は fwHeaderFields に格納される（T3: ディレクティブ分離）
     */
    @Test
    public void messageBlockDirectiveSeparatedFromFwHeader() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"MESSAGE=sendSyncTestData/REQ001/message", ""},
                {"text-encoding", "MS932"},
                {"requestId", "REQ001"},
                {"userId", "usr001"},
                {"", "FIELD1"},
                {"", "X"},
                {"", "req1"}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then: text-encoding は directives に格納され fwHeaderFields には含まれない
        MessageDataBlock block = (MessageDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDirectives().get("text-encoding"), is("MS932"));
        assertThat(block.getFwHeaderFields().containsKey("text-encoding"), is(false));
        assertThat(block.getFwHeaderFields().get("requestId"), is("REQ001"));
        assertThat(block.getFwHeaderFields().get("userId"), is("usr001"));
        assertThat(block.getRecords().size(), is(1));
        assertThat(block.getRecords().get(0).getRows().get(0), is(Arrays.asList("req1")));
    }

    /**
     * [Given] EXPECTED_REQUEST_HEADER_MESSAGES ブロックに text-encoding ディレクティブと requestId が混在
     * [When]  read() を呼び出す
     * [Then]  text-encoding は directives に、requestId は fwHeaderFields に格納される（T3: EXPECTED_REQUEST にもディレクティブ分離適用）
     */
    @Test
    public void expectedRequestHeaderDirectiveSeparatedFromFwHeader() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"EXPECTED_REQUEST_HEADER_MESSAGES=req/hdr", ""},
                {"text-encoding", "UTF-8"},
                {"requestId", "REQ001"},
                {"", "FIELD1"},
                {"", "X"},
                {"", "expected1"}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        MessageDataBlock block = (MessageDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDirectives().get("text-encoding"), is("UTF-8"));
        assertThat(block.getFwHeaderFields().containsKey("text-encoding"), is(false));
        assertThat(block.getFwHeaderFields().get("requestId"), is("REQ001"));
    }

    /**
     * [Given] MESSAGE ブロックに既知ディレクティブのみ（FW ヘッダなし）
     * [When]  read() を呼び出す
     * [Then]  fwHeaderFields は空で directives にすべてのディレクティブが格納される
     */
    @Test
    public void messageBlockOnlyDirectives() throws Exception {
        // Given
        File xls = temporaryFolder.newFile("FooTest.xls");
        writeXls(xls, new String[][]{
                {"MESSAGE=req/msg", ""},
                {"text-encoding", "UTF-8"},
                {"record-separator", "\\n"},
                {"", "FIELD1"},
                {"", "X"},
                {"", "val1"}
        });

        // When
        TestDataContainer result = sut.read(xls.toPath());

        // Then
        MessageDataBlock block = (MessageDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getDirectives().size(), is(2));
        assertThat(block.getDirectives().get("text-encoding"), is("UTF-8"));
        assertThat(block.getDirectives().get("record-separator"), is("\\n"));
        assertThat(block.getFwHeaderFields().isEmpty(), is(true));
    }

    // -------------------------------------------------------------------------
    // .xlsx 対応（WorkbookFactory 経由）
    // -------------------------------------------------------------------------

    /**
     * [Given] SETUP_TABLE ブロックを含む XLSX ファイル
     * [When]  read() を呼び出す
     * [Then]  TestDataContainer にテーブルデータブロックが格納される（.xlsx も読める）
     */
    @Test
    public void readXlsx() throws Exception {
        // Given
        File xlsx = temporaryFolder.newFile("FooTest.xlsx");
        writeXlsx(xlsx, new String[][]{
                {"SETUP_TABLE=USER_MASTER", "", ""},
                {"USER_ID", "NAME", ""},
                {"001", "taro", ""}
        });

        // When
        TestDataContainer result = sut.read(xlsx.toPath());

        // Then
        assertThat(result.getName(), is("FooTest"));
        TableDataBlock block = (TableDataBlock) result.getSections().get(0).getBlocks().get(0);
        assertThat(block.getIdentifier(), is("USER_MASTER"));
        assertThat(block.getColumnNames(), is(Arrays.asList("USER_ID", "NAME")));
        assertThat(block.getRows().get(0), is(Arrays.asList("001", "taro")));
    }

    // -------------------------------------------------------------------------
    // ヘルパー
    // -------------------------------------------------------------------------

    /** 単一シート "case01" に指定の行を書き込んだ XLS ファイルを生成する。 */
    private static void writeXls(File xls, String[][] data) throws Exception {
        Workbook wb = new HSSFWorkbook();
        Sheet sheet = wb.createSheet("case01");
        for (int r = 0; r < data.length; r++) {
            row(sheet, r, data[r]);
        }
        FileOutputStream out = new FileOutputStream(xls);
        try {
            wb.write(out);
        } finally {
            out.close();
        }
    }

    /** 単一シート "case01" に指定の行を書き込んだ XLSX ファイルを生成する。 */
    private static void writeXlsx(File xlsx, String[][] data) throws Exception {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("case01");
        for (int r = 0; r < data.length; r++) {
            row(sheet, r, data[r]);
        }
        FileOutputStream out = new FileOutputStream(xlsx);
        try {
            wb.write(out);
        } finally {
            out.close();
        }
    }

    private static void row(Sheet sheet, int rowNum, String... values) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i]);
        }
    }
}
