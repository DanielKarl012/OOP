import java.sql.*;

public class DbConnection {
    String url = "jdbc:mysql://localhost:3307/enrollment_system";
    String username = "root";
    String password = "120604";

    public Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(url, username, password);
            return conn;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public void connect() {
        Connection conn = null;
        Statement st = null;
        ResultSet result = null;

        try {
            conn = getConnection();

            if (conn == null) {
                return;
            }

            st = conn.createStatement();

            String sql = "SELECT * FROM users";

            result = st.executeQuery(sql);

            System.out.println("Database connected successfully!");

            while (result.next()) {
                System.out.println("User ID: " + result.getInt("user_id"));
                System.out.println("Username: " + result.getString("username"));
                System.out.println("Email: " + result.getString("email"));
                System.out.println("----------------------");
            }
        } catch (Exception e) {
            System.out.println("Database connection error: " + e.getMessage());
        } finally {
            try {
                if (result != null) {
                    result.close();
                }
            } catch (Exception e) {
                System.out.println(e);
            }

            try {
                if (st != null) {
                    st.close();
                }
            } catch (Exception e) {
                System.out.println(e);
            }

            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}
