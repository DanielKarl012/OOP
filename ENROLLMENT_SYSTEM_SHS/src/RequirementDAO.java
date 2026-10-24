
import java.sql.*;

public class RequirementDAO {
    private DbConnection db = new DbConnection();

    public String getRequirements() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringBuilder result = new StringBuilder();

        try {
            conn = db.getConnection();

            if (conn == null) {
                return "Error: Could not connect to the database.";
            }

            String sql = "SELECT requirement_id, requirement_name FROM requirements";

            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                result.append(rs.getInt("requirement_id"))
                        .append(" - ")
                        .append(rs.getString("requirement_name"))
                        .append("\n");
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
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

        return result.length() == 0 ? "No requirements found." : result.toString();
    }

    public boolean addRequirement(String name) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                return false;
            }

            String sql = "INSERT INTO requirements (requirement_name) VALUES (?)";

            ps = conn.prepareStatement(sql);
            ps.setString(1, name);

            int rowInserted = ps.executeUpdate();

            return rowInserted > 0;
        } catch (Exception e) {
            System.out.println("Requirement error: " + e.getMessage());
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

    public boolean submitRequirement(int enrollmentId, int requirementId,
                                     String status) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                return false;
            }

            String sql = "INSERT INTO submitted_requirements "
                    + "(enrollment_id, requirement_id, status) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE status = VALUES(status)";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, enrollmentId);
            ps.setInt(2, requirementId);
            ps.setString(3, status);

            int rowInserted = ps.executeUpdate();

            return rowInserted > 0;
        } catch (Exception e) {
            System.out.println("Requirement submission error: " + e.getMessage());
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

    public String getSubmittedRequirements(int enrollmentId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringBuilder result = new StringBuilder();

        try {
            conn = db.getConnection();

            if (conn == null) {
                return "Error: Could not connect to the database.";
            }

            String sql = "SELECT r.requirement_name, sr.status "
                    + "FROM submitted_requirements sr "
                    + "JOIN requirements r ON sr.requirement_id = r.requirement_id "
                    + "WHERE sr.enrollment_id = ?";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, enrollmentId);

            rs = ps.executeQuery();

            while (rs.next()) {
                result.append(rs.getString("requirement_name"))
                        .append(": ")
                        .append(rs.getString("status"))
                        .append("\n");
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
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

        return result.length() == 0
                ? "No requirement records found."
                : result.toString();
    }
}
