package nablarch.test.core.reader.yaml;

import nablarch.test.NablarchTestUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * YAML ファイルのロードとキャッシュ管理。
 *
 * <p>
 * {@code nablarch.test.core.reader.yaml} パッケージ内のビルダークラスおよび
 * {@link nablarch.test.core.reader.YamlTestDataParser} から使用する。
 * </p>
 *
 * <p>
 * SnakeYAML 2.x の {@link SafeConstructor} を使用し、型変換を制限して安全にロードする。
 * 重複キーは {@link IllegalStateException} をスローする。
 * </p>
 */
public final class YamlLoader {

    private static final String YAML_EXTENSION = ".yaml";

    /** 既存の {@link nablarch.test.core.reader.TableDataParser} 等のキャッシュサイズに合わせた値。 */
    private static final int YAML_CACHE_MAX_SIZE = 8;

    /** YAML キャッシュ（filePath → 解析済み Map）。アクセス順 LRU で最大 {@value #YAML_CACHE_MAX_SIZE} エントリを保持する。 */
    private static final Map<String, Map<String, Object>> YAML_CACHE =
            Collections.synchronizedMap(
                    NablarchTestUtils.<String, Map<String, Object>>createLRUMap(YAML_CACHE_MAX_SIZE));

    private YamlLoader() {
    }

    /**
     * YAML ファイルをロードしてトップレベル Map を返す（キャッシュあり）。
     *
     * @param basePath     ベースパス（末尾 "/" 付き）
     * @param resourceName リソース名（拡張子なし）
     * @return YAML トップレベル Map（空ファイルの場合は空 Map）
     * @throws IllegalStateException ファイルが存在しない場合、IO エラー、または重複キーが存在する場合
     */
    public static Map<String, Object> load(String basePath, String resourceName) {
        String filePath = basePath + resourceName + YAML_EXTENSION;
        synchronized (YAML_CACHE) {
            Map<String, Object> cached = YAML_CACHE.get(filePath);
            if (cached != null) {
                return cached;
            }
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            Yaml yaml = new Yaml(new SafeConstructor(options));
            try (FileInputStream in = new FileInputStream(new File(filePath))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) yaml.load(in);
                if (result == null) {
                    result = Collections.emptyMap();
                }
                YAML_CACHE.put(filePath, result);
                return result;
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load YAML file: " + filePath, e);
            } catch (YAMLException e) {
                throw new IllegalStateException("Failed to parse YAML file: " + filePath, e);
            }
        }
    }

    /**
     * YAML ファイルが存在するかどうかを返す。
     *
     * @param basePath     ベースパス
     * @param resourceName リソース名
     * @return 存在する場合 true
     */
    public static boolean isResourceExisting(String basePath, String resourceName) {
        return new File(basePath + resourceName + YAML_EXTENSION).exists();
    }

    /**
     * テスト専用: YAML キャッシュをクリアする。
     *
     * <p>
     * テスト間のキャッシュ汚染を防ぐために、各テストクラスの {@code @After} メソッドから必ず呼ぶこと。
     * 呼び忘れた場合、テスト間でファイルを変更しても古いキャッシュが使われ続け、テスト結果が不正になる。
     * </p>
     *
     * <p>
     * このメソッドはテストコードからのみ呼ぶこと。プロダクションコードからの呼び出しは不可。
     * </p>
     */
    public static void clearCacheForTest() {
        YAML_CACHE.clear();
    }
}
