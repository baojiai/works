<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><c:set var="pageTitle" value="排班管理"/><%@ include file="../header.jspf" %>
<div class="page-head"><div><span class="eyebrow">AVAILABILITY</span><h1>排班与可预约时段</h1><p>只能发布系统预设的标准时段；已占用时段不能直接关闭。</p></div></div>
<section class="card border-0 shadow-sm mb-3">
    <div class="card-body p-4">
        <form method="post" class="row g-3 align-items-end">
            <div class="col-12 col-md-4">
                <label class="form-label">服务日期<input type="date" name="serviceDate" required class="form-control"></label>
            </div>
            <div class="col-12 col-md-5">
                <label class="form-label">标准时段<select name="slotId" required class="form-select"><c:forEach items="${slots}" var="x"><option value="${x.slot_id}"><c:out value="${x.name}"/> · <c:out value="${x.start_time}"/>—<c:out value="${x.end_time}"/></option></c:forEach></select></label>
            </div>
            <div class="col-12 col-md-3">
                <button class="btn btn-primary w-100">发布时段</button>
            </div>
        </form>
    </div>
</section>
<c:choose>
    <c:when test="${empty schedules}">
        <div class="card text-center py-5"><div class="card-body text-secondary">暂无已发布时段，请先在上方发布可预约日期。</div></div>
    </c:when>
    <c:otherwise>
        <div class="table-responsive">
            <table class="table table-hover align-middle shadow-sm mb-0">
                <thead>
                <tr>
                    <th>服务日期</th>
                    <th>时段</th>
                    <th>时间</th>
                    <th>状态</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${schedules}" var="x">
                    <tr>
                        <td><b><c:out value="${x.service_date}"/></b></td>
                        <td><c:out value="${x.slot_name}"/></td>
                        <td><c:out value="${x.start_time}"/>—<c:out value="${x.end_time}"/></td>
                        <td><span class="badge ${x.status == 'AVAILABLE' ? 'text-bg-success' : (x.status == 'OCCUPIED' || x.status == 'BOOKED' ? 'text-bg-primary' : 'text-bg-secondary')}"><c:out value="${x.status}"/></span></td>
                        <td>
                            <c:if test="${x.status == 'AVAILABLE'}">
                                <form method="post" action="${pageContext.request.contextPath}/engineer/schedule/close" data-confirm="确认关闭这个空闲时段？" class="d-flex gap-2 flex-wrap">
                                    <input type="hidden" name="id" value="${x.schedule_id}">
                                    <button class="btn btn-outline-secondary btn-sm">关闭</button>
                                </form>
                            </c:if>
                            <c:if test="${x.status != 'AVAILABLE'}"><span class="text-secondary small">—</span></c:if>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:otherwise>
</c:choose>
<%@ include file="../footer.jspf" %>
