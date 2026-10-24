
import java.sql.*;

public class FeeDAO {
    private DbConnection db = new DbConnection();

    public boolean addFee(String feeName, double amount,
                          int schoolYearId, int strandId) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                return false;
            }

            String sql = "INSERT INTO fees "
                    + "(fee_name, amount, school_year_id, strand_id) "
                    + "VALUES (?, ?, ?, ?)";

            ps = conn.prepareStatement(sql);
            ps.setString(1, feeName);
            ps.setDouble(2, amount);
            ps.setInt(3, schoolYearId);
            ps.setInt(4, strandId);

            int rowInserted = ps.executeUpdate();

            return rowInserted > 0;
        } catch (Exception e) {
            System.out.println("Fee error: " + e.getMessage());
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

    public String getFees() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringBuilder result = new StringBuilder();

        try {
            conn = db.getConnection();

            if (conn == null) {
                return "Error: Could not connect to the database.";
            }

            String sql = "SELECT fee_id, fee_name, amount FROM fees";

            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                result.append("Fee ID: ").append(rs.getInt("fee_id"))
                        .append(" | ")
                        .append(rs.getString("fee_name"))
                        .append(" | PHP ")
                        .append(rs.getBigDecimal("amount"))
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

        return result.length() == 0 ? "No fees found." : result.toString();
    }

    public double getTotalFees(int schoolYearId, int strandId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                return 0;
            }

            String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM fees "
                    + "WHERE school_year_id = ? AND strand_id = ?";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, schoolYearId);
            ps.setInt(2, strandId);

            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (Exception e) {
            System.out.println("Fee total error: " + e.getMessage());
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

        return 0;
    }
}
