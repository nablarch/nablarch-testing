package nablarch.test.tool.converter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * テストデータ変換の意図を表す構造化型。
 *
 * <p>{@link TestDataConverter#convert(ConversionRequest)} の引数として使用する。</p>
 */
public final class ConversionRequest {

    /** 変換元形式（"xls" または "yaml"） */
    private final String sourceFormat;

    /** 変換先形式（"xls" または "yaml"） */
    private final String targetFormat;

    /** 変換元ファイル/ディレクトリのパス */
    private final Path inputPath;

    /** 変換先ディレクトリのパス */
    private final Path outputPath;

    /** 既存ファイルを上書きするか */
    private final boolean overwrite;

    /** 変換後にソースを削除するか */
    private final boolean deleteSource;

    /** 変換先が .xls 形式（true: .xls, false: .xlsx） */
    private final boolean xlsFormat;

    /** 変換前に YAML を検証するか（--from yaml のみ有効） */
    private final boolean validateOnConvert;

    /** インクルードパターン */
    private final List<String> includes;

    /** エクスクルードパターン */
    private final List<String> excludes;

    private ConversionRequest(Builder builder) {
        this.sourceFormat = builder.sourceFormat;
        this.targetFormat = builder.targetFormat;
        this.inputPath = builder.inputPath;
        this.outputPath = builder.outputPath;
        this.overwrite = builder.overwrite;
        this.deleteSource = builder.deleteSource;
        this.xlsFormat = builder.xlsFormat;
        this.validateOnConvert = builder.validateOnConvert;
        this.includes = Collections.unmodifiableList(new ArrayList<>(builder.includes));
        this.excludes = Collections.unmodifiableList(new ArrayList<>(builder.excludes));
    }

    public String getSourceFormat() { return sourceFormat; }
    public String getTargetFormat() { return targetFormat; }
    public Path getInputPath() { return inputPath; }
    public Path getOutputPath() { return outputPath; }
    public boolean isOverwrite() { return overwrite; }
    public boolean isDeleteSource() { return deleteSource; }
    public boolean isXlsFormat() { return xlsFormat; }
    public boolean isValidateOnConvert() { return validateOnConvert; }
    public List<String> getIncludes() { return includes; }
    public List<String> getExcludes() { return excludes; }

    /** {@link ConversionRequest} のビルダー */
    public static final class Builder {
        private String sourceFormat;
        private String targetFormat;
        private Path inputPath;
        private Path outputPath;
        private boolean overwrite = false;
        private boolean deleteSource = false;
        private boolean xlsFormat = false;
        private boolean validateOnConvert = false;
        private final List<String> includes = new ArrayList<>();
        private final List<String> excludes = new ArrayList<>();

        public Builder sourceFormat(String sourceFormat) {
            this.sourceFormat = sourceFormat;
            return this;
        }

        public Builder targetFormat(String targetFormat) {
            this.targetFormat = targetFormat;
            return this;
        }

        public Builder inputPath(Path inputPath) {
            this.inputPath = inputPath;
            return this;
        }

        public Builder outputPath(Path outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public Builder overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return this;
        }

        public Builder deleteSource(boolean deleteSource) {
            this.deleteSource = deleteSource;
            return this;
        }

        public Builder xlsFormat(boolean xlsFormat) {
            this.xlsFormat = xlsFormat;
            return this;
        }

        public Builder validateOnConvert(boolean validateOnConvert) {
            this.validateOnConvert = validateOnConvert;
            return this;
        }

        public Builder include(String pattern) {
            this.includes.add(pattern);
            return this;
        }

        public Builder exclude(String pattern) {
            this.excludes.add(pattern);
            return this;
        }

        public ConversionRequest build() {
            return new ConversionRequest(this);
        }
    }
}
