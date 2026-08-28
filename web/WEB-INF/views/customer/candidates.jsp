<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="选择工程师"/>
<%@ include file="../header.jspf" %>

<div class="marketplace-hero">
    <div>
        <span class="eyebrow">STEP 2 OF 3 · ENGINEER MARKET</span>
        <h1>附近可接单工程师</h1>
        <p>AI 已根据 <b><c:out value="${repairRequest.device_name}"/></b> · <b><c:out value="${repairRequest.fault_name}"/></b> · <b><c:out value="${repairRequest.area_name}"/></b> 为你筛选。</p>
    </div>
    <form method="get" class="market-sort">
        <input type="hidden" name="requestId" value="${repairRequest.repair_request_id}">
        <label>排序方式
            <select name="sort" onchange="this.form.submit()">
                <option value="">AI 综合推荐</option>
                <option value="rating" ${param.sort=='rating'?'selected':''}>评分优先</option>
                <option value="service" ${param.sort=='service'?'selected':''}>服务次数优先</option>
                <option value="earliest" ${param.sort=='earliest'?'selected':''}>最快可约</option>
            </select>
        </label>
    </form>
</div>

<div class="dispatch-strip">
    <div><b>已定位</b><span><c:out value="${repairRequest.fault_name}"/></span></div>
    <div><b>服务区域</b><span><c:out value="${repairRequest.area_name}"/></span></div>
    <div><b>派单模式</b><span>用户自主选择 + 平台智能排序</span></div>
    <div><b>保障</b><span>认证工程师 / 可改约 / 可评价</span></div>
</div>

<c:choose>
    <c:when test="${empty candidates}">
        <div class="empty">
            <b>暂时没有可接单工程师</b>
            <span>可以返回修改服务区域、期望日期或时段，系统会重新搜索可用工程师。</span>
            <a class="btn" href="${pageContext.request.contextPath}/customer/request">重新搜索问题</a>
        </div>
    </c:when>
    <c:otherwise>
        <div class="engineer-market-grid">
            <c:forEach items="${candidates}" var="x" varStatus="s">
                <article class="engineer-market-card ${s.index == 0 ? 'recommended' : ''}">
                    <div class="engineer-card-top">
                        <div class="avatar"><c:out value="${x.engineer_name.substring(0,1)}"/></div>
                        <div>
                            <div class="candidate-title">
                                <h2><c:out value="${x.engineer_name}"/></h2>
                                <span class="rating">★ <c:out value="${x.average_rating}"/></span>
                            </div>
                            <p><c:out value="${x.bio}"/></p>
                        </div>
                    </div>
                    <c:if test="${s.index == 0}">
                        <div class="ai-pick">AI 推荐：综合评分、履约率和可预约时间最匹配</div>
                    </c:if>
                    <div class="delivery-style-meta">
                        <div><b><c:out value="${x.service_date}"/></b><span>可预约日期</span></div>
                        <div><b><c:out value="${x.slot_name}"/></b><span><c:out value="${x.start_time}"/>—<c:out value="${x.end_time}"/></span></div>
                        <div><b><c:out value="${x.completed_count}"/> 单</b><span>历史服务</span></div>
                    </div>
                    <div class="chips">
                        <span><c:out value="${x.skill_name}"/></span>
                        <span>履约 <c:out value="${x.fulfillment_rate}"/>%</span>
                        <span><c:out value="${x.review_count}"/> 条评价</span>
                        <span>可接单</span>
                    </div>
                    <div class="market-card-action">
                        <span>预约后生成维修订单，工程师按时上门。</span>
                        <form method="post" action="${pageContext.request.contextPath}/customer/book" data-confirm="确认选择该工程师和时段？预约提交后立即生效。">
                            <input type="hidden" name="requestId" value="${repairRequest.repair_request_id}">
                            <input type="hidden" name="engineerId" value="${x.engineer_id}">
                            <input type="hidden" name="scheduleId" value="${x.schedule_id}">
                            <input type="hidden" name="replacesId" value="${param.replacesId}">
                            <button class="btn primary">预约这位工程师</button>
                        </form>
                    </div>
                </article>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="../footer.jspf" %>
