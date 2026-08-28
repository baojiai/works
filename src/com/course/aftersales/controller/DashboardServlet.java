package com.course.aftersales.controller;

import com.course.aftersales.service.DashboardService;
import com.course.aftersales.util.Web;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet(urlPatterns={"/dashboard"})
public class DashboardServlet extends HttpServlet {
    private final DashboardService service = new DashboardService();
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (Web.user(req)==null) { resp.sendRedirect(req.getContextPath()+"/login"); return; }
        try { req.setAttribute("summary",service.summary(Web.user(req))); Web.view(req,resp,"dashboard"); }
        catch(Exception e){ throw new ServletException(e); }
    }
}
