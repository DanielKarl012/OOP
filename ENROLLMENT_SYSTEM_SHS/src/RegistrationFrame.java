
import javax.swing.*;
import java.awt.*;

public class RegistrationFrame extends JFrame {
    private JTextField usernameField = new JTextField();
    private JTextField emailField = new JTextField();
    private JPasswordField passwordField = new JPasswordField();
    private JTextField lrnField = new JTextField();
    private JTextField firstNameField = new JTextField();
    private JTextField middleNameField = new JTextField();
    private JTextField lastNameField = new JTextField();
    private JTextField previousSchoolField = new JTextField();

    private AuthService authService = new AuthService();

    public RegistrationFrame() {
        setTitle("Student Registration");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(9, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("LRN:"));
        panel.add(lrnField);
        panel.add(new JLabel("First Name:"));
        panel.add(firstNameField);
        panel.add(new JLabel("Middle Name:"));
        panel.add(middleNameField);
        panel.add(new JLabel("Last Name:"));
        panel.add(lastNameField);
        panel.add(new JLabel("Previous School:"));
        panel.add(previousSchoolField);

        JButton registerButton = new JButton("Register");
        JButton cancelButton = new JButton("Cancel");

        panel.add(registerButton);
        panel.add(cancelButton);

        add(panel);

        registerButton.addActionListener(e -> register());
        cancelButton.addActionListener(e -> dispose());
    }

    private void register() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String lrn = lrnField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String middleName = middleNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String previousSchool = previousSchoolField.getText().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()
                || lrn.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in all required fields.");
            return;
        }

        boolean success = authService.register(
                username, password, email, lrn, firstName,
                middleName, lastName, previousSchool);

        if (success) {
            JOptionPane.showMessageDialog(this,
                    "Registration successful. You can now log in.");
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Registration failed. Check for duplicate username, email, or LRN.");
        }
    }
}
