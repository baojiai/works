<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="工单详情"/>
<%@ include file="header.jspf" %>

<div class="page-head">
    <div>
        <span class="eyebrow">ORDER DETAIL</span>
        <h1><c:out value="${order.order_no}"/></h1>
        <p><c:out value="${order.device_name}"/> · <c:out value="${order.fault_name}"/> · <span class="status"><c:out value="${order.order_status}"/></span></p>
    </div>
    <a class="btn" href="${pageContext.request.contextPath}/orders">← 返回工单</a>
</div>

<div class="detail-layout">
    <div class="detail-main">
        <section class="card">
            <div class="section-head"><h2>服务信息</h2></div>
            <dl class="detail-list">
                <div><dt>客户</dt><dd><c:out value="${order.customer_name}"/></dd></div>
                <div><dt>工程师</dt><dd><c:out value="${order.engineer_name}"/></dd></div>
                <div><dt>联系方式</dt><dd><c:out value="${order.contact_phone}"/></dd></div>
                <div><dt>服务地址</dt><dd><c:out value="${order.service_address}"/></dd></div>
                <div class="wide"><dt>故障描述</dt><dd><c:out value="${order.fault_description}"/></dd></div>
            </dl>
        </section>

        <section class="card">
            <div class="section-head"><h2>维修时间线</h2></div>
            <div class="timeline">
                <c:forEach items="${logs}" var="x">
                    <div><i></i><b><c:out value="${x.new_status}"/></b><span><c:out value="${x.created_at}"/> · <c:out value="${x.operator_name}"/></span><p><c:out value="${x.reason}"/></p></div>
                </c:forEach>
            </div>
        </section>

        <section class="card">
            <div class="section-head"><h2>维修记录</h2></div>
            <c:forEach items="${records}" var="x">
                <article class="record">
                    <div><b><c:out value="${x.engineer_name}"/></b><time><c:out value="${x.created_at}"/></time></div>
                    <h3>诊断</h3><p><c:out value="${x.diagnosis}"/></p>
                    <h3>维修措施 · <c:out value="${x.labor_hours}"/> 小时</h3><p><c:out value="${x.repair_action}"/></p>
                    <c:if test="${not empty x.remark}"><small><c:out value="${x.remark}"/></small></c:if>
                </article>
            </c:forEach>
            <c:if test="${empty records}"><p class="muted">尚未填写维修记录。</p></c:if>
        </section>

        <section class="card">
            <div class="section-head"><h2>配件使用</h2></div>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>配件</th><th>申请</th><th>出库</th><th>退回</th><th>状态</th></tr></thead>
                    <tbody>
                    <c:forEach items="${parts}" var="x">
                        <tr><td><c:out value="${x.name}"/> <small><c:out value="${x.model}"/></small></td><td><c:out value="${x.request_quantity}"/></td><td><c:out value="${x.issued_quantity}"/></td><td><c:out value="${x.return_quantity}"/></td><td><span class="status"><c:out value="${x.status}"/></span></td></tr>
                    </c:forEach>
                    <c:if test="${empty parts}"><tr><td colspan="5" class="empty-cell">未使用配件</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </section>
    </div>

    <aside class="detail-side">
        <c:if test="${order.engineer_id == sessionScope.user.id}">
            <section class="card sticky">
                <h2>工程师操作</h2>
                <div class="stack">
                    <c:if test="${order.order_status == 'PENDING_VISIT'}">
                        <form method="post" action="${pageContext.request.contextPath}/engineer/order/action"><input type="hidden" name="id" value="${order.order_id}"><input type="hidden" name="action" value="START"><button class="btn primary block">开始维修</button></form>
                    </c:if>
                    <c:if test="${order.order_status == 'REPAIRING' || order.order_status == 'REWORK'}">
                        <form method="post" action="${pageContext.request.contextPath}/engineer/order/record" class="compact-form">
                            <input type="hidden" name="id" value="${order.order_id}">
                            <label>故障诊断<textarea name="diagnosis" required></textarea></label>
                            <label>维修措施<textarea name="repairAction" required></textarea></label>
                            <label>工时<input type="number" step="0.5" min="0" name="hours" required></label>
                            <label>备注<input name="remark"></label>
                            <button class="btn block">保存维修记录</button>
                        </form>
                        <a class="btn block" href="${pageContext.request.contextPath}/engineer/part/request?orderId=${order.order_id}">申请配件</a>
                        <form method="post" action="${pageContext.request.contextPath}/engineer/order/action"><input type="hidden" name="id" value="${order.order_id}"><input type="hidden" name="action" value="WAIT_PARTS"><button class="btn block">进入等待配件</button></form>
                        <form method="post" action="${pageContext.request.contextPath}/engineer/order/action" data-confirm="确认维修记录完整并提交客户验收？"><input type="hidden" name="id" value="${order.order_id}"><input type="hidden" name="action" value="FINISH"><button class="btn primary block">提交完工</button></form>
                    </c:if>
                    <c:if test="${order.order_status == 'WAITING_PARTS'}">
                        <form method="post" action="${pageContext.request.contextPath}/engineer/order/action"><input type="hidden" name="id" value="${order.order_id}"><input type="hidden" name="action" value="RESUME"><button class="btn primary block">配件已满足，恢复维修</button></form>
                    </c:if>
                </div>
            </section>
        </c:if>

        <c:if test="${order.customer_id == sessionScope.user.id && order.order_status == 'PENDING_ACCEPTANCE'}">
            <section class="card sticky">
                <h2>客户验收</h2>
                <p class="muted">请先查看维修记录和配件使用情况。</p>
                <form method="post" action="${pageContext.request.contextPath}/order/accept" class="stack" data-confirm="确认提交验收结果？">
                    <input type="hidden" name="id" value="${order.order_id}">
                    <label>验收结果<select name="result"><option value="PASSED">验收通过</option><option value="FAILED">验收不通过，进入返修</option></select></label>
                    <label>说明<textarea name="comment" placeholder="验收不通过时必须填写原因"></textarea></label>
                    <button class="btn primary block">提交验收</button>
                </form>
            </section>
        </c:if>

        <c:if test="${order.customer_id == sessionScope.user.id && order.order_status == 'COMPLETED' && empty review}">
            <section class="card sticky">
                <h2>服务评价</h2>
                <form method="post" action="${pageContext.request.contextPath}/order/review" class="stack">
                    <input type="hidden" name="id" value="${order.order_id}">
                    <label>星级评分<select name="rating"><option value="5">★★★★★ 5星</option><option value="4">★★★★ 4星</option><option value="3">★★★ 3星</option><option value="2">★★ 2星</option><option value="1">★ 1星</option></select></label>
                    <label>文字评价<textarea name="content"></textarea></label>
                    <button class="btn primary block">提交评价</button>
                </form>
            </section>
        </c:if>

        <c:if test="${not empty review}">
            <section class="card"><h2>客户评价</h2><div class="rating big">★ <c:out value="${review.rating}"/> / 5</div><p><c:out value="${review.content}"/></p></section>
        </c:if>
    </aside>
</div>

<%@ include file="footer.jspf" %>
