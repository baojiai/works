package com.course.aftersales.filter;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

public class AuthFilter implements Filter {
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String requestUri = req.getRequestURI();
        if (requestUri.contains("/assets/") || requestUri.endsWith("/favicon.ico")) {
            chain.doFilter(request, response);
            return;
        }
        String path = req.getServletPath();
        boolean publicPath = path == null || path.equals("") || path.equals("/")
                || path.equals("/login") || path.equals("/client/login") || path.equals("/warehouse/login") || path.equals("/admin/login")
                || path.equals("/register") || path.equals("/favicon.ico")
                || path.startsWith("/assets/");
        if (!publicPath && req.getSession(false) != null && req.getSession(false).getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if (!publicPath && req.getSession(false) == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        chain.doFilter(request, response);
    }
}
