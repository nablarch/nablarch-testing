package nablarch.test.tool.sanitizingcheck.out;

import nablarch.test.tool.sanitizingcheck.SanitizingCheckTestSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SanitizingCheckResultOutクラスのテスト
 *
 * @author Tomokazu Kagawa
 * @see SanitizingCheckResultOut
 */
public class SanitizingCheckResultOutTest extends SanitizingCheckTestSupport {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    /**
     * outToXmlメソッドのテスト
     *
     * @see SanitizingCheckResultOut#outToXml(Map, String)
     */
    @Test
    public void testOutToXml() throws IOException {

        // Mapのサイズが1、Listサイズが1の場合
        Map<String, List<String>> sanitizingCheckMessages11 = new LinkedHashMap<String, List<String>>();
        List<String> list11 = new ArrayList<String>();
        list11.add("message1");
        sanitizingCheckMessages11.put("testFile1", list11);
        String actualFilePath = tmpDir.newFile("map1list1.xml").getAbsolutePath();
        SanitizingCheckResultOut.outToXml(sanitizingCheckMessages11, actualFilePath);
        assertFile("src/test/java/nablarch/test/tool/sanitizingcheck/out/expected/map1list1.xml",
                actualFilePath);

        // Mapのサイズが1、Listサイズが2の場合
        Map<String, List<String>> sanitizingCheckMessages12 = new LinkedHashMap<String, List<String>>();
        List<String> list12 = new ArrayList<String>();
        list12.add("message1");
        list12.add("message2");
        sanitizingCheckMessages12.put("testFile1", list12);
        actualFilePath = tmpDir.newFile("map1list2.xml").getAbsolutePath();
        SanitizingCheckResultOut.outToXml(sanitizingCheckMessages12,
                actualFilePath);
        assertFile("src/test/java/nablarch/test/tool/sanitizingcheck/out/expected/map1list2.xml",
                actualFilePath);

        // Mapのサイズが1、Listサイズが3の場合
        Map<String, List<String>> sanitizingCheckMessages13 = new LinkedHashMap<String, List<String>>();
        List<String> list13 = new ArrayList<String>();
        list13.add("message1");
        list13.add("message2");
        list13.add("message3");
        sanitizingCheckMessages13.put("testFile1", list13);
        actualFilePath = tmpDir.newFile("map1list3.xml").getAbsolutePath();
        SanitizingCheckResultOut.outToXml(sanitizingCheckMessages13, actualFilePath);
        assertFile("src/test/java/nablarch/test/tool/sanitizingcheck/out/expected/map1list3.xml", actualFilePath);

        // Mapのサイズが2、Listサイズが1の場合
        Map<String, List<String>> sanitizingCheckMessages21 = new LinkedHashMap<String, List<String>>();
        List<String> list211 = new ArrayList<String>();
        list211.add("message1");
        sanitizingCheckMessages21.put("testFile1", list211);
        List<String> list212 = new ArrayList<String>();
        list212.add("message2");
        sanitizingCheckMessages21.put("testFile2", list212);
        actualFilePath = tmpDir.newFile("map2list1.xml").getAbsolutePath();
        SanitizingCheckResultOut.outToXml(sanitizingCheckMessages21, actualFilePath);
        assertFile("src/test/java/nablarch/test/tool/sanitizingcheck/out/expected/map2list1.xml", actualFilePath);

        // Mapのサイズが3、Listサイズが1の場合
        Map<String, List<String>> sanitizingCheckMessages31 = new LinkedHashMap<String, List<String>>();
        List<String> list311 = new ArrayList<String>();
        list311.add("message1");
        sanitizingCheckMessages31.put("testFile1", list311);
        List<String> list312 = new ArrayList<String>();
        list312.add("message2");
        sanitizingCheckMessages31.put("testFile2", list312);
        List<String> list313 = new ArrayList<String>();
        list313.add("message3");
        sanitizingCheckMessages31.put("testFile3", list313);
        actualFilePath = tmpDir.newFile("map3list1.xml").getAbsolutePath();
        SanitizingCheckResultOut.outToXml(sanitizingCheckMessages31, actualFilePath);
        assertFile("src/test/java/nablarch/test/tool/sanitizingcheck/out/expected/map3list1.xml", actualFilePath);
    }
}
