package nablarch.test.tool.converter.xls;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.test.core.reader.TestCoreReaderAdapter;
import nablarch.test.core.reader.TestDataReader;
import nablarch.test.tool.converter.model.FieldDef;
import nablarch.test.tool.converter.model.FileDataBlock;
import nablarch.test.tool.converter.model.ListMapBlock;
import nablarch.test.tool.converter.model.MessageDataBlock;
import nablarch.test.tool.converter.model.RecordLayout;
import nablarch.test.tool.converter.model.TableDataBlock;
import nablarch.test.tool.converter.model.TestDataBlock;
import nablarch.test.tool.converter.model.TestDataContainer;
import nablarch.test.tool.converter.model.TestDataSection;

import org.junit.Test;

/**
 * {@link XlsFormatReader}のテストクラス。
 * <p>
 * 実 Excel を使わず、{@link FakeTestDataReader}に canned な行データを与えて
 * {@link TestCoreReaderAdapter}を駆動し、本体器が中間モデルへ無損失（IN 値が記法のまま）に
 * 写されることを検証する。{@link TestCoreReaderAdapter}内部の静的キャッシュ衝突を避けるため、
 * テストメソッドごとにリソース名を一意にする。
 * </p>
 *
 * @author kiyobot
 */
public class XlsFormatReaderTest {

    /** ディレクトリ（ダミー） */
    private static final String DIR = "dummy-dir";

    /**
     * テスト用の{@link TestDataReader}実装。リソース名をキーに canned データを返す。
     */
    private static final class FakeTestDataReader implements TestDataReader {

        /** リソース名 → 行データ */
        private final Map<String, List<List<String>>> dataByResource = new HashMap<String, List<List<String>>>();

        /** 現在オープン中のイテレータ */
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

    /**
     * null セルを含められるよう{@link Arrays#asList}で行を組み立てる。
     *
     * @param cells セル
     * @return 行データ
     */
    private static List<String> row(String... cells) {
        return Arrays.asList(cells);
    }

    /**
     * Fake リーダに 1 リソース分の行を登録した{@link XlsFormatReader}を生成する。
     *
     * @param resource リソース名
     * @param lines    行データ
     * @return リーダ
     */
    private static XlsFormatReader readerOf(String resource, List<List<String>> lines) {
        FakeTestDataReader fake = new FakeTestDataReader().put(resource, lines);
        return new XlsFormatReader(new TestCoreReaderAdapter(fake));
    }

    // ------------------------------------------------------------------ table

    /**
     * Given: {@code ${...}}・空文字・null セルを含む SETUP_TABLE ブロック 1 件。
     * When : {@code read}。
     * Then : TableDataBlock に写され、IN 値が記法のまま（null と空文字を区別）。
     */
    @Test
    public void readMapsTableBlockPreservingRawValues() {
        String resource = "book/readMapsTableBlockPreservingRawValues";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("SETUP_TABLE=USERS"));
        lines.add(row("USER_NAME", "AGE"));
        lines.add(row("${userName}", ""));
        lines.add(row("literal", null));

        TestDataContainer container = readerOf(resource, lines).read(DIR, resource);

        List<TestDataBlock> blocks = container.getSections().get(0).getBlocks();
        assertThat(blocks.size(), is(1));
        TableDataBlock table = (TableDataBlock) blocks.get(0);
        assertThat(table.getIdentifier(), is("USERS"));
        assertThat(table.getColumnNames(), is(Arrays.asList("USER_NAME", "AGE")));
        assertThat(table.getRows().get(0), is(Arrays.asList("${userName}", "")));
        // null セルは null のまま（空文字と区別）
        assertThat(table.getRows().get(1).get(0), is("literal"));
        assertThat(table.getRows().get(1).get(1), is(nullValue()));
    }

    /**
     * Given: 同一タイプ・同一グループ（無指定）の SETUP_TABLE が 2 件。
     * When : {@code read}。
     * Then : 2 ブロックに展開され、一括取得の重複読みで増殖しない。
     */
    @Test
    public void readMapsMultipleTablesWithoutDuplication() {
        String resource = "book/readMapsMultipleTablesWithoutDuplication";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("SETUP_TABLE=USERS"));
        lines.add(row("USER_NAME"));
        lines.add(row("alice"));
        lines.add(row("SETUP_TABLE=ROLES"));
        lines.add(row("ROLE_NAME"));
        lines.add(row("admin"));

        TestDataContainer container = readerOf(resource, lines).read(DIR, resource);

