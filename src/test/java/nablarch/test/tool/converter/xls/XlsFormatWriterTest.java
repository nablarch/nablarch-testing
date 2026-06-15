package nablarch.test.tool.converter.xls;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nablarch.test.core.reader.DataType;
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
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@link XlsFormatWriter} のテストクラス。
 *
 * <p>
 * {@code build}（メモリ上のブック）でセル値・背景色・罫線・列幅を直接アサートし、{@code write} ＋ 実
 * {@link XlsFormatReader} で往復（モデル → Excel → モデル）の同値を検証する。本体パーサのキャッシュ衝突を
 * 避けるため、往復テストはテストごとに一意のブック名・{@link TemporaryFolder} を使う。
 * </p>
 *
 * @author kiyobot
 */
public class XlsFormatWriterTest {

    /** 往復テストの出力先（テストごとに一意）。 */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    // ------------------------------------------------------------------ helpers

    /**
     * null を含められるよう {@link Arrays#asList} で行を組み立てる。
     *
     * @param cells セル
     * @return 行
     */
    private static List<String> row(String... cells) {
        return Arrays.asList(cells);
    }

    /**
     * 1 セクション 1 コンテナを組み立てる。
     *
     * @param book   ブック名
     * @param sheet  シート名
     * @param blocks ブロック
     * @return コンテナ
     */
    private static TestDataContainer container(String book, String sheet, TestDataBlock... blocks) {
        TestDataSection section = new TestDataSection(sheet, Arrays.asList(blocks));
        return new TestDataContainer(book, Collections.singletonList(section));
    }

    /** キー順を保つマップを作る。 */
    private static Map<String, String> map(String... kv) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    /** 既定設定のライタでブックを組み立てる。 */
    private static Workbook build(TestDataContainer container) {
        return new XlsFormatWriter().build(container);
    }

    /** 単一セクションのシートを取り出す。 */
    private static Sheet onlySheet(Workbook wb, String name) {
        return wb.getSheet(name);
    }

    /** セル文字列（行・セルが無ければ null）。 */
    private static String cell(Sheet sheet, int r, int c) {
        Row row = sheet.getRow(r);
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(c);
        return cell == null ? null : cell.getStringCellValue();
    }

