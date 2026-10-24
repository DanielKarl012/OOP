
public class EnrollmentService {
    private EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private StudentDAO studentDAO = new StudentDAO();

    public boolean applyForEnrollment(int userId, int schoolYearId,
                                      int strandId, int gradeLevel,
                                      String enrolleeType) {
        if (gradeLevel != 11 && gradeLevel != 12) {
            return false;
        }

        int studentId = studentDAO.getStudentId(userId);

        if (studentId == -1) {
            return false;
        }

        return enrollmentDAO.apply(studentId, schoolYearId, strandId,
                gradeLevel, enrolleeType);
    }

    public boolean updateEnrollmentStatus(int enrollmentId, String status) {
        return enrollmentDAO.updateStatus(enrollmentId, status);
    }
}
