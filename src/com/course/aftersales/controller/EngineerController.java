package com.course.aftersales.controller;

import com.course.aftersales.model.SessionUser;
import com.course.aftersales.service.EngineerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.sql.Date;

@Controller
public class EngineerController {
    private final EngineerService service;

    public EngineerController(EngineerService service) {
        this.service = service;
    }

    @RequestMapping(value = "/engineer/profile", method = RequestMethod.GET)
    public String profile(HttpSession session, Model model) {
        try {
            model.addAllAttributes(service.profileData(engineer(session).getId()));
            return "engineer/profile";
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/engineer/profile", method = RequestMethod.POST)
    public String updateProfile(@RequestParam(defaultValue = "") String phone,
                                @RequestParam(defaultValue = "") String bio,
                                @RequestParam(value = "faultId", required = false) String[] faultIds,
                                @RequestParam(value = "areaId", required = false) String[] areaIds,
                                HttpSession session,
                                Model model) {
        try {
            service.updateProfile(engineer(session).getId(), text(phone), text(bio), faultIds, areaIds);
            return redirect(session, "/engineer/profile", "档案与服务能力已更新");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/engineer/schedule", method = RequestMethod.GET)
    public String schedule(HttpSession session, Model model) {
        try {
            model.addAllAttributes(service.scheduleData(engineer(session).getId()));
            return "engineer/schedule";
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/engineer/schedule", method = RequestMethod.POST)
    public String addSchedule(@RequestParam(defaultValue = "") String serviceDate,
                              @RequestParam(defaultValue = "0") String slotId,
                              HttpSession session,
                              Model model) {
        try {
            String dateText = text(serviceDate);
            service.addSchedule(engineer(session).getId(), dateText.isEmpty() ? null : Date.valueOf(dateText), id(slotId));
            return redirect(session, "/engineer/schedule", "排班已发布");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/engineer/schedule/close", method = RequestMethod.POST)
    public String closeSchedule(@RequestParam(defaultValue = "0") String id,
                                HttpSession session,
                                Model model) {
        try {
            service.closeSchedule(engineer(session).getId(), id(id));
            return redirect(session, "/engineer/schedule", "空闲时段已关闭");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/engineer/appointment/cancel", method = RequestMethod.POST)
    public String cancelAppointment(@RequestParam(defaultValue = "0") String id,
                                    @RequestParam(defaultValue = "") String reason,
                                    HttpSession session,
                                    Model model) {
        try {
            service.abnormalCancel(engineer(session).getId(), id(id), text(reason));
            return redirect(session, "/appointments", "异常取消已记录，客户已收到改约通知");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/engineer/order/action", method = RequestMethod.POST)
    public String orderAction(@RequestParam(defaultValue = "0") String id,
                              @RequestParam(defaultValue = "") String action,
                              HttpSession session,
                              Model model) {
        long orderId = id(id);
        try {
            service.transition(engineer(session).getId(), orderId, text(action));
            return redirect(session, "/order/detail?id=" + orderId, "工单状态已更新");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/engineer/order/record", method = RequestMethod.POST)
    public String saveRecord(@RequestParam(defaultValue = "0") String id,
                             @RequestParam(defaultValue = "") String diagnosis,
                             @RequestParam(defaultValue = "") String repairAction,
                             @RequestParam(defaultValue = "") String hours,
                             @RequestParam(defaultValue = "") String remark,
                             HttpSession session,
                             Model model) {
        long orderId = id(id);
        try {
            service.saveRecord(engineer(session).getId(), orderId, text(diagnosis), text(repairAction), hours(hours), text(remark));
            return redirect(session, "/order/detail?id=" + orderId, "维修记录已保存");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/engineer/part/request", method = RequestMethod.GET)
    public String partRequest(@RequestParam(defaultValue = "0") String orderId,
                              HttpSession session,
                              Model model) {
        try {
            engineer(session);
            model.addAttribute("orderId", id(orderId));
            model.addAttribute("parts", service.parts());
            return "engineer/part-request";
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/engineer/part/request", method = RequestMethod.POST)
    public String createPartRequest(@RequestParam(defaultValue = "0") String orderId,
                                    @RequestParam(defaultValue = "") String reason,
                                    @RequestParam(value = "partId", required = false) String[] partIds,
                                    @RequestParam(value = "quantity", required = false) String[] quantities,
                                    HttpSession session,
                                    Model model) {
        long targetOrderId = id(orderId);
        try {
            service.createPartRequest(engineer(session).getId(), targetOrderId, text(reason), partIds, quantities);
            return redirect(session, "/order/detail?id=" + targetOrderId, "配件申请已提交仓库审核");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    private SessionUser engineer(HttpSession session) {
        SessionUser user = (SessionUser) session.getAttribute("user");
        if (user == null || !user.hasRole("ENGINEER")) throw new SecurityException("当前账号无权执行此操作");
        return user;
    }

    private String redirect(HttpSession session, String path, String message) {
        session.setAttribute("flash", message);
        return "redirect:" + path;
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

    private double hours(String value) {
        try {
            return Double.parseDouble(text(value));
        } catch (Exception e) {
            return -1;
        }
    }
}
