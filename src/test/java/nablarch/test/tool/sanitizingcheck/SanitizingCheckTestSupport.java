package nablarch.test.tool.sanitizingcheck;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

/**
 * サニタイジングチェックツールテストのサポートクラス
 * 
 * @author Tomokazu Kagawa
 * @see SanitizingChecker
 */
public abstract class SanitizingCheckTestSupport {

    /**
     * 行番号と列番号より文字列の位置を計算する。
     * 
     * @param jsp 解析対象JSPの文字列
     * @param line 行番号
     * @param column 列番号
     * @return 文字列の位置
     */
    protected static int calcPoint(String jsp, int line, int column) {
        String lineSeparator = System.getProperty("line.separator");

        int index = -lineSeparator.length();
        for (int i = 0; i < line - 1; i++) {
            index = jsp.indexOf(lineSeparator, index + 1);
        }
        return index + lineSeparator.length() + column - 1;
    }


    /**
     * ファイルを読み込みファイル内容を文字列に変換する。
     * 改行文字もスキップしないで文字列に追加する。
     * 
     * @param filePath 読み込み対象ファイルパス
     * @return 読み込んだファイル内容を文字列にしたインスタンス
     */
    protected static String readFileToString(String filePath) {

        File file = new File(filePath);

        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();

        try {
            reader = open(file, Charset.forName("UTF-8"));

            int c;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
            }

        } catch (IOException e) {
            throw new RuntimeException("can't read file [" + file + "]", e);
        } finally {
            closeQuietly(reader);
        }

        return sb.toString();
    }
    
    /**
     * 指定したファイルのBufferedReaderを作成し、返却する。
     * 
     * @param file ファイル指定
     * @param charset 文字コード指定
     * @return 指定したファイルのBufferedReader
     * @throws FileNotFoundException ファイルが存在しなかった場合に生じる例外
     */
    protected static BufferedReader open(File file, Charset charset) throws FileNotFoundException {
        InputStream in = new FileInputStream(file);
        return new BufferedReader(new InputStreamReader(in, charset));
    }

    /**
     * Closeableインタフェースの実装クラスをクローズする。
     * 
     * @param closeable クローズ対象インスタンス
     */
    protected static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception ignored) {
            // 例外をthrowしても、ユーザーは対処できない。
            // そのため、処理を行わない。
            return;
        }
    }
    
    /**
     * ファイル内容を比較する。
     * 
     * @param expectedFilePath 期待するファイルのパス
     * @param actualFilePath 実際のファイルのパス
     */
    protected void assertFile(String expectedFilePath, String actualFilePath) {

        String expected = readFileToString(expectedFilePath);
        String actual   = readFileToString(actualFilePath).replace("\r\n", "\n");

        assertEquals(expected, actual);
    }
}
