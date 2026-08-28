<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><c:set var="pageTitle" value="工程师认证"/><%@ include file="../header.jspf" %>
<div class="page-head"><div><span class="eyebrow">ENGINEER CERTIFICATION</span><h1>申请成为平台工程师</h1><p>通过认证后可以维护服务档案、发布排班，并在维修工单中申请配件。</p></div></div>
<c:if test="${not empty error}"><div class="alert danger"><span>!</span><c:out value="${error}"/></div></c:if>
<c:if test="${not empty latest}"><section class="card"><div class="section-head"><div><span class="eyebrow">LATEST APPLICATION</span><h2>最近一次申请</h2><p>状态：<span class="status"><c:out value="${latest.status}"/></span> · 提交时间：<c:out value="${latest.created_at}"/></p></div></div><c:if test="${not empty latest.review_comment}"><p class="note"><b>审核意见：</b><c:out value="${latest.review_comment}"/></p></c:if></section></c:if>
<form method="post" class="card form-card">
    <div class="section-head"><div><span class="eyebrow">BASIC MATERIALS</span><h2>认证资料</h2></div></div>
    <div class="form-grid">
        <label>真实姓名<input name="realName" value="${param.realName}" required placeholder="与资质材料保持一致"></label>
        <label>联系电话<input name="phone" value="${empty param.phone ? sessionScope.user.username : param.phone}" required maxlength="11" placeholder="11 位手机号"></label>
        <label>身份证号 / 身份证明编号<input name="idCardNo" value="${param.idCardNo}" required placeholder="用于管理员审核"></label>
        <label>从业年限<input type="number" name="experienceYears" min="0" value="${empty param.experienceYears ? 0 : param.experienceYears}" required></label>
        <label>主要服务区域<select name="areaId" required><option value="">请选择</option><c:forEach items="${areas}" var="x"><option value="${x.service_area_id}" ${param.areaId == x.service_area_id ? 'selected' : ''}><c:out value="${x.name}"/></option></c:forEach></select></label>
        <label>证书编号（可选）<input name="certificateNo" value="${param.certificateNo}" placeholder="电工证、厂家认证、培训证书等"></label>
        <label class="wide">可维修故障类型<div class="check-grid"><c:forEach items="${faults}" var="x"><label class="check"><input type="checkbox" name="faultId" value="${x.fault_type_id}"><span><c:out value="${x.device_name}"/> · <c:out value="${x.name}"/></span></label></c:forEach></div></label>
        <label class="wide">技能与服务经验<textarea name="skillDescription" rows="4" required placeholder="例如：擅长电脑无法开机、打印机卡纸、冰箱制冷异常等"><c:out value="${param.skillDescription}"/></textarea></label>
        <label class="wide">材料说明<textarea name="materialDescription" rows="4" required placeholder="请描述已准备的证明材料，例如身份证明、资格证书、过往维修案例、培训记录等"><c:out value="${param.materialDescription}"/></textarea></label>
    </div>
    <div class="form-actions"><button class="btn primary">提交认证申请</button></div>
</form>
<%@ include file="../footer.jspf" %>
