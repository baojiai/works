<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="AI 报修"/>
<%@ include file="../header.jspf" %>

<div class="page-head">
    <div>
        <span class="eyebrow">STEP 1 OF 3 · AI SERVICE MATCH</span>
        <h1>像点外卖一样预约维修工程师</h1>
        <p>不用先选择复杂故障类型，先搜索你遇到的问题，平台 AI 会帮你判断方向并推荐可接单工程师。</p>
    </div>
</div>

<c:if test="${not empty error}">
    <div class="alert danger"><c:out value="${error}"/></div>
</c:if>

<form method="post" class="repair-market-form" data-ai-repair-form data-ai-endpoint="${pageContext.request.contextPath}/api/ai/diagnose">
    <section class="repair-search-shell">
        <div class="repair-search-main">
            <span class="eyebrow">专属 AI 诊断助手</span>
            <h2>先告诉我：设备哪里不舒服？</h2>
            <p>例如：电脑开不了机、打印机一直卡纸、冰箱不制冷、系统蓝屏、家电异响。</p>
            <div class="repair-search-box">
                <input name="problemQuery" value="${param.problemQuery}" data-problem-search placeholder="搜索你的问题，例如“电脑打不开”“打印不了”“冰箱不制冷”">
                <button type="button" class="btn primary" data-ai-diagnose>AI 诊断</button>
            </div>
            <div class="search-examples">
                <button type="button" data-example="电脑无法开机">电脑无法开机</button>
                <button type="button" data-example="系统很卡或蓝屏">系统很卡或蓝屏</button>
                <button type="button" data-example="打印机不能打印">打印机不能打印</button>
                <button type="button" data-example="冰箱不制冷">冰箱不制冷</button>
            </div>
        </div>
        <aside class="ai-assistant-card" data-ai-result>
            <div class="ai-orb">AI</div>
            <h3>等待你的问题</h3>
            <p>输入问题后，我会根据平台服务能力推断故障大类、提醒可先检查的事项，并自动为下一步筛选工程师。</p>
            <ol>
                <li>理解问题描述</li>
                <li>定位维修类型</li>
                <li>推荐附近可预约工程师</li>
            </ol>
        </aside>
    </section>

    <section class="card issue-picker">
        <div class="section-head">
            <div>
                <span class="eyebrow">AI MATCHED SERVICES</span>
                <h2>AI 生成的服务词条</h2>
                <p>这些词条会根据你上方输入的问题实时生成；带“可预约工程师”的词条可以继续进入工程师市场。</p>
            </div>
            <span class="badge" data-selected-issue>等待输入问题</span>
        </div>
        <div class="issue-card-grid">
            <c:forEach items="${faults}" var="x" varStatus="s">
                <button type="button" class="issue-card" data-issue-card
                        data-device-id="${x.device_type_id}"
                        data-fault-id="${x.fault_type_id}"
                        data-device-name="${x.device_name}"
                        data-fault-name="${x.name}">
                    <span class="issue-icon">${s.index == 0 ? '⏻' : s.index == 1 ? '⚙' : s.index == 2 ? '🖨' : '❄'}</span>
                    <b><c:out value="${x.name}"/></b>
                    <small><c:out value="${x.device_name}"/> · 平台认证工程师可接单</small>
                </button>
            </c:forEach>
        </div>
    </section>

    <section class="card service-order-card">
        <div class="section-head">
            <div>
                <span class="eyebrow">SERVICE ORDER</span>
                <h2>填写上门信息</h2>
                <p>这些信息会像外卖订单地址一样，用于筛选服务区域、预约时间和工程师可达性。</p>
            </div>
        </div>
        <div class="hidden-routing-fields" aria-hidden="true">
            <select name="deviceId" required>
                <option value="">请选择</option>
                <c:forEach items="${devices}" var="x">
                    <option value="${x.device_type_id}" ${param.deviceId == x.device_type_id ? 'selected' : ''}><c:out value="${x.name}"/></option>
                </c:forEach>
            </select>
            <select name="faultId" required>
                <option value="">请选择</option>
                <c:forEach items="${faults}" var="x">
                    <option value="${x.fault_type_id}" data-device="${x.device_type_id}" ${param.faultId == x.fault_type_id ? 'selected' : ''}><c:out value="${x.name}"/></option>
                </c:forEach>
            </select>
        </div>
        <div class="form-grid">
            <label>服务区域
                <select name="areaId" required>
                    <option value="">选择所在区域</option>
                    <c:forEach items="${areas}" var="x">
                        <option value="${x.service_area_id}" ${param.areaId == x.service_area_id ? 'selected' : ''}><c:out value="${x.name}"/></option>
                    </c:forEach>
                </select>
            </label>
            <label>期望日期
                <input type="date" name="expectedDate" value="${param.expectedDate}" required>
            </label>
            <label>期望时段
                <select name="slotId">
                    <option value="">不限时段，优先最快可达</option>
                    <c:forEach items="${slots}" var="x">
                        <option value="${x.slot_id}" ${param.slotId == x.slot_id ? 'selected' : ''}><c:out value="${x.name}"/> · <c:out value="${x.start_time}"/>—<c:out value="${x.end_time}"/></option>
                    </c:forEach>
                </select>
            </label>
            <label>联系电话
                <input name="phone" value="${param.phone}" required placeholder="工程师接单后用于联系">
            </label>
            <label class="wide">上门地址
                <input name="address" value="${param.address}" required placeholder="小区/楼栋/门牌号，越清楚越容易快速接单">
            </label>
            <label class="wide">补充描述
                <textarea name="description" required rows="4" placeholder="可以补充：什么时候开始、是否有异响/报错、是否尝试重启等"><c:out value="${param.description}"/></textarea>
            </label>
        </div>
        <div class="service-flow">
            <span><b>1</b> AI 定位问题</span>
            <i></i>
            <span><b>2</b> 展示可接单工程师</span>
            <i></i>
            <span><b>3</b> 预约上门服务</span>
        </div>
        <div class="form-actions">
            <button class="btn primary">查看可接单工程师 →</button>
        </div>
    </section>
</form>

<%@ include file="../footer.jspf" %>
