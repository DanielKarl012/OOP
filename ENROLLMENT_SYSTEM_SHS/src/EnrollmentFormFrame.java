
import javax.swing.*;
import java.awt.*;

public class EnrollmentFormFrame extends JFrame {
    private StudentDAO studentDAO = new StudentDAO();
    private EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    public EnrollmentFormFrame(User user) {
        setTitle("Enrollment Form");
        setSize(550, 450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JTextArea form = new JTextArea();
        form.setEditable(false);
        form.setFont(new Font("Monospaced", Font.PLAIN, 14));

        int studentId = studentDAO.getStudentId(user.getUserId());

        StringBuilder text = new StringBuilder();
        text.append("SENIOR HIGH SCHOOL ENROLLMENT FORM\n");
        text.append("====================================\n");
        text.append("Student: ")
                .append(studentDAO.getStudentName(user.getUserId()))
                .append("\n\n");

        if (studentId != -1) {
            text.append(enrollmentDAO.getMyEnrollments(studentId));
        } else {
            text.append("Student profile not found.");
        }

        form.setText(text.toString());

        JButton printButton = new JButton("Print Enrollment Form");
        printButton.addActionListener(e -> {
            try {
                form.print();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Printing error: " + ex.getMessage());
            }
        });

        add(new JScrollPane(form), BorderLayout.CENTER);
        add(printButton, BorderLayout.SOUTH);
    }
}
