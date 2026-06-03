package nablarch.test.tool.converter.xls;

import nablarch.test.core.reader.DataType;
import nablarch.test.tool.converter.ConverterException;
import nablarch.test.tool.converter.TestDataFormatReader;
import nablarch.test.tool.converter.model.ColumnRowDataBlock;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * XLS ファイルを読み込んで {@link TestDataContainer} に変換する Reader。
 */
public class XlsFormatReader implements TestDataFormatReader {

    /** 直前の read() 呼び出しで検出したコメント行数。 */
    private int lastCommentLineCount = 0;

    /** 直前の read() 呼び出しで検出したコメント行数を返す。 */
    public int getLastCommentLineCount() {
        return lastCommentLineCount;
    }

    @Override
    public TestDataContainer read(Path sourcePath) throws ConverterException {
        String fileName = sourcePath.getFileName().toString();
        String name;
        if (fileName.endsWith(".xlsx")) {
            name = fileName.substring(0, fileName.length() - 5);
        } else if (fileName.endsWith(".xls")) {
            name = fileName.substring(0, fileName.length() - 4);
        } else {
            name = fileName;
        }
        lastCommentLineCount = 0;

        try {
            Workbook wb;
            try (FileInputStream fis = new FileInputStream(sourcePath.toFile())) {
                wb = WorkbookFactory.create(fis);
            }
            List<TestDataSection> sections = new ArrayList<>();
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                sections.add(parseSheet(sheet, sourcePath));
            }
            return new TestDataContainer(name, sections);
        } catch (Exception e) {
            throw new ConverterException("Failed to read XLS file: " + sourcePath, e);
        }
    }

    private TestDataSection parseSheet(Sheet sheet, Path sourcePath) throws ConverterException {
        List<List<String>> rows = readRows(sheet, sourcePath);
        List<TestDataBlock> blocks = parseBlocks(rows);
        return new TestDataSection(sheet.getSheetName(), blocks);
    }

    /** シートの全行を読み込み、コメント行スキップ・行内コメント切り捨て・空行スキップを適用する。 */
    private List<List<String>> readRows(Sheet sheet, Path sourcePath) {
        List<List<String>> result = new ArrayList<>();
        int lastRow = sheet.getLastRowNum();
        for (int r = 0; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            List<String> cells = readCells(row, sourcePath, r + 1);
            if (cells.isEmpty()) {
                continue;  // HC-07: 空行スキップ
            }
            if (cells.get(0).startsWith("//")) {
                // HC-05: コメント行スキップ（警告出力・カウント）
                lastCommentLineCount++;
                System.err.println("WARN: " + sourcePath + " sheet=" + sheet.getSheetName()
                        + " row=" + (r + 1) + ": comment line skipped (HC-05)");
                continue;
            }
            result.add(cells);
        }
        return result;
    }

    /** 1行のセルを読み込む。行内コメント（HC-06）を切り捨て、末尾の空セルは保持する。 */
    private List<String> readCells(Row row, Path sourcePath, int rowNum) {
        int lastCell = row.getLastCellNum();
        List<String> cells = new ArrayList<>();
        for (int c = 0; c < lastCell; c++) {
            Cell cell = row.getCell(c);
            String value;
            if (cell == null) {
                value = "";
            } else {
                // 数値書式・日付書式セルは警告出力（NG-4）
                if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    System.err.println("WARN: " + sourcePath + " row=" + rowNum + " col=" + (c + 1)
                            + ": numeric/date cell detected. Cell.toString() result used.");
                }
                value = cell.toString();
            }
            if (c > 0 && value.startsWith("//")) {
                // HC-06: 先頭以外のセルが "//" で始まる場合、そのセル以降を切り捨て
                break;
            }
            cells.add(value);
        }
        return trimTrailingEmpty(cells);
    }

    /** 行リストを走査してデータブロックに分割する。 */
    private List<TestDataBlock> parseBlocks(List<List<String>> rows) throws ConverterException {
        List<TestDataBlock> blocks = new ArrayList<>();
        int i = 0;
        while (i < rows.size()) {
            List<String> row = rows.get(i);
            DataType dataType = detectDataType(row.get(0));
            if (dataType == null) {
                i++;
                continue;
            }
            // 識別行の解析（DT-02, DT-06）
            String[] parsed = parseIdentifierRow(row.get(0), dataType);
            String groupId = parsed[0];
            String identifier = parsed[1];

            if (isColumnRowType(dataType)) {
                // テーブルデータ・LIST_MAP の解析
                int[] next = new int[1];
                next[0] = i + 1;
                TestDataBlock block = parseColumnRowBlock(dataType, groupId, identifier, rows, next);
                blocks.add(block);
                i = next[0];
            } else if (isFileType(dataType)) {
                int[] next = new int[1];
                next[0] = i + 1;
                TestDataBlock block = parseFileBlock(dataType, groupId, identifier, rows, next);
                blocks.add(block);
                i = next[0];
            } else if (isMessageType(dataType)) {
                int[] next = new int[1];
                next[0] = i + 1;
                TestDataBlock block = parseMessageBlock(dataType, groupId, identifier, rows, next);
                blocks.add(block);
                i = next[0];
            } else {
                throw new AssertionError("UNREACHABLE: unexpected DataType: " + dataType);
            }
        }
        return blocks;
    }

    /** テーブルデータブロック・LIST_MAP ブロックの解析（SS-01, HC-01, HC-03, HC-04）。 */
    private TestDataBlock parseColumnRowBlock(DataType dataType, String groupId, String identifier,
                                               List<List<String>> rows, int[] nextIndex) {
        int i = nextIndex[0];
        // ヘッダ行
        List<String> headerRow = i < rows.size() ? rows.get(i++) : new ArrayList<>();
        List<String> columnNames = trimTrailingEmpty(headerRow);  // HC-03

        // データ行
        List<List<String>> dataRows = new ArrayList<>();
        while (i < rows.size()) {
            List<String> row = rows.get(i);
            if (detectDataType(row.get(0)) != null) {
                break;
            }
            // HC-04: データ行がヘッダより短い場合、空文字補完
            List<String> dataRow = new ArrayList<>(row);
            while (dataRow.size() < columnNames.size()) {
                dataRow.add("");
            }
            dataRows.add(new ArrayList<>(dataRow.subList(0, columnNames.size())));
            i++;
        }
        nextIndex[0] = i;

        if (dataType == DataType.LIST_MAP) {
            return new ListMapBlock(groupId, identifier, columnNames, dataRows);
        }
        return new TableDataBlock(dataType, groupId, identifier, columnNames, dataRows);
    }

    /** ファイルデータブロックの解析（SS-08〜SS-13, SS-15, SS-17, DR-01, DR-07）。 */
    private FileDataBlock parseFileBlock(DataType dataType, String groupId, String identifier,
                                          List<List<String>> rows, int[] nextIndex) {
        Map<String, String> directives = new LinkedHashMap<>();
        List<RecordLayout> records = new ArrayList<>();
        int i = nextIndex[0];

        // ディレクティブ行の読み込み
        // 判定ルール: 先頭セルが非空かつ次行が EOF または次行先頭も非空 → ディレクティブ行
        //            先頭セルが空 → フィールド名行の開始（break）
        //            次行先頭が空 → フィールド名行の開始（break）
        while (i < rows.size()) {
            List<String> row = rows.get(i);
            if (detectDataType(row.get(0)) != null) {
                break;
            }
            if (row.get(0).isEmpty()) {
                break;  // フィールド名行（先頭が空）に到達
            }
            // 次行が存在し、かつ次行先頭セルが空 → 現在行はフィールド名行の直前（break前にディレクティブ登録はしない）
            boolean nextExists = (i + 1 < rows.size()) && !rows.get(i + 1).isEmpty();
            boolean nextFirstEmpty = nextExists && rows.get(i + 1).get(0).isEmpty();
            if (nextFirstEmpty) {
                // 次行はフィールド名行（先頭空） → ここで break してレコードレイアウト解析へ
                break;
            }
            // ディレクティブ行として登録（次行が EOF / 次行先頭が非空 / 次行が新 DataType の場合も含む）
            directives.put(row.get(0), row.size() > 1 ? row.get(1) : "");
            i++;
        }

        // レコードレイアウトの解析
        FileDataBlock.FileType fileType = resolveFileType(dataType);
        i = parseRecordLayouts(rows, i, fileType == FileDataBlock.FileType.FIXED, null, records);

        nextIndex[0] = i;
        return new FileDataBlock(dataType, groupId, identifier, fileType, directives, records);
    }

    /** メッセージングデータブロックの解析（MS-01, MS-02）。 */
    private MessageDataBlock parseMessageBlock(DataType dataType, String groupId, String identifier,
                                                List<List<String>> rows, int[] nextIndex) {
        Map<String, String> directives = new LinkedHashMap<>();
        Map<String, String> fwHeaderFields = new LinkedHashMap<>();
        List<RecordLayout> records = new ArrayList<>();
        int i = nextIndex[0];

        // 先頭非空行（ディレクティブ または FW 制御ヘッダ）の読み込み。先頭が空になったらフィールド名行の開始
        while (i < rows.size()) {
            List<String> row = rows.get(i);
            if (detectDataType(row.get(0)) != null) {
                break;
            }
            if (row.get(0).isEmpty()) {
                break;  // フィールド名行（no列: 先頭が空）
            }
            String key = row.get(0);
            String value = row.size() > 1 ? row.get(1) : "";
            if (MessageDataBlock.KNOWN_DIRECTIVE_NAMES.contains(key)) {
                directives.put(key, value);
            } else {
                fwHeaderFields.put(key, value);
            }
            i++;
        }

        // レコードレイアウトの解析（MS-02: no列省略 = 先頭セルが空がフィールド名行の合図）
        i = parseRecordLayouts(rows, i, false, "default", records);

        nextIndex[0] = i;
        return new MessageDataBlock(dataType, groupId, identifier, directives, fwHeaderFields, records);
    }

    /**
     * レコードレイアウト（フィールド名行→型行→[長行]→データ行）を解析して {@code out} に追加する。
     *
     * @param rows          シート全行リスト
     * @param startIndex    解析開始行インデックス
     * @param withLength    固定長ファイルの場合 true（フィールド長行を読む）
     * @param fixedRecordType null のとき行の先頭セルを recordType として使用（ファイルデータ）、
     *                        非 null のとき固定文字列を recordType として使用（メッセージング）
     * @param out           解析結果の格納先
     * @return 次の未処理行インデックス
     */
    private int parseRecordLayouts(List<List<String>> rows, int startIndex,
                                    boolean withLength, String fixedRecordType,
                                    List<RecordLayout> out) {
        int i = startIndex;
        while (i < rows.size()) {
            List<String> row = rows.get(i);
            if (detectDataType(row.get(0)) != null) {
                break;
            }
            if (fixedRecordType == null) {
                // ファイルデータ: 先頭セルが recordType（非空）
                if (row.get(0).isEmpty()) {
                    break;
                }
            } else {
                // メッセージング: 先頭セルが空 = フィールド名行
                if (!row.get(0).isEmpty()) {
                    break;
                }
            }

            // フィールド名行（ファイルデータは先頭セルがレコード種別、メッセージングは先頭セルが空）
            String recordType = fixedRecordType != null ? fixedRecordType : row.get(0);
            List<String> fieldNames = trimTrailingEmpty(row.subList(1, row.size()));
            i++;

            // データ型行
            List<String> types = new ArrayList<>();
            if (i < rows.size() && rows.get(i).get(0).isEmpty()) {
                types = trimTrailingEmpty(rows.get(i).subList(1, rows.get(i).size()));
                i++;
            }

            // フィールド長行（固定長のみ）
            List<String> lengths = new ArrayList<>();
            if (withLength && i < rows.size() && rows.get(i).get(0).isEmpty()) {
                lengths = trimTrailingEmpty(rows.get(i).subList(1, rows.get(i).size()));
                i++;
            }

            // FieldDef の構築
            List<FieldDef> fields = new ArrayList<>();
            for (int f = 0; f < fieldNames.size(); f++) {
                String type = f < types.size() ? types.get(f) : null;
                String length = (withLength && f < lengths.size()) ? lengths.get(f) : null;
                fields.add(new FieldDef(fieldNames.get(f), type, length));
            }

            // データ行（HC-04: フィールド数に合わせて補完）
            List<List<String>> dataRows = new ArrayList<>();
            while (i < rows.size() && rows.get(i).get(0).isEmpty()) {
                List<String> dataRow = rows.get(i).subList(1, rows.get(i).size());
                List<String> padded = new ArrayList<>(dataRow);
                while (padded.size() < fields.size()) {
                    padded.add("");
                }
                dataRows.add(new ArrayList<>(padded.subList(0, fields.size())));
                i++;
                if (i < rows.size() && !rows.get(i).get(0).isEmpty()) {
                    break;
                }
            }

            out.add(new RecordLayout(recordType, fields, dataRows));
        }
        return i;
    }

    /** DataType の判定（DT-03: 前方一致）。DEFAULT は対象外。 */
    private DataType detectDataType(String cellValue) {
        if (cellValue == null || cellValue.isEmpty()) {
            return null;
        }
        for (DataType dt : DataType.values()) {
            if (dt == DataType.DEFAULT) {
                continue;
            }
            if (cellValue.startsWith(dt.getName())) {
                return dt;
            }
        }
        return null;
    }

    /** 識別行から groupId と identifier を解析する（DT-02, DT-06）。書式不正時は ConverterException をスロー。 */
    private String[] parseIdentifierRow(String cellValue, DataType dataType) throws ConverterException {
        String rest = cellValue.substring(dataType.getName().length());
        String groupId = "";
        if (rest.startsWith("[")) {
            int end = rest.indexOf(']');
            if (end > 0) {
                groupId = rest.substring(1, end);
                rest = rest.substring(end + 1);
            }
        }
        // "=" が必須区切り文字（DT-02）
        if (!rest.startsWith("=")) {
            throw new ConverterException("Invalid identifier row format (missing '='): " + cellValue);
        }
        String identifier = rest.substring(1);
        return new String[]{groupId, identifier};
    }

    private boolean isColumnRowType(DataType dt) {
        return dt == DataType.SETUP_TABLE_DATA || dt == DataType.EXPECTED_TABLE_DATA
                || dt == DataType.EXPECTED_COMPLETED || dt == DataType.LIST_MAP;
    }

    private boolean isFileType(DataType dt) {
        return dt == DataType.SETUP_FIXED || dt == DataType.SETUP_VARIABLE
                || dt == DataType.EXPECTED_FIXED || dt == DataType.EXPECTED_VARIABLE;
    }

    private boolean isMessageType(DataType dt) {
        return dt == DataType.MESSAGE || dt == DataType.EXPECTED_REQUEST_HEADER_MESSAGES
                || dt == DataType.EXPECTED_REQUEST_BODY_MESSAGES
                || dt == DataType.RESPONSE_HEADER_MESSAGES || dt == DataType.RESPONSE_BODY_MESSAGES;
    }

    private FileDataBlock.FileType resolveFileType(DataType dt) {
        if (dt == DataType.SETUP_FIXED || dt == DataType.EXPECTED_FIXED) {
            return FileDataBlock.FileType.FIXED;
        }
        return FileDataBlock.FileType.VARIABLE;
    }

    private List<String> trimTrailingEmpty(List<String> list) {
        List<String> result = new ArrayList<>(list);
        while (!result.isEmpty() && result.get(result.size() - 1).isEmpty()) {
            result.remove(result.size() - 1);
        }
        return result;
    }
}
