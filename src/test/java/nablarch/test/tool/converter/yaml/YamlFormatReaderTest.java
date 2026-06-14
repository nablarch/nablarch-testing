package nablarch.test.tool.converter.yaml;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nablarch.test.core.file.DataFile;
import nablarch.test.core.reader.DataType;
import nablarch.test.core.reader.YamlTestCoreAdapter;
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
 * {@link YamlFormatReader} のテストクラス。
 *
 * <p>
 * 本リーダは本体 YAML ビルダ（{@link YamlTestCoreAdapter} 経由）で構造を得て、{@code loadRawMap} の
 * 順序保持 Map から原文を復元し中間モデルへ写す。各テストは {@code loadRawMap} のみを差し替えた
 * アダプタ（{@link #reader(Map)}）を用い、本体の実ビルダを通る統合テストとしてブロックの内容を検証する
 * （{@code read*} は内部で {@code loadRawMap} を呼ぶため、差し替え一点で器も原文も同一の in-memory Map から駆動する）。
 * </p>
 *
 * @author kiyobot
 */
public class YamlFormatReaderTest {

    private static final String DIR = "unused";
    private static final String RESOURCE = "testdata";

    // ------------------------------------------------------------------------
    // テーブル系
    // ------------------------------------------------------------------------

    @Test
    public void readTable_setup_mapsUppercaseNameAndColumnsWithRawValues() {
        // Given: setup_tables の users（${...}・null・"" を含む）
        Map<String, Object> yaml = map(
                "setup_tables", list(
                        map("table", "users", "rows", list(
                                map("id", "1", "name", "${user.name}", "note", null),
                                map("id", "2", "name", "", "note", "x")))));

        // When
        TestDataContainer container = reader(yaml).read(DIR, RESOURCE);

        // Then: 器の大文字テーブル名・大文字カラム・記法のままの値（null/""保持）
        TableDataBlock block = (TableDataBlock) onlyBlock(container);
        assertThat(block.getDataType(), is(DataType.SETUP_TABLE_DATA));
        assertThat(block.getGroupId(), is(""));
        assertThat(block.getIdentifier(), is("USERS"));
        assertThat(block.getColumnNames(), is(list("ID", "NAME", "NOTE")));
        assertThat(block.getRows().get(0), is(Arrays.asList("1", "${user.name}", null)));
        assertThat(block.getRows().get(1), is(Arrays.asList("2", "", "x")));
    }

    @Test
    public void readTable_expectedWithGroup_formatsGroupIdAndCreatesBlockPerGroup() {
        // Given: 同一セクションに 2 グループ（記述順）
        Map<String, Object> yaml = map(
                "expected_tables", list(
                        map("group_id", "case01", "table", "orders",
                                "rows", list(map("order_id", "10"))),
                        map("group_id", "case02", "table", "orders",
                                "rows", list(map("order_id", "20")))));

        // When
        TestDataContainer container = reader(yaml).read(DIR, RESOURCE);

        // Then: グループごとに 1 ブロック・整形済みグループ ID
        List<TestDataBlock> blocks = blocks(container);
        assertThat(blocks.size(), is(2));
        assertThat(blocks.get(0).getDataType(), is(DataType.EXPECTED_TABLE_DATA));
        assertThat(blocks.get(0).getGroupId(), is("[case01]"));
        assertThat(((TableDataBlock) blocks.get(0)).getRows().get(0), is(Arrays.asList("10")));
        assertThat(blocks.get(1).getGroupId(), is("[case02]"));
        assertThat(((TableDataBlock) blocks.get(1)).getRows().get(0), is(Arrays.asList("20")));
    }

    @Test
    public void readTable_completed_mapsExpectedCompletedType() {
        // Given
        Map<String, Object> yaml = map(
                "expected_complete_tables", list(
                        map("table", "items", "rows", list(map("item_id", "A")))));

        // When
        TestDataContainer container = reader(yaml).read(DIR, RESOURCE);

        // Then
        TableDataBlock block = (TableDataBlock) onlyBlock(container);
        assertThat(block.getDataType(), is(DataType.EXPECTED_COMPLETED));
        assertThat(block.getIdentifier(), is("ITEMS"));
    }

    // ------------------------------------------------------------------------
    // LIST_MAP
    // ------------------------------------------------------------------------

    @Test
    public void readListMap_preservesYamlColumnOrderExcludesMarkersAndKeepsNull() {
        // Given: マーカーカラム [ignore] を含み、2 行目は null 値
        Map<String, Object> yaml = map(
                "list_maps", list(
                        map("id", "lm1", "rows", list(
                                map("key", "k1", "[ignore]", "m", "val", "${v1}"),
                                map("key", "k2", "[ignore]", "m", "val", null)))));

        // When
        TestDataContainer container = reader(yaml).read(DIR, RESOURCE);

        // Then: カラムは YAML 順・マーカー除外、値は記法のまま・null 保持
        ListMapBlock block = (ListMapBlock) onlyBlock(container);
        assertThat(block.getDataType(), is(DataType.LIST_MAP));
        assertThat(block.getIdentifier(), is("lm1"));
        assertThat(block.getColumnNames(), is(list("key", "val")));
        assertThat(block.getRows().get(0), is(Arrays.asList("k1", "${v1}")));
        assertThat(block.getRows().get(1), is(Arrays.asList("k2", null)));
    }

    // ------------------------------------------------------------------------
    // ファイル系
    // ------------------------------------------------------------------------

    @Test
    public void readFile_fixed_mapsRawFieldDefsAndValues() {
        // Given: 固定長・型/長さ記法（length 省略フィールド含む）・directives・複数レコード
        Map<String, Object> yaml = map(
                "setup_files", list(
                        map("path", "input.dat", "type", "fixed",
                                "directives", map("file-type", "Fixed", "text-encoding", "UTF-8"),
                                "records", list(
                                        map("record_type", "head",
                                                "fields", list(field("f1", "半角英字", "5")),
                                                "rows", list(list("${a}"))),
                                        map("record_type", "data",
                                                "fields", list(field("f2", "数値", null)),
                                                "rows", list(list("12"), list("")))))));

        // When
        TestDataContainer container = reader(yaml).read(DIR, RESOURCE);

        // Then
        FileDataBlock block = (FileDataBlock) onlyBlock(container);
        assertThat(block.getDataType(), is(DataType.SETUP_FIXED));
        assertThat(block.getFileType(), is(FileDataBlock.FileType.FIXED));
        assertThat(block.getIdentifier(), is("input.dat"));
        assertThat(block.getDirectives().get("text-encoding"), is("UTF-8"));
        assertThat(block.getRecords().size(), is(2));

        RecordLayout head = block.getRecords().get(0);
        assertThat(head.getRecordType(), is("head"));
        assertFieldDef(head.getFields().get(0), "f1", "半角英字", "5");
        assertThat(head.getRows().get(0), is(Arrays.asList("${a}")));

        RecordLayout data = block.getRecords().get(1);
        assertThat(data.getRecordType(), is("data"));
        // 長さ省略は Map 原文どおり null（器の正規化値は使わない）
        assertFieldDef(data.getFields().get(0), "f2", "数値", null);
        assertThat(data.getRows().get(0), is(Arrays.asList("12")));
        assertThat(data.getRows().get(1), is(Arrays.asList("")));
    }

    @Test
    public void readFile_variable_mapsVariableTypeWithNullLengths() {
        // Given: 可変長（長さ行なし）
        Map<String, Object> yaml = map(
                "expected_files", list(
                        map("path", "out.csv", "type", "variable",
                                "records", list(
                                        map("fields", list(field("c1", "半角英字", null), field("c2", "半角英字", null)),
                                                "rows", list(list("x", "${b}")))))));

        // When
        TestDataContainer container = reader(yaml).read(DIR, RESOURCE);

        // Then
        FileDataBlock block = (FileDataBlock) onlyBlock(container);
        assertThat(block.getDataType(), is(DataType.EXPECTED_VARIABLE));
        assertThat(block.getFileType(), is(FileDataBlock.FileType.VARIABLE));
        RecordLayout record = block.getRecords().get(0);
        assertFieldDef(record.getFields().get(0), "c1", "半角英字", null);
        assertFieldDef(record.getFields().get(1), "c2", "半角英字", null);
        assertThat(record.getRows().get(0), is(Arrays.asList("x", "${b}")));
    }

    // ------------------------------------------------------------------------
    // MESSAGE
    // ------------------------------------------------------------------------

    @Test
    public void readMessage_mapsRawFwHeaderAndExcludesFwHeaderRecord() {
        // Given: fw_header（${...} 未加工）と FW_HEADER レコード＋本文レコードの併存
        Map<String, Object> yaml = map(
                "messages", list(
                        map("id", "RM01",
                                "fw_header", map("requestId", "RM01", "userId", "${user}"),
                                "records", list(
                                        map("record_type", "FW_HEADER",
                                                "fields", list(field("h1", "半角英字", "1")),
                                                "rows", list(list("z"))),
                                        map("record_type", "body",
                                                "fields", list(field("m1", "半角英字", "3")),
                                                "rows", list(list("abc")))))));

        // When
        TestDataContainer container = reader(yaml).read(DIR, RESOURCE);

        // Then: FW ヘッダは原文・MESSAGE 種別・グループ空、本文は FW_HEADER レコードを除いた 1 件
        MessageDataBlock block = (MessageDataBlock) onlyBlock(container);
        assertThat(block.getDataType(), is(DataType.MESSAGE));
        assertThat(block.getGroupId(), is(""));
        assertThat(block.getIdentifier(), is("RM01"));
        assertThat(block.getFwHeaderFields().get("requestId"), is("RM01"));
        assertThat(block.getFwHeaderFields().get("userId"), is("${user}"));
        assertThat(block.getRecords().size(), is(1));
        assertThat(block.getRecords().get(0).getRecordType(), is("body"));
        assertFieldDef(block.getRecords().get(0).getFields().get(0), "m1", "半角英字", "3");
        assertThat(block.getRecords().get(0).getRows().get(0), is(Arrays.asList("abc")));
    }

    @Test
    public void readMessage_emptyBody_isStillMapped() {
        // Given: id はあるが本文レコードが無いエントリ
        Map<String, Object> yaml = map(
                "messages", list(map("id", "EMPTY")));

        // When
        TestDataContainer container = reader(yaml).read(DIR, RESOURCE);

        // Then: 本文レコード 0 件・FW ヘッダ空のブロックが 1 つ
        MessageDataBlock block = (MessageDataBlock) onlyBlock(container);
        assertThat(block.getIdentifier(), is("EMPTY"));
        assertThat(block.getRecords().size(), is(0));
        assertTrue(block.getFwHeaderFields().isEmpty());
    }

    @Test
    public void readMessage_nullContent_isSkipped() {
        // Given: 器（readMessage）が null を返す（本文が存在しない）
        final Map<String, Object> yaml = map(
                "messages", list(map("id", "ABSENT", "records", list())));
        YamlTestCoreAdapter adapter = new YamlTestCoreAdapter() {
            @Override
            public Map<String, Object> loadRawMap(String path, String resource) {
                return yaml;
            }

            @Override
            public nablarch.test.core.reader.yaml.YamlMessageBuilder.MessageContent readMessage(
                    String path, String resource, String id) {
                return null;
            }
        };

        // When
        TestDataContainer container = new YamlFormatReader(adapter).read(DIR, RESOURCE);

        // Then: ブロック化されない
        assertThat(blocks(container).size(), is(0));
    }

    // ------------------------------------------------------------------------
    // 送信同期メッセージ（送信系 4 種）
    // ------------------------------------------------------------------------

    @Test
    public void readSendSync_groupsByRawValueFormatsGroupIdAndKeepsNoField() {
        // Given: 同一グループ case1 に 2 件・別グループ case2 に 1 件。"no" フィールドも原文どおり保持。
        Map<String, Object> yaml = map(
                "expected_request_header_messages", list(
                        map("group_id", "case1", "id", "MSG1",
                                "records", list(map("fields", list(field("no", "半角英字", "1"), field("s1", "半角英字", "2")),
                                        "rows", list(list("1", "${z}"))))),
                        map("group_id", "case1", "id", "MSG2",
                                "records", list(map("fields", list(field("t1", "半角英字", "2")),
                                        "rows", list(list("pq"))))),
                        map("group_id", "case2", "id", "MSG3",
                                "records", list(map("fields", list(field("u1", "半角英字", "2")),
                                        "rows", list(list("rs")))))));

        // When
        TestDataContainer container = reader(yaml).read(DIR, RESOURCE);

        // Then: 記述順 MSG1/MSG2（[case1]）・MSG3（[case2]）、送信系種別、FW ヘッダ空、"no" 保持
        List<TestDataBlock> blocks = blocks(container);
        assertThat(blocks.size(), is(3));
        MessageDataBlock msg1 = (MessageDataBlock) blocks.get(0);
        assertThat(msg1.getDataType(), is(DataType.EXPECTED_REQUEST_HEADER_MESSAGES));
        assertThat(msg1.getGroupId(), is("[case1]"));
        assertThat(msg1.getIdentifier(), is("MSG1"));
        assertTrue(msg1.getFwHeaderFields().isEmpty());
        assertFieldDef(msg1.getRecords().get(0).getFields().get(0), "no", "半角英字", "1");
        assertThat(msg1.getRecords().get(0).getRows().get(0), is(Arrays.asList("1", "${z}")));
        assertThat(((MessageDataBlock) blocks.get(1)).getIdentifier(), is("MSG2"));
        assertThat(((MessageDataBlock) blocks.get(1)).getGroupId(), is("[case1]"));
        assertThat(((MessageDataBlock) blocks.get(2)).getIdentifier(), is("MSG3"));
        assertThat(((MessageDataBlock) blocks.get(2)).getGroupId(), is("[case2]"));
    }

    @Test
    public void readSendSync_allFourTypesAreRecognized() {
        // Given: 送信系 4 種それぞれ 1 件
        Map<String, Object> yaml = map(
                "expected_request_header_messages", list(sendSyncEntry("g", "A")),
                "expected_request_body_messages", list(sendSyncEntry("g", "B")),
                "response_header_messages", list(sendSyncEntry("g", "C")),
                "response_body_messages", list(sendSyncEntry("g", "D")));

        // When
        List<TestDataBlock> blocks = blocks(reader(yaml).read(DIR, RESOURCE));

        // Then: 4 種が記述順に並ぶ
        assertThat(blocks.size(), is(4));
        assertThat(blocks.get(0).getDataType(), is(DataType.EXPECTED_REQUEST_HEADER_MESSAGES));
        assertThat(blocks.get(1).getDataType(), is(DataType.EXPECTED_REQUEST_BODY_MESSAGES));
        assertThat(blocks.get(2).getDataType(), is(DataType.RESPONSE_HEADER_MESSAGES));
        assertThat(blocks.get(3).getDataType(), is(DataType.RESPONSE_BODY_MESSAGES));
    }

    // ------------------------------------------------------------------------
    // セクション構成・コンテナ
    // ------------------------------------------------------------------------

    @Test
    public void read_mixedSections_keepsDescriptionOrderAndIgnoresUnknownKeys() {
        // Given: 1 ファイルに複数セクション＋未知キー
        Map<String, Object> yaml = map(
                "unknown_section", list(map("x", "y")),
                "setup_tables", list(map("table", "t1", "rows", list(map("c", "1")))),
                "messages", list(map("id", "M", "records", list(
                        map("fields", list(field("f", "半角英字", "1")), "rows", list(list("v")))))));

        // When
        TestDataContainer container = reader(yaml).read(DIR, RESOURCE);

        // Then: 既知 2 セクションのみが記述順でブロック化される
        List<TestDataBlock> blocks = blocks(container);
        assertThat(blocks.size(), is(2));
        assertTrue(blocks.get(0) instanceof TableDataBlock);
        assertTrue(blocks.get(1) instanceof MessageDataBlock);
    }

    @Test
    public void read_namesContainerAndSectionByResourceName() {
        // When
        TestDataContainer container = reader(map()).read(DIR, RESOURCE);

        // Then: コンテナ名・セクション名はともにリソース名、ブロックは空
        assertThat(container.getName(), is(RESOURCE));
        assertThat(container.getSections().size(), is(1));
        TestDataSection section = container.getSections().get(0);
        assertThat(section.getName(), is(RESOURCE));
        assertThat(section.getBlocks().size(), is(0));
    }

    // ------------------------------------------------------------------------
    // fail-fast（器↔原文の不整合）
    // ------------------------------------------------------------------------

    @Test
    public void read_containerCountMismatch_failsFast() {
        // Given: Map には setup_files エントリが 1 件あるが、器（readFiles）は 0 件を返す
        final Map<String, Object> yaml = map(
                "setup_files", list(map("path", "input.dat", "type", "fixed",
                        "records", list(map("fields", list(field("f", "半角英字", "1")), "rows", list(list("v")))))));
        YamlTestCoreAdapter adapter = new YamlTestCoreAdapter() {
            @Override
            public Map<String, Object> loadRawMap(String path, String resource) {
                return yaml;
            }

            @Override
            public List<DataFile> readFiles(String path, String resource, String groupId, DataType type) {
                return new ArrayList<DataFile>();
            }
        };

        // When / Then
        try {
            new YamlFormatReader(adapter).read(DIR, RESOURCE);
            fail("should throw");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("器と原文エントリが不整合"));
        }
    }

    @Test
    public void read_fragmentRecordMismatch_failsFast() {
        // Given: 器が 2 断片を生成する Map（reader 内部の read* 用）に対し、
        //        reader がトップレベルで読む原文は records 1 件 ＝ 断片数と不整合
        final Map<String, Object> oneRecord = map(
                "setup_files", list(map("path", "f.dat", "type", "fixed",
                        "records", list(map("fields", list(field("a", "半角英字", "1")), "rows", list(list("p")))))));
        final Map<String, Object> twoRecords = map(
                "setup_files", list(map("path", "f.dat", "type", "fixed",
                        "records", list(
                                map("fields", list(field("a", "半角英字", "1")), "rows", list(list("p"))),
                                map("fields", list(field("b", "半角英字", "1")), "rows", list(list("q")))))));
        YamlTestCoreAdapter adapter = new YamlTestCoreAdapter() {
            private int calls = 0;

            @Override
            public Map<String, Object> loadRawMap(String path, String resource) {
                // 1 回目（reader トップレベル）は records 1 件、2 回目以降（read* 内部）は 2 件。
                return calls++ == 0 ? oneRecord : twoRecords;
            }
        };

        // When / Then: 断片 2 ≠ 原文レコード 1 で fail-fast
        try {
            new YamlFormatReader(adapter).read(DIR, RESOURCE);
            fail("should throw");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("器の断片構造と原文レコードが不整合"));
        }
    }

    // ------------------------------------------------------------------------
    // ヘルパー
    // ------------------------------------------------------------------------

    /** {@code loadRawMap} を固定 Map に差し替えたアダプタで SUT を生成する。 */
    private static YamlFormatReader reader(final Map<String, Object> yaml) {
        return new YamlFormatReader(new YamlTestCoreAdapter() {
            @Override
            public Map<String, Object> loadRawMap(String path, String resource) {
                return yaml;
            }
        });
    }

    /** 送信系の 1 エントリ（group/id・単一フィールド）を組み立てる。 */
    private static Map<String, Object> sendSyncEntry(String group, String id) {
        return map("group_id", group, "id", id,
                "records", list(map("fields", list(field("f", "半角英字", "1")), "rows", list(list("v")))));
    }

    /** records.fields の 1 要素を組み立てる（length は null 可）。 */
    private static Map<String, Object> field(String name, String type, String length) {
        Map<String, Object> field = new LinkedHashMap<String, Object>();
        field.put("name", name);
        field.put("type", type);
        if (length != null) {
            field.put("length", length);
        }
        return field;
    }

    private static List<TestDataBlock> blocks(TestDataContainer container) {
        return container.getSections().get(0).getBlocks();
    }

    private static TestDataBlock onlyBlock(TestDataContainer container) {
        List<TestDataBlock> blocks = blocks(container);
        assertThat(blocks.size(), is(1));
        return blocks.get(0);
    }

    private static void assertFieldDef(FieldDef field, String name, String type, String length) {
        assertThat(field.getName(), is(name));
        assertThat(field.getType(), is(type));
        if (length == null) {
            assertThat(field.getLength(), is(nullValue()));
        } else {
            assertThat(field.getLength(), is(length));
        }
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    private static List<Object> list(Object... values) {
        return new ArrayList<Object>(Arrays.asList(values));
    }

    /** 文字列リスト用の {@code list}（型推論補助）。 */
    private static List<String> list(String... values) {
        return new ArrayList<String>(Arrays.asList(values));
    }
}
