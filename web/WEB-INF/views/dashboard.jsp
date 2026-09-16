<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="工作台"/>
<%@ include file="header.jspf" %>

<section class="hero dashboard-hero">
    <div>
        <span class="eyebrow">今日工作台</span>
        <h1>你好，<c:out value="${sessionScope.user.displayName}"/></h1>
        <p>当前身份：<span class="badge text-bg-light border"><c:out value="${sessionScope.user.roleLabel}"/></span>。<c:choose><c:when test="${sessionScope.user.role == 'CUSTOMER'}">可在线报修、预约工程师并跟踪维修进度。</c:when><c:when test="${sessionScope.user.role == 'ENGINEER'}">工程师账号同时保留客户能力，可继续预约其他工程师。</c:when><c:when test="${sessionScope.user.role == 'WAREHOUSE'}">可审核配件申请、办理出入库并追踪库存流水。</c:when><c:otherwise>可管理平台账号、资质、规则与异常记录。</c:otherwise></c:choose></p>
    </div>
    <div class="d-flex gap-2 flex-wrap">
        <c:choose>
            <c:when test="${sessionScope.user.role == 'ENGINEER'}">
                <a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/customer/request">我要找工程师</a>
                <a class="btn btn-primary" href="${pageContext.request.contextPath}/engineer/schedule">发布可约时段</a>
            </c:when>
            <c:when test="${sessionScope.user.role == 'CUSTOMER'}">
                <a class="btn btn-primary" href="${pageContext.request.contextPath}/customer/request">+ 发起维修需求</a>
            </c:when>
            <c:when test="${sessionScope.user.role == 'WAREHOUSE'}">
                <a class="btn btn-primary" href="${pageContext.request.contextPath}/warehouse/requests">处理配件申请</a>
            </c:when>
            <c:otherwise>
                <a class="btn btn-primary" href="${pageContext.request.contextPath}/admin">进入系统管理</a>
            </c:otherwise>
        </c:choose>
    </div>
</section>

<div class="row g-3 mt-1 dashboard-metrics">
    <c:forEach items="${summary}" var="item">
        <div class="col-6 col-lg-3">
            <div class="card border-0 shadow-sm h-100">
                <div class="card-body p-3">
                    <span class="text-secondary small"><c:out value="${item.key}"/></span>
                    <strong class="d-block fs-2 mt-1"><c:out value="${item.value}"/></strong>
                </div>
            </div>
        </div>
    </c:forEach>
</div>

<section class="card border-0 shadow-sm mt-3 dashboard-overview">
    <div class="card-body p-4 d-flex flex-column flex-lg-row justify-content-between gap-4">
        <div>
            <span class="eyebrow">平台概览</span>
            <h2 class="h5 mt-1 mb-2">从用户报修到工程师接单，再到仓库配件协同</h2>
            <p class="text-secondary mb-3">RepairFlow 将客户报修、工程师预约、维修服务、配件协同与验收评价串联为统一服务流程。</p>
            <div class="chips"><span>在线报修</span><span>工程师预约</span><span>工程师认证</span><span>配件协同</span></div>
        </div>
        <img src="${pageContext.request.contextPath}/assets/images/ops-dashboard.png" alt="平台运营中心插画" class="d-none d-md-block" style="max-height:150px">
    </div>
</section>

