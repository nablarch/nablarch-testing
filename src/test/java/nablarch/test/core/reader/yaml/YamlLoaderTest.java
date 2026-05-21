package nablarch.test.core.reader.yaml;

import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link YamlLoader} のテストクラス。
 *
 * <p>
 * YAML ファイルのロード・キャッシュ・エラー処理を検証する。
 * </p>
 */
public class YamlLoaderTest {

    private static final String RESOURCE_ROOT = "src/test/java/";
    private static final String DIR = RESOURCE_ROOT + "nablarch/test/core/reader/yaml/";

    @After
    public void after() {
        YamlLoader.clearCacheForTest();
    }

    // ========================================================================
    // load: YAML ファイルを正常にロードできること
    // ========================================================================

    /**
     * [YamlLoader] load: YAML ファイルをロードしてトップレベル Map を返すこと。
     *
     * <p>
     * Given: setup_tables セクションを含む YAML ファイル<br>
     * When:  load(dir, "YamlLoaderTest/simple") を呼ぶ<br>
     * Then:  Map が返り、setup_tables キーが存在すること
     * </p>
     */
    @Test
    public void testLoad_returnsTopLevelMap() {
        // Given / When
        Map<String, Object> result = YamlLoader.load(DIR, "YamlLoaderTest/simple");

        // Then
        assertThat(result, notNullValue());
        assertTrue(result.containsKey("setup_tables"));
    }

    /**
     * [YamlLoader] load: setup_tables の値が List であること。
     *
     * <p>
     * Given: setup_tables セクションを含む YAML ファイル<br>
     * When:  load し、setup_tables の値を取得する<br>
     * Then:  List であること
     * </p>
     */
    @Test
    public void testLoad_setupTablesIsList() {
        // Given / When
        Map<String, Object> result = YamlLoader.load(DIR, "YamlLoaderTest/simple");

        // Then
        Object setupTables = result.get("setup_tables");
        assertTrue(setupTables instanceof List);
    }

    // ========================================================================
    // load: キャッシュ（同一パスは同一インスタンスを返す）
    // ========================================================================

    /**
     * [YamlLoader] load: 同一パスを2回ロードした場合、同一インスタンスが返ること（キャッシュ）。
     *
     * <p>
     * Given: 同じ YAML ファイルパス<br>
     * When:  load を2回呼ぶ<br>
     * Then:  同一 Map インスタンスが返ること
     * </p>
     */
    @Test
    public void testLoad_returnsCachedInstance() {
        // Given / When
        Map<String, Object> first = YamlLoader.load(DIR, "YamlLoaderTest/simple");
        Map<String, Object> second = YamlLoader.load(DIR, "YamlLoaderTest/simple");

        // Then: 同一インスタンス
        assertThat(first == second, is(true));
    }

    // ========================================================================
    // load: 重複キーは例外をスローすること
    // ========================================================================

    /**
     * [YamlLoader] load: YAML ファイルに重複キーがある場合は IllegalStateException がスローされること。
     *
     * <p>
     * Given: setup_tables キーが2回定義された YAML ファイル<br>
     * When:  load を呼ぶ<br>
     * Then:  IllegalStateException がスローされること
     * </p>
     */
    @Test
    public void testLoad_throwsOnDuplicateKey() {
        // Given / When / Then
        try {
            YamlLoader.load(DIR, "YamlLoaderTest/duplicateKey");
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // OK
        }
    }

    // ========================================================================
    // load: ファイルが存在しない場合は IllegalStateException をスローすること
    // ========================================================================

    /**
     * [YamlLoader] load: 存在しないファイルを指定した場合は IllegalStateException がスローされること。
     *
     * <p>
     * Given: 存在しないファイルパス<br>
     * When:  load を呼ぶ<br>
     * Then:  IllegalStateException がスローされること
     * </p>
     */
    @Test
    public void testLoad_throwsWhenFileNotExists() {
        // Given / When / Then
        try {
            YamlLoader.load(DIR, "YamlLoaderTest/noSuchFile");
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // OK
        }
    }

    // ========================================================================
    // load: 空の YAML は空 Map を返すこと
    // ========================================================================

    /**
     * [YamlLoader] load: 空の YAML ファイルをロードした場合は空 Map が返ること。
     *
     * <p>
     * Given: 内容が空の YAML ファイル<br>
     * When:  load を呼ぶ<br>
     * Then:  空 Map が返ること
     * </p>
     */
    @Test
    public void testLoad_emptyYamlReturnsEmptyMap() {
        // Given / When
        Map<String, Object> result = YamlLoader.load(DIR, "YamlLoaderTest/empty");

        // Then
        assertThat(result.isEmpty(), is(true));
    }

    // ========================================================================
    // isResourceExisting
    // ========================================================================

