<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="AI 报修"/>
<%@ include file="../header.jspf" %>

<div class="page-head">
    <div>
        <span class="eyebrow">STEP 1 OF 3 · AI SERVICE MATCH</span>
        <h1>像点外卖一样预约维修工程师</h1>
        <p>不用先选择复杂故障类型。你只要描述问题，AI 会生成服务词条，并匹配可预约工程师。</p>
    </div>
    <div class="page-head-art d-none d-lg-block">
        <img src="${pageContext.request.contextPath}/assets/images/hero-service.png" alt="AI 维修预约插画">
    </div>
</div>

<c:if test="${not empty error}">
    <div class="alert alert-danger d-flex align-items-center gap-2"><c:out value="${error}"/></div>
</c:if>

<form method="post" class="repair-market-form" data-ai-repair-form data-ai-endpoint="${pageContext.request.contextPath}/api/ai/diagnose">
    <div class="row g-3">
        <section class="col-12 col-lg-8">
            <div class="card h-100 border-0 shadow-sm">
                <div class="card-body p-4">
                    <span class="eyebrow">专属 AI 诊断助手</span>
                    <h2 class="h4 mt-1 mb-2">先告诉我：设备哪里不舒服？</h2>
                    <p class="text-secondary">例如：电饭煲打不开、微波炉不加热、打印机一直卡纸、冰箱不制冷、电脑蓝屏重启。</p>
                    <div class="input-group input-group-lg mb-3">
                        <input name="problemQuery" value="${param.problemQuery}" data-problem-search class="form-control" placeholder="搜索你的问题，例如“微波炉加热有问题”“电饭煲打不开”">
                        <button type="button" class="btn btn-primary px-4" data-ai-diagnose>AI 诊断</button>
                    </div>
                    <div class="d-flex flex-wrap gap-2">
                        <button type="button" class="btn btn-outline-secondary btn-sm" data-example="微波炉加热有问题，食物加热很久也不热">微波炉不加热</button>
                        <button type="button" class="btn btn-outline-secondary btn-sm" data-example="电饭煲打不开，插电后没有反应">电饭煲打不开</button>
                        <button type="button" class="btn btn-outline-secondary btn-sm" data-example="打印机一直卡纸不能打印">打印机卡纸</button>
                        <button type="button" class="btn btn-outline-secondary btn-sm" data-example="电脑蓝屏并且频繁重启">电脑蓝屏重启</button>
                    </div>
                </div>
            </div>
        </section>
        <aside class="col-12 col-lg-4">
            <div class="card h-100 border-0 bg-light" data-ai-result>
                <div class="card-body p-4">
                    <div class="ai-orb">AI</div>
                    <h3 class="h5 mt-3">等待你的问题</h3>
                    <p class="text-secondary">输入问题后，我会生成与当前问题相关的服务词条，再判断哪些词条能进入工程师预约市场。</p>
                    <ol class="small text-secondary mb-0">
                        <li>理解问题描述</li>
                        <li>动态生成服务词条</li>
                        <li>匹配附近可预约工程师</li>
                    </ol>
                </div>
            </div>
        </aside>
    </div>

    <section class="card border-0 shadow-sm mt-3">
        <div class="card-body p-4">
            <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-3">
                <div>
                    <span class="eyebrow">AI MATCHED SERVICES</span>
                    <h2 class="h5 mt-1 mb-1">AI 生成的服务词条</h2>
                    <p class="text-secondary mb-0">这些词条会根据上方输入的问题实时生成；带“可预约工程师”的词条可以继续进入工程师市场。</p>
                </div>
                <span class="badge text-bg-secondary" data-selected-issue>等待输入问题</span>
            </div>
            <div class="ai-service-empty" data-ai-service-empty>
                <span>AI</span>
                <b>还没有生成推荐服务</b>
                <small>请先在上方输入设备问题并点击“AI 诊断”，系统会根据问题生成可选择的服务词条。</small>
            </div>
            <div class="issue-card-grid" data-ai-service-grid></div>
            <div class="platform-catalog" hidden aria-hidden="true">
                <c:forEach items="${faults}" var="x" varStatus="s">
                    <span data-issue-card
                            data-device-id="${x.device_type_id}"
                            data-fault-id="${x.fault_type_id}"
                            data-device-name="${x.device_name}"
                            data-fault-name="${x.name}"></span>
                </c:forEach>
            </div>
        </div>
    </section>

    <section class="card border-0 shadow-sm mt-3">
        <div class="card-body p-4">
            <div class="mb-3">
                <span class="eyebrow">SERVICE ORDER</span>
                <h2 class="h5 mt-1 mb-1">填写上门信息</h2>
                <p class="text-secondary mb-0">这些信息会像外卖订单地址一样，用于筛选服务区域、预约时间和工程师可达性。</p>
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
            <div class="row g-3">
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
                    <label class="form-label">期望日期
                        <input type="date" name="expectedDate" value="${param.expectedDate}" required class="form-control">
                    </label>
                </div>
                <div class="col-12 col-md-6">
                    <label class="form-label">期望时段
                        <select name="slotId" class="form-select">
                            <option value="">不限时段，优先最快可达</option>
                            <c:forEach items="${slots}" var="x">
                                <option value="${x.slot_id}" ${param.slotId == x.slot_id ? 'selected' : ''}><c:out value="${x.name}"/> · <c:out value="${x.start_time}"/>—<c:out value="${x.end_time}"/></option>
                            </c:forEach>
                        </select>
                    </label>
                </div>
                <div class="col-12 col-md-6">
                    <label class="form-label">联系电话
                        <input name="phone" value="${param.phone}" required placeholder="工程师接单后用于联系" class="form-control">
                    </label>
                </div>
                <div class="col-12">
                    <label class="form-label">上门地址
                        <input name="address" value="${param.address}" required placeholder="小区/楼栋/门牌号，越清楚越容易快速接单" class="form-control">
                    </label>
                </div>
                <div class="col-12">
                    <label class="form-label">补充描述
                        <textarea name="description" required rows="4" placeholder="可以补充：什么时候开始、是否有异响/报错、是否尝试重启等" class="form-control"><c:out value="${param.description}"/></textarea>
                    </label>
                </div>
            </div>
            <div class="service-flow">
                <span><b>1</b> AI 定位问题</span>
                <i></i>
                <span><b>2</b> 展示可接单工程师</span>
                <i></i>
                <span><b>3</b> 预约上门服务</span>
            </div>
            <div class="d-flex justify-content-end">
                <button class="btn btn-primary px-4">查看可接单工程师 →</button>
            </div>
        </div>
    </section>
</form>

<%@ include file="../footer.jspf" %>
