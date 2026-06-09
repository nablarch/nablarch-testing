package nablarch.test.core.reader.yaml;

import nablarch.fw.ExecutionContext;
import nablarch.fw.launcher.CommandLine;
import nablarch.test.RepositoryInitializer;
import nablarch.test.core.standalone.MainForRequestTesting;
import nablarch.test.core.standalone.TestShot;
import nablarch.test.tool.converter.ConversionRequest;
import nablarch.test.tool.converter.DataFormat;
import nablarch.test.tool.converter.TestDataConverter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * YAML モードテストを支援するユーティリティ。
 *
 * <p>
 * YAML 版テストクラス（{@code XxxYamlTest extends XxxTest}）から呼び出す static メソッドと定数を提供する。
 * </p>
 *
 * <p>使い方:</p>
 * <pre>
 * public class FooYamlTest extends FooTest {
 *     &#64;Rule
 *     public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");
 *
 *     &#64;BeforeClass
 *     public static void prepareYaml() {
 *         YamlModeTestBase.prepareYamlData(FooYamlTest.class, FooTest.class);
 *     }
 *
 *     &#64;Before
 *     public void switchToYaml() {
 *         repositoryResource.addComponent("testDataParser", new YamlTestDataParser());
 *         repositoryResource.addComponent("nablarch.test.resource-root", YamlModeTestBase.YAML_ROOT);
 *     }
 * }
 * </pre>
 */
public final class YamlModeTestBase {

    /** 変換先ルートディレクトリ（target 配下・.gitignore で管理対象外） */
    public static final String YAML_ROOT = "target/generated-test-yaml";

    private YamlModeTestBase() {
    }

    /**
     * テスト実行前に Excel を YAML に変換し、自クラス名ディレクトリへ複製する。
     *
     * <p>
     * YAML 版テストクラスの {@code @BeforeClass} メソッドから呼ぶ。
     * </p>
     *
     * @param concreteClass YAML 版テストクラス自身（自クラス名ディレクトリ名に使用）
     * @param baseTestClass 変換元 Excel が置かれているテストクラス（Excel 版クラス）
     */
    public static void prepareYamlData(Class<?> concreteClass, Class<?> baseTestClass) {
        String packagePath = baseTestClass.getPackage().getName().replace('.', '/');
        String baseName = baseTestClass.getSimpleName();
        String yamlClassName = concreteClass.getSimpleName();

        Path inputDir = Paths.get("src/test/java", packagePath);
        Path outputDir = Paths.get(YAML_ROOT, packagePath);

        // 自クラス名ディレクトリの古い YAML を削除してから生成
        Path yamlClassDir = outputDir.resolve(yamlClassName);
        deleteDirectory(yamlClassDir);

        // Excel → YAML 変換（拡張子込みグロブで include 指定）
        ConversionRequest request = new ConversionRequest.Builder()
                .sourceFormat(DataFormat.XLS)
                .targetFormat(DataFormat.YAML)
                .inputPath(inputDir)
                .outputPath(outputDir)
                .overwrite(true)
                .include(baseName + ".xls")
                .include(baseName + ".xlsx")
                .build();
        TestDataConverter.convert(request);

        // 変換出力ディレクトリ名（元ブック名）を自クラス名ディレクトリへ複製
        Path convertedDir = outputDir.resolve(baseName);
        if (Files.exists(convertedDir) && !baseName.equals(yamlClassName)) {
            copyDirectory(convertedDir, yamlClassDir);
        }
    }

    /**
     * {@code @ClassRule} を使うテストクラス向けに、{@link TestShot.TestShotAround} をラップする。
     *
     * <p>
     * {@link MainForRequestTesting#handle} の finally で {@code revertDefaultRepository()} が呼ばれ
     * {@code SystemRepository} が unit-test.xml ベースに戻る。このメソッドが返す
     * {@code TestShotAround} は、{@code createMain()} で返す {@code Main} の {@code handle()} が
     * 完了した直後に {@code reInitializeRepository(repositoryXml)} を呼び直すことで、
     * 後続の {@code assertAll()} で YAML 版リポジトリが使われるようにする。
     * </p>
     *
     * @param original      元の {@link TestShot.TestShotAround} 実装
     * @param repositoryXml 差し替え先リポジトリ XML（例: {@code "unit-test-yaml.xml"}）
     * @return ラップされた {@link TestShot.TestShotAround}
     */
    public static TestShot.TestShotAround wrapForYaml(
            TestShot.TestShotAround original, String repositoryXml) {
        return new TestShot.TestShotAround() {
            @Override
            public void setUpInputData(TestShot testShot) {
                original.setUpInputData(testShot);
            }
            @Override
            public void assertOutputData(String msgOnFail, TestShot testShot) {
                original.assertOutputData(msgOnFail, testShot);
            }
            @Override
            public boolean isColumnForTestFramework(String columnName) {
                return original.isColumnForTestFramework(columnName);
            }
            @Override
            public String compareStatus(int actual, TestShot testShot) {
                return original.compareStatus(actual, testShot);
            }
            @Override
            public nablarch.fw.launcher.Main createMain() {
                return new MainForRequestTesting() {
                    @Override
                    public Integer handle(CommandLine commandLine, ExecutionContext context) {
                        try {
                            return super.handle(commandLine, context);
                        } finally {
                            RepositoryInitializer.reInitializeRepository(repositoryXml);
                        }
                    }
                };
            }
        };
    }

    private static void deleteDirectory(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void copyDirectory(Path src, Path dest) {
        try (Stream<Path> walk = Files.walk(src)) {
            walk.forEach(source -> {
                Path target = dest.resolve(src.relativize(source));
                try {
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
