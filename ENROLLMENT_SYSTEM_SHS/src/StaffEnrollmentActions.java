
import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StaffEnrollmentActions {

    private StaffDashboard dashboard;
    private DbConnection db = new DbConnection();
    private EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private PaymentDAO paymentDAO = new PaymentDAO();

    public StaffEnrollmentActions(StaffDashboard dashboard) {
        this.dashboard = dashboard;
    }

    public void updateEnrollmentStatus() {
        EnrollmentOption enrollment = selectEnrollment();

        if (enrollment == null) {
            return;
        }

        String[] statuses = {
                "Pending", "Enrolled", "Rejected"
        };

        String status = (String) JOptionPane.showInputDialog(
                dashboard,
                "Select the new enrollment status:",
                "Update Enrollment Status",
                JOptionPane.QUESTION_MESSAGE,
                null,
                statuses,
                statuses[0]);

        if (status == null) {
            return;
        }

        if (status.equals("Enrolled") && enrollment.sectionId == null) {
            StaffDashboard.showError(dashboard,
                    "Assign a section first, then mark the student as Enrolled.");
            return;
        }

        String sql =
                "UPDATE enrollments SET enrollment_status = ? "
                        + "WHERE enrollment_id = ? "
                        + "AND year_level IN (11, 12)";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = db.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setInt(2, enrollment.enrollmentId);

            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(
                        dashboard, "Enrollment status updated.");
                dashboard.viewEnrollmentRecords();
            } else {
                StaffDashboard.showError(dashboard, "Enrollment record was not updated.");
            }
        } catch (Exception ex) {
            StaffDashboard.showError(dashboard, "Could not update status: "
                    + ex.getMessage()
                    + "\nCheck the allowed values of "
                    + "enrollments_chk_2.");
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

    public void assignSection() {
        EnrollmentOption enrollment = selectEnrollment();

        if (enrollment == null) {
            return;
        }

        List<EnrollmentDAO.SectionOption> sections;

        try {
            sections = enrollmentDAO.getAvailableSections(
                    enrollment.yearLevel, enrollment.strandId,
                    enrollment.schoolYearId, enrollment.sectionId);
        } catch (SQLException ex) {
            StaffDashboard.showError(dashboard,
                    "Could not load sections: " + ex.getMessage());
            return;
        }

        if (sections.isEmpty()) {
            StaffDashboard.showError(dashboard,
                    "No active sections with available slots "
                    + "match this student's grade, strand, "
                    + "and school year.");
            return;
        }

        JComboBox<EnrollmentDAO.SectionOption> sectionBox = new JComboBox<>();

        for (EnrollmentDAO.SectionOption section : sections) {
            sectionBox.addItem(section);
        }

        int choice = JOptionPane.showConfirmDialog(
                dashboard, sectionBox,
                "Assign Section to " + enrollment.studentName,
                JOptionPane.OK_CANCEL_OPTION);

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        EnrollmentDAO.SectionOption selected =
                (EnrollmentDAO.SectionOption) sectionBox.getSelectedItem();

        if (selected == null) {
            return;
        }

        try {
            enrollmentDAO.assignSection(
                    enrollment.enrollmentId, selected.sectionId,
                    enrollment.yearLevel, enrollment.strandId,
                    enrollment.schoolYearId);

            JOptionPane.showMessageDialog(
                    dashboard, "Section assigned successfully.");
            dashboard.viewEnrollmentRecords();
        } catch (SQLException ex) {
            StaffDashboard.showError(dashboard,
                    "Could not assign section: " + ex.getMessage());
        }
    }

    public EnrollmentOption selectEnrollment() {
        List<EnrollmentOption> enrollments =
                loadEnrollmentOptions();

        if (enrollments.isEmpty()) {
            StaffDashboard.showError(dashboard, "No SHS enrollment records are available.");
            return null;
        }

        JComboBox<EnrollmentOption> enrollmentBox =
                new JComboBox<>();

        for (EnrollmentOption enrollment : enrollments) {
            enrollmentBox.addItem(enrollment);
        }

        int choice = JOptionPane.showConfirmDialog(
                dashboard, enrollmentBox, "Select Enrollment",
                JOptionPane.OK_CANCEL_OPTION);

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        return (EnrollmentOption) enrollmentBox.getSelectedItem();
    }

    private List<EnrollmentOption> loadEnrollmentOptions() {
        List<EnrollmentOption> enrollments = new ArrayList<>();

        String sql =
                "SELECT e.enrollment_id, st.student_id, st.lrn, "
                        + "st.first_name, st.last_name, "
                        + "e.year_level, e.strand_id, str.strand_code, "
                        + "e.school_year_id, e.section_id, "
                        + "sec.section_name "
                        + "FROM enrollments e "
                        + "JOIN students st ON e.student_id = st.student_id "
                        + "JOIN strands str ON e.strand_id = str.strand_id "
                        + "LEFT JOIN sections sec "
                        + "ON e.section_id = sec.section_id "
                        + "WHERE e.year_level IN (11, 12) "
                        + "AND e.enrollment_status <> 'Rejected' "
                        + "ORDER BY st.last_name, st.first_name";

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
            while (rs.next()) {
                EnrollmentOption item = new EnrollmentOption();

                item.enrollmentId = rs.getInt("enrollment_id");
                item.studentId = rs.getInt("student_id");
                item.strandId = rs.getInt("strand_id");
                item.schoolYearId = rs.getInt("school_year_id");
                item.yearLevel = rs.getInt("year_level");
                item.studentName = rs.getString("first_name")
                        + " " + rs.getString("last_name");
                item.lrn = rs.getString("lrn");
                item.strandName = rs.getString("strand_code");

                int sectionId = rs.getInt("section_id");
                if (!rs.wasNull()) {
                    item.sectionId = sectionId;
                }

                item.sectionName = rs.getString("section_name");
                if (item.sectionName == null) {
                    item.sectionName = "Not Assigned";
                }

                enrollments.add(item);
            }
        } catch (Exception ex) {
            StaffDashboard.showError(dashboard, "Could not load enrollment options: "
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

        return enrollments;
    }

    public void deleteEnrollmentRecord() {
        EnrollmentOption enrollment = selectEnrollment();

        if (enrollment == null) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                dashboard,
                "Delete enrollment for " + enrollment.studentName
                        + "?\nRelated records may prevent deletion.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        String sql =
                "DELETE FROM enrollments WHERE enrollment_id = ? "
                        + "AND year_level IN (11, 12)";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = db.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            ps = conn.prepareStatement(sql);
            ps.setInt(1, enrollment.enrollmentId);

            if (ps.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(
                        dashboard, "Enrollment record deleted.");
                dashboard.viewEnrollmentRecords();
            } else {
                StaffDashboard.showError(dashboard, "Enrollment record was not deleted.");
            }
        } catch (Exception ex) {
            StaffDashboard.showError(dashboard, "Could not delete enrollment: " + ex.getMessage());
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

    public void createPaymentRequest() {
        StaffEnrollmentActions.EnrollmentOption enrollment =
                selectEnrollment();

        if (enrollment == null) {
            return;
        }

        String input = JOptionPane.showInputDialog(
                dashboard,
                "Enter discount IDs separated by commas.\n"
                        + "Leave blank if no discount applies.");

        if (input == null) {
            return;
        }

        List<Integer> discountIds = new ArrayList<>();

        if (!input.trim().isEmpty()) {
            try {
                for (String value : input.split(",")) {
                    discountIds.add(Integer.parseInt(value.trim()));
                }
            } catch (NumberFormatException ex) {
                StaffDashboard.showError(dashboard, "Enter valid discount IDs.");
                return;
            }
        }

        try {
            paymentDAO.createPaymentRequest(
                    enrollment.enrollmentId, discountIds);

            JOptionPane.showMessageDialog(
                    dashboard, "Payment request created.");
        } catch (Exception ex) {
            StaffDashboard.showError(dashboard, "Could not create payment request: "
                    + ex.getMessage());
        }
    }

    public static class EnrollmentOption {
        public int enrollmentId;
        public int studentId;
        public int strandId;
        public int schoolYearId;
        public int yearLevel;
        public Integer sectionId;
        public String studentName;
        public String lrn;
        public String strandName;
        public String sectionName;

        @Override
        public String toString() {
            return "ID: " + enrollmentId
                    + " | " + studentName
                    + " | Grade " + yearLevel
                    + " | " + strandName
                    + " | Section: " + sectionName;
        }
    }
}
