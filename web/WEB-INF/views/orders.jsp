<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="维修工单"/>
<%@ include file="header.jspf" %>

<div class="page-head">
    <div>
        <span class="eyebrow">REPAIR ORDERS</span>
        <h1>维修工单</h1>
        <p>查看诊断、维修、配件、验收和返修全过程；工程师可同时查看自己发起和自己负责的工单。</p>
    </div>
</div>

<div class="order-grid">
    <c:forEach items="${orders}" var="x">
        <a class="order-card" href="${pageContext.request.contextPath}/order/detail?id=${x.order_id}">
            <div>
                <span class="status"><c:out value="${x.order_status}"/></span>
                <small><c:out value="${x.order_no}"/></small>
            </div>
            <h2><c:out value="${x.fault_name}"/></h2>
            <p><c:out value="${x.service_address}"/></p>
            <div class="order-meta">
                <c:choose>
                    <c:when test="${x.customer_id == sessionScope.user.id}"><span>关系：我发起的维修</span></c:when>
                    <c:when test="${x.engineer_id == sessionScope.user.id}"><span>关系：我负责的工单</span></c:when>
                    <c:otherwise><span>平台工单</span></c:otherwise>
                </c:choose>
                <span>客户：<c:out value="${x.customer_name}"/></span>
                <span>工程师：<c:out value="${x.engineer_name}"/></span>
            </div>
        </a>
    </c:forEach>
</div>

<c:if test="${empty orders}"><div class="empty">暂无维修工单</div></c:if>

<%@ include file="footer.jspf" %>
