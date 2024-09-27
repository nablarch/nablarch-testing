<!-- 複数の種類のタグがソンジするケース -->

<%-- JSPコメント --%>
<%@ taglib prefix="n" uri="http://tis.co.jp/nablarch" %>

<jsp:setProperty name="hoge" property="value" />
<n:set var="userName" value="${user.name}" />
<n:set var="hoge" value="${100 * 100}}" />

<n:form name="name" cssClass="form">
  <%! String hoge; %><%! int fuga; %><%
  hoge = "1";
%>
  <n:button uri="hoge" name="${userName}" />
  ${user.Name}
</n:form>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="true">
  <!-- <n:set value="hoge" var="fuga" />-->
</c:if>

<!-- <n:set value="hoge" var="fuga" />-->


