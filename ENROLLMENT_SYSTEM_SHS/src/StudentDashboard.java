
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class StudentDashboard extends JFrame {

    public User user;
    public JTextArea outputArea = new JTextArea();
    public JLabel helpLabel = new JLabel(
        "Choose a feature below to manage your enrollment."
    );

    private Color backgroundColor = new Color(245, 247, 250);
    private Color primaryColor = new Color(24, 55, 90);

    private StudentEnrollmentActions enrollmentActions;
    private StudentRequirementActions requirementActions;
    private StudentPaymentActions paymentActions;

    public StudentDashboard(User user) {
        this.user = user;
        this.enrollmentActions = new StudentEnrollmentActions(this);
        this.requirementActions = new StudentRequirementActions(this);
        this.paymentActions = new StudentPaymentActions(this);

        setTitle("Student Enrollment System - Student Dashboard");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(backgroundColor);

        JLabel titleLabel = new JLabel(
            "STUDENT ENROLLMENT DASHBOARD",
            SwingConstants.CENTER
        );
        titleLabel.setFont(new Font("Arial", Font.BOLD, 23));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setOpaque(true);
        titleLabel.setBackground(primaryColor);
        titleLabel.setBorder(
            BorderFactory.createEmptyBorder(20, 10, 20, 10)
        );
        add(titleLabel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(backgroundColor);
        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 15, 15, 15)
        );

        JPanel buttonPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        buttonPanel.setBackground(backgroundColor);

        addButton(buttonPanel, "Submit Enrollment Application",
            enrollmentActions::submitEnrollmentApplication);
        addButton(buttonPanel, "View My Enrollments",
            this::viewMyEnrollments);
        addButton(buttonPanel, "Print Enrollment Form",
            enrollmentActions::printEnrollmentForm);
        addButton(buttonPanel, "Upload Enrollment Requirements",
            requirementActions::submitRequirement);
        addButton(buttonPanel, "View My Requirements",
            this::viewMyRequirements);
        addButton(buttonPanel, "Delete My Requirement",
            requirementActions::deleteMyRequirement);
        addButton(buttonPanel, "View Payment Requests",
            paymentActions::viewPaymentRequests);
        addButton(buttonPanel, "View Request Breakdown",
            paymentActions::viewRequestBreakdown);
        addButton(buttonPanel, "Submit Payment",
            paymentActions::submitPayment);
        addButton(buttonPanel, "View My Payments",
            paymentActions::viewMyPayments);
        addButton(buttonPanel, "How to Use Dashboard",
            this::showInstructions);
        addButton(buttonPanel, "Logout", this::logout);

        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setText(
            "Welcome to the Student Enrollment System.\n\n"
            + "Select a feature to get started."
        );

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setPreferredSize(new Dimension(500, 250));

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(backgroundColor);
        contentPanel.add(buttonPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        helpLabel.setBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        );

        mainPanel.add(helpLabel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    private void addButton(JPanel panel, String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBackground(Color.WHITE);
        button.setForeground(primaryColor);
        button.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(210, 218, 228)
                ),
                BorderFactory.createEmptyBorder(10, 8, 10, 8)
            )
        );
        button.addActionListener(e -> action.run());
        panel.add(button);
    }

    public Connection getConnection() throws SQLException {
        return new DbConnection().getConnection();
    }

    private void viewMyEnrollments() {
        String sql =
            "SELECT e.enrollment_id, sy.year_label, "
            + "st.strand_code, st.strand_name, e.year_level, "
            + "e.enrollee_type, e.enrollment_status, "
            + "sec.section_name AS assigned_section, "
            + "sec.grade_level AS section_grade_level "
            + "FROM enrollments e "
            + "JOIN students s ON e.student_id = s.student_id "
            + "JOIN school_years sy "
            + "ON e.school_year_id = sy.school_year_id "
            + "JOIN strands st ON e.strand_id = st.strand_id "
            + "LEFT JOIN sections sec ON e.section_id = sec.section_id "
            + "WHERE s.user_id = ? "
            + "ORDER BY e.enrollment_id DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            ps = conn.prepareStatement(sql);
            ps.setInt(1, user.getUserId());

            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder output =
                    new StringBuilder("MY ENROLLMENT RECORDS\n\n");
                boolean found = false;

                while (rs.next()) {
                    found = true;

                    output.append("Enrollment ID: ")
                        .append(rs.getInt("enrollment_id"))
                        .append("\nSchool Year: ")
                        .append(rs.getString("year_label"))
                        .append("\nStrand: ")
                        .append(rs.getString("strand_code"))
                        .append(" - ")
                        .append(rs.getString("strand_name"))
                        .append("\nGrade Level: ")
                        .append(rs.getInt("year_level"))
                        .append("\nEnrollee Type: ")
                        .append(rs.getString("enrollee_type"))
                        .append("\nStatus: ")
                        .append(rs.getString("enrollment_status"))
                        .append("\nAssigned Section: ");

                    String section = rs.getString("assigned_section");

                    if (section == null || section.trim().isEmpty()) {
                        output.append("Not yet assigned\n");
                    } else {
                        output.append(section).append("\n");
                        output.append("Section Grade Level: ")
                            .append(rs.getInt("section_grade_level"))
                            .append("\n");
                    }

                    output.append("--------------------------------\n");
                }

                if (!found) {
                    output.append("No enrollment records found.");
                }

                outputArea.setText(output.toString());
                helpLabel.setText(
                    "Your enrollment records and assigned sections."
                );
            }
        } catch (Exception ex) {
            showError(
                "Could not load enrollments: " + ex.getMessage()
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

    private void viewMyRequirements() {
        String sql =
            "SELECT sr.requirement_id, sr.enrollment_id, "
            + "sr.requirement_name, sr.requirement_status, "
            + "sr.original_file_name, sr.submitted_at "
            + "FROM student_requirements sr "
            + "JOIN enrollments e "
            + "ON sr.enrollment_id = e.enrollment_id "
            + "JOIN students s ON e.student_id = s.student_id "
            + "WHERE s.user_id = ? "
            + "ORDER BY sr.requirement_id DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            ps = conn.prepareStatement(sql);
            ps.setInt(1, user.getUserId());

            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder output =
                    new StringBuilder("MY SUBMITTED REQUIREMENTS\n\n");
                boolean found = false;

                while (rs.next()) {
                    found = true;
                    output.append("Requirement ID: ")
                        .append(rs.getInt("requirement_id"))
                        .append("\nEnrollment ID: ")
                        .append(rs.getInt("enrollment_id"))
                        .append("\nRequirement: ")
                        .append(rs.getString("requirement_name"))
                        .append("\nFile: ")
                        .append(rs.getString("original_file_name"))
                        .append("\nStatus: ")
                        .append(rs.getString("requirement_status"))
                        .append("\nSubmitted: ")
                        .append(rs.getString("submitted_at"))
                        .append("\n--------------------------------\n");
                }

                if (!found) {
                    output.append(
                        "You have not submitted any requirements."
                    );
                }

                outputArea.setText(output.toString());
                helpLabel.setText(
                    "Use the Requirement ID to delete an eligible submission."
                );
            }
        } catch (Exception ex) {
            showError(
                "Could not load requirements: " + ex.getMessage()
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

    private void showInstructions() {
        outputArea.setText(
            "HOW TO USE THE STUDENT DASHBOARD\n\n"
            + "1. Submit Enrollment Application.\n"
            + "2. View My Enrollments to check your enrollment status "
            + "and assigned section.\n"
            + "3. Print Enrollment Form to print your enrollment details.\n"
            + "4. Upload Enrollment Requirements.\n"
            + "5. View My Requirements to check your submissions.\n"
            + "6. Delete My Requirement to remove an eligible submission.\n"
            + "7. View Payment Requests.\n"
            + "8. View Request Breakdown.\n"
            + "9. Submit Payment.\n"
            + "10. View My Payments.\n\n"
            + "Contact enrollment staff if your information is incorrect."
        );
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to log out?",
            "Logout",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            "Student Dashboard",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private void showError(String message) {
        showError(this, message);
    }

    public static class ChoiceItem {
        public int id;
        public String label;

        public ChoiceItem(int id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