        List<TestDataBlock> blocks = container.getSections().get(0).getBlocks();
        assertThat(blocks.size(), is(2));
        assertThat(blocks.get(0).getIdentifier(), is("USERS"));
        assertThat(blocks.get(1).getIdentifier(), is("ROLES"));
    }

    /**
     * Given: グループ ID 付きの EXPECTED_TABLE ブロック。
     * When : {@code read}。
     * Then : ブロックにグループ ID とデータタイプが保持される。
     */
    @Test
    public void readPreservesGroupIdAndDataType() {
        String resource = "book/readPreservesGroupIdAndDataType";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("EXPECTED_TABLE[g1]=USERS"));
        lines.add(row("USER_NAME"));
        lines.add(row("${u}"));

        TestDataContainer container = readerOf(resource, lines).read(DIR, resource);

        TableDataBlock table = (TableDataBlock) container.getSections().get(0).getBlocks().get(0);
        assertThat(table.getGroupId(), is("[g1]"));
        assertThat(table.getDataType(), is(nablarch.test.core.reader.DataType.EXPECTED_TABLE_DATA));
        assertThat(table.getRows().get(0).get(0), is("${u}"));
    }

    // ------------------------------------------------------------------ list_map

    /**
     * Given: {@code ${...}}・空文字を含む LIST_MAP ブロック。
     * When : {@code read}。
     * Then : ListMapBlock に写され、IN 値が記法のまま、列順が保たれる。
     */
    @Test
    public void readMapsListMapBlock() {
        String resource = "book/readMapsListMapBlock";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("LIST_MAP=result"));
        lines.add(row("ID", "NAME"));
        lines.add(row("${id}", ""));
        lines.add(row("2", "bob"));

        TestDataContainer container = readerOf(resource, lines).read(DIR, resource);

        ListMapBlock listMap = (ListMapBlock) container.getSections().get(0).getBlocks().get(0);
        assertThat(listMap.getIdentifier(), is("result"));
        assertThat(listMap.getColumnNames(), is(Arrays.asList("ID", "NAME")));
        assertThat(listMap.getRows().get(0), is(Arrays.asList("${id}", "")));
        assertThat(listMap.getRows().get(1), is(Arrays.asList("2", "bob")));
    }

    // ------------------------------------------------------------------ fixed file

    /**
     * Given: SETUP_FIXED の固定長ファイル（型・長さ・{@code ${...}}を含む）。
     * When : {@code read}。
     * Then : FileDataBlock（FIXED）に写され、レコードレイアウト・フィールド定義・行が無損失。
     */
    @Test
    public void readMapsFixedLengthFileBlock() {
        String resource = "book/readMapsFixedLengthFileBlock";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("SETUP_FIXED=test.dat"));
        lines.add(row("data", "field1", "field2"));
        lines.add(row("", "半角英字", "半角英字"));
        lines.add(row("", "10", "5"));
        lines.add(row("", "${value}", "abc"));

        TestDataContainer container = readerOf(resource, lines).read(DIR, resource);

        FileDataBlock file = (FileDataBlock) container.getSections().get(0).getBlocks().get(0);
        assertThat(file.getIdentifier(), is("test.dat"));
        assertThat(file.getFileType(), is(FileDataBlock.FileType.FIXED));
        assertThat(file.getRecords().size(), is(1));
        RecordLayout record = file.getRecords().get(0);
        assertThat(record.getRecordType(), is("data"));
        assertThat(record.getFields().size(), is(2));
        FieldDef f1 = record.getFields().get(0);
        assertThat(f1.getName(), is("field1"));
        // 型は原文（設計記法）が生行から復元される。器が FW シンボル（X/N/Z）へ正規化した値ではない。
        assertThat(f1.getType(), is("半角英字"));
        assertThat(f1.getLength(), is("10"));
        FieldDef f2 = record.getFields().get(1);
        assertThat(f2.getName(), is("field2"));
        assertThat(f2.getLength(), is("5"));
        // IN 値は記法のまま
        assertThat(record.getRows().get(0), is(Arrays.asList("${value}", "abc")));
    }

    /**
     * Given: 長さ省略（{@code -}）フィールドと明示レコード種別を含む SETUP_FIXED ブロック。
     * When : {@code read}。
     * Then : レコード種別・型記法・長さ（{@code -} 含む）が生行の原文どおり復元される
     *        （器が正規化した値＝実バイト長・FW シンボルは現れない）。
     */
    @Test
    public void readRestoresOriginalRecordTypeTypeAndOmittedLengthFromRawLines() {
        String resource = "book/readRestoresOriginalRecordTypeTypeAndOmittedLength";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("SETUP_FIXED=om.dat"));
        lines.add(row("text-encoding", "UTF-8"));        // ディレクティブ行（名前行より前）
        lines.add(row("rt", "f1", "f2"));                // 名前行（列0=レコード種別）
        lines.add(row("", "半角英字", "半角英字"));        // 型（設計記法）
        lines.add(row("", "-", "5"));                    // 長さ（f1 は省略）
        lines.add(row("", "abcd", "xy"));                // データ行

        TestDataContainer container = readerOf(resource, lines).read(DIR, resource);

        FileDataBlock file = (FileDataBlock) container.getSections().get(0).getBlocks().get(0);
        RecordLayout record = file.getRecords().get(0);
        // レコード種別は生行の名前行 列0 から（器では private で読めない）
        assertThat(record.getRecordType(), is("rt"));
        FieldDef f1 = record.getFields().get(0);
        // 型は原文記法（器の FW シンボル X ではない）
        assertThat(f1.getType(), is("半角英字"));
        // 長さ省略 "-" は原文どおり（器が上書きする実バイト長 "4" ではない）
        assertThat(f1.getLength(), is("-"));
        assertThat(record.getFields().get(1).getLength(), is("5"));
        // 値は記法のまま
        assertThat(record.getRows().get(0), is(Arrays.asList("abcd", "xy")));
    }

    /**
     * Given: SETUP_VARIABLE の可変長ファイル（長さなし）。
     * When : {@code read}。
     * Then : FileDataBlock（VARIABLE）に写され、長さは省略（{@code null}）。
     */
    @Test
    public void readMapsVariableLengthFileBlock() {
        String resource = "book/readMapsVariableLengthFileBlock";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("SETUP_VARIABLE=in.csv"));
        lines.add(row("data", "f1"));
        lines.add(row("", "半角英字"));
        lines.add(row("", "${v}"));

        TestDataContainer container = readerOf(resource, lines).read(DIR, resource);

        FileDataBlock file = (FileDataBlock) container.getSections().get(0).getBlocks().get(0);
        assertThat(file.getFileType(), is(FileDataBlock.FileType.VARIABLE));
        RecordLayout record = file.getRecords().get(0);
        assertThat(record.getFields().get(0).getName(), is("f1"));
        assertThat(record.getFields().get(0).getLength(), is(nullValue()));
        assertThat(record.getRows().get(0), is(Arrays.asList("${v}")));
    }

    // ------------------------------------------------------------------ message

    /**
     * Given: FW 制御ヘッダ＋本文を持つ MESSAGE ブロック。
     * When : {@code read}。
     * Then : MessageDataBlock に写され、FW ヘッダ・本文レコードが記法のまま。
     */
    @Test
    public void readMapsMessageBlock() {
        String resource = "book/readMapsMessageBlock";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("MESSAGE=msg1"));
        lines.add(row("requestId", "${rid}"));
        lines.add(row("data", "body1", "body2"));
        lines.add(row("", "半角英字", "半角英字"));
        lines.add(row("", "10", "5"));
        lines.add(row("", "${b}", "xyz"));

        TestDataContainer container = readerOf(resource, lines).read(DIR, resource);

        MessageDataBlock message = (MessageDataBlock) container.getSections().get(0).getBlocks().get(0);
        assertThat(message.getIdentifier(), is("msg1"));
        assertThat(message.getDataType(), is(nablarch.test.core.reader.DataType.MESSAGE));
        // FW ヘッダは記法のまま
        assertThat(message.getFwHeaderFields().get("requestId"), is("${rid}"));
        // 本文レコード
        assertThat(message.getRecords().size(), is(1));
        RecordLayout record = message.getRecords().get(0);
        assertThat(record.getFields().get(0).getName(), is("body1"));
        assertThat(record.getRows().get(0), is(Arrays.asList("${b}", "xyz")));
    }

    // ------------------------------------------------------------- send-sync messages

    /**
     * Given: {@code no} 列＋本文フィールドを持つ EXPECTED_REQUEST_HEADER_MESSAGES ブロック
     *        （{@code TYPE[group]=id} 形式マーカー）。
     * When : {@code read}。
     * Then : MessageDataBlock に写される。グループ ID は {@code [case1]}、識別子は {@code =} 以降。
     *        {@code no} 列はメタ情報のため脱落し、本文フィールド・値は記法のまま。FW ヘッダは空。
     */
    @Test
    public void readMapsExpectedRequestHeaderMessageBlock() {
        String resource = "book/readMapsExpectedRequestHeaderMessageBlock";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("EXPECTED_REQUEST_HEADER_MESSAGES[case1]=RM21AA0104_01"));
        lines.add(row("text-encoding", "ms932"));
        lines.add(row("no", "requestId", "resendFlag"));
        lines.add(row("", "半角", "半角"));
        lines.add(row("", "20", "1"));
        lines.add(row("1", "RM21AA0104_01", "0"));

        TestDataContainer container = readerOf(resource, lines).read(DIR, resource);

        MessageDataBlock message = (MessageDataBlock) container.getSections().get(0).getBlocks().get(0);
        assertThat(message.getDataType(), is(nablarch.test.core.reader.DataType.EXPECTED_REQUEST_HEADER_MESSAGES));
        assertThat(message.getGroupId(), is("[case1]"));
        assertThat(message.getIdentifier(), is("RM21AA0104_01"));
        // 送信系に FW 制御ヘッダは無い（常に空）
        assertTrue(message.getFwHeaderFields().isEmpty());
        // ディレクティブは記法のまま
        assertThat(message.getDirectives().get("text-encoding"), is("ms932"));
        // 本文レコード: no 列は脱落し、フィールドは requestId/resendFlag のみ
        assertThat(message.getRecords().size(), is(1));
        RecordLayout record = message.getRecords().get(0);
        List<String> fieldNames = new ArrayList<String>();
        for (FieldDef field : record.getFields()) {
            fieldNames.add(field.getName());
        }
        assertThat(fieldNames, is(Arrays.asList("requestId", "resendFlag")));
        assertThat(record.getFields().get(0).getType(), is("半角"));
        assertThat(record.getFields().get(0).getLength(), is("20"));
        // 値行も no（NO 値）が脱落し、本文値のみ
        assertThat(record.getRows().get(0), is(Arrays.asList("RM21AA0104_01", "0")));
    }

    /**
     * Given: 同一グループ {@code [case1]} に EXPECTED_REQUEST_HEADER/BODY、
     *        グループ {@code [res_case1]} に RESPONSE_HEADER/BODY を持つシート。
     * When : {@code read}。
     * Then : 4 種すべてが MessageDataBlock として写り、データタイプ・グループ ID が保たれる。
     */
    @Test
    public void readMapsAllFourSendSyncMessageTypes() {
        String resource = "book/readMapsAllFourSendSyncMessageTypes";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("EXPECTED_REQUEST_HEADER_MESSAGES[case1]=RM21AA0104_01"));
        lines.add(row("no", "requestId"));
        lines.add(row("", "半角"));
        lines.add(row("", "20"));
        lines.add(row("1", "RM21AA0104_01"));
        lines.add(row("EXPECTED_REQUEST_BODY_MESSAGES[case1]=RM21AA0104_01"));
        lines.add(row("no", "userId"));
        lines.add(row("", "半角"));
        lines.add(row("", "10"));
        lines.add(row("1", "user01"));
        lines.add(row("RESPONSE_HEADER_MESSAGES[res_case1]=RM21AA0104_01"));
        lines.add(row("no", "requestId"));
        lines.add(row("", "半角"));
        lines.add(row("", "20"));
        lines.add(row("1", "RM21AA0101"));
        lines.add(row("RESPONSE_BODY_MESSAGES[res_case1]=RM21AA0104_01"));
        lines.add(row("no", "failureCode"));
        lines.add(row("", "半角"));
        lines.add(row("", "20"));
        lines.add(row("1", "0"));

        TestDataContainer container = readerOf(resource, lines).read(DIR, resource);

        List<TestDataBlock> blocks = container.getSections().get(0).getBlocks();
        Map<nablarch.test.core.reader.DataType, String> typeToGroup =
                new HashMap<nablarch.test.core.reader.DataType, String>();
        for (TestDataBlock block : blocks) {
            assertTrue(block instanceof MessageDataBlock);
            MessageDataBlock m = (MessageDataBlock) block;
            typeToGroup.put(m.getDataType(), m.getGroupId());
        }
        assertThat(blocks.size(), is(4));
        assertThat(typeToGroup.get(nablarch.test.core.reader.DataType.EXPECTED_REQUEST_HEADER_MESSAGES), is("[case1]"));
        assertThat(typeToGroup.get(nablarch.test.core.reader.DataType.EXPECTED_REQUEST_BODY_MESSAGES), is("[case1]"));
        assertThat(typeToGroup.get(nablarch.test.core.reader.DataType.RESPONSE_HEADER_MESSAGES), is("[res_case1]"));
        assertThat(typeToGroup.get(nablarch.test.core.reader.DataType.RESPONSE_BODY_MESSAGES), is("[res_case1]"));
    }

    /**
     * Given: 同一データタイプ・同一グループに識別子の異なる 2 ブロック。
     * When : {@code read}。
     * Then : 各識別子ごとに MessageDataBlock が 1 件ずつ写る。
     */
    @Test
    public void readMapsMultipleSendSyncBlocksInSameGroup() {
        String resource = "book/readMapsMultipleSendSyncBlocksInSameGroup";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("EXPECTED_REQUEST_HEADER_MESSAGES[case1]=RM21AA0104_01"));
        lines.add(row("no", "requestId"));
        lines.add(row("", "半角"));
        lines.add(row("", "20"));
        lines.add(row("1", "RM21AA0104_01"));
        lines.add(row("EXPECTED_REQUEST_HEADER_MESSAGES[case1]=RM21AA0104_02"));
        lines.add(row("no", "requestId"));
        lines.add(row("", "半角"));
        lines.add(row("", "20"));
        lines.add(row("1", "RM21AA0104_02"));

        TestDataContainer container = readerOf(resource, lines).read(DIR, resource);

        List<TestDataBlock> blocks = container.getSections().get(0).getBlocks();
        assertThat(blocks.size(), is(2));
        List<String> identifiers = new ArrayList<String>();
        for (TestDataBlock block : blocks) {
            identifiers.add(((MessageDataBlock) block).getIdentifier());
        }
        assertThat(identifiers, hasItem("RM21AA0104_01"));
        assertThat(identifiers, hasItem("RM21AA0104_02"));
    }

    // ------------------------------------------------------------------ container / section

    /**
     * Given: {@code "ブック名/シート名"} 形式のリソース名。
     * When : {@code read}。
     * Then : コンテナ名＝ブック名、セクション名＝シート名。
     */
    @Test
    public void readDerivesContainerAndSectionNamesFromResource() {
        String resource = "MyBook/MySheet";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("SETUP_TABLE=T"));
        lines.add(row("C"));
        lines.add(row("v"));

        TestDataContainer container = readerOf(resource, lines).read(DIR, resource);

        assertThat(container.getName(), is("MyBook"));
        assertThat(container.getSections().size(), is(1));
        assertThat(container.getSections().get(0).getName(), is("MySheet"));
    }

    /**
     * Given: マーカー行が存在しないリソース。
     * When : {@code read}。
     * Then : ブロックが空のセクションを 1 つ持つコンテナを返す。
     */
    @Test
    public void readReturnsEmptySectionWhenNoBlocks() {
        String resource = "book/readReturnsEmptySectionWhenNoBlocks";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("just", "data"));

        TestDataContainer container = readerOf(resource, lines).read(DIR, resource);

        assertThat(container.getSections().size(), is(1));
        assertTrue(container.getSections().get(0).getBlocks().isEmpty());
    }

    /**
     * Given: テーブル・固定長ファイル・LIST_MAP・MESSAGE が混在するリソース。
     * When : {@code read}。
     * Then : 1 セクションに 4 種のブロックが揃う（全種別を 1 シートから組み立てられる）。
     */
    @Test
    public void readAssemblesMixedBlockTypesInOneSection() {
        String resource = "book/readAssemblesMixedBlockTypesInOneSection";
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(row("SETUP_TABLE=T"));
        lines.add(row("C"));
        lines.add(row("v"));
        lines.add(row("SETUP_FIXED=f.dat"));
        lines.add(row("data", "f1"));
        lines.add(row("", "半角英字"));
        lines.add(row("", "5"));
        lines.add(row("", "x"));
        lines.add(row("LIST_MAP=lm"));
        lines.add(row("K"));
        lines.add(row("1"));
        lines.add(row("MESSAGE=m"));
        lines.add(row("requestId", "R"));
        lines.add(row("data", "b1"));
        lines.add(row("", "半角英字"));
        lines.add(row("", "3"));
        lines.add(row("", "y"));

        TestDataContainer container = readerOf(resource, lines).read(DIR, resource);

        TestDataSection section = container.getSections().get(0);
        List<Class<?>> kinds = new ArrayList<Class<?>>();
        for (TestDataBlock block : section.getBlocks()) {
            kinds.add(block.getClass());
        }
        assertThat(kinds.size(), is(4));
        assertThat(kinds, hasItem((Class<?>) TableDataBlock.class));
        assertThat(kinds, hasItem((Class<?>) FileDataBlock.class));
        assertThat(kinds, hasItem((Class<?>) ListMapBlock.class));
        assertThat(kinds, hasItem((Class<?>) MessageDataBlock.class));
    }
}
