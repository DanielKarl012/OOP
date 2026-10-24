
import java.sql.*;

public class StudentDAO {
    private DbConnection db = new DbConnection();

    public boolean addStudent(int userId, String lrn, String firstName,
                              String middleName, String lastName,
                              String previousSchool) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                return false;
            }

            String sql = "INSERT INTO students "
                    + "(user_id, lrn, first_name, middle_name, last_name, previous_school) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, lrn);
            ps.setString(3, firstName);
            ps.setString(4, middleName);
            ps.setString(5, lastName);
            ps.setString(6, previousSchool);

            int rowInserted = ps.executeUpdate();

            return rowInserted > 0;
        } catch (Exception e) {
            System.out.println("Student error: " + e.getMessage());
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

    public int getStudentId(int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                return -1;
            }

            String sql = "SELECT student_id FROM students WHERE user_id = ?";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);

            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("student_id");
            }
        } catch (Exception e) {
            System.out.println("Student lookup error: " + e.getMessage());
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

        return -1;
    }

    public String getStudentName(int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = db.getConnection();

            if (conn == null) {
                return "Student";
            }

            String sql = "SELECT first_name, last_name FROM students WHERE user_id = ?";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);

            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("first_name") + " "
                        + rs.getString("last_name");
            }
        } catch (Exception e) {
            System.out.println("Student lookup error: " + e.getMessage());
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

        return "Student";
    }

    public int findOrCreateStudent(
        Connection conn,
        int userId,
        String lrn,
        String firstName,
        String middleName,
        String lastName,
        String previousSchool
    ) throws SQLException {

        String findSql =
            "SELECT student_id, lrn FROM students WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(findSql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int studentId = rs.getInt("student_id");
                    String existingLrn = rs.getString("lrn");

                    if (!lrn.equals(existingLrn)) {
                        try (PreparedStatement check =
                                 conn.prepareStatement(
                                     "SELECT student_id FROM students "
                                     + "WHERE lrn = ?")) {
                            check.setString(1, lrn);

                            try (ResultSet checkRs = check.executeQuery()) {
                                if (checkRs.next()) {
                                    throw new SQLException(
                                        "This LRN is already registered."
                                    );
                                }
                            }
                        }
                    }

                    String updateSql =
                        "UPDATE students SET lrn = ?, first_name = ?, "
                        + "middle_name = ?, last_name = ?, "
                        + "previous_school = ? WHERE student_id = ?";

                    try (PreparedStatement update =
                             conn.prepareStatement(updateSql)) {
                        update.setString(1, lrn);
                        update.setString(2, firstName);
                        update.setString(
                            3,
                            middleName.isEmpty() ? null : middleName
                        );
                        update.setString(4, lastName);
                        update.setString(5, previousSchool);
                        update.setInt(6, studentId);
                        update.executeUpdate();
                    }

                    return studentId;
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT student_id FROM students WHERE lrn = ?")) {
            ps.setString(1, lrn);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new SQLException(
                        "This LRN is already registered."
                    );
                }
            }
        }

        String insertSql =
            "INSERT INTO students "
            + "(user_id, lrn, first_name, middle_name, last_name, "
            + "previous_school) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(
                insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, lrn);
            ps.setString(3, firstName);
            ps.setString(
                4,
                middleName.isEmpty() ? null : middleName
            );
            ps.setString(5, lastName);
            ps.setString(6, previousSchool);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        throw new SQLException("Could not save student information.");
    }
}
