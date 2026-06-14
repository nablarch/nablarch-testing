package nablarch.test.core.reader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nablarch.test.core.db.TableData;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.file.TestCoreFileAdapter;
import nablarch.test.core.file.TestCoreFileAdapter.FileView;
import nablarch.test.core.file.TestCoreFileAdapter.FragmentView;
import nablarch.test.core.file.VariableLengthFile;
import nablarch.test.core.reader.yaml.YamlLoader;
import nablarch.test.core.reader.yaml.YamlMessageBuilder.MessageContent;

import org.junit.After;
import org.junit.Test;

/**
 * {@link YamlTestCoreAdapter} のテストクラス。
 * <p>
 * 本アダプタは本体 YAML ビルダを空インタープリタ・補完なしで配線し、生の本体器を取り出す。Excel 経路の
 * {@link TestCoreReaderAdapter} と対称。各テストは「本体器を返すこと」と「IN 値が記法のまま（未加工。
 * {@code ${...}}・{@code ${binaryFile:...}}・{@code null}・{@code ""} がそのまま）であること」、および
 * {@code loadRawMap} が順序保持の原文 Map を返すことを検証する。
 * </p>
 *
 * @author kiyobot
 */
public class YamlTestCoreAdapterTest {

    private static final String DIR = "src/test/java/nablarch/test/core/reader/";

    private final YamlTestCoreAdapter sut = new YamlTestCoreAdapter();

    @After
    public void after() {
        YamlLoader.clearCacheForTest();
    }

    // ------------------------------------------------------------------------
    // テーブル系
    // ------------------------------------------------------------------------

    @Test
    public void readTables_setup_returnsRawTableData() {
        // Given: setup_tables の users（${...}・null・""・${binaryFile:} を含む）
        // When
        List<TableData> tables = sut.readTables(DIR, "YamlTestCoreAdapterTest/tables", "", DataType.SETUP_TABLE_DATA);

        // Then: 1 テーブル・値は記法のまま（binaryFile は HexString へ解決されない）
        assertThat(tables.size(), is(1));
        TableData users = tables.get(0);
        assertThat(users.getTableName(), is("USERS"));
        assertThat(value(users, 0, "NAME"), is("${user.name}"));
        assertThat(value(users, 0, "NOTE"), is(nullValue()));
        assertThat(value(users, 1, "NAME"), is(""));
        assertThat(value(users, 1, "NOTE"), is("${binaryFile:data/x.bin}"));
    }

    @Test
    public void readTables_expectedWithGroup_filtersByGroupId() {
        // Given: expected_tables の orders（group_id=case01）
        // When: 整形済みグループ ID で取得
        List<TableData> match = sut.readTables(DIR, "YamlTestCoreAdapterTest/tables", "[case01]",
                DataType.EXPECTED_TABLE_DATA);
        List<TableData> noMatch = sut.readTables(DIR, "YamlTestCoreAdapterTest/tables", "[other]",
                DataType.EXPECTED_TABLE_DATA);

        // Then
        assertThat(match.size(), is(1));
        assertThat(match.get(0).getTableName(), is("ORDERS"));
        assertThat(noMatch.size(), is(0));
    }

    @Test
    public void readTables_completed_doesNotFillDefaults() {
        // Given: expected_complete_tables の items
        // When: 補完なしで取得（fillDefaultValues が走れば StubDbInfo の番人メソッドが発火し例外になる）
        List<TableData> tables = sut.readTables(DIR, "YamlTestCoreAdapterTest/tables", "",
                DataType.EXPECTED_COMPLETED);

        // Then: 例外なく取得でき、値は記法のまま＝補完が走っていない
        assertThat(tables.size(), is(1));
        assertThat(tables.get(0).getTableName(), is("ITEMS"));
        assertThat(value(tables.get(0), 0, "PRICE"), is("${item.price}"));
    }

