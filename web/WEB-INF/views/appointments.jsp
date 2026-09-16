<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="预约管理"/>
<%@ include file="header.jspf" %>

<div class="page-head">
    <div>
        <span class="eyebrow">预约服务</span>
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
                <c:choose>
                    <c:when test="${x.status == 'BOOKED'}"><c:set var="appointmentStatusLabel" value="已预约"/></c:when>
                    <c:when test="${x.status == 'FULFILLED'}"><c:set var="appointmentStatusLabel" value="已履约"/></c:when>
                    <c:when test="${x.status == 'CANCELLED'}"><c:set var="appointmentStatusLabel" value="已取消"/></c:when>
                    <c:when test="${x.status == 'EXPIRED'}"><c:set var="appointmentStatusLabel" value="已过期"/></c:when>
                    <c:when test="${x.status == 'PENDING_RESCHEDULE'}"><c:set var="appointmentStatusLabel" value="待改约"/></c:when>
                    <c:otherwise><c:set var="appointmentStatusLabel" value="处理中"/></c:otherwise>
                </c:choose>
                <c:choose>
                    <c:when test="${x.order_status == 'PENDING_VISIT'}"><c:set var="appointmentOrderStatusLabel" value="待工程师上门"/></c:when>
                    <c:when test="${x.order_status == 'REPAIRING'}"><c:set var="appointmentOrderStatusLabel" value="维修中"/></c:when>
                    <c:when test="${x.order_status == 'WAITING_PARTS'}"><c:set var="appointmentOrderStatusLabel" value="等待配件"/></c:when>
                    <c:when test="${x.order_status == 'PENDING_ACCEPTANCE'}"><c:set var="appointmentOrderStatusLabel" value="待客户验收"/></c:when>
                    <c:when test="${x.order_status == 'COMPLETED'}"><c:set var="appointmentOrderStatusLabel" value="已完成"/></c:when>
                    <c:when test="${x.order_status == 'REWORK'}"><c:set var="appointmentOrderStatusLabel" value="返修中"/></c:when>
                    <c:when test="${x.order_status == 'PENDING_RESCHEDULE'}"><c:set var="appointmentOrderStatusLabel" value="待改约"/></c:when>
                    <c:when test="${x.order_status == 'CANCELLED'}"><c:set var="appointmentOrderStatusLabel" value="已取消"/></c:when>
                    <c:otherwise><c:set var="appointmentOrderStatusLabel" value="处理中"/></c:otherwise>
                </c:choose>
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
                    <td><span class="badge ${x.status == 'BOOKED' ? 'text-bg-primary' : (x.status == 'FULFILLED' ? 'text-bg-success' : (x.status == 'CANCELLED' || x.status == 'EXPIRED' ? 'text-bg-secondary' : (x.status == 'PENDING_RESCHEDULE' ? 'text-bg-warning' : 'text-bg-light')))}"><c:out value="${appointmentStatusLabel}"/></span><small><c:out value="${appointmentOrderStatusLabel}"/></small></td>
                    <td>
                        <div class="d-flex gap-2 flex-wrap align-items-center">
                            <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}/order/detail?id=${x.order_id}">查看工单</a>
                            <c:if test="${x.customer_id == sessionScope.user.id && x.status == 'BOOKED'}">
                                <button type="button" class="btn btn-outline-danger btn-sm" data-bs-toggle="modal" data-bs-target="#customerCancelModal${x.appointment_id}">取消预约</button>
                                <a class="btn btn-outline-secondary btn-sm" href="${pageContext.request.contextPath}/customer/candidates?requestId=${x.request_id}&replacesId=${x.appointment_id}">改约</a>
                            </c:if>
                            <c:if test="${x.customer_id == sessionScope.user.id && x.status == 'PENDING_RESCHEDULE'}">
                                <a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/customer/candidates?requestId=${x.request_id}&replacesId=${x.appointment_id}">重新选择</a>
                            </c:if>
                            <c:if test="${x.engineer_id == sessionScope.user.id && x.status == 'BOOKED' && x.order_status == 'PENDING_VISIT'}">
                                <button type="button" class="btn btn-outline-danger btn-sm" data-bs-toggle="modal" data-bs-target="#engineerCancelModal${x.appointment_id}">异常取消</button>
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

<c:forEach items="${appointments}" var="x">
    <c:if test="${x.customer_id == sessionScope.user.id && x.status == 'BOOKED'}">
        <div class="modal fade" id="customerCancelModal${x.appointment_id}" tabindex="-1" aria-labelledby="customerCancelModalLabel${x.appointment_id}" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered"><div class="modal-content">
                <div class="modal-header"><h2 class="modal-title fs-5" id="customerCancelModalLabel${x.appointment_id}">取消预约</h2><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button></div>
                <form method="post" action="${pageContext.request.contextPath}/appointment/cancel" data-confirm="确认取消预约并释放时段？">
                    <div class="modal-body"><input type="hidden" name="id" value="${x.appointment_id}"><label class="form-label mb-0">取消原因<textarea name="reason" required rows="3" placeholder="请填写取消原因" class="form-control"></textarea></label></div>
                    <div class="modal-footer"><button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">返回</button><button class="btn btn-danger">确认取消预约</button></div>
                </form>
            </div></div>
        </div>
    </c:if>
    <c:if test="${x.engineer_id == sessionScope.user.id && x.status == 'BOOKED' && x.order_status == 'PENDING_VISIT'}">
        <div class="modal fade" id="engineerCancelModal${x.appointment_id}" tabindex="-1" aria-labelledby="engineerCancelModalLabel${x.appointment_id}" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered"><div class="modal-content">
                <div class="modal-header"><h2 class="modal-title fs-5" id="engineerCancelModalLabel${x.appointment_id}">异常取消预约</h2><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button></div>
                <form method="post" action="${pageContext.request.contextPath}/engineer/appointment/cancel" data-confirm="异常取消会计入履约率并通知客户，确认继续？">
                    <div class="modal-body"><p class="text-secondary small">异常取消会计入履约率，并通知客户重新选择工程师。</p><input type="hidden" name="id" value="${x.appointment_id}"><label class="form-label mb-0">异常原因<textarea name="reason" required rows="3" placeholder="请填写异常原因" class="form-control"></textarea></label></div>
                    <div class="modal-footer"><button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">返回</button><button class="btn btn-danger">确认异常取消</button></div>
                </form>
            </div></div>
        </div>
    </c:if>
</c:forEach>

<%@ include file="footer.jspf" %>
