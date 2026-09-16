<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><c:set var="pageTitle" value="配件申请"/><%@ include file="../header.jspf" %>
<div class="page-head"><div><span class="eyebrow">配件申请</span><h1>配件申请审核与出库</h1><p>审核通过时锁定库存；实际领取后再从总量和锁定量中出库。</p></div></div>
<c:choose>
    <c:when test="${empty requests}">
        <div class="card text-center py-5"><div class="card-body text-secondary">暂无配件申请</div></div>
    </c:when>
    <c:otherwise>
        <div class="d-grid gap-3">
            <c:forEach items="${requests}" var="r">
                <c:choose><c:when test="${r.status == 'PENDING'}"><c:set var="requestStatusLabel" value="待审核"/></c:when><c:when test="${r.status == 'APPROVED'}"><c:set var="requestStatusLabel" value="已通过"/></c:when><c:when test="${r.status == 'ISSUED'}"><c:set var="requestStatusLabel" value="已出库"/></c:when><c:when test="${r.status == 'REJECTED'}"><c:set var="requestStatusLabel" value="已驳回"/></c:when><c:when test="${r.status == 'COMPLETED'}"><c:set var="requestStatusLabel" value="已完成"/></c:when><c:when test="${r.status == 'CANCELLED'}"><c:set var="requestStatusLabel" value="已取消"/></c:when><c:otherwise><c:set var="requestStatusLabel" value="处理中"/></c:otherwise></c:choose>
                <article class="card border-0 shadow-sm">
                    <div class="card-body p-4">
                        <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
                            <div>
                                <span class="badge ${r.status == 'PENDING' ? 'text-bg-warning' : (r.status == 'APPROVED' ? 'text-bg-primary' : (r.status == 'ISSUED' ? 'text-bg-success' : (r.status == 'REJECTED' ? 'text-bg-danger' : (r.status == 'COMPLETED' ? 'text-bg-secondary' : 'text-bg-light'))))}"><c:out value="${requestStatusLabel}"/></span>
                                <h2 class="h5 mt-2 mb-1"><c:out value="${r.request_no}"/></h2>
                                <p class="text-secondary mb-0">工单 <c:out value="${r.order_no}"/> · <c:out value="${r.engineer_name}"/> · <c:out value="${r.created_at}"/></p>
                            </div>
                        </div>
                        <p class="mb-3"><b>申请原因：</b><c:out value="${r.reason}"/></p>
                        <div class="table-responsive mb-3">
                            <table class="table table-sm align-middle mb-0">
                                <thead><tr><th>配件</th><th>申请</th><th>可用 / 锁定</th><th>已出库</th><th>已退回</th><th>退回操作</th></tr></thead>
                                <tbody>
                                <c:forEach items="${items}" var="i">
                                    <c:if test="${i.part_request_id == r.part_request_id}">
                                        <tr>
                                            <td><b><c:out value="${i.part_name}"/></b><small><c:out value="${i.model}"/></small></td>
                                            <td><c:out value="${i.request_quantity}"/> <c:out value="${i.unit}"/></td>
                                            <td><c:out value="${i.available_quantity}"/> / <c:out value="${i.locked_quantity}"/></td>
                                            <td><c:out value="${i.issued_quantity}"/></td>
                                            <td><c:out value="${i.return_quantity}"/></td>
                                            <td>
                                                <c:if test="${(r.status == 'ISSUED' || r.status == 'COMPLETED') && i.issued_quantity > i.return_quantity}">
                                                    <form class="d-flex gap-1 align-items-center flex-wrap" method="post" action="${pageContext.request.contextPath}/warehouse/return">
                                                        <input type="hidden" name="itemId" value="${i.item_id}">
                                                        <input type="number" name="quantity" min="1" max="${i.issued_quantity-i.return_quantity}" required placeholder="数量" class="form-control form-control-sm w-auto">
                                                        <input name="reason" required placeholder="退回原因" class="form-control form-control-sm w-auto">
                                                        <button class="btn btn-outline-warning btn-sm">确认退回</button>
                                                    </form>
                                                </c:if>
                                            </td>
                                        </tr>
                                    </c:if>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                        <div class="d-flex gap-2 flex-wrap justify-content-end">
                            <c:if test="${r.status == 'PENDING'}">
                                <form method="post" action="${pageContext.request.contextPath}/warehouse/review" class="d-flex gap-2 align-items-center flex-wrap">
                                    <input type="hidden" name="id" value="${r.part_request_id}">
                                    <input name="comment" placeholder="审核意见" class="form-control form-control-sm w-auto">
                                    <button class="btn btn-outline-danger btn-sm" name="decision" value="reject">整单驳回</button>
                                    <button class="btn btn-primary btn-sm" name="decision" value="approve">通过并锁定</button>
                                </form>
                            </c:if>
                            <c:if test="${r.status == 'APPROVED'}">
                                <form method="post" action="${pageContext.request.contextPath}/warehouse/release" class="d-flex gap-2 align-items-center">
                                    <input type="hidden" name="id" value="${r.part_request_id}">
                                    <input name="reason" required placeholder="取消原因" class="form-control form-control-sm w-auto">
                                    <button class="btn btn-outline-danger btn-sm">取消并释放</button>
                                </form>
                                <form method="post" action="${pageContext.request.contextPath}/warehouse/issue" data-confirm="确认工程师已实际领取，执行出库？">
                                    <input type="hidden" name="id" value="${r.part_request_id}">
                                    <button class="btn btn-success btn-sm">确认出库</button>
                                </form>
                            </c:if>
                            <c:if test="${r.status == 'ISSUED'}">
                                <form method="post" action="${pageContext.request.contextPath}/warehouse/complete">
                                    <input type="hidden" name="id" value="${r.part_request_id}">
                                    <button class="btn btn-primary btn-sm">核对完成</button>
                                </form>
                            </c:if>
                        </div>
                    </div>
                </article>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>
<%@ include file="../footer.jspf" %>
