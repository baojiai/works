<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="系统管理"/><%@ include file="../header.jspf" %>
<div class="admin-page">
<div class="page-head">
    <div>
        <span class="eyebrow">系统管理</span>
        <h1>系统管理</h1>
        <p>维护账号、资质、基础分类、业务规则、SLA 与异常记录。</p>
    </div>
    <div class="d-flex gap-2 flex-wrap admin-sla-actions">
        <form method="post" action="${pageContext.request.contextPath}/admin/expire"><button class="btn btn-outline-warning btn-sm">处理改约超时</button></form>
        <form method="post" action="${pageContext.request.contextPath}/admin/sla"><button class="btn btn-warning btn-sm">运行 SLA 检查</button></form>
    </div>
</div>

<div class="row g-3 mb-3 admin-metrics">
    <c:forEach items="${stats}" var="x" varStatus="status">
        <div class="col-6 col-md-4 col-xl-3">
            <div class="card border-0 shadow-sm h-100 admin-metric-card admin-metric-${status.index}">
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
        <span class="eyebrow">工程师申请</span>
        <h2 class="h5 mt-1 mb-1">工程师认证审核</h2>
        <p class="text-secondary mb-3">审核通过后，申请人将升级为工程师并获得接单、排班和配件申请能力。</p>
        <div class="admin-application-list">
            <c:forEach items="${applications}" var="a">
                <article class="admin-application-card">
                    <div class="admin-application-summary">
                        <div class="admin-application-field"><span>申请人</span><b><c:out value="${a.real_name}"/></b><small><c:out value="${a.user_name}"/> · <c:out value="${a.created_at}"/></small></div>
                        <div class="admin-application-field"><span>联系方式</span><b><c:out value="${a.phone}"/></b><small><c:out value="${a.id_card_no}"/></small></div>
                        <div class="admin-application-field"><span>服务区域</span><b><c:out value="${a.area_name}"/></b></div>
                        <div class="admin-application-field"><span>从业年限</span><b><c:out value="${a.experience_years}"/> 年</b></div>
                        <div class="admin-application-field"><span>状态</span><div><span class="badge ${a.status == 'PENDING' ? 'text-bg-warning' : (a.status == 'APPROVED' ? 'text-bg-success' : 'text-bg-danger')}"><c:choose><c:when test="${a.status == 'APPROVED'}">已通过</c:when><c:when test="${a.status == 'REJECTED'}">已驳回</c:when><c:otherwise>待审核</c:otherwise></c:choose></span></div></div>
                        <div class="admin-application-action">
                            <span>审核操作</span>
                            <c:choose>
                                <c:when test="${a.status == 'PENDING'}">
                                    <form method="post" action="${pageContext.request.contextPath}/admin/application">
                                        <input type="hidden" name="id" value="${a.application_id}">
                                        <input name="comment" placeholder="审核意见" class="form-control form-control-sm">
                                        <button class="btn btn-outline-danger btn-sm" name="decision" value="reject">驳回</button>
                                        <button class="btn btn-success btn-sm" name="decision" value="approve">通过</button>
                                    </form>
                                </c:when>
                                <c:otherwise><span class="text-secondary small">已完成审核</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <div class="admin-application-details">
                        <div><span>技能</span><div class="chips"><c:forEach items="${applicationSkills}" var="s"><c:if test="${s.application_id == a.application_id}"><span><c:out value="${s.device_name}"/> · <c:out value="${s.fault_name}"/></span></c:if></c:forEach></div><p><c:out value="${a.skill_description}"/></p></div>
                        <div><span>材料说明</span><p><c:out value="${a.material_description}"/></p><c:if test="${not empty a.certificate_no}"><small>证书：<c:out value="${a.certificate_no}"/></small></c:if><c:if test="${not empty a.review_comment}"><small>意见：<c:out value="${a.review_comment}"/></small></c:if></div>
                    </div>
                </article>
            </c:forEach>
            <c:if test="${empty applications}"><div class="admin-empty-state"><strong>暂无工程师认证申请</strong></div></c:if>
        </div>
    </div>
</section>

