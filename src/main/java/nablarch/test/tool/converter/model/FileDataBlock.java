package nablarch.test.tool.converter.model;

import nablarch.test.core.reader.DataType;

import java.util.List;
import java.util.Map;

/**
 * 固定長／可変長ファイルのデータブロック。
 *
 * <p>
 * データ種別は {@link DataType#SETUP_FIXED}／{@link DataType#EXPECTED_FIXED}／
 * {@link DataType#SETUP_VARIABLE}／{@link DataType#EXPECTED_VARIABLE} のいずれか。
 * 識別子はファイルパスを保持する。固定長か可変長かは SETUP／EXPECTED を問わず {@link FileType} で区別する。
 * ディレクティブとレコードレイアウト群（FW_HEADER レコードもスキップせず保持）を記述順で持つ。
 * </p>
 *
 * <p>getter が返すコレクションは防御的コピーせず保持参照を返すため、呼び出し側は読み取り専用として扱うこと。</p>
 *
 * @author kiyotis
 */
public final class FileDataBlock extends TestDataBlock {

    /** ファイルデータブロックの種別。SETUP／EXPECTED を問わず固定長か可変長かを区別する。 */
    public enum FileType { FIXED, VARIABLE }

    private final FileType fileType;
    private final Map<String, String> directives;
    private final List<RecordLayout> records;

    /**
     * コンストラクタ。
     *
     * @param dataType   データ種別
     * @param groupId    グループ ID（省略時は空文字）
     * @param identifier ファイルパス
     * @param fileType   固定長／可変長の区別
     * @param directives ディレクティブ（キー → 値。記述順を保つため挿入順を維持する Map を渡すこと）
     * @param records    レコードレイアウト群（記述順。FW_HEADER もスキップせず保持）
     */
    public FileDataBlock(DataType dataType, String groupId, String identifier,
                         FileType fileType, Map<String, String> directives, List<RecordLayout> records) {
        super(dataType, groupId, identifier);
        this.fileType = fileType;
        this.directives = directives;
        this.records = records;
    }

    /** @return 固定長／可変長の区別 */
    public FileType getFileType() {
        return fileType;
    }

    /** @return ディレクティブ（記述順） */
    public Map<String, String> getDirectives() {
        return directives;
    }

    /** @return レコードレイアウト群（記述順） */
    public List<RecordLayout> getRecords() {
        return records;
    }
}
