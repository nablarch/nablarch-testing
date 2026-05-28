package nablarch.test.tool.converter.model;

import java.util.List;

/**
 * Excel シート / YAML ファイル 1 枚に相当する。NTF の読み込み単位。
 */
public class TestDataSection {

    private final String name;
    private final List<TestDataBlock> blocks;

    public TestDataSection(String name, List<TestDataBlock> blocks) {
        this.name = name;
        this.blocks = blocks;
    }

    /** シート名 / YAML ファイル名（拡張子なし）。 */
    public String getName() {
        return name;
    }

    public List<TestDataBlock> getBlocks() {
        return blocks;
    }
}
