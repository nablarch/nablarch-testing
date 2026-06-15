package nablarch.test.tool.converter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1 回の変換の意図を表す不変リクエスト。
 *
 * <p>
 * 変換元・変換先の形式、入出力ディレクトリ、上書き可否、走査時の include／exclude グロブを束ねる。
 * 入口（{@link TestDataConverter#convert(ConversionRequest)}）が本リクエストを解釈して実行する。
 * テストコード・CLI・将来の Maven プラグインはいずれも {@link Builder} で組み立てて渡す。
 * </p>
 *
 * <p>
 * 同一形式（{@code XLS→XLS}／{@code YAML→YAML}）も許容する（4 方向変換の往復検証に用いるため）。
 * </p>
 *
 * @author kiyobot
 */
public final class ConversionRequest {

    /** 変換元形式 */
    private final DataFormat sourceFormat;

    /** 変換先形式 */
    private final DataFormat targetFormat;

    /** 入力ディレクトリ */
    private final Path inputPath;

    /** 出力ディレクトリ */
    private final Path outputPath;

    /** 出力先が既存の場合に上書きするか */
    private final boolean overwrite;

    /** 取り込み対象を絞る glob パターン（空なら全件） */
    private final List<String> includes;

    /** 除外する glob パターン（空なら除外なし） */
    private final List<String> excludes;

    /**
     * {@link Builder} から生成する。
     *
     * @param builder 設定済みビルダ
     */
    private ConversionRequest(Builder builder) {
        this.sourceFormat = builder.sourceFormat;
        this.targetFormat = builder.targetFormat;
        this.inputPath = builder.inputPath;
        this.outputPath = builder.outputPath;
        this.overwrite = builder.overwrite;
        this.includes = Collections.unmodifiableList(new ArrayList<>(builder.includes));
        this.excludes = Collections.unmodifiableList(new ArrayList<>(builder.excludes));
    }

    /** @return 変換元形式 */
    public DataFormat getSourceFormat() {
        return sourceFormat;
    }

    /** @return 変換先形式 */
    public DataFormat getTargetFormat() {
        return targetFormat;
    }

    /** @return 入力ディレクトリ */
    public Path getInputPath() {
        return inputPath;
    }

    /** @return 出力ディレクトリ */
    public Path getOutputPath() {
        return outputPath;
    }

    /** @return 出力先が既存の場合に上書きするか */
    public boolean isOverwrite() {
        return overwrite;
    }

    /** @return 取り込み対象を絞る glob パターン（変更不可・空なら全件） */
    public List<String> getIncludes() {
        return includes;
    }

    /** @return 除外する glob パターン（変更不可・空なら除外なし） */
    public List<String> getExcludes() {
        return excludes;
    }

    /**
     * {@link ConversionRequest} のビルダ。
     *
     * <p>{@code sourceFormat}／{@code targetFormat}／{@code inputPath}／{@code outputPath} は必須。
     * {@code overwrite} の既定値は {@code false}（出力衝突は失敗）。</p>
     */
    public static final class Builder {

        private DataFormat sourceFormat;
        private DataFormat targetFormat;
        private Path inputPath;
        private Path outputPath;
        private boolean overwrite;
        private final List<String> includes = new ArrayList<>();
        private final List<String> excludes = new ArrayList<>();

        /**
         * 変換元形式を設定する。
         *
         * @param sourceFormat 変換元形式
         * @return 自身
         */
        public Builder sourceFormat(DataFormat sourceFormat) {
            this.sourceFormat = sourceFormat;
            return this;
        }

        /**
         * 変換先形式を設定する。
         *
         * @param targetFormat 変換先形式
         * @return 自身
         */
        public Builder targetFormat(DataFormat targetFormat) {
            this.targetFormat = targetFormat;
            return this;
        }

        /**
         * 入力ディレクトリを設定する。
         *
         * @param inputPath 入力ディレクトリ
         * @return 自身
         */
        public Builder inputPath(Path inputPath) {
            this.inputPath = inputPath;
            return this;
        }

        /**
         * 出力ディレクトリを設定する。
         *
         * @param outputPath 出力ディレクトリ
         * @return 自身
         */
        public Builder outputPath(Path outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        /**
         * 上書き可否を設定する。
         *
         * @param overwrite 出力先が既存の場合に上書きするなら {@code true}
         * @return 自身
         */
        public Builder overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return this;
        }

        /**
         * 取り込み対象を絞る glob パターンを追加する。
         *
         * @param pattern glob パターン（入力ディレクトリからの相対パスに対して評価）
         * @return 自身
         */
        public Builder include(String pattern) {
            this.includes.add(pattern);
            return this;
        }

        /**
         * 除外する glob パターンを追加する。
         *
         * @param pattern glob パターン（入力ディレクトリからの相対パスに対して評価）
         * @return 自身
         */
        public Builder exclude(String pattern) {
            this.excludes.add(pattern);
            return this;
        }

        /**
         * リクエストを生成する。
         *
         * @return 不変リクエスト
         * @throws IllegalArgumentException 必須項目が未設定の場合
         */
        public ConversionRequest build() {
            require(sourceFormat, "sourceFormat");
            require(targetFormat, "targetFormat");
            require(inputPath, "inputPath");
            require(outputPath, "outputPath");
            return new ConversionRequest(this);
        }

        /**
         * 必須項目が設定済みかを検証する。
         *
         * @param value 値
         * @param name  項目名
         */
        private static void require(Object value, String name) {
            if (value == null) {
                throw new IllegalArgumentException(name + " is required");
            }
        }
    }
}
