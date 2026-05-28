package nablarch.test.tool.converter.model;

import nablarch.test.core.reader.DataType;

import java.util.List;
import java.util.Map;

/**
 * SETUP_FIXED / SETUP_VARIABLE / EXPECTED_FIXED / EXPECTED_VARIABLE のデータブロック。
 */
public class FileDataBlock extends TestDataBlock {

    /** ファイルデータブロックの種別。SETUP/EXPECTED を問わず固定長か可変長かを区別する。 */
    public enum FileType { FIXED, VARIABLE }

    private final FileType fileType;
    /** ディレクティブ（キー → 値）。Excel の行順を保持するため LinkedHashMap を使用する。 */
    private final Map<String, String> directives;
    private final List<RecordLayout> records;

    public FileDataBlock(DataType dataType, String groupId, String identifier,
                         FileType fileType, Map<String, String> directives, List<RecordLayout> records) {
        super(dataType, groupId, identifier);
        this.fileType = fileType;
        this.directives = directives;
        this.records = records;
    }

    public FileType getFileType() {
        return fileType;
    }

    public Map<String, String> getDirectives() {
        return directives;
    }

    public List<RecordLayout> getRecords() {
        return records;
    }
}
