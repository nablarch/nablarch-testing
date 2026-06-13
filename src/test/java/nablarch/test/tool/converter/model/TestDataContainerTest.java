package nablarch.test.tool.converter.model;

import nablarch.test.core.reader.DataType;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link TestDataContainer} と {@link TestDataSection} のテスト。
 *
 * <p>テストクラス 1 つ分（ブック／ディレクトリ）と読み込み単位（シート／ファイル）の階層を保持することを検証する。</p>
 */
public class TestDataContainerTest {

    @Test
    public void コンテナは名前とセクション群を保持する() {
        // Given
        TestDataSection section = new TestDataSection("sheet1", List.of());
        List<TestDataSection> sections = List.of(section);

        // When
        TestDataContainer sut = new TestDataContainer("BookA", sections);

        // Then
        assertThat(sut.getName(), is("BookA"));
        assertThat(sut.getSections(), is(sameInstance(sections)));
    }

    @Test
    public void セクションは名前とブロック群を保持する() {
        // Given: セクション配下に複数データ種別のブロックが並ぶ
        TableDataBlock table = new TableDataBlock(
                DataType.SETUP_TABLE_DATA, "", "emp", List.of("id"), List.of(List.of("1")));
        List<TestDataBlock> blocks = List.of(table);

        // When
        TestDataSection sut = new TestDataSection("sheet1", blocks);

        // Then
        assertThat(sut.getName(), is("sheet1"));
        assertThat(sut.getBlocks(), is(sameInstance(blocks)));
        assertThat(sut.getBlocks().get(0), is(sameInstance(table)));
    }
}
