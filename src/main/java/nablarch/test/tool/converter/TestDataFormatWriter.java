package nablarch.test.tool.converter;

import nablarch.test.tool.converter.model.TestDataContainer;

/**
 * 形式中立な中間モデル（{@link TestDataContainer}）を形式（Excel／YAML）へ書き出す OUT 側のインタフェース。
 *
 * <p>
 * {@link TestDataFormatReader} と対称をなす。各実装は中間モデルを各形式の記法どおりに直列化して
 * 出力する（YAML は 1 セクション＝1 ファイル、Excel は 1 セクション＝1 シート）。出力先ディレクトリのみ
 * 受け取り、リソース名（ファイル名・シート名）は中間モデルのセクション名から決まる。複数リソースの
 * 配置やディレクトリ走査は入口・パス解決層の責務で、本インタフェースは解決済みの出力先を受け取る。
 * </p>
 *
 * <p>依存方向は変換ツール → 本体の一方向で、本体は中間モデルを知らない。</p>
 *
 * @author kiyobot
 */
public interface TestDataFormatWriter {

    /**
     * 中間モデルを形式へ書き出す。
     *
     * @param container 中間モデル（テストクラス 1 つ分）
     * @param basePath  書き出し先のディレクトリ
     */
    void write(TestDataContainer container, String basePath);
}
