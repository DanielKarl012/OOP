
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class AuthService {
    private UserDAO userDAO = new UserDAO();
    private StudentDAO studentDAO = new StudentDAO();

    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();

            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }

            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("Unable to hash password.", e);
        }
    }

    public User login(String username, String password) {
        return userDAO.login(username, hashPassword(password));
    }

    public boolean register(String username, String password, String email,
                            String lrn, String firstName, String middleName,
                            String lastName, String previousSchool) {
        if (username.isEmpty() || password.isEmpty() || email.isEmpty()
                || lrn.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            return false;
        }

        if (!userDAO.addUser(username, hashPassword(password), email, "student")) {
            return false;
        }

        int userId = userDAO.getUserId(username);

        if (userId == -1) {
            return false;
        }

        return studentDAO.addStudent(userId, lrn, firstName, middleName,
                lastName, previousSchool);
    }
}
