package nablarch.test.core.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import nablarch.core.util.StringUtil;
import nablarch.test.core.reader.yaml.YamlRowBuilder;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * YAMLファイルからテストデータを読み込む {@link TestDataReader} 実装。
 *
 * <p>
 * {@link #open(String, String)} に指定された {@code dataName} に対して
 * {@code {path}/{dataName}.yaml} を検索して読み込む。
 * </p>
 *
 * <p>
 * YAML ネイティブ型の変換ルール（RS-03〜RS-06 参照）:
 * <ul>
 *   <li>{@code null} → 文字列 {@code "null"}</li>
 *   <li>{@code true}/{@code false} → 文字列 {@code "true"}/{@code "false"}</li>
 *   <li>整数/浮動小数点 → 数字文字列</li>
 *   <li>各行の末尾が省略された列は {@code ""} で補完</li>
 * </ul>
 * </p>
 *
 * <p>
 * {@link #open(String, String)} を複数回呼び出した場合、以前のデータは破棄され
 * 新しいファイルのデータで上書きされる。読み込み位置は先頭にリセットされる。
 * </p>
 */
public class YamlTestDataReader implements TestDataReader {

    /** 行シーケンスの組み立てを委譲するビルダ */
    private final YamlRowBuilder rowBuilder = new YamlRowBuilder();

    /** 読み込んだ行シーケンス */
    private List<List<String>> rows = null;

    /** 現在の読み込み位置 */
    private int index = 0;

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException {@code path} または {@code dataName} が null または空の場合
     * @throws RuntimeException         YAMLファイルが存在しない場合、またはファイル読み込みに失敗した場合
     */
    @Override
    public void open(String path, String dataName) {
        if (StringUtil.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("path must not be null or empty.");
        }
        if (StringUtil.isNullOrEmpty(dataName)) {
            throw new IllegalArgumentException("dataName must not be null or empty.");
        }

        File file = new File(path, dataName + ".yaml");
        if (!file.exists()) {
            throw new RuntimeException("YAML test data file not found: " + file.getAbsolutePath());
        }

        Map<String, Object> yaml = loadYaml(file);
        rows = rowBuilder.build(yaml);
        index = 0;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        rows = null;
        index = 0;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> readLine() {
        if (rows == null || index >= rows.size()) {
            return null;
        }
        return rows.get(index++);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isResourceExisting(String basePath, String resourceName) {
        return existsYamlFile(basePath, resourceName);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDataExisting(String basePath, String resourceName) {
        return existsYamlFile(basePath, resourceName);
    }

    // -----------------------------------------------------------------------
    // プライベートメソッド
    // -----------------------------------------------------------------------

    /**
     * 指定パス配下に {@code {resourceName}.yaml} が存在するかを返す。
     *
     * @param basePath     ベースパス
     * @param resourceName リソース名（拡張子なし）
     * @return ファイルが存在する場合 {@code true}
     */
    private boolean existsYamlFile(String basePath, String resourceName) {
        return new File(basePath, resourceName + ".yaml").exists();
    }

    /**
     * YAML ファイルをロードしてトップレベルマップを返す。
     *
     * @param file 読み込む YAML ファイル
     * @return YAML トップレベルマップ。マップ形式でない場合は空マップ
     * @throws RuntimeException ファイル読み込みに失敗した場合
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(File file) {
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(options));
        try (FileInputStream fis = new FileInputStream(file);
             Reader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            Object result = yaml.load(reader);
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            }
            return Collections.emptyMap();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YAML file: " + file.getAbsolutePath(), e);
        }
    }
}
