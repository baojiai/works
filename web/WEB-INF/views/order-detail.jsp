<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="工单详情"/>
<%@ include file="header.jspf" %>

<c:choose>
    <c:when test="${order.order_status == 'PENDING_VISIT'}"><c:set var="orderStatusLabel" value="待工程师上门"/><c:set var="orderStatusClass" value="text-bg-primary"/></c:when>
    <c:when test="${order.order_status == 'REPAIRING'}"><c:set var="orderStatusLabel" value="维修中"/><c:set var="orderStatusClass" value="text-bg-info"/></c:when>
    <c:when test="${order.order_status == 'WAITING_PARTS'}"><c:set var="orderStatusLabel" value="等待配件"/><c:set var="orderStatusClass" value="text-bg-warning"/></c:when>
    <c:when test="${order.order_status == 'PENDING_ACCEPTANCE'}"><c:set var="orderStatusLabel" value="待客户验收"/><c:set var="orderStatusClass" value="text-bg-info"/></c:when>
    <c:when test="${order.order_status == 'COMPLETED'}"><c:set var="orderStatusLabel" value="已完成"/><c:set var="orderStatusClass" value="text-bg-success"/></c:when>
    <c:when test="${order.order_status == 'REWORK'}"><c:set var="orderStatusLabel" value="返修中"/><c:set var="orderStatusClass" value="text-bg-warning"/></c:when>
    <c:when test="${order.order_status == 'PENDING_RESCHEDULE'}"><c:set var="orderStatusLabel" value="待改约"/><c:set var="orderStatusClass" value="text-bg-warning"/></c:when>
    <c:when test="${order.order_status == 'CANCELLED'}"><c:set var="orderStatusLabel" value="已取消"/><c:set var="orderStatusClass" value="text-bg-secondary"/></c:when>
    <c:otherwise><c:set var="orderStatusLabel" value="处理中"/><c:set var="orderStatusClass" value="text-bg-light"/></c:otherwise>
</c:choose>
<c:set var="showOrderActions" value="${order.engineer_id == sessionScope.user.id || (order.customer_id == sessionScope.user.id && (order.order_status == 'PENDING_ACCEPTANCE' || (order.order_status == 'COMPLETED' && empty review) || not empty review))}"/>
<div class="order-detail-page">
<div class="page-head">
    <div>
        <span class="eyebrow">服务工单</span>
        <h1><c:out value="${order.order_no}"/></h1>
        <p><c:out value="${order.device_name}"/> · <c:out value="${order.fault_name}"/> · <span class="badge ${orderStatusClass}"><c:out value="${orderStatusLabel}"/></span></p>
    </div>
    <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/orders">← 返回工单</a>
</div>

