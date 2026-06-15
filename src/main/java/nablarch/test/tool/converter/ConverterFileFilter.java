package nablarch.test.tool.converter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 入力ディレクトリを走査し、変換対象（Excel ブックファイル／YAML コンテナディレクトリ）を
 * include／exclude グロブで絞り込んで列挙する純関数群。
 *
 * <p>
 * include／exclude は入力ルートからの相対パスに対して評価する。include が空なら全件対象、
 * exclude に合致したものは除外する。結果は再現性のため辞書順にソートして返す。
 * </p>
 *
 * @author kiyobot
 */
public final class ConverterFileFilter {

    /** Excel ブックの拡張子（小文字） */
    private static final String[] XLS_EXTENSIONS = {".xls", ".xlsx"};

    /** YAML ファイルの拡張子（小文字） */
    private static final String YAML_EXTENSION = ".yaml";

    /** Excel の一時ロックファイル接頭辞 */
    private static final String TEMP_FILE_PREFIX = "~$";

    /** ユーティリティクラスにつきインスタンス化不可。 */
    private ConverterFileFilter() {
        throw new AssertionError("ConverterFileFilter は static 専用です");
    }

    /**
     * 入力ディレクトリ配下の Excel ブックファイルを列挙する。
     *
     * @param inputRoot 入力ルートディレクトリ
     * @param includes  取り込み glob（空なら全件）
     * @param excludes  除外 glob
     * @return 条件に合致するブックファイルのリスト（相対パス辞書順）
     * @throws ConverterException 入力ディレクトリが存在しない場合
     */
    public static List<Path> findXlsFiles(Path inputRoot, List<String> includes, List<String> excludes) {
        return find(inputRoot, includes, excludes, ConverterFileFilter::isXlsBook);
    }

    /**
     * 入力ディレクトリ配下の YAML コンテナディレクトリ（{@code .yaml} ファイルを直下に持つディレクトリ）を列挙する。
     *
     * @param inputRoot 入力ルートディレクトリ
     * @param includes  取り込み glob（空なら全件）
     * @param excludes  除外 glob
     * @return 条件に合致するディレクトリのリスト（相対パス辞書順）
     * @throws ConverterException 入力ディレクトリが存在しない場合
     */
    public static List<Path> findYamlDirs(Path inputRoot, List<String> includes, List<String> excludes) {
        requireDirectory(inputRoot);
        TreeSet<Path> dirs = new TreeSet<>();
        try (Stream<Path> walk = Files.walk(inputRoot)) {
            walk.filter(ConverterFileFilter::isYamlFile)
                .map(Path::getParent)
                .forEach(dirs::add);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to scan input directory: " + inputRoot, e);
        }
        return dirs.stream()
                   .filter(dir -> accepted(inputRoot, dir, includes, excludes))
                   .collect(Collectors.toList());
    }

    /**
     * 入力ディレクトリ配下を走査し、述語に合致し include／exclude を通過したパスを辞書順で返す。
     *
     * @param inputRoot 入力ルートディレクトリ
     * @param includes  取り込み glob
     * @param excludes  除外 glob
     * @param predicate 対象判定述語
     * @return 条件に合致するパスのリスト（相対パス辞書順）
     */
    private static List<Path> find(Path inputRoot, List<String> includes, List<String> excludes,
                                   java.util.function.Predicate<Path> predicate) {
        requireDirectory(inputRoot);
        try (Stream<Path> walk = Files.walk(inputRoot)) {
            return walk.filter(predicate)
                       .filter(path -> accepted(inputRoot, path, includes, excludes))
                       .sorted()
                       .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("failed to scan input directory: " + inputRoot, e);
        }
    }

    /**
     * パスが include／exclude を通過するか判定する。
     *
     * @param inputRoot 入力ルートディレクトリ
     * @param path      判定対象パス
     * @param includes  取り込み glob（空なら全件通過）
     * @param excludes  除外 glob
     * @return 通過するなら {@code true}
     */
    private static boolean accepted(Path inputRoot, Path path, List<String> includes, List<String> excludes) {
        Path relative = inputRoot.relativize(path);
        if (!includes.isEmpty() && !matchesAny(inputRoot, relative, includes)) {
            return false;
        }
        return !matchesAny(inputRoot, relative, excludes);
    }

    /**
     * 相対パスがいずれかの glob に合致するか判定する。
     *
     * @param inputRoot 入力ルートディレクトリ（PathMatcher 生成のため）
     * @param relative  入力ルートからの相対パス
     * @param patterns  glob パターン群
     * @return いずれかに合致するなら {@code true}
     */
    private static boolean matchesAny(Path inputRoot, Path relative, List<String> patterns) {
        for (String pattern : patterns) {
            PathMatcher matcher = inputRoot.getFileSystem().getPathMatcher("glob:" + pattern);
            if (matcher.matches(relative)) {
                return true;
            }
        }
        return false;
    }

    /**
     * パスが Excel ブックファイル（一時ロックファイルを除く）か判定する。
     *
     * @param path 判定対象パス
     * @return Excel ブックなら {@code true}
     */
    private static boolean isXlsBook(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String name = path.getFileName().toString();
        if (name.startsWith(TEMP_FILE_PREFIX)) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String extension : XLS_EXTENSIONS) {
            if (lower.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * パスが YAML ファイルか判定する。
     *
     * @param path 判定対象パス
     * @return YAML ファイルなら {@code true}
     */
    private static boolean isYamlFile(Path path) {
        return Files.isRegularFile(path)
            && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(YAML_EXTENSION);
    }

    /**
     * 入力がディレクトリであることを検証する。
     *
     * @param inputRoot 入力ルートディレクトリ
     * @throws ConverterException ディレクトリでない場合
     */
    private static void requireDirectory(Path inputRoot) {
        if (!Files.isDirectory(inputRoot)) {
            throw new ConverterException("input directory not found: " + inputRoot);
        }
    }
}
