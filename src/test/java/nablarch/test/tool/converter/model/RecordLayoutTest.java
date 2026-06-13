package nablarch.test.tool.converter.model;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link RecordLayout} のテスト。
 *
 * <p>レコード種別・フィールド定義・データ行を、加工・補完せず保持することを検証する。</p>
 */
public class RecordLayoutTest {

    @Test
    public void レコード種別とフィールドと行を保持する() {
        // Given
        List<FieldDef> fields = List.of(new FieldDef("id", "数値", "5"));
        List<List<String>> rows = List.of(List.of("00001"));

        // When
        RecordLayout sut = new RecordLayout("data", fields, rows);

        // Then: 参照そのまま保持（防御コピーしない＝読み取り専用契約）
        assertThat(sut.getRecordType(), is("data"));
        assertThat(sut.getFields(), is(sameInstance(fields)));
        assertThat(sut.getRows(), is(sameInstance(rows)));
    }

    @Test
    public void レコード種別省略をnullで保持する() {
        // Given: record_type 省略
        // When
        RecordLayout sut = new RecordLayout(null, List.of(), List.of());

        // Then: デフォルト補完せず null のまま
        assertThat(sut.getRecordType(), is(nullValue()));
        assertThat(sut.getFields().isEmpty(), is(true));
        assertThat(sut.getRows().isEmpty(), is(true));
    }
}
