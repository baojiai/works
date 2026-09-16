import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class Stage9MySqlConnectionProbe {
    public static void main(String[] args) throws Exception {
        String url = System.getenv("APP_DB_URL");
        String user = System.getenv("APP_DB_USER");
        String password = System.getenv("APP_DB_PASSWORD");
        String driver = System.getenv("APP_DB_DRIVER");
        if (url == null || user == null || password == null) throw new IllegalStateException("APP_DB_URL/USER/PASSWORD 环境变量未设置");
        Class.forName(driver != null ? driver : "com.mysql.cj.jdbc.Driver");
        try (Connection c = DriverManager.getConnection(url, user, password);
             Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT VERSION()")) {
                rs.next();
                System.out.println("VERSION=" + rs.getString(1));
            }
            try (ResultSet rs = st.executeQuery("SELECT CURRENT_USER()")) {
                rs.next();
                System.out.println("CURRENT_USER=" + rs.getString(1));
            }
            try (ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
                rs.next();
                System.out.println("DATABASE=" + rs.getString(1));
            }
        }
        System.out.println("MYSQL_CONNECTION_OK");
    }
}
