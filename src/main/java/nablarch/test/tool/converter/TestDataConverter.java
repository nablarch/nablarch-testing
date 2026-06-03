package nablarch.test.tool.converter;

import nablarch.test.tool.converter.model.TestDataContainer;
import nablarch.test.tool.converter.model.TestDataSection;
import nablarch.test.tool.converter.xls.XlsFormatReader;
import nablarch.test.tool.converter.xls.XlsFormatWriter;
import nablarch.test.tool.converter.yaml.ValidationError;
import nablarch.test.tool.converter.yaml.YamlFormatReader;
import nablarch.test.tool.converter.yaml.YamlFormatWriter;
import nablarch.test.tool.converter.yaml.YamlTestDataValidator;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * テストデータ変換ツールのエントリポイント。
 *
 * <p>使用方法: TestDataConverter --from &lt;形式&gt; --to &lt;形式&gt; [options] &lt;入力パス&gt; &lt;出力パス&gt;</p>
 */
public class TestDataConverter {

    public static void main(String[] args) {
        run(args);
    }

    /**
     * 変換処理を実行する。テストからはこのメソッドを直接呼び出して終了コードを検証する。
     *
     * @param args コマンドライン引数
     * @return 終了コード（0: 正常, 1: 変換エラーあり, 2: 引数エラー）
     */
    public static int run(String[] args) {
        Options opts = parseArgs(args);
        if (opts == null) {
            System.err.println("Usage: TestDataConverter --from <xls|yaml> --to <xls|yaml> [--overwrite] [--delete-source] [--include <pattern>]... [--exclude <pattern>]... <inputPath> <outputPath>");
            System.err.println("       TestDataConverter --validate <inputPath>");
            return 2;
        }

        // --validate モード
        if (opts.validateOnly) {
            if (opts.from != null) {
                System.err.println("--validate cannot be combined with --from/--to.");
                return 2;
            }
            return runValidateOnly(opts.inputPath);
        }

        if (opts.from == null || opts.to == null) {
            System.err.println("Usage: TestDataConverter --from <xls|yaml> --to <xls|yaml> [--overwrite] [--delete-source] [--include <pattern>]... [--exclude <pattern>]... <inputPath> <outputPath>");
            return 2;
        }

        if (opts.from.equals(opts.to)) {
            System.err.println("--from and --to must be different formats.");
            return 2;
        }

        if (!opts.from.equals("xls") && !opts.from.equals("yaml")) {
            System.err.println("Invalid --from value: " + opts.from + ". Must be 'xls' or 'yaml'.");
            System.err.println("Usage: TestDataConverter --from <xls|yaml> --to <xls|yaml> [--overwrite] [--delete-source] [--include <pattern>]... [--exclude <pattern>]... <inputPath> <outputPath>");
            return 2;
        }
        if (!opts.to.equals("xls") && !opts.to.equals("yaml")) {
            System.err.println("Invalid --to value: " + opts.to + ". Must be 'xls' or 'yaml'.");
            System.err.println("Usage: TestDataConverter --from <xls|yaml> --to <xls|yaml> [--overwrite] [--delete-source] [--include <pattern>]... [--exclude <pattern>]... <inputPath> <outputPath>");
            return 2;
        }

        if (opts.xlsFormat && !opts.to.equals("xls")) {
            System.err.println("--xls option is only valid with --to xls.");
            System.err.println("Usage: TestDataConverter --from <xls|yaml> --to <xls|yaml> [--overwrite] [--delete-source] [--xls] [--include <pattern>]... [--exclude <pattern>]... <inputPath> <outputPath>");
            return 2;
        }

        // --validate-on-convert は --from yaml のときのみ有効
        if (opts.validateOnConvert && !opts.from.equals("yaml")) {
            System.err.println("--validate-on-convert is only valid with --from yaml.");
            return 2;
        }

        XlsFormatReader xlsReader = opts.from.equals("xls") ? new XlsFormatReader() : null;
        TestDataFormatReader reader;
        TestDataFormatWriter writer;
        if (opts.from.equals("xls")) {
            reader = xlsReader;
            writer = new YamlFormatWriter();
        } else {
            reader = new YamlFormatReader();
            writer = new XlsFormatWriter(opts.xlsFormat);
        }

        int[] skipCount = {0};
        List<Path> targets;
        try {
            if (opts.from.equals("xls")) {
                targets = ConverterFileFilter.findXlsFiles(opts.inputPath, opts.includes, opts.excludes, skipCount);
            } else {
                targets = ConverterFileFilter.findYamlDirs(opts.inputPath, opts.includes, opts.excludes, skipCount);
            }
        } catch (ConverterException e) {
            System.err.println("ERROR: " + e.getMessage());
            return 1;
        }

        int errorCount = 0;
        int successCount = 0;
        int totalCommentLines = 0;
        int commentLineFiles = 0;

        YamlTestDataValidator validator = opts.validateOnConvert ? new YamlTestDataValidator() : null;

        for (Path target : targets) {
            try {
                // --validate-on-convert: 変換前に YAML を検証し、エラーがあればスキップ
                if (validator != null) {
                    List<ValidationError> validationErrors = validator.validate(target);
                    if (!validationErrors.isEmpty()) {
                        for (ValidationError ve : validationErrors) {
                            System.err.println("VALIDATION ERROR: " + ve);
                        }
                        System.err.println("ERROR: " + target + ": 検証エラーのためスキップ (" + validationErrors.size() + " 件)");
                        errorCount++;
                        continue;
                    }
                }

                TestDataContainer container = reader.read(target);

                if (xlsReader != null) {
                    int commentLines = xlsReader.getLastCommentLineCount();
                    if (commentLines > 0) {
                        totalCommentLines += commentLines;
                        commentLineFiles++;
                    }
                }

                // Warn and skip if no blocks in any section (NG-5)
                boolean hasAnyBlock = false;
                for (TestDataSection section : container.getSections()) {
                    if (!section.getBlocks().isEmpty()) {
                        hasAnyBlock = true;
                        break;
                    }
                }
                if (!hasAnyBlock && !container.getSections().isEmpty()) {
                    System.err.println("WARN: " + target + ": no data blocks found (empty sheet or comment-only). Skipping output.");
                    successCount++;
                    if (opts.deleteSource) {
                        deleteSource(target);
                    }
                    continue;
                }

                // Calculate output path
                Path outputBase;
                if (opts.from.equals("xls")) {
                    // For YamlFormatWriter, outputPath is the parent of containerName dir
                    outputBase = ConverterPathResolver.xlsToYamlDir(opts.inputPath, target, opts.outputPath).getParent();
                    if (outputBase == null) outputBase = opts.outputPath;
                } else {
                    // For XlsFormatWriter, outputPath is the parent of containerName.xls
                    outputBase = ConverterPathResolver.yamlDirToXls(opts.inputPath, target, opts.outputPath).getParent();
                    if (outputBase == null) outputBase = opts.outputPath;
                }

                writer.write(container, outputBase, opts.overwrite);

                if (opts.deleteSource) {
                    deleteSource(target);
                }
                successCount++;
            } catch (ConverterException e) {
                System.err.println("ERROR: " + target + ": " + e.getMessage());
                errorCount++;
            }
        }

        System.out.println("=== TestDataConverter 変換サマリー ===");
        System.out.println("変換成功: " + successCount + " 件");
        if (skipCount[0] > 0) {
            System.out.println("スキップ: " + skipCount[0] + " 件（除外パターン合致）");
        }
        System.out.println("エラー:   " + errorCount + " 件");
        if (totalCommentLines > 0) {
            System.out.println("コメント行ロスト: " + totalCommentLines + " 行（" + commentLineFiles + " ファイル）");
        }
        return errorCount > 0 ? 1 : 0;
    }

