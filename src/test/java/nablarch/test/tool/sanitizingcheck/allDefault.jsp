Comment
        <%-- comment --%>
        <!-- comment -->

Declaration
    <%! int i = 0; %>
    <jsp:declaration>   
       int a, b, c;
    </jsp:declaration>

Expression
    <%= map.size() %>
    <jsp:expression>
        numguess.getHint()
    </jsp:expression>


Scriptlet
    <%
       String name = null;
       if (request.getParameter("name") == null) {
    %>
    <jsp:scriptlet> 
        code fragment   
    </jsp:scriptlet>

EL
    <a:tag value="${x+y}" />    
    <a:tag value="${first} ${last}" />  
    <a:tag>${x+y}</a:tag>
    ${1 > (4/2)}
    ${4.0 >= 3}
    ${100.0 == 100}
    ${(10*10) ne 100}
    ${'a' < 'b'}
    ${'hip' gt 'hit'}
    ${4 > 3}
    ${1.2E4 + 1.4}
    ${3 div 4}
    ${10 mod 4}
    ${!empty param.Add}
    ${pageContext.request.contextPath}
    ${sessionScope.cart.numberOfItems}
    ${param['mycom.productId']}
    ${header["host"]}
    ${departments[deptName]}
    ${requestScope['jakarta.servlet.forward.servlet_path']}



Attribute Directive
    <%@ attribute name="shipping" required="true" %>
    <jsp:directive.attribute name="shipping" required="true" />


Include Directive
    <%@ include file="date.jsp" %>
    <jsp:directive.include file="relativeURL" />



Page Directive
    <%@ page import="java.util.*, java.lang.*" %>
    <jsp:directive.page errorPage="error.jsp" />



Tag Directive
    <%@ tag dynamic-attributes="colorMap"%> 
    <jsp:directive.tag dynamic-attributes="colorMap" />



Taglib Directive
    <%@ taglib uri="http://www.jspcentral.com/tags" prefix="public" %>



Variable Directive
    <%@ variable name-given="x" scope="AT_END" %>
    <jsp:directive.variable name-given="x" scope="AT_END" />



JspAttribute
    <jsp:attribute name="value" >
        <fmt:message key="TitleBookCatalog"/>
    </jsp:attribute>



JspBody
    <jsp:attribute name="lang">hoge</jsp:attribute>
    <jsp:body>a</jsp:body>



JspElement
    <jsp:element  name="${content.headerName}" />
    <jsp:element  name="${content.headerName}" > **** </jsp:element>
    <jsp:element name="elementName"> 
        <jsp:attribute name="attributeName" />
        <jsp:body> *** </jsp:body>
    </jsp:element name="elementName">
    <jsp:element name="elementName"> 
        <jsp:attribute name="attributeName"> **** </jsp:attribute>
        <jsp:body> *** </jsp:body>
    </jsp:element name="elementName">
    <jsp:element name="${content.headerName}"
        xmlns:jsp="http://java.sun.com/JSP/Page">
        <jsp:attribute name="lang"> ${content.lang} </jsp:attribute>
        <jsp:body>${content.body}</jsp:body>    
    </jsp:element>



JspDobody
    <jsp:doBody var="scopedAttributeName" />
    <jsp:doBody  />


JspForward
    <jsp:forward page="http://test.com/" />
    <jsp:forward page="${ Expression}" />
    <jsp:forward page="<%= expression %>" />
    
    <jsp:forward page="http://test.com/" >
        <jsp:param name="parameterName" value="paramValue"  />
        <jsp:param name="parameterName" value="${ Expression} />
        <jsp:param name="parameterName" value="<%= expression %>"  />
    </jsp:forward>
    <jsp:forward page="${ Expression }" >
        <jsp:param name="parameterName" value="paramValue" } />
        <jsp:param name="parameterName" value="${ Expression" } />
        <jsp:param name="parameterName" value="<%= expression %>"  />
    </jsp:forward>
    <jsp:forward page="<%= expression %>" >
        <jsp:param name="parameterName" value="paramValue" />
        <jsp:param name="parameterName" value="${ Expression}  />
        <jsp:param name="parameterName" value="<%= expression %>"  />
    </jsp:forward>
    

    <jsp:forward page="%= expression %" />
    
    <jsp:forward page="http://test.com/" >
        <jsp:param name="parameterName" value="paramValue"  />
        <jsp:param name="parameterName" value="${ Expression} />
        <jsp:param name="parameterName" value="%= expression %"  />
    </jsp:forward>
    <jsp:forward page="${ Expression }" >
        <jsp:param name="parameterName" value="paramValue"  />
        <jsp:param name="parameterName" value="${ Expression}  />
        <jsp:param name="parameterName" value="%= expression %"  />
    </jsp:forward>
    <jsp:forward page="%= expression %" >
        <jsp:param name="parameterName" value="paramValue"  />
        <jsp:param name="parameterName" value="${ Expression}  />
        <jsp:param name="parameterName" value="%= expression %"  />
    </jsp:forward>

JspGetProperty
<jsp:getProperty name="beanInstanceName" property="propertyName" />

