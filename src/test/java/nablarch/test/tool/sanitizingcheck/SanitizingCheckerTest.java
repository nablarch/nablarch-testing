package nablarch.test.tool.sanitizingcheck;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import nablarch.test.support.tool.Hereis;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * サニタイジングチェックツールテスト
 *
 * @author Tomokazu Kagawa
 * @see SanitizingChecker
 */
public class SanitizingCheckerTest {

    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    /** テスト用のディレクトリ */
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /** テストで使う設定ファイルのパス */
    private File configFile = null;

    @Before
    public void setUp() throws Exception {
        configFile = temporaryFolder.newFile("config.txt");
    }

    /**
     * JSP1ファイルだけをチェック対象とした場合でチェックエラーが発生する場合。
     * <p/>
     * 対象ファイルに対するチェックが行えること。
     */
    @Test
    public void testSingleJsp() throws Exception {
        // 設定ファイルの作成
        Hereis.file(configFile.getAbsolutePath());
        /*
        <n:form
        <%@ taglib
        <%@ page
        */

        // チェック対象のJSP
        // 「<%」、「<%=」、「n:set」がエラーとなる
        String jspFilePath = temporaryFolder.newFile("target.jsp").getAbsolutePath();
        createJspFile(jspFilePath, "set");

        // テスト実行
        SanitizingChecker checker = new SanitizingChecker(configFile.getAbsolutePath(), CHARSET_UTF8, Collections.<String>emptyList(), null);
        Map<String, List<String>> checkResult = checker.checkSanitizing(jspFilePath);

        assertThat("エラーが発生するので、JSPファイルに対応するエラーリストが格納されている。", checkResult.containsKey(jspFilePath), is(true));
        List<String> errorList = checkResult.get(jspFilePath);
        assertThat("エラーは3箇所", errorList.size(), is(3));
        assertThat("スクリプトレットがエラーとなること", errorList.get(0), is("JSP Scriptlet: <% xxx %>"
                + " (at line=5 column=1) is misplaced or is not permitted to use in this project."));
        assertThat("JSP式がエラーとなること", errorList.get(1), is("JSP Expression: <%= xxx %>"
                + " (at line=14 column=21) is misplaced or is not permitted to use in this project."));
        assertThat("許可されない「n:set」がエラーとなること", errorList.get(2), is("Custom Tag: <n:set>"
                + " (at line=15 column=3) is misplaced or is not permitted to use in this project."));

    }

    /** JSP1ファイルをチェック対象とした場合で、チェックエラーが発生しない場合。 */
    @Test
    public void testSingleJspNoerrors() throws Exception {
        // 設定ファイルの作成
        Hereis.file(configFile.getAbsolutePath());
        /*
        <n
        <%@ taglib
        <%@ page
        <%
        <%=
        */

        // チェック対象のJSP
        // 「<%」、「<%=」、「n:set」がエラーとなる
        String jspFilePath = temporaryFolder.newFile("target2.jsp").getAbsolutePath();
        createJspFile(jspFilePath, "set");

        // テスト実行
        SanitizingChecker checker = new SanitizingChecker(configFile.getAbsolutePath(), CHARSET_UTF8, Collections.<String>emptyList(), null);
        Map<String, List<String>> checkResult = checker.checkSanitizing(jspFilePath);

        assertThat("エラーが発生しないので、エラーリストは格納されていない", checkResult.containsKey(jspFilePath), is(false));
    }

