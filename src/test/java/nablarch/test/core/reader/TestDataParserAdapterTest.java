package nablarch.test.core.reader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.test.core.db.TableData;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.VariableLengthFile;

import org.junit.Test;

/**
 * {@link TestDataParserAdapter}のテストクラス。
 * <p>
 * 本アダプタは本体 Parser を空 interpreters で配線し {@code parse → getResult} で
 * 生の本体器を取り出す。各テストは「本体器を返すこと」と「IN 値が記法のまま（未加工）であること」を検証する。
 * </p>
 * <p>
 * 注意：{@link TestDataParsingTemplate} 等は dir/resource をキーとした静的キャッシュを持つため、
 * テストメソッドごとに resource 名を一意にしてキャッシュ衝突を避ける。
 * </p>
 *
 * @author kiyobot
 */
public class TestDataParserAdapterTest {

    /**
     * テスト用の{@link TestDataReader}実装。
     * resource 名をキーに canned なテストデータを返却し、Excel ファイルを使わずに解析させる。
     */
    private static class FakeTestDataReader implements TestDataReader {

        /** resource 名 → 行データ */
        private final Map<String, List<List<String>>> dataByResource = new HashMap<String, List<List<String>>>();

        /** 現在オープン中のリソースのイテレータ */
        private java.util.Iterator<List<String>> current;

        /**
         * canned データを登録する。
         *
         * @param resource リソース名
         * @param lines    行データ
         * @return 自身
         */
        FakeTestDataReader put(String resource, List<List<String>> lines) {
            dataByResource.put(resource, lines);
            return this;
        }

        @Override
        public void open(String path, String dataName) {
            List<List<String>> lines = dataByResource.get(dataName);
            if (lines == null) {
                lines = new ArrayList<List<String>>();
            }
            current = lines.iterator();
        }

        @Override
        public void close() {
            current = null;
        }

        @Override
        public List<String> readLine() {
            return (current != null && current.hasNext()) ? current.next() : null;
        }

        @Override
        public boolean isResourceExisting(String basePath, String resourceName) {
            return dataByResource.containsKey(resourceName);
        }

        @Override
        public boolean isDataExisting(String basePath, String resourceName) {
            return dataByResource.containsKey(resourceName);
        }
    }

    /** ディレクトリ（ダミー） */
    private static final String DIR = "dummy-dir";

    /**
     * 行データを組み立てるユーティリティ。null セルを含められるよう{@link Arrays#asList}を使う。
     *
     * @param cells セル
     * @return 行データ
     */
    private static List<String> row(String... cells) {
        return Arrays.asList(cells);
    }

    // ------------------------------------------------------------------ readTables

    /**
     * Given: マーカーカラム・{@code ${...}}・空文字・null セルを含む SETUP_TABLE ブロック。
     * When : {@code readTables(SETUP_TABLE_DATA)} を呼ぶ。
     * Then : 本体器{@link TableData}が返り、IN 値が記法のまま（未加工）で、マーカーカラムが除外される。
     */
    @Test
    public void readTablesReturnsRawTableData() {
        String resource = "readTablesReturnsRawTableData";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("SETUP_TABLE=USERS"));
        lines.add(row("USER_NAME", "AGE", "[NOTE]"));   // [NOTE] はマーカーカラム
        lines.add(row("${userName}", "", "memo"));       // ${...}・空文字
        lines.add(row("literal", null, "memo2"));        // null セル

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        List<TableData> tables = adapter.readTables(DIR, resource, "", DataType.SETUP_TABLE_DATA);

