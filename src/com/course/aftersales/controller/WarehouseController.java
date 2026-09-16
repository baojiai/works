package com.course.aftersales.controller;

import com.course.aftersales.model.SessionUser;
import com.course.aftersales.service.WarehouseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

@Controller
public class WarehouseController {
    private final WarehouseService service;

    public WarehouseController(WarehouseService service) {
        this.service = service;
    }

    @GetMapping("/warehouse/requests")
    public String requests(HttpSession session, Model model) {
        try {
            warehouse(session);
            model.addAllAttributes(service.requestData());
            return "warehouse/requests";
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @GetMapping("/warehouse/inventory")
    public String inventory(HttpSession session, Model model) {
        try {
            warehouse(session);
            model.addAttribute("inventory", service.inventory());
            model.addAttribute("flows", service.flows());
            return "warehouse/inventory";
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @PostMapping("/warehouse/review")
    public String review(@RequestParam(defaultValue = "0") String id,
                         @RequestParam(defaultValue = "") String decision,
                         @RequestParam(defaultValue = "") String comment,
                         HttpSession session,
                         Model model) {
        try {
            service.review(warehouse(session).getId(), id(id), "approve".equals(text(decision)), text(comment));
            return redirect(session, "配件申请处理成功");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @PostMapping("/warehouse/issue")
    public String issue(@RequestParam(defaultValue = "0") String id, HttpSession session, Model model) {
        try {
            service.issue(warehouse(session).getId(), id(id));
            return redirect(session, "配件申请处理成功");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @PostMapping("/warehouse/release")
    public String release(@RequestParam(defaultValue = "0") String id,
                          @RequestParam(defaultValue = "") String reason,
                          HttpSession session,
                          Model model) {
        try {
            service.release(warehouse(session).getId(), id(id), text(reason));
            return redirect(session, "配件申请处理成功");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @PostMapping("/warehouse/return")
    public String returnPart(@RequestParam(defaultValue = "0") String itemId,
                             @RequestParam(defaultValue = "0") String quantity,
                             @RequestParam(defaultValue = "") String reason,
                             HttpSession session,
                             Model model) {
        try {
            service.returnPart(warehouse(session).getId(), id(itemId), number(quantity, 0), text(reason));
            return redirect(session, "配件申请处理成功");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @PostMapping("/warehouse/complete")
    public String complete(@RequestParam(defaultValue = "0") String id, HttpSession session, Model model) {
        try {
            service.complete(warehouse(session).getId(), id(id));
            return redirect(session, "配件申请处理成功");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    @PostMapping("/warehouse/stock")
    public String stock(@RequestParam(defaultValue = "0") String partId,
                        @RequestParam(defaultValue = "0") String quantity,
                        @RequestParam(defaultValue = "") String type,
                        @RequestParam(defaultValue = "") String reason,
                        HttpSession session,
                        Model model) {
        try {
            service.stock(warehouse(session).getId(), id(partId), number(quantity, 0), text(type), text(reason));
            return redirect(session, "/warehouse/inventory", "库存及流水已更新");
        } catch (Exception e) {
            return error(model, e);
        }
    }

    private SessionUser warehouse(HttpSession session) {
        SessionUser user = (SessionUser) session.getAttribute("user");
        if (user == null || !user.hasRole("WAREHOUSE")) throw new SecurityException("当前账号无权执行此操作");
        return user;
    }

    private String redirect(HttpSession session, String message) {
        session.setAttribute("flash", message);
        return "redirect:/warehouse/requests";
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

    private int number(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }
}
