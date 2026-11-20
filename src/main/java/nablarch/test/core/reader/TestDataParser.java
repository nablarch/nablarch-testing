package nablarch.test.core.reader;


import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.TableData;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.List;
import java.util.Map;

import nablarch.core.util.annotation.Published;


/**
 * テストデータ取得インターフェース。
 *
 * @author Hisaaki Sioiri
 */
@Published(tag = "architect")
public interface TestDataParser {

    /**
     * 期待するテーブルデータを取得する。<br/>
     *
     * @param path         取得元パス
     * @param resourceName 取得元データリソース名
     * @param groupId      グループID（オプション）
     * @return 期待するテーブルデータを取得する。<br/>
     */
    List<TableData> getExpectedTableData(String path, String resourceName, String... groupId);


    /**
     * 準備用のテーブルデータを取得する。<br/>
     *
     * @param path         取得元パス
     * @param resourceName 取得元データリソース名
     * @param groupId      グループID（オプション）
     * @return 準備用のテーブルデータ
     */
    List<TableData> getSetupTableData(String path, String resourceName, String... groupId);

    /**
     * List-Map形式でデータを取得する。<br/>
     *
     * @param path         取得元パス
     * @param resourceName 取得元データリソース名
     * @param id           ID
     * @return List-Map形式のデータ
     */
    List<Map<String, String>> getListMap(String path, String resourceName, String id);


    /**
     * 準備用の固定長ファイルデータを取得する。
     *
     * @param path         取得元パス
     * @param resourceName 取得元データリソース名
     * @param groupId      グループID（オプション）
     * @return 固定長ファイル
     */
    List<DataFile> getSetupFile(String path, String resourceName, String... groupId);

    /**
     * 期待する固定長ファイルデータを取得する。
     *
     * @param path         取得元パス
     * @param resourceName 取得元データリソース名
     * @param groupId      グループID（オプション）
     * @return 固定長ファイル
     */
    List<DataFile> getExpectedFile(String path, String resourceName, String... groupId);


    /**
     * メッセージ用に固定長ファイルデータを取得する。
     *
     * @param path         取得元パス
     * @param resourceName 取得元データリソース名
     * @param id           ID
     * @return 固定長ファイル
     */
    MessagePool getMessage(String path, String resourceName, String id);

    /**
     * テストデータリーダを設定する。<br>
     *
     * @param testDataReader テストデータリーダ
     */
    void setTestDataReader(TestDataReader testDataReader);


    /**
     * DbInfoを設定する。
     *
     * @param dbInfo DbInfo
     */
    void setDbInfo(DbInfo dbInfo);

    /**
     * テストデータの解釈クラスを設定する。
     *
     * @param interpreter 解釈クラス
     */
    void setInterpreters(List<TestDataInterpreter> interpreter);

    /**
     * 指定されたパスとリソース名に該当するデータコンテナが存在するか判定する。
     * <p>
     * データコンテナは、リソース名の最後の{@code "/"}より前の部分を名前とする、テストデータの入れ物である
     * （Excel形式では{@code <basePath>/<名前>.xls}または{@code .xlsx}のファイル）。
     * データコンテナ内のデータセクション（Excel形式ではシート）が存在するかは判定しない。
     * </p>
     *
     * @param basePath パス
     * @param resourceName リソース名
     * @return 存在する場合、真
     */
    boolean isResourceExisting(String basePath, String resourceName);
}
