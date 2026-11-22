package nablarch.test.core.db;

import nablarch.core.db.connection.TransactionManagerConnection;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.StringUtil;
import nablarch.test.NablarchTestUtils;
import nablarch.test.core.reader.BasicTestDataParser;
import nablarch.test.core.reader.PoiXlsReader;
import nablarch.test.core.reader.TestDataParser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nablarch.core.util.Builder.concat;

/**
 * マスタデータ投入クラス。
 * <p>
 * 最初に、処理対象テーブルのデータ削除（DELETE）を行う。
 * その後、処理対象テーブルのデータ投入（INSERT）を行う。
 * 同一のテーブルのデータが複数含まれる場合は、
 * 先のデータが投入され、後のデータが追加される（一意性制約違反が無い場合）。
 * </p>
 *
 * @author T.Kawasaki
 */
public class MasterDataSetUpper {

    /** マスタデータファイルのパスのList */
    private final List<File> masterDataFiles;

    /** バックアップスキーマ名 */
    private final String backUpSchemaName;

    /** セットアップ完了テーブル */
    private Set<String> tablesFinished = new HashSet<String>();

    /**
     * コンストラクタ
     * @param masterDataFiles マスタデータファイルパス
     * @param backUpSchema バックアップスキーマ名
     */
    public MasterDataSetUpper(List<File> masterDataFiles, String backUpSchema) {
        this.masterDataFiles = masterDataFiles;
        this.backUpSchemaName = backUpSchema;
    }

