<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><c:set var="pageTitle" value="库存管理"/><%@ include file="../header.jspf" %>
<div class="inventory-page">
<div class="page-head"><div><span class="eyebrow">库存管理</span><h1>库存与流水</h1><p>当前总量始终等于可用量与锁定量之和，所有变化均可追溯。</p></div></div>
<div class="row g-3">
    <c:forEach items="${inventory}" var="x">
        <c:choose><c:when test="${x.available_quantity <= 0}"><c:set var="stockTone" value="out"/><c:set var="stockLabel" value="库存缺货"/><c:set var="stockBadge" value="text-bg-danger"/></c:when><c:when test="${x.available_quantity <= x.warning_threshold}"><c:set var="stockTone" value="warning"/><c:set var="stockLabel" value="库存预警"/><c:set var="stockBadge" value="text-bg-warning"/></c:when><c:otherwise><c:set var="stockTone" value="normal"/><c:set var="stockLabel" value="库存正常"/><c:set var="stockBadge" value="text-bg-success"/></c:otherwise></c:choose>
        <div class="col-12 col-md-6 col-xl-4">
            <article class="card border-0 shadow-sm h-100 inventory-card inventory-card-${stockTone}">
                <div class="card-body p-4 d-flex flex-column">
                    <div class="d-flex justify-content-between align-items-start mb-3">
                        <div>
                            <small class="text-secondary"><c:out value="${x.part_code}"/></small>
                            <h2 class="h5 mb-0"><c:out value="${x.name}"/></h2>
                            <p class="text-secondary small mb-0"><c:out value="${x.model}"/></p>
                        </div>
                        <span class="badge ${stockBadge}"><c:out value="${stockLabel}"/></span>
                    </div>
                    <div class="row g-2 text-center mb-4 inventory-figures">
                        <div class="col-3"><b class="fs-5 d-block"><c:out value="${x.total_quantity}"/></b><span class="text-secondary small">当前总量</span></div>
                        <div class="col-3"><b class="fs-5 d-block"><c:out value="${x.available_quantity}"/></b><span class="text-secondary small">可用</span></div>
                        <div class="col-3"><b class="fs-5 d-block"><c:out value="${x.locked_quantity}"/></b><span class="text-secondary small">锁定</span></div>
                        <div class="col-3"><b class="fs-5 d-block"><c:out value="${x.issued_quantity}"/></b><span class="text-secondary small">净出库</span></div>
                    </div>
                    <div class="inventory-card-action mt-auto"><button type="button" class="btn btn-outline-secondary btn-sm inventory-adjust-btn" data-bs-toggle="modal" data-bs-target="#stockModal${x.part_id}">调整库存</button></div>
                </div>
            </article>
            <div class="modal fade" id="stockModal${x.part_id}" tabindex="-1" aria-labelledby="stockModalLabel${x.part_id}" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <div><small class="text-secondary"><c:out value="${x.part_code}"/></small><h2 class="modal-title fs-5" id="stockModalLabel${x.part_id}">调整<c:out value="${x.name}"/>库存</h2></div>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                        </div>
                        <form method="post" action="${pageContext.request.contextPath}/warehouse/stock">
                            <div class="modal-body d-grid gap-3">
                                <input type="hidden" name="partId" value="${x.part_id}">
                                <label class="form-label mb-0">操作类型<select name="type" class="form-select"><option value="RESTOCK">补充入库</option><option value="ADJUST">盘点调整</option></select></label>
                                <label class="form-label mb-0">数量<input type="number" name="quantity" required placeholder="数量（调整可负）" class="form-control"></label>
                                <label class="form-label mb-0">原因<input name="reason" required placeholder="请填写调整原因" class="form-control"></label>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">取消</button>
                                <button class="btn btn-primary">确认登记</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </c:forEach>
</div>
<section class="card border-0 shadow-sm mt-3">
    <div class="card-body p-4">
        <h2 class="h5 mb-3">最近库存流水</h2>
        <c:choose>
            <c:when test="${empty flows}">
                <div class="inventory-empty-state">
                    <strong>暂无库存流水</strong>
                    <p>入库、出库、锁定及库存调整记录将在这里展示。</p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table class="table table-sm table-hover align-middle mb-0">
                        <thead><tr><th>时间</th><th>配件</th><th>类型</th><th>数量</th><th>关联申请 / 工单</th><th>操作者</th><th>原因</th></tr></thead>
                        <tbody>
                        <c:forEach items="${flows}" var="x">
                            <c:choose>
                                <c:when test="${x.flow_type == 'OUT'}"><c:set var="flowTypeLabel" value="出库"/></c:when>
                                <c:when test="${x.flow_type == 'IN' || x.flow_type == 'RESTOCK'}"><c:set var="flowTypeLabel" value="入库"/></c:when>
                                <c:when test="${x.flow_type == 'ADJUST'}"><c:set var="flowTypeLabel" value="库存调整"/></c:when>
                                <c:when test="${x.flow_type == 'LOCK'}"><c:set var="flowTypeLabel" value="锁定"/></c:when>
                                <c:when test="${x.flow_type == 'RETURN'}"><c:set var="flowTypeLabel" value="退库"/></c:when>
                                <c:when test="${x.flow_type == 'RELEASE' || x.flow_type == 'UNLOCK'}"><c:set var="flowTypeLabel" value="释放"/></c:when>
                                <c:otherwise><c:set var="flowTypeLabel" value="其它库存变动"/></c:otherwise>
                            </c:choose>
                            <tr>
                                <td><c:out value="${x.created_at}"/></td>
                                <td><c:out value="${x.part_name}"/></td>
                                <td><span class="badge text-bg-light"><c:out value="${flowTypeLabel}"/></span></td>
                                <td><c:out value="${x.quantity}"/></td>
                                <td><c:out value="${x.part_request_id}"/> / <c:out value="${x.order_id}"/></td>
                                <td><c:out value="${x.operator_name}"/></td>
                                <td><c:out value="${x.reason}"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</section>
</div>
<%@ include file="../footer.jspf" %>
