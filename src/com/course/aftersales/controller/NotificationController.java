package com.course.aftersales.controller;

import com.course.aftersales.model.SessionUser;
import com.course.aftersales.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpSession;

@Controller
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @RequestMapping(value = "/notifications", method = RequestMethod.GET)
    public String list(HttpSession session, Model model) throws Exception {
        SessionUser user = (SessionUser) session.getAttribute("user");
        model.addAttribute("notifications", service.list(user.getId()));
        return "notifications";
    }

    @RequestMapping(value = "/notifications/read", method = RequestMethod.POST)
    public String read(@RequestParam(defaultValue = "") String mode,
                       @RequestParam(defaultValue = "0") long id,
                       HttpSession session) throws Exception {
        SessionUser user = (SessionUser) session.getAttribute("user");
        if ("all".equals(mode.trim())) service.readAll(user.getId());
        else service.read(user.getId(), id);
        session.setAttribute("flash", "通知状态已更新");
        return "redirect:/notifications";
    }
}
