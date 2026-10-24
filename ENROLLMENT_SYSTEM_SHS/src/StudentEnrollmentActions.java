
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class StudentEnrollmentActions {

    private StudentDashboard dashboard;
    private User user;
    private JTextArea outputArea;
    private JLabel helpLabel;
    private StudentDAO studentDAO = new StudentDAO();

    public StudentEnrollmentActions(StudentDashboard dashboard) {
        this.dashboard = dashboard;
        this.user = dashboard.user;
        this.outputArea = dashboard.outputArea;
        this.helpLabel = dashboard.helpLabel;
    }

    public void submitEnrollmentApplication() {
        JTextField lrnField = new JTextField();
        JTextField firstNameField = new JTextField();
        JTextField middleNameField = new JTextField();
        JTextField lastNameField = new JTextField();
        JTextField previousSchoolField = new JTextField();

        JComboBox<StudentDashboard.ChoiceItem> schoolYearBox = new JComboBox<>();
        JComboBox<StudentDashboard.ChoiceItem> strandBox = new JComboBox<>();

        JComboBox<String> gradeLevelBox = new JComboBox<>(
            new String[]{"11", "12"}
        );

        JComboBox<String> enrolleeTypeBox = new JComboBox<>(
            new String[]{
                "New Student",
                "Transferee",
                "Returning Student"
            }
        );

        Connection conn = null;
        try {
            conn = dashboard.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            String yearSql =
                "SELECT school_year_id, year_label FROM school_years "
                + "ORDER BY school_year_id DESC";

            try (PreparedStatement ps = conn.prepareStatement(yearSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    schoolYearBox.addItem(new StudentDashboard.ChoiceItem(
                        rs.getInt("school_year_id"),
                        rs.getString("year_label")
                    ));
                }
            }

            String strandSql =
                "SELECT strand_id, strand_code, strand_name FROM strands "
                + "ORDER BY strand_code";

            try (PreparedStatement ps = conn.prepareStatement(strandSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    strandBox.addItem(new StudentDashboard.ChoiceItem(
                        rs.getInt("strand_id"),
                        rs.getString("strand_code") + " - "
                            + rs.getString("strand_name")
                    ));
                }
            }
        } catch (Exception ex) {
            StudentDashboard.showError(dashboard,
                "Could not load school years and strands: "
                + ex.getMessage()
            );
            return;
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception closeEx) {
                System.out.println(closeEx);
            }
        }

        if (schoolYearBox.getItemCount() == 0
                || strandBox.getItemCount() == 0) {
            StudentDashboard.showError(dashboard,
                "No school years or strands are available. "
                + "Ask the administrator to add them first."
            );
            return;
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("LRN (12 digits):"));
        form.add(lrnField);
        form.add(new JLabel("First Name:"));
        form.add(firstNameField);
        form.add(new JLabel("Middle Name (optional):"));
        form.add(middleNameField);
        form.add(new JLabel("Last Name:"));
        form.add(lastNameField);
        form.add(new JLabel("Previous School:"));
        form.add(previousSchoolField);
        form.add(new JLabel("School Year:"));
        form.add(schoolYearBox);
        form.add(new JLabel("Strand:"));
        form.add(strandBox);
        form.add(new JLabel("Grade Level:"));
        form.add(gradeLevelBox);
        form.add(new JLabel("Enrollee Type:"));
        form.add(enrolleeTypeBox);

        int result = JOptionPane.showConfirmDialog(
            dashboard,
            form,
            "Enrollment Application",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String lrn = lrnField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String middleName = middleNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String previousSchool = previousSchoolField.getText().trim();

        if (!lrn.matches("\\d{12}")) {
            StudentDashboard.showError(dashboard, "LRN must contain exactly 12 digits.");
            return;
        }

        if (firstName.isEmpty() || lastName.isEmpty()
                || previousSchool.isEmpty()) {
            StudentDashboard.showError(dashboard,
                "First name, last name, and previous school are required."
            );
            return;
        }

        StudentDashboard.ChoiceItem selectedYear =
            (StudentDashboard.ChoiceItem) schoolYearBox.getSelectedItem();
        StudentDashboard.ChoiceItem selectedStrand =
            (StudentDashboard.ChoiceItem) strandBox.getSelectedItem();

        if (selectedYear == null || selectedStrand == null) {
            StudentDashboard.showError(dashboard, "Please select a school year and strand.");
            return;
        }

        conn = null;
        try {
            conn = dashboard.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            conn.setAutoCommit(false);

            try {
                int studentId = studentDAO.findOrCreateStudent(
                    conn,
                    user.getUserId(),
                    lrn,
                    firstName,
                    middleName,
                    lastName,
                    previousSchool
                );

                String duplicateSql =
                    "SELECT enrollment_id FROM enrollments "
                    + "WHERE student_id = ? AND school_year_id = ?";

                try (PreparedStatement ps =
                         conn.prepareStatement(duplicateSql)) {
                    ps.setInt(1, studentId);
                    ps.setInt(2, selectedYear.id);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            conn.rollback();
                            StudentDashboard.showError(dashboard,
                                "You already have an enrollment "
                                + "application for this school year."
                            );
                            return;
                        }
                    }
                }

                String insertSql =
                    "INSERT INTO enrollments "
                    + "(student_id, school_year_id, strand_id, section_id, "
                    + "year_level, enrollee_type) "
                    + "VALUES (?, ?, ?, NULL, ?, ?)";

                int enrollmentId;

                try (PreparedStatement ps = conn.prepareStatement(
                        insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, studentId);
                    ps.setInt(2, selectedYear.id);
                    ps.setInt(3, selectedStrand.id);
                    ps.setInt(
                        4,
                        Integer.parseInt(
                            gradeLevelBox.getSelectedItem().toString()
                        )
                    );
                    ps.setString(
                        5,
                        enrolleeTypeBox.getSelectedItem().toString()
                    );
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                "Could not retrieve enrollment ID."
                            );
                        }
                        enrollmentId = keys.getInt(1);
                    }
                }

                conn.commit();

                outputArea.setText(
                    "ENROLLMENT APPLICATION SUBMITTED\n\n"
                    + "Enrollment ID: " + enrollmentId
                    + "\nStudent ID: " + studentId
                    + "\nSchool Year: " + selectedYear.label
                    + "\nStrand: " + selectedStrand.label
                    + "\nGrade Level: "
                    + gradeLevelBox.getSelectedItem()
                    + "\nStatus: Pending"
                    + "\nAssigned Section: Not yet assigned"
                    + "\n\nNext step: upload your requirements."
                );

                helpLabel.setText("Your application has been saved.");

                JOptionPane.showMessageDialog(
                    dashboard,
                    "Enrollment application submitted successfully!\n"
                    + "Enrollment ID: " + enrollmentId
                );

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception ex) {
            StudentDashboard.showError(dashboard,
                "Could not submit enrollment application: "
                + ex.getMessage()
            );
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception closeEx) {
                System.out.println(closeEx);
            }
        }
    }

    public void printEnrollmentForm() {
        String sql =
            "SELECT e.enrollment_id, e.year_level, e.enrollee_type, "
            + "e.enrollment_status, sy.year_label, "
            + "st.strand_code, st.strand_name, "
            + "sec.section_name AS assigned_section, "
            + "sec.grade_level AS section_grade_level, "
            + "s.lrn, s.first_name, s.middle_name, s.last_name, "
            + "s.previous_school "
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
            conn = dashboard.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            ps = conn.prepareStatement(sql);
            ps.setInt(1, user.getUserId());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    StudentDashboard.showError(dashboard,
                        "No enrollment record found. "
                        + "Submit an enrollment application first."
                    );
                    return;
                }

                StringBuilder form = new StringBuilder();

                form.append("================================================\n");
                form.append("          STUDENT ENROLLMENT FORM\n");
                form.append("================================================\n\n");
                form.append("ENROLLMENT INFORMATION\n");
                form.append("-----------------------------------------------\n");
                form.append("Enrollment ID: ")
                    .append(rs.getInt("enrollment_id")).append("\n");
                form.append("School Year: ")
                    .append(rs.getString("year_label")).append("\n");
                form.append("Enrollment Status: ")
                    .append(rs.getString("enrollment_status")).append("\n\n");

                form.append("STUDENT INFORMATION\n");
                form.append("-----------------------------------------------\n");
                form.append("LRN: ")
                    .append(rs.getString("lrn")).append("\n");
                form.append("First Name: ")
                    .append(rs.getString("first_name")).append("\n");
                form.append("Middle Name: ")
                    .append(valueOrBlank(rs.getString("middle_name")))
                    .append("\n");
                form.append("Last Name: ")
                    .append(rs.getString("last_name")).append("\n");
                form.append("Previous School: ")
                    .append(rs.getString("previous_school")).append("\n\n");

                form.append("ACADEMIC INFORMATION\n");
                form.append("-----------------------------------------------\n");
                form.append("Grade Level: ")
                    .append(rs.getInt("year_level")).append("\n");
                form.append("Strand: ")
                    .append(rs.getString("strand_code"))
                    .append(" - ")
                    .append(rs.getString("strand_name")).append("\n");
                form.append("Enrollee Type: ")
                    .append(rs.getString("enrollee_type")).append("\n");

                String section = rs.getString("assigned_section");

                form.append("Assigned Section: ");
                if (section == null || section.trim().isEmpty()) {
                    form.append("Not yet assigned\n");
                } else {
                    form.append(section).append("\n");
                    form.append("Section Grade Level: ")
                        .append(rs.getInt("section_grade_level"))
                        .append("\n");
                }

                form.append("\n");
                form.append("Student Signature: __________________________\n\n");
                form.append("Parent/Guardian Signature: __________________\n\n");
                form.append("Enrollment Staff: ____________________________\n\n");
                form.append("Date: ________________________________________\n\n");
                form.append("================================================\n");
                form.append("This form reflects the latest saved enrollment record.\n");

                String formText = form.toString();
                outputArea.setText(formText);
                helpLabel.setText(
                    "Enrollment form is ready to print."
                );

                JTextArea printArea = new JTextArea(formText);
                printArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                printArea.setEditable(false);
                printArea.setLineWrap(false);

                try {
                    boolean printed = printArea.print();

                    if (printed) {
                        JOptionPane.showMessageDialog(
                            dashboard,
                            "Enrollment form sent to the printer."
                        );
                    }
                } catch (java.awt.print.PrinterException ex) {
                    StudentDashboard.showError(dashboard,
                        "Could not print enrollment form: "
                        + ex.getMessage()
                    );
                }
            }
        } catch (Exception ex) {
            StudentDashboard.showError(dashboard,
                "Could not prepare enrollment form: " + ex.getMessage()
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

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }
}
