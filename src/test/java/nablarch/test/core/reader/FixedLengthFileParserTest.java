package nablarch.test.core.reader;

import nablarch.test.core.util.interpreter.TestDataInterpreter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
}
