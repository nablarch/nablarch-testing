package nablarch.test.tool.converter.xls;

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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * {@link XlsFormatWriter} のテスト（7.2節）。
 */
public class XlsFormatWriterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final XlsFormatWriter sut = new XlsFormatWriter();

    // -------------------------------------------------------------------------
    // テーブルデータブロック
    // -------------------------------------------------------------------------

    /**
     * [Given] SETUP_TABLE ブロック
     * [When]  write() を呼び出す
     * [Then]  識別行・ヘッダ行・データ行の順で出力される
     */
    @Test
    public void writeSetupTable() throws Exception {
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "USER_MASTER",
                Arrays.asList("USER_ID", "NAME"),
                Arrays.asList(Arrays.asList("001", "taro"))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        Sheet sheet = openSheet(outputDir, "FooTest", "case01");
        assertThat(cellStr(sheet, 0, 0), is("SETUP_TABLE=USER_MASTER"));
        assertThat(cellStr(sheet, 1, 0), is("USER_ID"));
        assertThat(cellStr(sheet, 1, 1), is("NAME"));
        assertThat(cellStr(sheet, 2, 0), is("001"));
        assertThat(cellStr(sheet, 2, 1), is("taro"));
    }

    /**
     * [Given] groupId を持つ EXPECTED_TABLE ブロック
     * [When]  write() を呼び出す
     * [Then]  識別行に groupId が含まれる（7.2.2節）
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

        Sheet sheet = openSheet(outputDir, "FooTest", "sheet1");
        assertThat(cellStr(sheet, 0, 0), is("EXPECTED_TABLE[case01]=ORDERS"));
    }

    /**
     * [Given] LIST_MAP ブロック
     * [When]  write() を呼び出す
     * [Then]  識別行に LIST_MAP が出力される
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

        Sheet sheet = openSheet(outputDir, "FooTest", "case01");
        assertThat(cellStr(sheet, 0, 0), is("LIST_MAP=myList"));
        assertThat(cellStr(sheet, 1, 0), is("KEY1"));
        assertThat(cellStr(sheet, 2, 0), is("a"));
    }

    // -------------------------------------------------------------------------
    // ファイルデータブロック
    // -------------------------------------------------------------------------

    /**
     * [Given] SETUP_FIXED ブロック
     * [When]  write() を呼び出す
     * [Then]  識別行・ディレクティブ行・フィールド名行・型行・長さ行・データ行が出力される
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
                FileDataBlock.FileType.FIXED, directives, Arrays.asList(record)
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        Sheet sheet = openSheet(outputDir, "FooTest", "case01");
        assertThat(cellStr(sheet, 0, 0), is("SETUP_FIXED=input/data.dat"));
        assertThat(cellStr(sheet, 1, 0), is("text-encoding"));
        assertThat(cellStr(sheet, 1, 1), is("MS932"));
        assertThat(cellStr(sheet, 2, 0), is("DATA"));
        assertThat(cellStr(sheet, 2, 1), is("USER_ID"));
        assertThat(cellStr(sheet, 2, 2), is("AMOUNT"));
        assertThat(cellStr(sheet, 3, 0), is(""));  // data type row: first cell empty
        assertThat(cellStr(sheet, 3, 1), is("X"));
        assertThat(cellStr(sheet, 4, 0), is(""));  // length row: first cell empty
        assertThat(cellStr(sheet, 4, 1), is("10"));
        assertThat(cellStr(sheet, 5, 0), is(""));  // data row: first cell empty
        assertThat(cellStr(sheet, 5, 1), is("001"));
    }

    /**
     * [Given] SETUP_VARIABLE ブロック（可変長: フィールド長行なし）
     * [When]  write() を呼び出す
     * [Then]  フィールド長行が省略される（7.2.4節）
     */
    @Test
    public void writeSetupVariableOmitsLengthRow() throws Exception {
        List<FieldDef> fields = Arrays.asList(new FieldDef("NAME", "X", null));
        RecordLayout record = new RecordLayout("DATA", fields, Arrays.asList(Arrays.asList("taro")));
        FileDataBlock block = new FileDataBlock(
                DataType.SETUP_VARIABLE, "", "out.csv",
                FileDataBlock.FileType.VARIABLE, new LinkedHashMap<>(), Arrays.asList(record)
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        Sheet sheet = openSheet(outputDir, "FooTest", "case01");
        // row 0: identifier, row 1: field names, row 2: types, row 3: data (no length row)
        assertThat(cellStr(sheet, 0, 0), is("SETUP_VARIABLE=out.csv"));
        assertThat(cellStr(sheet, 1, 0), is("DATA"));
        assertThat(cellStr(sheet, 2, 0), is(""));   // type row
        assertThat(cellStr(sheet, 2, 1), is("X"));
        assertThat(cellStr(sheet, 3, 0), is(""));   // data row (no length row)
        assertThat(cellStr(sheet, 3, 1), is("taro"));
    }

    // -------------------------------------------------------------------------
    // メッセージングデータブロック
    // -------------------------------------------------------------------------

    /**
     * [Given] MESSAGE ブロック（FW ヘッダあり）
     * [When]  write() を呼び出す
     * [Then]  識別行・FW ヘッダ行・フィールド名行・型行・データ行が出力される（7.2.5節）
     */
    @Test
    public void writeMessage() throws Exception {
        Map<String, String> fwHeaders = new LinkedHashMap<>();
        fwHeaders.put("requestId", "REQ001");
        fwHeaders.put("userId", "usr001");
        List<FieldDef> bodyFields = Arrays.asList(new FieldDef("FIELD1", "X", null));
        RecordLayout bodyRecord = new RecordLayout("default", bodyFields,
                Arrays.asList(Arrays.asList("req1")));
        MessageDataBlock block = new MessageDataBlock(
                DataType.MESSAGE, "", "sendSyncTestData/REQ001/message",
                new LinkedHashMap<>(), fwHeaders, Arrays.asList(bodyRecord)
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        Sheet sheet = openSheet(outputDir, "FooTest", "case01");
        assertThat(cellStr(sheet, 0, 0), is("MESSAGE=sendSyncTestData/REQ001/message"));
        assertThat(cellStr(sheet, 1, 0), is("requestId"));
        assertThat(cellStr(sheet, 1, 1), is("REQ001"));
        assertThat(cellStr(sheet, 2, 0), is("userId"));
        assertThat(cellStr(sheet, 2, 1), is("usr001"));
        assertThat(cellStr(sheet, 3, 0), is(""));  // field name row (no-column)
        assertThat(cellStr(sheet, 3, 1), is("FIELD1"));
    }

    /**
     * [Given] MESSAGE ブロック（データ行あり）を Write → Read するラウンドトリップ
     * [When]  write() で出力した XLS ファイルを XlsFormatReader.read() で再読み込みする
     * [Then]  元のデータと一致する（データ行が消失しない）
     */
    @Test
    public void messageBlockRoundTrip() throws Exception {
        // Given: データ行が2件ある MESSAGE ブロック
        Map<String, String> fwHeaders = new LinkedHashMap<>();
        fwHeaders.put("requestId", "REQ001");
        List<FieldDef> bodyFields = Arrays.asList(
                new FieldDef("FIELD1", "X", null),
                new FieldDef("FIELD2", "X", null));
        RecordLayout bodyRecord = new RecordLayout("default", bodyFields,
                Arrays.asList(
                        Arrays.asList("val1", "val2"),
                        Arrays.asList("val3", "val4")));
        MessageDataBlock block = new MessageDataBlock(
                DataType.MESSAGE, "", "req/id/msg",
                new LinkedHashMap<>(), fwHeaders, Arrays.asList(bodyRecord));
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        // When: 出力した XLSX を再読み込みする（XlsFormatWriter のデフォルト出力は outputDir/FooTest.xlsx）
        File xlsFile = new File(outputDir, "FooTest.xlsx");
        XlsFormatReader reader = new XlsFormatReader();
        TestDataContainer result = reader.read(xlsFile.toPath());

        // Then: データ行が消失せず、元の値と一致する
        nablarch.test.tool.converter.model.MessageDataBlock readBlock =
                (nablarch.test.tool.converter.model.MessageDataBlock)
                        result.getSections().get(0).getBlocks().get(0);
        assertThat(readBlock.getFwHeaderFields().get("requestId"), is("REQ001"));
        assertThat(readBlock.getRecords().get(0).getFields().get(0).getName(), is("FIELD1"));
        assertThat(readBlock.getRecords().get(0).getRows().size(), is(2));
        assertThat(readBlock.getRecords().get(0).getRows().get(0), is(Arrays.asList("val1", "val2")));
        assertThat(readBlock.getRecords().get(0).getRows().get(1), is(Arrays.asList("val3", "val4")));
    }

    // -------------------------------------------------------------------------
    // セル値の書き出し規則（7.2.1節）
    // -------------------------------------------------------------------------

    /**
     * [Given] null 値を含む行
     * [When]  write() を呼び出す
     * [Then]  セルに文字列 "null" と書き出される
     */
    @Test
    public void nullValueWrittenAsString() throws Exception {
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "TBL",
                Arrays.asList("COL"),
                Arrays.asList(Collections.singletonList(null))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        Sheet sheet = openSheet(outputDir, "FooTest", "case01");
        assertThat(cellStr(sheet, 2, 0), is("null"));
    }

    /**
     * [Given] 空文字値を含む行
     * [When]  write() を呼び出す
     * [Then]  セルが空（空文字列として書き込まれる）
     */
    @Test
    public void emptyStringWrittenAsEmpty() throws Exception {
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "TBL",
                Arrays.asList("COL"),
                Arrays.asList(Collections.singletonList(""))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        Sheet sheet = openSheet(outputDir, "FooTest", "case01");
        assertThat(cellStr(sheet, 2, 0), is(""));
    }

    // -------------------------------------------------------------------------
    // ファイル制御
    // -------------------------------------------------------------------------

    /**
     * [Given] 既存ファイルあり・overwrite=false
     * [When]  write() を呼び出す
     * [Then]  ConverterException がスローされる
     */
    @Test(expected = ConverterException.class)
    public void overwriteFalseThrowsWhenFileExists() throws Exception {
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "T1",
                Arrays.asList("C1"), Arrays.asList(Arrays.asList("v1"))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);
        sut.write(container, outputDir.toPath(), false);
    }

    /**
     * [Given] 既存ファイルあり・overwrite=true
     * [When]  write() を呼び出す
     * [Then]  例外なく上書きされる
     */
    @Test
    public void overwriteTrueOverwrites() throws Exception {
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "T1",
                Arrays.asList("C1"), Arrays.asList(Arrays.asList("v1"))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);
        sut.write(container, outputDir.toPath(), true);
    }

    /**
     * [Given] 複数セクション
     * [When]  write() を呼び出す
     * [Then]  1 つの XLS ファイルに複数シートとして出力される
     */
    @Test
    public void multipleSectionsWrittenToSameXls() throws Exception {
        TestDataBlock b1 = new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "T1",
                Arrays.asList("C1"), Arrays.asList(Arrays.asList("v1")));
        TestDataBlock b2 = new TableDataBlock(DataType.EXPECTED_TABLE_DATA, "", "T2",
                Arrays.asList("C2"), Arrays.asList(Arrays.asList("v2")));
        List<TestDataSection> sections = Arrays.asList(
                new TestDataSection("case01", Arrays.asList(b1)),
                new TestDataSection("case02", Arrays.asList(b2))
        );
        TestDataContainer container = new TestDataContainer("FooTest", sections);

        File outputDir = temporaryFolder.newFolder("out");
        sut.write(container, outputDir.toPath(), false);

        assertTrue(new File(outputDir, "FooTest.xlsx").exists());
        Workbook wb = openWorkbook(outputDir, "FooTest");
        assertThat(wb.getNumberOfSheets(), is(2));
        assertThat(wb.getSheetAt(0).getSheetName(), is("case01"));
        assertThat(wb.getSheetAt(1).getSheetName(), is("case02"));
    }

    /**
     * [Given] xlsFormat=true の XlsFormatWriter
     * [When]  write() を呼び出す
     * [Then]  .xls ファイルが生成される
     */
    @Test
    public void xlsFormatFlagProducesXlsFile() throws Exception {
        XlsFormatWriter xlsWriter = new XlsFormatWriter(true);
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "T1",
                Arrays.asList("C1"), Arrays.asList(Arrays.asList("v1"))
        );
        TestDataContainer container = container("case01", block);

        File outputDir = temporaryFolder.newFolder("out");
        xlsWriter.write(container, outputDir.toPath(), false);

        assertTrue(new File(outputDir, "FooTest.xls").exists());
        assertFalse(new File(outputDir, "FooTest.xlsx").exists());
        Workbook wb = openXlsWorkbook(outputDir, "FooTest");
        assertThat(wb.getNumberOfSheets(), is(1));
    }

    // -------------------------------------------------------------------------
    // 追加テスト（カバレッジ拡充）
    // -------------------------------------------------------------------------

    /**
     * [Given] 出力先パスがディレクトリではなくファイルとして既に存在する
     * [When]  write() を呼び出す（Files.createDirectories がファイルパスで IOException を送出）
     * [Then]  ConverterException がスローされる
     */
    @Test(expected = ConverterException.class)
    public void iOExceptionOnDirectoryCreationThrowsConverterException() throws Exception {
        // Given: "out" という名前のファイルを作成しておく
        File outFile = temporaryFolder.newFile("out");
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "T1",
                Arrays.asList("C1"), Arrays.asList(Arrays.asList("v1"))
        );
        TestDataContainer container = container("case01", block);

        // When: outFile のパス（ファイル）を outputPath として渡す
        // XlsFormatWriter は outputPath.resolve(containerName+".xlsx") を生成するが、
        // その前に Files.createDirectories(outputPath) を試みる。
        // outFile がファイルなので createDirectories は IOException を投げる。
        sut.write(container, outFile.toPath(), false);
    }

    /**
     * [Given] 出力先ファイルが書き込み不可の状態
     * [When]  write() を呼び出す
     * [Then]  ConverterException がスローされる
     */
    @Test(expected = ConverterException.class)
    public void iOExceptionOnFileOutputStreamThrowsConverterException() throws Exception {
        // Given
        TestDataBlock block = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "T1",
                Arrays.asList("C1"), Arrays.asList(Arrays.asList("v1"))
        );
        TestDataContainer container = container("case01", block);
        File outputDir = temporaryFolder.newFolder("out");
        File outFile = new File(outputDir, "FooTest.xlsx");
        outFile.createNewFile();
        outFile.setWritable(false);

        try {
            sut.write(container, outputDir.toPath(), true);
        } finally {
            outFile.setWritable(true);
        }
    }

    // -------------------------------------------------------------------------
    // ヘルパー
    // -------------------------------------------------------------------------

    private TestDataContainer container(String sectionName, TestDataBlock block) {
        return new TestDataContainer("FooTest",
                Arrays.asList(new TestDataSection(sectionName, Arrays.asList(block))));
    }

    private Workbook openWorkbook(File outputDir, String name) throws Exception {
        File xlsxFile = new File(outputDir, name + ".xlsx");
        FileInputStream fis = new FileInputStream(xlsxFile);
        try {
            return WorkbookFactory.create(fis);
        } finally {
            fis.close();
        }
    }

    private Workbook openXlsWorkbook(File outputDir, String name) throws Exception {
        File xlsFile = new File(outputDir, name + ".xls");
        FileInputStream fis = new FileInputStream(xlsFile);
        try {
            return WorkbookFactory.create(fis);
        } finally {
            fis.close();
        }
    }

    private Sheet openSheet(File outputDir, String name, String sheetName) throws Exception {
        return openWorkbook(outputDir, name).getSheet(sheetName);
    }

    private String cellStr(Sheet sheet, int row, int col) {
        Row r = sheet.getRow(row);
        if (r == null) return "";
        Cell c = r.getCell(col);
        if (c == null) return "";
        return c.getStringCellValue();
    }
}
