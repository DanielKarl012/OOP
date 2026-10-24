
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    public String getDiscounts() throws SQLException {
        StringBuilder result = new StringBuilder();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = new DbConnection().getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            String sql = "SELECT discount_id, discount_name, discount_type, discount_value " +
                    "FROM discounts ORDER BY discount_id";

            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            boolean found = false;

            while (rs.next()) {
                found = true;
                result.append("Discount ID: ").append(rs.getInt("discount_id"))
                        .append("\nName: ").append(rs.getString("discount_name"))
                        .append("\nType: ").append(rs.getString("discount_type"))
                        .append("\nValue: ").append(rs.getBigDecimal("discount_value"))
                        .append("\n------------------------------\n");
            }

            if (!found) {
                result.append("No discounts are available.");
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

        return result.toString();
    }

    public String createPaymentRequest(int enrollmentId, List<Integer> discountIds)
            throws SQLException {

        if (discountIds == null) {
            discountIds = new ArrayList<>();
        }

        try (Connection conn = new DbConnection().getConnection()) {
            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            conn.setAutoCommit(false);

            try {
                int schoolYearId;
                int strandId;

                String enrollmentSql =
                        "SELECT school_year_id, strand_id FROM enrollments " +
                        "WHERE enrollment_id = ? FOR UPDATE";

                try (PreparedStatement ps = conn.prepareStatement(enrollmentSql)) {
                    ps.setInt(1, enrollmentId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Enrollment ID was not found.");
                        }

                        schoolYearId = rs.getInt("school_year_id");
                        strandId = rs.getInt("strand_id");
                    }
                }

                List<FeeItem> items = new ArrayList<>();
                double total = 0.0;

                String feeSql =
                        "SELECT fee_name, amount FROM fees " +
                        "WHERE school_year_id = ? AND strand_id = ?";

                try (PreparedStatement ps = conn.prepareStatement(feeSql)) {
                    ps.setInt(1, schoolYearId);
                    ps.setInt(2, strandId);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String name = rs.getString("fee_name");
                            double amount = rs.getDouble("amount");

                            items.add(new FeeItem(name, "Fee", amount));
                            total += amount;
                        }
                    }
                }

                if (items.isEmpty()) {
                    throw new SQLException(
                            "No fees are configured for this enrollment's school year and strand.");
                }

                for (Integer discountId : discountIds) {
                    if (discountId == null) {
                        throw new SQLException("A discount ID cannot be empty.");
                    }

                    String discountSql =
                            "SELECT discount_name, discount_type, discount_value " +
                            "FROM discounts WHERE discount_id = ?";

                    try (PreparedStatement ps = conn.prepareStatement(discountSql)) {
                        ps.setInt(1, discountId);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new SQLException(
                                        "Discount ID " + discountId + " was not found.");
                            }

                            String name = rs.getString("discount_name");
                            String type = rs.getString("discount_type");
                            double value = rs.getDouble("discount_value");
                            double discountAmount;

                            if ("percentage".equalsIgnoreCase(type)
                                    || "percent".equalsIgnoreCase(type)) {
                                if (value < 0 || value > 100) {
                                    throw new SQLException(
                                            "Percentage discounts must be between 0 and 100.");
                                }

                                discountAmount = total * value / 100.0;
                            } else if ("fixed".equalsIgnoreCase(type)
                                    || "amount".equalsIgnoreCase(type)) {
                                if (value < 0) {
                                    throw new SQLException("Discount value cannot be negative.");
                                }

                                discountAmount = value;
                            } else {
                                throw new SQLException(
                                        "Unknown discount type for " + name +
                                        ". Use Percentage or Fixed.");
                            }

                            discountAmount = Math.min(discountAmount, total);
                            discountAmount = Math.round(discountAmount * 100.0) / 100.0;

                            items.add(new FeeItem(name, "Discount", -discountAmount));
                            total = Math.max(0.0, total - discountAmount);
                        }
                    }
                }

                total = Math.round(total * 100.0) / 100.0;

                String requestSql =
                        "INSERT INTO payment_requests " +
                        "(enrollment_id, amount_due, description, request_status) " +
                        "VALUES (?, ?, ?, 'Open')";

                int requestId;

                try (PreparedStatement ps = conn.prepareStatement(
                        requestSql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setInt(1, enrollmentId);
                    ps.setDouble(2, total);
                    ps.setString(3, "Enrollment fees and selected discounts");
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Could not create the payment request.");
                        }

                        requestId = keys.getInt(1);
                    }
                }

                String detailSql =
                        "INSERT INTO payment_request_details " +
                        "(request_id, item_name, item_type, amount) VALUES (?, ?, ?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(detailSql)) {
                    for (FeeItem item : items) {
                        ps.setInt(1, requestId);
                        ps.setString(2, item.name);
                        ps.setString(3, item.type);
                        ps.setDouble(4, item.amount);
                        ps.addBatch();
                    }

                    ps.executeBatch();
                }

                conn.commit();

                return "Payment request created successfully.\n\n" +
                        "Request ID: " + requestId + "\n" +
                        "Enrollment ID: " + enrollmentId + "\n" +
                        "Total Amount Due: PHP " + String.format("%.2f", total) + "\n" +
                        "Status: Open";

            } catch (Exception ex) {
                conn.rollback();

                if (ex instanceof SQLException) {
                    throw (SQLException) ex;
                }

                throw new SQLException("Could not create payment request.", ex);

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public String getPaymentsForStudent(int userId) throws SQLException {
        String sql =
                "SELECT p.payment_id, p.request_id, p.enrollment_id, " +
                "p.amount_paid, p.payment_method, p.reference_number, " +
                "p.payment_status, p.paid_at " +
                "FROM payments p " +
                "JOIN enrollments e ON p.enrollment_id = e.enrollment_id " +
                "JOIN students s ON e.student_id = s.student_id " +
                "WHERE s.user_id = ? ORDER BY p.payment_id DESC";

        return readPayments(sql, userId);
    }

    public String getPendingPayments() throws SQLException {
        String sql =
                "SELECT p.payment_id, p.request_id, p.enrollment_id, " +
                "p.amount_paid, p.payment_method, p.reference_number, " +
                "p.payment_status, p.paid_at FROM payments p " +
                "WHERE p.payment_status = 'Pending' ORDER BY p.payment_id DESC";

        return readPayments(sql, 0);
    }

    public String getPayments() throws SQLException {
        String sql =
                "SELECT p.payment_id, p.request_id, p.enrollment_id, " +
                "p.amount_paid, p.payment_method, p.reference_number, " +
                "p.payment_status, p.paid_at FROM payments p " +
                "ORDER BY p.payment_id DESC";

        return readPayments(sql, 0);
    }

    private String readPayments(String sql, int userId) throws SQLException {
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

            if (sql.contains("s.user_id = ?")) {
                ps.setInt(1, userId);
            }

            rs = ps.executeQuery();

            while (rs.next()) {
                found = true;
                result.append("Payment ID: ").append(rs.getInt("payment_id"))
                        .append("\nRequest ID: ").append(rs.getObject("request_id"))
                        .append("\nEnrollment ID: ").append(rs.getInt("enrollment_id"))
                        .append("\nAmount: PHP ")
                        .append(String.format("%.2f", rs.getDouble("amount_paid")))
                        .append("\nMethod: ").append(rs.getString("payment_method"))
                        .append("\nReference: ").append(rs.getString("reference_number"))
                        .append("\nStatus: ").append(rs.getString("payment_status"))
                        .append("\nDate: ").append(rs.getTimestamp("paid_at"))
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
            result.append("No payment records found.");
        }

        return result.toString();
    }

    public String markPaymentPaid(int paymentId) throws SQLException {
        try (Connection conn = new DbConnection().getConnection()) {
            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            conn.setAutoCommit(false);

            try {
                int requestId;
                String status;

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
                    throw new SQLException("This payment is already marked as Paid.");
                }

                String updatePaymentSql =
                        "UPDATE payments SET payment_status = 'Paid', " +
                        "paid_at = CURRENT_TIMESTAMP WHERE payment_id = ?";

                try (PreparedStatement ps = conn.prepareStatement(updatePaymentSql)) {
                    ps.setInt(1, paymentId);
                    ps.executeUpdate();
                }

                String updateRequestSql =
                        "UPDATE payment_requests SET request_status = 'Paid' " +
                        "WHERE request_id = ?";

                try (PreparedStatement ps = conn.prepareStatement(updateRequestSql)) {
                    ps.setInt(1, requestId);
                    ps.executeUpdate();
                }

                conn.commit();

                return "Payment verified successfully.\n" +
                        "Payment ID: " + paymentId + "\n" +
                        "Request ID: " + requestId + "\n" +
                        "Status: Paid";

            } catch (Exception ex) {
                conn.rollback();

                if (ex instanceof SQLException) {
                    throw (SQLException) ex;
                }

                throw new SQLException("Could not verify payment.", ex);

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static class FeeItem {
        private String name;
        private String type;
        private double amount;

        public FeeItem(String name, String type, double amount) {
            this.name = name;
            this.type = type;
            this.amount = amount;
        }
    }
}
