package nablarch.test.core.reader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import nablarch.core.util.StringUtil;

/**
 * タブ区切りテキストからテストデータを読み込むテスト用の{@link TestDataReader}実装クラス。<br/>
 * <p>
 * テストデータは「ベースパス/データ名.txt」に配置する。
 * データ名は「リクエストID/シート名」となるため、テストデータはリクエストIDと同名のディレクトリ配下に配置される。
 * これは、ベースパスに拡張子を設定しない構成（YAML形式のテストデータ）と同じ配置である。
 * </p>
 *
 * @author kiyohito ito
 */
public class TsvTestDataReader implements TestDataReader {

    /** テストデータファイルの拡張子 */
    private static final String EXTENSION = ".txt";

    /** テストデータファイルの文字エンコーディング */
    private static final Charset CHARSET = Charset.forName("UTF-8");

    /** 列の区切り文字 */
    private static final Pattern COLUMN_SEPARATOR = Pattern.compile("\t");

    /** 読み込んだ行のイテレータ */
    private Iterator<String> lines;

    /** {@inheritDoc} */
    public void open(String path, String dataName) {
        if (StringUtil.isNullOrEmpty(dataName)) {
            throw new IllegalArgumentException("dataName must not be null or empty.");
        }
        File file = toFile(path, dataName);
        try {
            lines = Files.readAllLines(file.toPath(), CHARSET).iterator();
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "test data file was not readable. path=[" + file.getAbsolutePath() + ']', e);
        }
    }

    /** {@inheritDoc} */
    public void close() {
        lines = null;
    }

    /** {@inheritDoc} */
    public List<String> readLine() {
        if (!lines.hasNext()) {
            return null;
        }
        // 行末の空セルも列として扱うため、limitに負数を指定する。
        return Arrays.asList(COLUMN_SEPARATOR.split(lines.next(), -1));
    }

    /** {@inheritDoc} */
    public boolean isResourceExisting(String basePath, String resourceName) {
        return toFile(basePath, resourceName).exists();
    }

    /** {@inheritDoc} */
    public boolean isDataExisting(String basePath, String resourceName) {
        return isResourceExisting(basePath, resourceName);
    }

    /**
     * テストデータファイルを取得する。
     *
     * @param path     ファイル配置ディレクトリのパス
     * @param dataName テストデータ名
     * @return テストデータファイル
     */
    private File toFile(String path, String dataName) {
        return new File(path, dataName + EXTENSION);
    }
}
