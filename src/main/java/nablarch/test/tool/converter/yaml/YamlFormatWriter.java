package nablarch.test.tool.converter.yaml;

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

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * {@link TestDataContainer} を YAML ファイル群として書き出す Writer。
 *
 * <p>出力先構成: outputPath/containerName/sectionName.yaml</p>
 */
public class YamlFormatWriter implements TestDataFormatWriter {

    @Override
    public void write(TestDataContainer container, Path outputPath, boolean overwrite) throws ConverterException {
        Path containerDir = outputPath.resolve(container.getName());
        try {
            Files.createDirectories(containerDir);
        } catch (IOException e) {
            throw new ConverterException("Failed to create directory: " + containerDir, e);
        }

        for (TestDataSection section : container.getSections()) {
            Path yamlFile = containerDir.resolve(section.getName() + ".yaml");
            if (!overwrite && Files.exists(yamlFile)) {
                throw new ConverterException("File already exists (use overwrite=true): " + yamlFile);
            }
            try {
                Writer w = Files.newBufferedWriter(yamlFile, StandardCharsets.UTF_8);
                try {
                    writeSection(w, section);
                } finally {
                    w.close();
                }
            } catch (IOException e) {
                throw new ConverterException("Failed to write YAML: " + yamlFile, e);
            }
        }
    }

    private void writeSection(Writer w, TestDataSection section) throws IOException {
        // Group blocks by top-level key, preserving order; output each key once
        // Use a linked map to maintain insertion order
        java.util.LinkedHashMap<String, List<TestDataBlock>> grouped = new java.util.LinkedHashMap<>();
        for (TestDataBlock block : section.getBlocks()) {
            String key = sectionKey(block);
            if (!grouped.containsKey(key)) {
                grouped.put(key, new java.util.ArrayList<>());
            }
            grouped.get(key).add(block);
        }

        for (Map.Entry<String, List<TestDataBlock>> entry : grouped.entrySet()) {
            w.write(entry.getKey() + ":\n");
            for (TestDataBlock block : entry.getValue()) {
                writeBlock(w, block);
            }
        }
    }

    private String sectionKey(TestDataBlock block) {
        DataType dt = block.getDataType();
        if (dt == DataType.SETUP_TABLE_DATA) return "setup_tables";
        if (dt == DataType.EXPECTED_TABLE_DATA) return "expected_tables";
        if (dt == DataType.EXPECTED_COMPLETED) return "expected_complete_tables";
        if (dt == DataType.LIST_MAP) return "list_maps";
        if (dt == DataType.SETUP_FIXED || dt == DataType.SETUP_VARIABLE) return "setup_files";
        if (dt == DataType.EXPECTED_FIXED || dt == DataType.EXPECTED_VARIABLE) return "expected_files";
        if (dt == DataType.MESSAGE) return "messages";
        if (dt == DataType.EXPECTED_REQUEST_HEADER_MESSAGES) return "expected_request_header_messages";
        if (dt == DataType.EXPECTED_REQUEST_BODY_MESSAGES) return "expected_request_body_messages";
        if (dt == DataType.RESPONSE_HEADER_MESSAGES) return "response_header_messages";
        if (dt == DataType.RESPONSE_BODY_MESSAGES) return "response_body_messages";
        throw new AssertionError("UNREACHABLE: unknown DataType: " + dt);
    }

    private void writeBlock(Writer w, TestDataBlock block) throws IOException {
        if (block instanceof ColumnRowDataBlock) {
            writeColumnRowBlock(w, (ColumnRowDataBlock) block);
        } else if (block instanceof FileDataBlock) {
            writeFileBlock(w, (FileDataBlock) block);
        } else if (block instanceof MessageDataBlock) {
            writeMessageBlock(w, (MessageDataBlock) block);
        }
    }

    private void writeColumnRowBlock(Writer w, ColumnRowDataBlock block) throws IOException {
        boolean isListMap = block.getDataType() == DataType.LIST_MAP;
        String indent = "  ";

        // group_id before identifier key
        if (!block.getGroupId().isEmpty()) {
            w.write(indent + "- group_id: " + quoteString(block.getGroupId()) + "\n");
            w.write(indent + "  " + (isListMap ? "id" : "table") + ": " + quoteString(block.getIdentifier()) + "\n");
        } else {
            w.write(indent + "- " + (isListMap ? "id" : "table") + ": " + quoteString(block.getIdentifier()) + "\n");
        }

        if (block.getRows().isEmpty()) {
            w.write(indent + "  rows: []\n");
        } else {
            w.write(indent + "  rows:\n");
            for (List<String> row : block.getRows()) {
                w.write(indent + "    - ");
                boolean first = true;
                for (int i = 0; i < block.getColumnNames().size(); i++) {
                    String colName = block.getColumnNames().get(i);
                    String value = i < row.size() ? row.get(i) : "";
                    if (!first) {
                        w.write(indent + "      ");
                    }
                    w.write(quoteKey(colName) + ": " + quoteValue(value) + "\n");
                    first = false;
                }
            }
        }
    }

