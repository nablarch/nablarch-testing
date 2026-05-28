package nablarch.test.tool.converter.xls;

import nablarch.test.core.reader.DataType;
import nablarch.test.tool.converter.ConverterException;
import nablarch.test.tool.converter.TestDataFormatWriter;
import nablarch.test.tool.converter.model.ColumnRowDataBlock;
import nablarch.test.tool.converter.model.FieldDef;
import nablarch.test.tool.converter.model.FileDataBlock;
import nablarch.test.tool.converter.model.MessageDataBlock;
import nablarch.test.tool.converter.model.RecordLayout;
import nablarch.test.tool.converter.model.TestDataBlock;
import nablarch.test.tool.converter.model.TestDataContainer;
import nablarch.test.tool.converter.model.TestDataSection;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * {@link TestDataContainer} を XLS ファイルとして書き出す Writer。
 *
 * <p>出力先: outputPath/containerName.xls</p>
 */
public class XlsFormatWriter implements TestDataFormatWriter {

    @Override
    public void write(TestDataContainer container, Path outputPath, boolean overwrite) throws ConverterException {
        try {
            Files.createDirectories(outputPath);
        } catch (IOException e) {
            throw new ConverterException("Failed to create output directory: " + outputPath, e);
        }
        Path xlsFile = outputPath.resolve(container.getName() + ".xls");
        if (!overwrite && Files.exists(xlsFile)) {
            throw new ConverterException("File already exists (use overwrite=true): " + xlsFile);
        }

        Workbook wb = new HSSFWorkbook();
        try {
            for (TestDataSection section : container.getSections()) {
                Sheet sheet = wb.createSheet(section.getName());
                int rowNum = 0;
                for (TestDataBlock block : section.getBlocks()) {
                    rowNum = writeBlock(sheet, block, rowNum);
                }
            }
            FileOutputStream out = new FileOutputStream(xlsFile.toFile());
            try {
                wb.write(out);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new ConverterException("Failed to write XLS: " + xlsFile, e);
        }
    }

    private int writeBlock(Sheet sheet, TestDataBlock block, int rowNum) {
        if (block instanceof ColumnRowDataBlock) {
            return writeColumnRowBlock(sheet, (ColumnRowDataBlock) block, rowNum);
        } else if (block instanceof FileDataBlock) {
            return writeFileBlock(sheet, (FileDataBlock) block, rowNum);
        } else if (block instanceof MessageDataBlock) {
            return writeMessageBlock(sheet, (MessageDataBlock) block, rowNum);
        }
        return rowNum;
    }

    private int writeColumnRowBlock(Sheet sheet, ColumnRowDataBlock block, int rowNum) {
        boolean isListMap = block.getDataType() == DataType.LIST_MAP;
        // identifier row
        setCellStr(sheet, rowNum++, 0, buildIdentifierCell(block));
        // header row
        Row headerRow = sheet.createRow(rowNum++);
        List<String> columnNames = block.getColumnNames();
        for (int i = 0; i < columnNames.size(); i++) {
            setCellStrOnRow(headerRow, i, columnNames.get(i));
        }
        // data rows
        for (List<String> dataRow : block.getRows()) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < dataRow.size(); i++) {
                setCellStrOnRow(row, i, nullToLiteral(dataRow.get(i)));
            }
        }
        return rowNum;
    }

    private int writeFileBlock(Sheet sheet, FileDataBlock block, int rowNum) {
        // identifier row
        setCellStr(sheet, rowNum++, 0, buildIdentifierCell(block));
        // directives
        for (Map.Entry<String, String> entry : block.getDirectives().entrySet()) {
            Row row = sheet.createRow(rowNum++);
            setCellStrOnRow(row, 0, entry.getKey());
            setCellStrOnRow(row, 1, entry.getValue());
        }
        // records
        boolean isFixed = block.getFileType() == FileDataBlock.FileType.FIXED;
        for (RecordLayout record : block.getRecords()) {
            // field name row
            Row fnRow = sheet.createRow(rowNum++);
            setCellStrOnRow(fnRow, 0, record.getRecordType());
            List<FieldDef> fields = record.getFields();
            for (int i = 0; i < fields.size(); i++) {
                setCellStrOnRow(fnRow, i + 1, fields.get(i).getName());
            }
            // data type row
            Row typeRow = sheet.createRow(rowNum++);
            setCellStrOnRow(typeRow, 0, "");
            for (int i = 0; i < fields.size(); i++) {
                String type = fields.get(i).getType();
                setCellStrOnRow(typeRow, i + 1, type != null ? type : "");
            }
            // field length row (fixed only)
            if (isFixed) {
                Row lenRow = sheet.createRow(rowNum++);
                setCellStrOnRow(lenRow, 0, "");
                for (int i = 0; i < fields.size(); i++) {
                    String length = fields.get(i).getLength();
                    setCellStrOnRow(lenRow, i + 1, length != null ? length : "");
                }
            }
            // data rows
            for (List<String> dataRow : record.getRows()) {
                Row row = sheet.createRow(rowNum++);
                setCellStrOnRow(row, 0, "");
                for (int i = 0; i < dataRow.size(); i++) {
                    setCellStrOnRow(row, i + 1, nullToLiteral(dataRow.get(i)));
                }
            }
        }
        return rowNum;
    }

    private int writeMessageBlock(Sheet sheet, MessageDataBlock block, int rowNum) {
        // identifier row
        setCellStr(sheet, rowNum++, 0, buildIdentifierCell(block));
        // FW header rows
        for (Map.Entry<String, String> entry : block.getFwHeaderFields().entrySet()) {
            Row row = sheet.createRow(rowNum++);
            setCellStrOnRow(row, 0, entry.getKey());
            setCellStrOnRow(row, 1, entry.getValue());
        }
        // records (no-column: first cell empty)
        for (RecordLayout record : block.getRecords()) {
            // field name row (no-column)
            Row fnRow = sheet.createRow(rowNum++);
            setCellStrOnRow(fnRow, 0, "");
            List<FieldDef> fields = record.getFields();
            for (int i = 0; i < fields.size(); i++) {
                setCellStrOnRow(fnRow, i + 1, fields.get(i).getName());
            }
            // data type row
            Row typeRow = sheet.createRow(rowNum++);
            setCellStrOnRow(typeRow, 0, "");
            for (int i = 0; i < fields.size(); i++) {
                String type = fields.get(i).getType();
                setCellStrOnRow(typeRow, i + 1, type != null ? type : "");
            }
            // data rows
            for (List<String> dataRow : record.getRows()) {
                Row row = sheet.createRow(rowNum++);
                setCellStrOnRow(row, 0, "");
                for (int i = 0; i < dataRow.size(); i++) {
                    setCellStrOnRow(row, i + 1, nullToLiteral(dataRow.get(i)));
                }
            }
        }
        return rowNum;
    }

    /** 識別セルの文字列を生成する（7.2.2節）。 */
    private String buildIdentifierCell(TestDataBlock block) {
        StringBuilder sb = new StringBuilder(block.getDataType().getName());
        if (!block.getGroupId().isEmpty()) {
            sb.append("[").append(block.getGroupId()).append("]");
        }
        sb.append("=").append(block.getIdentifier());
        return sb.toString();
    }

    private String nullToLiteral(String value) {
        return value == null ? "null" : value;
    }

    private void setCellStr(Sheet sheet, int rowNum, int colNum, String value) {
        Row row = sheet.createRow(rowNum);
        setCellStrOnRow(row, colNum, value);
    }

    private void setCellStrOnRow(Row row, int colNum, String value) {
        Cell cell = row.createCell(colNum);
        cell.setCellValue(value);
    }
}
