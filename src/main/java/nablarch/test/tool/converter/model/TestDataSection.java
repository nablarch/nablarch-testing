package nablarch.test.tool.converter.model;

import java.util.List;

/**
 * NTF の読み込み単位 1 つ分を表す中間モデル。
 *
 * <p>
 * Excel では 1 シート、YAML では 1 ファイルに相当する。配下に同一読み込み単位内の
 * データブロック（{@link TestDataBlock}）を記述順で保持する。
 * </p>
 *
 * <p>getter が返すコレクションは防御的コピーせず保持参照を返すため、呼び出し側は読み取り専用として扱うこと。</p>
 *
 * @author kiyotis
 */
public final class TestDataSection {

    private final String name;
    private final List<TestDataBlock> blocks;

    /**
     * コンストラクタ。
     *
     * @param name   シート名／ファイル名（拡張子なし）
     * @param blocks データブロックのリスト（記述順）
     */
    public TestDataSection(String name, List<TestDataBlock> blocks) {
        this.name = name;
        this.blocks = blocks;
    }

    /** @return シート名／ファイル名（拡張子なし） */
    public String getName() {
        return name;
    }

    /** @return データブロックのリスト（記述順） */
    public List<TestDataBlock> getBlocks() {
        return blocks;
    }
}
