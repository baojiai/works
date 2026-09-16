<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><c:set var="pageTitle" value="服务档案"/><%@ include file="../header.jspf" %>
<div class="engineer-profile-page">
    <div class="page-head"><div><span class="eyebrow">服务档案</span><h1>工程师服务档案</h1><p>评分、服务次数与履约率由系统自动计算，无法手工修改。</p></div></div>

    <section class="engineer-profile-summary">
        <div class="engineer-profile-avatar"><c:out value="${sessionScope.user.displayName.substring(0,1)}"/></div>
        <div class="engineer-profile-identity">
            <span class="eyebrow">工程师概要</span>
            <h2><c:out value="${profile.display_name}"/></h2>
            <p>账号：<c:out value="${sessionScope.user.username}"/></p>
            <div class="d-flex flex-wrap gap-2 mt-2">
                <span class="badge text-bg-primary">工程师</span>
                <span class="badge ${profile.qualification_status == 'APPROVED' ? 'text-bg-success' : (profile.qualification_status == 'EXPIRED' ? 'text-bg-danger' : 'text-bg-warning')}"><c:choose><c:when test="${profile.qualification_status == 'APPROVED'}">已认证</c:when><c:when test="${profile.qualification_status == 'EXPIRED'}">认证已失效</c:when><c:when test="${profile.qualification_status == 'REJECTED'}">认证已驳回</c:when><c:otherwise>认证待审核</c:otherwise></c:choose></span>
                <span class="badge ${profile.employment_status == 'ACTIVE' ? 'text-bg-success' : 'text-bg-secondary'}"><c:choose><c:when test="${profile.employment_status == 'ACTIVE'}">正常接单</c:when><c:when test="${profile.employment_status == 'DISABLED'}">已停用</c:when><c:otherwise>暂停接单</c:otherwise></c:choose></span>
            </div>
        </div>
        <div class="engineer-profile-areas">
            <span>当前服务区域</span>
            <div class="chips">
                <c:set var="hasProfileArea" value="false"/>
                <c:forEach items="${areas}" var="x"><c:if test="${x.selected == 1}"><c:set var="hasProfileArea" value="true"/><span><c:out value="${x.name}"/></span></c:if></c:forEach>
                <c:if test="${not hasProfileArea}"><small>暂未设置服务区域</small></c:if>
            </div>
        </div>
    </section>

    <div class="row g-3">
        <div class="col-12 col-xl-8">
            <form method="post" class="card border-0 shadow-sm">
                <div class="card-body p-4">
                    <span class="eyebrow">档案资料</span>
                    <h2 class="h5 mt-1 mb-3">公开服务信息</h2>
                    <div class="row g-3">
                        <div class="col-12 col-md-6">
                            <label class="form-label">联系电话<input name="phone" value="${profile.phone}" class="form-control"></label>
                        </div>
                        <div class="col-12">
                            <label class="form-label">个人简介<textarea name="bio" rows="5" class="form-control"><c:out value="${profile.bio}"/></textarea></label>
                        </div>
                        <div class="col-12">
                            <span class="eyebrow">维修技能</span>
                            <h2 class="h6 mt-1 mb-2">擅长故障类型</h2>
                            <div class="row g-2">
                                <c:forEach items="${faults}" var="x">
                                    <div class="col-12 col-md-6 col-xl-4">
                                        <div class="form-check choice-block">
                                            <input class="form-check-input" type="checkbox" name="faultId" value="${x.fault_type_id}" ${x.selected == 1 ? 'checked' : ''} id="pfault-${x.fault_type_id}">
                                            <label class="form-check-label" for="pfault-${x.fault_type_id}"><c:out value="${x.name}"/></label>
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>
                        </div>
                        <div class="col-12">
                            <span class="eyebrow">服务区域</span>
                            <h2 class="h6 mt-1 mb-2">服务区域</h2>
                            <div class="row g-2">
                                <c:forEach items="${areas}" var="x">
                                    <div class="col-12 col-md-6 col-xl-4">
                                        <div class="form-check choice-block">
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
            <section class="card border-0 shadow-sm engineer-profile-stats">
                <div class="card-body p-4">
                    <h2 class="h5 mb-3">系统统计</h2>
                    <div class="engineer-stat-grid">
                        <div class="engineer-stat-card is-blue"><span>资质</span><strong><c:choose><c:when test="${profile.qualification_status == 'APPROVED'}">已通过</c:when><c:when test="${profile.qualification_status == 'EXPIRED'}">已失效</c:when><c:when test="${profile.qualification_status == 'REJECTED'}">已驳回</c:when><c:otherwise>待审核</c:otherwise></c:choose></strong><small>平台认证状态</small></div>
                        <div class="engineer-stat-card is-green"><span>接单状态</span><strong><c:choose><c:when test="${profile.employment_status == 'ACTIVE'}">正常接单</c:when><c:when test="${profile.employment_status == 'DISABLED'}">已停用</c:when><c:otherwise>暂停接单</c:otherwise></c:choose></strong><small>当前服务能力</small></div>
                        <div class="engineer-stat-card is-purple"><span>完成服务</span><strong><c:out value="${profile.completed_count}"/></strong><small>累计完成工单</small></div>
                        <div class="engineer-stat-card is-orange"><span>评分 / 履约</span><strong><c:choose><c:when test="${profile.completed_count == 0}">暂无评分</c:when><c:otherwise><c:out value="${profile.average_rating}"/></c:otherwise></c:choose></strong><small><c:choose><c:when test="${profile.completed_count == 0}">暂无履约数据</c:when><c:otherwise>履约率 <c:out value="${profile.fulfillment_rate}"/>%</c:otherwise></c:choose></small></div>
                    </div>
                </div>
            </section>
        </aside>
    </div>
</div>
<%@ include file="../footer.jspf" %>
