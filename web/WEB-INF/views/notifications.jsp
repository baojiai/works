<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="站内通知"/>
<%@ include file="header.jspf" %>

<div class="page-head">
    <div>
        <span class="eyebrow">消息通知</span>
        <h1>站内通知</h1>
        <p>预约、工单、配件与 SLA 提醒均在平台内部完成。</p>
    </div>
    <form method="post" action="${pageContext.request.contextPath}/notifications/read">
        <input type="hidden" name="mode" value="all">
        <button class="btn btn-outline-secondary">全部标记已读</button>
    </form>
</div>

<div class="list-group shadow-sm notification-list">
    <c:forEach items="${notifications}" var="x">
        <c:choose><c:when test="${x.notification_type == 'RESCHEDULE_EXPIRED'}"><c:set var="notificationTypeLabel" value="改约提醒"/></c:when><c:when test="${x.notification_type == 'APPOINTMENT_REMINDER'}"><c:set var="notificationTypeLabel" value="预约提醒"/></c:when><c:when test="${x.notification_type == 'ORDER_OVERDUE'}"><c:set var="notificationTypeLabel" value="工单超时提醒"/></c:when><c:when test="${x.notification_type == 'PART_REVIEW_OVERDUE'}"><c:set var="notificationTypeLabel" value="配件审核提醒"/></c:when><c:when test="${x.notification_type == 'PART_RESULT'}"><c:set var="notificationTypeLabel" value="配件审核结果"/></c:when><c:when test="${x.notification_type == 'PART_ISSUED'}"><c:set var="notificationTypeLabel" value="配件出库通知"/></c:when><c:otherwise><c:set var="notificationTypeLabel" value="业务通知"/></c:otherwise></c:choose>
        <c:choose><c:when test="${x.related_business_type == 'APPOINTMENT'}"><c:set var="relatedBusinessLabel" value="预约"/></c:when><c:when test="${x.related_business_type == 'ORDER'}"><c:set var="relatedBusinessLabel" value="工单"/></c:when><c:when test="${x.related_business_type == 'PART_REQUEST'}"><c:set var="relatedBusinessLabel" value="配件申请"/></c:when><c:otherwise><c:set var="relatedBusinessLabel" value="平台业务"/></c:otherwise></c:choose>
        <div class="list-group-item d-flex justify-content-between align-items-start gap-3 notification-item ${x.is_read ? 'is-read' : 'is-unread'}">
            <div class="min-w-0">
                <span class="eyebrow"><c:out value="${notificationTypeLabel}"/></span>
                <h2 class="h6 fw-bold mb-1"><c:out value="${x.title}"/></h2>
                <p class="mb-1 ${x.is_read ? 'text-secondary' : ''}"><c:out value="${x.content}"/></p>
                <small class="text-muted"><fmt:formatDate value="${x.created_at}" pattern="yyyy-MM-dd HH:mm"/> · <c:out value="${relatedBusinessLabel}"/> #<c:out value="${x.related_business_id}"/></small>
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
