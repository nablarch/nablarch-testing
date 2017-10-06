package nablarch.test;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import nablarch.core.ThreadContext;
import nablarch.core.repository.SystemRepository;
import nablarch.test.core.db.TableData;
import nablarch.test.core.reader.BasicTestDataParser;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DatabaseTestRunner.class)
public class TestSupportTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test.xml");

    private TestSupport target;

    @BeforeClass
    public static void beforeClass() {
        VariousDbTestHelper.createTable(TestSupportTestTable.class);
    }

    @Before
    public void before() {
        target = new TestSupport(getClass());
        VariousDbTestHelper.delete(TestSupportTestTable.class);
    }

    @Test
    public void testSetThreadContextValues() {

        ThreadContext.setLanguage(null);
        ThreadContext.setRequestId(null);
        ThreadContext.setUserId(null);
        try {
            Map<String, String> m = new HashMap<String, String>();
            m.put(ThreadContext.LANG_KEY, "ja_JP");
            m.put(ThreadContext.REQUEST_ID_KEY, "RQ0000001");
            m.put(ThreadContext.USER_ID_KEY, "U00001");
            TestSupport.setThreadContextValues(m);
            assertEquals("ja_JP", ThreadContext.getLanguage()
                    .toString());
            assertEquals("RQ0000001", ThreadContext.getRequestId());
            assertEquals("U00001", ThreadContext.getUserId());
        } finally {
            ThreadContext.setLanguage(null);
            ThreadContext.setRequestId(null);
            ThreadContext.setUserId(null);
        }
    }

    /**
     * {@link TestSupport#getExpectedTableData(String, String...)}のテスト。<br/>
     * 期待するテーブル(EXPECTED_TABLE指定)が取得されることを確認する。
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testExpectedGetTableData() {

        List<TableData> actualList = target.getExpectedTableData("withoutGroupId");
        assertNotNull(actualList);
        assertThat(actualList.size(), is(2));


        TableData actual1 = actualList.get(0);
        assertThat(actual1.getTableName(), is("TEST_SUPPORT_TEST_TABLE"));

        assertThat(actual1.getPrimaryKeys().length, is(2));
        assertThat(actual1.getPrimaryKeys()[0], equalToIgnoringCase("PK_COL1"));
        assertThat(actual1.getPrimaryKeys()[1], equalToIgnoringCase("PK_COL2"));
        assertThat(actual1.getColumnNames().length, is(5));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL1")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL2")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("VARCHAR2_COL")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL2")));

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

        TableData actual2 = actualList.get(1);
        assertThat(actual1.getTableName(), is("TEST_SUPPORT_TEST_TABLE"));
        assertThat(actual2.getPrimaryKeys().length, is(2));
        assertThat(actual2.getPrimaryKeys()[0], equalToIgnoringCase("PK_COL1"));
        assertThat(actual2.getPrimaryKeys()[1], equalToIgnoringCase("PK_COL2"));

        assertThat(actual2.getColumnNames().length, is(5));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL1")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL2")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("VARCHAR2_COL")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL2")));

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

    /** {@link TestSupport#getExpectedTableData(String, String...)} のテスト。 グループIDを指定した場合、グループIDが合致するテーブルだけが取得できることを確認する。 */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetExpectedTableDataWithGroupId() {

        List<TableData> case01 = target.getExpectedTableData("withGroupId", "case01"); // グループID指定
        assertNotNull(case01);
        assertThat(case01.size(), is(1));

        TableData actual1 = case01.get(0);
        assertThat(actual1.getTableName(), is("TEST_SUPPORT_TEST_TABLE"));
        assertThat(actual1.getPrimaryKeys().length, is(2));
        assertThat(actual1.getPrimaryKeys()[0], equalToIgnoringCase("PK_COL1"));
        assertThat(actual1.getPrimaryKeys()[1], equalToIgnoringCase("PK_COL2"));

        assertThat(actual1.getColumnNames().length, is(5));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL1")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL2")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("VARCHAR2_COL")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL2")));

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

        List<TableData> case02 = target.getExpectedTableData("withGroupId", "case02"); // グループID指定
        assertThat(case02.size(), is(2));
        // １つめ
        TableData actual2 = case02.get(0);
        assertThat(actual2.getTableName(), is("TEST_SUPPORT_TEST_TABLE"));

        assertThat(actual2.getPrimaryKeys().length, is(2));
        assertThat(actual2.getPrimaryKeys()[0], equalToIgnoringCase("PK_COL1"));
        assertThat(actual2.getPrimaryKeys()[1], equalToIgnoringCase("PK_COL2"));

        assertThat(actual2.getColumnNames().length, is(5));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL1")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL2")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("VARCHAR2_COL")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL2")));

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
        assertThat(actual2.getTableName(), is("TEST_SUPPORT_TEST_TABLE"));

        assertThat(actual2.getPrimaryKeys().length, is(2));
        assertThat(actual2.getPrimaryKeys()[0], equalToIgnoringCase("PK_COL1"));
        assertThat(actual2.getPrimaryKeys()[1], equalToIgnoringCase("PK_COL2"));

        assertThat(actual2.getColumnNames().length, is(5));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL1")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL2")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("VARCHAR2_COL")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL2")));

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
    public void testGetExpectedTableDataNotExist() {
        List<TableData> actualList = target.getExpectedTableData("withGroupId", "notexits"); // 存在しないグループID
        assertNotNull(actualList);
        assertThat(actualList.size(), is(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetSetupTableData() {

        List<TableData> actualList = target.getSetupTableData("withoutGroupId");
        assertNotNull(actualList);
        assertThat(actualList.size(), is(2));

        TableData actual1 = actualList.get(0);
        assertThat(actual1.getTableName(), is("TEST_SUPPORT_TEST_TABLE"));
        assertThat(actual1.getPrimaryKeys().length, is(2));
        assertThat(actual1.getPrimaryKeys()[0], equalToIgnoringCase("PK_COL1"));
        assertThat(actual1.getPrimaryKeys()[1], equalToIgnoringCase("PK_COL2"));

        assertThat(actual1.getColumnNames().length, is(5));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL1")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL2")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("VARCHAR2_COL")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL2")));

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

        TableData actual2 = actualList.get(1);
        assertThat(actual2.getTableName(), is("TEST_SUPPORT_TEST_TABLE"));
        assertThat(actual2.getPrimaryKeys().length, is(2));
        assertThat(actual2.getPrimaryKeys()[0], equalToIgnoringCase("PK_COL1"));
        assertThat(actual2.getPrimaryKeys()[1], equalToIgnoringCase("PK_COL2"));

        assertThat(actual2.getColumnNames().length, is(5));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL1")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL2")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("VARCHAR2_COL")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL2")));

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

    /** {@link BasicTestDataParser#getTableData(String, String, nablarch.test.core.reader.DataType, String)}のテスト。 グループIDを指定した場合、グループIDが合致するテーブルだけが取得できることを確認する。 */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetSetupTableDataWithGroupId() {

        List<TableData> case01 = target.getSetupTableData("withGroupId", "case01"); // グループID指定
        assertNotNull(case01);
        assertThat(case01.size(), is(1));

        TableData actual1 = case01.get(0);
        assertThat(actual1.getTableName(), is("TEST_SUPPORT_TEST_TABLE"));
        assertThat(actual1.getPrimaryKeys().length, is(2));
        assertThat(actual1.getPrimaryKeys()[0], equalToIgnoringCase("PK_COL1"));
        assertThat(actual1.getPrimaryKeys()[1], equalToIgnoringCase("PK_COL2"));

        assertThat(actual1.getColumnNames().length, is(5));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL1")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL2")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("VARCHAR2_COL")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL")));
        assertThat(actual1.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL2")));
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

        List<TableData> case02 = target.getExpectedTableData("withGroupId", "case02"); // グループID指定
        assertThat(case02.size(), is(2));
        TableData actual2 = case02.get(0);
        assertThat(actual2.getTableName(), is("TEST_SUPPORT_TEST_TABLE"));
        assertThat(actual2.getPrimaryKeys().length, is(2));
        assertThat(actual2.getPrimaryKeys()[0], equalToIgnoringCase("PK_COL1"));
        assertThat(actual2.getPrimaryKeys()[1], equalToIgnoringCase("PK_COL2"));

        assertThat(actual2.getColumnNames().length, is(5));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL1")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL2")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("VARCHAR2_COL")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL2")));

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
        assertThat(actual2.getTableName(), is("TEST_SUPPORT_TEST_TABLE"));
        assertThat(actual2.getPrimaryKeys().length, is(2));
        assertThat(actual2.getPrimaryKeys()[0], equalToIgnoringCase("PK_COL1"));
        assertThat(actual2.getPrimaryKeys()[1], equalToIgnoringCase("PK_COL2"));

        assertThat(actual2.getColumnNames().length, is(5));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL1")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("PK_COL2")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("VARCHAR2_COL")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL")));
        assertThat(actual2.getColumnNames(), hasItemInArray(equalToIgnoringCase("NUMBER_COL2")));

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
    public void testSetThreadContext() {
        ThreadContext.clear();
        Map<String, String> contextValues = new HashMap<String, String>();
        contextValues.put(ThreadContext.USER_ID_KEY, "userId");
        contextValues.put(ThreadContext.REQUEST_ID_KEY, "requestId");
        contextValues.put(ThreadContext.LANG_KEY, "en");
        TestSupport.setThreadContextValues(contextValues);

        assertThat(ThreadContext.getUserId(), is("userId"));
        assertThat(ThreadContext.getRequestId(), is("requestId"));
        Locale locale = ThreadContext.getLanguage();
        assertThat(locale.toString(), is("en"));
    }

    @Test
    public void testSetThreadContext2() {
        ThreadContext.clear();
        Map<String, String> contextValues = new HashMap<String, String>();
        TestSupport.setThreadContextValues(contextValues);
        Locale locale = ThreadContext.getLanguage();
        assertThat(locale.toString(), is("ja_JP"));    // リポジトリの値
    }


    @Test
    public void testSetThreadContext3() {
        ThreadContext.clear();
        SystemRepository.clear();
        Map<String, String> contextValues = new HashMap<String, String>();
        TestSupport.setThreadContextValues(contextValues);
        Locale locale = ThreadContext.getLanguage();
        assertNull(locale);
    }


    @Test
    public void testGetParameterMap() {
        Map<String, String[]> param = target.getParameterMap("testGetParameterMap", "parameters");
        assertArrayEquals(new String[] {"foo"}, param.get("single"));
        assertArrayEquals(new String[] {"bar", "buz"}, param.get("multi"));
    }

    /** 円マークのエスケープが解除されること。 */
    @Test
    public void testGetParameterMapWithYen() {

        Map<String, String[]> param = target.getParameterMap("testGetParameterMap", "yen_sign");
        assertArrayEquals(new String[] {"\\foo\\"}, param.get("single"));
        assertArrayEquals(new String[] {"\\bar", "buz\\"}, param.get("multi"));
    }


    /** Excelから読み取ったパラメータのエスケープが解除されること。 */
    @Test
    public void testGetParameterEscaped() {

        Map<String, String[]> param = target.getParameterMap("testGetParameterMap", "escaped");
        assertArrayEquals(new String[] {"\\1,000"}, param.get("single"));
        assertArrayEquals(new String[] {"", "bar", "buz", "", ","}, param.get("multi"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetParameterMapFail() {
        target.getParameterMap("testGetParameterMap", "hoge");
    }

    /** エスケープ解除単独でテストするテストケース。 */
    @Test
    public void testUnescapeAndSplit() {

        assertEquals(Arrays.asList("aaa", "bbb"), TestSupport.unescapeAndSplit("aaa,bbb"));
        assertEquals(Arrays.asList("aaabbb"), TestSupport.unescapeAndSplit("aaabbb"));
        assertEquals(Arrays.asList("aaa,bbb"), TestSupport.unescapeAndSplit("aaa\\,bbb"));
        assertEquals(Arrays.asList("", "", ""), TestSupport.unescapeAndSplit(",,"));
        assertEquals(Arrays.asList("buz", "", ","), TestSupport.unescapeAndSplit("buz,,\\,"));
        assertEquals(Arrays.asList(""), TestSupport.unescapeAndSplit(""));
    }

    @Test
    public void testGetMap() {
        String sheetName = "testGetParameterMap";
        try {
            target.getMap(sheetName, "invalid id");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("no data row. sheetName=[" + sheetName + "]"));
        }
    }

    @Test
    public void testConvert() {
        Map<String, String> in = new HashMap<String, String>();
        in.put("1", "\\\\");                   // 円いっこ
        in.put("2", "\\\\\\\\");              // 円複数個
        in.put("3", "\\\\ho\\\\ge\\\\");     // 文字列中の円
        in.put("4", "\thoge\n");              // 他のエスケープ文字
        in.put("5", "");                       // 空文字
        in.put("6", "\\\\a,\\\\b");          // カンマ区切り
        Map<String, String[]> converted = TestSupport.convert(in);
        assertThat(converted.get("1")[0], is("\\"));
        assertThat(converted.get("2")[0], is("\\\\"));
        assertThat(converted.get("3")[0], is("\\ho\\ge\\"));
        assertThat(converted.get("4")[0], is("\thoge\n"));
        assertThat(converted.get("5")[0], is(""));
        assertThat(converted.get("6")[0], is("\\a"));
        assertThat(converted.get("6")[1], is("\\b"));
    }

    /** リソースが存在するパスが取得できること。 */
    @Test
    public void testGetPathOf() {
        repositoryResource.addComponent("nablarch.test.resource-root", "src/test/java;resources");
        try {
            String path = target.getPathOf("TestSupportTest/withGroupId");
            assertThat("javaディレクトリに存在する", path, is("src/test/java/nablarch/test"));
        } finally {
            RepositoryInitializer.initializeDefaultRepository();
        }
    }

    /** リソースが発見できない場合、例外が発生すること */
    @Test
    public void testGetPathOfFail() {
        new Trap("リソースが発見できない場合、例外が発生すること") {
            @Override
            protected void shouldFail() throws Exception {
                target.getPathOf("Not/Exists");
            }
        }.capture(IllegalArgumentException.class)
                .whichMessageContains(
                        "couldn't find resource [Not/Exists] in [src/test/java/nablarch/test]");
    }

    /**
     * リソースルートディレクトリが複数指定された場合、
     * それぞれに対応するテストデータのパスが取得できること。
     */
    @Test
    public void testGetTestDataPaths() {
        repositoryResource.addComponent("nablarch.test.resource-root", "java;resources");
        try {
            List<String> testDataPaths = target.getTestDataPaths();
            assertThat(testDataPaths, is(asList("java/nablarch/test",
                    "resources/nablarch/test")));
        } finally {
            RepositoryInitializer.initializeDefaultRepository();
        }
    }

    /** リソースが存在するパスが取得できること。 */
    @Test
    public void testGetPathResourceExisting() {
        List<String> candidatePath = asList("src/test/java/nablarch/test/core", // こっちには無い
                "src/test/java/nablarch/test/");    // こっちに存在する
        String resourceName = "TestSupportTest/SetUpDb";
        assertThat(target.getPathResourceExisting(candidatePath, resourceName), is("src/test/java/nablarch/test/"));
    }

    /** どこにもリソースが存在しない場合、nullが返却されること。 */
    @Test
    public void testGetPathResourceExistingFail() {
        List<String> candidates = asList("src/test/java/nablarch/test/core", "src/test/java/nablarch/test/");
        String resourceName = "Not/Exists";
        String path = target.getPathResourceExisting(candidates, resourceName);
        assertThat(path, is(nullValue()));
    }

    /** シート名にnullが許容されないこと */
    @Test(expected = IllegalArgumentException.class)
    public void testGetResourceNameNull() {
        target.getResourceName(null);
    }


    /** シート名に空文字が許容されないこと */
    @Test(expected = IllegalArgumentException.class)
    public void testGetResourceNameEmpty() {
        target.getResourceName("");
    }

    /**
     * ${半角記号}を使用した場合に、意図しない結果とならないこと
     *
     * - カンマが混じって、カンマ区切り（配列）と見做されないこと
     * - 指定した文字数だけ文字が生成されること
     *    （生成した文字列がエスケープシーケンスと見做されないこと）
     */
    @Test
    public void testHankakuKigou() {
        Map<String, String[]> parameterMap = target.getParameterMap("testHankakuKigou", "parameters");

        String[] value1 = parameterMap.get("eiji");
        assertThat(value1.length, is(1));
        String eiji = value1[0];
        assertThat(eiji, eiji.matches("[A-Za-z]"), is(true));

        String[] value2 = parameterMap.get("kigou");
        // 半角記号にカンマが混じってしまい、その結果カンマ区切りと見做され配列にsplitされないこと
        assertThat(value2.length, is(1));
        String kigou = value2[0];
        assertThat(kigou.length(), is(4096));

    }

    @Entity
    @Table(name = "TEST_SUPPORT_TEST_TABLE")
    public static class TestSupportTestTable {

        public TestSupportTestTable() {
        }

        ;

        public TestSupportTestTable(String pkCol1, String pkCol2,
                String varchar2Col, Long numberCol, BigDecimal numberCol2) {
            this.pkCol1 = pkCol1;
            this.pkCol2 = pkCol2;
            this.varchar2Col = varchar2Col;
            this.numberCol = numberCol;
            this.numberCol2 = numberCol2;
        }

        @Id
        @Column(name = "PK_COL1", length = 10, nullable = false)
        public String pkCol1;

        @Id
        @Column(name = "PK_COL2", length = 2, nullable = false)
        public String pkCol2;

        @Column(name = "VARCHAR2_COL", length = 20, nullable = false)
        public String varchar2Col;

        @Column(name = "NUMBER_COL", length = 10, nullable = false)
        public Long numberCol;

        @Column(name = "NUMBER_COL2", precision = 10, scale = 3, nullable = false)
        public BigDecimal numberCol2;
    }
}
