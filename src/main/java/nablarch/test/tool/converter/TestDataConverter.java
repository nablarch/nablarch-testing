package nablarch.test.tool.converter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import nablarch.test.core.reader.PoiXlsReader;
import nablarch.test.tool.converter.model.TestDataContainer;
import nablarch.test.tool.converter.model.TestDataSection;
import nablarch.test.tool.converter.xls.XlsFormatReader;
import nablarch.test.tool.converter.xls.XlsFormatWriter;
import nablarch.test.tool.converter.yaml.YamlFormatReader;
import nablarch.test.tool.converter.yaml.YamlFormatWriter;

/**
 * テストデータ変換ツールの入口。
 *
 * <p>
 * 変換元・変換先の形式と入出力ディレクトリを受け取り、入力ディレクトリ配下の各リソースを
 * 形式中立な中間モデル（{@link TestDataContainer}）経由で変換先形式へ書き出す。Excel↔YAML の
 * 双方向に加え、同一形式変換（往復検証用）も扱う 4 方向対応。
 * </p>
 *
 * <p>
 * 本体テストコードから直接呼び出して「Excel を実行時に一時 YAML へ変換」する用途を主目的とする
 * （CLI・Maven プラグインはリポジトリ分割後に整備）。リソースの粒度差（Excel は 1 ブックに複数シート、
 * YAML は 1 ディレクトリに複数ファイル）は本クラスが吸収し、Reader へは解決済みの 1 リソースを渡す。
 * </p>
 *
 * @author kiyobot
 */
public final class TestDataConverter {

    /** YAML ファイルの拡張子（小文字） */
    private static final String YAML_EXTENSION = ".yaml";

    /** Excel ブックの拡張子（小文字） */
    private static final String XLSX_EXTENSION = ".xlsx";

    /** ユーティリティクラスにつきインスタンス化不可。 */
    private TestDataConverter() {
        throw new AssertionError("TestDataConverter は static 専用です");
    }

    /**
     * 変換元・変換先の形式と入出力ディレクトリだけを指定する簡易入口。
     *
     * <p>上書きは行わず、include／exclude による絞り込みもしない既定リクエストで変換する。</p>
     *
     * @param from   変換元形式
     * @param to     変換先形式
     * @param input  入力ディレクトリ
     * @param output 出力ディレクトリ
     * @return 変換したコンテナ（テストクラス相当）の件数
     */
    public static int convert(DataFormat from, DataFormat to, Path input, Path output) {
        return convert(new ConversionRequest.Builder()
                .sourceFormat(from)
                .targetFormat(to)
                .inputPath(input)
                .outputPath(output)
                .build());
    }

    /**
     * 変換リクエストを解釈して変換を実行する共通入口。
     *
     * @param request 変換リクエスト
     * @return 変換したコンテナ（テストクラス相当）の件数
     * @throws ConverterException 入力ディレクトリが存在しない／上書き禁止下で出力が衝突した場合
     */
    public static int convert(ConversionRequest request) {
        List<Path> targets = findTargets(request);
        TestDataFormatWriter writer = writerFor(request.getTargetFormat());

        int converted = 0;
        for (Path target : targets) {
            TestDataContainer container = read(request.getSourceFormat(), target);
            Path outputBase = resolveOutputBase(request, target);
            checkOverwrite(request, container, outputBase);
            writer.write(container, outputBase.toString());
            converted++;
        }
        return converted;
    }

    /**
     * 変換元形式に応じて変換対象（Excel ブック／YAML ディレクトリ）を列挙する。
     *
     * @param request 変換リクエスト
     * @return 変換対象パスのリスト
     */
    private static List<Path> findTargets(ConversionRequest request) {
        if (request.getSourceFormat() == DataFormat.XLS) {
            return ConverterFileFilter.findXlsFiles(request.getInputPath(), request.getIncludes(), request.getExcludes());
        }
        return ConverterFileFilter.findYamlDirs(request.getInputPath(), request.getIncludes(), request.getExcludes());
    }

    /**
     * 1 つの変換対象を読み込み、複数リソースを 1 コンテナへ集約する。
     *
     * @param sourceFormat 変換元形式
     * @param target       変換対象（Excel ブックファイル／YAML ディレクトリ）
     * @return 集約済みコンテナ
     */
    private static TestDataContainer read(DataFormat sourceFormat, Path target) {
        return sourceFormat == DataFormat.XLS ? readBook(target) : readYamlDir(target);
    }

