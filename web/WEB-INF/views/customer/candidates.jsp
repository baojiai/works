<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="选择工程师"/>
<%@ include file="../header.jspf" %>

<div class="page-head">
    <div>
        <span class="eyebrow">STEP 2 OF 3 · ENGINEER MARKET</span>
        <h1>附近可接单工程师</h1>
        <p>AI 已根据 <b><c:out value="${repairRequest.device_name}"/></b> · <b><c:out value="${repairRequest.fault_name}"/></b> · <b><c:out value="${repairRequest.area_name}"/></b> 为你筛选。</p>
    </div>
    <div class="d-flex align-items-center gap-3">
        <form method="get" class="d-flex align-items-center gap-2">
            <input type="hidden" name="requestId" value="${repairRequest.repair_request_id}">
            <label class="form-label mb-0 text-nowrap">排序方式
                <select name="sort" onchange="this.form.submit()" class="form-select form-select-sm d-inline-block w-auto ms-2">
                    <option value="">AI 综合推荐</option>
                    <option value="rating" ${param.sort=='rating'?'selected':''}>评分优先</option>
                    <option value="service" ${param.sort=='service'?'selected':''}>服务次数优先</option>
                    <option value="earliest" ${param.sort=='earliest'?'selected':''}>最快可约</option>
                </select>
            </label>
        </form>
    </div>
</div>

<div class="dispatch-strip">
    <div><b>当前故障</b><span>故障匹配 / <c:out value="${repairRequest.fault_name}"/></span></div>
    <div><b>服务区域</b><span><c:out value="${repairRequest.area_name}"/></span></div>
    <div><b>派单模式</b><span>用户自主选择 + 平台智能排序</span></div>
    <div><b>保障</b><span>认证工程师 / 可改约 / 可评价</span></div>
</div>

<c:choose>
    <c:when test="${empty candidates}">
        <div class="card text-center py-5">
            <div class="card-body">
                <h5 class="card-title">暂时没有可接单工程师</h5>
                <p class="card-text text-secondary">可以返回修改服务区域、期望日期或时段，系统会重新搜索可用工程师。</p>
                <a class="btn btn-primary" href="${pageContext.request.contextPath}/customer/request">重新搜索问题</a>
            </div>
        </div>
    </c:when>
    <c:otherwise>
        <div class="row g-3 justify-content-center">
            <c:forEach items="${candidates}" var="x" varStatus="s">
                <div class="col-12 col-md-6 col-xl-4">
                    <article class="card h-100 border-0 shadow-sm candidate-card ${s.index == 0 ? 'recommended' : ''}">
                        <div class="card-body p-4 d-flex flex-column">
                            <div class="d-flex gap-3 candidate-identity">
                                <div class="avatar"><c:out value="${x.engineer_name.substring(0,1)}"/></div>
                                <div class="min-w-0">
                                    <div class="d-flex align-items-center gap-2">
                                        <h2 class="h5 mb-0"><c:out value="${x.engineer_name}"/></h2>
                                        <span class="rating">★ <c:out value="${x.average_rating}"/></span>
                                    </div>
                                    <p class="candidate-bio"><c:out value="${x.bio}"/></p>
                                </div>
                            </div>
                            <c:if test="${s.index == 0}">
                                <div class="ai-pick">AI 推荐：综合评分、履约率和可预约时间最匹配</div>
                            </c:if>
                            <div class="candidate-slot">
                                <div><span>可预约日期</span><b><c:out value="${x.service_date}"/></b></div>
                                <div><span><c:out value="${x.slot_name}"/></span><b><c:out value="${x.start_time}"/>—<c:out value="${x.end_time}"/></b></div>
                            </div>
                            <div class="candidate-metrics">
                                <div><b><c:out value="${x.completed_count}"/></b><span>历史服务</span></div>
                                <div><b><c:out value="${x.fulfillment_rate}"/>%</b><span>履约率</span></div>
                                <div><b><c:out value="${x.review_count}"/></b><span>客户评价</span></div>
                            </div>
                            <div class="chips candidate-skills"><span><c:out value="${x.skill_name}"/></span></div>
                            <div class="candidate-action mt-auto">
                                <form method="post" action="${pageContext.request.contextPath}/customer/book" data-confirm="确认选择该工程师和时段？预约提交后立即生效。">
                                    <input type="hidden" name="requestId" value="${repairRequest.repair_request_id}">
                                    <input type="hidden" name="engineerId" value="${x.engineer_id}">
                                    <input type="hidden" name="scheduleId" value="${x.schedule_id}">
                                    <input type="hidden" name="replacesId" value="${param.replacesId}">
                                    <button class="btn btn-primary w-100">预约这位工程师</button>
                                </form>
                            </div>
                        </div>
                    </article>
                </div>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="../footer.jspf" %>
