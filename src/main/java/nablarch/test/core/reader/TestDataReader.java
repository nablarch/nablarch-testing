package nablarch.test.core.reader;

import java.util.List;

import nablarch.core.util.annotation.Published;

/**
 * テストデータの読み込みインタフェース。<br>
 *
 * @author Hisaaki Sioiri
 */
@Published(tag = "architect")
public interface TestDataReader {

    /**
     * オープン処理。<br/>
     *
     * @param path ファイル配置ディレクトリのパス
     * @param dataName テストデータ名
     */
    void open(String path, String dataName);

    /**
     * クローズ処理。<br/>
     *
     */
    void close();

    /**
     * 1行データの読み込み処理。<br/>
     * @return 読み込んだ１行データ
     */
    List<String> readLine();

    /**
     * 指定されたパスとリソース名に該当するExcelファイルが存在するか判定する。
     * @param basePath パス
     * @param resourceName リソース名
     * @return 存在する場合、真
     */
    boolean isResourceExisting(String basePath, String resourceName);

    /**
     * 指定されたパスとリソース名に該当するExcelファイルおよびデータシートが存在するか判定する。
     * @param basePath パス
     * @param resourceName リソース名
     * @return 存在する場合、真
     */
    boolean isDataExisting(String basePath, String resourceName);
}
