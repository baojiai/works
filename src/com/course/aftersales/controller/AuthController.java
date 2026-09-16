package com.course.aftersales.controller;

import com.course.aftersales.model.SessionUser;
import com.course.aftersales.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @RequestMapping(value = {"/login", "/client/login", "/warehouse/login", "/admin/login"}, method = RequestMethod.GET)
    public String loginPage(HttpServletRequest request, HttpSession session, Model model) {
        if (session.getAttribute("user") != null) return "redirect:/dashboard";
        prepareLogin(path(request), model);
        return "login";
    }

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public String registerPage(HttpSession session) {
        if (session.getAttribute("user") != null) return "redirect:/dashboard";
        return "register";
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/client/login";
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String register(@RequestParam(defaultValue = "") String phone,
                           @RequestParam(defaultValue = "") String displayName,
                           @RequestParam(defaultValue = "") String password,
                           @RequestParam(defaultValue = "") String confirmPassword,
                           HttpSession session,
                           Model model) {
        try {
            SessionUser user = service.registerCustomer(phone.trim(), displayName.trim(), password.trim(), confirmPassword.trim());
            session.setAttribute("user", user);
            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", rootMessage(e));
            return "register";
        }
    }

    @RequestMapping(value = {"/login", "/client/login", "/warehouse/login", "/admin/login"}, method = RequestMethod.POST)
    public String login(@RequestParam(defaultValue = "") String account,
                        @RequestParam(defaultValue = "") String username,
                        @RequestParam(defaultValue = "") String password,
                        HttpServletRequest request,
                        HttpSession session,
                        Model model) {
        String requestPath = path(request);
        prepareLogin(requestPath, model);
        try {
            SessionUser user = service.login(account.trim(), password.trim());
            if (user == null && !username.trim().isEmpty()) user = service.login(username.trim(), password.trim());
            if (user == null) {
                model.addAttribute("error", "账号、密码错误或账号已停用");
                return "login";
            }
            if (!canEnter(requestPath, user)) {
                model.addAttribute("error", "当前账号不属于该发行版本，请切换到对应入口登录");
                return "login";
            }
            session.setAttribute("user", user);
            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", rootMessage(e));
            return "login";
        }
    }

    private void prepareLogin(String requestPath, Model model) {
        if ("/warehouse/login".equals(requestPath)) {
            model.addAttribute("edition", "warehouse");
            model.addAttribute("editionName", "区域仓库版");
            model.addAttribute("editionKicker", "WAREHOUSE CONSOLE");
            model.addAttribute("editionLead", "面向各地区仓库，处理工程师配件申请、出库、退回和库存流水。");
            model.addAttribute("accountLabel", "仓库账号 / 绑定手机号");
            model.addAttribute("accountPlaceholder", "请输入仓库账号");
            model.addAttribute("loginAction", "/warehouse/login");
        } else if ("/admin/login".equals(requestPath)) {
            model.addAttribute("edition", "admin");
            model.addAttribute("editionName", "平台管理端");
            model.addAttribute("editionKicker", "ADMIN CENTER");
            model.addAttribute("editionLead", "面向平台运营人员，审核工程师认证、维护基础数据和处理异常业务。");
            model.addAttribute("accountLabel", "管理员账号 / 绑定手机号");
            model.addAttribute("accountPlaceholder", "请输入管理员账号");
            model.addAttribute("loginAction", "/admin/login");
        } else {
            model.addAttribute("edition", "client");
            model.addAttribute("editionName", "客户版");
            model.addAttribute("editionKicker", "CUSTOMER APP");
            model.addAttribute("editionLead", "面向普通用户，支持手机号注册登录、报修预约、验收评价和工程师认证申请。");
            model.addAttribute("accountLabel", "手机号");
            model.addAttribute("accountPlaceholder", "请输入 11 位手机号");
            model.addAttribute("loginAction", "/client/login");
        }
    }

    private boolean canEnter(String requestPath, SessionUser user) {
        if ("/warehouse/login".equals(requestPath)) return user.hasRole("WAREHOUSE");
        if ("/admin/login".equals(requestPath)) return user.hasRole("ADMIN");
        return user.hasRole("CUSTOMER") || user.hasRole("ENGINEER");
    }

    private String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage();
    }
}
