package com.course.aftersales.controller;

import com.course.aftersales.model.SessionUser;
import com.course.aftersales.service.AuthService;
import com.course.aftersales.util.Web;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet(urlPatterns={"/login","/client/login","/warehouse/login","/admin/login","/logout","/register"})
public class AuthServlet extends HttpServlet {
    private final AuthService service = new AuthService();
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getServletPath().equals("/logout")) { req.getSession().invalidate(); resp.sendRedirect(req.getContextPath()+"/client/login"); return; }
        if (Web.user(req) != null) { resp.sendRedirect(req.getContextPath()+"/dashboard"); return; }
        if (req.getServletPath().equals("/register")) { Web.view(req, resp, "register"); return; }
        prepareLogin(req);
        Web.view(req, resp, "login");
    }
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (req.getServletPath().equals("/register")) {
                SessionUser user = service.registerCustomer(Web.text(req,"phone"), Web.text(req,"displayName"), Web.text(req,"password"), Web.text(req,"confirmPassword"));
                req.getSession(true).setAttribute("user", user);
                resp.sendRedirect(req.getContextPath()+"/dashboard");
                return;
            }
            prepareLogin(req);
            SessionUser user = service.login(Web.text(req,"account"), Web.text(req,"password"));
            if (user == null && !Web.text(req,"username").isEmpty()) user = service.login(Web.text(req,"username"), Web.text(req,"password"));
            if (user == null) { req.setAttribute("error","账号、密码错误或账号已停用"); Web.view(req,resp,"login"); return; }
            if (!canEnter(req.getServletPath(), user)) {
                req.setAttribute("error", "当前账号不属于该发行版本，请切换到对应入口登录");
                Web.view(req, resp, "login");
                return;
            }
            req.getSession(true).setAttribute("user", user);
            resp.sendRedirect(req.getContextPath()+"/dashboard");
        } catch (Exception e) {
            Throwable cause=e; while(cause.getCause()!=null)cause=cause.getCause();
            req.setAttribute("error", cause.getMessage());
            if (!req.getServletPath().equals("/register")) prepareLogin(req);
            Web.view(req, resp, req.getServletPath().equals("/register") ? "register" : "login");
        }
    }

    private void prepareLogin(HttpServletRequest req) {
        String path = req.getServletPath();
        if (path.equals("/warehouse/login")) {
            req.setAttribute("edition", "warehouse");
            req.setAttribute("editionName", "区域仓库版");
            req.setAttribute("editionKicker", "WAREHOUSE CONSOLE");
            req.setAttribute("editionLead", "面向各地区仓库，处理工程师配件申请、出库、退回和库存流水。");
            req.setAttribute("accountLabel", "仓库账号 / 绑定手机号");
            req.setAttribute("accountPlaceholder", "请输入仓库账号");
            req.setAttribute("loginAction", "/warehouse/login");
        } else if (path.equals("/admin/login")) {
            req.setAttribute("edition", "admin");
            req.setAttribute("editionName", "平台管理端");
            req.setAttribute("editionKicker", "ADMIN CENTER");
            req.setAttribute("editionLead", "面向平台运营人员，审核工程师认证、维护基础数据和处理异常业务。");
            req.setAttribute("accountLabel", "管理员账号 / 绑定手机号");
            req.setAttribute("accountPlaceholder", "请输入管理员账号");
            req.setAttribute("loginAction", "/admin/login");
        } else {
            req.setAttribute("edition", "client");
            req.setAttribute("editionName", "客户版");
            req.setAttribute("editionKicker", "CUSTOMER APP");
            req.setAttribute("editionLead", "面向普通用户，支持手机号注册登录、报修预约、验收评价和工程师认证申请。");
            req.setAttribute("accountLabel", "手机号");
            req.setAttribute("accountPlaceholder", "请输入 11 位手机号");
            req.setAttribute("loginAction", "/client/login");
        }
    }

    private boolean canEnter(String path, SessionUser user) {
        if (path.equals("/warehouse/login")) return user.hasRole("WAREHOUSE");
        if (path.equals("/admin/login")) return user.hasRole("ADMIN");
        return user.hasRole("CUSTOMER") || user.hasRole("ENGINEER");
    }
}
