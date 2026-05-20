package nablarch.test.core.reader.yaml;

import java.util.List;
import java.util.Map;

/**
 * YAML の1エントリを行シーケンスに変換するインタフェース。
 *
 * <p>
 * 各セクション種別（テーブル系・LIST_MAP・ファイル系・メッセージ系）に対して実装を提供する。
 * </p>
 */
interface SectionConverter {

    /**
     * エントリ1件を行シーケンスに変換して {@code out} に追加する。
     *
     * @param entry YAML セクション内の1エントリ（Map）
     * @param out   変換結果を追記する行シーケンス
     */
    void convert(Map<String, Object> entry, List<List<String>> out);
}
