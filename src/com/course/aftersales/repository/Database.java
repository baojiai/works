package com.course.aftersales.repository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public final class Database {
    private static String url;
    private static String user;
    private static String password;
    private static boolean h2;

    private Database() {}

    public static synchronized void initialize() {
        if (url != null) return;
        url = env("APP_DB_URL", "jdbc:h2:file:" + System.getProperty("catalina.base") + "/data/after_sales;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=SYSTEM_USER");
        user = env("APP_DB_USER", "sa");
        password = env("APP_DB_PASSWORD", "");
        String driver = env("APP_DB_DRIVER", url.startsWith("jdbc:mysql:") ? "com.mysql.cj.jdbc.Driver" : "org.h2.Driver");
        h2 = driver.equals("org.h2.Driver");
        try {
            Class.forName(driver);
            if (h2) initializeH2();
        } catch (Exception e) {
            throw new IllegalStateException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    public static Connection open() throws SQLException {
        if (url == null) initialize();
        Connection c = DriverManager.getConnection(url, user, password);
        c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        return c;
    }

    private static void initializeH2() throws Exception {
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            if (!tableExists(c, "system_user")) {
                runScript(c, "/schema-h2.sql");
                runScript(c, "/seed-h2.sql");
            }
            ensureEngineerApplicationTable(c);
        }
    }

    private static void ensureEngineerApplicationTable(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS engineer_application (" +
                    "application_id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, real_name VARCHAR(80) NOT NULL, id_card_no VARCHAR(40) NOT NULL, phone VARCHAR(30) NOT NULL, " +
                    "service_area_id BIGINT NOT NULL, experience_years INT NOT NULL DEFAULT 0, certificate_no VARCHAR(80), skill_description VARCHAR(1000) NOT NULL, material_description VARCHAR(1000) NOT NULL, " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', reviewer_id BIGINT, review_comment VARCHAR(500), reviewed_at TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(user_id) REFERENCES system_user(user_id), FOREIGN KEY(service_area_id) REFERENCES service_area(service_area_id), FOREIGN KEY(reviewer_id) REFERENCES system_user(user_id))");
            st.execute("CREATE TABLE IF NOT EXISTS engineer_application_skill (" +
                    "application_id BIGINT NOT NULL, fault_type_id BIGINT NOT NULL, PRIMARY KEY(application_id,fault_type_id), " +
                    "FOREIGN KEY(application_id) REFERENCES engineer_application(application_id), FOREIGN KEY(fault_type_id) REFERENCES fault_type(fault_type_id))");
        }
    }

    private static boolean tableExists(Connection c, String table) throws SQLException {
        try (ResultSet rs = c.getMetaData().getTables(null, null, table, new String[]{"TABLE"})) { return rs.next(); }
    }

    private static void runScript(Connection c, String resource) throws Exception {
        InputStream in = Database.class.getResourceAsStream(resource);
        if (in == null) throw new FileNotFoundException(resource);
        String text;
        try (Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) { text = scanner.hasNext() ? scanner.next() : ""; }
        for (String statement : text.split(";\\s*(?:\\r?\\n|$)")) {
            String sql = statement.trim();
            if (!sql.isEmpty()) try (Statement st = c.createStatement()) { st.execute(sql); }
        }
    }

    public static List<Map<String,Object>> query(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = prepare(c, sql, args); ResultSet rs = ps.executeQuery()) {
            List<Map<String,Object>> rows = new ArrayList<>();
            ResultSetMetaData md = rs.getMetaData();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= md.getColumnCount(); i++) row.put(md.getColumnLabel(i), rs.getObject(i));
                rows.add(row);
            }
            return rows;
        }
    }

    public static Map<String,Object> one(Connection c, String sql, Object... args) throws SQLException {
        List<Map<String,Object>> rows = query(c, sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static int update(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = prepare(c, sql, args)) { return ps.executeUpdate(); }
    }

    public static long insert(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, args);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getLong(1); }
        }
        throw new SQLException("No generated key returned");
    }

    private static PreparedStatement prepare(Connection c, String sql, Object... args) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        bind(ps, args);
        return ps;
    }

    private static void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
    }

    public static void tx(TxWork work) throws Exception {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try { work.run(c); c.commit(); }
            catch (Exception e) { c.rollback(); throw e; }
        }
    }

    public interface TxWork { void run(Connection connection) throws Exception; }
    private static String env(String key, String fallback) { String v = System.getenv(key); return v == null || v.trim().isEmpty() ? fallback : v.trim(); }
}