<div class="row g-3">
    <div class="${showOrderActions ? 'col-12 col-xl-8' : 'col-12'}">
        <section class="card border-0 shadow-sm mb-3">
            <div class="card-body p-4">
                <h2 class="h5 mb-3">服务信息</h2>
                <dl class="detail-list mb-0">
                    <div><dt>客户</dt><dd><c:out value="${order.customer_name}"/></dd></div>
                    <div><dt>工程师</dt><dd><c:out value="${order.engineer_name}"/></dd></div>
                    <div><dt>联系方式</dt><dd><c:out value="${order.contact_phone}"/></dd></div>
                    <div><dt>服务地址</dt><dd><c:out value="${order.service_address}"/></dd></div>
                    <div class="wide"><dt>故障描述</dt><dd><c:out value="${order.fault_description}"/></dd></div>
                </dl>
            </div>
        </section>

        <section class="card border-0 shadow-sm mb-3">
            <div class="card-body p-4">
                <h2 class="h5 mb-3">维修时间线</h2>
                <div class="timeline mb-0">
                    <c:forEach items="${logs}" var="x">
                        <c:choose>
                            <c:when test="${x.new_status == 'PENDING_VISIT'}"><c:set var="timelineStatus" value="待工程师上门"/></c:when>
                            <c:when test="${x.new_status == 'REPAIRING'}"><c:set var="timelineStatus" value="维修中"/></c:when>
                            <c:when test="${x.new_status == 'WAITING_PARTS'}"><c:set var="timelineStatus" value="等待配件"/></c:when>
                            <c:when test="${x.new_status == 'PENDING_ACCEPTANCE'}"><c:set var="timelineStatus" value="待客户验收"/></c:when>
                            <c:when test="${x.new_status == 'COMPLETED'}"><c:set var="timelineStatus" value="已完成"/></c:when>
                            <c:when test="${x.new_status == 'REWORK'}"><c:set var="timelineStatus" value="返修中"/></c:when>
                            <c:when test="${x.new_status == 'PENDING_RESCHEDULE'}"><c:set var="timelineStatus" value="待改约"/></c:when>
                            <c:when test="${x.new_status == 'CANCELLED'}"><c:set var="timelineStatus" value="已取消"/></c:when>
                            <c:otherwise><c:set var="timelineStatus" value="状态已更新"/></c:otherwise>
                        </c:choose>
                        <c:choose>
                            <c:when test="${x.reason == '工程师执行START'}"><c:set var="timelineReason" value="工程师开始维修"/></c:when>
                            <c:when test="${x.reason == '工程师执行WAIT_PARTS'}"><c:set var="timelineReason" value="工程师标记为等待配件"/></c:when>
                            <c:when test="${x.reason == '工程师执行RESUME'}"><c:set var="timelineReason" value="配件就绪，工程师继续维修"/></c:when>
                            <c:when test="${x.reason == '工程师执行FINISH'}"><c:set var="timelineReason" value="工程师提交完工"/></c:when>
                            <c:otherwise><c:set var="timelineReason" value="${x.reason}"/></c:otherwise>
                        </c:choose>
                        <div><i></i><b><c:out value="${timelineStatus}"/></b><span><c:out value="${x.created_at}"/> · <c:out value="${x.operator_name}"/></span><p><c:out value="${timelineReason}"/></p></div>
                    </c:forEach>
                </div>
            </div>
        </section>

        <section class="card border-0 shadow-sm mb-3">
            <div class="card-body p-4">
                <h2 class="h5 mb-3">维修记录</h2>
                <c:forEach items="${records}" var="x">
                    <article class="record">
                        <div><b><c:out value="${x.engineer_name}"/></b><time><c:out value="${x.created_at}"/></time></div>
                        <h3>诊断</h3><p><c:out value="${x.diagnosis}"/></p>
                        <h3>维修措施 · <c:out value="${x.labor_hours}"/> 小时</h3><p><c:out value="${x.repair_action}"/></p>
                        <c:if test="${not empty x.remark}"><small><c:out value="${x.remark}"/></small></c:if>
                    </article>
                </c:forEach>
                <c:if test="${empty records}"><p class="text-secondary mb-0">尚未填写维修记录。</p></c:if>
            </div>
        </section>

        <section class="card border-0 shadow-sm">
            <div class="card-body p-4">
                <h2 class="h5 mb-3">配件使用</h2>
                <div class="table-responsive">
                    <table class="table align-middle mb-0">
                        <thead><tr><th>配件</th><th>申请</th><th>出库</th><th>退回</th><th>状态</th></tr></thead>
                        <tbody>
                        <c:forEach items="${parts}" var="x">
                            <c:choose><c:when test="${x.status == 'PENDING'}"><c:set var="partStatus" value="待审核"/></c:when><c:when test="${x.status == 'APPROVED'}"><c:set var="partStatus" value="已通过"/></c:when><c:when test="${x.status == 'ISSUED'}"><c:set var="partStatus" value="已出库"/></c:when><c:when test="${x.status == 'COMPLETED'}"><c:set var="partStatus" value="已完成"/></c:when><c:when test="${x.status == 'REJECTED'}"><c:set var="partStatus" value="已驳回"/></c:when><c:when test="${x.status == 'CANCELLED'}"><c:set var="partStatus" value="已取消"/></c:when><c:otherwise><c:set var="partStatus" value="处理中"/></c:otherwise></c:choose>
                            <tr><td><c:out value="${x.name}"/> <small><c:out value="${x.model}"/></small></td><td><c:out value="${x.request_quantity}"/></td><td><c:out value="${x.issued_quantity}"/></td><td><c:out value="${x.return_quantity}"/></td><td><span class="badge text-bg-light"><c:out value="${partStatus}"/></span></td></tr>
                        </c:forEach>
                        <c:if test="${empty parts}"><tr><td colspan="5" class="text-center text-secondary py-4">未使用配件</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </section>
    </div>

    <c:if test="${showOrderActions}"><div class="col-12 col-xl-4">
        <c:if test="${order.engineer_id == sessionScope.user.id}">
            <section class="card border-0 shadow-sm mb-3">
                <div class="card-body p-4">
                    <h2 class="h5 mb-3">工程师操作</h2>
                    <div class="d-grid gap-2">
                        <c:if test="${order.order_status == 'PENDING_VISIT'}">
                            <form method="post" action="${pageContext.request.contextPath}/engineer/order/action"><input type="hidden" name="id" value="${order.order_id}"><input type="hidden" name="action" value="START"><button class="btn btn-primary">开始维修</button></form>
                        </c:if>
                        <c:if test="${order.order_status == 'REPAIRING' || order.order_status == 'REWORK'}">
                            <form method="post" action="${pageContext.request.contextPath}/engineer/order/record" class="d-grid gap-2">
                                <input type="hidden" name="id" value="${order.order_id}">
                                <label class="form-label mb-0">故障诊断<textarea name="diagnosis" required class="form-control"></textarea></label>
                                <label class="form-label mb-0">维修措施<textarea name="repairAction" required class="form-control"></textarea></label>
                                <label class="form-label mb-0">工时<input type="number" step="0.5" min="0" name="hours" required class="form-control"></label>
                                <label class="form-label mb-0">备注<input name="remark" class="form-control"></label>
                                <button class="btn btn-outline-primary">保存维修记录</button>
                            </form>
                            <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/engineer/part/request?orderId=${order.order_id}">申请配件</a>
                            <form method="post" action="${pageContext.request.contextPath}/engineer/order/action"><input type="hidden" name="id" value="${order.order_id}"><input type="hidden" name="action" value="WAIT_PARTS"><button class="btn btn-outline-secondary">进入等待配件</button></form>
                            <form method="post" action="${pageContext.request.contextPath}/engineer/order/action" data-confirm="确认维修记录完整并提交客户验收？"><input type="hidden" name="id" value="${order.order_id}"><input type="hidden" name="action" value="FINISH"><button class="btn btn-primary">提交完工</button></form>
                        </c:if>
                        <c:if test="${order.order_status == 'WAITING_PARTS'}">
                            <form method="post" action="${pageContext.request.contextPath}/engineer/order/action"><input type="hidden" name="id" value="${order.order_id}"><input type="hidden" name="action" value="RESUME"><button class="btn btn-primary">配件已满足，恢复维修</button></form>
                        </c:if>
                    </div>
                </div>
            </section>
        </c:if>

        <c:if test="${order.customer_id == sessionScope.user.id && order.order_status == 'PENDING_ACCEPTANCE'}">
            <section class="card border-primary shadow-sm mb-3">
                <div class="card-body p-4">
                    <h2 class="h5 mb-2">客户验收</h2>
                    <p class="text-secondary">请先查看维修记录和配件使用情况。</p>
                    <form method="post" action="${pageContext.request.contextPath}/order/accept" class="d-grid gap-2" data-confirm="确认提交验收结果？">
                        <input type="hidden" name="id" value="${order.order_id}">
                        <label class="form-label mb-0">验收结果<select name="result" class="form-select"><option value="PASSED">验收通过</option><option value="FAILED">验收不通过，进入返修</option></select></label>
                        <label class="form-label mb-0">说明<textarea name="comment" class="form-control" placeholder="验收不通过时必须填写原因"></textarea></label>
                        <button class="btn btn-success">提交验收</button>
                    </form>
                </div>
            </section>
        </c:if>

        <c:if test="${order.customer_id == sessionScope.user.id && order.order_status == 'COMPLETED' && empty review}">
            <section class="card border-0 shadow-sm mb-3">
                <div class="card-body p-4">
                    <h2 class="h5 mb-3">服务评价</h2>
                    <form method="post" action="${pageContext.request.contextPath}/order/review" class="d-grid gap-2">
                        <input type="hidden" name="id" value="${order.order_id}">
                        <label class="form-label mb-0">星级评分<select name="rating" class="form-select"><option value="5">★★★★★ 5星</option><option value="4">★★★★ 4星</option><option value="3">★★★ 3星</option><option value="2">★★ 2星</option><option value="1">★ 1星</option></select></label>
                        <label class="form-label mb-0">文字评价<textarea name="content" class="form-control"></textarea></label>
                        <button class="btn btn-primary">提交评价</button>
                    </form>
                </div>
            </section>
        </c:if>

        <c:if test="${not empty review}">
            <section class="card border-0 shadow-sm">
                <div class="card-body p-4">
                    <h2 class="h5 mb-2">客户评价</h2>
                    <div class="rating big">★ <c:out value="${review.rating}"/> / 5</div>
                    <p class="mb-0"><c:out value="${review.content}"/></p>
                </div>
            </section>
        </c:if>
    </div></c:if>
</div>
</div>

<%@ include file="footer.jspf" %>
