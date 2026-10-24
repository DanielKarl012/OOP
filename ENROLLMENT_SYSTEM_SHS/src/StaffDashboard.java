
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StaffDashboard extends JFrame {
    private User user;
    private DbConnection db = new DbConnection();
    private PaymentDAO paymentDAO = new PaymentDAO();
    private ReportDAO reportDAO = new ReportDAO();
    private StaffEnrollmentActions enrollmentActions =
            new StaffEnrollmentActions(this);
    private JTextArea outputArea = new JTextArea();
    private JLabel helpLabel =
            new JLabel("Choose a feature from the menu.");

    public StaffDashboard(User user) {
        this.user = user;

        setTitle("Student Enrollment System - Staff Dashboard");
        setSize(1050, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel(
                "STAFF DASHBOARD", JLabel.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 22));
        add(heading, BorderLayout.NORTH);

        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        JPanel menuPanel = new JPanel(
                new GridLayout(0, 1, 5, 5));
        menuPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));

        addButton(menuPanel, "View Enrollment Records",
                this::viewEnrollmentRecords);
        addButton(menuPanel, "Update Enrollment Status",
                enrollmentActions::updateEnrollmentStatus);
        addButton(menuPanel, "Assign Section",
                enrollmentActions::assignSection);
        addButton(menuPanel, "Delete Enrollment Record",
                enrollmentActions::deleteEnrollmentRecord);
        addButton(menuPanel, "Delete Student Requirement",
                this::deleteStudentRequirement);
        addButton(menuPanel, "Review Requirement",
                this::reviewRequirement);
        addButton(menuPanel, "View Discounts",
                this::viewDiscounts);
        addButton(menuPanel, "Create Payment Request",
                enrollmentActions::createPaymentRequest);
        addButton(menuPanel, "View Payments",
                this::viewPayments);
        addButton(menuPanel, "Report: Enrollees by Strand",
                () -> showEnrollmentReport("strand"));
        addButton(menuPanel, "Report: Enrollees by Grade",
                () -> showEnrollmentReport("grade"));
        addButton(menuPanel, "Report: Enrollees by Section",
                () -> showEnrollmentReport("section"));
        addButton(menuPanel, "Incomplete Requirements",
                this::showIncompleteRequirements);
        addButton(menuPanel, "Dashboard Guide",
                this::showGuide);
        addButton(menuPanel, "Logout", this::logout);

        add(new JScrollPane(menuPanel), BorderLayout.WEST);
        add(helpLabel, BorderLayout.SOUTH);

        viewEnrollmentRecords();
    }

    private void addButton(
            JPanel panel, String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        panel.add(button);
    }

    public void viewEnrollmentRecords() {
        String sql =
                "SELECT e.enrollment_id, st.student_id, st.lrn, "
                        + "st.first_name, st.last_name, "
                        + "e.year_level, str.strand_code, "
                        + "sy.year_label, sec.section_name, "
                        + "e.enrollment_status "
                        + "FROM enrollments e "
                        + "JOIN students st ON e.student_id = st.student_id "
                        + "JOIN strands str ON e.strand_id = str.strand_id "
                        + "JOIN school_years sy "
                        + "ON e.school_year_id = sy.school_year_id "
                        + "LEFT JOIN sections sec "
                        + "ON e.section_id = sec.section_id "
                        + "WHERE e.year_level IN (11, 12) "
                        + "ORDER BY e.enrollment_id DESC";

        StringBuilder result =
                new StringBuilder("SHS ENROLLMENT RECORDS\n\n");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = db.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            boolean found = false;

            while (rs.next()) {
                found = true;

                String section = rs.getString("section_name");
                if (section == null) {
                    section = "Not Assigned";
                }

                result.append("Enrollment ID: ")
                        .append(rs.getInt("enrollment_id"))
                        .append("\nStudent ID: ")
                        .append(rs.getInt("student_id"))
                        .append("\nLRN: ")
                        .append(rs.getString("lrn"))
                        .append("\nStudent: ")
                        .append(rs.getString("first_name"))
                        .append(" ")
                        .append(rs.getString("last_name"))
                        .append("\nGrade: ")
                        .append(rs.getInt("year_level"))
                        .append("\nStrand: ")
                        .append(rs.getString("strand_code"))
                        .append("\nSchool Year: ")
                        .append(rs.getString("year_label"))
                        .append("\nSection: ")
                        .append(section)
                        .append("\nStatus: ")
                        .append(rs.getString("enrollment_status"))
                        .append("\n----------------------------------------\n");
            }

            if (!found) {
                result.append("No SHS enrollment records found.");
            }

            outputArea.setText(result.toString());
            helpLabel.setText("Enrollment records loaded.");
        } catch (Exception ex) {
            showError("Could not load enrollment records: "
                    + ex.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception closeEx) {
                System.out.println(closeEx);
            }

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

    private void deleteStudentRequirement() {
        String input = JOptionPane.showInputDialog(
                this, "Enter the Requirement ID to delete:");

        if (input == null) {
            return;
        }

        try {
            int id = Integer.parseInt(input.trim());

            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Delete this requirement record and its file?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION);

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }

            String selectSql =
                    "SELECT sr.file_path "
                            + "FROM student_requirements sr "
                            + "JOIN enrollments e "
                            + "ON sr.enrollment_id = e.enrollment_id "
                            + "WHERE sr.requirement_id = ? "
                            + "AND e.year_level IN (11, 12)";

            String path;

            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = db.getConnection();

                if (conn == null) {
                    throw new SQLException("Could not connect to the database.");
                }

                ps = conn.prepareStatement(selectSql);
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        showError("Requirement not found.");
                        return;
                    }

                    path = rs.getString("file_path");
                }
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

            String deleteSql =
                    "DELETE sr FROM student_requirements sr "
                            + "JOIN enrollments e "
                            + "ON sr.enrollment_id = e.enrollment_id "
                            + "WHERE sr.requirement_id = ? "
                            + "AND e.year_level IN (11, 12)";

            conn = null;
            ps = null;
            try {
                conn = db.getConnection();

                if (conn == null) {
                    throw new SQLException("Could not connect to the database.");
                }

                ps = conn.prepareStatement(deleteSql);
                ps.setInt(1, id);

                if (ps.executeUpdate() == 0) {
                    showError("Requirement was not deleted.");
                    return;
                }
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

            if (path != null && !path.trim().isEmpty()) {
                File file = new File(path);
                if (file.isFile()) {
                    file.delete();
                }
            }

            JOptionPane.showMessageDialog(
                    this, "Requirement record deleted.");
            viewEnrollmentRecords();

        } catch (NumberFormatException ex) {
            showError("Requirement ID must be numeric.");
        } catch (SQLException ex) {
            showError("Could not delete requirement: "
                    + ex.getMessage());
        }
    }

    private void reviewRequirement() {
        String input = JOptionPane.showInputDialog(
                this, "Enter the Requirement ID to review:\n"
                        + "(Use Incomplete Requirements to see the IDs.)");

        if (input == null || input.trim().isEmpty()) {
            return;
        }

        int id;

        try {
            id = Integer.parseInt(input.trim());
        } catch (NumberFormatException ex) {
            showError("Requirement ID must be numeric.");
            return;
        }

        String[] statuses = {"Complete", "Incomplete"};

        String status = (String) JOptionPane.showInputDialog(
                this,
                "Mark this requirement as:",
                "Review Requirement",
                JOptionPane.QUESTION_MESSAGE,
                null,
                statuses,
                statuses[0]);

        if (status == null) {
            return;
        }

        String sql =
                "UPDATE student_requirements sr "
                        + "JOIN enrollments e "
                        + "ON sr.enrollment_id = e.enrollment_id "
                        + "SET sr.requirement_status = ? "
                        + "WHERE sr.requirement_id = ? "
                        + "AND e.year_level IN (11, 12)";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = db.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setInt(2, id);

            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(
                        this, "Requirement marked as " + status + ".");
                showIncompleteRequirements();
            } else {
                showError("Requirement not found.");
            }
        } catch (Exception ex) {
            showError("Could not update requirement: "
                    + ex.getMessage());
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

    private void viewDiscounts() {
        try {
            outputArea.setText(
                    "DISCOUNTS\n\n" + paymentDAO.getDiscounts());
        } catch (Exception ex) {
            showError("Could not load discounts: " + ex.getMessage());
        }
    }

    private void viewPayments() {
        try {
            outputArea.setText(
                    "PAYMENT RECORDS\n\n" + paymentDAO.getPayments());
        } catch (Exception ex) {
            showError("Could not load payments: " + ex.getMessage());
        }
    }

    private void showEnrollmentReport(String type) {
        try {
            outputArea.setText(reportDAO.getEnrollmentCountReport(type));
        } catch (SQLException ex) {
            showError("Could not generate report: " + ex.getMessage());
        }
    }

    private void showIncompleteRequirements() {
        try {
            outputArea.setText(reportDAO.getIncompleteRequirementsReport());
        } catch (SQLException ex) {
            showError("Could not load incomplete requirements: "
                    + ex.getMessage());
        }
    }

    private void showGuide() {
        outputArea.setText(
                "STAFF DASHBOARD GUIDE\n\n"
                        + "View enrollment records for Grade 11 and 12.\n"
                        + "Update status using the allowed options.\n"
                        + "Review submitted requirements (Complete or Incomplete).\n"
                        + "Assign students to matching active sections.\n"
                        + "Review payments and discounts.\n"
                        + "Generate enrollment reports by strand, grade, "
                        + "or section.\n"
                        + "Review students with incomplete requirements.");
    }

    private void logout() {
        int choice = JOptionPane.showConfirmDialog(
                this, "Are you sure you want to log out?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    public static void showError(java.awt.Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent, message, "Staff Dashboard",
                JOptionPane.ERROR_MESSAGE);
    }

    private void showError(String message) {
        showError(this, message);
    }
}
