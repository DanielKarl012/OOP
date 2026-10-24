
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class CashierDashboard extends JFrame {
    private User user;
    private PaymentDAO paymentDAO = new PaymentDAO();
    private DbConnection db = new DbConnection();
    private JTextArea outputArea = new JTextArea();

    public CashierDashboard(User user) {
        this.user = user;

        setTitle("Student Enrollment System - Cashier Dashboard");
        setSize(900, 550);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel("CASHIER DASHBOARD - PAYMENTS ONLY");
        heading.setFont(new Font("Arial", Font.BOLD, 19));

        JButton pendingButton = new JButton("View Pending Payments");
        JButton historyButton = new JButton("View Payment History");
        JButton verifyButton = new JButton("Verify Payment");
        JButton deleteButton = new JButton("Delete Payment Record");
        JButton refreshButton = new JButton("Refresh Payments");
        JButton logoutButton = new JButton("Logout");

        pendingButton.setToolTipText(
                "Display payments awaiting cashier verification."
        );

        historyButton.setToolTipText(
                "Display payment records and their statuses."
        );

        verifyButton.setToolTipText(
                "Mark a pending payment as Paid after verifying the actual payment."
        );

        deleteButton.setToolTipText(
                "Delete an eligible payment record after confirmation."
        );

        refreshButton.setToolTipText(
                "Reload the latest payment records from MySQL."
        );

        JPanel menu = new JPanel(new GridLayout(0, 1, 5, 5));
        menu.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        menu.add(pendingButton);
        menu.add(historyButton);
        menu.add(verifyButton);
        menu.add(deleteButton);
        menu.add(refreshButton);
        menu.add(logoutButton);

        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        outputArea.setText(
                "Welcome to the Cashier Dashboard.\n\n" +
                "1. View pending payments.\n" +
                "2. View payment history.\n" +
                "3. Verify payments after checking actual payment.\n" +
                "4. Delete an eligible payment record.\n\n" +
                "Example: Enter Payment ID 1 to verify a pending payment."
        );

        JPanel top = new JPanel(new BorderLayout());
        top.add(heading, BorderLayout.CENTER);
        top.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(menu), BorderLayout.WEST);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        pendingButton.addActionListener(e -> viewPendingPayments());
        historyButton.addActionListener(e -> viewPaymentHistory());
        verifyButton.addActionListener(e -> verifyPayment());
        deleteButton.addActionListener(e -> deletePayment());
        refreshButton.addActionListener(e -> viewPaymentHistory());

        logoutButton.addActionListener(e -> {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Log out of the cashier dashboard?",
                    "Confirm Logout",
                    JOptionPane.YES_NO_OPTION
            );

            if (answer == JOptionPane.YES_OPTION) {
                dispose();
                new LoginFrame().setVisible(true);
            }
        });
    }

    private void viewPendingPayments() {
        try {
            outputArea.setText(
                    "PENDING PAYMENTS\n\n" + paymentDAO.getPendingPayments()
            );
        } catch (Exception ex) {
            showError("Could not load pending payments.\n" + ex.getMessage());
        }
    }

    private void viewPaymentHistory() {
        try {
            outputArea.setText(
                    "PAYMENT HISTORY\n\n" + paymentDAO.getPayments()
            );
        } catch (Exception ex) {
            showError("Could not load payment history.\n" + ex.getMessage());
        }
    }

    private void verifyPayment() {
        String input = JOptionPane.showInputDialog(
                this,
                "Enter the Payment ID to verify.\nExample: 1"
        );

        if (input == null) {
            return;
        }

        int paymentId;

        try {
            paymentId = Integer.parseInt(input.trim());

            if (paymentId <= 0) {
                showError("Payment ID must be a positive number.");
                return;
            }
        } catch (NumberFormatException ex) {
            showError("Payment ID must be a positive number.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Have you confirmed that the actual payment was received?\n\n" +
                "Only choose Yes after checking the payment.\n" +
                "Payment ID: " + paymentId,
                "Confirm Payment Verification",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            String result = paymentDAO.markPaymentPaid(paymentId);
            outputArea.setText("PAYMENT VERIFICATION RESULT\n\n" + result);
        } catch (Exception ex) {
            showError("Could not verify payment.\n" + ex.getMessage());
        }
    }

    private void deletePayment() {
        String input = JOptionPane.showInputDialog(
                this,
                "Enter the Payment ID you want to delete.\n\n" +
                "Use View Payment History to find the ID.",
                "Delete Payment Record",
                JOptionPane.QUESTION_MESSAGE
        );

        if (input == null) {
            return;
        }

        int paymentId;

        try {
            paymentId = Integer.parseInt(input.trim());

            if (paymentId <= 0) {
                showError("Payment ID must be a positive number.");
                return;
            }
        } catch (NumberFormatException ex) {
            showError("Enter a valid numeric Payment ID.");
            return;
        }

        String paymentStatus;
        double amountPaid;
        String referenceNumber;

        String selectSql =
                "SELECT payment_status, amount_paid, reference_number " +
                "FROM payments WHERE payment_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = db.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            ps = conn.prepareStatement(selectSql);
            ps.setInt(1, paymentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    showError("No payment record found with ID " + paymentId + ".");
                    return;
                }

                paymentStatus = rs.getString("payment_status");
                amountPaid = rs.getDouble("amount_paid");
                referenceNumber = rs.getString("reference_number");
            }
        } catch (Exception ex) {
            showError(
                    "Could not find the payment record.\n" +
                    "Check the payments table columns.\n\n" + ex.getMessage()
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

        if (paymentStatus != null &&
                paymentStatus.equalsIgnoreCase("Paid")) {
            showError(
                    "This payment is already marked as Paid.\n\n" +
                    "For accurate financial records, paid transactions cannot " +
                    "be deleted from the cashier dashboard."
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete this payment record?\n\n" +
                "Payment ID: " + paymentId + "\n" +
                "Amount Paid: " + amountPaid + "\n" +
                "Reference Number: " +
                (referenceNumber == null ? "None" : referenceNumber) + "\n" +
                "Status: " + paymentStatus + "\n\n" +
                "This action cannot be undone. The database may prevent deletion " +
                "if other records reference this payment.",
                "Confirm Payment Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String deleteSql =
                "DELETE FROM payments " +
                "WHERE payment_id = ? AND LOWER(payment_status) <> 'paid'";

        conn = null;
        ps = null;
        try {
            conn = db.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            ps = conn.prepareStatement(deleteSql);
            ps.setInt(1, paymentId);

            int deleted = ps.executeUpdate();

            if (deleted > 0) {
                outputArea.setText(
                        "PAYMENT RECORD DELETED\n\n" +
                        "Payment ID: " + paymentId + "\n" +
                        "Amount Paid: " + amountPaid + "\n" +
                        "Reference Number: " +
                        (referenceNumber == null ? "None" : referenceNumber) + "\n" +
                        "Previous Status: " + paymentStatus + "\n\n" +
                        "The eligible payment record was deleted."
                );

                JOptionPane.showMessageDialog(
                        this,
                        "Payment record deleted successfully."
                );
            } else {
                showError(
                        "The payment was not deleted. It may already be marked Paid " +
                        "or may have been changed by another process."
                );
            }
        } catch (Exception ex) {
            showError(
                    "Could not delete the payment record.\n\n" +
                    "The payment may be linked to other records, or a database " +
                    "constraint may prevent deletion.\n\n" +
                    "Preserve payment history and do not remove related records " +
                    "just to force deletion.\n\n" +
                    ex.getMessage()
            );
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
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Cashier Dashboard",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
