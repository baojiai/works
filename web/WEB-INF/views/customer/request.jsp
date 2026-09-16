<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="提交维修需求"/>
<%@ include file="../header.jspf" %>

<div class="page-head">
    <div>
        <span class="eyebrow">STEP 1 OF 3 · 提交报修</span>
        <h1>提交维修需求</h1>
        <p>填写设备与故障信息，平台将为你匹配可预约工程师。</p>
    </div>
</div>

<c:if test="${not empty error}">
    <div class="alert alert-danger d-flex align-items-center gap-2"><c:out value="${error}"/></div>
</c:if>

<form method="post" class="repair-request-form">
    <section class="card border-0 shadow-sm">
        <div class="card-body p-4">
            <div class="mb-4">
                <span class="eyebrow">服务信息</span>
                <h2 class="h5 mt-1 mb-1">填写设备与上门信息</h2>
                <p class="text-secondary mb-0">请选择实际设备和故障类型，并补充便于工程师判断的现象与地址。</p>
            </div>
            <div class="row g-3">
                <div class="col-12 col-md-6">
                    <label class="form-label">设备类型
                        <select name="deviceId" required class="form-select">
                            <option value="">请选择设备类型</option>
                            <c:forEach items="${devices}" var="x">
                                <option value="${x.device_type_id}" ${param.deviceId == x.device_type_id ? 'selected' : ''}><c:out value="${x.name}"/></option>
                            </c:forEach>
                        </select>
                    </label>
                </div>
                <div class="col-12 col-md-6">
                    <label class="form-label">故障类型
                        <select name="faultId" required class="form-select">
                            <option value="">请选择故障类型</option>
                            <c:forEach items="${faults}" var="x">
                                <option value="${x.fault_type_id}" data-device="${x.device_type_id}" ${param.faultId == x.fault_type_id ? 'selected' : ''}><c:out value="${x.name}"/></option>
                            </c:forEach>
                        </select>
                    </label>
                </div>
                <div class="col-12">
                    <label class="form-label">故障描述
                        <textarea name="description" required rows="4" placeholder="请描述故障现象、出现时间以及已经尝试过的处理方式" class="form-control"><c:out value="${param.description}"/></textarea>
                    </label>
                </div>
                <div class="col-12 col-md-6">
                    <label class="form-label">服务区域
                        <select name="areaId" required class="form-select">
                            <option value="">选择所在区域</option>
                            <c:forEach items="${areas}" var="x">
                                <option value="${x.service_area_id}" ${param.areaId == x.service_area_id ? 'selected' : ''}><c:out value="${x.name}"/></option>
                            </c:forEach>
                        </select>
                    </label>
                </div>
                <div class="col-12 col-md-6">
                    <label class="form-label">上门地址
                        <input name="address" value="${param.address}" required placeholder="小区、楼栋及门牌号" class="form-control">
                    </label>
                </div>
                <div class="col-12 col-md-4">
                    <label class="form-label">期望日期
                        <input type="date" name="expectedDate" value="${param.expectedDate}" required class="form-control">
                    </label>
                </div>
                <div class="col-12 col-md-4">
                    <label class="form-label">期望时段
                        <select name="slotId" class="form-select">
                            <option value="">不限时段，优先最快可达</option>
                            <c:forEach items="${slots}" var="x">
                                <option value="${x.slot_id}" ${param.slotId == x.slot_id ? 'selected' : ''}><c:out value="${x.name}"/> · <c:out value="${x.start_time}"/>—<c:out value="${x.end_time}"/></option>
                            </c:forEach>
                        </select>
                    </label>
                </div>
                <div class="col-12 col-md-4">
                    <label class="form-label">联系电话
                        <input name="phone" value="${param.phone}" required placeholder="用于工程师联系" class="form-control">
                    </label>
                </div>
            </div>
            <div class="service-flow">
                <span><b>1</b> 填写故障信息</span>
                <i></i>
                <span><b>2</b> 选择可约工程师</span>
                <i></i>
                <span><b>3</b> 确认上门预约</span>
            </div>
            <div class="d-flex justify-content-end mt-3">
                <button class="btn btn-primary px-4">提交报修并选择工程师 →</button>
            </div>
        </div>
    </section>
</form>

<%@ include file="../footer.jspf" %>
