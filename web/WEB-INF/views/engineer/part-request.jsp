<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><c:set var="pageTitle" value="申请配件"/><%@ include file="../header.jspf" %>
<div class="page-head"><div><span class="eyebrow">PART REQUEST</span><h1>从工单申请配件</h1><p>一张申请可包含多种配件，提交后由仓库管理员整单审核。</p></div></div>
<form method="post" class="card border-0 shadow-sm">
    <div class="card-body p-4">
        <input type="hidden" name="orderId" value="${orderId}">
        <div class="row g-3">
            <c:forEach begin="1" end="3">
                <div class="col-12">
                    <div class="row g-2">
                        <div class="col-12 col-md-8">
                            <label class="form-label">配件<select name="partId" class="form-select"><option value="0">请选择</option><c:forEach items="${parts}" var="x"><option value="${x.part_id}"><c:out value="${x.name}"/> / <c:out value="${x.model}"/>（可用 <c:out value="${x.available_quantity}"/>）</option></c:forEach></select></label>
                        </div>
                        <div class="col-12 col-md-4">
                            <label class="form-label">申请数量<input type="number" name="quantity" min="0" value="0" class="form-control"></label>
                        </div>
                    </div>
                </div>
            </c:forEach>
            <div class="col-12">
                <label class="form-label">申请原因<textarea name="reason" required rows="4" placeholder="说明诊断结果和使用配件的必要性" class="form-control"></textarea></label>
            </div>
        </div>
        <div class="d-flex gap-2 flex-wrap justify-content-end mt-4">
            <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/order/detail?id=${orderId}">返回工单</a>
            <button class="btn btn-primary px-4">提交申请</button>
        </div>
    </div>
</form>
<%@ include file="../footer.jspf" %>
