package nablarch.test.core.messaging;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.util.FilePathSetting;
import nablarch.test.core.reader.DataType;
import nablarch.test.support.SystemRepositoryResource;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    /** テストデータのリソース名（リクエストIDと同名のディレクトリからの相対パス） */
    private static final String RESOURCE_NAME = "message.txt";

    /** サブディレクトリ配下に配置したファイルのパス（リクエストIDと同名のディレクトリからの相対パス） */
    private static final String NESTED_RESOURCE_NAME = "sub/nested.txt";

    /**
     * 最終更新日時に設定する未来方向のオフセット（ミリ秒）。
     * <p>
     * Unix環境では{@code lastModified()}が秒の精度でしか得られない制限があるため、
     * ファイルを書き換えてから最終更新日時を確認するまでの時間が1秒未満だと値が変化しない。
     * これを回避するため、1秒より先の未来を明示的に設定する。
     * </p>
     */
    private static final long FUTURE_OFFSET_MILLIS = 2000L;

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
    }

    /**
     * 退避しておいたテストデータファイルの内容と最終更新日時を復元する。
     *
     * @throws Exception 予期しない例外
     */
    @After
    public void restoreTestData() throws Exception {
        for (Map.Entry<File, byte[]> entry : savedContents.entrySet()) {
            File file = entry.getKey();
            Files.write(file.toPath(), entry.getValue());
            assertTrue("テストデータの最終更新日時の復元に失敗した。file=[" + file.getAbsolutePath() + ']',
                    file.setLastModified(savedLastModified.get(file)));
        }
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
    }

    /**
     * テストデータがディレクトリでない場合（Excel形式の場合）、
     * ファイル自体の最終更新日時がそのまま返却されることのテスト。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testGetTimestampSignatureReturnsLastModifiedWhenNotDirectory() throws Exception {

        // Given: ディレクトリではないテストデータ（ベースパスに拡張子が設定されている構成）
        File file = temporaryFolder.newFile("message.xls");
        assertTrue(file.setLastModified(System.currentTimeMillis() - FAR_OFFSET_MILLIS));

        // When
        long actual = target.getTimestampSignature(file);

        // Then: 最終更新日時と完全に一致する（Excel形式の挙動は修正前と同一）
        assertThat(actual, is(file.lastModified()));
    }

    /**
     * 配下にファイルが存在しないディレクトリの場合、
     * ディレクトリ自体の最終更新日時が返却されることのテスト。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testGetTimestampSignatureForEmptyDirectory() throws Exception {

        // Given: 配下にファイルが1件も存在しないディレクトリ
        File directory = temporaryFolder.newFolder("empty");

        // When/Then: ディレクトリ自体の最終更新日時が返却される
        assertThat(target.getTimestampSignature(directory), is(directory.lastModified()));
    }

    /**
     * サブディレクトリを含むディレクトリの場合、
     * サブディレクトリ配下のファイルの最終更新日時が変わると署名も変わることのテスト。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testGetTimestampSignatureChangesWhenFileInSubDirectoryIsUpdated() throws Exception {

        // Given: サブディレクトリ配下にファイルを持つディレクトリ
        File directory = temporaryFolder.newFolder("tree");
        File nestedFile = createFile(new File(directory, "sub"), "nested.txt");
        long before = target.getTimestampSignature(directory);

        // When: サブディレクトリ配下のファイルの最終更新日時だけを変更する
        setFutureLastModified(nestedFile);

        // Then: 署名が変化する（ディレクトリ直下しか走査しない実装では変化しない）
        assertThat(target.getTimestampSignature(directory), is(not(before)));
    }

    /**
     * 内容が変わっていないディレクトリに対して繰り返し呼び出しても、
     * 同じ署名が返却されることのテスト。<br/>
     * {@link java.io.File#listFiles()}の順序は保証されないため、
     * 走査順に依存する実装では呼び出しのたびに署名が変わり、毎回再読み込みが走ることになる。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testGetTimestampSignatureIsStableForSameDirectory() throws Exception {

        // Given: 複数のファイルとサブディレクトリを持つディレクトリ
        File directory = temporaryFolder.newFolder("stable");
        long now = System.currentTimeMillis();
        int index = 0;
        for (String name : new String[] {"c.txt", "a.txt", "b.txt", "d.txt", "e.txt"}) {
            File file = createFile(directory, name);
            // ファイルごとに異なる最終更新日時を設定し、走査順の違いが署名に現れるようにする
            assertTrue(file.setLastModified(now - FAR_OFFSET_MILLIS + (index++ * FUTURE_OFFSET_MILLIS)));
        }
        createFile(new File(directory, "sub"), "nested.txt");

        // When: 内容を変えずに繰り返し取得する
        long first = target.getTimestampSignature(directory);
        long second = target.getTimestampSignature(directory);
        long third = target.getTimestampSignature(directory);

        // Then: いずれも同じ値となる
        assertThat(second, is(first));
        assertThat(third, is(first));
    }

    /**
     * 未来日時の最終更新日時を持つファイルが配下に存在する場合でも、
     * 他のファイルの更新が検知できることのテスト。<br/>
     * 配下の最大値を採る方式では、最大値が未来日時のファイルに張り付いてしまい、
     * 以降どのファイルを書き換えても値が変化せず再読み込みが行われなくなる。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testGetTimestampSignatureDetectsUpdateUnderFutureTimestampedFile() throws Exception {

        // Given: 未来日時の最終更新日時を持つファイルと、それより古いファイルを含むディレクトリ
        File directory = temporaryFolder.newFolder("future");
        long now = System.currentTimeMillis();
        File futureFile = createFile(directory, "future.txt");
        assertTrue(futureFile.setLastModified(now + FAR_OFFSET_MILLIS));
        File updatedFile = createFile(directory, "updated.txt");
        assertTrue(updatedFile.setLastModified(now - FAR_OFFSET_MILLIS));

        long signatureBefore = target.getTimestampSignature(directory);
        long maxBefore = maxLastModified(directory);

        // When: 未来日時のファイルより古い範囲で、別のファイルの最終更新日時を変更する
        assertTrue(updatedFile.setLastModified(now - FAR_OFFSET_MILLIS + FUTURE_OFFSET_MILLIS));

        // Then: 署名は変化する
        assertThat(target.getTimestampSignature(directory), is(not(signatureBefore)));
        // Then: 最大値は未来日時のファイルに張り付いたまま変化しない（最大値方式では検知できない）
        assertThat(maxLastModified(directory), is(maxBefore));
    }

    /**
     * ディレクトリの一覧が取得できない場合、
     * ディレクトリ自体の最終更新日時が返却されることのテスト。<br/>
     * 読み取り権限を落としても一覧が取得できてしまう環境（root実行等）ではスキップする。
     *
     * @throws Exception 予期しない例外
     */
    @Test
    public void testGetTimestampSignatureWhenFilesCanNotBeListed() throws Exception {

        // Given: 読み取り不可としたディレクトリ
        File directory = temporaryFolder.newFolder("unreadable");
        createFile(directory, "message.txt");
        assertTrue("読み取り権限の変更に失敗した。", directory.setReadable(false));
        try {
            // 読み取り権限を落としても一覧が取得できる環境では、この観点は検証できない
            Assume.assumeTrue("ディレクトリの一覧が取得できてしまうためスキップする。", directory.listFiles() == null);

            // When/Then: ディレクトリ自体の最終更新日時が返却される
            assertThat(target.getTimestampSignature(directory), is(directory.lastModified()));
        } finally {
            // 一時ディレクトリを削除できるよう、読み取り権限を必ず戻す
            assertTrue("読み取り権限の復元に失敗した。", directory.setReadable(true));
        }
    }

    /**
     * ディレクトリ配下の全ファイルの最終更新日時の最大値を取得する。<br/>
     * 最大値を採る方式との差異を示すために使用する。
     *
     * @param file ファイルまたはディレクトリ
     * @return 最終更新日時の最大値
     */
    private long maxLastModified(File file) {
        long max = file.lastModified();
        File[] files = file.listFiles();
        if (files == null) {
            return max;
        }
        for (File child : files) {
            max = Math.max(max, maxLastModified(child));
        }
        return max;
    }

    /**
     * 最終更新日時に、現在時刻より確実に後となる未来日時を設定する。
     *
     * @param file 対象のファイル
     */
    private void setFutureLastModified(File file) {
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
}
