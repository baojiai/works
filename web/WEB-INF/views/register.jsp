<%@ page pageEncoding="UTF-8" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="theme-color" content="#f6f8fb">
    <title>手机号注册 · RepairFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app.css?v=3">
</head>
<body class="login-page register-page">
    <section class="login-visual">
        <div class="login-art">
            <div class="visual-brand"><span class="brand-mark">R</span><span><strong>RepairFlow</strong><small>AFTER-SALES SERVICE PLATFORM</small></span></div>
            <div class="visual-copy">
                <span class="eyebrow">CUSTOMER APP</span>
                <h1>用手机号创建客户账号</h1>
                <p>注册后可以提交报修、选择工程师、跟踪维修进度；需要接单赚钱时，可在系统内申请工程师认证。</p>
            </div>
            <div class="edition-grid">
                <div><b>1. 注册客户</b><span>手机号作为唯一身份</span></div>
                <div><b>2. 发起报修</b><span>填写设备、故障和服务地址</span></div>
                <div><b>3. 申请认证</b><span>审核通过后升级为工程师</span></div>
            </div>
        </div>
    </section>
    <section class="login-panel">
        <form method="post" action="${pageContext.request.contextPath}/register" class="login-card">
            <div class="mobile-brand"><span class="brand-mark">R</span><strong>RepairFlow</strong></div>
            <span class="login-kicker">CREATE ACCOUNT</span>
            <h2>注册客户版</h2>
            <p class="muted">注册成功后将直接进入平台。</p>
            <c:if test="${not empty error}"><div class="alert danger"><span>!</span><c:out value="${error}"/></div></c:if>
            <label>手机号<div class="input-wrap"><span>◎</span><input name="phone" value="${param.phone}" required maxlength="11" autocomplete="tel" placeholder="请输入 11 位手机号"></div></label>
            <label>昵称<div class="input-wrap"><span>◇</span><input name="displayName" value="${param.displayName}" placeholder="可填写真实姓名或昵称"></div></label>
            <label>密码<div class="input-wrap"><span>◆</span><input name="password" type="password" required minlength="6" autocomplete="new-password" placeholder="至少 6 位"></div></label>
            <label>确认密码<div class="input-wrap"><span>◆</span><input name="confirmPassword" type="password" required minlength="6" autocomplete="new-password" placeholder="再次输入密码"></div></label>
            <button class="btn primary block login-submit">注册并进入 <span>→</span></button>
            <p class="login-tip">已有账号？<a href="${pageContext.request.contextPath}/client/login">返回客户版登录</a></p>
        </form>
    </section>
    <script src="${pageContext.request.contextPath}/assets/app.js?v=3"></script>
</body>
</html>
