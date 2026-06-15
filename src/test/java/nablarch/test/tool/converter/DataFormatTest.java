package nablarch.test.tool.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * {@link DataFormat} のテストクラス。
 *
 * <p>識別子（{@code "xls"}／{@code "yaml"}）と列挙値の相互変換、未知識別子の扱いを検証する。</p>
 *
 * @author kiyobot
 */
public class DataFormatTest {

    /**
     * Given: 列挙値。
     * When : getArgument。
     * Then : 規定の識別子を返す。
     */
    @Test
    public void returnsArgumentToken() {
        assertThat(DataFormat.XLS.getArgument(), is("xls"));
        assertThat(DataFormat.YAML.getArgument(), is("yaml"));
    }

    /**
     * Given: 既知の識別子。
     * When : fromArgument。
     * Then : 対応する列挙値を返す。
     */
    @Test
    public void resolvesFromKnownArgument() {
        assertThat(DataFormat.fromArgument("xls"), sameInstance(DataFormat.XLS));
        assertThat(DataFormat.fromArgument("yaml"), sameInstance(DataFormat.YAML));
    }

    /**
     * Given: 未知の識別子。
     * When : fromArgument。
     * Then : IllegalArgumentException。
     */
    @Test
    public void rejectsUnknownArgument() {
        try {
            DataFormat.fromArgument("json");
            fail("should throw");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("unknown data format: json"));
        }
    }

    /**
     * Given: 大文字（厳密一致でない）識別子。
     * When : fromArgument。
     * Then : 受理せず IllegalArgumentException（識別子は小文字厳密一致）。
     */
    @Test
    public void rejectsUppercaseArgument() {
        try {
            DataFormat.fromArgument("XLS");
            fail("should throw");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("unknown data format: XLS"));
        }
    }

    /**
     * Given: 各列挙値。
     * When : getArgument → fromArgument。
     * Then : 元の列挙値へ戻る（往復一致）。
     */
    @Test
    public void roundTripsThroughArgument() {
        for (DataFormat format : DataFormat.values()) {
            assertThat(DataFormat.fromArgument(format.getArgument()), sameInstance(format));
        }
    }
}
