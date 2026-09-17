package com.course.aftersales.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionUserTest {
    @Test
    void engineerKeepsCustomerCapabilities() {
        SessionUser engineer = new SessionUser(1L, "engineer", "工程师", "ENGINEER");

        assertTrue(engineer.hasRole("ENGINEER"));
        assertTrue(engineer.hasRole("CUSTOMER"));
        assertFalse(engineer.hasRole("ADMIN"));
        assertEquals("工程师 / 客户", engineer.getRoleLabel());
    }

    @Test
    void roleLabelsAreStable() {
        assertEquals("客户", user("CUSTOMER").getRoleLabel());
        assertEquals("区域仓库", user("WAREHOUSE").getRoleLabel());
        assertEquals("平台管理员", user("ADMIN").getRoleLabel());
        assertFalse(user("CUSTOMER").hasRole(null));
    }

    private static SessionUser user(String role) {
        return new SessionUser(1L, role.toLowerCase(), role, role);
    }
}