    /**
     * [YamlLoader] isResourceExisting: YAML ファイルが存在する場合は true を返すこと。
     *
     * <p>
     * Given: 存在する YAML ファイル<br>
     * When:  isResourceExisting を呼ぶ<br>
     * Then:  true が返ること
     * </p>
     */
    @Test
    public void testIsResourceExisting_trueWhenExists() {
        // Given / When / Then
        assertThat(YamlLoader.isResourceExisting(DIR, "YamlLoaderTest/simple"), is(true));
    }

    /**
     * [YamlLoader] isResourceExisting: YAML ファイルが存在しない場合は false を返すこと。
     *
     * <p>
     * Given: 存在しないファイルパス<br>
     * When:  isResourceExisting を呼ぶ<br>
     * Then:  false が返ること
     * </p>
     */
    @Test
    public void testIsResourceExisting_falseWhenNotExists() {
        // Given / When / Then
        assertThat(YamlLoader.isResourceExisting(DIR, "YamlLoaderTest/noSuchFile"), is(false));
    }

    // ========================================================================
    // load: LRU キャッシュ上限超過で最古エントリが追い出されること（QA-5）
    // ========================================================================

    /**
     * [YamlLoader] load: LRU キャッシュ上限（8件）を超えると最初にロードしたエントリが追い出されること（QA-5）。
     *
     * <p>
     * Given: lru1.yaml〜lru9.yaml（9ファイル）。キャッシュ上限は 8<br>
     * When:  9ファイルをロードした後、lru1.yaml を再ロードする<br>
     * Then:  lru1.yaml の再ロード結果が最初のロードと別インスタンスであること（キャッシュから追い出されたため）
     * </p>
     */
    @Test
    public void testLoad_lruEvictionWhenCacheFull() {
        // Given: キャッシュ上限 8 を超える 9 ファイルをロードする
        Map<String, Object> first = YamlLoader.load(DIR, "YamlLoaderTest/lru1");
        YamlLoader.load(DIR, "YamlLoaderTest/lru2");
        YamlLoader.load(DIR, "YamlLoaderTest/lru3");
        YamlLoader.load(DIR, "YamlLoaderTest/lru4");
        YamlLoader.load(DIR, "YamlLoaderTest/lru5");
        YamlLoader.load(DIR, "YamlLoaderTest/lru6");
        YamlLoader.load(DIR, "YamlLoaderTest/lru7");
        YamlLoader.load(DIR, "YamlLoaderTest/lru8");

        // When: 9件目をロードして lru1 をキャッシュから追い出す
        YamlLoader.load(DIR, "YamlLoaderTest/lru9");

        // Then: lru1 は追い出されているため再ロードすると別インスタンス
        Map<String, Object> reloaded = YamlLoader.load(DIR, "YamlLoaderTest/lru1");
        assertThat("lru1 はキャッシュから追い出され、別インスタンスになること（QA-5）",
                first == reloaded, is(false));
    }

    /**
     * [YamlLoader] load: 最近アクセスしたエントリが LRU キャッシュから追い出されないこと（QA観点2-中）。
     *
     * <p>
     * Given: lru1.yaml〜lru8.yaml（8ファイル）をロード後、lru1 に再アクセスする<br>
     * When:  lru9.yaml（9件目）をロードしてエビクションを起こす<br>
     * Then:  最近アクセスした lru1 がキャッシュに残っており、同一インスタンスが返ること
     * </p>
     */
    @Test
    public void testLoad_recentlyAccessedEntryIsNotEvicted() {
        // Given: 8 ファイルをロードしてキャッシュを満杯にする
        Map<String, Object> lru1 = YamlLoader.load(DIR, "YamlLoaderTest/lru1");
        YamlLoader.load(DIR, "YamlLoaderTest/lru2");
        YamlLoader.load(DIR, "YamlLoaderTest/lru3");
        YamlLoader.load(DIR, "YamlLoaderTest/lru4");
        YamlLoader.load(DIR, "YamlLoaderTest/lru5");
        YamlLoader.load(DIR, "YamlLoaderTest/lru6");
        YamlLoader.load(DIR, "YamlLoaderTest/lru7");
        YamlLoader.load(DIR, "YamlLoaderTest/lru8");

        // When: lru1 に再アクセスして「最近使用」にしてから 9 件目をロード
        YamlLoader.load(DIR, "YamlLoaderTest/lru1");  // lru1 を最近使用に更新
        YamlLoader.load(DIR, "YamlLoaderTest/lru9");  // lru2 が追い出されるはず

        // Then: lru1 はキャッシュに残っており同一インスタンス（最近アクセスしたので追い出されない）
        Map<String, Object> afterEviction = YamlLoader.load(DIR, "YamlLoaderTest/lru1");
        assertThat("最近アクセスした lru1 はキャッシュに残っているため同一インスタンスであること",
                lru1 == afterEviction, is(true));
    }
}