    @Test
    public void readTables_unsupportedType_throws() {
        try {
            sut.readTables(DIR, "YamlTestCoreAdapterTest/tables", "", DataType.LIST_MAP);
            fail("should throw");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("readTables"));
        }
    }

    @Test
    public void readListMap_returnsRawRows() {
        // When
        List<Map<String, String>> rows = sut.readListMap(DIR, "YamlTestCoreAdapterTest/tables", "lm1");

        // Then: 2 行・値は記法のまま・null 保持
        assertThat(rows.size(), is(2));
        assertThat(rows.get(0).get("val"), is("${v1}"));
        assertThat(rows.get(1).get("val"), is(nullValue()));
    }

    // ------------------------------------------------------------------------
    // ファイル系
    // ------------------------------------------------------------------------

    @Test
    public void readFiles_fixed_returnsRawFixedLengthFile() {
        // When
        List<DataFile> files = sut.readFiles(DIR, "YamlTestCoreAdapterTest/files", "", DataType.SETUP_FIXED);

        // Then: 固定長 1 件・ディレクティブ・フィールド名・値が記法のまま
        assertThat(files.size(), is(1));
        DataFile file = files.get(0);
        assertTrue(file instanceof FixedLengthFile);
        FileView view = TestCoreFileAdapter.read(file);
        assertThat(view.getPath(), is("input.dat"));
        FragmentView fragment = view.getFragments().get(0);
        assertThat(fragment.getNames(), is(list("f1", "f2")));
        assertThat(fragment.getValues().get(0).get("f1"), is("${a}"));
        assertThat(fragment.getValues().get(0).get("f2"), is("123"));
    }

    @Test
    public void readFiles_variable_returnsRawVariableLengthFile() {
        // When
        List<DataFile> files = sut.readFiles(DIR, "YamlTestCoreAdapterTest/files", "", DataType.EXPECTED_VARIABLE);

        // Then: 可変長 1 件・値は記法のまま
        assertThat(files.size(), is(1));
        DataFile file = files.get(0);
        assertTrue(file instanceof VariableLengthFile);
        FragmentView fragment = TestCoreFileAdapter.read(file).getFragments().get(0);
        assertThat(fragment.getValues().get(0).get("c2"), is("${b}"));
    }

    @Test
    public void readFiles_unsupportedType_throws() {
        try {
            sut.readFiles(DIR, "YamlTestCoreAdapterTest/files", "", DataType.MESSAGE);
            fail("should throw");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("readFiles"));
        }
    }

    // ------------------------------------------------------------------------
    // メッセージ系
    // ------------------------------------------------------------------------

    @Test
    public void readMessage_returnsRawBodyAndFwHeader() {
        // When
        MessageContent content = sut.readMessage(DIR, "YamlTestCoreAdapterTest/messages", "RM01");

        // Then: FW ヘッダは文字列化のみ（${...} は未加工）・本文も記法のまま
        assertNotNull(content);
        assertThat(content.getFwHeader().get("requestId"), is("RM01"));
        assertThat(content.getFwHeader().get("userId"), is("${user}"));
        FragmentView fragment = TestCoreFileAdapter.read(content.getBody()).getFragments().get(0);
        assertThat(fragment.getNames(), is(list("m1")));
        assertThat(fragment.getValues().get(0).get("m1"), is("abc"));
    }

    @Test
    public void readMessage_absentId_returnsNull() {
        assertNull(sut.readMessage(DIR, "YamlTestCoreAdapterTest/messages", "ABSENT"));
    }

    // ------------------------------------------------------------------------
    // 送信同期メッセージ（送信系 4 種）
    // ------------------------------------------------------------------------

    @Test
    public void readSendSyncMessages_returnsGroupBodies() {
        // When: case1 グループ（MSG1・MSG2）を取得
        List<FixedLengthFile> bodies = sut.readSendSyncMessages(DIR, "YamlTestCoreAdapterTest/sendSync", "case1",
                DataType.EXPECTED_REQUEST_HEADER_MESSAGES);

        // Then: 2 件・path は id・値は記法のまま
        assertThat(bodies.size(), is(2));
        assertThat(bodies.get(0).getPath(), is("MSG1"));
        assertThat(bodies.get(1).getPath(), is("MSG2"));
        FragmentView fragment = TestCoreFileAdapter.read(bodies.get(0)).getFragments().get(0);
        assertThat(fragment.getValues().get(0).get("s1"), is("${z}"));
    }

    @Test
    public void readSendSyncMessages_noMatch_returnsEmpty() {
        List<FixedLengthFile> bodies = sut.readSendSyncMessages(DIR, "YamlTestCoreAdapterTest/sendSync", "absent",
                DataType.EXPECTED_REQUEST_HEADER_MESSAGES);
        assertThat(bodies.size(), is(0));
    }

    @Test
    public void readSendSyncMessages_unsupportedType_throws() {
        try {
            sut.readSendSyncMessages(DIR, "YamlTestCoreAdapterTest/sendSync", "case1", DataType.MESSAGE);
            fail("should throw");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("readSendSyncMessages"));
        }
    }

    // ------------------------------------------------------------------------
    // loadRawMap ／ isResourceExisting
    // ------------------------------------------------------------------------

    @Test
    public void loadRawMap_returnsOrderPreservingOriginalMap() {
        // When
        Map<String, Object> yaml = sut.loadRawMap(DIR, "YamlTestCoreAdapterTest/tables");

        // Then: トップレベルキーは記述順・原文のカラム名/値（小文字・null 保持）
        assertTrue(yaml.containsKey("setup_tables"));
        @SuppressWarnings("unchecked")
        List<Object> setupTables = (List<Object>) yaml.get("setup_tables");
        @SuppressWarnings("unchecked")
        Map<String, Object> users = (Map<String, Object>) setupTables.get(0);
        assertThat(users.get("table").toString(), is("users"));
        @SuppressWarnings("unchecked")
        List<Object> rows = (List<Object>) users.get("rows");
        @SuppressWarnings("unchecked")
        Map<String, Object> firstRow = (Map<String, Object>) rows.get(0);
        // YAML 記述順のカラム名（原文＝小文字）
        assertThat(new ArrayList<String>(firstRow.keySet()), is(list("id", "name", "note")));
        assertThat(firstRow.get("name").toString(), is("${user.name}"));
        assertThat(firstRow.get("note"), is(nullValue()));
    }

    @Test
    public void isResourceExisting_reflectsFileExistence() {
        assertTrue(sut.isResourceExisting(DIR, "YamlTestCoreAdapterTest/tables"));
        assertThat(sut.isResourceExisting(DIR, "YamlTestCoreAdapterTest/noSuchFile"), is(false));
    }

    // ------------------------------------------------------------------------
    // ヘルパー
    // ------------------------------------------------------------------------

    /** 指定行・カラムの値を文字列で取り出す（null は null のまま）。 */
    private static String value(TableData table, int row, String column) {
        Object v = table.getValue(row, column);
        return v == null ? null : v.toString();
    }

    private static List<String> list(String... values) {
        List<String> result = new ArrayList<String>(values.length);
        for (String v : values) {
            result.add(v);
        }
        return result;
    }
}
