package nablarch.test.core.reader.yaml;

import nablarch.test.NablarchTestUtils;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * YAML ファイルのロードとキャッシュ管理。
 *
 * <p>
 * {@code nablarch.test.core.reader.yaml} パッケージ内のビルダ（{@code Yaml*Builder}）・
 * {@link nablarch.test.core.reader.YamlTestDataParser}・{@link nablarch.test.core.reader.YamlTestCoreAdapter} から使用する。
 * </p>
 *
 * <p>
 * SnakeYAML Engine 3.x の {@link Load} を使用する。
 * デフォルトの Core Schema（YAML 1.2）が適用されるため、{@code no}/{@code yes}/{@code on}/{@code off} は
 * 文字列として扱われる（YAML 1.1 の Boolean 変換は行われない）。
 * 重複キーは {@link IllegalStateException} をスローする。
 * </p>
 *
 * @author kiyotis
 */
public final class YamlLoader {

    private static final String YAML_EXTENSION = ".yaml";

    /** 既存の {@link nablarch.test.core.reader.TableDataParser} 等のキャッシュサイズに合わせた値。 */
    private static final int YAML_CACHE_MAX_SIZE = 8;

    /** YAML キャッシュ（filePath → 解析済み Map）。アクセス順 LRU で最大 {@value #YAML_CACHE_MAX_SIZE} エントリを保持する。 */
    private static final Map<String, Map<String, Object>> YAML_CACHE =
            NablarchTestUtils.createLRUMap(YAML_CACHE_MAX_SIZE);

    private YamlLoader() {
    }

    /**
     * basePath と resourceName を "/" 1 つで連結してファイルパスを組み立てる。
     * basePath が末尾 "/" 付きの場合は余分な "/" を追加しない。
     */
    private static String buildFilePath(String basePath, String resourceName) {
        if (basePath.endsWith("/")) {
            return basePath + resourceName + YAML_EXTENSION;
        }
        return basePath + "/" + resourceName + YAML_EXTENSION;
    }

    /**
     * YAML ファイルをロードしてトップレベル Map を返す（キャッシュあり）。
     *
     * @param basePath     ベースパス（末尾 "/" あり・なし両方可）
     * @param resourceName リソース名（拡張子なし）
     * @return YAML トップレベル Map（空ファイルの場合は空 Map）
     * @throws IllegalStateException ファイルが存在しない場合、IO エラー、または重複キーが存在する場合
     */
    public static Map<String, Object> load(String basePath, String resourceName) {
        String filePath = buildFilePath(basePath, resourceName);
        Map<String, Object> cached = YAML_CACHE.get(filePath);
        if (cached != null) {
            return cached;
        }
        LoadSettings settings = LoadSettings.builder()
                .setAllowDuplicateKeys(false)
                .build();
        Load loader = new Load(settings);
        try (FileInputStream in = new FileInputStream(new File(filePath))) {
            Object loaded = loader.loadFromInputStream(in);
            if (loaded == null) {
                YAML_CACHE.put(filePath, Collections.emptyMap());
                return Collections.emptyMap();
            }
            if (!(loaded instanceof Map)) {
                throw new IllegalStateException(
                        "YAML root must be a mapping, but was "
                                + loaded.getClass().getSimpleName() + ". file=" + filePath);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) loaded;
            YAML_CACHE.put(filePath, result);
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load YAML file: " + filePath, e);
        } catch (YamlEngineException e) {
            throw new IllegalStateException("Failed to parse YAML file: " + filePath, e);
        }
    }

    /**
     * YAML ファイルが存在するかどうかを返す。
     *
     * @param basePath     ベースパス（末尾 "/" あり・なし両方可）
     * @param resourceName リソース名
     * @return 存在する場合 true
     */
    public static boolean isResourceExisting(String basePath, String resourceName) {
        return new File(buildFilePath(basePath, resourceName)).exists();
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
