package com.course.aftersales.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DeepSeekService {
    private static final String DEFAULT_URL = "https://api.deepseek.com/chat/completions";
    private static final String DEFAULT_MODEL = "deepseek-v4-flash";

    public String diagnose(String problem, String serviceType) throws Exception {
        String key = env("DEEPSEEK_API_KEY");
        if (key.isEmpty()) throw new IllegalStateException("DeepSeek API Key 未配置");
        String apiUrl = env("DEEPSEEK_API_URL").isEmpty() ? DEFAULT_URL : env("DEEPSEEK_API_URL");
        String model = env("DEEPSEEK_MODEL").isEmpty() ? DEFAULT_MODEL : env("DEEPSEEK_MODEL");

        String system = "你是售后维修平台的专属AI诊断助手。你的任务不是最终维修诊断，而是根据用户描述进行初步定位，帮助用户理解问题、选择合适工程师。回答必须简洁、专业、中文，避免夸大，不承诺一定修好。";
        String user = "用户问题：" + problem + "\n平台初步匹配服务：" + serviceType + "\n请输出三部分：1）初步判断；2）建议先检查；3）为什么推荐这类工程师。每部分不超过45字。";
        String body = "{"
                + "\"model\":\"" + json(model) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + json(system) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + json(user) + "\"}"
                + "],"
                + "\"stream\":false,"
                + "\"temperature\":0.3,"
                + "\"max_tokens\":220"
                + "}";

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(18000);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Authorization", "Bearer " + key);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        String response = readAll(is);
        if (code < 200 || code >= 300) throw new IllegalStateException("DeepSeek 调用失败，HTTP " + code);
        String content = extractContent(response);
        if (content.trim().isEmpty()) throw new IllegalStateException("DeepSeek 返回内容为空");
        return content.trim();
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private static String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) >= 0) baos.write(buf, 0, n);
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String extractContent(String json) {
        String needle = "\"content\"";
        int key = json.indexOf(needle);
        if (key < 0) return "";
        int colon = json.indexOf(':', key + needle.length());
        int start = json.indexOf('"', colon + 1);
        if (colon < 0 || start < 0) return "";
        StringBuilder out = new StringBuilder();
        boolean escape = false;
        for (int i = start + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escape) {
                if (ch == 'n') out.append('\n');
                else if (ch == 'r') out.append('\r');
                else if (ch == 't') out.append('\t');
                else if (ch == 'u' && i + 4 < json.length()) {
                    String hex = json.substring(i + 1, i + 5);
                    try { out.append((char) Integer.parseInt(hex, 16)); i += 4; }
                    catch (NumberFormatException e) { out.append("\\u").append(hex); i += 4; }
                } else out.append(ch);
                escape = false;
            } else if (ch == '\\') {
                escape = true;
            } else if (ch == '"') {
                break;
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static String json(String text) {
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
