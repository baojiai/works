package com.course.aftersales.controller;

import com.course.aftersales.service.NotificationService;
import com.course.aftersales.util.Web;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet(urlPatterns={"/notifications","/notifications/read"})
public class NotificationServlet extends HttpServlet{
    private final NotificationService service=new NotificationService();
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{try{req.setAttribute("notifications",service.list(Web.user(req).getId()));Web.view(req,resp,"notifications");}catch(Exception e){throw new ServletException(e);}}
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{try{if("all".equals(Web.text(req,"mode")))service.readAll(Web.user(req).getId());else service.read(Web.user(req).getId(),Web.id(req,"id"));Web.redirect(req,resp,"/notifications","通知状态已更新");}catch(Exception e){throw new ServletException(e);}}
}