    /**
     * Excel ブック（複数シート）を読み込み、各シートを 1 コンテナのセクションへ集約する。
     *
     * <p>本体 {@link PoiXlsReader#getSheetNames(java.io.File)} はシート名を順不同で返すため、
     * 出力の再現性を担保するためシート名を辞書順にソートして処理する。</p>
     *
     * @param bookFile Excel ブックファイル
     * @return ブック 1 つ分のコンテナ
     */
    private static TestDataContainer readBook(Path bookFile) {
        XlsFormatReader reader = new XlsFormatReader();
        String basePath = parentOf(bookFile);
        String bookName = ConverterPathResolver.stripExtension(bookFile.getFileName().toString());

        List<String> sheetNames = new ArrayList<>(PoiXlsReader.getSheetNames(bookFile.toFile()));
        sheetNames.sort(null);

        List<TestDataSection> sections = new ArrayList<>();
        for (String sheetName : sheetNames) {
            TestDataContainer single = reader.read(basePath, bookName + "/" + sheetName);
            sections.addAll(single.getSections());
        }
        return new TestDataContainer(bookName, sections);
    }

    /**
     * YAML ディレクトリ（複数ファイル）を読み込み、各ファイルを 1 コンテナのセクションへ集約する。
     *
     * @param yamlDir YAML コンテナディレクトリ
     * @return ディレクトリ 1 つ分のコンテナ
     */
    private static TestDataContainer readYamlDir(Path yamlDir) {
        YamlFormatReader reader = new YamlFormatReader();
        String basePath = yamlDir.toString();
        String dirName = yamlDir.getFileName().toString();

        List<TestDataSection> sections = new ArrayList<>();
        for (Path yamlFile : listYamlFiles(yamlDir)) {
            String resourceName = ConverterPathResolver.stripExtension(yamlFile.getFileName().toString());
            TestDataContainer single = reader.read(basePath, resourceName);
            sections.addAll(single.getSections());
        }
        return new TestDataContainer(dirName, sections);
    }

    /**
     * ディレクトリ直下の YAML ファイルを辞書順で列挙する。
     *
     * @param yamlDir YAML コンテナディレクトリ
     * @return YAML ファイルのリスト（ファイル名辞書順）
     */
    private static List<Path> listYamlFiles(Path yamlDir) {
        TreeMap<String, Path> byName = new TreeMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(yamlDir)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)
                        && entry.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(YAML_EXTENSION)) {
                    byName.put(entry.getFileName().toString(), entry);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("failed to list YAML files: " + yamlDir, e);
        }
        return new ArrayList<>(byName.values());
    }

    /**
     * 変換先形式に応じた Writer を生成する。
     *
     * @param targetFormat 変換先形式
     * @return Writer
     */
    private static TestDataFormatWriter writerFor(DataFormat targetFormat) {
        return targetFormat == DataFormat.XLS ? new XlsFormatWriter() : new YamlFormatWriter();
    }

    /**
     * 変換対象に対応する Writer 用の出力先 basePath を算出する。
     *
     * @param request 変換リクエスト
     * @param target  変換対象
     * @return 出力先ディレクトリ
     */
    private static Path resolveOutputBase(ConversionRequest request, Path target) {
        if (request.getTargetFormat() == DataFormat.XLS) {
            return ConverterPathResolver.outputBaseForXls(request.getInputPath(), target, request.getOutputPath());
        }
        return ConverterPathResolver.outputBaseForYaml(request.getInputPath(), target, request.getOutputPath());
    }

    /**
     * 上書き禁止時に出力衝突がないことを検証する。
     *
     * <p>Excel 出力は {@code <basePath>/<コンテナ名>.xlsx}、YAML 出力は {@code <basePath>/<セクション名>.yaml}
     * を出力先とする。いずれかが既存で {@code overwrite=false} なら例外。</p>
     *
     * @param request    変換リクエスト
     * @param container  出力するコンテナ
     * @param outputBase 出力先ディレクトリ
     * @throws ConverterException 上書き禁止下で出力が衝突した場合
     */
    private static void checkOverwrite(ConversionRequest request, TestDataContainer container, Path outputBase) {
        if (request.isOverwrite()) {
            return;
        }
        for (Path output : outputPaths(request.getTargetFormat(), container, outputBase)) {
            if (Files.exists(output)) {
                throw new ConverterException("output already exists (overwrite=false): " + output);
            }
        }
    }

    /**
     * コンテナが書き出す出力ファイルパスを列挙する。
     *
     * @param targetFormat 変換先形式
     * @param container    出力するコンテナ
     * @param outputBase   出力先ディレクトリ
     * @return 出力ファイルパスのリスト
     */
    private static List<Path> outputPaths(DataFormat targetFormat, TestDataContainer container, Path outputBase) {
        List<Path> paths = new ArrayList<>();
        if (targetFormat == DataFormat.XLS) {
            paths.add(outputBase.resolve(container.getName() + XLSX_EXTENSION));
        } else {
            for (TestDataSection section : container.getSections()) {
                paths.add(outputBase.resolve(section.getName() + YAML_EXTENSION));
            }
        }
        return paths;
    }

    /**
     * ファイルの親ディレクトリパス文字列を返す（親が無ければカレント）。
     *
     * @param file ファイル
     * @return 親ディレクトリパス文字列
     */
    private static String parentOf(Path file) {
        Path parent = file.getParent();
        return parent == null ? "." : parent.toString();
    }

}