    /** --validate モードの実行。YAML ディレクトリを再帰的に検証する。 */
    private static int runValidateOnly(Path inputPath) {
        File inputDir = inputPath.toFile();
        if (!inputDir.isDirectory()) {
            System.err.println("ERROR: --validate requires a directory: " + inputPath);
            return 1;
        }

        YamlTestDataValidator validator = new YamlTestDataValidator();
        int errorCount = 0;

        // inputPath 直下の各サブディレクトリ（コンテナ）を検証対象とする
        File[] containers = inputDir.listFiles(File::isDirectory);
        if (containers == null || containers.length == 0) {
            // 直下に YAML ファイルがある場合（セクションディレクトリとして扱う）
            List<ValidationError> errors = validator.validate(inputPath);
            errorCount += errors.size();
            for (ValidationError ve : errors) {
                System.err.println("VALIDATION ERROR: " + ve);
            }
        } else {
            for (File container : containers) {
                List<ValidationError> errors = validator.validate(container.toPath());
                errorCount += errors.size();
                for (ValidationError ve : errors) {
                    System.err.println("VALIDATION ERROR: " + ve);
                }
            }
        }

        System.out.println("=== TestDataConverter 検証サマリー ===");
        System.out.println("エラー: " + errorCount + " 件");
        return errorCount > 0 ? 1 : 0;
    }

    private static void deleteSource(Path target) {
        File f = target.toFile();
        if (f.isFile()) {
            if (!f.delete()) {
                System.err.println("WARN: Failed to delete source: " + f);
            }
        } else {
            deleteDirectory(f);
        }
    }

    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else if (!f.delete()) {
                    System.err.println("WARN: Failed to delete source file: " + f);
                }
            }
        }
        if (!dir.delete()) {
            System.err.println("WARN: Failed to delete source directory: " + dir);
        }
    }

    private static Options parseArgs(String[] args) {
        Options opts = new Options();
        List<String> positional = new ArrayList<>();
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            switch (arg) {
                case "--from":
                    if (++i >= args.length) return null;
                    opts.from = args[i];
                    break;
                case "--to":
                    if (++i >= args.length) return null;
                    opts.to = args[i];
                    break;
                case "--overwrite":
                    opts.overwrite = true;
                    break;
                case "--delete-source":
                    opts.deleteSource = true;
                    break;
                case "--xls":
                    opts.xlsFormat = true;
                    break;
                case "--include":
                    if (++i >= args.length) return null;
                    opts.includes.add(args[i]);
                    break;
                case "--exclude":
                    if (++i >= args.length) return null;
                    opts.excludes.add(args[i]);
                    break;
                case "--validate":
                    if (++i >= args.length) return null;
                    opts.validateOnly = true;
                    positional.add(args[i]); // 入力パスとして扱う
                    break;
                case "--validate-on-convert":
                    opts.validateOnConvert = true;
                    break;
                default:
                    positional.add(arg);
                    break;
            }
            i++;
        }
        if (opts.validateOnly) {
            if (positional.size() < 1) return null;
            opts.inputPath = Paths.get(positional.get(0));
            return opts;
        }
        if (opts.from == null || opts.to == null || positional.size() < 2) {
            return null;
        }
        opts.inputPath = Paths.get(positional.get(positional.size() - 2));
        opts.outputPath = Paths.get(positional.get(positional.size() - 1));
        return opts;
    }

    private static class Options {
        String from;
        String to;
        boolean overwrite = false;
        boolean deleteSource = false;
        boolean xlsFormat = false;
        boolean validateOnly = false;
        boolean validateOnConvert = false;
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        Path inputPath;
        Path outputPath;
    }
}
