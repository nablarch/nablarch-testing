package nablarch.test.core.messaging;

import java.io.File;
import java.nio.file.Files;
import java.util.Date;

import org.junit.Rule;
import org.junit.Test;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.util.FilePathSetting;
import nablarch.test.core.reader.DataType;
import nablarch.test.support.SystemRepositoryResource;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link SendSyncSupport}のテストクラス。
 *
 * @author kiyohito ito
 */
public class SendSyncSupportTest {

    /** テストデータがディレクトリ配下に配置される構成（sendSyncTestDataに拡張子未設定）の設定 */
    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
            "nablarch/test/core/messaging/directory/directory-component-configuration.xml");

    /** テスト対象のテストデータのリクエストID */
    private static final String REQUEST_ID = "RM11AD0201";

    /** テストデータのリソース名 */
    private static final String RESOURCE_NAME = "message.txt";

    /**
     * テストデータがディレクトリの場合（sendSyncTestDataに拡張子が設定されていない場合）のテスト。<br/>
     * <ul>
     *   <li>応答電文の数を上回りエラーとなった場合、何回目のリクエストでエラーとなったかがわかること</li>
     *   <li>ディレクトリ配下のテストデータを更新した場合は1つ目の応答電文から再読み込みされエラーとならないこと</li>
     *   <li>再読み込みした場合、テストデータの変更内容が応答電文に反映されること</li>
     * </ul>
     */
    @Test
    public void testReloadTestDataInDirectory() throws Exception {

        // Given: 応答電文が1件だけ記載されたテストデータを、リクエストIDと同名のディレクトリ配下に配置する
        File testDataFile = getTestDataFile(REQUEST_ID);
        overwrite(getTestDataFile(REQUEST_ID + "_original"), testDataFile);

        SendSyncSupport target = new SendSyncSupport();

        // When/Then: 1回目は1件目の応答電文が取得できる
        DataRecord first = target.getResponseMessageByRequestId(DataType.RESPONSE_BODY_MESSAGES, REQUEST_ID);
        assertThat(first.getString("failureCode"), is("test2"));

        // When/Then: 2回目以降は応答電文が存在しないため、何回目でエラーとなったかがわかる例外が発生する
        assertResponseMessageNotExists(target, 2);
        assertResponseMessageNotExists(target, 3);

        // When: ディレクトリ配下のテストデータファイルの内容を書き換える
        overwrite(getTestDataFile(REQUEST_ID + "_updated"), testDataFile);

        // CI環境(Unix環境)でも確実にテストが通るように、 lastModified を手動で設定している。
        // Unix環境では lastModified() が秒の精度でしか得られない制限がある。
        // このため、テストの先頭でオリジナルファイルを書き込んでから内容を置き換えるまでの時間が1秒未満だと、
        // lastModified() の値が変化せずタイムスタンプが変わった場合のテストが正常に実施できない。
        // この問題を回避するため、ここで明示的に1秒より先の未来時間を lastModified に設定している。
        testDataFile.setLastModified(new Date().getTime() + 2000);

        // Then: 例外が発生せず、更新後の1件目の応答電文が取得できる（再読み込みされている）
        DataRecord reloaded = target.getResponseMessageByRequestId(DataType.RESPONSE_BODY_MESSAGES, REQUEST_ID);
        assertThat(reloaded.getString("failureCode"), is("test4"));
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
     * @param target テスト対象
     * @param no     応答電文の番号
     */
    private void assertResponseMessageNotExists(SendSyncSupport target, int no) {
        try {
            target.getResponseMessageByRequestId(DataType.RESPONSE_BODY_MESSAGES, REQUEST_ID);
            fail("応答電文が存在しないため、例外が発生するはずである。");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString(
                    "receive message did not exists. data type=[RESPONSE_BODY_MESSAGES], "
                            + "request id=[" + REQUEST_ID + "], no=[" + no + "], "));
            assertThat(e.getMessage(), containsString("resource name=[" + REQUEST_ID + "/message]."));
        }
    }

    /**
     * リクエストIDと同名のディレクトリ配下のテストデータファイルを取得する。
     *
     * @param requestId リクエストID
     * @return テストデータファイル
     */
    private File getTestDataFile(String requestId) {
        File directory = FilePathSetting.getInstance()
                .getFileWithoutCreate(SendSyncSupport.SEND_SYNC_TEST_DATA_BASE_PATH, requestId);
        return new File(directory, RESOURCE_NAME);
    }
}
