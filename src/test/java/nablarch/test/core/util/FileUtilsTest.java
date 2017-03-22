package nablarch.test.core.util;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nablarch.test.NablarchTestUtils;
import nablarch.test.TestUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * {@link nablarch.test.core.util.FileUtilsTest}のテストクラス。
 *
 * @author Masato Inoue
 */
public class FileUtilsTest {

    private static File tempDir = new File(System.getProperty("java.io.tmpdir"), "FileUtilsTest");

    /**
     * 初期化時にtmpディレクトリを生成する。
     *
     * @throws IOException
     */
    @BeforeClass
    public static void classUp() throws IOException {
        // tmpディレクトリを削除する
        TestUtil.mkDirAfterClean(tempDir);
    }

    /**
     * 終了時にtmpディレクトリを削除する。
     *
     * @throws IOException
     */
    @AfterClass
    public static void classDown() throws IOException {
        // tmpディレクトリを削除する
        TestUtil.cleanDir(tempDir);
        tempDir.delete();
    }

    /**
     * {@link FileUtils#copyDir(java.io.File, java.io.File, java.io.FileFilter)} のテスト。
     * preserveFileDateがtrueのパターン。
     * コピー元ファイルの最終更新日付を、コピー先のファイルに反映する。
     *
     * @throws IOException
     */
    @Test
    public void testcopyDirPreserveFileDate() throws IOException {

        // コピー元のディレクトリを取得する
        File srcDir = new File("src/test/resources/nablarch/test/core/http/web");

        File destDir = new File(tempDir, "tmp/test_dump");
        TestUtil.mkDirAfterClean(destDir);

        FileUtils.copyDir(srcDir, destDir, new HtmlResourceExtensionFilter());

        // スタイルシートがコピーされることの確認
        assertTrue(new File(destDir, "sample.css").exists());
        assertTrue(new File(destDir, "css/sample01.css").exists());
        assertTrue(new File(destDir, "css/sample02.css").exists());
        // JavaScriptがコピーされることの確認
        assertTrue(new File(destDir, "js/sample.js").exists());
        // 空ディレクトリがコピーされることの確認
        assertTrue(new File(destDir, "action/empty").exists());
        // 階層の深いファイルがコピーされることの確認
        assertTrue(new File(destDir, "action/sample/sample.css").exists());
        assertTrue(new File(destDir, "action/sample/sample.jpg").exists());
        assertTrue(new File(destDir, "action/sample/sample.js").exists());
        // 指定された以外のファイルがコピーされないことの確認
        assertFalse(new File(destDir, "action/sample/sample.log").exists());


        // ファイルの更新時間比較
        assertEquals(new File(srcDir, "sample.css").lastModified(),
                new File(destDir, "sample.css").lastModified());
        assertEquals(new File(srcDir, "css/sample01.css").lastModified(),
                new File(destDir, "css/sample01.css").lastModified());
        assertEquals(new File(srcDir, "css/sample02.css").lastModified(),
                new File(destDir, "css/sample02.css").lastModified());
        assertEquals(new File(srcDir, "js/sample.js").lastModified(),
                new File(destDir, "js/sample.js").lastModified());
        assertEquals(new File(srcDir, "action/empty").lastModified(),
                new File(destDir, "action/empty").lastModified());
        assertEquals(new File(srcDir, "action/sample/sample.css").lastModified(),
                new File(destDir, "action/sample/sample.css").lastModified());
        assertEquals(new File(srcDir, "action/sample/sample.jpg").lastModified(),
                new File(destDir, "action/sample/sample.jpg").lastModified());
        assertEquals(new File(srcDir, "action/sample/sample.js").lastModified(),
                new File(destDir, "action/sample/sample.js").lastModified());

    }

