package nablarch.test.tool.converter;

import nablarch.test.core.reader.DataType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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

    private static void row(Sheet sheet, int rowNum, String... values) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i]);
        }
    }
}
