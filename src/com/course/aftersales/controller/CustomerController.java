package com.course.aftersales.controller;

import com.course.aftersales.model.SessionUser;
import com.course.aftersales.service.CustomerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.sql.Date;
import java.util.Map;

@Controller
public class CustomerController {
    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @RequestMapping(value = "/customer/request", method = RequestMethod.GET)
    public String requestPage(HttpSession session, Model model) {
        try {
            customer(session);
            model.addAllAttributes(service.formData());
            return "customer/request";
        } catch (Exception e) {
            model.addAttribute("error", rootMessage(e));
            try {
                model.addAllAttributes(service.formData());
            } catch (Exception ignored) {
                // Preserve the original request-page fallback if lookup data cannot be loaded.
            }
            return "customer/request";
        }
    }

    @RequestMapping(value = "/customer/request", method = RequestMethod.POST)
    public String submitRequest(@RequestParam(defaultValue = "0") String deviceId,
                                @RequestParam(defaultValue = "0") String faultId,
                                @RequestParam(defaultValue = "0") String areaId,
                                @RequestParam(defaultValue = "") String problemQuery,
                                @RequestParam(defaultValue = "") String description,
                                @RequestParam(defaultValue = "") String address,
                                @RequestParam(defaultValue = "") String phone,
                                @RequestParam(defaultValue = "") String expectedDate,
                                @RequestParam(defaultValue = "0") String slotId,
                                HttpSession session,
                                Model model) {
        try {
            SessionUser user = customer(session);
            String dateText = text(expectedDate);
            String query = text(problemQuery);
            String details = text(description);
            if (!query.isEmpty()) details = "用户搜索问题：" + query + "\n补充描述：" + details;
            long selectedSlot = id(slotId);
            long requestId = service.createRequest(user.getId(), id(deviceId), id(faultId), id(areaId), details,
                    text(address), text(phone), dateText.isEmpty() ? null : Date.valueOf(dateText),
                    selectedSlot > 0 ? selectedSlot : null);
            return redirect(session, "/customer/candidates?requestId=" + requestId, "报修需求已保存，请自主选择工程师和时段");
        } catch (Exception e) {
            model.addAttribute("error", rootMessage(e));
            try {
                model.addAllAttributes(service.formData());
            } catch (Exception ignored) {
                // Preserve the original request-page fallback if lookup data cannot be loaded.
            }
            return "customer/request";
        }
    }

    @RequestMapping(value = "/customer/candidates", method = RequestMethod.GET)
    public String candidates(@RequestParam(defaultValue = "0") String requestId,
                             @RequestParam(defaultValue = "") String sort,
                             HttpSession session,
                             Model model) {
        try {
            SessionUser user = customer(session);
            long id = id(requestId);
            model.addAttribute("repairRequest", service.request(user.getId(), id));
            model.addAttribute("candidates", service.candidates(user.getId(), id, sort.trim()));
            return "customer/candidates";
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/orders", method = RequestMethod.GET)
    public String orders(HttpSession session, Model model) {
        try {
            SessionUser user = (SessionUser) session.getAttribute("user");
            if (user == null) throw new SecurityException("请先登录");
            model.addAttribute("orders", service.orders(user));
            return "orders";
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/customer/book", method = RequestMethod.POST)
    public String book(@RequestParam(defaultValue = "0") String requestId,
                       @RequestParam(defaultValue = "0") String engineerId,
                       @RequestParam(defaultValue = "0") String scheduleId,
                       @RequestParam(defaultValue = "0") String replacesId,
                       HttpSession session,
                       Model model) {
        try {
            SessionUser user = customer(session);
            long appointmentId = service.book(user.getId(), id(requestId), id(engineerId), id(scheduleId), id(replacesId));
            return redirect(session, "/appointments", "预约成功，编号已生成（ID " + appointmentId + "）");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/appointments", method = RequestMethod.GET)
    public String appointments(HttpSession session, Model model) {
        try {
            model.addAttribute("appointments", service.appointments(user(session)));
            return "appointments";
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/appointment/cancel", method = RequestMethod.POST)
    public String cancelAppointment(@RequestParam(defaultValue = "0") String id,
                                    @RequestParam(defaultValue = "") String reason,
                                    HttpSession session,
                                    Model model) {
        try {
            service.cancel(customer(session).getId(), id(id), text(reason));
            return redirect(session, "/appointments", "预约已取消，原时段已释放");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/order/detail", method = RequestMethod.GET)
    public String orderDetail(@RequestParam(defaultValue = "0") String id,
                              HttpSession session,
                              Model model) {
        try {
            Map<String, Object> detail = service.orderDetail(user(session), id(id));
            model.addAllAttributes(detail);
            return "order-detail";
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/order/accept", method = RequestMethod.POST)
    public String accept(@RequestParam(defaultValue = "0") String id,
                         @RequestParam(defaultValue = "") String result,
                         @RequestParam(defaultValue = "") String comment,
                         HttpSession session,
                         Model model) {
        long orderId = id(id);
        try {
            service.accept(customer(session).getId(), orderId, "PASSED".equals(text(result)), text(comment));
            return redirect(session, "/order/detail?id=" + orderId, "验收结果已提交");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @RequestMapping(value = "/order/review", method = RequestMethod.POST)
    public String review(@RequestParam(defaultValue = "0") String id,
                         @RequestParam(defaultValue = "0") String rating,
                         @RequestParam(defaultValue = "") String content,
                         HttpSession session,
                         Model model) {
        long orderId = id(id);
        try {
            service.review(customer(session).getId(), orderId, number(rating), text(content));
            return redirect(session, "/order/detail?id=" + orderId, "评价已提交");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    private SessionUser customer(HttpSession session) {
        SessionUser user = user(session);
        if (user == null || !user.hasRole("CUSTOMER")) throw new SecurityException("当前账号无权执行此操作");
        return user;
    }

    private SessionUser user(HttpSession session) {
        SessionUser user = (SessionUser) session.getAttribute("user");
        if (user == null) throw new SecurityException("请先登录");
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

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
