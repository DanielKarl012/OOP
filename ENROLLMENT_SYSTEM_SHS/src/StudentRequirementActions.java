
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StudentRequirementActions {

    private StudentDashboard dashboard;
    private User user;
    private JTextArea outputArea;
    private JLabel helpLabel;
    private DeleteDAO deleteDAO = new DeleteDAO();

    public StudentRequirementActions(StudentDashboard dashboard) {
        this.dashboard = dashboard;
        this.user = dashboard.user;
        this.outputArea = dashboard.outputArea;
        this.helpLabel = dashboard.helpLabel;
    }

    public void submitRequirement() {
        List<StudentDashboard.ChoiceItem> enrollments = new ArrayList<>();

        String enrollmentSql =
            "SELECT e.enrollment_id, sy.year_label FROM enrollments e "
            + "JOIN students s ON e.student_id = s.student_id "
            + "JOIN school_years sy "
            + "ON e.school_year_id = sy.school_year_id "
            + "WHERE s.user_id = ? ORDER BY e.enrollment_id DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = dashboard.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            ps = conn.prepareStatement(enrollmentSql);
            ps.setInt(1, user.getUserId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    enrollments.add(new StudentDashboard.ChoiceItem(
                        rs.getInt("enrollment_id"),
                        "Enrollment #" + rs.getInt("enrollment_id")
                            + " - " + rs.getString("year_label")
                    ));
                }
            }
        } catch (Exception ex) {
            StudentDashboard.showError(dashboard,
                "Could not load enrollments: " + ex.getMessage()
            );
            return;
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

        if (enrollments.isEmpty()) {
            StudentDashboard.showError(dashboard, "Submit an enrollment application first.");
            return;
        }

        StudentDashboard.ChoiceItem selectedEnrollment = (StudentDashboard.ChoiceItem)
            JOptionPane.showInputDialog(
                dashboard,
                "Select the enrollment for this document:",
                "Select Enrollment",
                JOptionPane.QUESTION_MESSAGE,
                null,
                enrollments.toArray(),
                enrollments.get(0)
            );

        if (selectedEnrollment == null) {
            return;
        }

        String[] requirementTypes = {
            "PSA Birth Certificate",
            "Form 138 / Report Card",
            "Good Moral Certificate",
            "Certificate of Transfer",
            "Other Requirement"
        };

        String requirementType = (String) JOptionPane.showInputDialog(
            dashboard,
            "Select the requirement type:",
            "Requirement Type",
            JOptionPane.QUESTION_MESSAGE,
            null,
            requirementTypes,
            requirementTypes[0]
        );

        if (requirementType == null) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose Your Requirement Document");

        if (chooser.showOpenDialog(dashboard) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        String lowerName = selectedFile.getName().toLowerCase();

        if (!(lowerName.endsWith(".pdf")
                || lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png"))) {
            StudentDashboard.showError(dashboard, "Only PDF, JPG, JPEG, and PNG files are allowed.");
            return;
        }

        if (!selectedFile.isFile() || selectedFile.length() == 0) {
            StudentDashboard.showError(dashboard, "Select a valid, non-empty file.");
            return;
        }

        if (selectedFile.length() > 10L * 1024 * 1024) {
            StudentDashboard.showError(dashboard, "The file must not exceed 10 MB.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            dashboard,
            "Submit this requirement?\n\nType: " + requirementType
                + "\nFile: " + selectedFile.getName(),
            "Confirm Submission",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        File destination = null;

        conn = null;
        try {
            conn = dashboard.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            String duplicateSql =
                "SELECT sr.requirement_id FROM student_requirements sr "
                + "JOIN enrollments e "
                + "ON sr.enrollment_id = e.enrollment_id "
                + "JOIN students s ON e.student_id = s.student_id "
                + "WHERE s.user_id = ? AND sr.enrollment_id = ? "
                + "AND sr.requirement_name = ?";

            try (PreparedStatement psDuplicate =
                     conn.prepareStatement(duplicateSql)) {
                psDuplicate.setInt(1, user.getUserId());
                psDuplicate.setInt(2, selectedEnrollment.id);
                psDuplicate.setString(3, requirementType);

                try (ResultSet rs = psDuplicate.executeQuery()) {
                    if (rs.next()) {
                        StudentDashboard.showError(dashboard,
                            "This requirement already has a record. "
                            + "Contact staff if you need to replace it."
                        );
                        return;
                    }
                }
            }

            File uploadFolder = new File("uploads/requirements");

            if (!uploadFolder.exists() && !uploadFolder.mkdirs()) {
                throw new IOException(
                    "Could not create the upload folder."
                );
            }

            String extension = getFileExtension(selectedFile.getName());
            String savedName = UUID.randomUUID() + extension;
            destination = new File(uploadFolder, savedName);

            Files.copy(
                selectedFile.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            );

            String insertSql =
                "INSERT INTO student_requirements "
                + "(enrollment_id, requirement_name, requirement_status, "
                + "original_file_name, file_path, submitted_at) "
                + "VALUES (?, ?, 'Submitted', ?, ?, CURRENT_TIMESTAMP)";

            try (PreparedStatement psInsert =
                     conn.prepareStatement(insertSql)) {
                psInsert.setInt(1, selectedEnrollment.id);
                psInsert.setString(2, requirementType);
                psInsert.setString(3, selectedFile.getName());
                psInsert.setString(4, destination.getAbsolutePath());
                psInsert.executeUpdate();
            } catch (SQLException ex) {
                Files.deleteIfExists(destination.toPath());
                throw ex;
            }

            outputArea.setText(
                "REQUIREMENT SUBMITTED\n\n"
                + "Enrollment: " + selectedEnrollment.label
                + "\nRequirement: " + requirementType
                + "\nFile: " + selectedFile.getName()
                + "\nStatus: Submitted"
            );

            helpLabel.setText("Requirement submitted successfully.");

            JOptionPane.showMessageDialog(
                dashboard,
                "Your requirement has been submitted successfully."
            );
        } catch (SQLException | IOException ex) {
            if (destination != null && destination.exists()) {
                try {
                    Files.deleteIfExists(destination.toPath());
                } catch (IOException ignored) {
                }
            }

            StudentDashboard.showError(dashboard,
                "Could not submit requirement: " + ex.getMessage()
            );
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception closeEx) {
                System.out.println(closeEx);
            }
        }
    }

    public void deleteMyRequirement() {
        String input = JOptionPane.showInputDialog(
            dashboard,
            "Enter the Requirement ID shown in View My Requirements:"
        );

        if (input == null || input.trim().isEmpty()) {
            return;
        }

        int requirementId;

        try {
            requirementId = Integer.parseInt(input.trim());

            if (requirementId <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            StudentDashboard.showError(dashboard, "Enter a valid positive Requirement ID.");
            return;
        }

        String filePath;
        String status;

        Connection conn = null;
        try {
            conn = dashboard.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            String findSql =
                "SELECT sr.file_path, sr.requirement_status "
                + "FROM student_requirements sr "
                + "JOIN enrollments e "
                + "ON sr.enrollment_id = e.enrollment_id "
                + "JOIN students s ON e.student_id = s.student_id "
                + "WHERE sr.requirement_id = ? AND s.user_id = ?";

            try (PreparedStatement ps =
                     conn.prepareStatement(findSql)) {
                ps.setInt(1, requirementId);
                ps.setInt(2, user.getUserId());

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        StudentDashboard.showError(dashboard,
                            "Requirement not found in your account."
                        );
                        return;
                    }

                    filePath = rs.getString("file_path");
                    status = rs.getString("requirement_status");
                }
            }

            if (status == null
                    || !(status.equalsIgnoreCase("Submitted")
                    || status.equalsIgnoreCase("Incomplete"))) {
                StudentDashboard.showError(dashboard,
                    "You can delete only unreviewed requirements. "
                    + "Contact enrollment staff if this document "
                    + "has already been reviewed."
                );
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                dashboard,
                "Delete requirement ID " + requirementId + "?",
                "Confirm Requirement Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            if (!deleteDAO.deleteOwnRequirement(requirementId, user.getUserId())) {
                StudentDashboard.showError(dashboard,
                    "The requirement could not be deleted. "
                    + "It may have been reviewed or removed already."
                );
                return;
            }

            boolean fileRemoved = false;

            if (filePath != null && !filePath.trim().isEmpty()) {
                try {
                    Path uploadDirectory =
                        new File("uploads/requirements")
                            .getCanonicalFile().toPath();
                    Path savedPath =
                        new File(filePath).getCanonicalFile().toPath();

                    if (savedPath.startsWith(uploadDirectory)
                            && !savedPath.equals(uploadDirectory)) {
                        fileRemoved = Files.deleteIfExists(savedPath);
                    }
                } catch (IOException ignored) {
                    fileRemoved = false;
                }
            }

            outputArea.setText(
                "REQUIREMENT DELETED\n\n"
                + "Requirement ID: " + requirementId
                + "\nDatabase record: Deleted"
                + "\nUploaded file: "
                + (fileRemoved ? "Deleted" : "Not found or not removed")
            );

            helpLabel.setText("Requirement deletion completed.");

            JOptionPane.showMessageDialog(
                dashboard,
                "The requirement record was deleted."
                + (fileRemoved
                    ? "\nThe uploaded file was deleted too."
                    : "\nThe uploaded file may still exist in the upload folder.")
            );
        } catch (Exception ex) {
            StudentDashboard.showError(dashboard,
                "Could not delete requirement: " + ex.getMessage()
            );
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception closeEx) {
                System.out.println(closeEx);
            }
        }
    }

    private String getFileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }
}
