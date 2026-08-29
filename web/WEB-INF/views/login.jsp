<%@ page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="theme-color" content="#f6f8fb">
    <title><c:out value="${editionName}"/> · RepairFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app.css?v=12">
</head>
<body class="login-page edition-${edition}">
    <section class="login-visual">
        <div class="login-art">
            <div class="visual-brand"><span class="brand-mark">R</span><span><strong>RepairFlow</strong><small>AFTER-SALES SERVICE PLATFORM</small></span></div>
            <div class="visual-copy">
                <span class="eyebrow"><c:out value="${editionKicker}"/></span>
                <h1><c:out value="${editionName}"/></h1>
                <p><c:out value="${editionLead}"/></p>
            </div>
            <div class="login-illustration">
                <img src="${pageContext.request.contextPath}/assets/images/hero-service.png" alt="AI 售后维修服务平台插画">
                <div class="login-visual-card">
                    <b>AI 智能诊断</b>
                    <span>搜索问题 → 生成服务词条 → 匹配工程师</span>
                </div>
            </div>
            <div class="edition-grid">
                <a class="${edition == 'client' ? 'active' : ''}" href="${pageContext.request.contextPath}/client/login"><b>客户版</b><span>手机号注册、AI 报修预约、工程师认证申请</span></a>
                <a class="${edition == 'warehouse' ? 'active' : ''}" href="${pageContext.request.contextPath}/warehouse/login"><b>区域仓库版</b><span>配件审核、出库退回、库存盘点</span></a>
                <a class="${edition == 'admin' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/login"><b>平台管理端</b><span>认证审核、用户管理、规则与异常处理</span></a>
            </div>
            <div class="ops-panel">
                <div><span>今日服务流</span><b>报修 → 预约 → 维修 → 验收</b></div>
                <div><span>工程师入驻</span><b>客户提交认证，后台审核后开通接单</b></div>
                <div><span>仓库协同</span><b>工程师缺件时向区域仓库发起申请</b></div>
            </div>
        </div>
    </section>
    <section class="login-panel">
        <form method="post" action="${pageContext.request.contextPath}${loginAction}" class="login-card">
            <div class="mobile-brand"><span class="brand-mark">R</span><strong>RepairFlow</strong></div>
            <span class="login-kicker"><c:out value="${editionKicker}"/></span>
            <h2><c:out value="${editionName}"/>登录</h2>
            <p class="muted"><c:out value="${editionLead}"/></p>
            <c:if test="${not empty error}"><div class="alert danger"><span>!</span><c:out value="${error}"/></div></c:if>
            <label><c:out value="${accountLabel}"/><div class="input-wrap"><span>●</span><input name="account" value="${param.account}" required autofocus autocomplete="username" placeholder="${accountPlaceholder}"></div></label>
            <label>登录密码<div class="input-wrap"><span>◆</span><input name="password" type="password" required autocomplete="current-password" placeholder="请输入密码"></div></label>
            <button class="btn primary block login-submit">进入<c:out value="${editionName}"/> <span>→</span></button>
            <c:choose>
                <c:when test="${edition == 'client'}"><p class="login-tip">还没有账号？<a href="${pageContext.request.contextPath}/register">手机号注册客户版</a></p></c:when>
                <c:when test="${edition == 'warehouse'}"><p class="login-tip">仓库账号由平台管理端统一开通，不开放自助注册。</p></c:when>
                <c:otherwise><p class="login-tip">管理端仅供平台运营人员使用。</p></c:otherwise>
            </c:choose>
        </form>
    </section>
    <script src="${pageContext.request.contextPath}/assets/app.js?v=12"></script>
</body>
</html>