JspInclude

    <jsp:include page="http://test.com/" />
    <jsp:include page="${ Expression}" />
    <jsp:include page="<%= expression %>" />
    
    <jsp:include page="http://test.com/" >
        <jsp:param name="parameterName" value="paramValue"  />
        <jsp:param name="parameterName" value="${ Expression} />
        <jsp:param name="parameterName" value="<%= expression %>"  />
    </jsp:include>
    <jsp:include page="${ Expression }" >
        <jsp:param name="parameterName" value="paramValue" } />
        <jsp:param name="parameterName" value="${ Expression" } />
        <jsp:param name="parameterName" value="<%= expression %>"  />
    </jsp:include>
    <jsp:include page="<%= expression %>" >
        <jsp:param name="parameterName" value="paramValue" />
        <jsp:param name="parameterName" value="${ Expression}  />
        <jsp:param name="parameterName" value="<%= expression %>"  />
    </jsp:include>


    <jsp:include page="%= expression %" />
    
    <jsp:include page="http://test.com/" >
        <jsp:param name="parameterName" value="paramValue"  />
        <jsp:param name="parameterName" value="${ Expression} />
        <jsp:param name="parameterName" value="%= expression %"  />
    </jsp:include>
    <jsp:include page="${ Expression }" >
        <jsp:param name="parameterName" value="paramValue"  />
        <jsp:param name="parameterName" value="${ Expression}  />
        <jsp:param name="parameterName" value="%= expression %"  />
    </jsp:include>
    <jsp:include page="%= expression %" >
        <jsp:param name="parameterName" value="paramValue"  />
        <jsp:param name="parameterName" value="${ Expression}  />
        <jsp:param name="parameterName" value="%= expression %"  />
    </jsp:include>

JspInvoke
    <jsp:invoke fragment="fragmentName" />
    <jsp:invoke fragment="fragmentName" var="scopedAttributeName" />

JspOutput
    <jsp:output doctype-root-element="rootElement" doctype-public="PubidLiteral" doctype-system="SystemLiteral" />

JspPlugin
    
    <jsp:plugin  height="displayPixels" width="displayPixels" >
        <jsp:params> 
            <jsp:param name="parameterName" value="parameterValue" />
            <jsp:param name="parameterName" value="${ Expression }" />
            <jsp:param name="parameterName" value="<%= expression %>" />
        </jsp:params>
        <jsp:fallback> text message if plugin download fails </jsp:fallback>
    </jsp:plugin>
    
    <jsp:plugin  height="<%= expression %>}" width="<%= expression %>" >
        <jsp:params> 
            <jsp:param name="parameterName" value="parameterValue" />
            <jsp:param name="parameterName" value="${ Expression }" />
            <jsp:param name="parameterName" value="<%= expression %>" />
        </jsp:params>
        <jsp:fallback> text message if plugin download fails </jsp:fallback>
    </jsp:plugin>
    
    <jsp:plugin  height="displayPixels" width="<%= expression %>" >
        <jsp:params> 
            <jsp:param name="parameterName" value="parameterValue" />
            <jsp:param name="parameterName" value="${ Expression }" />
            <jsp:param name="parameterName" value="<%= expression %>" />
        </jsp:params>
        <jsp:fallback> text message if plugin download fails </jsp:fallback>
    </jsp:plugin>
    

    <jsp:plugin  height="displayPixels" width="displayPixels" >
        <jsp:params> 
            <jsp:param name="parameterName" value="parameterValue" />
            <jsp:param name="parameterName" value="${ Expression }" />
            <jsp:param name="parameterName" value="%= expression %" />
        </jsp:params>
        <jsp:fallback> text message if plugin download fails </jsp:fallback>
    </jsp:plugin>
    
    <jsp:plugin  height="%= expression %}" width="%= expression %" >
        <jsp:params> 
            <jsp:param name="parameterName" value="parameterValue" />
            <jsp:param name="parameterName" value="${ Expression }" />
            <jsp:param name="parameterName" value="%= expression %" />
        </jsp:params>
        <jsp:fallback> text message if plugin download fails </jsp:fallback>
    </jsp:plugin>
    
    <jsp:plugin  height="displayPixels" width="%= expression %" >
        <jsp:params> 
            <jsp:param name="parameterName" value="parameterValue" />
            <jsp:param name="parameterName" value="${ Expression }" />
            <jsp:param name="parameterName" value="%= expression %" />
        </jsp:params>
        <jsp:fallback> text message if plugin download fails </jsp:fallback>
    </jsp:plugin>
        
JspRoot
    <jsp:root xmlns:jsp="http://java.sun.com/JSP/Page" xmlns:taglibPrefix="URI" version="1.2 | 2.0">
          JSP Page
    </jsp:root>

JspSetProperty
    
    <jsp:setProperty name="beanInstanceName" property="propertyName" value="stringLiteral" />
    <jsp:setProperty name="beanInstanceName" property="propertyName" value="${ Expression }" />
    <jsp:setProperty name="beanInstanceName" property="propertyName" value="<%= expression %>" />
    

    <jsp:setProperty name="beanInstanceName" property="propertyName" value=" %= expression %"  />
    

JspText
    <jsp:text>
        template data
    </jsp:text>

JspUseBean
    
    <jsp:useBean id="cart" scope="session" class="session.Carts" beanName="package.class"  />
    <jsp:useBean id="cart" scope="session" class="session.Carts" beanName="${ Expression }"  />
    <jsp:useBean id="cart" scope="session" class="session.Carts" beanName="<%= expression %>"  />
    <jsp:useBean id="checking" scope="session" class="bank.Checking" beanName="package.class" >
        test
    </jsp:useBean>
    <jsp:useBean id="checking" scope="session" class="bank.Checking" beanName="${ Expression }" >
        test
    </jsp:useBean>
    <jsp:useBean id="checking" scope="session" class="bank.Checking" beanName="<%= expression %>" >
        test
    </jsp:useBean>

    <jsp:useBean id="cart" scope="session" class="session.Carts" beanName="%= expression %"  />
    <jsp:useBean id="checking" scope="session" class="bank.Checking" beanName="%= expression %" >
        test
    </jsp:useBean>

CustomTag
    <n:confirm>test</n:confirm>
    <c:if>test</c:if>
<n:a>${hoge}</n:a>
