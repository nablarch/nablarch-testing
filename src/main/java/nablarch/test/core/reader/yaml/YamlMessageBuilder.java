package nablarch.test.core.reader.yaml;

import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.file.MockMessages;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.messaging.RequestTestingMessagePool;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static nablarch.test.core.reader.yaml.YamlSection.FIELD_FW_HEADER;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_GROUP_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_RECORDS;
import static nablarch.test.core.reader.yaml.YamlSection.addBinaryFileInterpreter;
import static nablarch.test.core.reader.yaml.YamlSection.castMap;
import static nablarch.test.core.reader.yaml.YamlSection.getList;
import static nablarch.test.core.reader.yaml.YamlSection.objectToString;
import static nablarch.test.core.reader.yaml.YamlSection.toStr;

/**
 * YAML のメッセージ系セクション（{@code messages}／{@code expected_request_*_messages}／
 * {@code response_*_messages}）から、本体の器（{@link MessagePool}）を直接組み立てるビルダ。
 *
 * <p>
 * 本文レコード・データ行・ディレクティブの組み立ては {@link YamlFileBuilder} の処理を再利用する
 * （重複ゼロ）。FW 制御ヘッダ（{@code fw_header:}）は「マップであること」の検証・文字列化を、
 * <b>実際に読み出すメッセージに対してのみ遅延実行</b>する（同一ファイル内の誤記エントリが他エントリの
 * 読み出しを巻き添えにしない挙動）。値の解釈（interpret）は行わず文字列化のみを行う。
 * </p>
 *
 * @author kiyotis
 */
public final class YamlMessageBuilder {

    private final List<TestDataInterpreter> interpreters;

    /**
     * コンストラクタ。
     *
     * @param interpreters インタープリタプロトタイプ（{@code ${binaryFile:}} は basePath 付きで都度先頭に積む）
     */
    public YamlMessageBuilder(List<TestDataInterpreter> interpreters) {
        this.interpreters = interpreters;
    }

    /**
     * メッセージ系セクションから指定 ID の {@link MessagePool} を組み立てる。
     *
     * @param yaml        YAML トップレベル Map
     * @param sectionKey  セクションキー（例: {@code "messages"}）
     * @param id          メッセージ ID
     * @param useFwHeader {@code fw_header:} を使用するか（{@code messages} 経路のみ true。その他は空 Map）
     * @param basePath    インタープリタ用ベースパス
     * @return {@link MessagePool}（実体は {@link RequestTestingMessagePool}）、または存在しない場合 null
     */
    public MessagePool buildMessagePool(Map<String, Object> yaml, String sectionKey, String id,
                                        boolean useFwHeader, String basePath) {
        List<TestDataInterpreter> interps = addBinaryFileInterpreter(basePath, interpreters);
        for (Object entry : getList(yaml, sectionKey)) {
            Map<String, Object> map = castMap(entry);
            if (id.equals(toStr(map.get(FIELD_ID)))) {
                FixedLengthFile file = new FixedLengthFile(id);
                YamlFileBuilder.applyDirectives(file, YamlFileBuilder.mapDirectives(map));
                YamlFileBuilder.buildFragments(file, getList(map, FIELD_RECORDS), true, interps);
                Map<String, String> fwHeader = useFwHeader
                        ? convertFwHeader(map.get(FIELD_FW_HEADER), id)
                        : Collections.<String, String>emptyMap();
                return new RequestTestingMessagePool(file, fwHeader);
            }
        }
        return null;
    }

    /**
     * メッセージ系セクションから指定グループの SendSync 用メッセージリストを組み立てる。
     *
     * @param yaml       YAML トップレベル Map
     * @param sectionKey セクションキー
     * @param groupId    グループ ID（生値で一致比較する）
     * @param basePath   インタープリタ用ベースパス
     * @return {@link RequestTestingMessagePool} リスト、または存在しない場合 null
     */
    public List<RequestTestingMessagePool> buildSendSyncList(Map<String, Object> yaml, String sectionKey,
                                                             String groupId, String basePath) {
        List<TestDataInterpreter> interps = addBinaryFileInterpreter(basePath, interpreters);
        List<RequestTestingMessagePool> result = new ArrayList<RequestTestingMessagePool>();
        for (Object entry : getList(yaml, sectionKey)) {
            Map<String, Object> map = castMap(entry);
            String rawGroupId = toStr(map.get(FIELD_GROUP_ID));
            if (rawGroupId != null && rawGroupId.equals(groupId)) {
                String id = toStr(map.get(FIELD_ID));
                MockMessages file = new MockMessages(id != null ? id : "");
                YamlFileBuilder.applyDirectives(file, YamlFileBuilder.mapDirectives(map));
                YamlFileBuilder.buildFragments(file, getList(map, FIELD_RECORDS), true, interps);
                RequestTestingMessagePool pool =
                        new RequestTestingMessagePool(file, Collections.<String, String>emptyMap());
                if (id != null) {
                    pool.setRequestId(id);
                }
                result.add(pool);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * 生の {@code fw_header} 値を検証・文字列化して {@code Map<String,String>} へ変換する（{@code messages} 経路のみ呼ばれる）。
     *
     * <p>
     * 値は文字列化のみで解釈（interpret）はしない。マップ以外が指定された場合は ID 付きで
     * {@link IllegalStateException} を投げる。
     * </p>
     *
     * @param fwHeaderObj 生の fw_header 値（マップ／その他／null）
     * @param id          メッセージ ID（例外メッセージ用）
     * @return FW 制御ヘッダ Map（省略時・null 時は空 Map）
     */
    private Map<String, String> convertFwHeader(Object fwHeaderObj, String id) {
        if (fwHeaderObj == null) {
            return Collections.emptyMap();
        }
        if (!(fwHeaderObj instanceof Map)) {
            throw new IllegalStateException(
                    "fw_header in message entry id='" + id + "' must be a map, "
                            + "but was: " + fwHeaderObj.getClass().getSimpleName());
        }
        Map<String, String> fwHeader = new LinkedHashMap<String, String>();
        for (Map.Entry<?, ?> kv : ((Map<?, ?>) fwHeaderObj).entrySet()) {
            fwHeader.put(objectToString(kv.getKey()), objectToString(kv.getValue()));
        }
        return fwHeader;
    }
}