    /**
     * {@link FileUtils#copyDir(java.io.File, java.io.File, java.io.FileFilter)} のテスト。
     *
     * replaceがfalseの場合は、コピー先に同名のファイルがある場合コピーされないこと。
     *
     * @throws IOException
     */
    @Test
    public void testcopyDirNoReplace() throws IOException {

        // コピー元のディレクトリを取得する
        File srcDir = new File("src/test/resources/nablarch/test/core/http/web");

        File destDir = new File(tempDir, "tmp/test_dump");
        TestUtil.mkDirAfterClean(destDir);

        // コピー先に一部のファイルを作成
        String[] existingFilePaths = {"sample.css", "css/sample02.css", "action/sample/sample.js"};
        List<File> existingFiles = new ArrayList<File>(existingFilePaths.length);
        for (String e : existingFilePaths) {
            File file = new File(destDir,e);
            file.getParentFile().mkdirs();
            file.createNewFile();
            existingFiles.add(file);
        }
        // コピー実行
        FileUtils.copyDir(srcDir, destDir, null, false);

        for (File file : existingFiles) {
            assertThat(file.getName() + "はコピーされないのでサイズは0", file.length(), is(0L));
            assertThat(file.getName() + "は存在しているので、コピーされない", file.lastModified(),
                    is(not(new File(srcDir, file.getAbsolutePath()).lastModified())));
        }
        // 以下のファイルはコピーされる。
        String[] copied = {"css/sample01.css", "js/sample.js", "action/sample/sample.css", "action/sample/sample.jpg"};
        for (String e : copied) {
            assertEquals(e + "はコピーされる。",
                    new File(srcDir, e).lastModified(),
                    new File(destDir, e).lastModified());
        }
    }


    /**
     * {@link FileUtils#copyDir(java.io.File, java.io.File, java.io.FileFilter)} のテスト。
     * Filterがnullのパターン。
     * すべてのファイルがコピーされる。
     *
     * @throws IOException 予期しない例外
     */
    @Test
    public void testcopyDirFilterNull() throws IOException {

        // コピー元のディレクトリを取得する
        File srcDir = new File("src/test/resources/nablarch/test/core/http/web");

        File destDir = new File(tempDir, "tmp/test_dump");
        TestUtil.mkDirAfterClean(destDir);

        FileUtils.copyDir(srcDir, destDir, null);

        // スタイルシートがコピーされることの確認
        assertTrue(new File(destDir, "sample.css").exists());
        assertTrue(new File(destDir, "css/sample01.css").exists());
        assertTrue(new File(destDir, "css/sample02.css").exists());
        // JavaScriptがコピーされることの確認
        assertTrue(new File(destDir, "js/sample.js").exists());
        // 空ディレクトリがコピーされることの確認
        assertTrue(new File(destDir, "action/empty").exists());
        // 階層の深いファイルがコピーされることの確認
        assertTrue(new File(destDir, "action/sample/sample.css").exists());
        assertTrue(new File(destDir, "action/sample/sample.jpg").exists());
        assertTrue(new File(destDir, "action/sample/sample.js").exists());
        // 指定された以外のファイルがコピーされることの確認
        assertTrue(new File(destDir, "action/sample/sample.log").exists());


        // ファイルの更新時間比較
        assertTimestampEquals(srcDir, destDir
                , "sample.css"
                , "css/sample01.css"
                , "css/sample02.css"
                , "js/sample.js"
                , "action/empty"
                , "action/sample/sample.css"
                , "action/sample/sample.jpg"
                , "action/sample/sample.js");
    }

    private void assertTimestampEquals(File srcDir, File destDir, String... fileNames) {
        for (String fileName : fileNames) {
            assertEquals(
                    new File(srcDir, fileName).lastModified(),
                    new File(destDir, fileName).lastModified());
        }
    }

