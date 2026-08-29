package com.course.aftersales.controller;

import com.course.aftersales.service.DeepSeekService;
import com.course.aftersales.util.Web;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet(urlPatterns={"/api/ai/diagnose"})
public class AiServlet extends HttpServlet {
    private final DeepSeekService service = new DeepSeekService();

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        try {
            if (Web.user(req) == null) throw new SecurityException("请先登录");
            String problem = Web.text(req, "problem");
            String serviceType = Web.text(req, "serviceType");
            if (problem.isEmpty()) throw new IllegalArgumentException("请先输入问题描述");
            DeepSeekService.AiResult result = service.diagnose(problem, serviceType);
            StringBuilder json = new StringBuilder();
            json.append("{\"ok\":true,\"answer\":\"").append(json(result.answer)).append("\",\"suggestions\":[");
            for (int i = 0; i < result.suggestions.size(); i++) {
                DeepSeekService.Suggestion s = result.suggestions.get(i);
                if (i > 0) json.append(',');
                json.append("{\"title\":\"").append(json(s.title))
                        .append("\",\"device\":\"").append(json(s.device))
                        .append("\",\"fault\":\"").append(json(s.fault))
                        .append("\",\"note\":\"").append(json(s.note)).append("\"}");
            }
            json.append("]}");
            resp.getWriter().write(json.toString());
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null) cause = cause.getCause();
            resp.getWriter().write("{\"ok\":false,\"message\":\"" + json(cause.getMessage()) + "\"}");
        }
    }

    private static String json(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"') sb.append("\\\"");
            else if (ch == '\\') sb.append("\\\\");
            else if (ch == '\n') sb.append("\\n");
            else if (ch == '\r') sb.append("\\r");
            else if (ch == '\t') sb.append("\\t");
            else sb.append(ch);
        }
        return sb.toString();
    }
}