    /**
     * メインメソッド
     * <ol>
     * <li>コンポーネント設定ファイルのパス(任意)
     * <li>マスターデータファイルのパス(任意)
     * </ol>
     *
     * @param args プログラム引数
     */
    @SuppressWarnings("fallthrough")
    public static void main(String... args) {

        if (args.length < 2) {
            throw new IllegalArgumentException(
                    "program arguments must be greater than two. but was "
                            + Arrays.toString(args));
        }
        // コンポーネント設定ファイル
        String configFilePath = args[0];

        // 2番目以降の起動引数をマスタデータファイルとして扱う
        List<File> masterDataFiles = new ArrayList<File>();
        String backUpSchema = null;
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("--backUpSchema:")) {
                backUpSchema = args[i].substring("--backUpSchema:".length());
                break;
            }
            File file = new File(args[i]);
            if (!file.exists()) {
                throw new IllegalArgumentException("master data file was not found. specified master data file = [" + args[i] + "]");
            }
            masterDataFiles.add(file);
        }

        initializeRepository(configFilePath);  // 自動テスト全実行用
        MasterDataSetUpper masterDataSetUpper = new MasterDataSetUpper(masterDataFiles, backUpSchema);
        masterDataSetUpper.setUpMasterData();
    }
    
    /**
     * リポジトリの初期化を行う。
     * @param configFilePath コンポーネント設定ファイル
     */
    private static void initializeRepository(String configFilePath) {
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(configFilePath);
        SystemRepository.clear();
        SystemRepository.load(new DiContainer(loader));
    }

    /** マスタデータ投入を行う。 */
    public void setUpMasterData() {
        List<TableData> all = getAllTableDataInFiles(masterDataFiles);
        replaceAll(all);

        for (TableData table : all) {
            tablesFinished.add(table.getTableName());
        }

        if (!StringUtil.isNullOrEmpty(backUpSchemaName)) {
            copyDataToBackUpSchema(backUpSchemaName);
        }
    }

    private List<TableData> getAllTableDataInFiles(List<File> files) {
        List<TableData> all = new ArrayList<TableData>();
        for (File file : files) {
            all.addAll(getAllTableData(file));
        }
        return all;
    }

    /**
     * バックアップ用スキーマにデータをコピーする
     * @param backUpSchemaName コピー先のバックアップスキーマ名
     */
    private void copyDataToBackUpSchema(String backUpSchemaName) {
        String defaultSchema = "";
        MasterDataRestorer.TableDuplicator duplicator =
                new MasterDataRestorer.TableDuplicator(tablesFinished, defaultSchema, backUpSchemaName);
        duplicator.restoreAll();
    }

    /**
     * テーブルデータの置き換えを行う。
     *
     * @param allTables 置き換えるテーブル
     */
    void replaceAll(final List<TableData> allTables) {
        new TransactionTemplateInternal(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST_FW) {
            @Override
            protected void doInTransaction(TransactionManagerConnection conn) {
                deleteAll(allTables, conn);
                insertAll(allTables, conn);
            }
        }.execute();
    }

    /**
     * テーブルデータを投入する。
     *
     * @param allTables 置き換えるテーブル
     */
    private void insertAll(List<TableData> allTables, TransactionManagerConnection tranConn) {
        final List<TableData> sorted = TableDataSorter.sort(allTables, tranConn);
        for (TableData table : sorted) {
            table.insertData(tranConn);
        }
    }

    /**
     * テーブルのデータを削除する。
     *
     * @param allTables 置き換えるテーブル
     */
    private void deleteAll(List<TableData> allTables, TransactionManagerConnection tranConn) {
        List<TableData> reversed = TableDataSorter.reversed(allTables, tranConn);
        for (TableData table : reversed) {
            table.deleteData(tranConn);
        }
    }


    /**
     * 全テーブルデータを取得する。<br/>
     * Excel形式（{@code .xls}/{@code .xlsx}）のマスタデータファイルの場合は、全シートを走査しテーブルデータを取得する。
     * それ以外の形式（YAML形式等）の場合は、ファイル全体を1リソースとしてテーブルデータを取得する。
     * <p>
     * テストデータのパーサが{@link BasicTestDataParser}の場合は、読み込む前に、パーサが読み込む単位
     * （データセクション。Excel形式ではファイル内のシート）が1つでも存在するかを判定する。
     * テーブルを1つも記述していないファイルでもデータセクションは存在するため、テーブルデータが0件でも例外にはならない。
     * </p>
     *
     * @param masterDataFile マスタデータファイル
     * @return マスタデータファイル内の全テーブルデータ
     * @throws IllegalArgumentException パーサが読み込むデータセクションが1つも存在しない場合
     *                                  （ファイルの形式・拡張子と、設定したパーサが食い違っている場合など）
     */
    List<TableData> getAllTableData(File masterDataFile) {
        String dir = getMasterDataDir(masterDataFile);
        String fileNameWithoutSuffix = getMasterFileNameWithoutSuffix(masterDataFile);
        TestDataParser parser = SystemRepository.get("testDataParser");

        List<String> resourceNames = new ArrayList<String>();
        if (isExcelFile(masterDataFile)) {
            for (String sheet : PoiXlsReader.getSheetNames(masterDataFile)) {
                resourceNames.add(concat(fileNameWithoutSuffix, "/", sheet));
            }
        } else {
            // Excel形式以外はシートの概念が無いため、ファイル全体を1リソースとして問い合わせる。
            // 拡張子はリーダ側で付与されるため、拡張子を除いた名前を渡す。
            resourceNames.add(fileNameWithoutSuffix);
        }

        verifyDataSectionExisting(parser, masterDataFile, dir, resourceNames);

        List<TableData> allTables = new ArrayList<TableData>();
        for (String resourceName : resourceNames) {
            allTables.addAll(parser.getSetupTableData(dir, resourceName));
        }
        return allTables;
    }

    /**
     * マスタデータファイルについて、パーサが読み込むデータセクションが1つでも存在することを検証する。
     * <p>
     * ファイルの形式は拡張子で、読み込むパーサはコンポーネント設定で別々に決まるため、両者が食い違うことがある。
     * パーサはデータが無ければ空を返すため、食い違っても読み込み結果からは気付けない。
     * そのため、読み込む前にデータセクションの有無を判定する。
     * 判定の手段は{@link BasicTestDataParser}にしか無いため、それ以外のパーサでは判定しない。
     * </p>
     *
     * @param parser テストデータのパーサ
     * @param masterDataFile マスタデータファイル
     * @param dir マスタデータファイルの存在するディレクトリ
     * @param resourceNames マスタデータファイル内のリソース名
     * @throws IllegalArgumentException データセクションが1つも存在しない場合
     */
    private void verifyDataSectionExisting(TestDataParser parser, File masterDataFile, String dir,
            List<String> resourceNames) {
        if (!(parser instanceof BasicTestDataParser)) {
            return;
        }
        BasicTestDataParser basicParser = (BasicTestDataParser) parser;
        try {
            for (String resourceName : resourceNames) {
                if (basicParser.isDataSectionExisting(dir, resourceName)) {
                    return;
                }
            }
        } catch (IllegalArgumentException e) {
            // リーダが扱えない形式のリソース名（Excel形式用のリーダにシート名の無いリソース名）を渡した場合
            throw new IllegalArgumentException(createNoDataSectionMessage(parser, masterDataFile), e);
        }
        throw new IllegalArgumentException(createNoDataSectionMessage(parser, masterDataFile));
    }

    /**
     * データセクションが存在しない場合の例外メッセージを作成する。
     *
     * @param parser テストデータのパーサ
     * @param masterDataFile マスタデータファイル
     * @return 例外メッセージ
     */
    private String createNoDataSectionMessage(TestDataParser parser, File masterDataFile) {
        return concat(
                "no data was found in the master data file for the configured testDataParser. ",
                "check that the testDataParser supports the format and extension of the file. ",
                "file=[", masterDataFile.getAbsolutePath(), "], ",
                "testDataParser=[", parser.getClass().getName(), "]");
    }

    /** Excel形式と判定する拡張子（{@link PoiXlsReader}が許容するファイル名と同じ定義） */
    private static final List<String> EXCEL_EXTENSIONS = Arrays.asList("xls", "xlsx");

    /**
     * マスタデータファイルがExcel形式であるか判定する。
     *
     * @param masterDataFile マスタデータファイル
     * @return Excel形式であればtrue
     */
    private boolean isExcelFile(File masterDataFile) {
        String name = masterDataFile.getName();
        int dotPosition = name.lastIndexOf('.');
        if (dotPosition < 0) {
            // 拡張子のないファイルはExcel形式とみなさない
            return false;
        }
        return EXCEL_EXTENSIONS.contains(name.substring(dotPosition + 1));
    }

    /**
     * 拡張子なしのマスタデータファイル名を取得する。
     *
     * @param masterDataFile ファイル名
     * @return 拡張子なしのマスタデータファイル名
     */
    private String getMasterFileNameWithoutSuffix(File masterDataFile) {
        String masterFileName = masterDataFile.getName();
        return NablarchTestUtils.getFileNameWithoutSuffix(masterFileName);
    }



    /**
     * マスタデータファイルの存在するディレクトリを取得する。
     *
     * @param masterDataFile ファイル名
     * @return マスタデータファイルの存在するディレクトリ
     */
    private String getMasterDataDir(File masterDataFile) {
        return masterDataFile.getAbsoluteFile().getParent();
    }
}
