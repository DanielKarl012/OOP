
import java.sql.*;

public class ReportDAO {
    private DbConnection db = new DbConnection();

    public String getEnrollmentReport() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringBuilder result = new StringBuilder();

        try {
            conn = db.getConnection();

            if (conn == null) {
                return "Report error: Could not connect to the database.";
            }

            String sql = "SELECT s.strand_code, e.year_level, "
                    + "e.enrollment_status, COUNT(*) AS total "
                    + "FROM enrollments e "
                    + "JOIN strands s ON e.strand_id = s.strand_id "
                    + "GROUP BY s.strand_code, e.year_level, e.enrollment_status "
                    + "ORDER BY s.strand_code, e.year_level";

            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                result.append("Strand: ").append(rs.getString("strand_code"))
                        .append(" | Grade: ").append(rs.getInt("year_level"))
                        .append(" | Status: ")
                        .append(rs.getString("enrollment_status"))
                        .append(" | Total: ").append(rs.getInt("total"))
                        .append("\n");
            }
        } catch (Exception e) {
            return "Report error: " + e.getMessage();
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
                ? "No enrollment records found."
                : result.toString();
    }

    public int getTotalEnrollments() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                return 0;
            }

            String sql = "SELECT COUNT(*) AS total FROM enrollments";

            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (Exception e) {
            System.out.println("Report error: " + e.getMessage());
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

        return 0;
    }

    public String getEnrollmentCountReport(String type) throws SQLException {
        String sql;

        if ("strand".equals(type)) {
            sql = "SELECT str.strand_code AS report_name, COUNT(*) AS total "
                    + "FROM enrollments e "
                    + "JOIN strands str ON e.strand_id = str.strand_id "
                    + "WHERE e.year_level IN (11, 12) "
                    + "GROUP BY str.strand_code "
                    + "ORDER BY str.strand_code";
        } else if ("grade".equals(type)) {
            sql = "SELECT e.year_level AS report_name, COUNT(*) AS total "
                    + "FROM enrollments e "
                    + "WHERE e.year_level IN (11, 12) "
                    + "GROUP BY e.year_level ORDER BY e.year_level";
        } else {
            sql = "SELECT COALESCE(sec.section_name, "
                    + "'Not Assigned') AS report_name, COUNT(*) AS total "
                    + "FROM enrollments e "
                    + "LEFT JOIN sections sec "
                    + "ON e.section_id = sec.section_id "
                    + "WHERE e.year_level IN (11, 12) "
                    + "GROUP BY sec.section_id, sec.section_name "
                    + "ORDER BY report_name";
        }

        StringBuilder result = new StringBuilder(
                "ENROLLEES BY " + type.toUpperCase() + "\n\n");
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
                result.append(rs.getString("report_name"))
                        .append(": ")
                        .append(rs.getInt("total"))
                        .append(" student(s)\n");
            }

            if (!found) {
                result.append("No records found.");
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

    public String getIncompleteRequirementsReport() throws SQLException {
        String sql =
                "SELECT sr.requirement_id, st.student_id, st.lrn, st.first_name, "
                        + "st.last_name, e.year_level, "
                        + "str.strand_code, sr.requirement_name, "
                        + "sr.requirement_status "
                        + "FROM student_requirements sr "
                        + "JOIN enrollments e "
                        + "ON sr.enrollment_id = e.enrollment_id "
                        + "JOIN students st ON e.student_id = st.student_id "
                        + "JOIN strands str ON e.strand_id = str.strand_id "
                        + "WHERE e.year_level IN (11, 12) "
                        + "AND (sr.requirement_status IS NULL "
                        + "OR sr.requirement_status <> 'Complete') "
                        + "ORDER BY st.last_name, st.first_name";

        StringBuilder result = new StringBuilder(
                "INCOMPLETE REQUIREMENTS\n\n");
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
                result.append("Requirement ID: ")
                        .append(rs.getInt("requirement_id"))
                        .append("\nStudent ID: ")
                        .append(rs.getInt("student_id"))
                        .append("\nLRN: ")
                        .append(rs.getString("lrn"))
                        .append("\nName: ")
                        .append(rs.getString("first_name"))
                        .append(" ")
                        .append(rs.getString("last_name"))
                        .append("\nGrade: ")
                        .append(rs.getInt("year_level"))
                        .append("\nStrand: ")
                        .append(rs.getString("strand_code"))
                        .append("\nRequirement: ")
                        .append(rs.getString("requirement_name"))
                        .append("\nStatus: ")
                        .append(rs.getString("requirement_status"))
                        .append("\n----------------------------------------\n");
            }

            if (!found) {
                result.append("No incomplete requirements found.");
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

    public String getSectionsReport() throws SQLException {
        String sql =
                "SELECT sec.section_id, sec.section_name, "
                        + "sec.grade_level, st.strand_code, "
                        + "sy.year_label, sec.slot_limit, sec.is_active, "
                        + "(SELECT COUNT(*) FROM enrollments e "
                        + "WHERE e.section_id = sec.section_id "
                        + "AND e.school_year_id = sec.school_year_id) "
                        + "AS enrolled_count "
                        + "FROM sections sec "
                        + "JOIN strands st ON sec.strand_id = st.strand_id "
                        + "JOIN school_years sy "
                        + "ON sec.school_year_id = sy.school_year_id "
                        + "WHERE sec.grade_level IN (11, 12) "
                        + "ORDER BY sec.grade_level, sec.section_name";

        StringBuilder result = new StringBuilder();
        result.append("SENIOR HIGH SCHOOL SECTIONS\n\n");
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
                result.append("Section ID: ")
                        .append(rs.getInt("section_id"))
                        .append("\nSection: ")
                        .append(rs.getString("section_name"))
                        .append("\nGrade Level: ")
                        .append(rs.getInt("grade_level"))
                        .append("\nStrand: ")
                        .append(rs.getString("strand_code"))
                        .append("\nSchool Year: ")
                        .append(rs.getString("year_label"))
                        .append("\nEnrolled: ")
                        .append(rs.getInt("enrolled_count"))
                        .append("/")
                        .append(rs.getInt("slot_limit"))
                        .append("\nActive: ")
                        .append(rs.getBoolean("is_active"))
                        .append("\n----------------------------------------\n");
            }

            if (!found) {
                result.append("No Grade 11 or Grade 12 sections found.");
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
}
