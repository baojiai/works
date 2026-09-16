package com.course.aftersales.controller;

import com.course.aftersales.model.SessionUser;
import com.course.aftersales.service.DeepSeekService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
public class AiController {
    private final DeepSeekService service;

    public AiController(DeepSeekService service) {
        this.service = service;
    }

    @ResponseBody
    @RequestMapping(value = "/api/ai/diagnose", method = RequestMethod.POST,
            produces = "application/json;charset=UTF-8")
    public String diagnose(@RequestParam(defaultValue = "") String problem,
                           @RequestParam(defaultValue = "") String serviceType,
                           HttpSession session) {
        try {
            SessionUser user = (SessionUser) session.getAttribute("user");
            if (user == null) throw new SecurityException("请先登录");
            String description = problem.trim();
            if (description.isEmpty()) throw new IllegalArgumentException("请先输入问题描述");
            DeepSeekService.AiResult result = service.diagnose(description, serviceType.trim());
            StringBuilder response = new StringBuilder();
            response.append("{\"ok\":true,\"answer\":\"").append(json(result.answer)).append("\",\"suggestions\":[");
            for (int i = 0; i < result.suggestions.size(); i++) {
                DeepSeekService.Suggestion suggestion = result.suggestions.get(i);
                if (i > 0) response.append(',');
                response.append("{\"title\":\"").append(json(suggestion.title))
                        .append("\",\"device\":\"").append(json(suggestion.device))
                        .append("\",\"fault\":\"").append(json(suggestion.fault))
                        .append("\",\"note\":\"").append(json(suggestion.note)).append("\"}");
            }
            response.append("]}");
            return response.toString();
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null) cause = cause.getCause();
            return "{\"ok\":false,\"message\":\"" + json(cause.getMessage()) + "\"}";
        }
    }

    private static String json(String text) {
        if (text == null) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"') result.append("\\\"");
            else if (ch == '\\') result.append("\\\\");
            else if (ch == '\n') result.append("\\n");
            else if (ch == '\r') result.append("\\r");
            else if (ch == '\t') result.append("\\t");
            else result.append(ch);
        }
        return result.toString();
    }
}
