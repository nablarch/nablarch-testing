package nablarch.test.tool.converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * 変換対象ファイル・ディレクトリの列挙。
 */
public final class ConverterFileFilter {

    private ConverterFileFilter() {
    }

    /**
     * ルートディレクトリを再帰走査して .xls ファイルを列挙する。
     *
     * @param root     走査するルートディレクトリ
     * @param includes ファイル名グロブパターン（空リストは「全て含む」）
     * @param excludes ファイル名グロブパターン（空リストは「除外なし」）
     * @return 変換対象の .xls ファイルパスリスト
     */
    public static List<Path> findXlsFiles(Path root, List<String> includes, List<String> excludes)
            throws ConverterException {
        List<PathMatcher> includeMatchers = toMatchers(includes);
        List<PathMatcher> excludeMatchers = toMatchers(excludes);
        List<Path> result = new ArrayList<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String name = file.getFileName().toString();
                    if (!name.endsWith(".xls")) return FileVisitResult.CONTINUE;
                    if (!matchesIncludes(name, includeMatchers)) return FileVisitResult.CONTINUE;
                    if (matchesExcludes(name, excludeMatchers)) return FileVisitResult.CONTINUE;
                    result.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new ConverterException("Failed to scan directory: " + root, e);
        }
        return result;
    }

    /**
     * ルートディレクトリを再帰走査して YAML ディレクトリを列挙する。
     *
     * <p>YAML ディレクトリ: 直下に .yaml ファイルを 1 件以上含み、.yaml ファイルを含む
     * サブディレクトリを持たない最下位ディレクトリ。</p>
     *
     * @param root     走査するルートディレクトリ
     * @param includes ディレクトリ名グロブパターン（空リストは「全て含む」）
     * @param excludes ディレクトリ名グロブパターン（空リストは「除外なし」）
     * @return 変換対象の YAML ディレクトリパスリスト
     */
    public static List<Path> findYamlDirs(Path root, List<String> includes, List<String> excludes)
            throws ConverterException {
        List<PathMatcher> includeMatchers = toMatchers(includes);
        List<PathMatcher> excludeMatchers = toMatchers(excludes);
        List<Path> result = new ArrayList<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(root)) return FileVisitResult.CONTINUE;
                    String name = dir.getFileName().toString();
                    if (matchesExcludes(name, excludeMatchers)) return FileVisitResult.SKIP_SUBTREE;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    if (dir.equals(root)) return FileVisitResult.CONTINUE;
                    if (isYamlDir(dir)) {
                        String name = dir.getFileName().toString();
                        if (!matchesIncludes(name, includeMatchers)) return FileVisitResult.CONTINUE;
                        result.add(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new ConverterException("Failed to scan directory: " + root, e);
        }
        return result;
    }

    /** 直下に .yaml ファイルを持ち、.yaml を含むサブディレクトリを持たないか確認する。 */
    private static boolean isYamlDir(Path dir) {
        File[] files = dir.toFile().listFiles();
        if (files == null) return false;
        boolean hasYaml = false;
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".yaml")) {
                hasYaml = true;
            } else if (f.isDirectory() && containsYaml(f)) {
                return false; // sub-dir with yaml exists → not a leaf YAML dir
            }
        }
        return hasYaml;
    }

    private static boolean containsYaml(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return false;
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".yaml")) return true;
            if (f.isDirectory() && containsYaml(f)) return true;
        }
        return false;
    }

    private static boolean matchesIncludes(String name, List<PathMatcher> matchers) {
        if (matchers.isEmpty()) return true;
        Path namePath = FileSystems.getDefault().getPath(name);
        for (PathMatcher m : matchers) {
            if (m.matches(namePath)) return true;
        }
        return false;
    }

    private static boolean matchesExcludes(String name, List<PathMatcher> matchers) {
        Path namePath = FileSystems.getDefault().getPath(name);
        for (PathMatcher m : matchers) {
            if (m.matches(namePath)) return true;
        }
        return false;
    }

    private static List<PathMatcher> toMatchers(List<String> patterns) {
        List<PathMatcher> matchers = new ArrayList<>();
        for (String pattern : patterns) {
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + pattern));
        }
        return matchers;
    }
}