    /**
     * ディレクトリを対象とした場合、配下のJSPが（再帰的に）チェックされること。
     * また、追加でチェック対象とした拡張子すべてについて、チェックが行われること。
     */
    @Test
    public void testMultiJsp() throws Exception {
        // 設定ファイルの作成
        Hereis.file(configFile.getAbsolutePath());
        /*
        <n:form
        <%@ taglib
        <%@ page
        <%
        <%=
        */

        // チェック対象のJSP
        // 「n:set」がエラーとなる
        String jsp1 = temporaryFolder.newFile("target1.jsp").getAbsolutePath();
        createJspFile(jsp1, "set1");
        String jsp2 = temporaryFolder.newFile("target2.jsp").getAbsolutePath();
        createJspFile(jsp2, "set2");
        String tag1 = temporaryFolder.newFile("target1.tag").getAbsolutePath();
        createJspFile(tag1, "tag1");
        String inc1 = temporaryFolder.newFile("target2.inc").getAbsolutePath();
        createJspFile(inc1, "inc1");

        // サブディレクトリ
        File subDir = temporaryFolder.newFile("subDir");
        subDir.mkdir();
        String jsp3 = new File(subDir.getAbsoluteFile() + "target3.jsp").getAbsolutePath();
        createJspFile(jsp3, "set3");
        String jsp4 = new File(subDir.getAbsoluteFile() + "target4.jsp").getAbsolutePath();
        createJspFile(jsp4, "set4");
        String tag2 = new File(subDir.getAbsoluteFile() + "target3.tag").getAbsolutePath();
        createJspFile(tag2, "tag2");
        String inc2 = new File(subDir.getAbsoluteFile() + "target4.inc").getAbsolutePath();
        createJspFile(inc2, "inc2");

        // テスト実行
        SanitizingChecker checker = new SanitizingChecker(configFile.getAbsolutePath(), CHARSET_UTF8, Arrays.asList("tag", "inc"), null);
        Map<String, List<String>> checkResult = checker.checkSanitizing(temporaryFolder.getRoot().getAbsolutePath());

        assertThat("エラーが発生するので、JSPファイルに対応するエラーリストが格納されている。", checkResult.containsKey(jsp1), is(true));
        assertThat("エラーが発生するので、JSPファイルに対応するエラーリストが格納されている。", checkResult.containsKey(jsp2), is(true));
        assertThat("エラーが発生するので、JSPファイルに対応するエラーリストが格納されている。", checkResult.containsKey(jsp3), is(true));
        assertThat("エラーが発生するので、JSPファイルに対応するエラーリストが格納されている。", checkResult.containsKey(jsp4), is(true));
        assertThat("エラーが発生するので、tagファイルに対応するエラーリストが格納されている。", checkResult.containsKey(tag1), is(true));
        assertThat("エラーが発生するので、tagファイルに対応するエラーリストが格納されている。（サブディレクトリ）", checkResult.containsKey(tag2), is(true));
        assertThat("エラーが発生するので、incファイルに対応するエラーリストが格納されている。", checkResult.containsKey(inc1), is(true));
        assertThat("エラーが発生するので、incファイルに対応するエラーリストが格納されている。（サブディレクトリ）", checkResult.containsKey(inc2), is(true));

        List<String> errorList = checkResult.remove(jsp1);
        assertThat("エラーは1箇所", errorList.size(), is(1));
        assertThat("許可されない「n:set1」がエラーとなること", errorList.get(0), is("Custom Tag: <n:set1>"
                + " (at line=15 column=3) is misplaced or is not permitted to use in this project."));

        errorList = checkResult.remove(jsp2);
        assertThat("エラーは1箇所", errorList.size(), is(1));
        assertThat("許可されない「n:set2」がエラーとなること", errorList.get(0), is("Custom Tag: <n:set2>"
                + " (at line=15 column=3) is misplaced or is not permitted to use in this project."));

        errorList = checkResult.remove(jsp3);
        assertThat("エラーは1箇所", errorList.size(), is(1));
        assertThat("許可されない「n:set3」がエラーとなること", errorList.get(0), is("Custom Tag: <n:set3>"
                + " (at line=15 column=3) is misplaced or is not permitted to use in this project."));

        errorList = checkResult.remove(jsp4);
        assertThat("エラーは1箇所", errorList.size(), is(1));
        assertThat("許可されない「n:set4」がエラーとなること", errorList.get(0), is("Custom Tag: <n:set4>"
                + " (at line=15 column=3) is misplaced or is not permitted to use in this project."));

        errorList = checkResult.remove(tag1);
        assertThat("エラーは1箇所", errorList.size(), is(1));
        assertThat("許可されない「n:tag1」がエラーとなること", errorList.get(0), is("Custom Tag: <n:tag1>"
                + " (at line=15 column=3) is misplaced or is not permitted to use in this project."));

        errorList = checkResult.remove(tag2);
        assertThat("エラーは1箇所", errorList.size(), is(1));
        assertThat("許可されない「n:tag2」がエラーとなること", errorList.get(0), is("Custom Tag: <n:tag2>"
                + " (at line=15 column=3) is misplaced or is not permitted to use in this project."));

        errorList = checkResult.remove(inc1);
        assertThat("エラーは1箇所", errorList.size(), is(1));
        assertThat("許可されない「n:inc1」がエラーとなること", errorList.get(0), is("Custom Tag: <n:inc1>"
                + " (at line=15 column=3) is misplaced or is not permitted to use in this project."));

        errorList = checkResult.remove(inc2);
        assertThat("エラーは1箇所", errorList.size(), is(1));
        assertThat("許可されない「n:inc2」がエラーとなること", errorList.get(0), is("Custom Tag: <n:inc2>"
                + " (at line=15 column=3) is misplaced or is not permitted to use in this project."));

        assertThat("チェック結果は最後には空になる", checkResult.isEmpty(), is(true));
    }