<section class="card border-0 shadow-sm mb-3">
    <div class="card-body p-4">
        <span class="eyebrow">用户账号</span>
        <h2 class="h5 mt-1 mb-3">账号与工程师资质</h2>
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead><tr><th>账号</th><th>姓名</th><th>角色</th><th>账号状态</th><th>工程师资质</th><th>操作</th></tr></thead>
                <tbody>
                <c:forEach items="${users}" var="x">
                    <tr>
                        <td><c:out value="${x.username}"/></td>
                        <td><c:out value="${x.display_name}"/></td>
                        <td><c:choose><c:when test="${x.role_type == 'ADMIN'}">平台管理员</c:when><c:when test="${x.role_type == 'WAREHOUSE'}">仓库管理员</c:when><c:when test="${x.role_type == 'ENGINEER'}">工程师</c:when><c:otherwise>客户</c:otherwise></c:choose></td>
                        <td><span class="badge ${x.status == 'ACTIVE' ? 'text-bg-success' : 'text-bg-secondary'}">${x.status == 'ACTIVE' ? '正常' : '已停用'}</span></td>
                        <td><c:choose><c:when test="${x.qualification_status == 'APPROVED'}">已通过</c:when><c:when test="${x.qualification_status == 'PENDING'}">待审核</c:when><c:when test="${x.qualification_status == 'REJECTED'}">已驳回</c:when><c:when test="${x.qualification_status == 'EXPIRED'}">已失效</c:when><c:otherwise>—</c:otherwise></c:choose></td>
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
                <div class="admin-config-list">
                    <c:forEach items="${configs}" var="x">
                        <c:choose><c:when test="${x.config_key == 'APPOINTMENT_REMINDER_HOURS'}"><c:set var="configLabel" value="预约临期提醒"/></c:when><c:when test="${x.config_key == 'CANCEL_HOURS'}"><c:set var="configLabel" value="预约取消时限"/></c:when><c:when test="${x.config_key == 'ORDER_SLA_HOURS'}"><c:set var="configLabel" value="工单处理时限"/></c:when><c:when test="${x.config_key == 'PART_REVIEW_SLA_HOURS'}"><c:set var="configLabel" value="配件审核时限"/></c:when><c:when test="${x.config_key == 'RESCHEDULE_HOURS'}"><c:set var="configLabel" value="待改约超时时限"/></c:when><c:otherwise><c:set var="configLabel" value="业务规则"/></c:otherwise></c:choose>
                        <form method="post" action="${pageContext.request.contextPath}/admin/config" class="admin-config-row">
                            <div class="admin-config-meta">
                                <b><c:out value="${configLabel}"/></b>
                                <small class="text-secondary"><c:out value="${x.description}"/></small>
                            </div>
                            <input type="hidden" name="key" value="${x.config_key}">
                            <div class="admin-config-action"><div class="input-group input-group-sm"><input name="value" value="${x.config_value}" required class="form-control"><span class="input-group-text">小时</span></div><button class="btn btn-outline-primary btn-sm">保存</button></div>
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
                        <label class="form-label mb-1">所属设备（仅故障类型需要）<input type="number" name="parentId" placeholder="请输入设备 ID" class="form-control form-control-sm"></label>
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
        <span class="eyebrow">异常预约</span>
        <h2 class="h5 mt-1 mb-3">异常预约</h2>
        <c:choose>
            <c:when test="${empty exceptions}">
                <div class="admin-empty-state"><strong>暂无异常预约</strong><p>当前没有需要管理员处理的改约或异常预约。</p></div>
            </c:when>
            <c:otherwise>
                <div class="table-responsive"><table class="table table-sm table-hover align-middle mb-0"><thead><tr><th>编号</th><th>客户</th><th>工程师</th><th>状态</th><th>异常原因</th><th>更新时间</th></tr></thead><tbody>
                <c:forEach items="${exceptions}" var="x"><c:choose><c:when test="${x.status == 'PENDING_RESCHEDULE'}"><c:set var="exceptionStatusLabel" value="待改约"/></c:when><c:when test="${x.status == 'CANCELLED'}"><c:set var="exceptionStatusLabel" value="已取消"/></c:when><c:when test="${x.status == 'EXPIRED'}"><c:set var="exceptionStatusLabel" value="已过期"/></c:when><c:otherwise><c:set var="exceptionStatusLabel" value="处理中"/></c:otherwise></c:choose><tr><td><c:out value="${x.appointment_no}"/></td><td><c:out value="${x.customer_name}"/></td><td><c:out value="${x.engineer_name}"/></td><td><span class="badge text-bg-warning"><c:out value="${exceptionStatusLabel}"/></span></td><td><c:out value="${x.cancel_reason}"/></td><td><c:out value="${x.updated_at}"/></td></tr></c:forEach>
                </tbody></table></div>
            </c:otherwise>
        </c:choose>
    </div>
</section>

