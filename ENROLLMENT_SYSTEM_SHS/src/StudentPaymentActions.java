
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentPaymentActions {

    private StudentDashboard dashboard;
    private User user;
    private JTextArea outputArea;
    private JLabel helpLabel;
    private PaymentDAO paymentDAO = new PaymentDAO();
    private StudentPaymentDAO studentPaymentDAO = new StudentPaymentDAO();

    public StudentPaymentActions(StudentDashboard dashboard) {
        this.dashboard = dashboard;
        this.user = dashboard.user;
        this.outputArea = dashboard.outputArea;
        this.helpLabel = dashboard.helpLabel;
    }

    public void submitPayment() {
        List<StudentDashboard.ChoiceItem> requests = new ArrayList<>();

        String sql =
            "SELECT pr.request_id, pr.amount_due, pr.description "
            + "FROM payment_requests pr "
            + "JOIN enrollments e "
            + "ON pr.enrollment_id = e.enrollment_id "
            + "JOIN students s ON e.student_id = s.student_id "
            + "WHERE s.user_id = ? AND pr.request_status = 'Open' "
            + "ORDER BY pr.request_id DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = dashboard.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            ps = conn.prepareStatement(sql);
            ps.setInt(1, user.getUserId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    requests.add(new StudentDashboard.ChoiceItem(
                        rs.getInt("request_id"),
                        "Request #" + rs.getInt("request_id")
                            + " - PHP "
                            + String.format(
                                "%.2f", rs.getDouble("amount_due")
                            )
                            + " - " + rs.getString("description")
                    ));
                }
            }
        } catch (Exception ex) {
            StudentDashboard.showError(dashboard,
                "Could not load payment requests: " + ex.getMessage()
            );
            return;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception closeEx) {
                System.out.println(closeEx);
            }

            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception closeEx) {
                System.out.println(closeEx);
            }
        }

        if (requests.isEmpty()) {
            StudentDashboard.showError(dashboard, "You have no open payment requests.");
            return;
        }

        StudentDashboard.ChoiceItem selectedRequest = (StudentDashboard.ChoiceItem)
            JOptionPane.showInputDialog(
                dashboard,
                "Select a payment request:",
                "Submit Payment",
                JOptionPane.QUESTION_MESSAGE,
                null,
                requests.toArray(),
                requests.get(0)
            );

        if (selectedRequest == null) {
            return;
        }

        JComboBox<String> methodBox = new JComboBox<>(
            new String[]{
                "Cash",
                "Bank Transfer",
                "GCash",
                "Maya",
                "Other"
            }
        );
        JTextField referenceField = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Payment Method:"));
        form.add(methodBox);
        form.add(new JLabel("Reference Number:"));
        form.add(referenceField);

        int result = JOptionPane.showConfirmDialog(
            dashboard,
            form,
            "Payment Information",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String method = methodBox.getSelectedItem().toString();
        String reference = referenceField.getText().trim();

        if (reference.isEmpty()) {
            StudentDashboard.showError(dashboard, "Enter your payment reference number.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            dashboard,
            "Submit payment information?\n\n"
                + selectedRequest.label
                + "\nMethod: " + method
                + "\nReference: " + reference,
            "Confirm Payment Submission",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            studentPaymentDAO.addPaymentForStudent(
                selectedRequest.id,
                user.getUserId(),
                method,
                reference
            );

            outputArea.setText(
                "PAYMENT INFORMATION SUBMITTED\n\n"
                + selectedRequest.label
                + "\nMethod: " + method
                + "\nReference: " + reference
                + "\n\nYour payment is awaiting verification."
            );

            JOptionPane.showMessageDialog(
                dashboard,
                "Payment information submitted. "
                + "Wait for staff verification."
            );
        } catch (Exception ex) {
            StudentDashboard.showError(dashboard,
                "Could not submit payment: " + ex.getMessage()
            );
        }
    }

    public void viewPaymentRequests() {
        try {
            outputArea.setText("MY PAYMENT REQUESTS\n\n"
                + studentPaymentDAO.getPaymentRequestsForStudent(user.getUserId()));
        } catch (SQLException ex) {
            StudentDashboard.showError(
                dashboard,
                "Could not load payment requests: " + ex.getMessage()
            );
        }
    }

    public void viewRequestBreakdown() {
        String input = JOptionPane.showInputDialog(
            dashboard,
            "Enter the payment request ID:"
        );

        if (input == null || input.trim().isEmpty()) {
            return;
        }

        int requestId;

        try {
            requestId = Integer.parseInt(input.trim());

            if (requestId <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            StudentDashboard.showError(dashboard, "Enter a valid positive request ID.");
            return;
        }

        try {
            outputArea.setText("PAYMENT REQUEST BREAKDOWN\n\n"
                + studentPaymentDAO.getRequestDetailsForStudent(requestId, user.getUserId()));
        } catch (SQLException ex) {
            StudentDashboard.showError(
                dashboard,
                "Could not load request breakdown: " + ex.getMessage()
            );
        }
    }

    public void viewMyPayments() {
        try {
            outputArea.setText("MY PAYMENT RECORDS\n\n"
                + paymentDAO.getPaymentsForStudent(user.getUserId()));
        } catch (SQLException ex) {
            StudentDashboard.showError(
                dashboard,
                "Could not load payments: " + ex.getMessage()
            );
        }
    }

}
