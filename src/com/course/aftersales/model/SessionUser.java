package com.course.aftersales.model;

import java.io.Serializable;

public class SessionUser implements Serializable {
    private final long id;
    private final String username;
    private final String displayName;
    private final String role;

    public SessionUser(long id, String username, String displayName, String role) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
    }
    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public String getRoleLabel() {
        if ("ENGINEER".equals(role)) return "工程师 / 客户";
        if ("CUSTOMER".equals(role)) return "客户";
        if ("WAREHOUSE".equals(role)) return "区域仓库";
        if ("ADMIN".equals(role)) return "平台管理员";
        return role;
    }
    public boolean hasRole(String expected) {
        if (expected == null) return false;
        if (expected.equals(role)) return true;
        return "CUSTOMER".equals(expected) && "ENGINEER".equals(role);
    }
}
