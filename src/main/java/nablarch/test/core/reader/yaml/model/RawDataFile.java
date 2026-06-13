package nablarch.test.core.reader.yaml.model;

import java.util.List;
import java.util.Map;

/**
 * 構造マッピング層が返すファイルデータ（値未加工）。
 *
 * <p>
 * {@code setup_files}／{@code expected_files} の 1 エントリ分を、解釈を施さずに保持する不変オブジェクト。
 * ディレクティブ・レコードレイアウト・データ行をすべて YAML 記述のまま保持する。
 * </p>
 *
 * @author kiyotis
 */
public final class RawDataFile {

    private final String groupId;
    private final String path;
    private final String fileType;
    private final Map<String, String> directives;
    private final List<RawRecordLayout> records;

    /**
     * コンストラクタ。
     *
     * @param groupId    グループ ID（YAML 記述のまま。省略時 {@code null}）
     * @param path       ファイルパス（YAML 記述のまま）
     * @param fileType   ファイル種別（{@code "fixed"} 等。省略時 {@code null}）
     * @param directives ディレクティブ（YAML 順・未加工）
     * @param records    レコードレイアウト群（YAML 順）
     */
    public RawDataFile(String groupId, String path, String fileType,
                       Map<String, String> directives, List<RawRecordLayout> records) {
        this.groupId = groupId;
        this.path = path;
        this.fileType = fileType;
        this.directives = directives;
        this.records = records;
    }

    /** @return グループ ID（省略時 {@code null}） */
    public String getGroupId() {
        return groupId;
    }

    /** @return ファイルパス */
    public String getPath() {
        return path;
    }

    /** @return ファイル種別（省略時 {@code null}） */
    public String getFileType() {
        return fileType;
    }

    /** @return ディレクティブ（YAML 順・未加工） */
    public Map<String, String> getDirectives() {
        return directives;
    }

    /** @return レコードレイアウト群（YAML 順） */
    public List<RawRecordLayout> getRecords() {
        return records;
    }
}
