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
    public boolean hasRole(String expected) { return expected != null && expected.equals(role); }
}

