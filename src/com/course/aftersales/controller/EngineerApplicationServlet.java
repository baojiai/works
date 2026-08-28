package com.course.aftersales.controller;

import com.course.aftersales.service.EngineerApplicationService;
import com.course.aftersales.util.Web;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;

@WebServlet(urlPatterns={"/engineer/apply"})
public class EngineerApplicationServlet extends HttpServlet {
    private final EngineerApplicationService service = new EngineerApplicationService();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (Web.user(req) == null) throw new SecurityException("请先登录后再申请工程师认证");
            put(req, service.formData(Web.user(req).getId()));
            Web.view(req, resp, "engineer/apply");
        } catch (Exception e) { fail(req, resp, e); }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (Web.user(req) == null) throw new SecurityException("请先登录后再申请工程师认证");
            service.submit(Web.user(req).getId(), Web.text(req,"realName"), Web.text(req,"idCardNo"), Web.text(req,"phone"),
                    Web.id(req,"areaId"), Web.number(req,"experienceYears",0), Web.text(req,"certificateNo"),
                    Web.text(req,"skillDescription"), Web.text(req,"materialDescription"), req.getParameterValues("faultId"));
            Web.redirect(req, resp, "/engineer/apply", "认证申请已提交，平台管理员审核后会通过站内消息通知你");
        } catch (Exception e) {
            Throwable cause=e; while(cause.getCause()!=null)cause=cause.getCause();
            req.setAttribute("error", cause.getMessage());
            try { put(req, service.formData(Web.user(req).getId())); } catch(Exception ignored) {}
            Web.view(req, resp, "engineer/apply");
        }
    }

    private void put(HttpServletRequest req, Map<String,Object> data) { for (Map.Entry<String,Object> e : data.entrySet()) req.setAttribute(e.getKey(), e.getValue()); }
    private void fail(HttpServletRequest req,HttpServletResponse resp,Exception e)throws ServletException,IOException{Throwable c=e;while(c.getCause()!=null)c=c.getCause();req.setAttribute("error",c.getMessage());req.setAttribute("exception",e);Web.view(req,resp,"error");}
}