<section class="card border-0 shadow-sm mt-3 dashboard-actions">
    <div class="card-body p-4">
        <div class="mb-3">
            <span class="eyebrow">常用事项</span>
            <h2 class="h5 mt-1 mb-0">常用事项</h2>
        </div>
        <div class="row g-3">
            <c:if test="${sessionScope.user.role == 'CUSTOMER' || sessionScope.user.role == 'ENGINEER'}">
                <div class="col-12 col-md-4">
                    <a class="card border-0 shadow-sm text-decoration-none h-100" href="${pageContext.request.contextPath}/customer/request"><div class="card-body"><b class="text-primary small">在线报修</b><strong class="d-block my-1">提交报修并预约</strong><span class="text-secondary small">填写设备故障信息，选择可接单工程师</span></div></a>
                </div>
                <div class="col-12 col-md-4">
                    <a class="card border-0 shadow-sm text-decoration-none h-100" href="${pageContext.request.contextPath}/appointments"><div class="card-body"><b class="text-primary small">预约安排</b><strong class="d-block my-1">我的预约</strong><span class="text-secondary small">查看自己预约的服务，也查看自己接到的工程师服务安排</span></div></a>
                </div>
                <c:if test="${sessionScope.user.role == 'CUSTOMER'}">
                    <div class="col-12 col-md-4">
                        <a class="card border-0 shadow-sm text-decoration-none h-100" href="${pageContext.request.contextPath}/engineer/apply"><div class="card-body"><b class="text-primary small">能力认证</b><strong class="d-block my-1">申请工程师认证</strong><span class="text-secondary small">提交身份、技能、资质材料，审核通过后接单</span></div></a>
                    </div>
                </c:if>
            </c:if>
            <c:if test="${sessionScope.user.role == 'ENGINEER'}">
                <div class="col-12 col-md-4">
                    <a class="card border-0 shadow-sm text-decoration-none h-100" href="${pageContext.request.contextPath}/engineer/profile"><div class="card-body"><b class="text-primary small">技能档案</b><strong class="d-block my-1">维护工程师档案</strong><span class="text-secondary small">管理技能、区域和公开简介</span></div></a>
                </div>
                <div class="col-12 col-md-4">
                    <a class="card border-0 shadow-sm text-decoration-none h-100" href="${pageContext.request.contextPath}/engineer/schedule"><div class="card-body"><b class="text-primary small">排班时间</b><strong class="d-block my-1">发布可约时段</strong><span class="text-secondary small">设置自己可以接单的日期和时间</span></div></a>
                </div>
                <div class="col-12 col-md-4">
                    <a class="card border-0 shadow-sm text-decoration-none h-100" href="${pageContext.request.contextPath}/orders"><div class="card-body"><b class="text-primary small">服务工单</b><strong class="d-block my-1">维修工单</strong><span class="text-secondary small">同时查看自己发起的工单和自己负责的工单</span></div></a>
                </div>
            </c:if>
            <c:if test="${sessionScope.user.role == 'WAREHOUSE'}">
                <div class="col-12 col-md-4">
                    <a class="card border-0 shadow-sm text-decoration-none h-100" href="${pageContext.request.contextPath}/warehouse/requests"><div class="card-body"><b class="text-primary small">配件审核</b><strong class="d-block my-1">申请审核</strong><span class="text-secondary small">整单审核并事务锁定库存</span></div></a>
                </div>
                <div class="col-12 col-md-4">
                    <a class="card border-0 shadow-sm text-decoration-none h-100" href="${pageContext.request.contextPath}/warehouse/requests"><div class="card-body"><b class="text-primary small">库存流转</b><strong class="d-block my-1">出库与退回</strong><span class="text-secondary small">核对锁定量、出库量和可退数量</span></div></a>
                </div>
                <div class="col-12 col-md-4">
                    <a class="card border-0 shadow-sm text-decoration-none h-100" href="${pageContext.request.contextPath}/warehouse/inventory"><div class="card-body"><b class="text-primary small">库存管理</b><strong class="d-block my-1">库存流水</strong><span class="text-secondary small">补充入库、盘点调整与追溯</span></div></a>
                </div>
            </c:if>
            <c:if test="${sessionScope.user.role == 'ADMIN'}">
                <div class="col-12 col-md-4">
                    <a class="card border-0 shadow-sm text-decoration-none h-100" href="${pageContext.request.contextPath}/admin"><div class="card-body"><b class="text-primary small">用户管理</b><strong class="d-block my-1">账号与资质</strong><span class="text-secondary small">停用账号并管理工程师资质</span></div></a>
                </div>
                <div class="col-12 col-md-4">
                    <a class="card border-0 shadow-sm text-decoration-none h-100" href="${pageContext.request.contextPath}/admin"><div class="card-body"><b class="text-primary small">业务规则</b><strong class="d-block my-1">规则与 SLA</strong><span class="text-secondary small">配置取消、改约和提醒阈值</span></div></a>
                </div>
                <div class="col-12 col-md-4">
                    <a class="card border-0 shadow-sm text-decoration-none h-100" href="${pageContext.request.contextPath}/admin"><div class="card-body"><b class="text-primary small">平台日志</b><strong class="d-block my-1">异常与日志</strong><span class="text-secondary small">处理改约超时并审计关键操作</span></div></a>
                </div>
            </c:if>
        </div>
    </div>
</section>

<%@ include file="footer.jspf" %>
