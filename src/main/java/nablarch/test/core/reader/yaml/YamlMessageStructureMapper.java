package nablarch.test.core.reader.yaml;

import nablarch.test.core.reader.yaml.model.RawMessage;
import nablarch.test.core.reader.yaml.model.RawRecordLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static nablarch.test.core.reader.yaml.YamlSection.FIELD_FW_HEADER;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_GROUP_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_ID;
import static nablarch.test.core.reader.yaml.YamlSection.castMap;
import static nablarch.test.core.reader.yaml.YamlSection.getList;
import static nablarch.test.core.reader.yaml.YamlSection.toStr;

/**
 * YAML のメッセージ系セクション（{@code messages}／{@code expected_request_*_messages}／
 * {@code response_*_messages}）を、値を一切加工せずに生の構造レコード（{@link RawMessage}）へ
 * 写し取る構造マッピング層。
 *
 * <p>
 * 本クラスは <b>構造のみ</b>を扱う。本文レコード・データ行の写し取りは
 * {@link YamlFileStructureMapper} の処理を再利用する（重複ゼロ）。FW 制御ヘッダ（{@code fw_header:}）は
 * 本文とは別に未加工で保持する。「{@code fw_header} がマップであること」は構造上の制約なので本層で検証するが、
 * 「どの経路で {@code fw_header} を使うか（{@code messages} のみ）」は値加工層（{@link YamlValueProcessor}）の責務とする。
 * </p>
 *
 * @author kiyotis
 */
public final class YamlMessageStructureMapper {

    /**
     * メッセージ系セクションを全エントリ分 {@link RawMessage} へ写し取る（ID・グループ絞り込みなし）。
     *
     * @param yaml       YAML トップレベル Map
     * @param sectionKey セクションキー（例: {@code "messages"}）
     * @return 全エントリの {@link RawMessage}（記述順）
     */
    public List<RawMessage> mapMessages(Map<String, Object> yaml, String sectionKey) {
        List<Object> entries = getList(yaml, sectionKey);
        List<RawMessage> result = new ArrayList<RawMessage>();
        for (Object entry : entries) {
            Map<String, Object> map = castMap(entry);
            String groupId = toStr(map.get(FIELD_GROUP_ID));
            String id = toStr(map.get(FIELD_ID));
            Map<String, String> directives = YamlFileStructureMapper.mapDirectives(map);
            // fw_header は生の値（マップとは限らない）のまま保持する。「マップであること」の検証は
            // 実際に読み出すメッセージに対してのみ値加工層が遅延実行する（同一ファイル内の誤記エントリで
            // 他エントリの読み出しが巻き添えにならないようにするため＝旧挙動の維持）。
            Object fwHeader = map.get(FIELD_FW_HEADER);
            List<RawRecordLayout> records = YamlFileStructureMapper.mapRecords(map);
            result.add(new RawMessage(groupId, id, directives, fwHeader, records));
        }
        return result;
    }
}
