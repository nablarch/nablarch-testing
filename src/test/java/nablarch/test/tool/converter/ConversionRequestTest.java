package nablarch.test.tool.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.Test;

/**
 * {@link ConversionRequest} と {@link ConversionRequest.Builder} のテストクラス。
 *
 * <p>必須項目の検証・既定値・include／exclude の蓄積・不変性・同一形式許容を検証する。</p>
 *
 * @author kiyobot
 */
public class ConversionRequestTest {

    /** 入力ルート */
    private static final Path IN = Paths.get("/in");

    /** 出力ルート */
    private static final Path OUT = Paths.get("/out");

    /**
     * 全必須項目を設定した最小ビルダ。
     *
     * @return ビルダ
     */
    private static ConversionRequest.Builder minimal() {
        return new ConversionRequest.Builder()
                .sourceFormat(DataFormat.XLS)
                .targetFormat(DataFormat.YAML)
                .inputPath(IN)
                .outputPath(OUT);
    }

    /**
     * Given: 全必須項目を設定したビルダ。
     * When : build。
     * Then : 各値を保持し、既定値（overwrite=false・include/exclude 空）を持つ。
     */
    @Test
    public void buildsWithDefaults() {
        ConversionRequest request = minimal().build();

        assertThat(request.getSourceFormat(), is(DataFormat.XLS));
        assertThat(request.getTargetFormat(), is(DataFormat.YAML));
        assertThat(request.getInputPath(), is(IN));
        assertThat(request.getOutputPath(), is(OUT));
        assertThat(request.isOverwrite(), is(false));
        assertThat(request.getIncludes().isEmpty(), is(true));
        assertThat(request.getExcludes().isEmpty(), is(true));
    }

    /**
     * Given: overwrite と include／exclude を設定したビルダ。
     * When : build。
     * Then : 設定どおり蓄積される（記述順）。
     */
    @Test
    public void accumulatesPatternsAndOverwrite() {
        ConversionRequest request = minimal()
                .overwrite(true)
                .include("**/*.xls")
                .include("a/**")
                .exclude("**/_*")
                .build();

        assertThat(request.isOverwrite(), is(true));
        assertThat(request.getIncludes(), is(Arrays.asList("**/*.xls", "a/**")));
        assertThat(request.getExcludes(), is(Arrays.asList("**/_*")));
    }

    /**
     * Given: 同一形式（YAML→YAML）。
     * When : build。
     * Then : 許容される（往復検証用）。
     */
    @Test
    public void allowsSameFormat() {
        ConversionRequest request = minimal()
                .sourceFormat(DataFormat.YAML)
                .targetFormat(DataFormat.YAML)
                .build();

        assertThat(request.getSourceFormat(), is(DataFormat.YAML));
        assertThat(request.getTargetFormat(), is(DataFormat.YAML));
    }

    /**
     * Given: 取得した include リスト。
     * When : 変更を試みる。
     * Then : 変更不可（防御的）。
     */
    @Test(expected = UnsupportedOperationException.class)
    public void includesAreUnmodifiable() {
        minimal().include("x").build().getIncludes().add("y");
    }

    /**
     * Given: 取得した exclude リスト。
     * When : 変更を試みる。
     * Then : 変更不可（防御的）。
     */
    @Test(expected = UnsupportedOperationException.class)
    public void excludesAreUnmodifiable() {
        minimal().exclude("x").build().getExcludes().add("y");
    }

    /**
     * Given: 必須項目が欠けたビルダ。
     * When : build。
     * Then : 欠けた項目名つきで IllegalArgumentException。
     */
    @Test
    public void rejectsMissingRequired() {
        assertRequired(new ConversionRequest.Builder()
                .targetFormat(DataFormat.YAML).inputPath(IN).outputPath(OUT), "sourceFormat");
        assertRequired(new ConversionRequest.Builder()
                .sourceFormat(DataFormat.XLS).inputPath(IN).outputPath(OUT), "targetFormat");
        assertRequired(new ConversionRequest.Builder()
                .sourceFormat(DataFormat.XLS).targetFormat(DataFormat.YAML).outputPath(OUT), "inputPath");
        assertRequired(new ConversionRequest.Builder()
                .sourceFormat(DataFormat.XLS).targetFormat(DataFormat.YAML).inputPath(IN), "outputPath");
    }

    /**
     * ビルダの build が指定項目名で IllegalArgumentException を投げることを表明する。
     *
     * @param builder      検証対象ビルダ
     * @param expectedName 欠落していると報告されるべき項目名
     */
    private static void assertRequired(ConversionRequest.Builder builder, String expectedName) {
        try {
            builder.build();
            fail("should throw for missing " + expectedName);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(expectedName + " is required"));
        }
    }
}
