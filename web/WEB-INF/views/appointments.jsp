<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="预约管理"/>
<%@ include file="header.jspf" %>

<div class="page-head">
    <div>
        <span class="eyebrow">APPOINTMENTS</span>
        <h1>预约管理</h1>
        <p>工程师账号会同时显示“我预约别人的服务”和“别人预约我的服务”。</p>
    </div>
</div>

<div class="card border-0 shadow-sm">
    <div class="table-responsive">
        <table class="table align-middle mb-0">
            <thead>
            <tr>
                <th>预约编号</th>
                <th>关系</th>
                <th>服务对象</th>
                <th>故障 / 地址</th>
                <th>预约时间</th>
                <th>预约 / 工单状态</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${appointments}" var="x">
                <tr>
                    <td><b><c:out value="${x.appointment_no}"/></b></td>
                    <td>
                        <c:choose>
                            <c:when test="${x.customer_id == sessionScope.user.id}"><span class="badge text-bg-primary">我预约的</span></c:when>
                            <c:when test="${x.engineer_id == sessionScope.user.id}"><span class="badge text-bg-success">我接到的</span></c:when>
                            <c:otherwise><span class="badge text-bg-secondary">平台记录</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${x.customer_id == sessionScope.user.id}">工程师：<c:out value="${x.engineer_name}"/></c:when>
                            <c:otherwise>客户：<c:out value="${x.customer_name}"/></c:otherwise>
                        </c:choose>
                    </td>
                    <td><c:out value="${x.fault_name}"/><small><c:out value="${x.service_address}"/></small></td>
                    <td><c:out value="${x.service_date}"/><small><c:out value="${x.slot_name}"/> <c:out value="${x.start_time}"/></small></td>
                    <td><span class="badge ${x.status == 'BOOKED' ? 'text-bg-primary' : (x.status == 'FULFILLED' ? 'text-bg-success' : (x.status == 'CANCELLED' || x.status == 'EXPIRED' ? 'text-bg-secondary' : (x.status == 'PENDING_RESCHEDULE' ? 'text-bg-warning' : 'text-bg-light')))}"><c:out value="${x.status}"/></span><small><c:out value="${x.order_status}"/></small></td>
                    <td>
                        <div class="d-flex gap-2 flex-wrap align-items-center">
                            <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}/order/detail?id=${x.order_id}">查看工单</a>
                            <c:if test="${x.customer_id == sessionScope.user.id && x.status == 'BOOKED'}">
                                <form method="post" action="${pageContext.request.contextPath}/appointment/cancel" data-confirm="确认取消预约并释放时段？" class="d-flex gap-1 align-items-center">
                                    <input type="hidden" name="id" value="${x.appointment_id}">
                                    <input name="reason" required placeholder="取消原因" class="form-control form-control-sm w-auto">
                                    <button class="btn btn-outline-danger btn-sm">取消</button>
                                </form>
                                <a class="btn btn-outline-secondary btn-sm" href="${pageContext.request.contextPath}/customer/candidates?requestId=${x.request_id}&replacesId=${x.appointment_id}">改约</a>
                            </c:if>
                            <c:if test="${x.customer_id == sessionScope.user.id && x.status == 'PENDING_RESCHEDULE'}">
                                <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/customer/candidates?requestId=${x.request_id}&replacesId=${x.appointment_id}">重新选择</a>
                            </c:if>
                            <c:if test="${x.engineer_id == sessionScope.user.id && x.status == 'BOOKED' && x.order_status == 'PENDING_VISIT'}">
                                <form method="post" action="${pageContext.request.contextPath}/engineer/appointment/cancel" data-confirm="异常取消会计入履约率并通知客户，确认继续？" class="d-flex gap-1 align-items-center">
                                    <input type="hidden" name="id" value="${x.appointment_id}">
                                    <input name="reason" required placeholder="异常原因" class="form-control form-control-sm w-auto">
                                    <button class="btn btn-outline-danger btn-sm">异常取消</button>
                                </form>
                            </c:if>
                        </div>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty appointments}">
                <tr><td colspan="7" class="text-center text-secondary py-5">暂无预约记录</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="footer.jspf" %>
