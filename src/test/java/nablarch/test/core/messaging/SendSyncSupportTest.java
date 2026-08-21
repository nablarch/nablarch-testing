package nablarch.test.core.messaging;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.util.FilePathSetting;
import nablarch.test.core.reader.BasicTestDataParser;
import nablarch.test.core.reader.DataType;
import nablarch.test.core.reader.TsvTestDataReader;
import nablarch.test.core.util.interpreter.TestDataInterpreter;
import nablarch.test.support.SystemRepositoryResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

/**
 * {@link SendSyncSupport}のテストクラス。
 *
 * @author kiyohito ito
 */
public class SendSyncSupportTest {

    /** テストデータの再読み込みを検証するテストデータのリクエストID */
    private static final String REQUEST_ID = "RM11AD0301";

    /** サブディレクトリを含むテストデータのリクエストID */
    private static final String REQUEST_ID_WITH_SUB_DIRECTORY = "RM11AD0302";

    /** 読み込み中にテストデータが書き換えられる場合を検証するテストデータのリクエストID */
    private static final String REQUEST_ID_UPDATED_WHILE_READING = "RM11AD0303";

    /** テストデータのリソース名（リクエストIDと同名のディレクトリからの相対パス） */
    private static final String RESOURCE_NAME = "message.txt";

    /** テストデータとして読み込まれないダミーファイルのパス（リクエストIDと同名のディレクトリからの相対パス） */
    private static final String DUMMY_RESOURCE_NAME = "dummy.txt";

    /** サブディレクトリ配下に配置したファイルのパス（リクエストIDと同名のディレクトリからの相対パス） */
    private static final String NESTED_RESOURCE_NAME = "sub/nested.txt";

    /**
     * 最終更新日時に設定する未来方向のオフセット（ミリ秒）。
     * <p>
     * 直前に書き込んだファイルの最終更新日時はほぼ現在時刻であるため、
     * 同一ミリ秒内に書き換えると値が変化しない。
     * また{@code setLastModified}で設定した値をどこまでの精度で保持できるかはファイルシステムに依存する。
     * これらの影響を受けずに「更新された」と判別できるよう、現在時刻より十分に後の日時を明示的に設定する。
     * </p>
     */
    private static final long FUTURE_OFFSET_MILLIS = 2000L;

    /** エントリごとに異なる最終更新日時を与えるための刻み幅（ミリ秒） */
    private static final long STEP_MILLIS = 2000L;

    /** 十分に離れた日時を作るためのオフセット（ミリ秒）。1日。 */
    private static final long FAR_OFFSET_MILLIS = 24L * 60L * 60L * 1000L;

