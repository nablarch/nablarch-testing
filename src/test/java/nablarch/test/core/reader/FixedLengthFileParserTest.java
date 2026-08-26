package nablarch.test.core.reader;

import nablarch.core.dataformat.DataRecord;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.util.interpreter.TestDataInterpreter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author T.Kawasaki
 */
public class FixedLengthFileParserTest {

    /**
     * ディレクティブの指定が誤っている場合（値が設定されていない場合）、
     * 例外が発生すること。
     */
    @Test(expected = IllegalStateException.class)
    public void testInvalidDirectives() {
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(Arrays.asList("EXPECTED_FIXED[group]=hoge"));
        lines.add(Arrays.asList("file-encoding")); // 少ない

        FixedLengthFileParser target = new FixedLengthFileParser(new MockTestDataReader(lines),
                Collections.<TestDataInterpreter>emptyList(),
                DataType.EXPECTED_FIXED);
        target.parse("dummy", "dummy", "[group]");
    }

    /**
     * フィールド名称の数を超えた位置に書かれた値が、警告も例外もなく捨てられること。
     * <p>
     * 現行挙動を固定する特性テストである。データ行の超過分は
     * {@code DataFileFragment#addValue(List)} のループ上限（{@code names.size()}）で
     * 読まれずに終わる。上限を {@code line.size()} に変えると
     * {@link IndexOutOfBoundsException} となり本テストは失敗する。
     * </p>
     */
    @Test
    public void testValuesBeyondFieldNamesAreDiscarded() {
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(Arrays.asList("SETUP_FIXED[excess]=work/excess.txt"));
        lines.add(Arrays.asList("text-encoding", "UTF-8"));
        lines.add(Arrays.asList("DATA", "field1", "field2"));
        lines.add(Arrays.asList("", "半角英字", "半角英字"));
        lines.add(Arrays.asList("", "5", "5"));
        // フィールド名称は2つだが、3つ目の値を書いたデータ行
        lines.add(Arrays.asList("", "abc", "de", "excess"));

        FixedLengthFileParser target = new FixedLengthFileParser(new MockTestDataReader(lines),
                Collections.<TestDataInterpreter>emptyList(),
                DataType.SETUP_FIXED);
        target.parse("dummy", "excessValues", "[excess]");
        List<FixedLengthFile> files = target.getResult();

        assertThat(files.size(), is(1));
        List<DataRecord> records = files.get(0).toDataRecords();
        assertThat(records.size(), is(1));
        DataRecord record = records.get(0);
        assertThat("フィールド名称の数だけが読み込まれること", record.size(), is(2));
        assertThat(record.getString("field1"), is("abc"));
        assertThat(record.getString("field2"), is("de"));
        assertThat("超過分の値はどのフィールドにも現れないこと",
                record.values().contains("excess"), is(false));
    }

    /**
     * データ行の末尾に空セルが続いても、超過分として扱われないこと。
     * <p>
     * 末尾の空要素は {@code DataFileParser#onReadLine(List)} の
     * {@code NablarchTestUtils#trimTailCopy(List)} で取り除かれるため、
     * フィールド名称の数を超えた位置に達しない。解説書が述べる記法どおりに書いた利用者が
     * 超過分の切り捨てを踏まないことを示す特性テストである。
     * </p>
     */
    @Test
    public void testTrailingEmptyCellsAreNotTreatedAsExcess() {
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(Arrays.asList("SETUP_FIXED[trail]=work/trail.txt"));
        lines.add(Arrays.asList("text-encoding", "UTF-8"));
        lines.add(Arrays.asList("DATA", "field1", "field2"));
        lines.add(Arrays.asList("", "半角英字", "半角英字"));
        lines.add(Arrays.asList("", "5", "5"));
        // 罫線などの都合で末尾に空セルが続いた行
        lines.add(Arrays.asList("", "abc", "de", "", "", ""));

        FixedLengthFileParser target = new FixedLengthFileParser(new MockTestDataReader(lines),
                Collections.<TestDataInterpreter>emptyList(),
                DataType.SETUP_FIXED);
        target.parse("dummy", "trailingEmpty", "[trail]");
        List<FixedLengthFile> files = target.getResult();

        assertThat(files.size(), is(1));
        List<DataRecord> records = files.get(0).toDataRecords();
        assertThat(records.size(), is(1));
        DataRecord record = records.get(0);
        assertThat(record.size(), is(2));
        assertThat(record.getString("field1"), is("abc"));
        assertThat(record.getString("field2"), is("de"));
    }

    /**
     * データ行の末尾に置いたコメントセルが、超過分として扱われないこと。
     * <p>
     * {@code //} で始まるセル以降は {@code TestDataParsingTemplate#cutComment(List)} で
     * 切り落とされるため、フィールド名称の数を超えた位置に達しない。
     * {@link #testTrailingEmptyCellsAreNotTreatedAsExcess()} と同じく、記法どおりに書いた
     * 利用者が超過分の切り捨てを踏まないことを示す特性テストである。
     * </p>
     */
    @Test
    public void testTrailingCommentCellIsNotTreatedAsExcess() {
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(Arrays.asList("SETUP_FIXED[comment]=work/comment.txt"));
        lines.add(Arrays.asList("text-encoding", "UTF-8"));
        lines.add(Arrays.asList("DATA", "field1", "field2"));
        lines.add(Arrays.asList("", "半角英字", "半角英字"));
        lines.add(Arrays.asList("", "5", "5"));
        // 末尾にレコードの補足コメントを置いた行
        lines.add(Arrays.asList("", "abc", "de", "// このレコードが追加される"));

        FixedLengthFileParser target = new FixedLengthFileParser(new MockTestDataReader(lines),
                Collections.<TestDataInterpreter>emptyList(),
                DataType.SETUP_FIXED);
        target.parse("dummy", "trailingComment", "[comment]");
        List<FixedLengthFile> files = target.getResult();

        assertThat(files.size(), is(1));
        List<DataRecord> records = files.get(0).toDataRecords();
        assertThat(records.size(), is(1));
        DataRecord record = records.get(0);
        assertThat(record.size(), is(2));
        assertThat(record.getString("field1"), is("abc"));
        assertThat(record.getString("field2"), is("de"));
    }
}
