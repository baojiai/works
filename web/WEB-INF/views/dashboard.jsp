<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="工作台"/>
<%@ include file="header.jspf" %>

<section class="hero">
    <div>
        <span class="eyebrow">TODAY'S WORKSPACE</span>
        <h1>你好，<c:out value="${sessionScope.user.displayName}"/></h1>
        <p>当前身份：<span class="badge"><c:out value="${sessionScope.user.roleLabel}"/></span>。工程师账号仍然保留客户能力，可以继续预约其他工程师。</p>
    </div>
    <c:choose>
        <c:when test="${sessionScope.user.role == 'ENGINEER'}">
            <div class="actions">
                <a class="btn" href="${pageContext.request.contextPath}/customer/request">我要找工程师</a>
                <a class="btn primary" href="${pageContext.request.contextPath}/engineer/schedule">发布可约时段</a>
            </div>
        </c:when>
        <c:when test="${sessionScope.user.role == 'CUSTOMER'}">
            <a class="btn primary" href="${pageContext.request.contextPath}/customer/request">+ 发起维修需求</a>
        </c:when>
        <c:when test="${sessionScope.user.role == 'WAREHOUSE'}">
            <a class="btn primary" href="${pageContext.request.contextPath}/warehouse/requests">处理配件申请</a>
        </c:when>
        <c:otherwise>
            <a class="btn primary" href="${pageContext.request.contextPath}/admin">进入系统管理</a>
        </c:otherwise>
    </c:choose>
</section>

<section class="metric-grid">
    <c:forEach items="${summary}" var="item">
        <article class="metric"><span><c:out value="${item.key}"/></span><strong><c:out value="${item.value}"/></strong><i></i></article>
    </c:forEach>
</section>

<section class="card">
    <div class="section-head">
        <div><span class="eyebrow">TASK BOARD</span><h2>常用事项</h2></div>
    </div>
    <div class="quick-grid">
        <c:if test="${sessionScope.user.role == 'CUSTOMER' || sessionScope.user.role == 'ENGINEER'}">
            <a href="${pageContext.request.contextPath}/customer/request"><b>AI</b><strong>搜索问题并预约</strong><span>专属 AI 诊断问题，像点外卖一样选择可接单工程师</span></a>
            <a href="${pageContext.request.contextPath}/appointments"><b>PLAN</b><strong>我的预约</strong><span>查看自己预约的服务，也查看自己接到的工程师服务安排</span></a>
            <c:if test="${sessionScope.user.role == 'CUSTOMER'}">
                <a href="${pageContext.request.contextPath}/engineer/apply"><b>CERT</b><strong>申请工程师认证</strong><span>提交身份、技能、资质材料，审核通过后接单</span></a>
            </c:if>
        </c:if>
        <c:if test="${sessionScope.user.role == 'ENGINEER'}">
            <a href="${pageContext.request.contextPath}/engineer/profile"><b>SKILL</b><strong>维护工程师档案</strong><span>管理技能、区域和公开简介</span></a>
            <a href="${pageContext.request.contextPath}/engineer/schedule"><b>TIME</b><strong>发布可约时段</strong><span>设置自己可以接单的日期和时间</span></a>
            <a href="${pageContext.request.contextPath}/orders"><b>ORDER</b><strong>维修工单</strong><span>同时查看自己发起的工单和自己负责的工单</span></a>
        </c:if>
        <c:if test="${sessionScope.user.role == 'WAREHOUSE'}">
            <a href="${pageContext.request.contextPath}/warehouse/requests"><b>CHECK</b><strong>申请审核</strong><span>整单审核并事务锁定库存</span></a>
            <a href="${pageContext.request.contextPath}/warehouse/requests"><b>ISSUE</b><strong>出库与退回</strong><span>核对锁定量、出库量和可退数量</span></a>
            <a href="${pageContext.request.contextPath}/warehouse/inventory"><b>STOCK</b><strong>库存流水</strong><span>补充入库、盘点调整与追溯</span></a>
        </c:if>
        <c:if test="${sessionScope.user.role == 'ADMIN'}">
            <a href="${pageContext.request.contextPath}/admin"><b>USER</b><strong>账号与资质</strong><span>停用账号并管理工程师资质</span></a>
            <a href="${pageContext.request.contextPath}/admin"><b>RULE</b><strong>规则与 SLA</strong><span>配置取消、改约和提醒阈值</span></a>
            <a href="${pageContext.request.contextPath}/admin"><b>LOG</b><strong>异常与日志</strong><span>处理改约超时并审计关键操作</span></a>
        </c:if>
    </div>
</section>

<%@ include file="footer.jspf" %>