    /** テストデータがディレクトリ配下に配置される構成（sendSyncTestDataに拡張子未設定）の設定 */
    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
            "nablarch/test/core/messaging/directory/directory-component-configuration.xml");

    /** 一時ディレクトリ */
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /** テスト対象 */
    private final SendSyncSupport target = new SendSyncSupport();

    /** テスト実行前のテストデータファイルの内容 */
    private final Map<File, byte[]> savedContents = new HashMap<File, byte[]>();

    /** テスト実行前のテストデータファイルの最終更新日時 */
    private final Map<File, Long> savedLastModified = new HashMap<File, Long>();

    /**
     * テストデータファイルの内容と最終更新日時を退避する。
     * <p>
     * テストはクラスパス上（{@code target/test-classes}配下）のテストデータを書き換えるため、
     * 退避しておかないと更新後の内容と未来の最終更新日時が残り続ける。
     * この場合、コピー元より最終更新日時が新しいため{@code process-test-resources}でも再コピーされず、
     * {@code clean}するまでテストデータの編集が反映されなくなる。
     * </p>
     *
     * @throws Exception 予期しない例外
     */
    @Before
    public void saveTestData() throws Exception {
        saveRecursively(getTestDataDirectory(REQUEST_ID));
        saveRecursively(getTestDataDirectory(REQUEST_ID_WITH_SUB_DIRECTORY));
        saveRecursively(getTestDataDirectory(REQUEST_ID_UPDATED_WHILE_READING));
    }

    /**
     * 退避しておいたテストデータファイルの内容と最終更新日時を復元する。
     * <p>
     * 途中で復元に失敗しても残りのファイルの復元を試み、すべて試みた後に失敗を報告する。
     * </p>
     *
     * @throws Exception 予期しない例外
     */
    @After
    public void restoreTestData() throws Exception {
        List<String> failures = new ArrayList<String>();
        for (Map.Entry<File, byte[]> entry : savedContents.entrySet()) {
            File file = entry.getKey();
            try {
                Files.write(file.toPath(), entry.getValue());
                if (!file.setLastModified(savedLastModified.get(file))) {
                    failures.add(file.getAbsolutePath());
                }
            } catch (Exception e) {
                failures.add(file.getAbsolutePath() + " (" + e + ')');
            }
        }
        assertTrue("テストデータの復元に失敗した。file=" + failures, failures.isEmpty());
    }

    /**
     * テストデータがディレクトリの場合（sendSyncTestDataに拡張子が設定されていない場合）のテスト。<br/>
     * <ul>
     *   <li>応答電文の数を上回りエラーとなった場合、何回目のリクエストでエラーとなったかがわかること</li>
     *   <li>ディレクトリ配下のテストデータを更新した場合は1つ目の応答電文から再読み込みされエラーとならないこと</li>
     *   <li>再読み込みした場合、テストデータの変更内容が応答電文に反映されること</li>
     * </ul>
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testReloadTestDataInDirectory() throws Exception {

        // Given: 応答電文が1件だけ記載されたテストデータを、リクエストIDと同名のディレクトリ配下に配置する
        File testDataFile = getTestDataFile(REQUEST_ID, RESOURCE_NAME);
        overwrite(getTestDataFile(REQUEST_ID + "_original", RESOURCE_NAME), testDataFile);

        // When/Then: 1回目は1件目の応答電文が取得できる
        DataRecord first = target.getResponseMessageByRequestId(DataType.RESPONSE_BODY_MESSAGES, REQUEST_ID);
        assertThat(first.getString("failureCode"), is("test2"));

        // When/Then: 2回目以降は応答電文が存在しないため、何回目でエラーとなったかがわかる例外が発生する
        assertResponseMessageNotExists(REQUEST_ID, 2);
        assertResponseMessageNotExists(REQUEST_ID, 3);

        // When: ディレクトリ配下のテストデータファイルの内容を書き換える
        overwrite(getTestDataFile(REQUEST_ID + "_updated", RESOURCE_NAME), testDataFile);
        setFutureLastModified(testDataFile);

        // Then: 例外が発生せず、更新後の1件目の応答電文が取得できる（再読み込みされている）
        DataRecord reloaded = target.getResponseMessageByRequestId(DataType.RESPONSE_BODY_MESSAGES, REQUEST_ID);
        assertThat(reloaded.getString("failureCode"), is("test4"));
    }

    /**
     * 未来の最終更新日時を持つファイルが同じディレクトリに存在していても、
     * テストデータの更新が検知されることのテスト。<br/>
     * 配下の最終更新日時の最大値を採る方式では、最大値が未来日時のファイルに張り付いてしまい、
     * 以降どのファイルを書き換えても再読み込みが行われなくなる。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testReloadWhenFutureTimestampedFileExists() throws Exception {

        // Given: 未来の最終更新日時を持つダミーファイルが、テストデータと同じディレクトリに存在する
        File dummyFile = getTestDataFile(REQUEST_ID, DUMMY_RESOURCE_NAME);
        assertTrue("ダミーファイルが存在しない。", dummyFile.exists());
        long base = truncateToSecond(System.currentTimeMillis());
        assertTrue("最終更新日時の設定に失敗した。", dummyFile.setLastModified(base + FAR_OFFSET_MILLIS));

        // Given: 応答電文が1件だけ記載されたテストデータを配置する
        File testDataFile = getTestDataFile(REQUEST_ID, RESOURCE_NAME);
        overwrite(getTestDataFile(REQUEST_ID + "_original", RESOURCE_NAME), testDataFile);
        assertTrue("最終更新日時の設定に失敗した。", testDataFile.setLastModified(base));

        // When/Then: 1回目は1件目の応答電文が取得できる
        DataRecord first = target.getResponseMessageByRequestId(DataType.RESPONSE_BODY_MESSAGES, REQUEST_ID);
        assertThat(first.getString("failureCode"), is("test2"));

        // When: ダミーファイルより古い範囲で、テストデータファイルを書き換える
        overwrite(getTestDataFile(REQUEST_ID + "_updated", RESOURCE_NAME), testDataFile);
        assertTrue("最終更新日時の設定に失敗した。", testDataFile.setLastModified(base + FUTURE_OFFSET_MILLIS));

        // Then: 更新後の1件目の応答電文が取得できる（再読み込みされている）
        DataRecord reloaded = target.getResponseMessageByRequestId(DataType.RESPONSE_BODY_MESSAGES, REQUEST_ID);
        assertThat(reloaded.getString("failureCode"), is("test4"));
    }

    /**
     * サブディレクトリ配下のファイルを更新した場合も再読み込みされることのテスト。<br/>
     * ディレクトリ直下しか走査しない実装では、サブディレクトリ配下の変更を検知できず例外となる。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testReloadWhenFileInSubDirectoryIsUpdated() throws Exception {

        // Given: サブディレクトリを含むディレクトリ配下に、応答電文が1件だけ記載されたテストデータを配置する
        File nestedFile = getTestDataFile(REQUEST_ID_WITH_SUB_DIRECTORY, NESTED_RESOURCE_NAME);
        assertTrue("サブディレクトリ配下のファイルが存在しない。", nestedFile.exists());

        // When/Then: 1回目は1件目の応答電文が取得できる
        DataRecord first = target.getResponseMessageByRequestId(
                DataType.RESPONSE_BODY_MESSAGES, REQUEST_ID_WITH_SUB_DIRECTORY);
        assertThat(first.getString("failureCode"), is("test2"));

        // When/Then: 2回目は応答電文が存在しないため例外が発生する
        assertResponseMessageNotExists(REQUEST_ID_WITH_SUB_DIRECTORY, 2);

        // When: ディレクトリ直下のファイルは変更せず、サブディレクトリ配下のファイルだけを更新する
        setFutureLastModified(nestedFile);

        // Then: 再読み込みされ、1件目の応答電文が再び取得できる
        DataRecord reloaded = target.getResponseMessageByRequestId(
                DataType.RESPONSE_BODY_MESSAGES, REQUEST_ID_WITH_SUB_DIRECTORY);
        assertThat(reloaded.getString("failureCode"), is("test2"));

        // Then: 読み込む番号が1に戻っている（再読み込みが行われた結果であり、番号の据え置きではない）
        assertResponseMessageNotExists(REQUEST_ID_WITH_SUB_DIRECTORY, 2);
    }

    /**
     * テストデータの読み込み中に行われた書き換えが、次回の読み出しで検知されることのテスト。<br/>
     * 最終更新日時を読み込みの後に採取する実装では、読み込み中の書き換えを取り込んでしまい、
     * 次回の読み出しで再読み込みが行われない。
     * <p>
     * スナップショットの採取を{@code getMessages}の後へ移す変異は、本テストだけが検知する。
     * 採取のタイミングを変える改修を行う際は、本テストを削除・改変する前に同じ変異を試し、
     * 検知するテストが残ることを確かめること。
     * </p>
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testReloadWhenTestDataIsUpdatedWhileReading() throws Exception {

        // Given: テストデータの読み込み中に、そのテストデータを書き換えるリーダを差し込む
        File testDataFile = getTestDataFile(REQUEST_ID_UPDATED_WHILE_READING, RESOURCE_NAME);
        BasicTestDataParser parser = new BasicTestDataParser();
        parser.setTestDataReader(new UpdatingWhileReadingTsvTestDataReader(testDataFile));
        parser.setInterpreters(
                repositoryResource.<List<TestDataInterpreter>>getComponent("messagingTestInterpreters"));
        repositoryResource.addComponent("messagingTestDataParser", parser);

        // When/Then: 1回目は1件目の応答電文が取得できる（この読み込みの最中にテストデータが書き換えられる）
        DataRecord first = target.getResponseMessageByRequestId(
                DataType.RESPONSE_BODY_MESSAGES, REQUEST_ID_UPDATED_WHILE_READING);
        assertThat(first.getString("failureCode"), is("test2"));

        // Then: 読み込み中の書き換えが取りこぼされず、2回目は再読み込みされて1件目の応答電文が取得できる
        DataRecord reloaded = target.getResponseMessageByRequestId(
                DataType.RESPONSE_BODY_MESSAGES, REQUEST_ID_UPDATED_WHILE_READING);
        assertThat(reloaded.getString("failureCode"), is("test2"));

        // Then: 読み込む番号が1に戻っている（再読み込みが行われた結果であり、番号の据え置きではない）
        assertResponseMessageNotExists(REQUEST_ID_UPDATED_WHILE_READING, 2);
    }

    /**
     * テストデータがディレクトリでない場合、
     * そのファイル自体の最終更新日時だけがスナップショットに含まれることのテスト。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testGetTimestampSnapshotForNotDirectory() throws Exception {

        // Given: ディレクトリではないテストデータ（ベースパスに拡張子が設定されている構成）
        File file = temporaryFolder.newFile("message.xls");
        long lastModified = truncateToSecond(System.currentTimeMillis() - FAR_OFFSET_MILLIS);
        assertTrue("最終更新日時の設定に失敗した。", file.setLastModified(lastModified));

        // When/Then: 設定した最終更新日時が、起点自身のエントリとして1件だけ含まれる
        Map<String, Long> expected = Collections.singletonMap("", lastModified);
        assertThat(target.getTimestampSnapshot(file), is(expected));
    }

    /**
     * 配下にファイルが存在しないディレクトリの場合、
     * ディレクトリ自体の最終更新日時だけがスナップショットに含まれることのテスト。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testGetTimestampSnapshotForEmptyDirectory() throws Exception {

        // Given: 配下にファイルが1件も存在しないディレクトリ
        File directory = temporaryFolder.newFolder("empty");
        long lastModified = truncateToSecond(System.currentTimeMillis() - FAR_OFFSET_MILLIS);
        assertTrue("最終更新日時の設定に失敗した。", directory.setLastModified(lastModified));

        // When/Then: 設定した最終更新日時が、起点自身のエントリとして1件だけ含まれる
        Map<String, Long> expected = Collections.singletonMap("", lastModified);
        assertThat(target.getTimestampSnapshot(directory), is(expected));
    }

    /**
     * サブディレクトリを含むディレクトリの場合、
     * サブディレクトリ配下のファイルの最終更新日時が変わるとスナップショットも変わることのテスト。<br/>
     * 一定の深さで再帰を打ち切る実装を検知できるよう、ツリーの深さを3としている。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testGetTimestampSnapshotChangesWhenFileInSubDirectoryIsUpdated() throws Exception {

        // Given: サブディレクトリを2階層たどった先にファイルを持つディレクトリ
        File directory = temporaryFolder.newFolder("tree");
        File nestedFile = createFile(new File(directory, "sub/deep"), "nested.txt");
        Map<String, Long> before = target.getTimestampSnapshot(directory);

        // Given: サブディレクトリ配下のファイルが、"/"区切りの相対パスをキーとして採取されている
        assertTrue("サブディレクトリ配下のファイルが採取されていない。",
                before.containsKey("sub/deep/nested.txt"));

        // When: サブディレクトリ配下のファイルの最終更新日時だけを変更する
        setFutureLastModified(nestedFile);

        // Then: スナップショットが変化する（ディレクトリ直下しか走査しない実装では変化しない）
        assertThat(target.getTimestampSnapshot(directory), is(not(before)));
    }

    /**
     * エントリが追加・削除された場合もスナップショットが変化することのテスト。<br/>
     * スナップショットのキーは配下のエントリそのものであるため、
     * 内容が変わらないエントリの追加・削除も必ず差分として現れる。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testGetTimestampSnapshotChangesWhenEntryIsAddedOrRemoved() throws Exception {

        // Given: ファイルを1件だけ持つディレクトリ
        File directory = temporaryFolder.newFolder("entries");
        createFile(directory, "message.txt");
        Map<String, Long> before = target.getTimestampSnapshot(directory);

        // When: エントリを追加する
        File added = createFile(directory, "added.txt");

        // Then: スナップショットが変化し、追加したエントリがキーとして現れる
        Map<String, Long> afterAdded = target.getTimestampSnapshot(directory);
        assertThat(afterAdded, is(not(before)));
        assertTrue("追加したエントリが採取されていない。", afterAdded.containsKey("added.txt"));

        // When: 追加したエントリを削除する
        assertTrue("ファイルの削除に失敗した。", added.delete());

        // Then: スナップショットが変化し、キーの集合が追加前に戻る
        Map<String, Long> afterRemoved = target.getTimestampSnapshot(directory);
        assertThat(afterRemoved, is(not(afterAdded)));
        assertThat(afterRemoved.keySet(), is(before.keySet()));
    }

    /**
     * ディレクトリ・ファイルの双方が、起点からの相対パスをキーとして
     * もれなくスナップショットに採取されることのテスト。<br/>
     * 採取されるキーと値の一式は既知であるため、期待する一式との完全一致で表明する。
     * 採取するエントリを減らす実装（起点以外を採取しない、ディレクトリを採取しない等）は、
     * キーの欠落として現れる。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testGetTimestampSnapshotContainsAllEntriesInTree() throws Exception {

        // Given: 複数のファイルとサブディレクトリを持つディレクトリ
        File directory = temporaryFolder.newFolder("snapshot");
        long base = truncateToSecond(System.currentTimeMillis() - FAR_OFFSET_MILLIS);
        Map<String, Long> expected = new HashMap<String, Long>();
        int index = 0;
        for (String name : new String[] {"c.txt", "a.txt", "b.txt", "d.txt", "e.txt"}) {
            // エントリごとに異なる最終更新日時を設定し、キーと値の対応の誤りが現れるようにする
            expected.put(name, setLastModified(createFile(directory, name), base + (index++ * STEP_MILLIS)));
        }

        // Given: サブディレクトリと、その配下のファイル。
        // エントリを作成すると親ディレクトリの最終更新日時が変わるため、深い側から順に設定する
        File subDirectory = new File(directory, "sub");
        File nestedFile = createFile(subDirectory, "nested.txt");
        expected.put("sub/nested.txt", setLastModified(nestedFile, base + (index++ * STEP_MILLIS)));
        expected.put("sub", setLastModified(subDirectory, base + (index++ * STEP_MILLIS)));
        expected.put("", setLastModified(directory, base + (index * STEP_MILLIS)));

        // When/Then: 期待するキーと値の一式に完全一致する
        assertThat(target.getTimestampSnapshot(directory), is(expected));
    }

    /**
     * 未来の最終更新日時を持つファイルが配下に存在する場合でも、
     * 他のファイルの更新が検知できることのテスト。<br/>
     * 配下の最大値を採る方式では、最大値が未来日時のファイルに張り付いてしまい、
     * 以降どのファイルを書き換えても値が変化せず再読み込みが行われなくなる。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testGetTimestampSnapshotDetectsUpdateUnderFutureTimestampedFile() throws Exception {

        // Given: 未来日時の最終更新日時を持つファイルと、それより古いファイルを含むディレクトリ
        File directory = temporaryFolder.newFolder("future");
        long now = System.currentTimeMillis();
        File futureFile = createFile(directory, "future.txt");
        assertTrue("最終更新日時の設定に失敗した。", futureFile.setLastModified(now + FAR_OFFSET_MILLIS));
        File updatedFile = createFile(directory, "updated.txt");
        assertTrue("最終更新日時の設定に失敗した。", updatedFile.setLastModified(now - FAR_OFFSET_MILLIS));

        Map<String, Long> before = target.getTimestampSnapshot(directory);

        // When: 未来日時のファイルより古い範囲で、別のファイルの最終更新日時を変更する
        assertTrue("最終更新日時の設定に失敗した。",
                updatedFile.setLastModified(now - FAR_OFFSET_MILLIS + STEP_MILLIS));

        // Then: スナップショットは変化する
        assertThat(target.getTimestampSnapshot(directory), is(not(before)));
    }

    /**
     * ディレクトリの一覧が取得できない場合、
     * ディレクトリ自体の最終更新日時だけがスナップショットに含まれることのテスト。<br/>
     * 読み取り権限の概念がない環境や、読み取り権限を落としても一覧が取得できる環境（root実行等）では、
     * この観点は検証できないためスキップする。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testGetTimestampSnapshotWhenFilesCanNotBeListed() throws Exception {

        // Windowsの場合、本試験は行わない
        assumeNotWindows();

        // Given: 読み取り不可としたディレクトリ
        File directory = temporaryFolder.newFolder("unreadable");
        createFile(directory, "message.txt");
        assumeTrue("読み取り権限を落とせないためスキップする。", directory.setReadable(false));

        boolean restored;
        try {
            // 読み取り権限を落としても一覧が取得できる環境では、この観点は検証できない
            assumeTrue("ディレクトリの一覧が取得できてしまうためスキップする。", directory.listFiles() == null);

            // When/Then: ディレクトリ自体のエントリだけが含まれる
            Map<String, Long> expected = Collections.singletonMap("", directory.lastModified());
            assertThat(target.getTimestampSnapshot(directory), is(expected));
        } finally {
            // 一時ディレクトリを削除できるよう読み取り権限を戻す。
            // ここで表明すると本来の失敗を隠すため、復元結果はtry文を抜けてから表明する。
            restored = directory.setReadable(true);
        }
        assertTrue("読み取り権限の復元に失敗した。", restored);
    }

    /** Windows以外の環境であることを前提する。 */
    private void assumeNotWindows() {
        assumeThat(System.getProperty("os.name").toLowerCase(), not(containsString("windows")));
    }

    /**
     * 秒未満を切り捨てる。<br/>
     * {@code setLastModified}で設定した値が秒単位に丸められるファイルシステムでも、
     * 設定した値と取得できる値を一致させる必要がある場合に使用する。
     *
     * @param millis エポックからのミリ秒
     * @return 秒単位に切り捨てた値
     */
    private long truncateToSecond(long millis) {
        return millis / 1000L * 1000L;
    }

    /**
     * 最終更新日時を設定する。
     *
     * @param file 対象のファイルまたはディレクトリ
     * @param millis 設定する最終更新日時（エポックからのミリ秒）
     * @return 設定した最終更新日時
     */
    private static long setLastModified(File file, long millis) {
        assertTrue("最終更新日時の設定に失敗した。file=[" + file.getAbsolutePath() + ']',
                file.setLastModified(millis));
        return millis;
    }

    /**
     * 最終更新日時に、現在時刻より確実に後となる未来日時を設定する。
     *
     * @param file 対象のファイル
     */
    private static void setFutureLastModified(File file) {
        assertTrue("最終更新日時の設定に失敗した。file=[" + file.getAbsolutePath() + ']',
                file.setLastModified(System.currentTimeMillis() + FUTURE_OFFSET_MILLIS));
    }

    /**
     * ファイルを作成する。<br/>
     * 親ディレクトリが存在しない場合は併せて作成する。
     *
     * @param directory 親ディレクトリ
     * @param name      ファイル名
     * @return 作成したファイル
     * @throws Exception 予期しない例外
     */
    private File createFile(File directory, String name) throws Exception {
        if (!directory.exists()) {
            assertTrue("ディレクトリの作成に失敗した。", directory.mkdirs());
        }
        File file = new File(directory, name);
        Files.write(file.toPath(), name.getBytes("UTF-8"));
        return file;
    }

    /**
     * ディレクトリ配下の全ファイルの内容と最終更新日時を退避する。
     *
     * @param file ファイルまたはディレクトリ
     * @throws Exception 予期しない例外
     */
    private void saveRecursively(File file) throws Exception {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (File child : files) {
                saveRecursively(child);
            }
            return;
        }
        savedContents.put(file, Files.readAllBytes(file.toPath()));
        savedLastModified.put(file, file.lastModified());
    }

    /**
     * ファイルの内容を上書きする。<br/>
     * ディレクトリのエントリが変化するとディレクトリ自体の最終更新日時が変わってしまうため、
     * ファイルを削除・再作成せずに内容だけを書き換える。
     *
     * @param source 上書きする内容が記載されたファイル
     * @param target 上書き対象のファイル
     * @throws Exception 予期しない例外
     */
    private void overwrite(File source, File target) throws Exception {
        Files.write(target.toPath(), Files.readAllBytes(source.toPath()));
    }

    /**
     * 応答電文が存在しない旨の例外が発生することを表明する。
     *
     * @param requestId リクエストID
     * @param no        応答電文の番号
     */
    private void assertResponseMessageNotExists(String requestId, int no) {
        try {
            target.getResponseMessageByRequestId(DataType.RESPONSE_BODY_MESSAGES, requestId);
            fail("応答電文が存在しないため、例外が発生するはずである。");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString(
                    "receive message did not exists. data type=[RESPONSE_BODY_MESSAGES], "
                            + "request id=[" + requestId + "], no=[" + no + "], "));
            assertThat(e.getMessage(), containsString("resource name=[" + requestId + "/message]."));
        }
    }

    /**
     * リクエストIDと同名のテストデータディレクトリを取得する。
     *
     * @param requestId リクエストID
     * @return テストデータディレクトリ
     */
    private File getTestDataDirectory(String requestId) {
        return FilePathSetting.getInstance()
                .getFileWithoutCreate(SendSyncSupport.SEND_SYNC_TEST_DATA_BASE_PATH, requestId);
    }

    /**
     * リクエストIDと同名のディレクトリ配下のテストデータファイルを取得する。
     *
     * @param requestId    リクエストID
     * @param resourceName ディレクトリからの相対パス
     * @return テストデータファイル
     */
    private File getTestDataFile(String requestId, String resourceName) {
        return new File(getTestDataDirectory(requestId), resourceName);
    }

    /**
     * テストデータの読み込み時に、そのテストデータの最終更新日時を書き換えるリーダ。<br/>
     * 読み込みの最中にテストデータが書き換えられた状況を作るために使用する。<br/>
     * 書き換えは最初の読み込み時のみ行う。毎回書き換えると、
     * 再読み込みが行われたかどうかを次回の読み出しで判別できなくなるためである。
     */
    private static class UpdatingWhileReadingTsvTestDataReader extends TsvTestDataReader {

        /** 書き換える対象のテストデータファイル */
        private final File testDataFile;

        /** 書き換え済みか否か */
        private boolean updated;

        /**
         * コンストラクタ。
         *
         * @param testDataFile 書き換える対象のテストデータファイル
         */
        UpdatingWhileReadingTsvTestDataReader(File testDataFile) {
            this.testDataFile = testDataFile;
        }

        /** {@inheritDoc} */
        @Override
        public void open(String path, String dataName) {
            super.open(path, dataName);
            if (!updated) {
                updated = true;
                setFutureLastModified(testDataFile);
            }
        }
    }
}