        assertThat(tables.size(), is(1));
        TableData table = tables.get(0);
        assertThat(table.getTableName(), is("USERS"));
        // マーカーカラムは除外される
        assertThat(table.getColumnNames().length, is(2));
        // IN 値は記法のまま（未加工）
        assertThat(table.getValue(0, "USER_NAME").toString(), is("${userName}"));
        assertThat(table.getValue(0, "AGE").toString(), is(""));
        assertThat(table.getValue(1, "USER_NAME").toString(), is("literal"));
        assertThat(table.getValue(1, "AGE"), is(nullValue()));
    }

    /**
     * Given: 複数テーブルを含む SETUP_TABLE ブロック。
     * When : {@code readTables} を呼ぶ。
     * Then : すべてのテーブルが順に収集される（グループ収集）。
     */
    @Test
    public void readTablesCollectsMultipleTables() {
        String resource = "readTablesCollectsMultipleTables";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("SETUP_TABLE=USERS"));
        lines.add(row("USER_NAME"));
        lines.add(row("alice"));
        lines.add(row("SETUP_TABLE=ROLES"));
        lines.add(row("ROLE_NAME"));
        lines.add(row("admin"));

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        List<TableData> tables = adapter.readTables(DIR, resource, "", DataType.SETUP_TABLE_DATA);

        assertThat(tables.size(), is(2));
        assertThat(tables.get(0).getTableName(), is("USERS"));
        assertThat(tables.get(1).getTableName(), is("ROLES"));
    }

    /**
     * Given: 一部カラムのみ宣言した EXPECTED_COMPLETE_TABLE ブロック。
     * When : {@code readTables(EXPECTED_COMPLETED)} を呼ぶ。
     * Then : デフォルト値補完（{@code fillDefaultValues}）が行われない（後処理なし）。
     * <p>
     * {@code TableData#fillDefaultValues()} は「宣言されていない DB カラム」を埋める実装で、
     * その過程で {@code DbInfo#getColumns} を呼び、最後に {@code setColumnNames(allColumns)} で
     * カラムを DB 全カラムへ拡張する。本アダプタの {@code StubDbInfo#getColumns} は番人として
     * 例外を投げるため、補完が走れば本テストは例外で落ちる。「例外なく完了し」かつ
     * 「カラム数が宣言数のまま（拡張されない）」ことで後処理が行われないことを識別的に実証する。
     * </p>
     */
    @Test
    public void readTablesDoesNotPostProcessExpectedCompleted() {
        String resource = "readTablesDoesNotPostProcessExpectedCompleted";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("EXPECTED_COMPLETE_TABLE=USERS"));
        lines.add(row("USER_NAME"));    // 1 カラムのみ宣言（DB には他カラムがある想定）
        lines.add(row("${u}"));

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        // 補完が走れば StubDbInfo#getColumns が番人として例外を投げる＝ここで落ちる
        List<TableData> tables = adapter.readTables(DIR, resource, "", DataType.EXPECTED_COMPLETED);

        assertThat(tables.size(), is(1));
        // 補完が走れば setColumnNames(allColumns) でカラムが増える。宣言数のまま＝後処理なし。
        assertThat(tables.get(0).getColumnNames().length, is(1));
        // IN 値は記法のまま
        assertThat(tables.get(0).getValue(0, "USER_NAME").toString(), is("${u}"));
    }

    /**
     * Given: EXPECTED_TABLE と EXPECTED_COMPLETE_TABLE を併置したリソース。
     * When : {@code readTables(EXPECTED_COMPLETED)} を呼ぶ。
     * Then : 指定タイプ単独（EXPECTED_COMPLETE 分のみ）を返し、EXPECTED_TABLE とマージしない。
     * <p>
     * 本体 {@code getExpectedTableData} は両タイプをマージするが、アダプタは後処理を持ち込まない。
     * </p>
     */
    @Test
    public void readTablesDoesNotMergeExpectedTypes() {
        String resource = "readTablesDoesNotMergeExpectedTypes";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("EXPECTED_TABLE=PLAIN"));
        lines.add(row("COL"));
        lines.add(row("plain"));
        lines.add(row("EXPECTED_COMPLETE_TABLE=COMP"));
        lines.add(row("COL"));
        lines.add(row("comp"));

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        List<TableData> completed = adapter.readTables(DIR, resource, "", DataType.EXPECTED_COMPLETED);

        // EXPECTED_COMPLETED 単独＝COMP のみ（EXPECTED_TABLE の PLAIN を含まない＝マージしない）
        assertThat(completed.size(), is(1));
        assertThat(completed.get(0).getTableName(), is("COMP"));
    }

    /**
     * Given: 対象タイプのブロックが存在しないリソース。
     * When : {@code readTables} を呼ぶ。
     * Then : 空リストが返る。
     */
    @Test
    public void readTablesReturnsEmptyWhenNoBlock() {
        String resource = "readTablesReturnsEmptyWhenNoBlock";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("LIST_MAP=other"));
        lines.add(row("KEY"));
        lines.add(row("v"));

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        List<TableData> tables = adapter.readTables(DIR, resource, "", DataType.SETUP_TABLE_DATA);

        assertThat(tables.isEmpty(), is(true));
    }

    /**
     * Given: readTables にファイル系の DataType を渡す。
     * When : 呼び出す。
     * Then : {@link IllegalArgumentException} が送出される（不正タイプの早期検出）。
     */
    @Test
    public void readTablesRejectsNonTableType() {
        TestDataParserAdapter adapter = new TestDataParserAdapter(new FakeTestDataReader());
        try {
            adapter.readTables(DIR, "x", "", DataType.SETUP_FIXED);
            fail("IllegalArgumentException が送出されるべき");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    /**
     * Given: EXPECTED_TABLE ブロック。
     * When : {@code readTables(EXPECTED_TABLE_DATA)} を呼ぶ。
     * Then : 当該タイプのテーブルが取得できる（許容タイプの網羅）。
     */
    @Test
    public void readTablesSupportsExpectedTableType() {
        String resource = "readTablesSupportsExpectedTableType";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("EXPECTED_TABLE=USERS"));
        lines.add(row("USER_NAME"));
        lines.add(row("${u}"));

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        List<TableData> tables = adapter.readTables(DIR, resource, "", DataType.EXPECTED_TABLE_DATA);

        assertThat(tables.size(), is(1));
        assertThat(tables.get(0).getValue(0, "USER_NAME").toString(), is("${u}"));
    }

    // ------------------------------------------------------------------ readListMap

    /**
     * Given: マーカーカラム・{@code ${...}}・null を含む LIST_MAP ブロック。
     * When : {@code readListMap} を呼ぶ。
     * Then : {@code List<Map<String,String>>}が返り、IN 値が記法のまま、マーカーカラムが除外される。
     */
    @Test
    public void readListMapReturnsRawRows() {
        String resource = "readListMapReturnsRawRows";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("LIST_MAP=result"));
        lines.add(row("ID", "NAME", "[MARK]"));
        lines.add(row("${id}", "", "x"));

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        List<Map<String, String>> rows = adapter.readListMap(DIR, resource, "result");

        assertThat(rows.size(), is(1));
        Map<String, String> r = rows.get(0);
        assertThat(r.containsKey("[MARK]"), is(false));   // マーカーカラム除外
        assertThat(r.get("ID"), is("${id}"));             // 記法のまま
        assertThat(r.get("NAME"), is(""));
    }

    // ------------------------------------------------------------------ readFiles

    /**
     * Given: SETUP_FIXED の固定長ファイルブロック（{@code ${...}}を含む）。
     * When : {@code readFiles(SETUP_FIXED)} を呼ぶ。
     * Then : 本体器{@link nablarch.test.core.file.FixedLengthFile}が返り、IN 値が記法のまま。
     */
    @Test
    public void readFilesReturnsRawFixedLengthFile() {
        String resource = "readFilesReturnsRawFixedLengthFile";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("SETUP_FIXED=test.dat"));
        lines.add(row("data", "field1", "field2"));     // フィールド名行
        lines.add(row("", "半角英字", "半角英字"));        // 型（設計書記法）
        lines.add(row("", "10", "5"));                   // 長さ
        lines.add(row("", "${value}", "abc"));           // データ行

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        List<? extends DataFile> files = adapter.readFiles(DIR, resource, "", DataType.SETUP_FIXED);

        assertThat(files.size(), is(1));
        DataFile file = files.get(0);
        assertThat(file.getPath(), is("test.dat"));
        // IN 値は記法のまま（未加工）。getValues はフィールド名→値のマップ。
        Map<String, String> values = file.getAllFragments().get(0).getValues().get(0);
        assertThat(values.get("field1"), is("${value}"));
        assertThat(values.get("field2"), is("abc"));
    }

    /**
     * Given: EXPECTED_VARIABLE の可変長ファイルブロック。
     * When : {@code readFiles(EXPECTED_VARIABLE)} を呼ぶ。
     * Then : 本体器{@link VariableLengthFile}が返る。
     */
    @Test
    public void readFilesReturnsVariableLengthFile() {
        String resource = "readFilesReturnsVariableLengthFile";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("EXPECTED_VARIABLE=var.csv"));
        lines.add(row("data", "field1"));
        lines.add(row("", "半角英字"));      // 可変長は型のみ（長さなし）
        lines.add(row("", "${v}"));         // データ行

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        List<? extends DataFile> files = adapter.readFiles(DIR, resource, "", DataType.EXPECTED_VARIABLE);

        assertThat(files.size(), is(1));
        assertThat(files.get(0) instanceof VariableLengthFile, is(true));
        assertThat(files.get(0).getAllFragments().get(0).getValues().get(0).get("field1"), is("${v}"));
    }

    /**
     * Given: EXPECTED_FIXED の固定長ファイルブロック。
     * When : {@code readFiles(EXPECTED_FIXED)} を呼ぶ。
     * Then : 固定長ファイルが取得できる（許容タイプの網羅）。
     */
    @Test
    public void readFilesSupportsExpectedFixed() {
        String resource = "readFilesSupportsExpectedFixed";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("EXPECTED_FIXED=exp.dat"));
        lines.add(row("data", "field1"));
        lines.add(row("", "半角英字"));
        lines.add(row("", "5"));
        lines.add(row("", "${e}"));

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        List<? extends DataFile> files = adapter.readFiles(DIR, resource, "", DataType.EXPECTED_FIXED);

        assertThat(files.size(), is(1));
        assertThat(files.get(0).getAllFragments().get(0).getValues().get(0).get("field1"), is("${e}"));
    }

    /**
     * Given: SETUP_VARIABLE の可変長ファイルブロック。
     * When : {@code readFiles(SETUP_VARIABLE)} を呼ぶ。
     * Then : 可変長ファイルが取得できる（許容タイプの網羅）。
     */
    @Test
    public void readFilesSupportsSetupVariable() {
        String resource = "readFilesSupportsSetupVariable";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("SETUP_VARIABLE=in.csv"));
        lines.add(row("data", "field1"));
        lines.add(row("", "半角英字"));
        lines.add(row("", "${s}"));

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        List<? extends DataFile> files = adapter.readFiles(DIR, resource, "", DataType.SETUP_VARIABLE);

        assertThat(files.size(), is(1));
        assertThat(files.get(0) instanceof VariableLengthFile, is(true));
        assertThat(files.get(0).getAllFragments().get(0).getValues().get(0).get("field1"), is("${s}"));
    }

    /**
     * Given: readFiles にテーブル系の DataType を渡す。
     * When : 呼び出す。
     * Then : {@link IllegalArgumentException} が送出される。
     */
    @Test
    public void readFilesRejectsNonFileType() {
        TestDataParserAdapter adapter = new TestDataParserAdapter(new FakeTestDataReader());
        try {
            adapter.readFiles(DIR, "x", "", DataType.LIST_MAP);
            fail("IllegalArgumentException が送出されるべき");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    // ------------------------------------------------------------------ readMessage

    /**
     * Given: FW 制御ヘッダ（{@code ${...}}を含む）を持つ MESSAGE ブロック。
     * When : {@code readMessage} を呼ぶ。
     * Then : {@link TestDataParserAdapter.MessageData}が返り、FW ヘッダ値が記法のまま（未加工）。
     */
    @Test
    public void readMessageReturnsRawMessagePool() {
        String resource = "readMessageReturnsRawMessagePool";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("MESSAGE=msg1"));
        lines.add(row("requestId", "${rid}"));
        lines.add(row("userId", "U001"));

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        TestDataParserAdapter.MessageData message = adapter.readMessage(DIR, resource, "msg1");

        assertNotNull(message);
        assertThat(message.getFwHeader().get("requestId"), is("${rid}"));
        assertThat(message.getFwHeader().get("userId"), is("U001"));
        // 本文（固定長ファイル）も取り出せる
        assertNotNull(message.getBody());
    }

    /**
     * Given: MESSAGE ブロックが存在しないリソース。
     * When : {@code readMessage} を呼ぶ。
     * Then : {@code null} が返る（本体 {@link MessageParser} の挙動を踏襲）。
     */
    @Test
    public void readMessageReturnsNullWhenNoBlock() {
        String resource = "readMessageReturnsNullWhenNoBlock";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("LIST_MAP=x"));
        lines.add(row("K"));
        lines.add(row("v"));

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        TestDataParserAdapter.MessageData message = adapter.readMessage(DIR, resource, "missing");

        assertThat(message, is(nullValue()));
    }

    // ------------------------------------------------------------------ readHeaders

    /**
     * Given: 複数のデータタイプ（テーブル・ファイル・LIST_MAP・メッセージ）が混在するリソース。
     * When : {@code readHeaders} を呼ぶ。
     * Then : 全マーカー行が記述順に列挙され、データタイプ・グループ ID・識別子が分解される。
     */
    @Test
    public void readHeadersEnumeratesAllBlocksInDescriptionOrder() {
        String resource = "readHeadersEnumeratesAllBlocksInDescriptionOrder";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("SETUP_TABLE=USERS"));
        lines.add(row("USER_NAME"));
        lines.add(row("alice"));
        lines.add(row("SETUP_FIXED=in.dat"));
        lines.add(row("data", "f1"));
        lines.add(row("", "半角英字"));
        lines.add(row("", "5"));
        lines.add(row("", "x"));
        lines.add(row("LIST_MAP=result"));
        lines.add(row("ID"));
        lines.add(row("1"));
        lines.add(row("MESSAGE=msg1"));
        lines.add(row("requestId", "R001"));

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        List<TestDataParserAdapter.BlockHeader> headers = adapter.readHeaders(DIR, resource);

        assertThat(headers.size(), is(4));
        assertThat(headers.get(0).getType(), is(DataType.SETUP_TABLE_DATA));
        assertThat(headers.get(0).getGroupId(), is(""));
        assertThat(headers.get(0).getIdentifier(), is("USERS"));
        assertThat(headers.get(1).getType(), is(DataType.SETUP_FIXED));
        assertThat(headers.get(1).getIdentifier(), is("in.dat"));
        assertThat(headers.get(2).getType(), is(DataType.LIST_MAP));
        assertThat(headers.get(2).getIdentifier(), is("result"));
        assertThat(headers.get(3).getType(), is(DataType.MESSAGE));
        assertThat(headers.get(3).getIdentifier(), is("msg1"));
    }

    /**
     * Given: グループ ID 付きマーカー（{@code SETUP_TABLE[g1]=USERS}）。
     * When : {@code readHeaders} を呼ぶ。
     * Then : グループ ID が {@code [g1]}、識別子が {@code USERS} として切り出される。
     */
    @Test
    public void readHeadersExtractsGroupId() {
        String resource = "readHeadersExtractsGroupId";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("SETUP_TABLE[g1]=USERS"));
        lines.add(row("USER_NAME"));
        lines.add(row("alice"));

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        List<TestDataParserAdapter.BlockHeader> headers = adapter.readHeaders(DIR, resource);

        assertThat(headers.size(), is(1));
        assertThat(headers.get(0).getType(), is(DataType.SETUP_TABLE_DATA));
        assertThat(headers.get(0).getGroupId(), is("[g1]"));
        assertThat(headers.get(0).getIdentifier(), is("USERS"));
    }

    /**
     * Given: 同一データタイプ・同一グループの複数ブロック。
     * When : {@code readHeaders} を呼ぶ。
     * Then : 各ブロックが個別のヘッダとして記述順に列挙される（重複排除は呼び出し側の責務）。
     */
    @Test
    public void readHeadersListsEachBlockEvenWhenSameTypeAndGroup() {
        String resource = "readHeadersListsEachBlockEvenWhenSameTypeAndGroup";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("SETUP_TABLE=USERS"));
        lines.add(row("USER_NAME"));
        lines.add(row("alice"));
        lines.add(row("SETUP_TABLE=ROLES"));
        lines.add(row("ROLE_NAME"));
        lines.add(row("admin"));

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        List<TestDataParserAdapter.BlockHeader> headers = adapter.readHeaders(DIR, resource);

        assertThat(headers.size(), is(2));
        assertThat(headers.get(0).getIdentifier(), is("USERS"));
        assertThat(headers.get(1).getIdentifier(), is("ROLES"));
    }

    /**
     * Given: マーカー行が存在しないリソース。
     * When : {@code readHeaders} を呼ぶ。
     * Then : 空リストが返る。
     */
    @Test
    public void readHeadersReturnsEmptyWhenNoMarker() {
        String resource = "readHeadersReturnsEmptyWhenNoMarker";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("just", "some", "data"));

        TestDataParserAdapter adapter = new TestDataParserAdapter(
                new FakeTestDataReader().put(resource, lines));

        List<TestDataParserAdapter.BlockHeader> headers = adapter.readHeaders(DIR, resource);

        assertThat(headers.isEmpty(), is(true));
    }
}
