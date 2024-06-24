package nablarch.test.tool.sanitizingcheck;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import nablarch.test.tool.sanitizingcheck.tag.Directive;
import nablarch.test.tool.sanitizingcheck.tag.JspCore;
import nablarch.test.tool.sanitizingcheck.tag.Tag;
import nablarch.test.tool.sanitizingcheck.tag.TagType;

import org.junit.Test;

/**
 * {@link JspParser}のテストクラス。
 *
 * @author hisaaki sioiri
 */
public class JspParserTest {

    /**
     * ディレクティブタグのパーステスト。
     * <p>
     * ディレクティブタグの内容が正しく解析できることを確認する。
     */
    @Test
    public void directiveTag() throws Exception {
        JspParser parser = new JspParser("src/test/java/nablarch/test/tool/sanitizingcheck/data/directive.jsp", StandardCharsets.UTF_8);
        List<? extends Tag> tags = parser.parse();

        assertThat(tags.size(), is(5));

        // 行内にはこのディレクティブのみが存在する。
        // ひとつめ
        {
            Tag tag = tags.get(0);
            assertThat(tag.getType(), is(TagType.DIRECTIVE));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getCloseTagPosition(), is(32));
            assertThat(tag.getLineNo(), is(1));
            assertThat(tag.getName(), is("<%@ page"));
            assertThat(tag.isSuppressJspCheck(), is(false));
            assertThat(tag.toString(), is("JSP Directive: <%@ page %> (at line=1 column=1)"));

            Directive directive = (Directive) tag;
            assertThat(directive.getDirectiveName(), is("page"));

            List<Tag.TagAttribute> attributes = tag.getAttributes();
            assertThat(attributes.size(), is(1));
            assertThat(attributes.get(0).getName(), is("import"));
            assertThat(attributes.get(0).getValue(), is("\"java.util.*\""));
        }

        // 行内に複数のディレクティブが存在する。
        // ふたつめ
        {
            Tag tag = tags.get(1);
            assertThat(tag.getType(), is(TagType.DIRECTIVE));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getCloseTagPosition(), is(48));
            assertThat(tag.getLineNo(), is(2));
            assertThat(tag.isSuppressJspCheck(), is(false));
            Directive directive = (Directive) tag;
            assertThat(directive.getDirectiveName(), is("taglib"));

