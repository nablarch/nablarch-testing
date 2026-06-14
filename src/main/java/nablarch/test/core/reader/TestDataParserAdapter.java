package nablarch.test.core.reader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.db.TableData;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

/**
 * テストデータ変換ツール（{@code nablarch.test.tool.converter}）が、本体の構造解析を
 * 再利用して生の器を取り出すための薄いアダプタ。
 * <p>
 * 本体の各 Parser は取り出し口（{@code getResult}）や一部コンストラクタが
 * パッケージプライベートで、変換ツールのパッケージから直接呼べない。本クラスを
 * 本体 Parser と同一パッケージ（{@code nablarch.test.core.reader}）に 1 枚だけ
 * 相乗りさせ、この可視性の壁を越える。相乗りの影響は本クラスに局所化される
 * （設計書 判断 A）。
 * </p>
 * <p>
 * 配線は常に<b>空の interpreters</b>で行うため、{@code ${...}} 等の特殊記法・補完・
 * マージといった値加工は一切行われず、IN 値は記法のまま（未加工）で取り出される。
 * また {@code getExpectedTableData} のような後処理（デフォルト値補完・期待値マージ）も
 * 行わない。各メソッドはデータタイプに対応する本体器をそのまま返す。
 * </p>
 *
 * @author kiyobot
 */
public class TestDataParserAdapter {

    /** 空の interpreters（値加工を一切行わせないための配線） */
    private static final List<TestDataInterpreter> EMPTY_INTERPRETERS = Collections.emptyList();

    /** テストデータリーダ */
    private final TestDataReader reader;

    /** スタブの{@link DbInfo}（カラム型の取得にのみ使用される） */
    private final DbInfo dbInfo = new StubDbInfo();

    /** デフォルト値（{@link TableData}生成に必要なだけで、補完は実行しない） */
    private final DefaultValues defaultValues = new BasicDefaultValues();

    /**
     * コンストラクタ。
     *
     * @param reader テストデータリーダ
     */
    public TestDataParserAdapter(TestDataReader reader) {
        this.reader = reader;
    }

    /**
     * テーブルデータを取り出す。
     * <p>
     * 後処理（デフォルト値補完・期待値マージ）は行わず、指定されたデータタイプの
     * 生の{@link TableData}一覧をそのまま返す。
     * </p>
     *
     * @param path     取得元パス
     * @param resource 取得元リソース名
     * @param id       グループID（グループ指定が無い場合は空文字）
     * @param type     データタイプ（{@link DataType#SETUP_TABLE_DATA}／
     *                 {@link DataType#EXPECTED_TABLE_DATA}／{@link DataType#EXPECTED_COMPLETED}）
     * @return テーブルデータ一覧
     * @throws IllegalArgumentException データタイプがテーブル系でない場合
     */
    public List<TableData> readTables(String path, String resource, String id, DataType type) {
        switch (type) {
            case SETUP_TABLE_DATA:
            case EXPECTED_TABLE_DATA:
            case EXPECTED_COMPLETED:
                break;
            default:
                throw new IllegalArgumentException(
                        "unsupported data type for readTables. type=[" + type + "]");
        }
        TableDataParser parser = new TableDataParser(reader, EMPTY_INTERPRETERS, dbInfo, defaultValues, type);
        parser.parse(path, resource, id);
        return parser.getResult();
    }

    /**
     * {@code List<Map<String, String>>}形式のデータを取り出す。
     *
     * @param path     取得元パス
     * @param resource 取得元リソース名
     * @param id       ID
     * @return 行データ一覧（キーはヘッダ行のカラム名）
     */
    public List<Map<String, String>> readListMap(String path, String resource, String id) {
        ListMapParser parser = new ListMapParser(reader, EMPTY_INTERPRETERS);
        parser.parse(path, resource, id);
        return parser.getResult();
    }

