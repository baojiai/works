package com.course.aftersales.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Passwords {
    private Passwords() {}
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) result.append(String.format("%02x", b));
            return result.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}

