<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><c:set var="pageTitle" value="系统管理"/><%@ include file="../header.jspf" %>
<div class="page-head">
    <div>
        <span class="eyebrow">SYSTEM ADMINISTRATION</span>
        <h1>系统管理</h1>
        <p>维护账号、资质、基础分类、业务规则、SLA 与异常记录。</p>
    </div>
    <div class="d-flex gap-2 flex-wrap">
        <form method="post" action="${pageContext.request.contextPath}/admin/expire"><button class="btn btn-outline-warning btn-sm">处理改约超时</button></form>
        <form method="post" action="${pageContext.request.contextPath}/admin/sla"><button class="btn btn-warning btn-sm">运行 SLA 检查</button></form>
    </div>
</div>

<div class="row g-3 mb-3">
    <c:forEach items="${stats}" var="x">
        <div class="col-6 col-md-4 col-xl-3">
            <div class="card border-0 shadow-sm h-100">
                <div class="card-body p-3">
                    <span class="text-secondary small"><c:out value="${x.metric}"/></span>
                    <strong class="d-block fs-2 mt-1"><c:out value="${x.metric_value}"/></strong>
                </div>
            </div>
        </div>
    </c:forEach>
</div>

<section class="card border-0 shadow-sm mb-3">
    <div class="card-body p-4">
        <span class="eyebrow">ENGINEER APPLICATIONS</span>
        <h2 class="h5 mt-1 mb-1">工程师认证审核</h2>
        <p class="text-secondary mb-3">审核通过后，申请人将升级为工程师并获得接单、排班和配件申请能力。</p>
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead><tr><th>申请人</th><th>联系方式</th><th>区域 / 年限</th><th>技能</th><th>材料说明</th><th>状态</th><th>操作</th></tr></thead>
                <tbody>
                <c:forEach items="${applications}" var="a">
                    <tr>
                        <td><b><c:out value="${a.real_name}"/></b><small><c:out value="${a.user_name}"/> · <c:out value="${a.created_at}"/></small></td>
                        <td><c:out value="${a.phone}"/><small><c:out value="${a.id_card_no}"/></small></td>
                        <td><c:out value="${a.area_name}"/><small><c:out value="${a.experience_years}"/> 年经验</small></td>
                        <td><div class="chips"><c:forEach items="${applicationSkills}" var="s"><c:if test="${s.application_id == a.application_id}"><span><c:out value="${s.device_name}"/> · <c:out value="${s.fault_name}"/></span></c:if></c:forEach></div><small><c:out value="${a.skill_description}"/></small></td>
                        <td><c:out value="${a.material_description}"/><c:if test="${not empty a.certificate_no}"><small>证书：<c:out value="${a.certificate_no}"/></small></c:if><c:if test="${not empty a.review_comment}"><small>意见：<c:out value="${a.review_comment}"/></small></c:if></td>
                        <td><span class="badge ${a.status == 'PENDING' ? 'text-bg-warning' : (a.status == 'APPROVED' ? 'text-bg-success' : 'text-bg-danger')}"><c:out value="${a.status}"/></span></td>
                        <td>
                            <c:if test="${a.status == 'PENDING'}">
                                <form method="post" action="${pageContext.request.contextPath}/admin/application" class="d-flex gap-1 align-items-center flex-wrap">
                                    <input type="hidden" name="id" value="${a.application_id}">
                                    <input name="comment" placeholder="审核意见" class="form-control form-control-sm w-auto">
                                    <button class="btn btn-outline-danger btn-sm" name="decision" value="reject">驳回</button>
                                    <button class="btn btn-success btn-sm" name="decision" value="approve">通过</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty applications}"><tr><td colspan="7" class="text-center text-secondary py-4">暂无工程师认证申请</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </div>
</section>

<section class="card border-0 shadow-sm mb-3">
    <div class="card-body p-4">
        <span class="eyebrow">USERS</span>
        <h2 class="h5 mt-1 mb-3">账号与工程师资质</h2>
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead><tr><th>账号</th><th>姓名</th><th>角色</th><th>账号状态</th><th>工程师资质</th><th>操作</th></tr></thead>
                <tbody>
                <c:forEach items="${users}" var="x">
                    <tr>
                        <td><c:out value="${x.username}"/></td>
                        <td><c:out value="${x.display_name}"/></td>
                        <td><c:out value="${x.role_type}"/></td>
                        <td><span class="badge ${x.status == 'ACTIVE' ? 'text-bg-success' : 'text-bg-secondary'}"><c:out value="${x.status}"/></span></td>
                        <td><c:out value="${x.qualification_status}"/></td>
                        <td>
                            <div class="d-flex gap-2 flex-wrap align-items-center">
                                <form method="post" action="${pageContext.request.contextPath}/admin/user" class="d-flex gap-1 align-items-center">
                                    <input type="hidden" name="id" value="${x.user_id}">
                                    <select name="status" class="form-select form-select-sm w-auto"><option value="ACTIVE">启用</option><option value="DISABLED">停用</option></select>
                                    <button class="btn btn-outline-secondary btn-sm">更新账号</button>
                                </form>
                                <c:if test="${x.role_type == 'ENGINEER'}">
                                    <form method="post" action="${pageContext.request.contextPath}/admin/qualification" class="d-flex gap-1 align-items-center">
                                        <input type="hidden" name="id" value="${x.user_id}">
                                        <select name="status" class="form-select form-select-sm w-auto"><option value="APPROVED">审核通过</option><option value="PENDING">待审核</option><option value="EXPIRED">已失效</option></select>
                                        <button class="btn btn-outline-secondary btn-sm">更新资质</button>
                                    </form>
                                </c:if>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</section>

