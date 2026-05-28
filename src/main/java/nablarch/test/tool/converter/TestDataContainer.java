package nablarch.test.tool.converter;

import java.util.List;

/**
 * Excel ブック / YAML ディレクトリに相当するコンテナ。テストクラスと 1 対 1 に対応する。
 */
public class TestDataContainer {

    private final String name;
    private final List<TestDataSection> sections;

    public TestDataContainer(String name, List<TestDataSection> sections) {
        this.name = name;
        this.sections = sections;
    }

    /** ブック名 / ディレクトリ名（拡張子なし）。 */
    public String getName() {
        return name;
    }

    public List<TestDataSection> getSections() {
        return sections;
    }
}