    /** 1 行を文字列リストとして取り出す（trailing 含む。行が無ければ null）。 */
    private static List<String> line(Sheet sheet, int r) {
        Row row = sheet.getRow(r);
        if (row == null) {
            return null;
        }
        List<String> cells = new ArrayList<String>();
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            cells.add(cell == null ? null : cell.getStringCellValue());
        }
        return cells;
    }

    // ------------------------------------------------------------------ table

    /**
     * Given: {@code ${...}}・空文字・null セルを含む SETUP_TABLE。
     * When : build。
     * Then : 識別行 → カラム名行 → データ行の版面。null はリテラル {@code null}・空文字は空セル。
     */
    @Test
    public void writesTableBlock() {
        TableDataBlock table = new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "USERS",
                row("USER_NAME", "AGE"),
                Arrays.asList(row("${userName}", ""), row("literal", null)));
        Sheet sheet = onlySheet(build(container("book", "sheet", table)), "sheet");

        assertThat(cell(sheet, 0, 0), is("SETUP_TABLE=USERS"));
        assertThat(line(sheet, 1), is(Arrays.asList("USER_NAME", "AGE")));
        assertThat(line(sheet, 2), is(Arrays.asList("${userName}", "")));
        // null はリテラル null（空文字 "" と区別して書く）
        assertThat(line(sheet, 3), is(Arrays.asList("literal", "null")));
    }

    /**
     * Given: グループ ID 付き EXPECTED_TABLE。
     * When : build。
     * Then : 識別セルが {@code TYPE[group]=id}。
     */
    @Test
    public void writesTableMarkerWithGroupId() {
        TableDataBlock table = new TableDataBlock(DataType.EXPECTED_TABLE_DATA, "[g1]", "USERS",
                row("USER_NAME"), Collections.singletonList(row("${u}")));
        Sheet sheet = onlySheet(build(container("book", "sheet", table)), "sheet");

        assertThat(cell(sheet, 0, 0), is("EXPECTED_TABLE[g1]=USERS"));
    }

    /**
     * Given: EXPECTED_COMPLETE_TABLE。
     * When : build。
     * Then : 識別セルがデータタイプ名どおり。
     */
    @Test
    public void writesExpectedCompleteTableMarker() {
        TableDataBlock table = new TableDataBlock(DataType.EXPECTED_COMPLETED, "", "USERS",
                row("C"), Collections.singletonList(row("v")));
        Sheet sheet = onlySheet(build(container("book", "sheet", table)), "sheet");

        assertThat(cell(sheet, 0, 0), is("EXPECTED_COMPLETE_TABLE=USERS"));
    }

    // ------------------------------------------------------------------ list_map

    /**
     * Given: LIST_MAP。
     * When : build。
     * Then : 識別行 → カラム名行 → データ行。
     */
    @Test
    public void writesListMapBlock() {
        ListMapBlock listMap = new ListMapBlock("", "result",
                row("ID", "NAME"), Arrays.asList(row("${id}", ""), row("2", "bob")));
        Sheet sheet = onlySheet(build(container("book", "sheet", listMap)), "sheet");

        assertThat(cell(sheet, 0, 0), is("LIST_MAP=result"));
        assertThat(line(sheet, 1), is(Arrays.asList("ID", "NAME")));
        assertThat(line(sheet, 2), is(Arrays.asList("${id}", "")));
        assertThat(line(sheet, 3), is(Arrays.asList("2", "bob")));
    }

    /**
     * Given: マーカーカラム {@code [NOTE]} を含むテーブル。
     * When : build。
     * Then : マーカーカラムのセルにマーカー背景色が付く。
     */
    @Test
    public void tintsMarkerColumn() {
        TableDataBlock table = new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "T",
                row("ID", "[NOTE]"), Collections.singletonList(row("1", "memo")));
        Sheet sheet = onlySheet(build(container("book", "sheet", table)), "sheet");

        short marker = ExcelFormatConfig.defaults().getMarkerColumnColorIndex();
        // データ行のマーカーカラム（列 1）はマーカー背景色
        assertThat(sheet.getRow(2).getCell(1).getCellStyle().getFillForegroundColor(), is(marker));
        assertThat(sheet.getRow(2).getCell(1).getCellStyle().getFillPattern(), is(CellStyle.SOLID_FOREGROUND));
        // 通常データセル（列 0）は背景色なし
        assertThat(sheet.getRow(2).getCell(0).getCellStyle().getFillPattern(), is(CellStyle.NO_FILL));
    }

    // ------------------------------------------------------------------ fixed file

    /**
     * Given: ディレクティブ＋固定長レコード（型・長さ・{@code -} 含む）の SETUP_FIXED。
     * When : build。
     * Then : 識別行 → ディレクティブ行 → 名前行 → 型行 → 長さ行 → データ行。データ行の列 0 は空。
     */
    @Test
    public void writesFixedFileBlock() {
        RecordLayout record = new RecordLayout("data",
                Arrays.asList(new FieldDef("f1", "半角英字", "-"), new FieldDef("f2", "半角英字", "5")),
                Collections.singletonList(row("abcd", "xy")));
        FileDataBlock file = new FileDataBlock(DataType.SETUP_FIXED, "", "test.dat",
                FileDataBlock.FileType.FIXED, map("text-encoding", "UTF-8"),
                Collections.singletonList(record));
        Sheet sheet = onlySheet(build(container("book", "sheet", file)), "sheet");

        assertThat(cell(sheet, 0, 0), is("SETUP_FIXED=test.dat"));
        assertThat(cell(sheet, 1, 0), is("text-encoding"));
        assertThat(cell(sheet, 1, 1), is("UTF-8"));
        assertThat(line(sheet, 2), is(Arrays.asList("data", "f1", "f2")));
        assertThat(line(sheet, 3), is(Arrays.asList("", "半角英字", "半角英字")));
        assertThat(line(sheet, 4), is(Arrays.asList("", "-", "5")));
        assertThat(line(sheet, 5), is(Arrays.asList("", "abcd", "xy")));
    }

    /**
     * Given: 可変長ファイル（長さなし）。
     * When : build。
     * Then : 長さ行を持たない（名前行 → 型行 → データ行）。
     */
    @Test
    public void writesVariableFileWithoutLengthRow() {
        RecordLayout record = new RecordLayout("data",
                Collections.singletonList(new FieldDef("f1", "半角英字", null)),
                Collections.singletonList(row("${v}")));
        FileDataBlock file = new FileDataBlock(DataType.SETUP_VARIABLE, "", "in.csv",
                FileDataBlock.FileType.VARIABLE, map(), Collections.singletonList(record));
        Sheet sheet = onlySheet(build(container("book", "sheet", file)), "sheet");

        assertThat(cell(sheet, 0, 0), is("SETUP_VARIABLE=in.csv"));
        assertThat(line(sheet, 1), is(Arrays.asList("data", "f1")));
        assertThat(line(sheet, 2), is(Arrays.asList("", "半角英字")));
        // 長さ行は無く、3 行目がデータ行
        assertThat(line(sheet, 3), is(Arrays.asList("", "${v}")));
    }

    /**
     * Given: 2 レコードレイアウトを持つ固定長ファイル。
     * When : build。
     * Then : 各レコードの名前行（レコード種別が列 0）が独立に書かれる。
     */
    @Test
    public void writesMultipleRecordLayouts() {
        RecordLayout header = new RecordLayout("header",
                Collections.singletonList(new FieldDef("h1", "半角英字", "5")),
                Collections.singletonList(row("AAAAA")));
        RecordLayout data = new RecordLayout("data",
                Collections.singletonList(new FieldDef("d1", "半角英字", "2")),
                Collections.singletonList(row("xy")));
        FileDataBlock file = new FileDataBlock(DataType.SETUP_FIXED, "", "multi.dat",
                FileDataBlock.FileType.FIXED, map(), Arrays.asList(header, data));
        Sheet sheet = onlySheet(build(container("book", "sheet", file)), "sheet");

        // header: 名前(1)/型(2)/長さ(3)/データ(4)。data: 名前(5)/型(6)/長さ(7)/データ(8)
        assertThat(line(sheet, 1), is(Arrays.asList("header", "h1")));
        assertThat(line(sheet, 5), is(Arrays.asList("data", "d1")));
    }

    /**
     * Given: 2 レコード目のレコード種別が null の固定長ファイル。
     * When : build。
     * Then : IllegalStateException（列 0 が空の名前行は本体パーサが直前レコードのデータ行と誤読し、
     *        読み戻せない版面になるため、黙って書かず早期に失敗する）。
     */
    @Test(expected = IllegalStateException.class)
    public void rejectsNullRecordTypeOnSecondRecord() {
        RecordLayout first = new RecordLayout("header",
                Collections.singletonList(new FieldDef("h1", "半角英字", "5")),
                Collections.singletonList(row("AAAAA")));
        RecordLayout second = new RecordLayout(null,   // 2 レコード目はレコード種別必須
                Collections.singletonList(new FieldDef("d1", "半角英字", "2")),
                Collections.singletonList(row("xy")));
        FileDataBlock file = new FileDataBlock(DataType.SETUP_FIXED, "", "bad.dat",
                FileDataBlock.FileType.FIXED, map(), Arrays.asList(first, second));

        build(container("book", "sheet", file));
    }

    /**
     * Given: 2 レコード目のレコード種別が空文字の固定長ファイル。
     * When : build。
     * Then : IllegalStateException（空文字も列 0 が空になるため null と同様に弾く）。
     */
    @Test(expected = IllegalStateException.class)
    public void rejectsEmptyRecordTypeOnSecondRecord() {
        RecordLayout first = new RecordLayout("header",
                Collections.singletonList(new FieldDef("h1", "半角英字", "5")),
                Collections.singletonList(row("AAAAA")));
        RecordLayout second = new RecordLayout("",   // 空文字も列 0 空＝NG
                Collections.singletonList(new FieldDef("d1", "半角英字", "2")),
                Collections.singletonList(row("xy")));
        FileDataBlock file = new FileDataBlock(DataType.SETUP_FIXED, "", "bad.dat",
                FileDataBlock.FileType.FIXED, map(), Arrays.asList(first, second));

        build(container("book", "sheet", file));
    }

    /**
     * Given: レコード種別が null の単一レコード固定長ファイル。
     * When : build。
     * Then : 例外にならない（1 レコード目の列 0 空は本体パーサが位置で名前行を特定でき、誤読しない）。
     */
    @Test
    public void allowsNullRecordTypeOnSingleRecord() {
        RecordLayout only = new RecordLayout(null,
                Collections.singletonList(new FieldDef("f1", "半角英字", "2")),
                Collections.singletonList(row("xy")));
        FileDataBlock file = new FileDataBlock(DataType.SETUP_FIXED, "", "ok.dat",
                FileDataBlock.FileType.FIXED, map(), Collections.singletonList(only));
        Sheet sheet = onlySheet(build(container("book", "sheet", file)), "sheet");

        // 名前行の列 0 は空（レコード種別省略）
        assertThat(line(sheet, 1), is(Arrays.asList("", "f1")));
    }

    // ------------------------------------------------------------------ message

    /**
     * Given: FW 制御ヘッダ＋本文を持つ MESSAGE。
     * When : build。
     * Then : 識別行 → FW ヘッダ行 → 名前行 → 型行 → 長さ行 → データ行。データ行の列 0 は空。
     */
    @Test
    public void writesMessageBlock() {
        RecordLayout record = new RecordLayout("data",
                Arrays.asList(new FieldDef("body1", "半角英字", "10"), new FieldDef("body2", "半角英字", "5")),
                Collections.singletonList(row("${b}", "xyz")));
        MessageDataBlock message = new MessageDataBlock(DataType.MESSAGE, "", "msg1",
                map(), map("requestId", "${rid}"), Collections.singletonList(record));
        Sheet sheet = onlySheet(build(container("book", "sheet", message)), "sheet");

        assertThat(cell(sheet, 0, 0), is("MESSAGE=msg1"));
        // FW ヘッダ行はブロック幅（3）へ矩形整形され、末尾に空セルが補填される（読み戻し時に trim される）
        assertThat(line(sheet, 1), is(Arrays.asList("requestId", "${rid}", "")));
        assertThat(line(sheet, 2), is(Arrays.asList("data", "body1", "body2")));
        assertThat(line(sheet, 3), is(Arrays.asList("", "半角英字", "半角英字")));
        assertThat(line(sheet, 4), is(Arrays.asList("", "10", "5")));
        // データ行の列 0 は空（本体は列 0 空をデータ行と判定する）
        assertThat(line(sheet, 5), is(Arrays.asList("", "${b}", "xyz")));
    }

    /**
     * Given: 送信系（EXPECTED_REQUEST_HEADER_MESSAGES・no 列）。
     * When : build。
     * Then : FW ヘッダ無し・データ行の列 0 に no（連番）。
     */
    @Test
    public void writesSendSyncMessageWithSequenceNo() {
        RecordLayout record = new RecordLayout("no",
                Arrays.asList(new FieldDef("requestId", "半角", "20"), new FieldDef("resendFlag", "半角", "1")),
                Arrays.asList(row("RM21AA0104_01", "0"), row("RM21AA0104_02", "1")));
        MessageDataBlock message = new MessageDataBlock(DataType.EXPECTED_REQUEST_HEADER_MESSAGES,
                "[case1]", "RM21AA0104_01", map(), map(), Collections.singletonList(record));
        Sheet sheet = onlySheet(build(container("book", "sheet", message)), "sheet");

        assertThat(cell(sheet, 0, 0), is("EXPECTED_REQUEST_HEADER_MESSAGES[case1]=RM21AA0104_01"));
        assertThat(line(sheet, 1), is(Arrays.asList("no", "requestId", "resendFlag")));
        assertThat(line(sheet, 2), is(Arrays.asList("", "半角", "半角")));
        assertThat(line(sheet, 3), is(Arrays.asList("", "20", "1")));
        // データ行の列 0 は no（連番）
        assertThat(line(sheet, 4), is(Arrays.asList("1", "RM21AA0104_01", "0")));
        assertThat(line(sheet, 5), is(Arrays.asList("2", "RM21AA0104_02", "1")));
    }

    // ------------------------------------------------------------------ styling

    /**
     * Given: 既定設定。
     * When : build。
     * Then : 識別行・ヘッダ行に背景色、データ行は背景色なし。
     */
    @Test
    public void appliesHeaderBackgroundColor() {
        TableDataBlock table = new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "T",
                row("C"), Collections.singletonList(row("v")));
        Sheet sheet = onlySheet(build(container("book", "sheet", table)), "sheet");

        short header = ExcelFormatConfig.defaults().getHeaderColorIndex();
        assertThat(sheet.getRow(0).getCell(0).getCellStyle().getFillForegroundColor(), is(header));
        assertThat(sheet.getRow(1).getCell(0).getCellStyle().getFillForegroundColor(), is(header));
        // データ行は背景色なし
        assertThat(sheet.getRow(2).getCell(0).getCellStyle().getFillPattern(), is(CellStyle.NO_FILL));
    }

    /**
     * Given: 既定設定（外枠罫線あり）。
     * When : build。
     * Then : ブロックの四隅セルに外枠の罫線が付く。
     */
    @Test
    public void drawsBlockOuterBorder() {
        TableDataBlock table = new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "T",
                row("A", "B"), Collections.singletonList(row("1", "2")));
        Sheet sheet = onlySheet(build(container("book", "sheet", table)), "sheet");

        // 左上セル：上辺・左辺に罫線
        CellStyle topLeft = sheet.getRow(0).getCell(0).getCellStyle();
        assertThat(topLeft.getBorderTop(), is(CellStyle.BORDER_THIN));
        assertThat(topLeft.getBorderLeft(), is(CellStyle.BORDER_THIN));
        // 右下セル：下辺・右辺に罫線
        CellStyle bottomRight = sheet.getRow(2).getCell(1).getCellStyle();
        assertThat(bottomRight.getBorderBottom(), is(CellStyle.BORDER_THIN));
        assertThat(bottomRight.getBorderRight(), is(CellStyle.BORDER_THIN));
        // 内側（上辺）には外枠罫線が無い
        assertThat(sheet.getRow(1).getCell(0).getCellStyle().getBorderTop(), is(CellStyle.BORDER_NONE));
    }

    /**
     * Given: 2 ブロックを持つセクション（既定＝ブロック間 1 空行）。
     * When : build。
     * Then : 2 ブロック目の識別行が 1 行空けて始まる。
     */
    @Test
    public void insertsBlankRowBetweenBlocks() {
        TableDataBlock t1 = new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "T1",
                row("C"), Collections.singletonList(row("v")));
        TableDataBlock t2 = new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "T2",
                row("C"), Collections.singletonList(row("v")));
        Sheet sheet = onlySheet(build(container("book", "sheet", t1, t2)), "sheet");

        // t1: 行 0,1,2。1 空行（行 3）。t2 の識別行は行 4。
        assertThat(cell(sheet, 0, 0), is("SETUP_TABLE=T1"));
        assertThat(sheet.getRow(3), is(nullValue()));
        assertThat(cell(sheet, 4, 0), is("SETUP_TABLE=T2"));
    }

    /**
     * Given: 自動列幅オン。
     * When : build。
     * Then : 列幅が最長値（＋余白）に応じて設定される。
     */
    @Test
    public void appliesAutoColumnWidth() {
        TableDataBlock table = new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "T",
                row("C"), Collections.singletonList(row("abcdefghij")));  // 10 文字
        Sheet sheet = onlySheet(build(container("book", "sheet", table)), "sheet");

        // 識別行 "SETUP_TABLE=T"（13 文字）が列 0 最長。(13+2)*256
        assertThat(sheet.getColumnWidth(0), is((13 + 2) * 256));
    }

    // ------------------------------------------------------------------ config override

    /**
     * Given: ブロック間空行 0・罫線なし・自動列幅なし・背景色変更の上書き設定。
     * When : build。
     * Then : 各上書きが反映される。
     */
    @Test
    public void honorsConfigOverrides() {
        short customHeader = org.apache.poi.ss.usermodel.IndexedColors.LIGHT_GREEN.getIndex();
        ExcelFormatConfig config = ExcelFormatConfig.defaults()
                .withBlankRowsBetweenBlocks(0)
                .withBlockBorder(false)
                .withAutoColumnWidth(false)
                .withHeaderColor(customHeader);
        TableDataBlock t1 = new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "T1",
                row("C"), Collections.singletonList(row("v")));
        TableDataBlock t2 = new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "T2",
                row("C"), Collections.singletonList(row("v")));
        Workbook wb = new XlsFormatWriter(config).build(container("book", "sheet", t1, t2));
        Sheet sheet = onlySheet(wb, "sheet");

        // 空行 0 → t2 は行 3 から
        assertThat(cell(sheet, 3, 0), is("SETUP_TABLE=T2"));
        // 罫線なし
        assertThat(sheet.getRow(0).getCell(0).getCellStyle().getBorderTop(), is(CellStyle.BORDER_NONE));
        // 背景色は上書き値
        assertThat(sheet.getRow(0).getCell(0).getCellStyle().getFillForegroundColor(), is(customHeader));
        // 自動列幅なし → 自動調整時の幅（最長 "SETUP_TABLE=T2" 14 文字＝(14+2)*256）にはならない
        assertThat(sheet.getColumnWidth(0), is(not((14 + 2) * 256)));
    }

    /**
     * Given: 空行数に負数。
     * When : ExcelFormatConfig 構築。
     * Then : IllegalArgumentException。
     */
    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeBlankRows() {
        ExcelFormatConfig.defaults().withBlankRowsBetweenBlocks(-1);
    }

    // ------------------------------------------------------------------ write (I/O) & multi-sheet

    /**
     * Given: 2 セクションのコンテナ。
     * When : write。
     * Then : {@code <basePath>/<コンテナ名>.xlsx} が生成され、各セクション＝シートになる。
     */
    @Test
    public void writesWorkbookFileWithSheetPerSection() throws Exception {
        TestDataSection s1 = new TestDataSection("Sheet1", Collections.<TestDataBlock>singletonList(
                new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "T", row("C"),
                        Collections.singletonList(row("v")))));
        TestDataSection s2 = new TestDataSection("Sheet2", Collections.<TestDataBlock>singletonList(
                new ListMapBlock("", "lm", row("K"), Collections.singletonList(row("1")))));
        TestDataContainer c = new TestDataContainer("MyBook", Arrays.asList(s1, s2));

        new XlsFormatWriter().write(c, folder.getRoot().getAbsolutePath());

        File xlsx = new File(folder.getRoot(), "MyBook.xlsx");
        assertTrue(xlsx.exists());
        try (java.io.InputStream in = new java.io.FileInputStream(xlsx)) {
            Workbook wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(in);
            assertThat(wb.getSheet("Sheet1"), is(notNullValue()));
            assertThat(wb.getSheet("Sheet2"), is(notNullValue()));
            assertThat(wb.getSheet("Sheet1").getRow(0).getCell(0).getStringCellValue(), is("SETUP_TABLE=T"));
        }
    }

    /**
     * Given: マーカーカラム背景色を上書きした設定。
     * When : build。
     * Then : マーカーカラムに上書き色が付く。
     */
    @Test
    public void honorsMarkerColumnColorOverride() {
        short custom = org.apache.poi.ss.usermodel.IndexedColors.LIGHT_TURQUOISE.getIndex();
        ExcelFormatConfig config = ExcelFormatConfig.defaults().withMarkerColumnColor(custom);
        TableDataBlock table = new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "T",
                row("ID", "[NOTE]"), Collections.singletonList(row("1", "memo")));
        Workbook wb = new XlsFormatWriter(config).build(container("book", "sheet", table));
        Sheet sheet = onlySheet(wb, "sheet");

        assertThat(sheet.getRow(2).getCell(1).getCellStyle().getFillForegroundColor(), is(custom));
    }

    /**
     * Given: 閉じ括弧の無いカラム名 {@code [half}（マーカーカラムでない）。
     * When : build。
     * Then : マーカー背景色は付かない（{@code [...]} の両端一致のみがマーカー）。
     */
    @Test
    public void doesNotTintUnclosedBracketColumn() {
        TableDataBlock table = new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "T",
                row("ID", "[half"), Collections.singletonList(row("1", "x")));
        Sheet sheet = onlySheet(build(container("book", "sheet", table)), "sheet");

        short marker = ExcelFormatConfig.defaults().getMarkerColumnColorIndex();
        assertThat(sheet.getRow(2).getCell(1).getCellStyle().getFillForegroundColor(), is(not(marker)));
    }

    /**
     * Given: 型・長さが null のフィールドと値が null のディレクティブを持つ固定長ファイル。
     * When : build。
     * Then : 省略は空セルとして書かれる（{@code null} → 空文字。データ行の null とは区別）。
     */
    @Test
    public void writesOmittedMetaAndFieldAsEmpty() {
        RecordLayout record = new RecordLayout("data",
                Collections.singletonList(new FieldDef("f1", null, null)),
                Collections.singletonList(row("v")));
        Map<String, String> directives = new LinkedHashMap<String, String>();
        directives.put("text-encoding", null);
        FileDataBlock file = new FileDataBlock(DataType.SETUP_FIXED, "", "f.dat",
                FileDataBlock.FileType.FIXED, directives, Collections.singletonList(record));
        Sheet sheet = onlySheet(build(container("book", "sheet", file)), "sheet");

        // ディレクティブ値 null → 空セル
        assertThat(cell(sheet, 1, 0), is("text-encoding"));
        assertThat(cell(sheet, 1, 1), is(""));
        // 型・長さ null → 空セル
        assertThat(line(sheet, 3), is(Arrays.asList("", "")));   // 型行
        assertThat(line(sheet, 4), is(Arrays.asList("", "")));   // 長さ行
        // データ行の値は記法のまま
        assertThat(line(sheet, 5), is(Arrays.asList("", "v")));
    }

    /**
     * Given: 送信系 4 種すべて。
     * When : build。
     * Then : いずれもデータ行の列 0 に no（連番）が付く（送信系判定が全種別で成立）。
     */
    @Test
    public void writesSequenceNoForAllSendSyncTypes() {
        DataType[] types = {
                DataType.EXPECTED_REQUEST_HEADER_MESSAGES,
                DataType.EXPECTED_REQUEST_BODY_MESSAGES,
                DataType.RESPONSE_HEADER_MESSAGES,
                DataType.RESPONSE_BODY_MESSAGES};
        for (DataType type : types) {
            RecordLayout record = new RecordLayout("no",
                    Collections.singletonList(new FieldDef("f", "半角", "5")),
                    Collections.singletonList(row("v")));
            MessageDataBlock message = new MessageDataBlock(type, "[g]", "id", map(), map(),
                    Collections.singletonList(record));
            Sheet sheet = onlySheet(build(container("book", "sheet", message)), "sheet");
            // 識別行 → 名前(1) → 型(2) → 長さ(3) → データ(4)。データ行の列 0 は連番 "1"
            assertThat("type=" + type, cell(sheet, 4, 0), is("1"));
        }
    }

    /**
     * Given: 親ディレクトリを作成できない出力先（既存の通常ファイルを basePath に与える）。
     * When : write。
     * Then : {@link java.io.UncheckedIOException} を送出する。
     */
    @Test(expected = java.io.UncheckedIOException.class)
    public void wrapsIoFailure() throws Exception {
        File blocker = folder.newFile("blocker");  // 通常ファイル。配下にディレクトリは作れない
        TestDataContainer c = container("Book", "s",
                new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "T", row("C"),
                        Collections.singletonList(row("v"))));
        new XlsFormatWriter().write(c, blocker.getAbsolutePath());
    }

    // ------------------------------------------------------------------ round-trip

    /** モデルを書き出し、実 {@link XlsFormatReader} で読み戻す。 */
    private TestDataContainer roundTrip(String book, String sheet, TestDataBlock block) {
        TestDataContainer container = container(book, sheet, block);
        new XlsFormatWriter().write(container, folder.getRoot().getAbsolutePath());
        return new XlsFormatReader().read(folder.getRoot().getAbsolutePath(), book + "/" + sheet);
    }

    /**
     * Given: テーブルブロック。
     * When : 書き出し → 実 Reader で読み戻し。
     * Then : データタイプ・識別子・カラム名・行が一致。
     */
    @Test
    public void roundTripsTable() {
        TableDataBlock table = new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "USERS",
                row("USER_NAME", "AGE"),
                Arrays.asList(row("${u}", "20"), row("alice", "")));

        TestDataBlock read = roundTrip("rt_table", "s", table).getSections().get(0).getBlocks().get(0);

        TableDataBlock actual = (TableDataBlock) read;
        assertThat(actual.getDataType(), is(DataType.SETUP_TABLE_DATA));
        assertThat(actual.getIdentifier(), is("USERS"));
        assertThat(actual.getColumnNames(), is(Arrays.asList("USER_NAME", "AGE")));
        assertThat(actual.getRows().get(0), is(Arrays.asList("${u}", "20")));
        assertThat(actual.getRows().get(1), is(Arrays.asList("alice", "")));
    }

    /**
     * Given: null セルと空文字セルを持つテーブル。
     * When : 書き出し → 実 Reader で読み戻し。
     * Then : null は文字列 {@code "null"} として戻り（リテラル null を書くため、空インタープリタの読み戻しで
     *        文字列化される＝既知の非可逆。Writer Javadoc 記載）、空文字 {@code ""} は空文字のまま戻る。
     *        この非可逆挙動をテストで固定し将来のリグレッションを検知する。
     */
    @Test
    public void roundTripsNullCellAsLiteralNullString() {
        TableDataBlock table = new TableDataBlock(DataType.SETUP_TABLE_DATA, "", "T",
                row("A", "B"), Collections.singletonList(row(null, "")));

        TestDataBlock read = roundTrip("rt_null", "s", table).getSections().get(0).getBlocks().get(0);

        TableDataBlock actual = (TableDataBlock) read;
        // null → リテラル "null" を書く → 読み戻しは文字列 "null"（null ↔ null は Excel 経路では復元不可）
        assertThat(actual.getRows().get(0).get(0), is("null"));
        // "" は空文字のまま
        assertThat(actual.getRows().get(0).get(1), is(""));
    }

    /**
     * Given: LIST_MAP ブロック。
     * When : 往復。
     * Then : 列順・値が一致。
     */
    @Test
    public void roundTripsListMap() {
        ListMapBlock listMap = new ListMapBlock("", "result",
                row("ID", "NAME"), Arrays.asList(row("${id}", ""), row("2", "bob")));

        TestDataBlock read = roundTrip("rt_listmap", "s", listMap).getSections().get(0).getBlocks().get(0);

        ListMapBlock actual = (ListMapBlock) read;
        assertThat(actual.getIdentifier(), is("result"));
        assertThat(actual.getColumnNames(), is(Arrays.asList("ID", "NAME")));
        assertThat(actual.getRows().get(0), is(Arrays.asList("${id}", "")));
        assertThat(actual.getRows().get(1), is(Arrays.asList("2", "bob")));
    }

    /**
     * Given: 固定長ファイル（型・長さ・{@code -} 含む）。
     * When : 往復。
     * Then : レコード種別・型記法・長さ（{@code -} 含む）・値が原文どおり。
     */
    @Test
    public void roundTripsFixedFile() {
        RecordLayout record = new RecordLayout("data",
                Arrays.asList(new FieldDef("f1", "半角英字", "-"), new FieldDef("f2", "半角英字", "5")),
                Collections.singletonList(row("abcd", "xy")));
        FileDataBlock file = new FileDataBlock(DataType.SETUP_FIXED, "", "test.dat",
                FileDataBlock.FileType.FIXED, map("text-encoding", "UTF-8"),
                Collections.singletonList(record));

        TestDataBlock read = roundTrip("rt_fixed", "s", file).getSections().get(0).getBlocks().get(0);

        FileDataBlock actual = (FileDataBlock) read;
        assertThat(actual.getIdentifier(), is("test.dat"));
        assertThat(actual.getFileType(), is(FileDataBlock.FileType.FIXED));
        RecordLayout rec = actual.getRecords().get(0);
        assertThat(rec.getRecordType(), is("data"));
        assertThat(rec.getFields().get(0).getType(), is("半角英字"));
        assertThat(rec.getFields().get(0).getLength(), is("-"));
        assertThat(rec.getFields().get(1).getLength(), is("5"));
        assertThat(rec.getRows().get(0), is(Arrays.asList("abcd", "xy")));
    }

    /**
     * Given: 2 レコードレイアウト（種別非空）を持つ固定長ファイル。
     * When : 往復。
     * Then : 2 レコードが各レコード種別・フィールド・値のまま分割復元される
     *        （番人が弾かない正常側の境界＝複数レコード版面の対称性を実 Reader で固定）。
     */
    @Test
    public void roundTripsMultipleRecordLayouts() {
        RecordLayout header = new RecordLayout("header",
                Collections.singletonList(new FieldDef("h1", "半角英字", "5")),
                Collections.singletonList(row("AAAAA")));
        RecordLayout data = new RecordLayout("data",
                Collections.singletonList(new FieldDef("d1", "半角英字", "2")),
                Collections.singletonList(row("xy")));
        FileDataBlock file = new FileDataBlock(DataType.SETUP_FIXED, "", "multi.dat",
                FileDataBlock.FileType.FIXED, map(), Arrays.asList(header, data));

        TestDataBlock read = roundTrip("rt_multi", "s", file).getSections().get(0).getBlocks().get(0);

        FileDataBlock actual = (FileDataBlock) read;
        assertThat(actual.getRecords().size(), is(2));
        assertThat(actual.getRecords().get(0).getRecordType(), is("header"));
        assertThat(actual.getRecords().get(0).getRows().get(0), is(Arrays.asList("AAAAA")));
        assertThat(actual.getRecords().get(1).getRecordType(), is("data"));
        assertThat(actual.getRecords().get(1).getRows().get(0), is(Arrays.asList("xy")));
    }

    /**
     * Given: 可変長ファイル（長さなし）。
     * When : 往復。
     * Then : 長さは省略（null）。
     */
    @Test
    public void roundTripsVariableFile() {
        RecordLayout record = new RecordLayout("data",
                Collections.singletonList(new FieldDef("f1", "半角英字", null)),
                Collections.singletonList(row("${v}")));
        FileDataBlock file = new FileDataBlock(DataType.SETUP_VARIABLE, "", "in.csv",
                FileDataBlock.FileType.VARIABLE, map(), Collections.singletonList(record));

        TestDataBlock read = roundTrip("rt_var", "s", file).getSections().get(0).getBlocks().get(0);

        FileDataBlock actual = (FileDataBlock) read;
        assertThat(actual.getFileType(), is(FileDataBlock.FileType.VARIABLE));
        RecordLayout rec = actual.getRecords().get(0);
        assertThat(rec.getFields().get(0).getLength(), is(nullValue()));
        assertThat(rec.getRows().get(0), is(Arrays.asList("${v}")));
    }

    /**
     * Given: FW 制御ヘッダ＋本文の MESSAGE。
     * When : 往復。
     * Then : FW ヘッダ・本文レコードが記法のまま一致。
     */
    @Test
    public void roundTripsMessage() {
        RecordLayout record = new RecordLayout("data",
                Arrays.asList(new FieldDef("body1", "半角英字", "10"), new FieldDef("body2", "半角英字", "5")),
                Collections.singletonList(row("${b}", "xyz")));
        MessageDataBlock message = new MessageDataBlock(DataType.MESSAGE, "", "msg1",
                map(), map("requestId", "${rid}"), Collections.singletonList(record));

        TestDataBlock read = roundTrip("rt_msg", "s", message).getSections().get(0).getBlocks().get(0);

        MessageDataBlock actual = (MessageDataBlock) read;
        assertThat(actual.getDataType(), is(DataType.MESSAGE));
        assertThat(actual.getIdentifier(), is("msg1"));
        assertThat(actual.getFwHeaderFields().get("requestId"), is("${rid}"));
        RecordLayout rec = actual.getRecords().get(0);
        assertThat(rec.getFields().get(0).getName(), is("body1"));
        assertThat(rec.getRows().get(0), is(Arrays.asList("${b}", "xyz")));
    }

    /**
     * Given: 送信系（no 列）。
     * When : 往復。
     * Then : データタイプ・グループ ID・本文値（no を除く）が一致。FW ヘッダは空。
     */
    @Test
    public void roundTripsSendSyncMessage() {
        RecordLayout record = new RecordLayout("no",
                Arrays.asList(new FieldDef("requestId", "半角", "20"), new FieldDef("resendFlag", "半角", "1")),
                Collections.singletonList(row("RM21AA0104_01", "0")));
        MessageDataBlock message = new MessageDataBlock(DataType.EXPECTED_REQUEST_HEADER_MESSAGES,
                "[case1]", "RM21AA0104_01", map(), map(), Collections.singletonList(record));

        TestDataBlock read = roundTrip("rt_sendsync", "s", message).getSections().get(0).getBlocks().get(0);

        MessageDataBlock actual = (MessageDataBlock) read;
        assertThat(actual.getDataType(), is(DataType.EXPECTED_REQUEST_HEADER_MESSAGES));
        assertThat(actual.getGroupId(), is("[case1]"));
        assertThat(actual.getIdentifier(), is("RM21AA0104_01"));
        assertTrue(actual.getFwHeaderFields().isEmpty());
        RecordLayout rec = actual.getRecords().get(0);
        List<String> fieldNames = new ArrayList<String>();
        for (FieldDef f : rec.getFields()) {
            fieldNames.add(f.getName());
        }
        assertThat(fieldNames, is(Arrays.asList("requestId", "resendFlag")));
        assertThat(rec.getRows().get(0), is(Arrays.asList("RM21AA0104_01", "0")));
    }
}
