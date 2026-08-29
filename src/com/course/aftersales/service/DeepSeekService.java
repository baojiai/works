package com.course.aftersales.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DeepSeekService {
    private static final String DEFAULT_URL = "https://api.deepseek.com/chat/completions";
    private static final String DEFAULT_MODEL = "deepseek-v4-flash";

    public AiResult diagnose(String problem, String serviceType) throws Exception {
        String key = env("DEEPSEEK_API_KEY");
        if (key.isEmpty()) throw new IllegalStateException("DeepSeek API Key 未配置");
        String apiUrl = env("DEEPSEEK_API_URL").isEmpty() ? DEFAULT_URL : env("DEEPSEEK_API_URL");
        String model = env("DEEPSEEK_MODEL").isEmpty() ? DEFAULT_MODEL : env("DEEPSEEK_MODEL");

        String system = "你是售后维修平台的专属AI诊断助手。必须实事求是，不要为了凑结果而编造服务词条。只有当用户明确提供了设备名称和故障现象时，才生成服务词条；如果输入与维修无关、设备不清楚、现象不清楚或无法可靠判断，必须在 SERVICES: 后只输出 NONE。回答必须专业、简洁、中文。输出格式必须严格遵守：先输出 DIAGNOSIS: 后跟一段诊断建议；再输出 SERVICES:；如果可以判断，随后输出1到4行服务词条，每行格式为 标题|设备大类|故障大类|说明。设备大类尽量使用计算机、打印设备、家用电器之一；故障大类可以使用加热异常、通电异常、异响或漏水、制冷异常、无法开机、系统异常、无法打印等。";
        String user = "用户问题：" + problem + "\n平台初步匹配：" + serviceType + "\n请判断是否足够生成页面“可能需要的服务”词条。比如用户说微波炉不加热，就生成微波炉相关词条；如果用户只是随便输入、只说坏了、或者没有明确设备和现象，请返回 NONE。";
        String body = "{"
                + "\"model\":\"" + json(model) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + json(system) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + json(user) + "\"}"
                + "],"
                + "\"stream\":false,"
                + "\"temperature\":0.35,"
                + "\"max_tokens\":420"
                + "}";

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(20000);
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
        return parse(content.trim());
    }

    private static AiResult parse(String content) {
        AiResult result = new AiResult();
        result.answer = content;
        boolean inServices = false;
        StringBuilder diagnosis = new StringBuilder();
        for (String rawLine : content.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("SERVICES:") || line.startsWith("SERVICES：")) {
                inServices = true;
                continue;
            }
            if (line.equalsIgnoreCase("DIAGNOSIS:") || line.startsWith("DIAGNOSIS：")) {
                inServices = false;
                line = line.replaceFirst("(?i)^DIAGNOSIS[:：]\\s*", "").trim();
                if (line.isEmpty()) continue;
            }
            if (inServices) {
                if (line.equalsIgnoreCase("NONE") || line.equals("无") || line.contains("无法判断")) {
                    continue;
                }
                line = line.replaceFirst("^[-\\d.、\\s]+", "");
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    result.suggestions.add(new Suggestion(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim()));
                }
            } else {
                if (diagnosis.length() > 0) diagnosis.append('\n');
                diagnosis.append(line);
            }
        }
        if (diagnosis.length() > 0) result.answer = diagnosis.toString();
        return result;
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
            } else if (ch == '\\') escape = true;
            else if (ch == '"') break;
            else out.append(ch);
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

    public static class AiResult {
        public String answer = "";
        public final List<Suggestion> suggestions = new ArrayList<>();
    }

    public static class Suggestion {
        public final String title;
        public final String device;
        public final String fault;
        public final String note;
        public Suggestion(String title, String device, String fault, String note) {
            this.title = title;
            this.device = device;
            this.fault = fault;
            this.note = note;
        }
    }
}
