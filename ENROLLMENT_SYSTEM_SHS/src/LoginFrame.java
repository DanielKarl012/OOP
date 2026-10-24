
import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private AuthService authService = new AuthService();

    public LoginFrame() {
        setTitle("Student Enrollment System - Login");
        setSize(420, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(5, 1, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        panel.add(new JLabel("STUDENT ENROLLMENT SYSTEM", JLabel.CENTER));

        usernameField = new JTextField();
        passwordField = new JPasswordField();

        panel.add(createField("Username:", usernameField));
        panel.add(createField("Password:", passwordField));

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register Student Account");

        panel.add(loginButton);
        panel.add(registerButton);

        add(panel);

        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e ->
                new RegistrationFrame().setVisible(true));
    }

    private JPanel createField(String label, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.add(new JLabel(label), BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Enter your username and password.");
            return;
        }

        User user = authService.login(username, password);

        if (user == null) {
            JOptionPane.showMessageDialog(this,
                    "Invalid username or password.");
            return;
        }

        String role = user.getRole().toLowerCase();

        if (role.equals("admin")) {
            new AdminDashboard(user).setVisible(true);
        } else if (role.equals("staff")) {
            new StaffDashboard(user).setVisible(true);
        } else if (role.equals("cashier")) {
            new CashierDashboard(user).setVisible(true);
        } else if (role.equals("student")) {
            new StudentDashboard(user).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Unknown account role.");
            return;
        }

        dispose();
    }
}
