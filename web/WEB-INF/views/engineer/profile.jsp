<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><c:set var="pageTitle" value="服务档案"/><%@ include file="../header.jspf" %>
<div class="page-head"><div><span class="eyebrow">SERVICE PROFILE</span><h1>工程师服务档案</h1><p>评分、服务次数与履约率由系统自动计算，无法手工修改。</p></div></div>
<div class="row g-3">
    <div class="col-12 col-xl-8">
        <form method="post" class="card border-0 shadow-sm">
            <div class="card-body p-4">
                <div class="row g-3">
                    <div class="col-12 col-md-6">
                        <label class="form-label">联系电话<input name="phone" value="${profile.phone}" class="form-control"></label>
                    </div>
                    <div class="col-12">
                        <label class="form-label">个人简介<textarea name="bio" rows="5" class="form-control"><c:out value="${profile.bio}"/></textarea></label>
                    </div>
                    <div class="col-12">
                        <span class="eyebrow">SKILLS</span>
                        <h2 class="h6 mt-1 mb-2">擅长故障类型</h2>
                        <div class="row g-2">
                            <c:forEach items="${faults}" var="x">
                                <div class="col-12 col-md-6 col-xl-4">
                                    <div class="form-check border rounded p-2 bg-white">
                                        <input class="form-check-input" type="checkbox" name="faultId" value="${x.fault_type_id}" ${x.selected == 1 ? 'checked' : ''} id="pfault-${x.fault_type_id}">
                                        <label class="form-check-label" for="pfault-${x.fault_type_id}"><c:out value="${x.name}"/></label>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                    <div class="col-12">
                        <span class="eyebrow">AREAS</span>
                        <h2 class="h6 mt-1 mb-2">服务区域</h2>
                        <div class="row g-2">
                            <c:forEach items="${areas}" var="x">
                                <div class="col-12 col-md-6 col-xl-4">
                                    <div class="form-check border rounded p-2 bg-white">
                                        <input class="form-check-input" type="checkbox" name="areaId" value="${x.service_area_id}" ${x.selected == 1 ? 'checked' : ''} id="parea-${x.service_area_id}">
                                        <label class="form-check-label" for="parea-${x.service_area_id}"><c:out value="${x.name}"/></label>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                </div>
                <div class="d-flex justify-content-end mt-4"><button class="btn btn-primary px-4">保存档案</button></div>
            </div>
        </form>
    </div>
    <aside class="col-12 col-xl-4">
        <section class="card border-0 shadow-sm">
            <div class="card-body p-4">
                <h2 class="h5 mb-3">系统统计</h2>
                <dl class="stat-list mb-0">
                    <div><dt>资质</dt><dd><span class="badge ${profile.qualification_status == 'APPROVED' ? 'text-bg-success' : (profile.qualification_status == 'EXPIRED' ? 'text-bg-danger' : 'text-bg-warning')}"><c:out value="${profile.qualification_status}"/></span></dd></div>
                    <div><dt>在职状态</dt><dd><c:out value="${profile.employment_status}"/></dd></div>
                    <div><dt>平均评分</dt><dd><c:out value="${profile.average_rating}"/></dd></div>
                    <div><dt>完成服务</dt><dd><c:out value="${profile.completed_count}"/></dd></div>
                    <div><dt>履约率</dt><dd><c:out value="${profile.fulfillment_rate}"/>%</dd></div>
                </dl>
            </div>
        </section>
    </aside>
</div>
<%@ include file="../footer.jspf" %>