    /**
     * ファイル（固定長／可変長）を取り出す。
     *
     * @param path     取得元パス
     * @param resource 取得元リソース名
     * @param id       グループID（グループ指定が無い場合は空文字）
     * @param type     データタイプ（{@link DataType#SETUP_FIXED}／{@link DataType#EXPECTED_FIXED}／
     *                 {@link DataType#SETUP_VARIABLE}／{@link DataType#EXPECTED_VARIABLE}）
     * @return ファイル一覧
     * @throws IllegalArgumentException データタイプがファイル系でない場合
     */
    public List<? extends DataFile> readFiles(String path, String resource, String id, DataType type) {
        DataFileParser<? extends DataFile> parser;
        switch (type) {
            case SETUP_FIXED:
            case EXPECTED_FIXED:
                parser = new FixedLengthFileParser(reader, EMPTY_INTERPRETERS, type);
                break;
            case SETUP_VARIABLE:
            case EXPECTED_VARIABLE:
                parser = new VariableLengthFileParser(reader, EMPTY_INTERPRETERS, type);
                break;
            default:
                throw new IllegalArgumentException(
                        "unsupported data type for readFiles. type=[" + type + "]");
        }
        parser.parse(path, resource, id);
        return parser.getResult();
    }

    /**
     * メッセージを取り出す。
     * <p>
     * FW 制御ヘッダ（{@link MessagePool#getFwHeader()}）と本文（固定長ファイル）を
     * 持つ{@link MessagePool}を返す。対象が存在しない場合は{@code null}を返す
     * （本体{@link MessageParser}の挙動を踏襲）。
     * </p>
     *
     * @param path     取得元パス
     * @param resource 取得元リソース名
     * @param id       ID
     * @return メッセージ。対象が存在しない場合は{@code null}
     */
    public MessagePool readMessage(String path, String resource, String id) {
        MessageParser parser = new MessageParser(reader, EMPTY_INTERPRETERS, DataType.MESSAGE);
        parser.parse(path, resource, id);
        return parser.getResult();
    }

    /**
     * 本体 Parser の配線にのみ用いるスタブの{@link DbInfo}。
     * <p>
     * 変換ツールの読み込み（parse→getResult）経路で実際に呼ばれるのは
     * {@link #getColumnType(String, String)}のみ（{@link TableData#addRow(List)}でのカラム型取得）。
     * 値は型に依存せず生のまま格納されるため、一律で{@link java.sql.Types#VARCHAR}を返す。
     * </p>
     * <p>
     * その他のメソッドは DB 書き込み経路（insertData/replaceData）専用で、本アダプタの
     * DB レスな読み込み経路からは呼ばれない。これらは「呼ばれてはならない」ことを表明する
     * <b>番人コード</b>であり、万一呼ばれた場合は前提崩れとして{@link UnsupportedOperationException}で
     * 即座に失敗させる（誤った DB メタ情報で変換結果を静かに歪めない）。
     * </p>
     */
    private static final class StubDbInfo implements DbInfo {

        /** 読み込み経路から呼ばれてはならないメソッドが呼ばれた場合の例外を生成する。 */
        private static UnsupportedOperationException notOnReadPath(String method) {
            return new UnsupportedOperationException(
                    "DbInfo#" + method + " must not be called on the DB-less converter read path.");
        }

        @Override
        public int getColumnType(String tabName, String columnName) {
            return java.sql.Types.VARCHAR;
        }

        @Override
        public String[] getPrimaryKeys(String tabName) {
            throw notOnReadPath("getPrimaryKeys");
        }

        @Override
        public String[] getColumns(String tabName) {
            throw notOnReadPath("getColumns");
        }

        @Override
        public boolean isUniqueIndex(String tabName, String colName) {
            throw notOnReadPath("isUniqueIndex");
        }

        @Override
        public int getColumnLength(String tabName, String colName) {
            throw notOnReadPath("getColumnLength");
        }

        @Override
        public boolean isComputedColumn(String tabName, String colName) {
            throw notOnReadPath("isComputedColumn");
        }

        @Override
        public boolean isNumberTypeColumn(String tableName, String columnName) {
            throw notOnReadPath("isNumberTypeColumn");
        }

        @Override
        public boolean isDateTypeColumn(String tableName, String columnName) {
            throw notOnReadPath("isDateTypeColumn");
        }

        @Override
        public boolean isBinaryTypeColumn(String tableName, String columnName) {
            throw notOnReadPath("isBinaryTypeColumn");
        }

        @Override
        public boolean isBooleanTypeColumn(String tableName, String columnName) {
            throw notOnReadPath("isBooleanTypeColumn");
        }
    }
}