<div class="row g-3 mb-3">
    <section class="col-12 col-xl-6">
        <div class="card border-0 shadow-sm h-100">
            <div class="card-body p-4">
                <h2 class="h5 mb-3">业务规则参数</h2>
                <div class="d-grid gap-2">
                    <c:forEach items="${configs}" var="x">
                        <form method="post" action="${pageContext.request.contextPath}/admin/config" class="d-flex gap-2 align-items-center flex-wrap">
                            <div class="flex-grow-1" style="min-width:180px">
                                <b><c:out value="${x.config_key}"/></b>
                                <small class="text-secondary"><c:out value="${x.description}"/></small>
                            </div>
                            <input type="hidden" name="key" value="${x.config_key}">
                            <input name="value" value="${x.config_value}" required class="form-control form-control-sm w-auto">
                            <button class="btn btn-outline-primary btn-sm">保存</button>
                        </form>
                    </c:forEach>
                </div>
            </div>
        </div>
    </section>
    <section class="col-12 col-xl-6">
        <div class="card border-0 shadow-sm h-100">
            <div class="card-body p-4">
                <h2 class="h5 mb-3">新增基础数据</h2>
                <form method="post" action="${pageContext.request.contextPath}/admin/basic" class="row g-2 align-items-end">
                    <div class="col-12 col-md-4">
                        <label class="form-label mb-1">数据类型<select name="kind" class="form-select form-select-sm"><option value="device">设备类别</option><option value="fault">故障类别</option><option value="area">服务区域</option><option value="slot">标准时段</option></select></label>
                    </div>
                    <div class="col-12 col-md-4">
                        <label class="form-label mb-1">名称<input name="name" required class="form-control form-control-sm"></label>
                    </div>
                    <div class="col-12 col-md-4">
                        <label class="form-label mb-1">所属设备 ID（仅故障类别）<input type="number" name="parentId" class="form-control form-control-sm"></label>
                    </div>
                    <div class="col-6 col-md-3">
                        <label class="form-label mb-1">开始时间<input type="time" name="startTime" class="form-control form-control-sm"></label>
                    </div>
                    <div class="col-6 col-md-3">
                        <label class="form-label mb-1">结束时间<input type="time" name="endTime" class="form-control form-control-sm"></label>
                    </div>
                    <div class="col-12 col-md-6 d-flex justify-content-end">
                        <button class="btn btn-primary btn-sm px-3">新增</button>
                    </div>
                </form>
                <div class="chips mt-3">
                    <c:forEach items="${devices}" var="x"><span>设备 ${x.device_type_id} · <c:out value="${x.name}"/></span></c:forEach>
                    <c:forEach items="${areas}" var="x"><span>区域 · <c:out value="${x.name}"/></span></c:forEach>
                    <c:forEach items="${slots}" var="x"><span>时段 · <c:out value="${x.name}"/></span></c:forEach>
                </div>
            </div>
        </div>
    </section>
</div>

<section class="card border-0 shadow-sm mb-3">
    <div class="card-body p-4">
        <span class="eyebrow">EXCEPTIONS</span>
        <h2 class="h5 mt-1 mb-3">异常预约</h2>
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead><tr><th>编号</th><th>客户</th><th>工程师</th><th>状态</th><th>异常原因</th><th>更新时间</th></tr></thead>
                <tbody>
                <c:forEach items="${exceptions}" var="x">
                    <tr><td><c:out value="${x.appointment_no}"/></td><td><c:out value="${x.customer_name}"/></td><td><c:out value="${x.engineer_name}"/></td><td><span class="badge text-bg-warning"><c:out value="${x.status}"/></span></td><td><c:out value="${x.cancel_reason}"/></td><td><c:out value="${x.updated_at}"/></td></tr>
                </c:forEach>
                <c:if test="${empty exceptions}"><tr><td colspan="6" class="text-center text-secondary py-4">暂无异常预约</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </div>
</section>

<section class="card border-0 shadow-sm">
    <div class="card-body p-4">
        <span class="eyebrow">AUDIT LOG</span>
        <h2 class="h5 mt-1 mb-3">最近操作日志</h2>
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead><tr><th>时间</th><th>操作者</th><th>操作</th><th>业务对象</th><th>说明</th></tr></thead>
                <tbody>
                <c:forEach items="${logs}" var="x">
                    <tr><td><c:out value="${x.created_at}"/></td><td><c:out value="${x.display_name}"/></td><td><c:out value="${x.operation_type}"/></td><td><c:out value="${x.business_type}"/> #<c:out value="${x.business_id}"/></td><td><c:out value="${x.description}"/></td></tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</section>
<%@ include file="../footer.jspf" %>
