package nablarch.test.core.util;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


/**
 * @author T.Kawasaki
 */
public class MapCollectorTest {

    private Map<String, String> orig = new HashMap<String, String>();

    @Before
    public void setUp() {
        orig.put("one", "1");
        orig.put("two", "2");
        orig.put("three", "3");
        orig.put("four", "4");
    }

    @Test
    public void testCollect() {
        Map<String, Integer> actual = new MapCollector<Integer, String, String>() {
            @Override
            protected Integer evaluate(String key, String value) {
                return Integer.valueOf(value);
            }
        }.collect(orig);
        assertThat(actual.get("one"), is(1));
        assertThat(actual.get("two"), is(2));
        assertThat(actual.get("three"), is(3));
        assertThat(actual.get("four"), is(4));
        assertThat(actual.size(), is(4));
    }

    @Test
    public void testFullConstructor() {
        Map<String, Integer> container = new TreeMap<String, Integer>();
        Map<String, Integer> actual = new MapCollector<Integer, String, String>(container) {
            @Override
            protected Integer evaluate(String key, String value) {
                return null;
            }
        }.collect(orig);
        assertTrue(actual instanceof TreeMap);
        assertSame(container, actual);
    }

    @Test
    public void testSkip() {
        Map<String, Integer> actual = new MapCollector<Integer, String, String>() {
            @Override
            protected Integer evaluate(String key, String value) {
                int intValue = Integer.valueOf(value);
                if (intValue % 2 == 0) {
                    return skip();
                }
                return intValue;
            }
        }.collect(orig);
        assertThat(actual.get("one"), is(1));
        assertThat(actual.get("three"), is(3));
        assertThat(actual.size(), is(2));
    }
}
