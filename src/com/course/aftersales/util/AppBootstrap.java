package com.course.aftersales.util;

import com.course.aftersales.repository.Database;
import javax.servlet.*;

public class AppBootstrap implements ServletContextListener {
    @Override public void contextInitialized(ServletContextEvent event) {
        Database.initialize();
        event.getServletContext().log("After-sales system initialized.");
    }
}

