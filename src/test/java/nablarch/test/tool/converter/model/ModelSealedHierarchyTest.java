package nablarch.test.tool.converter.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * 中間モデルの継承・sealed 関係（設計書 §3 クラス図）が崩れていないことを検証する。
 *
 * <p>
 * sealed 階層は変換ツールの Reader/Writer が網羅的に分岐（switch パターン）する前提のため、
 * permits の集合が設計図と一致することを保証する。
 * </p>
 */
public class ModelSealedHierarchyTest {

    @Test
    public void TestDataBlockはsealedで三つの直接サブクラスのみを許可する() {
        // Then
        assertThat(TestDataBlock.class.isSealed(), is(true));
        assertThat(permittedNames(TestDataBlock.class),
                is(Set.of("ColumnRowDataBlock", "FileDataBlock", "MessageDataBlock")));
    }

    @Test
    public void ColumnRowDataBlockはsealedでテーブルとLISTMAPのみを許可する() {
        // Then
        assertThat(ColumnRowDataBlock.class.isSealed(), is(true));
        assertThat(permittedNames(ColumnRowDataBlock.class),
                is(Set.of("TableDataBlock", "ListMapBlock")));
    }

    @Test
    public void 葉のブロックはfinalで継承の終端である() {
        // Then: sealed 階層の末端は final（さらなる派生を禁止）
        assertThat(java.lang.reflect.Modifier.isFinal(TableDataBlock.class.getModifiers()), is(true));
        assertThat(java.lang.reflect.Modifier.isFinal(ListMapBlock.class.getModifiers()), is(true));
        assertThat(java.lang.reflect.Modifier.isFinal(FileDataBlock.class.getModifiers()), is(true));
        assertThat(java.lang.reflect.Modifier.isFinal(MessageDataBlock.class.getModifiers()), is(true));
    }

    @Test
    public void 継承関係が設計図どおりである() {
        // Then
        assertThat(TestDataBlock.class.isAssignableFrom(ColumnRowDataBlock.class), is(true));
        assertThat(ColumnRowDataBlock.class.isAssignableFrom(TableDataBlock.class), is(true));
        assertThat(ColumnRowDataBlock.class.isAssignableFrom(ListMapBlock.class), is(true));
        assertThat(TestDataBlock.class.isAssignableFrom(FileDataBlock.class), is(true));
        assertThat(TestDataBlock.class.isAssignableFrom(MessageDataBlock.class), is(true));
    }

    private static Set<String> permittedNames(Class<?> sealed) {
        return Arrays.stream(sealed.getPermittedSubclasses())
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
    }
}
