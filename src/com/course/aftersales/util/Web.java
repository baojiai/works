package com.course.aftersales.util;

import com.course.aftersales.model.SessionUser;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

public final class Web {
    private Web() {}
    public static SessionUser user(HttpServletRequest req) { return (SessionUser) req.getSession().getAttribute("user"); }
    public static long id(HttpServletRequest req, String name) {
        try { return Long.parseLong(req.getParameter(name)); } catch (Exception e) { return 0L; }
    }
    public static int number(HttpServletRequest req, String name, int fallback) {
        try { return Integer.parseInt(req.getParameter(name)); } catch (Exception e) { return fallback; }
    }
    public static String text(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null ? "" : value.trim();
    }
    public static void view(HttpServletRequest req, HttpServletResponse resp, String jsp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/" + jsp + ".jsp").forward(req, resp);
    }
    public static void redirect(HttpServletRequest req, HttpServletResponse resp, String path, String message) throws IOException {
        if (message != null) req.getSession().setAttribute("flash", message);
        resp.sendRedirect(req.getContextPath() + path);
    }
    public static void requireRole(HttpServletRequest req, String role) {
        SessionUser user = user(req);
        if (user == null || !user.hasRole(role)) throw new SecurityException("当前账号无权执行此操作");
    }
}

