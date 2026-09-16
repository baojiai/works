<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="操作提示"/>
<%@ include file="header.jspf" %>

<div class="error-state">
    <section class="card border-0 shadow-sm error-card">
        <div class="card-body p-4 p-md-5 text-center">
            <div class="error-hint-icon" aria-hidden="true">!</div>
            <h1 class="h4 mb-3">操作未完成</h1>
            <p class="text-secondary mb-4"><c:out value="${not empty error ? error : '请求不存在或系统暂时无法处理。'}"/></p>
            <div class="d-flex gap-2 justify-content-center flex-wrap">
                <button class="btn btn-outline-secondary" type="button" onclick="history.back()">返回上一页</button>
                <a class="btn btn-primary" href="${pageContext.request.contextPath}/dashboard">回到工作台</a>
            </div>
        </div>
    </section>
</div>

<%@ include file="footer.jspf" %>
