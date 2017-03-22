package nablarch.test.core.reader;

import static nablarch.test.TestUtil.assertMatches;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nablarch.core.util.BinaryUtil;
import nablarch.core.util.FileUtil;
import nablarch.test.core.db.TableData;
import nablarch.test.core.db.TestTable;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link nablarch.test.core.reader.BasicTestDataParser}のテストクラス。<br>
 *
 * @author Hisaaki Sioiri
 */
@RunWith(DatabaseTestRunner.class)
public class BasicTestDataParserTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    private static final String resourceRoot = "src/test/java/";

    private TestDataParser target;

    @BeforeClass
    public static void beforeClass() {
        VariousDbTestHelper.createTable(TestTable.class);
    }

    @Before
    public void before() {
        target = repositoryResource.getComponent("testDataParser");
        VariousDbTestHelper.delete(TestTable.class);
    }

    /**
     * {@link BasicTestDataParser#getTableData(String, String, DataType, String)} のテスト。<br/>
     * 期待するテーブル(EXPECTED_TABLE指定)が取得されることを確認する。
     *
     * @throws IOException ファイル読み込み時のエラー
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testExpectedGetTableData() throws IOException {

        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/withoutGroupId";
        List<TableData> actualList = target.getExpectedTableData(dir, resource);
        assertNotNull(actualList);
        assertThat(actualList.size(), is(2));

        TableData actual1 = actualList.get(0);
        assertThat(actual1.getTableName(), is("TEST_TABLE"));

        assertThat(actual1.getPrimaryKeys(), arrayContaining(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2")));
        assertThat(actual1.getColumnNames(), arrayContainingInAnyOrder(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2"),
                equalToIgnoringCase("VARCHAR2_COL"),
                equalToIgnoringCase("NUMBER_COL"),
                equalToIgnoringCase("NUMBER_COL2"),
                equalToIgnoringCase("CLOB_COL"),
                equalToIgnoringCase("BLOB_COL")));

        // 1行目
        assertThat(actual1.getValue(0, "PK_COL1")
                .toString(), is("0000000001"));
        assertThat(actual1.getValue(0, "PK_COL2")
                .toString(), is("AB"));
        assertThat(actual1.getValue(0, "VARCHAR2_COL")
                .toString(), is("あいうえお"));
        assertThat(actual1.getValue(0, "NUMBER_COL")
                .toString(), is("1"));
        assertThat(actual1.getValue(0, "NUMBER_COL2")
                .toString(), is("1.1"));
        assertThat(actual1.getValue(0, "CLOB_COL")
                .toString(), is("CLOBです1"));
        assertThat(actual1.getValue(0, "BLOB_COL")
                .toString(),
                is(BinaryUtil.convertToHexString("あいうえお\r\nかきくけこ\r\nさしすせそ".getBytes())));

        // 2行目
        assertThat(actual1.getValue(1, "PK_COL1")
                .toString(), is("0000000002"));
        assertThat(actual1.getValue(1, "PK_COL2")
                .toString(), is("CD"));
        assertThat(actual1.getValue(1, "VARCHAR2_COL")
                .toString(), is("かきくけこ"));
        assertThat(actual1.getValue(1, "NUMBER_COL")
                .toString(), is("10"));
        assertThat(actual1.getValue(1, "NUMBER_COL2")
                .toString(), is("1.11"));
        assertThat(actual1.getValue(1, "CLOB_COL")
                .toString(), is("CLOBです2"));

        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            fis = new FileInputStream(dir + "BasicTestDataParserTest.xls");
            bis = new BufferedInputStream(fis);
            int ch;
            while ((ch = bis.read()) != -1) {
                baos.write(ch);
            }
        } finally {
            FileUtil.closeQuietly(fis);
            FileUtil.closeQuietly(bis);
        }

        String expectedHex = BinaryUtil.convertToHexString(baos.toByteArray());
        assertThat(actual1.getValue(1, "BLOB_COL")
                .toString(), is(expectedHex));

        TableData actual2 = actualList.get(1);
        assertThat(actual2.getTableName(), is("TEST_TABLE"));
        assertThat(actual2.getPrimaryKeys(), arrayContaining(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2")));
        assertThat(actual2.getColumnNames(), arrayContainingInAnyOrder(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2"),
                equalToIgnoringCase("VARCHAR2_COL"),
                equalToIgnoringCase("NUMBER_COL"),
                equalToIgnoringCase("NUMBER_COL2")));

        // 1行目
        assertThat(actual2.getValue(0, "PK_COL1")
                .toString(), is("0000000003"));
        assertThat(actual2.getValue(0, "PK_COL2")
                .toString(), is("EF"));
        assertThat(actual2.getValue(0, "VARCHAR2_COL")
                .toString(), is("さしすせそ"));
        assertThat(actual2.getValue(0, "NUMBER_COL")
                .toString(), is("100"));
        assertThat(actual2.getValue(0, "NUMBER_COL2")
                .toString(), is("1.111"));
        // 2行目
        assertThat(actual2.getValue(1, "PK_COL1")
                .toString(), is("0000000004"));
        assertThat(actual2.getValue(1, "PK_COL2")
                .toString(), is("GH"));
        assertThat(actual2.getValue(1, "VARCHAR2_COL")
                .toString(), is("たちつてと"));
        assertThat(actual2.getValue(1, "NUMBER_COL")
                .toString(), is("1000"));
        assertThat(actual2.getValue(1, "NUMBER_COL2")
                .toString(), is("1.1111"));
    }

    /**
     * {@link BasicTestDataParser#getTableData(String, String, DataType, String)} のテスト。
     * グループIDを指定した場合、グループIDが合致するテーブルだけが取得できることを確認する。
     *
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetExpectedTableDataWithGroupId() throws IOException {

        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/withGroupId";
        List<TableData> case01 = target.getExpectedTableData(dir, resource, "case01");  // グループID指定
        assertNotNull(case01);
        assertThat(case01.size(), is(1));

        TableData actual1 = case01.get(0);
        assertThat(actual1.getTableName(), is("TEST_TABLE"));
        assertThat(actual1.getPrimaryKeys(), arrayContaining(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2")));

        assertThat(actual1.getColumnNames(), arrayContainingInAnyOrder(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2"),
                equalToIgnoringCase("VARCHAR2_COL"),
                equalToIgnoringCase("NUMBER_COL"),
                equalToIgnoringCase("NUMBER_COL2"),
                equalToIgnoringCase("CLOB_COL"),
                equalToIgnoringCase("BLOB_COL")));
        // 1行目
        assertThat(actual1.getValue(0, "PK_COL1")
                .toString(), is("0000000001"));
        assertThat(actual1.getValue(0, "PK_COL2")
                .toString(), is("AB"));
        assertThat(actual1.getValue(0, "VARCHAR2_COL")
                .toString(), is("あいうえお"));
        assertThat(actual1.getValue(0, "NUMBER_COL")
                .toString(), is("1"));
        assertThat(actual1.getValue(0, "NUMBER_COL2")
                .toString(), is("1.1"));
        assertThat(actual1.getValue(0, "CLOB_COL")
                .toString(), is("CLOBです1"));
        assertThat(actual1.getValue(0, "BLOB_COL")
                .toString(),
                is(BinaryUtil.convertToHexString("あいうえお\r\nかきくけこ\r\nさしすせそ".getBytes())));
        // 2行目
        assertThat(actual1.getValue(1, "PK_COL1")
                .toString(), is("0000000002"));
        assertThat(actual1.getValue(1, "PK_COL2")
                .toString(), is("CD"));
        assertThat(actual1.getValue(1, "VARCHAR2_COL")
                .toString(), is("かきくけこ"));
        assertThat(actual1.getValue(1, "NUMBER_COL")
                .toString(), is("10"));
        assertThat(actual1.getValue(1, "NUMBER_COL2")
                .toString(), is("1.11"));
        assertThat(actual1.getValue(1, "CLOB_COL")
                .toString(), is("CLOBです2"));

        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            fis = new FileInputStream(dir + "BasicTestDataParserTest.xls");
            bis = new BufferedInputStream(fis);
            int ch;
            while ((ch = bis.read()) != -1) {
                baos.write(ch);
            }
        } finally {
            FileUtil.closeQuietly(fis);
            FileUtil.closeQuietly(bis);
        }

        String expectedHex = BinaryUtil.convertToHexString(baos.toByteArray());
        assertThat(actual1.getValue(1, "BLOB_COL")
                .toString(), is(expectedHex));

        List<TableData> case02 = target.getExpectedTableData(dir, resource, "case02");  // グループID指定
        assertThat(case02.size(), is(2));
        // １つめ
        TableData actual2 = case02.get(0);
        assertThat(actual2.getTableName(), is("TEST_TABLE"));
        assertThat(actual2.getPrimaryKeys(), arrayContaining(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2")));
        assertThat(actual2.getColumnNames(), arrayContainingInAnyOrder(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2"),
                equalToIgnoringCase("VARCHAR2_COL"),
                equalToIgnoringCase("NUMBER_COL"),
                equalToIgnoringCase("NUMBER_COL2")));

        assertThat(actual2.getValue(0, "PK_COL1")
                .toString(), is("0000000003"));
        assertThat(actual2.getValue(0, "PK_COL2")
                .toString(), is("EF"));
        assertThat(actual2.getValue(0, "VARCHAR2_COL")
                .toString(), is("さしすせそ"));
        assertThat(actual2.getValue(0, "NUMBER_COL")
                .toString(), is("100"));
        assertThat(actual2.getValue(0, "NUMBER_COL2")
                .toString(), is("1.111"));

        // ２つめ
        actual2 = case02.get(1);
        assertThat(actual2.getTableName(), is("TEST_TABLE"));
        assertThat(actual2.getPrimaryKeys(), arrayContaining(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2")));
        assertThat(actual2.getColumnNames(), arrayContainingInAnyOrder(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2"),
                equalToIgnoringCase("VARCHAR2_COL"),
                equalToIgnoringCase("NUMBER_COL"),
                equalToIgnoringCase("NUMBER_COL2")));
        assertThat(actual2.getValue(0, "PK_COL1")
                .toString(), is("0000000004"));
        assertThat(actual2.getValue(0, "PK_COL2")
                .toString(), is("GH"));
        assertThat(actual2.getValue(0, "VARCHAR2_COL")
                .toString(), is("たちつてと"));
        assertThat(actual2.getValue(0, "NUMBER_COL")
                .toString(), is("1000"));
        assertThat(actual2.getValue(0, "NUMBER_COL2")
                .toString(), is("1.1111"));
    }

    /** グループIDが存在しなかった場合、空のリストが返却されることを確認する。 */
    @Test
    public void testGetTableDataNotExist() {
        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/withGroupId";
        List<TableData> actualList = target.getExpectedTableData(dir, resource, "notexits"); //存在しないグループID
        assertNotNull(actualList);
        assertThat(actualList.size(), is(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetSetupTableData() throws IOException {
        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/withoutGroupId";
        List<TableData> actualList = target.getSetupTableData(dir, resource);
        assertNotNull(actualList);
        assertThat(actualList.size(), is(2));

        TableData actual1 = actualList.get(0);
        assertThat(actual1.getTableName(), is("TEST_TABLE"));
        assertThat(actual1.getPrimaryKeys(), arrayContaining(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2")));
        assertThat(actual1.getColumnNames(), arrayContainingInAnyOrder(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2"),
                equalToIgnoringCase("VARCHAR2_COL"),
                equalToIgnoringCase("NUMBER_COL"),
                equalToIgnoringCase("NUMBER_COL2"),
                equalToIgnoringCase("CLOB_COL"),
                equalToIgnoringCase("BLOB_COL")));
        // 1行目
        assertThat(actual1.getValue(0, "PK_COL1")
                .toString(), is("0000000005"));
        assertThat(actual1.getValue(0, "PK_COL2")
                .toString(), is("IJ"));
        assertThat(actual1.getValue(0, "VARCHAR2_COL")
                .toString(), is("なにぬねの"));
        assertThat(actual1.getValue(0, "NUMBER_COL")
                .toString(), is("10000"));
        assertThat(actual1.getValue(0, "NUMBER_COL2")
                .toString(), is("2.2"));
        assertThat(actual1.getValue(0, "CLOB_COL")
                .toString(), is("CLOBです1"));
        assertThat(actual1.getValue(0, "BLOB_COL")
                .toString(),
                is(BinaryUtil.convertToHexString("あいうえお\r\nかきくけこ\r\nさしすせそ".getBytes())));
        // 2行目
        assertThat(actual1.getValue(1, "PK_COL1")
                .toString(), is("0000000006"));
        assertThat(actual1.getValue(1, "PK_COL2")
                .toString(), is("KL"));
        assertThat(actual1.getValue(1, "VARCHAR2_COL"), nullValue());
        assertThat(actual1.getValue(1, "NUMBER_COL")
                .toString(), is("100000"));
        assertThat(actual1.getValue(1, "NUMBER_COL2")
                .toString(), is("2.22"));
        assertThat(actual1.getValue(1, "CLOB_COL")
                .toString(), is("CLOBです2"));

        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            fis = new FileInputStream(dir + "BasicTestDataParserTest.xls");
            bis = new BufferedInputStream(fis);
            int ch;
            while ((ch = bis.read()) != -1) {
                baos.write(ch);
            }
        } finally {
            FileUtil.closeQuietly(fis);
            FileUtil.closeQuietly(bis);
        }

        String expectedHex = BinaryUtil.convertToHexString(baos.toByteArray());
        assertThat(actual1.getValue(1, "BLOB_COL")
                .toString(), is(expectedHex));


        TableData actual2 = actualList.get(1);
        assertThat(actual2.getTableName(), is("TEST_TABLE"));
        assertThat(actual2.getPrimaryKeys(), arrayContaining(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2")));
        assertThat(actual2.getColumnNames(), arrayContainingInAnyOrder(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2"),
                equalToIgnoringCase("VARCHAR2_COL"),
                equalToIgnoringCase("NUMBER_COL"),
                equalToIgnoringCase("NUMBER_COL2")));

        // 1行目
        assertThat(actual2.getValue(0, "PK_COL1")
                .toString(), is("0000000007"));
        assertThat(actual2.getValue(0, "PK_COL2")
                .toString(), is("MN"));
        assertThat(actual2.getValue(0, "VARCHAR2_COL")
                .toString(), is("まみむめも"));
        assertThat(actual2.getValue(0, "NUMBER_COL")
                .toString(), is("1000000"));
        assertThat(actual2.getValue(0, "NUMBER_COL2")
                .toString(), is("2.222"));
        // 2行目
        assertThat(actual2.getValue(1, "PK_COL1")
                .toString(), is("0000000008"));
        assertThat(actual2.getValue(1, "PK_COL2")
                .toString(), is("OP"));
        assertThat(actual2.getValue(1, "VARCHAR2_COL")
                .toString(), is("やゆよ"));
        assertThat(actual2.getValue(1, "NUMBER_COL")
                .toString(), is("10000000"));
        assertThat(actual2.getValue(1, "NUMBER_COL2")
                .toString(), is("2.2222"));
    }

    /**
     * {@link BasicTestDataParser#getTableData(String, String, DataType, String)} のテスト。
     * グループIDを指定した場合、グループIDが合致するテーブルだけが取得できることを確認する。
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetSetupTableDataWithGroupId() {

        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/withGroupId";
        List<TableData> case01 = target.getSetupTableData(dir, resource, "case01");  // グループID指定
        assertNotNull(case01);
        assertThat(case01.size(), is(1));

        TableData actual1 = case01.get(0);
        assertThat(actual1.getTableName(), is("TEST_TABLE"));
        assertThat(actual1.getPrimaryKeys(), arrayContaining(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2")));
        assertThat(actual1.getColumnNames(), arrayContainingInAnyOrder(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2"),
                equalToIgnoringCase("VARCHAR2_COL"),
                equalToIgnoringCase("NUMBER_COL"),
                equalToIgnoringCase("NUMBER_COL2")));
        // 1行目
        assertThat(actual1.getValue(0, "PK_COL1")
                .toString(), is("0000000005"));
        assertThat(actual1.getValue(0, "PK_COL2")
                .toString(), is("IJ"));
        assertThat(actual1.getValue(0, "VARCHAR2_COL")
                .toString(), is("なにぬねの"));
        assertThat(actual1.getValue(0, "NUMBER_COL")
                .toString(), is("10000"));
        assertThat(actual1.getValue(0, "NUMBER_COL2")
                .toString(), is("2.2"));

        // 2行目
        assertThat(actual1.getValue(1, "PK_COL1")
                .toString(), is("0000000006"));
        assertThat(actual1.getValue(1, "PK_COL2")
                .toString(), is("KL"));
        assertThat(actual1.getValue(1, "VARCHAR2_COL")
                .toString(), is("はひふへほ"));
        assertThat(actual1.getValue(1, "NUMBER_COL")
                .toString(), is("100000"));
        assertThat(actual1.getValue(1, "NUMBER_COL2")
                .toString(), is("2.22"));

        List<TableData> case02 = target.getExpectedTableData(dir, resource, "case02");  // グループID指定
        assertThat(case02.size(), is(2));
        TableData actual2 = case02.get(0);
        assertThat(actual1.getTableName(), is("TEST_TABLE"));
        assertThat(actual1.getPrimaryKeys(), arrayContaining(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2")));
        assertThat(actual1.getColumnNames(), arrayContainingInAnyOrder(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2"),
                equalToIgnoringCase("VARCHAR2_COL"),
                equalToIgnoringCase("NUMBER_COL"),
                equalToIgnoringCase("NUMBER_COL2")));

        // １つめのテーブル
        assertThat(actual2.getValue(0, "PK_COL1")
                .toString(), is("0000000003"));
        assertThat(actual2.getValue(0, "PK_COL2")
                .toString(), is("EF"));
        assertThat(actual2.getValue(0, "VARCHAR2_COL")
                .toString(), is("さしすせそ"));
        assertThat(actual2.getValue(0, "NUMBER_COL")
                .toString(), is("100"));
        assertThat(actual2.getValue(0, "NUMBER_COL2")
                .toString(), is("1.111"));

        // 2つめのテーブル
        actual2 = case02.get(1);
        assertThat(actual2.getTableName(), is("TEST_TABLE"));
        assertThat(actual2.getPrimaryKeys(), arrayContaining(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2")));
        assertThat(actual2.getColumnNames(), arrayContainingInAnyOrder(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2"),
                equalToIgnoringCase("VARCHAR2_COL"),
                equalToIgnoringCase("NUMBER_COL"),
                equalToIgnoringCase("NUMBER_COL2")));
        assertThat(actual2.getValue(0, "PK_COL1")
                .toString(), is("0000000004"));
        assertThat(actual2.getValue(0, "PK_COL2")
                .toString(), is("GH"));
        assertThat(actual2.getValue(0, "VARCHAR2_COL")
                .toString(), is("たちつてと"));
        assertThat(actual2.getValue(0, "NUMBER_COL")
                .toString(), is("1000"));
        assertThat(actual2.getValue(0, "NUMBER_COL2")
                .toString(), is("1.1111"));
    }

    @Test
    public void testGetListMap() {
        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/getListMap";
        List<Map<String, String>> actual = target.getListMap(dir, resource, "params");
        assertNotNull(actual);
        assertThat(actual.size(), is(2));
    }

    /** 末尾に見えない要素が存在するデータでも正しく読み込みできることを確認する。 */
    @Test
    public void testGetListMapWithInvisibleTail() {
        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/invisibleTail";

        List<Map<String, String>> actual = target.getListMap(dir, resource, "expectedUgroup");
        assertNotNull(actual);
        assertThat(actual.size(), is(3));

        {
            Map<String, String> row = actual.get(0);
            assertThat(row.size(), is(2));
            assertThat(row.get("UGROUP_ID"), is("0000000001"));
            assertThat(row.get("UGROUP_NAME"), is("総務部"));
        }
        {
            Map<String, String> row = actual.get(1);
            assertThat(row.size(), is(2));
            assertThat(row.get("UGROUP_ID"), is("0000000002"));
            assertThat(row.get("UGROUP_NAME"), is("人事部"));
        }
        {
            Map<String, String> row = actual.get(2);
            assertThat(row.size(), is(2));
            assertThat(row.get("UGROUP_ID"), is("0000000003"));
            assertThat(row.get("UGROUP_NAME"), is("テスト用"));
        }
    }

    /** 末尾に見えない要素が存在するデータでも正しく読み込みできることを確認する。 */
    @Test
    public void testGetTableDataWithInvisibleTail() {
        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/invisibleTail";

        List<TableData> actual = target.getExpectedTableData(dir, resource);
        assertThat(actual.size(), is(1));
        TableData tableData = actual.get(0);
        assertThat(tableData.size(), is(3));
        assertEquals("1234567890", tableData.getValue(0, "pk_col1"));
        assertEquals("1234567891", tableData.getValue(1, "pk_col1"));
        assertEquals("1234567892", tableData.getValue(2, "pk_col1"));
        assertEquals("01", tableData.getValue(0, "pk_col2"));
        assertEquals("02", tableData.getValue(1, "pk_col2"));
        assertEquals("03", tableData.getValue(2, "pk_col2"));
    }

    /**
     * テストデータの値が変換されることを確認する。
     *
     * @throws IOException
     */
    @Test
    public void testConvertedValues() throws IOException {
        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/convertedValues";

        List<Map<String, String>> actual = target.getListMap(dir, resource, "expectedUgroup");
        assertNotNull(actual);
        assertThat(actual.size(), is(4));

        {
            Map<String, String> row = actual.get(0);
            assertThat(row.size(), is(2));
            assertMatches("[0-9]{10}", row.get("UGROUP_ID"));
            assertMatches("(\\p{InHiragana}|ー){50}", row.get("UGROUP_NAME"));
        }
        {
            Map<String, String> row = actual.get(1);
            assertThat(row.size(), is(2));
            assertMatches("[A-z]{10}", row.get("UGROUP_ID"));
            assertEquals(" abc ", row.get("UGROUP_NAME"));
        }
        {
            Map<String, String> row = actual.get(2);
            assertThat(row.size(), is(2));
            assertMatches("[0-9]{10}", row.get("UGROUP_ID"));
            assertNull(row.get("UGROUP_NAME"));
        }
        {
            Map<String, String> row = actual.get(3);
            assertThat(row.size(), is(2));
            assertThat(row.get("UGROUP_ID"), is(BinaryUtil.convertToHexString("あいうえお\r\nかきくけこ\r\nさしすせそ".getBytes())));

            FileInputStream fis = null;
            BufferedInputStream bis = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try {
                fis = new FileInputStream(dir + "BasicTestDataParserTest.xls");
                bis = new BufferedInputStream(fis);
                int ch;
                while ((ch = bis.read()) != -1) {
                    baos.write(ch);
                }
            } finally {
                FileUtil.closeQuietly(fis);
                FileUtil.closeQuietly(bis);
            }

            String expectedHex = BinaryUtil.convertToHexString(baos.toByteArray());
            assertThat(row.get("UGROUP_NAME")
                    .toString(), is(expectedHex));


        }
    }

    /** EXPECTED_COMPLETE_TABLE（IDなし）を取得するケース。 */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetExpectedTableDataCompletedWithoutId() {
        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/completedWithoutId";

        List<TableData> actualList = target.getExpectedTableData(dir, resource);
        assertNotNull(actualList);
        assertThat(actualList.size(), is(2));

        TableData actual1 = actualList.get(0);
        assertThat(actual1.getTableName(), is("TEST_TABLE"));
        assertThat(actual1.getPrimaryKeys(), arrayContaining(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2")));
        assertThat(actual1.getColumnNames(), arrayContainingInAnyOrder(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2"),
                equalToIgnoringCase("VARCHAR2_COL"),
                equalToIgnoringCase("NUMBER_COL"),
                equalToIgnoringCase("NUMBER_COL2"),
                equalToIgnoringCase("DATE_COL"),
                equalToIgnoringCase("TIMESTAMP_COL"),
                equalToIgnoringCase("NULL_COL"),
                equalToIgnoringCase("CLOB_COL"),
                equalToIgnoringCase("BLOB_COL"),
                equalToIgnoringCase("BOOL_COL")));
        // 1行目
        assertThat(actual1.getValue(0, "PK_COL1")
                .toString(), is("0000000001"));
        assertThat(actual1.getValue(0, "PK_COL2")
                .toString(), is("AB"));
        // デフォルト値が補われること
        assertThat(actual1.getValue(0, "VARCHAR2_COL")
                .toString(), is(" "));
        assertThat(actual1.getValue(0, "NUMBER_COL")
                .toString(), is("0"));
        assertThat(actual1.getValue(0, "NUMBER_COL2")
                .toString(), is("0"));
        assertThat(actual1.getValue(0, "CLOB_COL")
                .toString(), is(" "));
        assertThat(actual1.getValue(0, "BLOB_COL")
                .toString(), is(BinaryUtil.convertToHexString(new byte[10])));
        assertThat(actual1.getValue(0, "BOOL_COL").toString(), anyOf(is("0"), is("false")));

        TableData actual2 = actualList.get(1);
        assertThat(actual2.getTableName(), is("TEST_TABLE"));
        assertThat(actual2.getPrimaryKeys(), arrayContaining(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2")));
        assertThat(actual2.getColumnNames(), arrayContainingInAnyOrder(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2"),
                equalToIgnoringCase("VARCHAR2_COL"),
                equalToIgnoringCase("NUMBER_COL"),
                equalToIgnoringCase("NUMBER_COL2"),
                equalToIgnoringCase("DATE_COL"),
                equalToIgnoringCase("TIMESTAMP_COL"),
                equalToIgnoringCase("NULL_COL"),
                equalToIgnoringCase("CLOB_COL"),
                equalToIgnoringCase("BLOB_COL"),
                equalToIgnoringCase("BOOL_COL")));

        // 1行目
        assertThat(actual2.getValue(0, "PK_COL1")
                .toString(), is("0000000002"));
        assertThat(actual2.getValue(0, "PK_COL2")
                .toString(), is("CD"));
        // デフォルト値が補われること
        assertThat(actual2.getValue(0, "VARCHAR2_COL")
                .toString(), is(" "));
        assertThat(actual2.getValue(0, "NUMBER_COL")
                .toString(), is("0"));
        assertThat(actual2.getValue(0, "NUMBER_COL2")
                .toString(), is("0"));
        assertThat(actual2.getValue(0, "CLOB_COL")
                .toString(), is(" "));
        assertThat(actual2.getValue(0, "BLOB_COL")
                .toString(), is(BinaryUtil.convertToHexString(new byte[10])));
        assertThat(actual2.getValue(0, "BOOL_COL").toString(), anyOf(is("0"), is("false")));

    }

    /** EXPECTED_COMPLETE_TABLE（IDあり）を取得するケース。 */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetExpectedTableDataCompletedWithId() {
        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/completedWithId";
        List<TableData> actualList = target.getExpectedTableData(dir, resource, "with_ID");
        assertNotNull(actualList);
        assertThat(actualList.size(), is(2));

        TableData actual1 = actualList.get(0);
        assertThat(actual1.getTableName(), is("TEST_TABLE"));
        assertThat(actual1.getPrimaryKeys(), arrayContaining(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2")));
        assertThat(actual1.getColumnNames(), arrayContainingInAnyOrder(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2"),
                equalToIgnoringCase("VARCHAR2_COL"),
                equalToIgnoringCase("NUMBER_COL"),
                equalToIgnoringCase("NUMBER_COL2"),
                equalToIgnoringCase("DATE_COL"),
                equalToIgnoringCase("TIMESTAMP_COL"),
                equalToIgnoringCase("NULL_COL"),
                equalToIgnoringCase("CLOB_COL"),
                equalToIgnoringCase("BLOB_COL"),
                equalToIgnoringCase("BOOL_COL")));
        // 1行目
        assertThat(actual1.getValue(0, "PK_COL1")
                .toString(), is("0000000003"));
        assertThat(actual1.getValue(0, "PK_COL2")
                .toString(), is("EF"));
        // デフォルト値が補われること
        assertThat(actual1.getValue(0, "VARCHAR2_COL")
                .toString(), is(" "));
        assertThat(actual1.getValue(0, "NUMBER_COL")
                .toString(), is("0"));
        assertThat(actual1.getValue(0, "NUMBER_COL2")
                .toString(), is("0"));


        TableData actual2 = actualList.get(1);
        assertThat(actual2.getTableName(), is("TEST_TABLE"));
        assertThat(actual2.getPrimaryKeys(), arrayContaining(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2")));
        assertThat(actual2.getColumnNames(), arrayContainingInAnyOrder(
                equalToIgnoringCase("PK_COL1"),
                equalToIgnoringCase("PK_COL2"),
                equalToIgnoringCase("VARCHAR2_COL"),
                equalToIgnoringCase("NUMBER_COL"),
                equalToIgnoringCase("NUMBER_COL2"),
                equalToIgnoringCase("DATE_COL"),
                equalToIgnoringCase("TIMESTAMP_COL"),
                equalToIgnoringCase("NULL_COL"),
                equalToIgnoringCase("CLOB_COL"),
                equalToIgnoringCase("BLOB_COL"),
                equalToIgnoringCase("BOOL_COL")));

        // 1行目
        assertThat(actual2.getValue(0, "PK_COL1")
                .toString(), is("0000000004"));
        assertThat(actual2.getValue(0, "PK_COL2")
                .toString(), is("GH"));
        // デフォルト値が補われること
        assertThat(actual1.getValue(0, "VARCHAR2_COL")
                .toString(), is(" "));
        assertThat(actual1.getValue(0, "NUMBER_COL")
                .toString(), is("0"));
        assertThat(actual1.getValue(0, "NUMBER_COL2")
                .toString(), is("0"));
    }

    /**
     * getListMapにて取得対象となるデータにマーカーカラムが記載されている場合、
     * そのカラムは実際のデータとしては読み込まれないこと。
     */
    @Test
    public void testGetListMapIgnoredColumn() {
        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/markerColumn";
        List<Map<String, String>> actual = target.getListMap(dir, resource, "params");

        assertNotNull(actual);
        assertThat(actual.size(), is(2));
        for (Map<String, String> e : actual) {
            // []で囲われたカラムは含まれていないこと。
            assertEquals(new HashSet<String>(Arrays.asList("id", "name", "address")), e.keySet());
        }
        // 1行目
        {
            Map<String, String> m = actual.get(0);
            assertThat(m.get("id"), is("0000000001"));
            assertThat(m.get("name"), is("山田太郎"));
            assertThat(m.get("address"), is("1"));
        }
        // 2行目
        {
            Map<String, String> m = actual.get(1);
            assertThat(m.get("id"), is("0000000002"));
            assertThat(m.get("name"), is("鈴木一郎"));
            assertThat(m.get("address"), is("10"));
        }
    }


    /**
     * getSetupTablにて取得対象となるデータにマーカーカラムが記載されている場合、
     * そのカラムは実際のデータとしては読み込まれないこと。
     */
    @Test
    public void testGetExpectedTableIgnoredColumn() {
        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/markerColumn";
        List<TableData> tables = target.getExpectedTableData(dir, resource);

        Set<String> expectedColumns = new HashSet<String>(
                Arrays.asList("PK_COL1", "PK_COL2", "NUMBER_COL", "VARCHAR2_COL", "NUMBER_COL2"));
        for (TableData e : tables) {
            Set<String> actualColumns = new HashSet<String>(Arrays.asList(e.getColumnNames()));
            // []で囲われたカラムは含まれていないこと。
            assertEquals(expectedColumns, actualColumns);
        }
        TableData t = tables.get(0);
        // 1行目
        assertEquals("0000000003", t.getValue(0, "PK_COL1"));
        assertEquals("EF", t.getValue(0, "PK_COL2"));
        assertEquals("100", t.getValue(0, "NUMBER_COL"));
        assertEquals("さしすせそ", t.getValue(0, "VARCHAR2_COL"));
        assertEquals("1.111", t.getValue(0, "NUMBER_COL2"));
        // 2行目
        assertEquals("0000000004", t.getValue(1, "PK_COL1"));
        assertEquals("GH", t.getValue(1, "PK_COL2"));
        assertEquals("1000", t.getValue(1, "NUMBER_COL"));
        assertEquals("たちつてと", t.getValue(1, "VARCHAR2_COL"));
        assertEquals("1.1111", t.getValue(1, "NUMBER_COL2"));

    }

    /**
     * getExpectedTableにて取得対象となるデータにマーカーカラムが記載されている場合、
     * そのカラムは実際のデータとしては読み込まれないこと。
     */
    @Test
    public void testGetSetupTableIgnoredColumn() {
        String dir = resourceRoot + "nablarch/test/core/reader/";
        String resource = "BasicTestDataParserTest/markerColumn";
        List<TableData> tables = target.getSetupTableData(dir, resource);

        Set<String> expectedColumns = new HashSet<String>(
                Arrays.asList("PK_COL1", "PK_COL2", "NUMBER_COL", "VARCHAR2_COL", "NUMBER_COL2"));
        for (TableData e : tables) {
            Set<String> actualColumns = new HashSet<String>(Arrays.asList(e.getColumnNames()));
            // []で囲われたカラムは含まれていないこと。
            assertEquals(expectedColumns, actualColumns);
        }
    }

    /**
     * グループIDのフォーマットが正しく行われること。
     * <ul>
     * <li>グループID指定なしの場合、空文字が返却されること</li>
     * <li>グループID指定ありの場合、グループIDが角カッコで囲まれて返却されること</li>
     * </ul>
     */
    @Test
    public void testFormatGroupId() {
        BasicTestDataParser target = new BasicTestDataParser();
        assertThat(target.formatGroupId(null), is(""));
        assertThat(target.formatGroupId(new String[0]), is(""));
        assertThat(target.formatGroupId(new String[] {"group"}), is("[group]"));
    }

    /** グループIDの可変長引数が2個以上の場合、例外が発生すること */
    @Test(expected = IllegalArgumentException.class)
    public void testFormatGroupIdFail() {
        BasicTestDataParser target = new BasicTestDataParser();
        target.formatGroupId(new String[] {"one", "two"});
    }
}
