
import java.sql.*;

public class StudentPaymentDAO {

    public String getOpenPaymentRequestsForStudent(int userId) throws SQLException {
        return readRequests(userId, true);
    }

    public String getPaymentRequestsForStudent(int userId) throws SQLException {
        return readRequests(userId, false);
    }

    private String readRequests(int userId, boolean onlyUnpaid) throws SQLException {
        String sql =
                "SELECT pr.request_id, pr.enrollment_id, pr.amount_due, " +
                "pr.description, pr.request_status, pr.created_at " +
                "FROM payment_requests pr " +
                "JOIN enrollments e ON pr.enrollment_id = e.enrollment_id " +
                "JOIN students s ON e.student_id = s.student_id " +
                "WHERE s.user_id = ? " +
                (onlyUnpaid ? "AND pr.request_status <> 'Paid' " : "") +
                "ORDER BY pr.request_id DESC";

        StringBuilder result = new StringBuilder();
        boolean found = false;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = new DbConnection().getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);

            rs = ps.executeQuery();

            while (rs.next()) {
                found = true;
                result.append("Request ID: ").append(rs.getInt("request_id"))
                        .append("\nEnrollment ID: ").append(rs.getInt("enrollment_id"))
                        .append("\nAmount Due: PHP ")
                        .append(String.format("%.2f", rs.getDouble("amount_due")))
                        .append("\nDescription: ").append(rs.getString("description"))
                        .append("\nStatus: ").append(rs.getString("request_status"))
                        .append("\nCreated: ").append(rs.getTimestamp("created_at"))
                        .append("\n------------------------------\n");
            }
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e.getMessage(), e);
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

        if (!found) {
            result.append("No payment requests found.");
        }

        return result.toString();
    }

    public String getRequestDetailsForStudent(int requestId, int userId)
            throws SQLException {

        String requestSql =
                "SELECT pr.request_id, pr.enrollment_id, pr.amount_due, " +
                "pr.description, pr.request_status " +
                "FROM payment_requests pr " +
                "JOIN enrollments e ON pr.enrollment_id = e.enrollment_id " +
                "JOIN students s ON e.student_id = s.student_id " +
                "WHERE pr.request_id = ? AND s.user_id = ?";

        String detailSql =
                "SELECT item_name, item_type, amount " +
                "FROM payment_request_details WHERE request_id = ?";

        StringBuilder result = new StringBuilder();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        PreparedStatement psDetails = null;
        ResultSet rsDetails = null;

        try {
            conn = new DbConnection().getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            ps = conn.prepareStatement(requestSql);
            ps.setInt(1, requestId);
            ps.setInt(2, userId);

            rs = ps.executeQuery();

            if (!rs.next()) {
                return "Payment request not found for your account.";
            }

            result.append("Request ID: ").append(rs.getInt("request_id"))
                    .append("\nEnrollment ID: ").append(rs.getInt("enrollment_id"))
                    .append("\nDescription: ").append(rs.getString("description"))
                    .append("\nAmount Due: PHP ")
                    .append(String.format("%.2f", rs.getDouble("amount_due")))
                    .append("\nStatus: ").append(rs.getString("request_status"))
                    .append("\n\nITEM BREAKDOWN\n");

            psDetails = conn.prepareStatement(detailSql);
            psDetails.setInt(1, requestId);

            rsDetails = psDetails.executeQuery();

            while (rsDetails.next()) {
                result.append(rsDetails.getString("item_name"))
                        .append(" (").append(rsDetails.getString("item_type"))
                        .append("): PHP ")
                        .append(String.format("%.2f", rsDetails.getDouble("amount")))
                        .append("\n");
            }
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e.getMessage(), e);
        } finally {
            try {
                if (rsDetails != null) {
                    rsDetails.close();
                }
            } catch (Exception e) {
                System.out.println(e);
            }

            try {
                if (psDetails != null) {
                    psDetails.close();
                }
            } catch (Exception e) {
                System.out.println(e);
            }

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

        return result.toString();
    }

    public String addPaymentForStudent(
            int requestId, int userId, String method, String reference)
            throws SQLException {

        if (method == null || method.trim().isEmpty()) {
            throw new SQLException("Please select a payment method.");
        }

        try (Connection conn = new DbConnection().getConnection()) {
            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            conn.setAutoCommit(false);

            try {
                double amountDue;
                String status;

                String requestSql =
                        "SELECT pr.amount_due, pr.request_status " +
                        "FROM payment_requests pr " +
                        "JOIN enrollments e ON pr.enrollment_id = e.enrollment_id " +
                        "JOIN students s ON e.student_id = s.student_id " +
                        "WHERE pr.request_id = ? AND s.user_id = ? FOR UPDATE";

                try (PreparedStatement ps = conn.prepareStatement(requestSql)) {
                    ps.setInt(1, requestId);
                    ps.setInt(2, userId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Payment request not found for your account.");
                        }

                        amountDue = rs.getDouble("amount_due");
                        status = rs.getString("request_status");
                    }
                }

                if ("Paid".equalsIgnoreCase(status)) {
                    throw new SQLException("This payment request is already paid.");
                }

                if ("Payment Pending".equalsIgnoreCase(status)) {
                    throw new SQLException(
                            "A payment has already been submitted for this request.");
                }

                String insertSql =
                        "INSERT INTO payments " +
                        "(request_id, enrollment_id, amount_paid, payment_method, " +
                        "reference_number, payment_status) " +
                        "SELECT ?, enrollment_id, ?, ?, ?, 'Pending' " +
                        "FROM payment_requests WHERE request_id = ?";

                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, requestId);
                    ps.setDouble(2, amountDue);
                    ps.setString(3, method.trim());
                    ps.setString(4,
                            reference == null || reference.trim().isEmpty()
                                    ? null : reference.trim());
                    ps.setInt(5, requestId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Could not save the payment.");
                    }
                }

                String updateSql =
                        "UPDATE payment_requests SET request_status = 'Payment Pending' " +
                        "WHERE request_id = ?";

                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, requestId);
                    ps.executeUpdate();
                }

                conn.commit();

                return "Payment information submitted successfully.\n" +
                        "Request ID: " + requestId + "\n" +
                        "Amount: PHP " + String.format("%.2f", amountDue) + "\n" +
                        "Payment Status: Pending\n" +
                        "The cashier must verify the payment.";

            } catch (Exception ex) {
                conn.rollback();

                if (ex instanceof SQLException) {
                    throw (SQLException) ex;
                }

                throw new SQLException("Could not submit payment.", ex);

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
