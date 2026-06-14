package nablarch.test.core.reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.db.TableData;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.FixedLengthFile;
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
     * メッセージ（{@link DataType#MESSAGE}）を取り出す。
     * <p>
     * 変換ツールが中間モデルへ写すのに必要な FW 制御ヘッダと本文（固定長ファイル）を
     * 併せ持つ{@link MessageData}を返す。本文の{@link FixedLengthFile}は本体
     * {@link MessageParser#getDelegate()}（同一パッケージからのみ可視）から取り出す。
     * これは{@link MessagePool#getSource()}が protected で変換ツールのパッケージから
     * 読めないための相乗りであり、相乗りの影響は本アダプタに局所化される（設計書 §共通）。
     * 対象が存在しない場合は{@code null}を返す（本体{@link MessageParser}の挙動を踏襲）。
     * </p>
     *
     * @param path     取得元パス
     * @param resource 取得元リソース名
     * @param id       メッセージ ID（{@code =}以降の識別子）
     * @return メッセージ。対象が存在しない場合は{@code null}
     */
    public MessageData readMessage(String path, String resource, String id) {
        MessageParser parser = new MessageParser(reader, EMPTY_INTERPRETERS, DataType.MESSAGE);
        parser.parse(path, resource, id);
        List<FixedLengthFile> bodies = parser.getDelegate().getResult();
        if (bodies.isEmpty()) {
            return null;
        }
        return new MessageData(parser.getFwHeader(), bodies.get(0));
    }

    /**
     * リソース内に存在する全データブロックの<b>ヘッダ</b>（データタイプ・グループ ID・識別子）を
     * シート記述順に列挙する。ブロック本体の解析は行わない。
     * <p>
     * 変換ツールはアダプタの各 {@code read*} メソッドを (データタイプ, ID) 単位で呼ぶため、
     * 「リソースにどのブロックが存在するか」を知る手段が要る。本メソッドは本体
     * {@link TestDataParsingTemplate#getDataType(String)}／{@link TestDataParsingTemplate#getTypeValue(List)}
     * を再利用してマーカー行を判定するため、行分類のロジックを本体と二重実装しない
     * （変換ツール側に構造解析を持ち込まない）。グループ ID（{@code [g1]} 等）はデータタイプ名と
     * {@code =}の間の文字列として切り出す。
     * </p>
     *
     * @param path     取得元パス
     * @param resource 取得元リソース名
     * @return ブロックヘッダ一覧（記述順。マーカー行が無ければ空）
     */
    public List<BlockHeader> readHeaders(String path, String resource) {
        HeaderCollector collector = new HeaderCollector(reader);
        collector.parse(path, resource, "");
        return collector.getResult();
    }

    /**
     * 1 データブロックのヘッダ（マーカー行から取り出した属性）。
     * <p>
     * {@code SETUP_TABLE[g1]=USERS} のようなマーカー行を、データタイプ
     * （{@code SETUP_TABLE}）・グループ ID（{@code [g1]}、無指定は空文字）・
     * 識別子（{@code USERS}）へ分解して保持する。
     * </p>
     */
    public static final class BlockHeader {

        /** データタイプ */
        private final DataType type;

        /** グループ ID（{@code [g1]} 等。無指定は空文字） */
        private final String groupId;

        /** 識別子（テーブル名／ファイルパス／LIST_MAP ID／メッセージ ID） */
        private final String identifier;

        /**
         * コンストラクタ。
         *
         * @param type       データタイプ
         * @param groupId    グループ ID（無指定は空文字）
         * @param identifier 識別子
         */
        BlockHeader(DataType type, String groupId, String identifier) {
            this.type = type;
            this.groupId = groupId;
            this.identifier = identifier;
        }

        /** @return データタイプ */
        public DataType getType() {
            return type;
        }

        /** @return グループ ID（{@code [g1]} 等。無指定は空文字） */
        public String getGroupId() {
            return groupId;
        }

        /** @return 識別子（テーブル名／ファイルパス／LIST_MAP ID／メッセージ ID） */
        public String getIdentifier() {
            return identifier;
        }
    }

    /**
     * メッセージ（{@link DataType#MESSAGE}）の取り出し結果。FW 制御ヘッダと本文を併せ持つ。
     *
     * @see #readMessage(String, String, String)
     */
    public static final class MessageData {

        /** FW 制御ヘッダ（{@code requestId}／{@code userId} 等。記法のまま・未加工） */
        private final Map<String, String> fwHeader;

        /** 本文（固定長ファイルの器。記法のまま・未加工） */
        private final FixedLengthFile body;

        /**
         * コンストラクタ。
         *
         * @param fwHeader FW 制御ヘッダ
         * @param body     本文
         */
        MessageData(Map<String, String> fwHeader, FixedLengthFile body) {
            this.fwHeader = fwHeader;
            this.body = body;
        }

        /** @return FW 制御ヘッダ（記法のまま・未加工） */
        public Map<String, String> getFwHeader() {
            return fwHeader;
        }

        /** @return 本文（固定長ファイルの器。記法のまま・未加工） */
        public FixedLengthFile getBody() {
            return body;
        }
    }

    /**
     * リソース内のマーカー行を走査してブロックヘッダを収集する、解析を伴わない
     * {@link TestDataParsingTemplate}。
     * <p>
     * 本体のテンプレートが提供する{@code getDataType}／{@code getTypeValue}で
     * マーカー行の判定・識別子抽出を行い、ブロック本体（カラム・行・型）の解析はしない。
     * 特定のデータタイプを対象にしないため{@link #isTargetType}は常に偽を返し、
     * 走査ロジックは{@link #parse(String)}を上書きして実装する。
     * </p>
     */
    private static final class HeaderCollector extends TestDataParsingTemplate<List<BlockHeader>> {

        /** 収集したヘッダ（記述順） */
        private final List<BlockHeader> headers = new ArrayList<BlockHeader>();

        /**
         * コンストラクタ。
         *
         * @param reader テストデータリーダ
         */
        HeaderCollector(TestDataReader reader) {
            super(reader, EMPTY_INTERPRETERS, DataType.DEFAULT);
        }

        @Override
        void parse(String id) {
            List<String> line;
            while ((line = readLine()) != null) {
                String first = line.get(0);
                DataType type = getDataType(first);
                if (type == DataType.DEFAULT) {
                    continue;
                }
                String afterName = first.substring(type.getName().length());
                int eq = afterName.indexOf('=');
                if (eq < 0) {
                    // データタイプ名で始まるがマーカー行でない（'='なし）＝対象外
                    continue;
                }
                String groupId = afterName.substring(0, eq);
                String identifier = getTypeValue(line);
                headers.add(new BlockHeader(type, groupId, identifier));
            }
        }

        @Override
        void onReadLine(List<String> line) {
            // ブロック本体は解析しない
        }

        @Override
        void onTargetTypeFound(List<String> line) {
            // 特定タイプを対象にしない
        }

        @Override
        boolean isTargetType(List<String> line, String id) {
            return false;
        }

        @Override
        boolean shouldStopOnNextOne() {
            return false;
        }

        @Override
        List<BlockHeader> getResult() {
            return headers;
        }
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
