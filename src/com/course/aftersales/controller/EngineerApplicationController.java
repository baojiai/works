package com.course.aftersales.controller;

import com.course.aftersales.model.SessionUser;
import com.course.aftersales.service.EngineerApplicationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

@Controller
public class EngineerApplicationController {
    private final EngineerApplicationService service;

    public EngineerApplicationController(EngineerApplicationService service) {
        this.service = service;
    }

    @RequestMapping(value = "/engineer/apply", method = RequestMethod.GET)
    public String applicationPage(HttpSession session, Model model) {
        try {
            SessionUser user = user(session);
            model.addAllAttributes(service.formData(user.getId()));
            return "engineer/apply";
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/engineer/apply", method = RequestMethod.POST)
    public String submit(@RequestParam(defaultValue = "") String realName,
                         @RequestParam(defaultValue = "") String idCardNo,
                         @RequestParam(defaultValue = "") String phone,
                         @RequestParam(defaultValue = "0") String areaId,
                         @RequestParam(defaultValue = "0") String experienceYears,
                         @RequestParam(defaultValue = "") String certificateNo,
                         @RequestParam(defaultValue = "") String skillDescription,
                         @RequestParam(defaultValue = "") String materialDescription,
                         @RequestParam(value = "faultId", required = false) String[] faultIds,
                         HttpSession session,
                         Model model) {
        SessionUser user = null;
        try {
            user = user(session);
            service.submit(user.getId(), text(realName), text(idCardNo), text(phone),
                    id(areaId), number(experienceYears), text(certificateNo),
                    text(skillDescription), text(materialDescription), faultIds);
            session.setAttribute("flash", "认证申请已提交，平台管理员审核后会通过站内消息通知你");
            return "redirect:/engineer/apply";
        } catch (Exception e) {
            model.addAttribute("error", rootMessage(e));
            if (user != null) {
                try {
                    model.addAllAttributes(service.formData(user.getId()));
                } catch (Exception ignored) {
                    // Preserve the original page fallback when form data cannot be reloaded.
                }
            }
            return "engineer/apply";
        }
    }

    private SessionUser user(HttpSession session) {
        SessionUser user = (SessionUser) session.getAttribute("user");
        if (user == null) throw new SecurityException("请先登录后再申请工程师认证");
        return user;
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

    private int number(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
}
