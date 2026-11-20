package nablarch.test.core.reader;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nablarch.test.Trap;
import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.GenericJdbcDbInfo;
import nablarch.test.core.db.TableData;
import nablarch.test.core.util.interpreter.InterpretationContext;
import nablarch.test.core.util.interpreter.NullInterpreter;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import org.junit.Before;
import org.junit.Test;

/**
 * {@link TestDataParsingTemplate}のテストクラス。
 *
 * @author T.Kawasaki
 */
public class TestDataParsingTemplateTest {

    private TestDataParsingTemplate target
            = new ListMapParser(new MockTestDataReader(),
                                Collections.<TestDataInterpreter>emptyList());

    /**
     * parseメソッドが失敗した場合、ディレクトリ名、リソース名、グループIDが
     * 例外メッセージに含められること。
     */
    @Test
    public void testParseFail() {
        try {
            new ListMapParser(new MockTestDataReader(),
                              Collections.<TestDataInterpreter>emptyList()) {
                @Override
                protected void parse(String id) {
                    throw new RuntimeException("for test.");
                }
            }
            .parse("dir", "resource", "gid");
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(),
                       containsString("directory=[dir] resource=[resource] id=[gid]"));
        }
    }

    /** データタイプ取得時の引数がnullの場合、デフォルトが返却されること。 */
    @Test
    public void testGetDataTypeNull() {
        assertThat(target.getDataType(null), is(DataType.DEFAULT));
    }

    /** コメント行の判定が正しいこと。 */
    @Test
    public void testIsCommentRow() {
        // コメント行
        assertThat(target.isCommentRow(Arrays.asList("// comment", "value")), is(true));
        // コメント行でない
        assertThat(target.isCommentRow(Arrays.asList("value1", "value2")), is(false));

        // nullの場合もコメント行でない
        List<String> nullLine = new ArrayList<String>();
        nullLine.add(null);
        assertThat(target.isCommentRow(nullLine), is(false));
    }


    private List<List<String>> containingNull = new ArrayList<List<String>>();


    @Before
    public void setUpList() {
        containingNull.add(Arrays.asList("LIST_MAP=listMap"));
        containingNull.add(Arrays.asList("foo", "bar", "buz"));
        containingNull.add(Arrays.asList("null", "two", "three"));
    }

    /**
     * NullInterpreterで行の先頭セルがnullに変換された場合、
     * {@link NullPointerException}が発生する問題を対処。<br/>
     * 
     */
    @Test
    public void testSingleDataWithNullInterpreter() {

        ListMapParser parser = new ListMapParser(
                new MockTestDataReader(containingNull),
                Arrays.<TestDataInterpreter>asList(new NullInterpreter())
        );
        parser.parse(getClass().getName(), "testSingleDataWithNullInterpreter", "listMap");
        List<Map<String, String>> result = parser.getResult();
        assertThat(result.size(), is(1));
        List<Map<String, String>> expected = new ArrayList<Map<String, String>>();

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("foo", null);
        map.put("bar", "two");
        map.put("buz", "three");
        expected.add(map);

        assertEquals(result, expected);
    }

    /**
     * NullInterpreterで行の先頭セルがnullに変換された場合、
     * {@link NullPointerException}が発生する問題を対処。<br/>
     * 
     */
    @Test
    public void testGroupDataWithNullInterpreter() {
        TableDataParser parser = new TableDataParser(
                new MockTestDataReader(containingNull),
                Arrays.<TestDataInterpreter>asList(new NullInterpreter()),
                new GenericJdbcDbInfo(),
                new BasicDefaultValues(),
                DataType.EXPECTED_TABLE_DATA);
        parser.parse(getClass().getName(), "testGroupDataWithNullInterpreter", "hoge");
        List<TableData> result = parser.getResult();
        assertThat(result.size(), is(0));
    }

    /**
     * saveCache=true の場合、2 回目の parse でファイルを再読み込みしないこと（キャッシュヒット）。
     * saveCache=false の場合、毎回ファイルを再読み込みすること（キャッシュを使わない）。
     */
    @Test
    public void testSaveCacheControlsCaching() {
        // Given: open 呼び出し回数を記録するリーダ
        final int[] openCount = {0};
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(Arrays.asList("LIST_MAP=id1"));
        lines.add(Arrays.asList("key", "value"));
        lines.add(Arrays.asList("a", "1"));
        MockTestDataReader countingReader = new MockTestDataReader(lines) {
            @Override
            public void open(String path, String dataName) {
                openCount[0]++;
            }
        };

        // When: saveCache=true で 2 回 parse
        ListMapParser parser1 = new ListMapParser(countingReader, Collections.<TestDataInterpreter>emptyList());
        parser1.parse("dir", "resource1", "id1", true);
        parser1.parse("dir", "resource1", "id1", true);

        // Then: ファイルを開くのは 1 回だけ（2 回目はキャッシュヒット）
        assertThat(openCount[0], is(1));

        // Given: カウンタをリセット
        openCount[0] = 0;

        // When: saveCache=false で 2 回 parse
        ListMapParser parser2 = new ListMapParser(countingReader, Collections.<TestDataInterpreter>emptyList());
        parser2.parse("dir", "resource2", "id1", false);
        parser2.parse("dir", "resource2", "id1", false);

        // Then: キャッシュを使わないので毎回ファイルを開く
        assertThat(openCount[0], is(2));
    }

    /**
     * 同じリソースを saveCache=true で読んだ後に saveCache=false で読むと、
     * 生テストデータのキャッシュ（TEST_DATA_CACHE）を参照せず、ファイルを読み直すこと。
     */
    @Test
    public void testSaveCacheFalseDoesNotReferCacheStoredBySaveCacheTrue() {
        // Given: open のたびに内容が変わるリーダ（n 回目の open で value 列に n を返す）
        final int[] openCount = {0};
        TestDataReader changingReader = new TestDataReader() {
            private Iterator<List<String>> iterator;

            @Override
            public void open(String path, String dataName) {
                openCount[0]++;
                iterator = Arrays.<List<String>>asList(
                        Arrays.asList("LIST_MAP=id1"),
                        Arrays.asList("key", "value"),
                        Arrays.asList("a", String.valueOf(openCount[0]))).iterator();
            }

            @Override
            public void close() {
            }

            @Override
            public List<String> readLine() {
                return iterator.hasNext() ? iterator.next() : null;
            }

            @Override
            public boolean isResourceExisting(String basePath, String resourceName) {
                return true;
            }

            @Override
            public boolean isDataExisting(String basePath, String resourceName) {
                return true;
            }
        };
        String directory = getClass().getName();
        String resource = "testSaveCacheFalseDoesNotReferCacheStoredBySaveCacheTrue";
        ListMapParser cachedParser = new ListMapParser(changingReader, Collections.<TestDataInterpreter>emptyList());
        cachedParser.parse(directory, resource, "id1", true);
        assertThat(openCount[0], is(1));
        assertThat(cachedParser.getResult().get(0).get("value"), is("1"));

        // When: 同じリソースを saveCache=false で parse
        ListMapParser notCachedParser = new ListMapParser(changingReader, Collections.<TestDataInterpreter>emptyList());
        notCachedParser.parse(directory, resource, "id1", false);

        // Then: ファイルを開き直し、キャッシュの値ではなく読み直した値を返す
        assertThat(openCount[0], is(2));
        assertThat(notCachedParser.getResult().get(0).get("value"), is("2"));
    }

    /**
     * {@link TableDataParser} で同じリソースを saveCache=true で読んだ後に saveCache=false で読むと、
     * 解析結果のキャッシュを参照せず、ファイルを読み直すこと。
     */
    @Test
    public void testTableDataParserSaveCacheFalseDoesNotReferCache() {
        // Given: n 回目の open でテーブル名 TABLE_n のブロックを返すリーダ
        ChangingReader reader = new ChangingReader() {
            @Override
            List<List<String>> linesFor(int openCount) {
                return Arrays.<List<String>>asList(
                        Arrays.asList("EXPECTED_TABLE=TABLE_" + openCount),
                        Arrays.asList("ID"));
            }
        };
        String directory = getClass().getName();
        String resource = "testTableDataParserSaveCacheFalseDoesNotReferCache";
        TableDataParser cachedParser = newTableDataParser(reader);
        cachedParser.parse(directory, resource, "", true);
        assertThat(cachedParser.getResult().get(0).getTableName(), is("TABLE_1"));

        // When: 同じリソースを saveCache=false で parse
        TableDataParser notCachedParser = newTableDataParser(reader);
        notCachedParser.parse(directory, resource, "", false);

        // Then: ファイルを開き直し、キャッシュの値ではなく読み直した値を返す
        assertThat(reader.openCount, is(2));
        assertThat(notCachedParser.getResult().size(), is(1));
        assertThat(notCachedParser.getResult().get(0).getTableName(), is("TABLE_2"));

        // Then: saveCache=false で読んだ結果はキャッシュに保存されていない（saveCache=true では 1 回目の結果が返る）
        TableDataParser cachedAgainParser = newTableDataParser(reader);
        cachedAgainParser.parse(directory, resource, "", true);
        assertThat(reader.openCount, is(2));
        assertThat(cachedAgainParser.getResult().get(0).getTableName(), is("TABLE_1"));
    }

    /**
     * {@link DataFileParser}（{@link FixedLengthFileParser}）で同じリソースを saveCache=true で読んだ後に
     * saveCache=false で読むと、解析結果のキャッシュを参照せず、ファイルを読み直すこと。
     * <p>
     * {@link DataFileParser} は、キャッシュにヒットしても値は再利用せずに解析し直し、
     * 結果が空の場合だけキャッシュの値を使う。そのため、1 回目は対象のブロックが無く（空の結果がキャッシュされる）、
     * 2 回目に対象のブロックがある場合で確かめる。
     * </p>
     */
    @Test
    public void testDataFileParserSaveCacheFalseDoesNotReferCache() {
        // Given: 1 回目の open では対象のブロックが無く、2 回目以降はファイル名 file_n.dat のブロックを返すリーダ
        ChangingReader reader = new ChangingReader() {
            @Override
            List<List<String>> linesFor(int openCount) {
                if (openCount == 1) {
                    return Arrays.<List<String>>asList(
                            Arrays.asList("LIST_MAP=other"),
                            Arrays.asList("key"),
                            Arrays.asList("value"));
                }
                return Arrays.<List<String>>asList(
                        Arrays.asList("EXPECTED_FIXED=file_" + openCount + ".dat"),
                        Arrays.asList("data", "name"),
                        Arrays.asList("", "半角英数字"),
                        Arrays.asList("", "5"),
                        Arrays.asList("", "abc"));
            }
        };
        String directory = getClass().getName();
        String resource = "testDataFileParserSaveCacheFalseDoesNotReferCache";
        FixedLengthFileParser cachedParser = newFixedLengthFileParser(reader);
        cachedParser.parse(directory, resource, "", true);
        assertThat(cachedParser.getResult().isEmpty(), is(true));

        // When: 同じリソースを saveCache=false で parse
        FixedLengthFileParser notCachedParser = newFixedLengthFileParser(reader);
        notCachedParser.parse(directory, resource, "", false);

        // Then: キャッシュされた空の結果ではなく、ファイルを開き直して読んだ値を返す
        assertThat(reader.openCount, is(2));
        assertThat(notCachedParser.getResult().size(), is(1));
        assertThat(notCachedParser.getResult().get(0).getPath(), is("file_2.dat"));
    }

    private static TableDataParser newTableDataParser(TestDataReader reader) {
        return new TableDataParser(reader, Collections.<TestDataInterpreter>emptyList(),
                new GenericJdbcDbInfo(), new BasicDefaultValues(), DataType.EXPECTED_TABLE_DATA);
    }

    private static FixedLengthFileParser newFixedLengthFileParser(TestDataReader reader) {
        return new FixedLengthFileParser(reader, Collections.<TestDataInterpreter>emptyList(),
                DataType.EXPECTED_FIXED);
    }

    /** open のたびに、開いた回数に応じた内容を返すリーダ。 */
    private abstract static class ChangingReader implements TestDataReader {

        /** open された回数 */
        int openCount = 0;

        /** 読み込み中の行 */
        private Iterator<List<String>> iterator;

        /**
         * n 回目の open で返す行を作る。
         *
         * @param openCount open された回数（1 始まり）
         * @return 行
         */
        abstract List<List<String>> linesFor(int openCount);

        @Override
        public void open(String path, String dataName) {
            openCount++;
            iterator = linesFor(openCount).iterator();
        }

        @Override
        public void close() {
        }

        @Override
        public List<String> readLine() {
            return iterator.hasNext() ? iterator.next() : null;
        }

        @Override
        public boolean isResourceExisting(String basePath, String resourceName) {
            return true;
        }

        @Override
        public boolean isDataExisting(String basePath, String resourceName) {
            return true;
        }
    }

    @Test
    public void testClosedWhenExceptionOccurred() {
        final boolean[] closed = {false};
        MockTestDataReader mockReader = new MockTestDataReader(new ArrayList<List<String>>(containingNull)) {
            @Override
            public void close() {
                closed[0] = true;
            }
        };

        final ListMapParser parser = new ListMapParser(
                mockReader,
                Arrays.<TestDataInterpreter>asList(new TestDataInterpreter() {
                    // データ読み込み後の変換処理で例外が発生する。
                    @Override
                    public String interpret(InterpretationContext context) {
                        throw new RuntimeException("for test");
                    }
                })
        );
        new Trap("例外が発生すること。") {
            @Override
            protected void shouldFail() throws Exception {
                parser.parse(getClass().getName(), "testSingleDataWithNullInterpreter", "listMap");
            }
        }.capture(RuntimeException.class)
         .whichMessageContains("interpretation failed.");

        assertThat("closeが呼ばれていること。",
                   closed[0], is(true));

    }
}
