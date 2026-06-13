package nablarch.test.core.reader.yaml.model;

import nablarch.core.util.annotation.Published;

import java.util.List;
import java.util.Map;

/**
 * 構造マッピング層が返すメッセージデータ（値未加工）。
 *
 * <p>
 * {@code messages}／{@code expected_request_*_messages}／{@code response_*_messages} の 1 エントリ分を、
 * 解釈を施さずに保持する不変オブジェクト（フィールドは final。ただし getter が返すコレクションは防御的コピーせず公開するため、呼び出し側は読み取り専用として扱うこと）。メッセージ本文は固定長レコード（{@link RawRecordLayout}）で、
 * FW 制御ヘッダ（{@code fw_header:} マップ）は本文とは別に未加工で保持する。
 * </p>
 *
 * <p>
 * {@code fw_header} を実際に使用するか（{@code messages} 経路のみ使用し、その他は空 Map とする）、
 * および「マップであること」の検証・文字列への変換は、値加工層が <b>実際に読み出すメッセージに対してのみ</b>
 * 遅延実行する（同一ファイル内に誤記エントリがあっても、読み出さないエントリでは例外にならない＝旧挙動）。
 * そのため本オブジェクトは {@code fw_header} を <b>YAML ロード時の生の値（{@link Object}）</b>のまま保持する
 * （マップ／リスト／null いずれもありうる）。
 * </p>
 *
 * @author kiyotis
 */
@Published(tag = "architect")
public final class RawMessage {

    private final String groupId;
    private final String id;
    private final Map<String, String> directives;
    private final Object fwHeader;
    private final List<RawRecordLayout> records;

    /**
     * コンストラクタ。
     *
     * @param groupId    グループ ID（SendSync 用。省略時 {@code null}）
     * @param id         メッセージ ID（YAML 記述のまま）
     * @param directives ディレクティブ（YAML 順・未加工）
     * @param fwHeader   FW 制御ヘッダの生の値（マップとは限らない。無い場合は {@code null}）
     * @param records    本文レコードレイアウト群（YAML 順・FW_HEADER もスキップせず保持）
     */
    public RawMessage(String groupId, String id, Map<String, String> directives,
                      Object fwHeader, List<RawRecordLayout> records) {
        this.groupId = groupId;
        this.id = id;
        this.directives = directives;
        this.fwHeader = fwHeader;
        this.records = records;
    }

    /** @return グループ ID（SendSync 用。省略時 {@code null}） */
    public String getGroupId() {
        return groupId;
    }

    /** @return メッセージ ID */
    public String getId() {
        return id;
    }

    /** @return ディレクティブ（YAML 順・未加工） */
    public Map<String, String> getDirectives() {
        return directives;
    }

    /** @return FW 制御ヘッダの生の値（マップとは限らない。無い場合は {@code null}）。検証・変換は値加工層が遅延実行する */
    public Object getFwHeader() {
        return fwHeader;
    }

    /** @return 本文レコードレイアウト群（YAML 順・FW_HEADER 保持） */
    public List<RawRecordLayout> getRecords() {
        return records;
    }
}