    /**
     * 複数ファイルがチェック対象で、エラーが発生するファイルが一部だけの場合。
     *
     * エラーが発生したファイルのエラー情報が正しく出力されること。
     */
    @Test
    public void testMultiJsp2() throws Exception {
        // 設定ファイルの作成
        Hereis.file(configFile.getAbsolutePath());
        /*
        <n:form
        <%@ taglib
        <%@ page
        <%
        <%=
        <n:set1
        */

        // エラーが発生しないファイル
        String jsp1 = temporaryFolder.newFile("target1.jsp").getAbsolutePath();
        createJspFile(jsp1, "set1");
        // エラーが発生するファイル
        String jsp2 = temporaryFolder.newFile("target2.jsp").getAbsolutePath();
        createJspFile(jsp2, "set2");

        // テスト実行
        SanitizingChecker checker = new SanitizingChecker(configFile.getAbsolutePath(), CHARSET_UTF8, Collections.<String>emptyList(), null);
        Map<String, List<String>> checkResult = checker.checkSanitizing(temporaryFolder.getRoot().getAbsolutePath());

        assertThat("エラーが発生するので、JSPファイルに対応するエラーリストが格納されている。", checkResult.containsKey(jsp1), is(false));
        assertThat("エラーが発生するので、JSPファイルに対応するエラーリストが格納されている。", checkResult.containsKey(jsp2), is(true));
    }

    /**
     * 全てが許可されない場合のテスト
     */
    @Test
    public void testAllDisallow() throws Exception {
        // 全ての行がコメントアウトされた設定ファイルを出力
        Hereis.file(configFile.getAbsolutePath());
        /*
        --<n:form
        --<%@ taglib
        --<%@ page
        */

        SanitizingChecker checker = new SanitizingChecker(configFile.getAbsolutePath(), CHARSET_UTF8, Collections.<String>emptyList(), null);

        String path = temporaryFolder.newFile("test.jsp").getAbsolutePath();
        Hereis.file(path);
        /*
        <%@ taglib prefix="n" uri="http://tis.co.jp/nablarch" %>
         <% String hoge; %>
          <!-- コメント -->
         <%! String fuga; %>
        <%-- JSPコメント --%>
        <p><%= new java.util.Date() %></p>
        <ul>
            <li>${100}</li>
            <li><n:write value="<%= userName %>" /></li>
            <li><n:write>${userName}</n:write></li>
        </ul>
        function hoge() {
          var fuga = ${el};
          var hogehoge = '<%= jspExpression %>';
        }
        */

        Map<String, List<String>> result1 = checker.checkSanitizing(path);
        assertThat(result1.containsKey(path), is(true));
        assertThat(result1.get(path).size(), is(13));
        assertThat(result1.get(path).get(0), is(allOf(containsString("<%@ taglib"), containsString("line=1"), containsString("column=1"))));
        assertThat(result1.get(path).get(1), is(allOf(containsString("<% xxx %>"), containsString("line=2"), containsString("column=2"))));
        assertThat(result1.get(path).get(2), is(allOf(containsString("<!-- xxx -->"), containsString("line=3"), containsString("column=3"))));
        assertThat(result1.get(path).get(3), is(allOf(containsString("<%! xxx %>"), containsString("line=4"), containsString("column=2"))));
        assertThat(result1.get(path).get(4), is(allOf(containsString("<%-- xxx --%>"), containsString("line=5"), containsString("column=1"))));
        assertThat(result1.get(path).get(5), is(allOf(containsString("<%= xxx %>"), containsString("line=6"), containsString("column=4"))));
        assertThat(result1.get(path).get(6), is(allOf(containsString("${ xxx }"), containsString("line=8"), containsString("column=9"))));
        assertThat(result1.get(path).get(7), is(allOf(containsString("<n:write>"), containsString("line=9"), containsString("column=9"))));
        assertThat(result1.get(path).get(8), is(allOf(containsString("<%= xxx %>"), containsString("line=9"), containsString("column=25"))));
        assertThat(result1.get(path).get(9), is(allOf(containsString("<n:write>"), containsString("line=10"), containsString("column=9"))));
        assertThat(result1.get(path).get(10), is(allOf(containsString("${ xxx }"), containsString("line=10"), containsString("column=18"))));
        assertThat(result1.get(path).get(11), is(allOf(containsString("${ xxx }"), containsString("line=13"), containsString("column=14"))));
        assertThat(result1.get(path).get(12), is(allOf(containsString("<%= xxx %>"), containsString("line=14"), containsString("column=19"))));
    }

