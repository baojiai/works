package com.course.aftersales.controller;

import com.course.aftersales.model.SessionUser;
import com.course.aftersales.service.AdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

@Controller
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @RequestMapping(value = "/admin", method = RequestMethod.GET)
    public String index(HttpSession session, Model model) {
        try {
            admin(session);
            model.addAllAttributes(service.data());
            return "admin/index";
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/admin/user", method = RequestMethod.POST)
    public String updateUser(@RequestParam(defaultValue = "0") String id,
                             @RequestParam(defaultValue = "") String status,
                             HttpSession session,
                             Model model) {
        try {
            service.setUserStatus(admin(session).getId(), id(id), text(status));
            return redirect(session, "用户状态已更新");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/admin/qualification", method = RequestMethod.POST)
    public String updateQualification(@RequestParam(defaultValue = "0") String id,
                                      @RequestParam(defaultValue = "") String status,
                                      HttpSession session,
                                      Model model) {
        try {
            service.setQualification(admin(session).getId(), id(id), text(status));
            return redirect(session, "工程师资质已更新");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/admin/application", method = RequestMethod.POST)
    public String reviewApplication(@RequestParam(defaultValue = "0") String id,
                                    @RequestParam(defaultValue = "") String decision,
                                    @RequestParam(defaultValue = "") String comment,
                                    HttpSession session,
                                    Model model) {
        try {
            service.reviewApplication(admin(session).getId(), id(id), "approve".equals(text(decision)), text(comment));
            return redirect(session, "工程师认证申请已处理");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/admin/config", method = RequestMethod.POST)
    public String updateConfig(@RequestParam(defaultValue = "") String key,
                               @RequestParam(defaultValue = "") String value,
                               HttpSession session,
                               Model model) {
        try {
            service.updateConfig(admin(session).getId(), text(key), text(value));
            return redirect(session, "系统参数已保存");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/admin/basic", method = RequestMethod.POST)
    public String addBasic(@RequestParam(defaultValue = "") String kind,
                           @RequestParam(defaultValue = "") String name,
                           @RequestParam(defaultValue = "0") String parentId,
                           @RequestParam(defaultValue = "") String startTime,
                           @RequestParam(defaultValue = "") String endTime,
                           HttpSession session,
                           Model model) {
        try {
            service.addBasic(admin(session).getId(), text(kind), text(name), id(parentId), text(startTime), text(endTime));
            return redirect(session, "基础数据已新增");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/admin/expire", method = RequestMethod.POST)
    public String expireReschedules(HttpSession session, Model model) {
        try {
            int count = service.expireReschedules(admin(session).getId());
            return redirect(session, "已处理 " + count + " 条超时改约");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/admin/sla", method = RequestMethod.POST)
    public String runSla(HttpSession session, Model model) {
        try {
            int count = service.runSla(admin(session).getId());
            return redirect(session, "已生成 " + count + " 条新提醒");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    private SessionUser admin(HttpSession session) {
        SessionUser user = (SessionUser) session.getAttribute("user");
        if (user == null || !user.hasRole("ADMIN")) throw new SecurityException("当前账号无权执行此操作");
        return user;
    }

    private String redirect(HttpSession session, String message) {
        session.setAttribute("flash", message);
        return "redirect:/admin";
    }

    private String error(Model model, Exception exception) {
        model.addAttribute("error", rootMessage(exception));
        model.addAttribute("exception", exception);
        return "error";
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage();
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private long id(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }
}
