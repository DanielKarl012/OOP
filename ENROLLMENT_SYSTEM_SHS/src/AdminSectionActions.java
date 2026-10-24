
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AdminSectionActions {

    private AdminDashboard dashboard;
    private DbConnection db = new DbConnection();

    public AdminSectionActions(AdminDashboard dashboard) {
        this.dashboard = dashboard;
    }

    public void addSection() {
        JTextField nameField = new JTextField();
        JComboBox<Integer> gradeBox =
                new JComboBox<>(new Integer[]{11, 12});
        JComboBox<IdOption> strandBox = new JComboBox<>();
        JComboBox<IdOption> yearBox = new JComboBox<>();
        JTextField slotField = new JTextField("40");

        if (!loadOptions(strandBox, yearBox)) {
            return;
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Section Name:"));
        form.add(nameField);
        form.add(new JLabel("Grade Level:"));
        form.add(gradeBox);
        form.add(new JLabel("Strand:"));
        form.add(strandBox);
        form.add(new JLabel("School Year:"));
        form.add(yearBox);
        form.add(new JLabel("Slot Limit:"));
        form.add(slotField);

        int choice = JOptionPane.showConfirmDialog(
                dashboard, form, "Create SHS Section",
                JOptionPane.OK_CANCEL_OPTION);

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String name = nameField.getText().trim();
        IdOption strand = (IdOption) strandBox.getSelectedItem();
        IdOption year = (IdOption) yearBox.getSelectedItem();

        if (name.isEmpty() || strand == null || year == null) {
            AdminDashboard.showError(dashboard, "Complete all section details.");
            return;
        }

        try {
            int grade = (Integer) gradeBox.getSelectedItem();
            int slots = Integer.parseInt(slotField.getText().trim());

            if ((grade != 11 && grade != 12) || slots < 1) {
                AdminDashboard.showError(dashboard, "Select Grade 11 or 12 and a valid slot limit.");
                return;
            }

            String sql = "INSERT INTO sections "
                    + "(section_name, grade_level, strand_id, "
                    + "school_year_id, slot_limit, is_active) "
                    + "VALUES (?, ?, ?, ?, ?, TRUE)";

            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = db.getConnection();

                if (conn == null) {
                    throw new SQLException("Could not connect to the database.");
                }

                ps = conn.prepareStatement(sql);
                ps.setString(1, name);
                ps.setInt(2, grade);
                ps.setInt(3, strand.id);
                ps.setInt(4, year.id);
                ps.setInt(5, slots);
                ps.executeUpdate();
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

            JOptionPane.showMessageDialog(
                    dashboard, "SHS section created successfully.");
            dashboard.viewSections();

        } catch (NumberFormatException ex) {
            AdminDashboard.showError(dashboard, "Slot limit must be a valid number.");
        } catch (SQLException ex) {
            AdminDashboard.showError(dashboard, "Could not create section: " + ex.getMessage());
        }
    }

    public boolean loadOptions(
            JComboBox<IdOption> strandBox,
            JComboBox<IdOption> yearBox) {

        String strandSql =
                "SELECT strand_id, strand_code, strand_name "
                        + "FROM strands ORDER BY strand_code";

        String yearSql =
                "SELECT school_year_id, year_label FROM school_years "
                        + "ORDER BY school_year_id DESC";

        Connection conn = null;
        try {
            conn = db.getConnection();

            if (conn == null) {
                throw new SQLException("Could not connect to the database.");
            }

            try (PreparedStatement ps =
                         conn.prepareStatement(strandSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    strandBox.addItem(new IdOption(
                            rs.getInt("strand_id"),
                            rs.getString("strand_code") + " - "
                                    + rs.getString("strand_name")));
                }
            }

            try (PreparedStatement ps =
                         conn.prepareStatement(yearSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    yearBox.addItem(new IdOption(
                            rs.getInt("school_year_id"),
                            rs.getString("year_label")));
                }
            }

            if (strandBox.getItemCount() == 0
                    || yearBox.getItemCount() == 0) {
                AdminDashboard.showError(dashboard,
                        "Add at least one strand and school year first.");
                return false;
            }

            return true;
        } catch (Exception ex) {
            AdminDashboard.showError(dashboard, "Could not load options: " + ex.getMessage());
            return false;
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

    public void updateSection() {
        String input = JOptionPane.showInputDialog(
                dashboard, "Enter the Section ID to update:");

        if (input == null) {
            return;
        }

        try {
            int id = Integer.parseInt(input.trim());

            String selectSql =
                    "SELECT section_name, grade_level, strand_id, "
                            + "school_year_id, slot_limit, is_active "
                            + "FROM sections WHERE section_id = ? "
                            + "AND grade_level IN (11, 12)";

            String name;
            int grade;
            int strandId;
            int yearId;
            int slots;
            boolean active;

            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = db.getConnection();

                if (conn == null) {
                    throw new SQLException("Could not connect to the database.");
                }

                ps = conn.prepareStatement(selectSql);
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        AdminDashboard.showError(dashboard, "SHS section not found.");
                        return;
                    }

                    name = rs.getString("section_name");
                    grade = rs.getInt("grade_level");
                    strandId = rs.getInt("strand_id");
                    yearId = rs.getInt("school_year_id");
                    slots = rs.getInt("slot_limit");
                    active = rs.getBoolean("is_active");
                }
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

            JTextField nameField = new JTextField(name);
            JComboBox<Integer> gradeBox =
                    new JComboBox<>(new Integer[]{11, 12});
            gradeBox.setSelectedItem(grade);

            JComboBox<IdOption> strandBox = new JComboBox<>();
            JComboBox<IdOption> yearBox = new JComboBox<>();

            if (!loadOptions(strandBox, yearBox)) {
                return;
            }

            selectOption(strandBox, strandId);
            selectOption(yearBox, yearId);

            JTextField slotField = new JTextField(
                    String.valueOf(slots));
            JCheckBox activeBox = new JCheckBox(
                    "Active Section", active);

            JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
            form.add(new JLabel("Section Name:"));
            form.add(nameField);
            form.add(new JLabel("Grade Level:"));
            form.add(gradeBox);
            form.add(new JLabel("Strand:"));
            form.add(strandBox);
            form.add(new JLabel("School Year:"));
            form.add(yearBox);
            form.add(new JLabel("Slot Limit:"));
            form.add(slotField);
            form.add(new JLabel("Status:"));
            form.add(activeBox);

            int choice = JOptionPane.showConfirmDialog(
                    dashboard, form, "Update SHS Section",
                    JOptionPane.OK_CANCEL_OPTION);

            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            String newName = nameField.getText().trim();
            IdOption strand = (IdOption) strandBox.getSelectedItem();
            IdOption year = (IdOption) yearBox.getSelectedItem();
            int newGrade = (Integer) gradeBox.getSelectedItem();
            int newSlots = Integer.parseInt(slotField.getText().trim());

            if (newName.isEmpty() || strand == null || year == null
                    || newSlots < 1) {
                AdminDashboard.showError(dashboard, "Enter valid section details.");
                return;
            }

            String updateSql =
                    "UPDATE sections SET section_name = ?, "
                            + "grade_level = ?, strand_id = ?, "
                            + "school_year_id = ?, slot_limit = ?, "
                            + "is_active = ? WHERE section_id = ? "
                            + "AND grade_level IN (11, 12)";

            conn = null;
            ps = null;
            try {
                conn = db.getConnection();

                if (conn == null) {
                    throw new SQLException("Could not connect to the database.");
                }

                ps = conn.prepareStatement(updateSql);
                ps.setString(1, newName);
                ps.setInt(2, newGrade);
                ps.setInt(3, strand.id);
                ps.setInt(4, year.id);
                ps.setInt(5, newSlots);
                ps.setBoolean(6, activeBox.isSelected());
                ps.setInt(7, id);
                ps.executeUpdate();
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

            JOptionPane.showMessageDialog(
                    dashboard, "Section updated successfully.");
            dashboard.viewSections();

        } catch (NumberFormatException ex) {
            AdminDashboard.showError(dashboard, "Enter valid numeric values.");
        } catch (SQLException ex) {
            AdminDashboard.showError(dashboard, "Could not update section: " + ex.getMessage());
        }
    }

    private void selectOption(
            JComboBox<IdOption> box, int id) {
        for (int i = 0; i < box.getItemCount(); i++) {
            if (box.getItemAt(i).id == id) {
                box.setSelectedIndex(i);
                return;
            }
        }
    }

    public void deleteSection() {
        String input = JOptionPane.showInputDialog(
                dashboard, "Enter the Section ID to deactivate:");

        if (input == null) {
            return;
        }

        try {
            int id = Integer.parseInt(input.trim());

            int choice = JOptionPane.showConfirmDialog(
                    dashboard,
                    "Deactivate this section? Existing enrollment records "
                            + "will be preserved.",
                    "Confirm Deactivation",
                    JOptionPane.YES_NO_OPTION);

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }

            String sql =
                    "UPDATE sections SET is_active = FALSE "
                            + "WHERE section_id = ? "
                            + "AND grade_level IN (11, 12)";

            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = db.getConnection();

                if (conn == null) {
                    throw new SQLException("Could not connect to the database.");
                }

                ps = conn.prepareStatement(sql);
                ps.setInt(1, id);

                if (ps.executeUpdate() == 0) {
                    AdminDashboard.showError(dashboard, "SHS section not found.");
                    return;
                }
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

            JOptionPane.showMessageDialog(
                    dashboard, "Section deactivated successfully.");
            dashboard.viewSections();

        } catch (NumberFormatException ex) {
            AdminDashboard.showError(dashboard, "Section ID must be a number.");
        } catch (SQLException ex) {
            AdminDashboard.showError(dashboard, "Could not deactivate section: " + ex.getMessage());
        }
    }

    public static class IdOption {
        public int id;
        public String label;

        public IdOption(int id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label + " (ID: " + id + ")";
        }
    }
}
