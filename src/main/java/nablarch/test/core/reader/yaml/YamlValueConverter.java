package nablarch.test.core.reader.yaml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * YAML から取得した値を行シーケンス用の文字列に変換するユーティリティ。
 *
 * <p>
 * 変換ルール（RS-03〜RS-06 参照: {@code docs/ntf-impl-spec-list.md}）:
 * <ul>
 *   <li>キーが省略されている（{@code isMissing=true}）→ {@code ""}</li>
 *   <li>YAML ネイティブ {@code null} → 文字列 {@code "null"}</li>
 *   <li>YAML ネイティブ boolean / integer / float → {@link String#valueOf(Object)} で文字列化</li>
 * </ul>
 * </p>
 */
class YamlValueConverter {

    private YamlValueConverter() {
    }

    /**
     * YAML 値を文字列セルに変換する。
     *
     * @param value     YAML 値（null / Boolean / Integer / Long / Double / String）
     * @param isMissing キーが存在しない（省略）場合は {@code true} → {@code ""} を返す（RS-06）
     * @return 変換後の文字列
     */
    static String toCell(Object value, boolean isMissing) {
        if (isMissing) {
            return "";  // RS-06: 省略キーは空文字
        }
        if (value == null) {
            return "null";  // RS-03: YAML ネイティブ null → "null"
        }
        return String.valueOf(value);  // RS-04/RS-05: boolean/integer/float → 数字文字列
    }

    /**
     * オブジェクトを {@link Map} として取得する。
     * {@code Map} でない場合は空の {@code LinkedHashMap} を返す。
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return new LinkedHashMap<String, Object>();
    }

    /**
     * オブジェクトを {@code List<Map<String, Object>>} として取得する。
     * {@code List} でない場合は空リストを返す。
     */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> asMapList(Object obj) {
        if (obj instanceof List) {
            return (List<Map<String, Object>>) obj;
        }
        return Collections.emptyList();
    }

    /**
     * オブジェクトを {@code List<Object>} として取得する。
     * {@code List} でない場合は空リストを返す。
     */
    @SuppressWarnings("unchecked")
    static List<Object> asList(Object obj) {
        if (obj instanceof List) {
            return (List<Object>) obj;
        }
        return Collections.emptyList();
    }

    /**
     * オブジェクトを文字列に変換する。{@code null} の場合は {@code null} を返す。
     */
    static String asString(Object obj) {
        if (obj == null) {
            return null;
        }
        return String.valueOf(obj);
    }

    /**
     * 要素1件からなる行を生成する。
     *
     * @param value 行の唯一の要素
     * @return 要素1件の行リスト
     */
    static List<String> singletonRow(String value) {
        List<String> row = new ArrayList<String>(1);
        row.add(value);
        return row;
    }

    /**
     * 全行の全キーを挿入順で収集する（union）。
     *
     * @param rows 行のリスト
     * @return 全行の全キーの union
     */
    static Set<String> collectAllKeys(List<Map<String, Object>> rows) {
        Set<String> keys = new LinkedHashSet<String>();
        for (Map<String, Object> row : rows) {
            keys.addAll(row.keySet());
        }
        return keys;
    }
}
