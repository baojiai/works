<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="站内通知"/>
<%@ include file="header.jspf" %>

<div class="page-head">
    <div>
        <span class="eyebrow">NOTIFICATIONS</span>
        <h1>站内通知</h1>
        <p>预约、工单、配件与 SLA 提醒均在平台内部完成。</p>
    </div>
    <form method="post" action="${pageContext.request.contextPath}/notifications/read">
        <input type="hidden" name="mode" value="all">
        <button class="btn btn-outline-secondary">全部标记已读</button>
    </form>
</div>

<div class="list-group shadow-sm">
    <c:forEach items="${notifications}" var="x">
        <div class="list-group-item d-flex justify-content-between align-items-start gap-3 ${x.is_read ? '' : 'list-group-item-primary'}">
            <div>
                <span class="eyebrow"><c:out value="${x.notification_type}"/></span>
                <h2 class="h6 mb-1"><c:out value="${x.title}"/></h2>
                <p class="mb-1 ${x.is_read ? 'text-secondary' : ''}"><c:out value="${x.content}"/></p>
                <small class="text-muted"><c:out value="${x.created_at}"/> · <c:out value="${x.related_business_type}"/> #<c:out value="${x.related_business_id}"/></small>
            </div>
            <c:if test="${!x.is_read}">
                <form method="post" action="${pageContext.request.contextPath}/notifications/read">
                    <input type="hidden" name="id" value="${x.notification_id}">
                    <button class="btn btn-outline-primary btn-sm text-nowrap">标记已读</button>
                </form>
            </c:if>
        </div>
    </c:forEach>
    <c:if test="${empty notifications}">
        <div class="list-group-item text-center text-secondary py-5">暂无通知</div>
    </c:if>
</div>

<%@ include file="footer.jspf" %>
