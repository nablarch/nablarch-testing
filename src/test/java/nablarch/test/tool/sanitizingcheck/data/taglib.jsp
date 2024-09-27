<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="jakarta.tags.functions" %>
<%@ taglib prefix="n" uri="http://tis.co.jp/nablarch" %>

<c:set property="fuga" />
<n:form>
  <c:if var="fuga" test="hoge" scope = "page">
    <ul><li>�ق�</li>
    </ul>
    <%-- suppress jsp check:この次の行はチェック対象外 --%>
    <n:checkbox name=
        "checkbox" accesskey
        ="1" autofocus="2\"\\"
        cssClass='cssClassName
        cssClassName2
        ' disabled='<%= disabled %>'
                           />
  </c:if>
</n:form>
