
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentDAO {
    private DbConnection db = new DbConnection();

    public boolean apply(int studentId, int schoolYearId, int strandId,
                         int gradeLevel, String enrolleeType) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                return false;
            }

            String sql = "INSERT INTO enrollments "
                    + "(student_id, school_year_id, strand_id, year_level, "
                    + "enrollee_type, enrollment_status) "
                    + "VALUES (?, ?, ?, ?, ?, 'Pending')";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, studentId);
            ps.setInt(2, schoolYearId);
            ps.setInt(3, strandId);
            ps.setInt(4, gradeLevel);
            ps.setString(5, enrolleeType);

            int rowInserted = ps.executeUpdate();

            return rowInserted > 0;
        } catch (Exception e) {
            System.out.println("Enrollment error: " + e.getMessage());
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

    public String getMyEnrollments(int studentId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringBuilder result = new StringBuilder();

        try {
            conn = db.getConnection();

            if (conn == null) {
                return "Error: Could not connect to the database.";
            }

            String sql = "SELECT e.enrollment_id, sy.year_label, s.strand_code, "
                    + "e.year_level, e.enrollee_type, e.enrollment_status "
                    + "FROM enrollments e "
                    + "JOIN school_years sy ON e.school_year_id = sy.school_year_id "
                    + "JOIN strands s ON e.strand_id = s.strand_id "
                    + "WHERE e.student_id = ?";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, studentId);

            rs = ps.executeQuery();

            while (rs.next()) {
                result.append("Application ID: ")
                        .append(rs.getInt("enrollment_id"))
                        .append("\nSchool Year: ")
                        .append(rs.getString("year_label"))
                        .append("\nStrand: ")
                        .append(rs.getString("strand_code"))
                        .append("\nGrade: ")
                        .append(rs.getInt("year_level"))
                        .append("\nType: ")
                        .append(rs.getString("enrollee_type"))
                        .append("\nStatus: ")
                        .append(rs.getString("enrollment_status"))
                        .append("\n-------------------------\n");
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

        return result.length() == 0
                ? "No enrollment applications found."
                : result.toString();
    }

    public String getAllEnrollments() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringBuilder result = new StringBuilder();

        try {
            conn = db.getConnection();

            if (conn == null) {
                return "Error: Could not connect to the database.";
            }

            String sql = "SELECT e.enrollment_id, st.first_name, st.last_name, "
                    + "s.strand_code, e.year_level, e.enrollee_type, "
                    + "e.enrollment_status FROM enrollments e "
                    + "JOIN students st ON e.student_id = st.student_id "
                    + "JOIN strands s ON e.strand_id = s.strand_id "
                    + "ORDER BY e.enrollment_id DESC";

            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                result.append("Application ID: ")
                        .append(rs.getInt("enrollment_id"))
                        .append("\nStudent: ")
                        .append(rs.getString("first_name")).append(" ")
                        .append(rs.getString("last_name"))
                        .append("\nStrand: ")
                        .append(rs.getString("strand_code"))
                        .append("\nGrade: ")
                        .append(rs.getInt("year_level"))
                        .append("\nType: ")
                        .append(rs.getString("enrollee_type"))
                        .append("\nStatus: ")
                        .append(rs.getString("enrollment_status"))
                        .append("\n-------------------------\n");
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

        return result.length() == 0
                ? "No applications found."
                : result.toString();
    }

    public boolean updateStatus(int enrollmentId, String status) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                return false;
            }

            String sql = "UPDATE enrollments SET enrollment_status = ? "
                    + "WHERE enrollment_id = ?";

            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setInt(2, enrollmentId);

            int rowUpdated = ps.executeUpdate();

            return rowUpdated > 0;
        } catch (Exception e) {
            System.out.println("Status update error: " + e.getMessage());
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

    public static class SectionOption {
        public int sectionId;
        public String sectionName;
        public int capacity;
        public int currentCount;

        @Override
        public String toString() {
            return sectionName + " ("
                    + currentCount + "/" + capacity + " slots)";
        }
    }

    public List<SectionOption> getAvailableSections(int yearLevel, int strandId,
            int schoolYearId, Integer currentSectionId) throws SQLException {

        List<SectionOption> sections = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            String sql =
                    "SELECT sec.section_id, sec.section_name, "
                            + "sec.slot_limit, "
                            + "(SELECT COUNT(*) FROM enrollments e "
                            + "WHERE e.section_id = sec.section_id "
                            + "AND e.enrollment_status <> 'Rejected') "
                            + "AS current_count "
                            + "FROM sections sec "
                            + "WHERE sec.grade_level = ? "
                            + "AND sec.strand_id = ? "
                            + "AND sec.school_year_id = ? "
                            + "AND sec.is_active = TRUE "
                            + "ORDER BY sec.section_name";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, yearLevel);
            ps.setInt(2, strandId);
            ps.setInt(3, schoolYearId);

            rs = ps.executeQuery();

            while (rs.next()) {
                int capacity = rs.getInt("slot_limit");
                int count = rs.getInt("current_count");

                if (currentSectionId != null
                        && currentSectionId
                        == rs.getInt("section_id")) {
                    count--;
                }

                if (count < capacity) {
                    SectionOption section = new SectionOption();
                    section.sectionId = rs.getInt("section_id");
                    section.sectionName =
                            rs.getString("section_name");
                    section.capacity = capacity;
                    section.currentCount = Math.max(count, 0);
                    sections.add(section);
                }
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

        return sections;
    }

    public void assignSection(int enrollmentId, int sectionId, int yearLevel,
            int strandId, int schoolYearId) throws SQLException {

        String sectionSql =
                "SELECT grade_level, strand_id, school_year_id, "
                        + "slot_limit, is_active "
                        + "FROM sections WHERE section_id = ?";

        String countSql =
                "SELECT COUNT(*) FROM enrollments "
                        + "WHERE section_id = ? "
                        + "AND enrollment_status <> 'Rejected' "
                        + "AND enrollment_id <> ?";

        String updateSql =
                "UPDATE enrollments SET section_id = ? "
                        + "WHERE enrollment_id = ? "
                        + "AND year_level IN (11, 12) "
                        + "AND strand_id = ? "
                        + "AND school_year_id = ?";

        try (Connection conn = db.getConnection()) {
            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            conn.setAutoCommit(false);

            try {
                int grade;
                int sectionStrandId;
                int sectionSchoolYearId;
                int capacity;
                boolean active;

                try (PreparedStatement ps =
                             conn.prepareStatement(sectionSql)) {
                    ps.setInt(1, sectionId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Section was not found.");
                        }

                        grade = rs.getInt("grade_level");
                        sectionStrandId = rs.getInt("strand_id");
                        sectionSchoolYearId = rs.getInt("school_year_id");
                        capacity = rs.getInt("slot_limit");
                        active = rs.getBoolean("is_active");
                    }
                }

                if (!active
                        || grade != yearLevel
                        || sectionStrandId != strandId
                        || sectionSchoolYearId != schoolYearId) {
                    throw new SQLException(
                            "Section does not match the student's "
                                    + "grade, strand, or school year.");
                }

                int count;

                try (PreparedStatement ps =
                             conn.prepareStatement(countSql)) {
                    ps.setInt(1, sectionId);
                    ps.setInt(2, enrollmentId);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        count = rs.getInt(1);
                    }
                }

                if (count >= capacity) {
                    throw new SQLException(
                            "This section has reached its slot limit.");
                }

                try (PreparedStatement ps =
                             conn.prepareStatement(updateSql)) {
                    ps.setInt(1, sectionId);
                    ps.setInt(2, enrollmentId);
                    ps.setInt(3, strandId);
                    ps.setInt(4, schoolYearId);

                    if (ps.executeUpdate() == 0) {
                        throw new SQLException(
                                "Section assignment failed.");
                    }
                }

                conn.commit();

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
