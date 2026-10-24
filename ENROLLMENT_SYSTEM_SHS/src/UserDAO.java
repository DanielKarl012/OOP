
import java.sql.*;

public class UserDAO {
    private DbConnection db = new DbConnection();

    public User login(String username, String passwordHash) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                return null;
            }

            String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ?";

            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, passwordHash);

            rs = ps.executeQuery();

            if (rs.next()) {
                return new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("role")
                );
            }
        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
                System.out.println(e);
            }

            try {
                if (ps != null) {
                    ps.close();
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

        return null;
    }

    public boolean addUser(String username, String passwordHash,
                           String email, String role) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                return false;
            }

            String sql = "INSERT INTO users "
                       + "(username, password_hash, email, role) VALUES (?, ?, ?, ?)";

            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, email);
            ps.setString(4, role);

            int rowInserted = ps.executeUpdate();

            return rowInserted > 0;
        } catch (Exception e) {
            System.out.println("User creation error: " + e.getMessage());
            return false;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
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

    public int getUserId(String username) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                return -1;
            }

            String sql = "SELECT user_id FROM users WHERE username = ?";

            ps = conn.prepareStatement(sql);
            ps.setString(1, username);

            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("user_id");
            }
        } catch (Exception e) {
            System.out.println("User lookup error: " + e.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
                System.out.println(e);
            }

            try {
                if (ps != null) {
                    ps.close();
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

        return -1;
    }
}
