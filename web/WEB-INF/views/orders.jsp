<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="维修工单"/>
<%@ include file="header.jspf" %>

<div class="page-head">
    <div>
        <span class="eyebrow">服务工单</span>
        <h1>维修工单</h1>
        <p>查看诊断、维修、配件、验收和返修全过程；工程师可同时查看自己发起和自己负责的工单。</p>
    </div>
</div>

<div class="row g-3">
    <c:forEach items="${orders}" var="x">
        <div class="col-12 col-md-6 col-xl-4">
            <a class="card border-0 shadow-sm text-decoration-none h-100" href="${pageContext.request.contextPath}/order/detail?id=${x.order_id}">
                <div class="card-body p-4">
                    <c:choose><c:when test="${x.order_status == 'PENDING_VISIT'}"><c:set var="orderListStatus" value="待工程师上门"/></c:when><c:when test="${x.order_status == 'REPAIRING'}"><c:set var="orderListStatus" value="维修中"/></c:when><c:when test="${x.order_status == 'WAITING_PARTS'}"><c:set var="orderListStatus" value="等待配件"/></c:when><c:when test="${x.order_status == 'PENDING_ACCEPTANCE'}"><c:set var="orderListStatus" value="待客户验收"/></c:when><c:when test="${x.order_status == 'COMPLETED'}"><c:set var="orderListStatus" value="已完成"/></c:when><c:when test="${x.order_status == 'CANCELLED'}"><c:set var="orderListStatus" value="已取消"/></c:when><c:when test="${x.order_status == 'REWORK'}"><c:set var="orderListStatus" value="返修中"/></c:when><c:when test="${x.order_status == 'PENDING_RESCHEDULE'}"><c:set var="orderListStatus" value="待改约"/></c:when><c:otherwise><c:set var="orderListStatus" value="处理中"/></c:otherwise></c:choose>
                    <div class="d-flex justify-content-between align-items-start">
                        <span class="badge ${x.order_status == 'COMPLETED' ? 'text-bg-success' : (x.order_status == 'CANCELLED' ? 'text-bg-secondary' : (x.order_status == 'PENDING_VISIT' ? 'text-bg-primary' : (x.order_status == 'REWORK' || x.order_status == 'WAITING_PARTS' ? 'text-bg-warning' : 'text-bg-info')))}"><c:out value="${orderListStatus}"/></span>
                        <small><c:out value="${x.order_no}"/></small>
                    </div>
                    <h2 class="h5 mt-3 mb-1"><c:out value="${x.fault_name}"/></h2>
                    <p class="text-secondary mb-3"><c:out value="${x.service_address}"/></p>
                    <div class="d-flex flex-column gap-1 small text-secondary">
                        <c:choose>
                            <c:when test="${x.customer_id == sessionScope.user.id}"><span>关系：我发起的维修</span></c:when>
                            <c:when test="${x.engineer_id == sessionScope.user.id}"><span>关系：我负责的工单</span></c:when>
                            <c:otherwise><span>平台工单</span></c:otherwise>
                        </c:choose>
                        <span>客户：<c:out value="${x.customer_name}"/></span>
                        <span>工程师：<c:out value="${x.engineer_name}"/></span>
                    </div>
                </div>
            </a>
        </div>
    </c:forEach>
</div>

<c:if test="${empty orders}"><div class="card text-center py-5"><div class="card-body text-secondary">暂无维修工单</div></div></c:if>

<%@ include file="footer.jspf" %>