<section class="card border-0 shadow-sm">
    <div class="card-body p-4">
        <span class="eyebrow">操作日志</span>
        <h2 class="h5 mt-1 mb-3">最近操作日志</h2>
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead><tr><th>时间</th><th>操作者</th><th>操作</th><th>业务对象</th><th>说明</th></tr></thead>
                <tbody>
                <c:forEach items="${logs}" var="x">
                    <c:choose><c:when test="${x.operation_type == 'CREATE_APPOINTMENT'}"><c:set var="operationLabel" value="创建预约"/></c:when><c:when test="${x.operation_type == 'CANCEL_APPOINTMENT'}"><c:set var="operationLabel" value="取消预约"/></c:when><c:when test="${x.operation_type == 'ENGINEER_CANCEL'}"><c:set var="operationLabel" value="工程师异常取消"/></c:when><c:when test="${x.operation_type == 'SUBMIT_ENGINEER_APPLICATION'}"><c:set var="operationLabel" value="提交工程师认证"/></c:when><c:when test="${x.operation_type == 'APPROVE_ENGINEER_APPLICATION'}"><c:set var="operationLabel" value="通过工程师认证"/></c:when><c:when test="${x.operation_type == 'REJECT_ENGINEER_APPLICATION'}"><c:set var="operationLabel" value="驳回工程师认证"/></c:when><c:when test="${x.operation_type == 'SET_USER_STATUS'}"><c:set var="operationLabel" value="更新账号状态"/></c:when><c:when test="${x.operation_type == 'SET_QUALIFICATION'}"><c:set var="operationLabel" value="更新工程师资质"/></c:when><c:when test="${x.operation_type == 'UPDATE_CONFIG'}"><c:set var="operationLabel" value="更新业务规则"/></c:when><c:when test="${x.operation_type == 'ADD_BASIC_DATA'}"><c:set var="operationLabel" value="新增基础数据"/></c:when><c:when test="${x.operation_type == 'EXPIRE_RESCHEDULES'}"><c:set var="operationLabel" value="处理改约超时"/></c:when><c:when test="${x.operation_type == 'RUN_SLA'}"><c:set var="operationLabel" value="运行服务时效检查"/></c:when><c:when test="${x.operation_type == 'UPDATE_PROFILE'}"><c:set var="operationLabel" value="更新工程师档案"/></c:when><c:when test="${x.operation_type == 'CREATE_SCHEDULE'}"><c:set var="operationLabel" value="发布可约时段"/></c:when><c:when test="${x.operation_type == 'CLOSE_SCHEDULE'}"><c:set var="operationLabel" value="关闭可约时段"/></c:when><c:when test="${x.operation_type == 'ADD_REPAIR_RECORD'}"><c:set var="operationLabel" value="添加维修记录"/></c:when><c:when test="${x.operation_type == 'ORDER_START'}"><c:set var="operationLabel" value="开始维修"/></c:when><c:when test="${x.operation_type == 'ORDER_WAIT_PARTS'}"><c:set var="operationLabel" value="等待配件"/></c:when><c:when test="${x.operation_type == 'ORDER_RESUME'}"><c:set var="operationLabel" value="恢复维修"/></c:when><c:when test="${x.operation_type == 'ORDER_FINISH'}"><c:set var="operationLabel" value="提交完工"/></c:when><c:when test="${x.operation_type == 'CREATE_PART_REQUEST'}"><c:set var="operationLabel" value="申请配件"/></c:when><c:when test="${x.operation_type == 'APPROVE_PART_REQUEST'}"><c:set var="operationLabel" value="通过配件申请"/></c:when><c:when test="${x.operation_type == 'REJECT_PART_REQUEST'}"><c:set var="operationLabel" value="驳回配件申请"/></c:when><c:when test="${x.operation_type == 'ISSUE_PART'}"><c:set var="operationLabel" value="配件出库"/></c:when><c:when test="${x.operation_type == 'RELEASE_PART'}"><c:set var="operationLabel" value="取消配件申请"/></c:when><c:when test="${x.operation_type == 'RETURN_PART'}"><c:set var="operationLabel" value="配件退回"/></c:when><c:when test="${x.operation_type == 'COMPLETE_PART_REQUEST'}"><c:set var="operationLabel" value="完成配件申请"/></c:when><c:when test="${x.operation_type == 'ACCEPT_ORDER'}"><c:set var="operationLabel" value="提交工单验收"/></c:when><c:when test="${x.operation_type == 'CREATE_REVIEW'}"><c:set var="operationLabel" value="提交服务评价"/></c:when><c:when test="${x.operation_type == 'IN'}"><c:set var="operationLabel" value="补充入库"/></c:when><c:when test="${x.operation_type == 'ADJUST'}"><c:set var="operationLabel" value="库存调整"/></c:when><c:otherwise><c:set var="operationLabel" value="业务操作"/></c:otherwise></c:choose>
                    <c:choose><c:when test="${x.business_type == 'APPOINTMENT'}"><c:set var="businessLabel" value="预约"/></c:when><c:when test="${x.business_type == 'ORDER'}"><c:set var="businessLabel" value="工单"/></c:when><c:when test="${x.business_type == 'ENGINEER'}"><c:set var="businessLabel" value="工程师"/></c:when><c:when test="${x.business_type == 'ENGINEER_APPLICATION'}"><c:set var="businessLabel" value="工程师申请"/></c:when><c:when test="${x.business_type == 'USER'}"><c:set var="businessLabel" value="用户"/></c:when><c:when test="${x.business_type == 'CONFIG'}"><c:set var="businessLabel" value="业务规则"/></c:when><c:when test="${x.business_type == 'SCHEDULE'}"><c:set var="businessLabel" value="排班"/></c:when><c:when test="${x.business_type == 'PART_REQUEST'}"><c:set var="businessLabel" value="配件申请"/></c:when><c:when test="${x.business_type == 'PART'}"><c:set var="businessLabel" value="配件"/></c:when><c:when test="${x.business_type == 'DEVICE'}"><c:set var="businessLabel" value="设备类别"/></c:when><c:when test="${x.business_type == 'FAULT'}"><c:set var="businessLabel" value="故障类别"/></c:when><c:when test="${x.business_type == 'AREA'}"><c:set var="businessLabel" value="服务区域"/></c:when><c:when test="${x.business_type == 'SLOT'}"><c:set var="businessLabel" value="标准时段"/></c:when><c:otherwise><c:set var="businessLabel" value="系统"/></c:otherwise></c:choose>
                    <c:choose><c:when test="${x.operation_type == 'SET_USER_STATUS'}"><c:set var="operationDescription" value="${x.description == 'ACTIVE' ? '账号已启用' : '账号已停用'}"/></c:when><c:when test="${x.operation_type == 'SET_QUALIFICATION'}"><c:choose><c:when test="${x.description == 'APPROVED'}"><c:set var="operationDescription" value="资质审核通过"/></c:when><c:when test="${x.description == 'EXPIRED'}"><c:set var="operationDescription" value="资质已失效"/></c:when><c:otherwise><c:set var="operationDescription" value="资质改为待审核"/></c:otherwise></c:choose></c:when><c:when test="${x.operation_type == 'UPDATE_CONFIG'}"><c:choose><c:when test="${fn:startsWith(x.description, 'APPOINTMENT_REMINDER_HOURS=')}"><c:set var="operationDescription" value="预约临期提醒已更新"/></c:when><c:when test="${fn:startsWith(x.description, 'CANCEL_HOURS=')}"><c:set var="operationDescription" value="预约取消时限已更新"/></c:when><c:when test="${fn:startsWith(x.description, 'ORDER_SLA_HOURS=')}"><c:set var="operationDescription" value="工单处理时限已更新"/></c:when><c:when test="${fn:startsWith(x.description, 'PART_REVIEW_SLA_HOURS=')}"><c:set var="operationDescription" value="配件审核时限已更新"/></c:when><c:when test="${fn:startsWith(x.description, 'RESCHEDULE_HOURS=')}"><c:set var="operationDescription" value="待改约超时时限已更新"/></c:when><c:otherwise><c:set var="operationDescription" value="业务规则已更新"/></c:otherwise></c:choose></c:when><c:when test="${x.operation_type == 'ORDER_START'}"><c:set var="operationDescription" value="工单进入维修中"/></c:when><c:when test="${x.operation_type == 'ORDER_WAIT_PARTS'}"><c:set var="operationDescription" value="工单进入等待配件"/></c:when><c:when test="${x.operation_type == 'ORDER_RESUME'}"><c:set var="operationDescription" value="工单恢复维修"/></c:when><c:when test="${x.operation_type == 'ORDER_FINISH'}"><c:set var="operationDescription" value="工单已提交客户验收"/></c:when><c:otherwise><c:set var="operationDescription" value="${x.description}"/></c:otherwise></c:choose>
                    <tr><td><fmt:formatDate value="${x.created_at}" pattern="yyyy-MM-dd HH:mm"/></td><td><c:out value="${x.display_name}"/></td><td><c:out value="${operationLabel}"/></td><td><c:out value="${businessLabel}"/> #<c:out value="${x.business_id}"/></td><td><c:out value="${operationDescription}"/></td></tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</section>
</div>
<%@ include file="../footer.jspf" %>
