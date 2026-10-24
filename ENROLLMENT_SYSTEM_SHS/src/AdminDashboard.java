
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AdminDashboard extends JFrame {
    private User user;
    private DbConnection db = new DbConnection();
    private ReportDAO reportDAO = new ReportDAO();
    private FeeDAO feeDAO = new FeeDAO();
    private UserDAO userDAO = new UserDAO();
    private AuthService authService = new AuthService();
    private AdminSectionActions sectionActions =
            new AdminSectionActions(this);
    private JTextArea outputArea = new JTextArea();

    public AdminDashboard(User user) {
        this.user = user;

        setTitle("Student Enrollment System - Admin Dashboard");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel(
                "ADMIN DASHBOARD", JLabel.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 22));
        add(heading, BorderLayout.NORTH);

        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        JPanel menuPanel = new JPanel(
                new GridLayout(0, 1, 6, 6));
        menuPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));

        addButton(menuPanel, "Create SHS Section", sectionActions::addSection);
        addButton(menuPanel, "View SHS Sections", this::viewSections);
        addButton(menuPanel, "Update Section", sectionActions::updateSection);
        addButton(menuPanel, "Delete Section", sectionActions::deleteSection);
        addButton(menuPanel, "Add School Year", this::addSchoolYear);
        addButton(menuPanel, "View School Years", this::viewSchoolYears);
        addButton(menuPanel, "Add Strand", this::addStrand);
        addButton(menuPanel, "View Strands", this::viewStrands);
        addButton(menuPanel, "Add Fee", this::addFee);
        addButton(menuPanel, "View Fees", this::viewFees);
        addButton(menuPanel, "Create Staff/Cashier Account",
                this::createStaffAccount);
        addButton(menuPanel, "Enrollment Report", this::viewEnrollmentReport);
        addButton(menuPanel, "Logout", this::logout);

        add(new JScrollPane(menuPanel), BorderLayout.WEST);

        viewSections();
    }

    private void addButton(
            JPanel panel, String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        panel.add(button);
    }

    public void viewSections() {
        try {
            outputArea.setText(reportDAO.getSectionsReport());
        } catch (SQLException ex) {
            showError("Could not load sections: " + ex.getMessage());
        }
    }

    private void addSchoolYear() {
        String year = JOptionPane.showInputDialog(
                this, "Enter school year (example: 2026-2027):");

        if (year == null || year.trim().isEmpty()) {
            return;
        }

        String sql =
                "INSERT INTO school_years (year_label, is_active) "
                        + "VALUES (?, FALSE)";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = db.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            ps = conn.prepareStatement(sql);
            ps.setString(1, year.trim());
            ps.executeUpdate();

            JOptionPane.showMessageDialog(
                    this, "School year added successfully.");
        } catch (Exception ex) {
            showError("Could not add school year: " + ex.getMessage());
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

    private void viewSchoolYears() {
        String sql =
                "SELECT school_year_id, year_label, is_active "
                        + "FROM school_years ORDER BY school_year_id DESC";

        StringBuilder result = new StringBuilder("SCHOOL YEARS\n\n");

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
                result.append("ID: ")
                        .append(rs.getInt("school_year_id"))
                        .append("\nSchool Year: ")
                        .append(rs.getString("year_label"))
                        .append("\nActive: ")
                        .append(rs.getBoolean("is_active"))
                        .append("\n------------------------------\n");
            }

            outputArea.setText(result.toString());
        } catch (Exception ex) {
            showError("Could not load school years: " + ex.getMessage());
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
    }

    private void addStrand() {
        JTextField codeField = new JTextField();
        JTextField nameField = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Strand Code:"));
        form.add(codeField);
        form.add(new JLabel("Strand Name:"));
        form.add(nameField);

        int choice = JOptionPane.showConfirmDialog(
                this, form, "Add Strand",
                JOptionPane.OK_CANCEL_OPTION);

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String code = codeField.getText().trim();
        String name = nameField.getText().trim();

        if (code.isEmpty() || name.isEmpty()) {
            showError("Enter both strand code and strand name.");
            return;
        }

        String sql =
                "INSERT INTO strands (strand_code, strand_name) "
                        + "VALUES (?, ?)";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = db.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            ps = conn.prepareStatement(sql);
            ps.setString(1, code);
            ps.setString(2, name);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(
                    this, "Strand added successfully.");
        } catch (Exception ex) {
            showError("Could not add strand: " + ex.getMessage());
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

    private void viewStrands() {
        String sql =
                "SELECT strand_id, strand_code, strand_name "
                        + "FROM strands ORDER BY strand_code";

        StringBuilder result = new StringBuilder("STRANDS\n\n");

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
                result.append("Strand ID: ")
                        .append(rs.getInt("strand_id"))
                        .append("\nCode: ")
                        .append(rs.getString("strand_code"))
                        .append("\nName: ")
                        .append(rs.getString("strand_name"))
                        .append("\n------------------------------\n");
            }

            outputArea.setText(result.toString());
        } catch (Exception ex) {
            showError("Could not load strands: " + ex.getMessage());
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
    }

    private void addFee() {
        JTextField nameField = new JTextField();
        JTextField amountField = new JTextField();
        JComboBox<AdminSectionActions.IdOption> strandBox = new JComboBox<>();
        JComboBox<AdminSectionActions.IdOption> yearBox = new JComboBox<>();

        if (!sectionActions.loadOptions(strandBox, yearBox)) {
            return;
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Fee Name:"));
        form.add(nameField);
        form.add(new JLabel("Amount (PHP):"));
        form.add(amountField);
        form.add(new JLabel("Strand:"));
        form.add(strandBox);
        form.add(new JLabel("School Year:"));
        form.add(yearBox);

        int choice = JOptionPane.showConfirmDialog(
                this, form, "Add Fee", JOptionPane.OK_CANCEL_OPTION);

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String name = nameField.getText().trim();
        AdminSectionActions.IdOption strand =
                (AdminSectionActions.IdOption) strandBox.getSelectedItem();
        AdminSectionActions.IdOption year =
                (AdminSectionActions.IdOption) yearBox.getSelectedItem();

        if (name.isEmpty() || strand == null || year == null) {
            showError("Enter the fee name and choose a strand and school year.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountField.getText().trim());

            if (amount <= 0) {
                showError("The amount must be greater than zero.");
                return;
            }

            if (feeDAO.addFee(name, amount, year.id, strand.id)) {
                JOptionPane.showMessageDialog(this, "Fee added successfully.");
                viewFees();
            } else {
                showError("The fee could not be saved. Check the console message.");
            }
        } catch (NumberFormatException ex) {
            showError("The amount must be a valid number.");
        }
    }

    private void viewFees() {
        outputArea.setText("FEES\n\n" + feeDAO.getFees());
    }

    private void createStaffAccount() {
        JTextField usernameField = new JTextField();
        JTextField emailField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JComboBox<String> roleBox =
                new JComboBox<>(new String[]{"staff", "cashier"});

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Username:"));
        form.add(usernameField);
        form.add(new JLabel("Email:"));
        form.add(emailField);
        form.add(new JLabel("Password:"));
        form.add(passwordField);
        form.add(new JLabel("Role:"));
        form.add(roleBox);

        int choice = JOptionPane.showConfirmDialog(
                this, form, "Create Staff/Cashier Account",
                JOptionPane.OK_CANCEL_OPTION);

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String role = (String) roleBox.getSelectedItem();

        if (username.isEmpty() || email.isEmpty() || password.length() < 6) {
            showError("Enter a username, an email, and a password "
                    + "of at least 6 characters.");
            return;
        }

        if (userDAO.getUserId(username) != -1) {
            showError("That username is already used.");
            return;
        }

        String hash = authService.hashPassword(password);

        if (userDAO.addUser(username, hash, email, role)) {
            JOptionPane.showMessageDialog(
                    this, "Account created for " + username + " (" + role + ").");
        } else {
            showError("The account could not be created. "
                    + "The email may already be used.");
        }
    }

    private void viewEnrollmentReport() {
        try {
            int total = reportDAO.getTotalEnrollments();
            String report = reportDAO.getEnrollmentReport();

            outputArea.setText(
                    "ENROLLMENT REPORT\n\n"
                            + "Total Enrollments: " + total + "\n\n"
                            + report);

        } catch (Exception ex) {
            showError("Could not load enrollment report: "
                    + ex.getMessage());
        }
    }

    private void logout() {
        int choice = JOptionPane.showConfirmDialog(
                this, "Are you sure you want to log out?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent, message, "Admin Dashboard",
                JOptionPane.ERROR_MESSAGE);
    }

    private void showError(String message) {
        showError(this, message);
    }
}