    private void writeFileBlock(Writer w, FileDataBlock block) throws IOException {
        String indent = "  ";
        String fileTypeStr = block.getFileType() == FileDataBlock.FileType.FIXED ? "fixed" : "variable";

        if (!block.getGroupId().isEmpty()) {
            w.write(indent + "- group_id: " + quoteString(block.getGroupId()) + "\n");
            w.write(indent + "  path: " + quoteString(block.getIdentifier()) + "\n");
        } else {
            w.write(indent + "- path: " + quoteString(block.getIdentifier()) + "\n");
        }
        w.write(indent + "  type: " + fileTypeStr + "\n");

        if (!block.getDirectives().isEmpty()) {
            w.write(indent + "  directives:\n");
            for (Map.Entry<String, String> entry : block.getDirectives().entrySet()) {
                w.write(indent + "    " + entry.getKey() + ": " + quoteString(entry.getValue()) + "\n");
            }
        }

        if (block.getRecords().isEmpty()) {
            w.write(indent + "  records: []\n");
        } else {
            w.write(indent + "  records:\n");
            for (RecordLayout record : block.getRecords()) {
                writeRecordLayout(w, record, indent + "    ", block.getFileType() == FileDataBlock.FileType.FIXED);
            }
        }
    }

    private void writeMessageBlock(Writer w, MessageDataBlock block) throws IOException {
        String indent = "  ";
        boolean isMessage = block.getDataType() == DataType.MESSAGE;

        if (!block.getGroupId().isEmpty()) {
            w.write(indent + "- group_id: " + quoteString(block.getGroupId()) + "\n");
            w.write(indent + "  id: " + quoteString(block.getIdentifier()) + "\n");
        } else {
            w.write(indent + "- id: " + quoteString(block.getIdentifier()) + "\n");
        }

        if (!block.getDirectives().isEmpty()) {
            w.write(indent + "  directives:\n");
            for (Map.Entry<String, String> entry : block.getDirectives().entrySet()) {
                w.write(indent + "    " + entry.getKey() + ": " + quoteString(entry.getValue()) + "\n");
            }
        }

        // fw_header は MESSAGE（messages）のみ出力する
        if (isMessage && !block.getFwHeaderFields().isEmpty()) {
            w.write(indent + "  fw_header:\n");
            for (Map.Entry<String, String> entry : block.getFwHeaderFields().entrySet()) {
                w.write(indent + "    " + entry.getKey() + ": " + quoteString(entry.getValue()) + "\n");
            }
        }

        w.write(indent + "  records:\n");
        for (RecordLayout record : block.getRecords()) {
            writeMessageRecord(w, record, indent + "    ");
        }
    }

    private void writeRecordLayout(Writer w, RecordLayout record, String indent, boolean includeLength) throws IOException {
        w.write(indent + "- record_type: " + quoteString(record.getRecordType()) + "\n");
        w.write(indent + "  fields:\n");
        for (FieldDef field : record.getFields()) {
            if (includeLength && field.getLength() != null && field.getType() != null) {
                w.write(indent + "    - {name: " + quoteString(field.getName())
                        + ", type: " + quoteString(field.getType())
                        + ", length: " + quoteString(field.getLength()) + "}\n");
            } else if (field.getType() != null) {
                w.write(indent + "    - {name: " + quoteString(field.getName())
                        + ", type: " + quoteString(field.getType()) + "}\n");
            } else {
                w.write(indent + "    - {name: " + quoteString(field.getName()) + "}\n");
            }
        }
        w.write(indent + "  rows:\n");
        for (List<String> row : record.getRows()) {
            w.write(indent + "    - [");
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) w.write(", ");
                w.write(quoteString(row.get(i)));
            }
            w.write("]\n");
        }
    }

    private void writeMessageRecord(Writer w, RecordLayout record, String indent) throws IOException {
        w.write(indent + "- record_type: " + quoteString(record.getRecordType()) + "\n");
        w.write(indent + "  fields:\n");
        for (FieldDef field : record.getFields()) {
            if (field.getType() != null) {
                w.write(indent + "    - {name: " + quoteString(field.getName())
                        + ", type: " + quoteString(field.getType()) + "}\n");
            } else {
                w.write(indent + "    - {name: " + quoteString(field.getName()) + "}\n");
            }
        }
        w.write(indent + "  rows:\n");
        for (List<String> row : record.getRows()) {
            w.write(indent + "    - [");
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) w.write(", ");
                w.write(quoteString(row.get(i)));
            }
            w.write("]\n");
        }
    }

    /** YAML キーのクォート。マーカーカラム "[FLAG]" は必ずダブルクォート。 */
    private String quoteKey(String key) {
        if (key.startsWith("[")) {
            return "\"" + key + "\"";
        }
        return key;
    }

    /** 値をダブルクォートで出力する。null は unquoted null。 */
    private String quoteValue(String value) {
        if (value == null) return "null";
        return "\"" + escapeYaml(value) + "\"";
    }

    /** 文字列をダブルクォートで出力する。null は unquoted null。 */
    private String quoteString(String value) {
        if (value == null) return "null";
        return "\"" + escapeYaml(value) + "\"";
    }

    private String escapeYaml(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
