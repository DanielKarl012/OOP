
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DeleteDAO {
    private DbConnection db = new DbConnection();

    public boolean deleteOwnRequirement(int requirementId, int userId)
            throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            String sql =
                    "DELETE sr FROM student_requirements sr " +
                    "JOIN enrollments e ON sr.enrollment_id = e.enrollment_id " +
                    "JOIN students s ON e.student_id = s.student_id " +
                    "WHERE sr.requirement_id = ? AND s.user_id = ? " +
                    "AND LOWER(sr.requirement_status) IN ('submitted', 'incomplete')";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, requirementId);
            ps.setInt(2, userId);

            int rowDeleted = ps.executeUpdate();

            return rowDeleted > 0;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e.getMessage(), e);
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

    public boolean deleteRequirement(int requirementId)
            throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            String sql =
                    "DELETE FROM student_requirements " +
                    "WHERE requirement_id = ? " +
                    "AND LOWER(requirement_status) IN ('submitted', 'incomplete')";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, requirementId);

            int rowDeleted = ps.executeUpdate();

            return rowDeleted > 0;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e.getMessage(), e);
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

    public boolean cancelPayment(int paymentId)
            throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            String sql =
                    "UPDATE payments SET payment_status = 'Cancelled' " +
                    "WHERE payment_id = ? " +
                    "AND LOWER(payment_status) = 'pending'";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, paymentId);

            int rowUpdated = ps.executeUpdate();

            return rowUpdated > 0;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e.getMessage(), e);
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

    public boolean cancelEnrollment(int enrollmentId, int userId)
            throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            String sql =
                    "UPDATE enrollments e " +
                    "JOIN students s ON e.student_id = s.student_id " +
                    "SET e.enrollment_status = 'Cancelled' " +
                    "WHERE e.enrollment_id = ? AND s.user_id = ? " +
                    "AND LOWER(e.enrollment_status) IN ('pending', 'rejected')";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, enrollmentId);
            ps.setInt(2, userId);

            int rowUpdated = ps.executeUpdate();

            return rowUpdated > 0;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e.getMessage(), e);
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

    public boolean deactivateUser(int userId)
            throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            String sql =
                    "UPDATE users SET is_active = 0 WHERE user_id = ?";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);

            int rowUpdated = ps.executeUpdate();

            return rowUpdated > 0;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e.getMessage(), e);
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

    public String deletePayment(int paymentId) throws SQLException {
        if (paymentId <= 0) {
            throw new SQLException("Enter a valid Payment ID.");
        }

        try (Connection conn = new DbConnection().getConnection()) {
            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            conn.setAutoCommit(false);

            try {
                String status;
                int requestId;

                String findSql =
                        "SELECT request_id, payment_status FROM payments " +
                        "WHERE payment_id = ? FOR UPDATE";

                try (PreparedStatement ps = conn.prepareStatement(findSql)) {
                    ps.setInt(1, paymentId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Payment ID was not found.");
                        }

                        requestId = rs.getInt("request_id");
                        status = rs.getString("payment_status");
                    }
                }

                if ("Paid".equalsIgnoreCase(status)) {
                    throw new SQLException(
                            "Paid payments cannot be deleted. Preserve verified transaction history.");
                }

                String deleteSql =
                        "DELETE FROM payments WHERE payment_id = ? " +
                        "AND (payment_status IS NULL OR payment_status <> 'Paid')";

                try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                    ps.setInt(1, paymentId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Payment record could not be deleted.");
                    }
                }

                if (requestId > 0) {
                    String resetSql =
                            "UPDATE payment_requests SET request_status = 'Open' " +
                            "WHERE request_id = ? AND request_status = 'Payment Pending' " +
                            "AND NOT EXISTS (" +
                            "SELECT 1 FROM payments WHERE request_id = ? " +
                            "AND payment_status = 'Pending')";

                    try (PreparedStatement ps = conn.prepareStatement(resetSql)) {
                        ps.setInt(1, requestId);
                        ps.setInt(2, requestId);
                        ps.executeUpdate();
                    }
                }

                conn.commit();

                return "Payment record deleted successfully.\nPayment ID: " + paymentId;

            } catch (Exception ex) {
                conn.rollback();

                if (ex instanceof SQLException) {
                    throw (SQLException) ex;
                }

                throw new SQLException("Could not delete payment.", ex);

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public String deletePaymentRequest(int requestId) throws SQLException {
        if (requestId <= 0) {
            throw new SQLException("Enter a valid Request ID.");
        }

        try (Connection conn = new DbConnection().getConnection()) {
            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            conn.setAutoCommit(false);

            try {
                String status;

                String findSql =
                        "SELECT request_status FROM payment_requests " +
                        "WHERE request_id = ? FOR UPDATE";

                try (PreparedStatement ps = conn.prepareStatement(findSql)) {
                    ps.setInt(1, requestId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Payment request was not found.");
                        }

                        status = rs.getString("request_status");
                    }
                }

                if ("Paid".equalsIgnoreCase(status)) {
                    throw new SQLException("Paid payment requests cannot be deleted.");
                }

                String paymentCheckSql =
                        "SELECT COUNT(*) FROM payments WHERE request_id = ?";

                try (PreparedStatement ps = conn.prepareStatement(paymentCheckSql)) {
                    ps.setInt(1, requestId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new SQLException(
                                    "This request has payment records. Resolve or delete those records first.");
                        }
                    }
                }

                String detailSql =
                        "DELETE FROM payment_request_details WHERE request_id = ?";

                try (PreparedStatement ps = conn.prepareStatement(detailSql)) {
                    ps.setInt(1, requestId);
                    ps.executeUpdate();
                }

                String deleteSql =
                        "DELETE FROM payment_requests WHERE request_id = ?";

                try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                    ps.setInt(1, requestId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Payment request was not deleted.");
                    }
                }

                conn.commit();

                return "Payment request deleted successfully.\nRequest ID: " + requestId;

            } catch (Exception ex) {
                conn.rollback();

                if (ex instanceof SQLException) {
                    throw (SQLException) ex;
                }

                throw new SQLException("Could not delete payment request.", ex);

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