            List<Tag.TagAttribute> attributes = tag.getAttributes();
            assertThat(attributes.size(), is(2));
            assertThat(attributes.get(0).getName(), is("prefix"));
            assertThat(attributes.get(0).getValue(), is("\"c\""));
            assertThat(attributes.get(1).getName(), is("uri"));
            assertThat(attributes.get(1).getValue(), is("\"jakarta.tags.core\""));
        }
        // みっつめ
        {
            Tag tag = tags.get(2);
            assertThat(tag.getType(), is(TagType.DIRECTIVE));
            assertThat(tag.getPosition(), is(50));
            assertThat(tag.getCloseTagPosition(), is(35));
            assertThat(tag.getLineNo(), is(2));
            assertThat(tag.isSuppressJspCheck(), is(false));
            Directive directive = (Directive) tag;
            assertThat(directive.getDirectiveName(), is("taglib"));

            List<Tag.TagAttribute> attributes = tag.getAttributes();
            assertThat(attributes.get(0).getName(), is("prefix"));
            assertThat(attributes.get(0).getValue(), is("\"f\""));
            assertThat(attributes.get(1).getName(), is("uri"));
            assertThat(attributes.get(1).getValue(), is("\"jakarta.tags.functions\""));
        }

        // 複数行にまたがって記述されているディレクティブ
        // よっつめ
        {
            Tag tag = tags.get(3);
            assertThat(tag.getType(), is(TagType.DIRECTIVE));
            assertThat(tag.getPosition(), is(2));
            assertThat(tag.getCloseTagPosition(), is(23));
            assertThat(tag.getLineNo(), is(5));
            assertThat(tag.isSuppressJspCheck(), is(true));
            Directive directive = (Directive) tag;
            assertThat(directive.getDirectiveName(), is("page"));

            List<Tag.TagAttribute> attributes = tag.getAttributes();
            assertThat(attributes.get(0).getName(), is("contentType"));
            assertThat(attributes.get(0).getValue(), is("\"text/html; charset=utf-8\""));
            assertThat(attributes.get(1).getName(), is("pageEncoding"));
            assertThat(attributes.get(1).getValue(), is("\"utf-8\""));
            assertThat(attributes.get(2).getName(), is("buffer"));
            assertThat(attributes.get(2).getValue(), is("\"16kb\""));
            assertThat(attributes.get(3).getName(), is("autoFlush"));
            assertThat(attributes.get(3).getValue(), is("\"true\""));
        }
    }

    /**
     * HTMLコメントのパーステスト。
     * <p>
     * HTMLコメントの内容が正しくパースできていること。
     */
    @Test
    public void htmlComment() throws Exception {
        JspParser parser = new JspParser("src/test/java/nablarch/test/tool/sanitizingcheck/data/htmlComment.jsp", StandardCharsets.UTF_8);
        List<? extends Tag> tags = parser.parse();

        assertThat(tags.size(), is(8));

        // コメントだけの行
        {
            Tag tag = tags.get(0);
            assertThat(tag.getName(), is("<!--"));
            assertThat(tag.getType(), is(TagType.HTML_COMMENT));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getCloseTagPosition(), is(1));
            assertThat(tag.getLineNo(), is(1));
            assertThat(tag.toString(), is("HTML Comment: <!-- xxx --> (at line=1 column=1)"));
            assertThat(tag.isSuppressJspCheck(), is(false));
        }

        // いち行に複数のコメントのひとつめ
        {
            Tag tag = tags.get(1);
            assertThat(tag.getName(), is("<!--"));
            assertThat(tag.getType(), is(TagType.HTML_COMMENT));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getCloseTagPosition(), is(1));
            assertThat(tag.getLineNo(), is(2));
            assertThat(tag.isSuppressJspCheck(), is(false));
        }

        // いち行に複数のコメントのふたつめ
        {
            Tag tag = tags.get(2);
            assertThat(tag.getName(), is("<!--"));
            assertThat(tag.getType(), is(TagType.HTML_COMMENT));
            assertThat(tag.getPosition(), is(14));
            assertThat(tag.getCloseTagPosition(), is(14));
            assertThat(tag.getLineNo(), is(2));
            assertThat(tag.isSuppressJspCheck(), is(false));
        }

        // 複数行に跨るコメント
        {
            Tag tag = tags.get(3);
            assertThat(tag.getName(), is("<!--"));
            assertThat(tag.getType(), is(TagType.HTML_COMMENT));
            assertThat(tag.getPosition(), is(3));
            assertThat(tag.getCloseTagPosition(), is(3));
            assertThat(tag.getLineNo(), is(3));
            assertThat(tag.isSuppressJspCheck(), is(false));
        }

        // チェック対象外コメントその１
        {
            Tag tag = tags.get(4);
            assertThat(tag.getName(), is("<!--"));
            assertThat(tag.getType(), is(TagType.HTML_COMMENT));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getCloseTagPosition(), is(1));
            assertThat(tag.getLineNo(), is(8));
            assertThat(tag.isSuppressJspCheck(), is(true));
        }

        // チェック対象外コメントその２
        {
            Tag tag = tags.get(5);
            assertThat(tag.getName(), is("<!--"));
            assertThat(tag.getType(), is(TagType.HTML_COMMENT));
            assertThat(tag.getPosition(), is(14));
            assertThat(tag.getCloseTagPosition(), is(14));
            assertThat(tag.getLineNo(), is(8));
            assertThat(tag.isSuppressJspCheck(), is(true));
        }

        // チェック対象外の次の行でもチェック対象になること
        {
            Tag tag = tags.get(6);
            assertThat(tag.getName(), is("<!--"));
            assertThat(tag.getType(), is(TagType.HTML_COMMENT));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getCloseTagPosition(), is(1));
            assertThat(tag.getLineNo(), is(9));
            assertThat(tag.isSuppressJspCheck(), is(false));
        }


        // 閉じられていないファイル末尾のコメント
        {
            Tag tag = tags.get(7);
            assertThat(tag.getName(), is("<!--"));
            assertThat(tag.getType(), is(TagType.HTML_COMMENT));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getCloseTagPosition(), is(1));
            assertThat(tag.getLineNo(), is(11));
            assertThat(tag.isSuppressJspCheck(), is(false));
        }
    }

    /**
     * HTMLコメント内に各種タグやEL式が存在しているケース
     */
    @Test
    public void htmlCommentCode() {
        JspParser parser = new JspParser("src/test/java/nablarch/test/tool/sanitizingcheck/data/htmlCommentTag.jsp", StandardCharsets.UTF_8);
        List<? extends Tag> tags = parser.parse();

        assertThat(tags.size(), is(2));
    }

    /**
     * タグリブのテスト。
     */
    @Test
    public void tagLib() {
        JspParser parser = new JspParser("src/test/java/nablarch/test/tool/sanitizingcheck/data/taglib.jsp", Charset.forName("Windows-31j"));
        List<? extends Tag> tags = parser.parse();

        assertThat(tags.size(), is(7));

        // ディレクティブ部分の確認
        // ディレクティブの確認
        {
            assertThat(tags.get(0).getType(), is(TagType.DIRECTIVE));
            assertThat(tags.get(1).getType(), is(TagType.DIRECTIVE));
            assertThat(tags.get(2).getType(), is(TagType.DIRECTIVE));
        }

        // タグリブだけが存在している行
        {
            Tag tag = tags.get(3);
            assertThat(tag.getName(), is("<c:set"));
            assertThat(tag.getLineNo(), is(5));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getCloseTagPosition(), is(25));
            assertThat(tag.getType(), is(TagType.TAGLIB));
            assertThat(tag.isSuppressJspCheck(), is(false));
            List<Tag.TagAttribute> attributes = tag.getAttributes();
            assertThat(attributes.size(), is(1));
            assertThat(attributes.get(0).getName(), is("property"));
            assertThat(attributes.get(0).getValue(), is("\"fuga\""));
            assertThat(attributes.get(0).getLineNo(), is(5));
            assertThat(attributes.get(0).getPosition(), is(17));
            assertThat(tag.toString(), is("Custom Tag: <c:set> (at line=5 column=1)"));
        }

        // 要素のあるタグ
        {
            Tag tag = tags.get(4);
            assertThat(tag.getName(), is("<n:form"));
            assertThat(tag.getLineNo(), is(6));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getCloseTagPosition(), is(8));
            assertThat(tag.getType(), is(TagType.TAGLIB));
            assertThat(tag.isSuppressJspCheck(), is(false));
            List<Tag.TagAttribute> attributes = tag.getAttributes();
            assertThat(attributes.size(), is(0));
        }

        // 子要素でさらに要素を持つタグで属性も持つ
        {
            Tag tag = tags.get(5);
            assertThat(tag.getName(), is("<c:if"));
            assertThat(tag.getLineNo(), is(7));
            assertThat(tag.getPosition(), is(3));
            assertThat(tag.getCloseTagPosition(), is(46));
            assertThat(tag.getType(), is(TagType.TAGLIB));
            assertThat(tag.isSuppressJspCheck(), is(false));

            List<Tag.TagAttribute> attributes = tag.getAttributes();
            assertThat(attributes.size(), is(3));
            assertThat(attributes.get(0).getName(), is("var"));
            assertThat(attributes.get(0).getValue(), is("\"fuga\""));
            assertThat(attributes.get(0).getLineNo(), is(7));
            assertThat(attributes.get(0).getPosition(), is(13));
            assertThat(attributes.get(1).getName(), is("test"));
            assertThat(attributes.get(1).getValue(), is("\"hoge\""));
            assertThat(attributes.get(1).getLineNo(), is(7));
            assertThat(attributes.get(1).getPosition(), is(25));
            assertThat(attributes.get(2).getName(), is("scope"));
            assertThat(attributes.get(2).getValue(), is("\"page\""));
            assertThat(attributes.get(2).getLineNo(), is(7));
            assertThat(attributes.get(2).getPosition(), is(40));
        }

        // 子要素で属性だけをもつタグ
        {
            Tag tag = tags.get(6);
            assertThat(tag.getName(), is("<n:checkbox"));
            assertThat(tag.getLineNo(), is(11));
            assertThat(tag.getPosition(), is(5));
            assertThat(tag.getCloseTagPosition(), is(29));
            assertThat(tag.getType(), is(TagType.TAGLIB));
            assertThat(tag.isSuppressJspCheck(), is(true));

            List<Tag.TagAttribute> attributes = tag.getAttributes();
            assertThat(attributes.size(), is(5));
            assertThat(attributes.get(0).getName(), is("name"));
            assertThat(attributes.get(0).getValue(), is("\"checkbox\""));
            assertThat(attributes.get(0).getLineNo(), is(12));
            assertThat(attributes.get(0).getPosition(), is(9));
            assertThat(attributes.get(1).getName(), is("accesskey"));
            assertThat(attributes.get(1).getValue(), is("\"1\""));
            assertThat(attributes.get(1).getLineNo(), is(13));
            assertThat(attributes.get(1).getPosition(), is(10));
            assertThat(attributes.get(2).getName(), is("autofocus"));
            assertThat(attributes.get(2).getValue(), is("\"2\\\"\\\\\""));
            assertThat(attributes.get(2).getLineNo(), is(13));
            assertThat(attributes.get(2).getPosition(), is(24));
            assertThat(attributes.get(3).getName(), is("cssClass"));
            assertThat(attributes.get(3).getValue(), is("'cssClassName        cssClassName2        '"));
            assertThat(attributes.get(3).getLineNo(), is(14));
            assertThat(attributes.get(3).getPosition(), is(18));
            assertThat(attributes.get(4).getName(), is("disabled"));
            assertThat(attributes.get(4).getValue(), is("'<%= disabled %>'"));
            assertThat(attributes.get(4).getLineNo(), is(16));
            assertThat(attributes.get(4).getPosition(), is(20));
        }
    }

    /**
     * JSPのコアタグ
     */
    @Test
    public void coreTag() {

        JspParser parser = new JspParser("src/test/java/nablarch/test/tool/sanitizingcheck/data/JspCore.jsp", Charset.forName("Windows-31j"));
        List<? extends Tag> tags = parser.parse();

        assertThat(tags.size(), is(8));

        // コメント
        {
            Tag tag = tags.get(0);

            assertThat(tag.getName(), is("<%--"));
            assertThat(tag.getLineNo(), is(1));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getType(), is(TagType.JSP_CORE));
            assertThat(tag.getCloseTagPosition(), is(11));
            assertThat(((JspCore) tag).getCoreTagType(), is(JspCore.CoreTagType.COMMENT));
            assertThat(tag.toString(), is("JSP Comment: <%-- xxx --%> (at line=1 column=1)"));
            assertThat(tag.isSuppressJspCheck(), is(false));
        }

        // 宣言
        {
            Tag tag = tags.get(1);
            assertThat(tag.getName(), is("<%!"));
            assertThat(tag.getLineNo(), is(2));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getType(), is(TagType.JSP_CORE));
            assertThat(tag.getCloseTagPosition(), is(1));
            assertThat(((JspCore) tag).getCoreTagType(), is(JspCore.CoreTagType.DEFINITION));
            assertThat(tag.toString(), is("JSP Declaration: <%! xxx %> (at line=2 column=1)"));
            assertThat(tag.isSuppressJspCheck(), is(false));
        }

        // スクリプトレット
        {
            Tag tag = tags.get(2);
            assertThat(tag.getName(), is("<%"));
            assertThat(tag.getLineNo(), is(9));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getType(), is(TagType.JSP_CORE));
            assertThat(tag.getCloseTagPosition(), is(1));
            assertThat(((JspCore) tag).getCoreTagType(), is(JspCore.CoreTagType.SCRIPTLET));
            assertThat(tag.toString(), is("JSP Scriptlet: <% xxx %> (at line=9 column=1)"));
            assertThat(tag.isSuppressJspCheck(), is(false));
        }

        // JSP式
        {
            Tag tag = tags.get(3);
            assertThat(tag.getName(), is("<%="));
            assertThat(tag.getLineNo(), is(17));
            assertThat(tag.getPosition(), is(4));
            assertThat(tag.getType(), is(TagType.JSP_CORE));
            assertThat(tag.getCloseTagPosition(), is(14));
            assertThat(((JspCore) tag).getCoreTagType(), is(JspCore.CoreTagType.EXPRESSION));
            assertThat(tag.toString(), is("JSP Expression: <%= xxx %> (at line=17 column=4)"));
            assertThat(tag.isSuppressJspCheck(), is(false));
        }

        // チェック対象外
        {
           Tag tag = tags.get(4);
           assertThat(tag.getName(), is("<%="));
           assertThat(tag.getLineNo(), is(20));
           assertThat(tag.getPosition(), is(7));
           assertThat(tag.getType(), is(TagType.JSP_CORE));
           assertThat(tag.getCloseTagPosition(), is(16));
           assertThat(((JspCore) tag).getCoreTagType(), is(JspCore.CoreTagType.EXPRESSION));
           assertThat(tag.isSuppressJspCheck(), is(true));
       }

        // 不完全なチェック対象外コメントはチェック対象となる
       {
            Tag tag = tags.get(5);
            assertThat(tag.getName(), is("<%--"));
            assertThat(tag.getLineNo(), is(22));
            assertThat(tag.getPosition(), is(3));
            assertThat(tag.getType(), is(TagType.JSP_CORE));
            assertThat(tag.getCloseTagPosition(), is(3));
            assertThat(((JspCore) tag).getCoreTagType(), is(JspCore.CoreTagType.COMMENT));
            assertThat(tag.isSuppressJspCheck(), is(false));
        }

        // 不完全なチェック対象外コメントはチェック対象となるその２
        {
            Tag tag = tags.get(6);
            assertThat(tag.getName(), is("<%--"));
            assertThat(tag.getLineNo(), is(24));
            assertThat(tag.getPosition(), is(7));
            assertThat(tag.getType(), is(TagType.JSP_CORE));
            assertThat(tag.getCloseTagPosition(), is(7));
            assertThat(((JspCore) tag).getCoreTagType(), is(JspCore.CoreTagType.COMMENT));
            assertThat(tag.isSuppressJspCheck(), is(false));
        }

        // 不完全なチェック対象外コメントはチェック対象となるその３
        {
            Tag tag = tags.get(7);
            assertThat(tag.getName(), is("<%--"));
            assertThat(tag.getLineNo(), is(29));
            assertThat(tag.getPosition(), is(5));
            assertThat(tag.getType(), is(TagType.JSP_CORE));
            assertThat(tag.getCloseTagPosition(), is(68));
            assertThat(((JspCore) tag).getCoreTagType(), is(JspCore.CoreTagType.COMMENT));
            assertThat(tag.isSuppressJspCheck(), is(false));
        }
    }

    /**
     * EL式の場合
     */
    @Test
    public void el() {

        JspParser parser = new JspParser("src/test/java/nablarch/test/tool/sanitizingcheck/data/el.jsp", StandardCharsets.UTF_8);
        List<? extends Tag> tags = parser.parse();

        assertThat(tags.size(), is(7));

        // EL式以外
        {
            assertThat(tags.get(0).getType(), is(TagType.DIRECTIVE));
            assertThat(tags.get(2).getType(), is(TagType.TAGLIB));
            assertThat(tags.get(3).getType(), is(TagType.TAGLIB));
        }

        // EL式だけの行
        {
            Tag tag = tags.get(1);
            assertThat(tag.getName(), is("${"));
            assertThat(tag.getLineNo(), is(3));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getCloseTagPosition(), is(12));
            assertThat(tag.getType(), is(TagType.EL));
            assertThat(tag.toString(), is("JSP EL Element: ${ xxx } (at line=3 column=1)"));
        }

        // 複数行に跨るEL式
        {
            Tag tag = tags.get(4);
            assertThat(tag.getName(), is("${"));
            assertThat(tag.getLineNo(), is(8));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getCloseTagPosition(), is(1));
            assertThat(tag.getType(), is(TagType.EL));
        }

        // リテラルの確認
        {
            Tag tag = tags.get(5);
            assertThat(tag.getName(), is("${"));
            assertThat(tag.getLineNo(), is(14));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getCloseTagPosition(), is(13));
            assertThat(tag.getType(), is(TagType.EL));

            tag = tags.get(6);
            assertThat(tag.getName(), is("${"));
            assertThat(tag.getLineNo(), is(15));
            assertThat(tag.getPosition(), is(1));
            assertThat(tag.getCloseTagPosition(), is(31));
            assertThat(tag.getType(), is(TagType.EL));
        }
    }

    /**
     * 複数の種類のタグが混在しているケース
     */
    @Test
    public void multiTag() {
        JspParser parser = new JspParser("src/test/java/nablarch/test/tool/sanitizingcheck/data/MultiTag.jsp", StandardCharsets.UTF_8);
        List<? extends Tag> tags = parser.parse();

        assertThat(tags.size(), is(18));

        assertThat(tags.get(0).getType(), is(TagType.HTML_COMMENT));

        assertThat(tags.get(1).getType(), is(TagType.JSP_CORE));
        assertThat(((JspCore) tags.get(1)).getCoreTagType(), is(JspCore.CoreTagType.COMMENT));

        assertThat(tags.get(2).getType(), is(TagType.DIRECTIVE));
        assertThat(tags.get(3).getType(), is(TagType.TAGLIB));
        assertThat(tags.get(4).getType(), is(TagType.TAGLIB));
        assertThat(tags.get(5).getType(), is(TagType.TAGLIB));
        assertThat(tags.get(6).getType(), is(TagType.TAGLIB));
        assertThat(tags.get(7).getType(), is(TagType.JSP_CORE));
        assertThat(((JspCore) tags.get(7)).getCoreTagType(), is(JspCore.CoreTagType.DEFINITION));
        assertThat(tags.get(8).getType(), is(TagType.JSP_CORE));
        assertThat(((JspCore) tags.get(8)).getCoreTagType(), is(JspCore.CoreTagType.DEFINITION));
        assertThat(tags.get(9).getType(), is(TagType.JSP_CORE));
        assertThat(((JspCore) tags.get(9)).getCoreTagType(), is(JspCore.CoreTagType.SCRIPTLET));
        assertThat(tags.get(10).getType(), is(TagType.TAGLIB));
        assertThat(tags.get(11).getType(), is(TagType.EL));

        assertThat(tags.get(12).getType(), is(TagType.DIRECTIVE));
        assertThat(tags.get(13).getType(), is(TagType.TAGLIB));
        assertThat(tags.get(14).getType(), is(TagType.HTML_COMMENT));
        assertThat(tags.get(15).getType(), is(TagType.TAGLIB));
        assertThat(tags.get(16).getType(), is(TagType.HTML_COMMENT));
        assertThat(tags.get(17).getType(), is(TagType.TAGLIB));
    }

    /**
     * JSPコンパイルが通らないJSPの場合
     */
    @Test
    public void invalidJsp() {
        JspParser parser = new JspParser("src/test/java/nablarch/test/tool/sanitizingcheck/data/invalid.jsp", StandardCharsets.UTF_8);
        List<? extends Tag> tags = parser.parse();
        assertThat(tags.size(), is(3));

        assertThat(tags.get(0).getName(), is("<n:write"));
        assertThat(tags.get(1).getName(), is("<n:form"));
        assertThat(tags.get(2).getName(), is("<c:if"));

    }
}

