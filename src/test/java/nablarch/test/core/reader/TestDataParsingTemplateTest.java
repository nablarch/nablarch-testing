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
