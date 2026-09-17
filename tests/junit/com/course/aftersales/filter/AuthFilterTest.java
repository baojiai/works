package com.course.aftersales.filter;

import com.course.aftersales.model.SessionUser;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthFilterTest {
    private final AuthFilter filter = new AuthFilter();

    @Test
    void anonymousUserIsRedirectedFromProtectedPage() throws Exception {
        MockHttpServletRequest request = request("/after-sales/dashboard", "/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals("/after-sales/login", response.getRedirectedUrl());
        assertNull(chain.getRequest());
    }

    @Test
    void publicAndStaticResourcesPassWithoutAuthentication() throws Exception {
        MockFilterChain loginChain = execute(request("/after-sales/client/login", "/client/login"));
        MockFilterChain assetChain = execute(request("/after-sales/assets/app.css", "/assets/app.css"));

        assertNotNull(loginChain.getRequest());
        assertNotNull(assetChain.getRequest());
    }

    @Test
    void authenticatedUserCanReachProtectedPage() throws Exception {
        MockHttpServletRequest request = request("/after-sales/dashboard", "/dashboard");
        MockHttpSession session = new MockHttpSession(null, "session-1");
        session.setAttribute("user", new SessionUser(1L, "customer", "客户", "CUSTOMER"));
        request.setSession(session);

        MockFilterChain chain = execute(request);

        assertNotNull(chain.getRequest());
    }

    private MockFilterChain execute(MockHttpServletRequest request) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    private static MockHttpServletRequest request(String uri, String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setContextPath("/after-sales");
        request.setServletPath(servletPath);
        return request;
    }
}
