package nablarch.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;

import nablarch.core.util.FileUtil;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * テスト用ユーティリティ。
 *
 * @author Masato Inoue
 */
public class TestUtil {


    /**
     * ディレクトリを空にしてから、ディレクトリを作成する。
     *
     * @param destDir ディレクトリ
     */
    public static void mkDirAfterClean(File destDir) {
        if (!destDir.exists()) {
            destDir.mkdir();
        } else {
            cleanDir(destDir);
            if (!destDir.delete()) {
                throw new RuntimeException("failed to delete directory. path=[" + destDir.getAbsolutePath() + "]");
            }
            destDir.mkdir();
        }
    }

    /**
     * ファイルまたはディレクトリを強制的に削除する。
     * サブディレクトリのファイル・ディレクトリもすべて削除する。
     * 削除に失敗した場合、ログを出力する。
     *
     * @param file 削除するファイル
     */
    private static void forceDelete(File file) {
        if (!file.exists()) {
            // 削除対象のファイルまたはディレクトリが存在しない場合は何もしない
            return;
        }
        if (file.isDirectory()) {
            deleteDir(file);
        } else {
            FileUtil.deleteFile(file);
        }
    }

    /**
     * ディレクトリを削除する。
     *
     * @param dir ディレクトリ
     */
    private static void deleteDir(File dir) {
        cleanDir(dir);
        dir.delete();
    }


    /**
     * ディレクトリ内のファイル・ディレクトリを全削除する。
     *
     * @param dir ディレクトリ
     */
    public static void cleanDir(File dir) {
        if (!dir.exists()) {
            return;
        }
        if (!dir.isDirectory()) {
            throw new RuntimeException("dir is not directory. path=[" + dir + "]");
        }
        File[] fileArray = dir.listFiles();
        for (File file : fileArray) {
            forceDelete(file);
        }
    }

    /**
     * ファイルの内容を文字列に変換する。
     *
     * @param file ファイル
     * @return 文字列
     */
    public static String fileToString(File file) {

        StringBuilder builder = new StringBuilder();
        InputStreamReader inputStream = null;
        try {
            inputStream = new InputStreamReader(new FileInputStream(file));
            char[] charBuffer = new char[1024];
            int length = 0;
            while ((length = inputStream.read(charBuffer)) >= 0) {
                builder.append(charBuffer, 0, length);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            FileUtil.closeQuietly(inputStream);
        }

        return builder.toString();
    }

    /**
     * テスト補助用アサートメソッド。実際の文字列が期待する正規表現にマッチするを表明する。
     *
     * @param expectedRegularExpression 期待値を表す正規表現
     * @param actualString              実際の文字列
     */
    public static void assertMatches(String expectedRegularExpression, String actualString) {
        String msg = "expected is " + expectedRegularExpression + ", but was " + actualString;
        assertTrue(msg, actualString.matches(expectedRegularExpression));
    }

    /**
     * テスト補助用アサートメソッド。実際の文字列が期待する正規表現にマッチしないことを表明する。
     *
     * @param expectedRegularExpression 期待値を表す正規表現
     * @param actualString              実際の文字列
     */
    public static void assertNotMatches(String expectedRegularExpression, String actualString) {
        String msg = "actual matched " + expectedRegularExpression + ". actual =[" + actualString + "]";
        assertFalse(msg, actualString.matches(expectedRegularExpression));
    }

    public static boolean isRunningOnJava7OrHigher(){
        return new BigDecimal(System.getProperty("java.specification.version")).compareTo(new BigDecimal("1.7")) >= 0;
    }
}
