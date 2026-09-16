<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><c:set var="pageTitle" value="工程师认证"/><%@ include file="../header.jspf" %>
<div class="page-head"><div><span class="eyebrow">ENGINEER CERTIFICATION</span><h1>申请成为平台工程师</h1><p>通过认证后可以维护服务档案、发布排班，并在维修工单中申请配件。</p></div></div>
<c:if test="${not empty error}"><div class="alert alert-danger d-flex align-items-center gap-2"><span>!</span><c:out value="${error}"/></div></c:if>
<c:if test="${not empty latest}">
    <section class="card border-0 shadow-sm mb-3">
        <div class="card-body p-4">
            <span class="eyebrow">LATEST APPLICATION</span>
            <h2 class="h5 mt-1 mb-2">最近一次申请</h2>
            <p class="mb-0">状态：<span class="badge ${latest.status == 'APPROVED' ? 'text-bg-success' : (latest.status == 'REJECTED' ? 'text-bg-danger' : 'text-bg-warning')}"><c:out value="${latest.status}"/></span> · 提交时间：<c:out value="${latest.created_at}"/></p>
            <c:if test="${not empty latest.review_comment}"><p class="note mb-0 mt-2"><b>审核意见：</b><c:out value="${latest.review_comment}"/></p></c:if>
        </div>
    </section>
</c:if>
<form method="post" class="card border-0 shadow-sm">
    <div class="card-body p-4">
        <span class="eyebrow">BASIC MATERIALS</span>
        <h2 class="h5 mt-1 mb-3">认证资料</h2>
        <div class="row g-3">
            <div class="col-12 col-md-6">
                <label class="form-label">真实姓名<input name="realName" value="${param.realName}" required placeholder="与资质材料保持一致" class="form-control"></label>
            </div>
            <div class="col-12 col-md-6">
                <label class="form-label">联系电话<input name="phone" value="${empty param.phone ? sessionScope.user.username : param.phone}" required maxlength="11" placeholder="11 位手机号" class="form-control"></label>
            </div>
            <div class="col-12 col-md-6">
                <label class="form-label">身份证号 / 身份证明编号<input name="idCardNo" value="${param.idCardNo}" required placeholder="用于管理员审核" class="form-control"></label>
            </div>
            <div class="col-12 col-md-6">
                <label class="form-label">从业年限<input type="number" name="experienceYears" min="0" value="${empty param.experienceYears ? 0 : param.experienceYears}" required class="form-control"></label>
            </div>
            <div class="col-12 col-md-6">
                <label class="form-label">主要服务区域<select name="areaId" required class="form-select"><option value="">请选择</option><c:forEach items="${areas}" var="x"><option value="${x.service_area_id}" ${param.areaId == x.service_area_id ? 'selected' : ''}><c:out value="${x.name}"/></option></c:forEach></select></label>
            </div>
            <div class="col-12 col-md-6">
                <label class="form-label">证书编号（可选）<input name="certificateNo" value="${param.certificateNo}" placeholder="电工证、厂家认证、培训证书等" class="form-control"></label>
            </div>
            <div class="col-12">
                <label class="form-label d-block">可维修故障类型</label>
                <div class="row g-2">
                    <c:forEach items="${faults}" var="x">
                        <div class="col-12 col-md-6 col-xl-4">
                            <div class="form-check border rounded p-2 bg-white">
                                <input class="form-check-input" type="checkbox" name="faultId" value="${x.fault_type_id}" id="fault-${x.fault_type_id}">
                                <label class="form-check-label" for="fault-${x.fault_type_id}"><c:out value="${x.device_name}"/> · <c:out value="${x.name}"/></label>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </div>
            <div class="col-12">
                <label class="form-label">技能与服务经验<textarea name="skillDescription" rows="4" required placeholder="例如：擅长电脑无法开机、打印机卡纸、冰箱制冷异常等" class="form-control"><c:out value="${param.skillDescription}"/></textarea></label>
            </div>
            <div class="col-12">
                <label class="form-label">材料说明<textarea name="materialDescription" rows="4" required placeholder="请描述已准备的证明材料，例如身份证明、资格证书、过往维修案例、培训记录等" class="form-control"><c:out value="${param.materialDescription}"/></textarea></label>
            </div>
        </div>
        <div class="d-flex justify-content-end mt-4"><button class="btn btn-primary px-4">提交认证申请</button></div>
    </div>
</form>
<%@ include file="../footer.jspf" %>