    /**
     * タグリブの属性でのEL式は使用してもエラーとならないこと。
     */
    @Test
    public void testELInTagAttribute() throws Exception {
        // EL式は許可しない
        Hereis.file(configFile.getAbsolutePath());
        /*
        <n:
        */

        SanitizingChecker checker = new SanitizingChecker(configFile.getAbsolutePath(), CHARSET_UTF8, Collections.<String>emptyList(), null);
        String path = temporaryFolder.newFile("test.jsp").getAbsolutePath();
        Hereis.file(path);
        /*
        <n:write name="${user.name}"/>
        */

        Map<String, List<String>> result = checker.checkSanitizing(path);
        assertThat("属性で使用しているEL式はエラーとならない。", result.containsKey(path), is(false));

    }

    /**
     * 様々なネストしたタグのテスト(全て許可しない場合)。
     */
    @Test
    public void testNestTagAllDisallow() throws Exception {
        // 全ての行がコメントアウトされた設定ファイルを出力
        Hereis.file(configFile.getAbsolutePath());
        /*
        --<n:form
        */

        SanitizingChecker checker = new SanitizingChecker(configFile.getAbsolutePath(), CHARSET_UTF8, Collections.<String>emptyList(), null);
        String path = temporaryFolder.newFile("test.jsp").getAbsolutePath();

        createOneLineJsp(path, "<!-- コメント ${100 + 100} -->");
        Map<String, List<String>> ret = checker.checkSanitizing(path);
        List<String> errors = ret.get(path);
        assertThat(errors.size(), is(2));
        assertThat(errors.get(0), is(allOf(containsString("<!-- xxx -->"), containsString("line=1"), containsString("column=1"))));
        assertThat(errors.get(1), is(allOf(containsString("${ xxx }"), containsString("line=1"), containsString("column=11"))));

        createOneLineJsp(path, "<n:write value='<%= hogehoge %>:<%= fugafuga' %>>${user.name}:${user.kanaName}</n:write>");
        ret = checker.checkSanitizing(path);
        errors = ret.get(path);
        assertThat(errors.size(), is(5));
        assertThat(errors.get(0), is(allOf(containsString("<n:write>"), containsString("line=1"), containsString("column=1"))));
        assertThat(errors.get(1), is(allOf(containsString("<%= xxx %>"), containsString("line=1"), containsString("column=17"))));
        assertThat(errors.get(2), is(allOf(containsString("<%= xxx %>"), containsString("line=1"), containsString("column=33"))));
        assertThat(errors.get(3), is(allOf(containsString("${ xxx }"), containsString("line=1"), containsString("column=50"))));
        assertThat(errors.get(4), is(allOf(containsString("${ xxx }"), containsString("line=1"), containsString("column=63"))));


        createOneLineJsp(path, "<tr id=\"${row.id}\">");
        ret = checker.checkSanitizing(path);
        errors = ret.get(path);
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), is(allOf(containsString("${ xxx }"), containsString("line=1"), containsString("column=9"))));
    }

    /**
     * 除外ファイル(ディレクトリ)を設定した場合のテスト。
     *
     * 除外対象のディレクトリ配下のJSPはチェックされないこと。
     */
    @Test
    public void testExcludePattern() throws Exception {
        // 設定ファイルを追加
        Hereis.file(configFile.getAbsolutePath());
        /*
        -- 許可対象のタグは無し
        */

        // ディレクトリの作成
        File includeDir = temporaryFolder.newFolder("includeDir");
        File excludeDir = temporaryFolder.newFolder("excludeDir");

        File includeJsp = new File(includeDir, "include.jsp");
        createOneLineJsp(includeJsp.getAbsolutePath(), "<% String include = null %>");
        File excludeJsp = new File(includeDir, "exclude.jsp");
        createOneLineJsp(excludeJsp.getAbsolutePath(), "<% String exclude = null %>");
        File excludeDirFile = new File(excludeDir, "include.jsp");
        createOneLineJsp(excludeDirFile.getAbsolutePath(), "<% String excludeDir = null %>");

        List<Pattern> excludePatterns = new ArrayList<Pattern>();
        excludePatterns.add(Pattern.compile("excludeDir"));
        excludePatterns.add(Pattern.compile("includeDir.exclude\\.jsp"));
        SanitizingChecker checker = new SanitizingChecker(
                configFile.getAbsolutePath(),
                CHARSET_UTF8,
                Collections.<String>emptyList(),
                excludePatterns
        );

        Map<String, List<String>> result = checker.checkSanitizing(temporaryFolder.getRoot().getAbsolutePath());
        assertThat("チェック対象のJSPファイルは1ファイルだけなので、チェック結果NGのファイルは1となる", result.size(), is(1));
        assertThat("チェックエラーとなったファイルは、チェック対象のJSPであること",
                result.containsKey(includeJsp.getAbsolutePath()), is(true));

        checker = new SanitizingChecker(
                configFile.getAbsolutePath(),
                CHARSET_UTF8,
                Collections.<String>emptyList(),
                excludePatterns
        );
        result = checker.checkSanitizing(includeJsp.getAbsolutePath());
        assertThat("チェック対象のJSPファイルは1ファイルだけなので、チェック結果NGのファイルは1となる", result.size(), is(1));
        assertThat("チェックエラーとなったファイルは、チェック対象のJSPであること",
                result.containsKey(includeJsp.getAbsolutePath()), is(true));

        checker = new SanitizingChecker(
                configFile.getAbsolutePath(),
                CHARSET_UTF8,
                Collections.<String>emptyList(),
                excludePatterns
        );
        result = checker.checkSanitizing(excludeJsp.getAbsolutePath());
        assertThat("チェック対象のファイルは存在しないので結果は0となる", result.size(), is(0));

    }

    /**
     * 指定された値を持つJSPファイルを生成する。
     *
     * @param jspFilePath 作成するJSPファイルのパス
     * @param line 行の内容
     */
    private static void createOneLineJsp(String jspFilePath, String line) {
        Hereis.file(jspFilePath, line);
        /*
        ${line}
        */
    }

    /**
     * テスト用のJSPファイルを作成する。
     *
     * @param jspFilePath 作成するJSPファイルのパス
     * @param replaceStr 生成するJSPの置き換え文字
     */
    private static void createJspFile(String jspFilePath, String replaceStr) {
        String devTool = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n"
                + "<!-- <%/* --> <script src=\"js/devtool.js\"></script><meta charset=\"utf-8\"><body> <!-- */%> -->";

        Hereis.file(jspFilePath, replaceStr, devTool);
        /*
        ${devTool}
        <%@ taglib prefix="n" uri="http://tis.co.jp/nablarch" %>
        <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
        <%
            String nowTime = new java.util.Date().toString();
        %>
        <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
        <html>
        <head><title>時刻を出力する例</title></head>
        <body>
        <n:form>
          <p>-- 時刻を出力する例 --</p>
          <p>現在の時刻は <strong><%= nowTime %></strong> です。</p>
          <n:${replaceStr} var="loginId"      name="11AC_W11AC01.loginId"/>
        </n:form>
        </body>
        <!--[if lte IE 8]>
        <link/>
        <![endif]-->
        <!--[if gte IE 9]>
        <link/>
        <![endif]-->
        <!--[if !IE]>-->
        <link href="non-ie.css" rel="stylesheet">
        <!--<![endif]-->
        </html>

        <%-- suppress jsp check:ここは対象外--%>
        <% String hoge = "hogefuga"; %>
        */
    }
}

