package nablarch.test.tool.converter;

import nablarch.test.tool.converter.model.TestDataContainer;

/**
 * 形式（Excel／YAML）を読み込み、形式中立な中間モデル（{@link TestDataContainer}）へ写す
 * IN 側のインタフェース。
 *
 * <p>
 * 各実装は本体の読み込み機構を再利用して 1 リソース（Excel は 1 シート、YAML は 1 ファイル）を
 * 読み、IN 値を記法のまま（未加工）保持した中間モデルを組み立てる。複数リソース（ブック内の
 * 全シート・ディレクトリ内の全ファイル）の走査は入口・パス解決層の責務であり、本インタフェースは
 * 解決済みの 1 リソースを受け取る。
 * </p>
 *
 * <p>依存方向は変換ツール → 本体の一方向で、本体は中間モデルを知らない。</p>
 *
 * @author kiyobot
 */
public interface TestDataFormatReader {

    /**
     * 1 リソースを読み込み、中間モデルへ写す。
     *
     * @param basePath     読み込み元のディレクトリ
     * @param resourceName リソース名（Excel は {@code "ブック名/シート名"}、YAML はファイル名）
     * @return 中間モデル（テストクラス 1 つ分。読み込み単位を 1 つ持つ）
     */
    TestDataContainer read(String basePath, String resourceName);
}
