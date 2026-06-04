package nablarch.test.tool.converter;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link DataFormat} の単体テスト。
 */
public class DataFormatTest {

    /**
     * [Given] "xls" という文字列
     * [When]  fromArgument() を呼び出す
     * [Then]  DataFormat.XLS が返される
     */
    @Test
    public void fromArgumentXls() {
        assertThat(DataFormat.fromArgument("xls"), is(DataFormat.XLS));
    }

    /**
     * [Given] "yaml" という文字列
     * [When]  fromArgument() を呼び出す
     * [Then]  DataFormat.YAML が返される
     */
    @Test
    public void fromArgumentYaml() {
        assertThat(DataFormat.fromArgument("yaml"), is(DataFormat.YAML));
    }

    /**
     * [Given] 未知の文字列（"csv"）
     * [When]  fromArgument() を呼び出す
     * [Then]  IllegalArgumentException がスローされる
     */
    @Test(expected = IllegalArgumentException.class)
    public void fromArgumentUnknownThrows() {
        DataFormat.fromArgument("csv");
    }

    /**
     * [Given] null
     * [When]  fromArgument() を呼び出す
     * [Then]  IllegalArgumentException がスローされる
     */
    @Test(expected = IllegalArgumentException.class)
    public void fromArgumentNullThrows() {
        DataFormat.fromArgument(null);
    }

    /**
     * [Given] DataFormat.XLS
     * [When]  toArgument() を呼び出す
     * [Then]  "xls" が返される
     */
    @Test
    public void toArgumentXls() {
        assertThat(DataFormat.XLS.toArgument(), is("xls"));
    }

    /**
     * [Given] DataFormat.YAML
     * [When]  toArgument() を呼び出す
     * [Then]  "yaml" が返される
     */
    @Test
    public void toArgumentYaml() {
        assertThat(DataFormat.YAML.toArgument(), is("yaml"));
    }
}