    /**
     * {@link FileUtils#copyDir(java.io.File, java.io.File, java.io.FileFilter)} のテスト。
     * コピー元ディレクトリ内のサブディレクトリにコピーするパターン。
     * フィルタあり。
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testcopyDirCopySubDir() throws Exception {

        // tmpディレクトリを生成する
        File srcDir = new File(tempDir, "tmp/parent");
        TestUtil.mkDirAfterClean(srcDir);

        // ディレクトリおよびファイルを生成する
        new File(srcDir, "test/test").mkdirs();
        new File(srcDir, "hoge").mkdirs();
        new File(srcDir, "hoge0.jpg").createNewFile();
        new File(srcDir, "test/hoge0.jpg").createNewFile();
        new File(srcDir, "test/hoge1.jpg").createNewFile();
        new File(srcDir, "test/test/hoge2.jpg").createNewFile();
        new File(srcDir, "test/test/hoge3.txt").createNewFile();
        new File(srcDir, "hoge/hoge5.jpg").createNewFile();
        new File(srcDir, "hoge/hoge6.txt").createNewFile();

        File destDir = new File(tempDir, "tmp/parent/test");
        try {
            FileUtils.copyDir(srcDir, destDir, new HtmlResourceExtensionFilter());
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * {@link FileUtils#copyDir(java.io.File, java.io.File, java.io.FileFilter)} のテスト。
     * src dirがnullのパターン。
     *
     * @throws IOException
     */
    @Test
    public void testcopyDirSrcDirNull() throws IOException {

        // コピー元のディレクトリを取得する
        File destDir = new File(tempDir, "tmp/test_dump");
        TestUtil.mkDirAfterClean(destDir);

        try {
            FileUtils.copyDir(null, destDir, new HtmlResourceExtensionFilter());
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * {@link FileUtils#copyDir(java.io.File, java.io.File, java.io.FileFilter)} のテスト。
     * dest dirがnullのパターン。
     *
     * @throws IOException
     */
    @Test
    public void testcopyDirDestDirNull() throws IOException {

        // コピー元のディレクトリを取得する
        File srcDir = new File(tempDir, "testcopyDirDestDirNull");
        srcDir.mkdirs();

        try {
            FileUtils.copyDir(srcDir, null, new HtmlResourceExtensionFilter());
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * {@link FileUtils#copyDir(java.io.File, java.io.File, java.io.FileFilter)} のテスト。
     * src dirが存在しないパターン。
     *
     * @throws IOException
     */
    @Test
    public void testcopyDirSrcDirNonExists() throws IOException {

        // 存在しないディレクトリを指定する
        File nonExistsPath = new File(tempDir, "tmpNonexitsPath");

        File destDir = new File(tempDir, "tmp/test_dump");
        TestUtil.mkDirAfterClean(destDir);

        try {
            FileUtils.copyDir(nonExistsPath, destDir, new HtmlResourceExtensionFilter());
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * {@link FileUtils#copyDir(java.io.File, java.io.File, java.io.FileFilter)} のテスト。
     * src dirがディレクトリでないパターン。
     *
     * @throws IOException
     */
    @Test
    public void testcopyDirSrcDirNonDir() throws IOException {

        // コピー元のディレクトリを取得する
        File srcDir = new File(tempDir, "testcopyDirSrcDirNonDir");
        srcDir.mkdirs();
        // ファイルを指定する
        File file = new File(srcDir, "sample.css");
        new FileWriter(file).append('a').close();

        File destDir = new File(tempDir, "tmp/test_dump");
        TestUtil.mkDirAfterClean(destDir);

        try {
            FileUtils.copyDir(file, destDir, new HtmlResourceExtensionFilter());
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    /**
     * {@link FileUtils#copyDir(java.io.File, java.io.File, java.io.FileFilter)} のテスト。
     * src dirとdest dirが同一のディレクトリのパターン。
     *
     * @throws IOException
     */
    @Test
    public void testcopyDirSameDir() throws IOException {

        // コピー元のディレクトリを取得する
        File srcDir = new File(tempDir, "resources/nablarch/test/core/http/web");

        try {
            FileUtils.copyDir(srcDir, srcDir, new HtmlResourceExtensionFilter());
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }


    /**
     * {@link FileUtils#copyDir(java.io.File, java.io.File, java.io.FileFilter)} のテスト。
     * ディレクトリをファイルにコピーしようとしたパターン。
     *
     * @throws IOException
     */
    @Test
    public void testcopyDirDirToFile() throws IOException {

        // コピー元のディレクトリを取得する
        File srcDir = new File(tempDir, "resources/nablarch/test/core/http/web");

        File destDir = new File(tempDir, "tmp");
        TestUtil.mkDirAfterClean(destDir);

        // actionという名のファイルを生成しておく
        new File(destDir, "action").createNewFile();

        try {
            FileUtils.copyDir(srcDir, destDir, new HtmlResourceExtensionFilter());
            fail();
        } catch (RuntimeException e) {
            assertTrue(true);
        }
    }


    /**
     * {@link FileUtils#copyDir(java.io.File, java.io.File, java.io.FileFilter)} のテスト。
     * ディレクトリを生成できないパターン。
     *
     * @throws IOException
     */
    @Test
    public void testcopyDirCannotMakeDestDir() throws IOException {

        // Windowsの場合、本試験は行わない
        assumeNotWindows();

        // コピー元のディレクトリを取得する
        File srcDir = new File(tempDir, "src/test/resources/nablarch/test/core/http/web");

        File tmpDir = new File(tempDir, "tmp");
        TestUtil.mkDirAfterClean(tmpDir);
        File readOnlyDir = new File(tmpDir, "notWritable");
        readOnlyDir.mkdir();
        readOnlyDir.setReadOnly();

        File destDir = new File(readOnlyDir, "hoge");

        try {
            FileUtils.copyDir(srcDir, destDir, new HtmlResourceExtensionFilter());
            fail();
        } catch (RuntimeException e) {
            assertTrue(true);
        } finally {
            // readOnlyDir.setReadable(true);
        }
    }

    /**
     * {@link FileUtils#copyDir(java.io.File, java.io.File, java.io.FileFilter)} のテスト。
     * ディレクトリが書き込み不可能なパターン。
     *
     * @throws IOException
     */
    @Test
    public void testcopyDirCannotWriteDestDir() throws IOException {

        // Windowsの場合、本試験は行わない
        assumeNotWindows();

        // コピー元のディレクトリを取得する
        File srcDir = new File(tempDir, "src/test/resources/nablarch/test/core/http/web");

        File tmpDir = new File(tempDir, "tmp");
        TestUtil.mkDirAfterClean(tmpDir);
        File destDir = new File(tmpDir, "notWritable");
        destDir.mkdir();
        destDir.setReadOnly();

        try {
            FileUtils.copyDir(srcDir, destDir, new HtmlResourceExtensionFilter());
            fail();
        } catch (RuntimeException e) {
            assertTrue(true);
        }
    }

    /**
     * {@link FileUtils#copyDir(java.io.File, java.io.File, java.io.FileFilter)} のテスト。
     * 不正なパスが指定されたパターン。(WINDOWS環境の場合のみ、本試験は行われる）
     *
     */
    @Test
    public void testcopyDirCannotGetCanonicalPath() {

        // Windows環境でない場合は終了する
        assumeThat(getOsName(), containsString("windows"));

        // コピー元のディレクトリを取得する
        File srcDir = new File("src/test/resources/nablarch/test/core/http/web");

        File tmpDir = new File(tempDir, "tmp");
        TestUtil.mkDirAfterClean(tmpDir);
        File destDir = new File("???");

        try {
            FileUtils.copyDir(srcDir, destDir, new HtmlResourceExtensionFilter());
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                return;
            }
            throw e;
        }
        fail();
    }

    /**
     * プライベートコンストラクタのテスト
     */
    @Test
    public void testPrivateConstructor() {
        NablarchTestUtils.invokePrivateDefaultConstructor(FileUtils.class);
    }

    /**
     *
     * @throws IOException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMkdirFail_ExistsAndNotDir() throws IOException {
        File file = File.createTempFile(this.getClass().getName(), null);
        file.deleteOnExit();
        FileUtils.mkdir(file);
    }

    /**
     * OS名を取得する。
     * @return OS名
     */
    private String getOsName() {
        return System.getProperty("os.name").toLowerCase();
    }

    /** Windows以外の環境であることを前提する。 */
    private void assumeNotWindows() {
        assumeThat(getOsName(), not(containsString("windows")));
    }

    private List<String> htmlResourcesExtensionList = Arrays.asList("css", "js", "jpg");

    /**
     * コピー対象となるHTMLリソースの拡張子を指定するフィルタ。
     * Filterが正しく使用されていることの確認のため使用する。
     */
    protected class HtmlResourceExtensionFilter implements FileFilter {

        /**
         * 指定された拡張子を持つファイルと、全ディレクトリをコピー対象とする。
         *
         * @param file ファイルまたはディレクトリのオブジェクト。
         * @return コピー対象の場合trueを
         */
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            }

            for (String extension : htmlResourcesExtensionList) {
                if (file.getName().endsWith("." + extension)) {
                    return true;
                }
            }
            return false;
        }
    }

}
