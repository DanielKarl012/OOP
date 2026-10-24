
import javax.swing.SwingUtilities;

public class App {
    public static void main(String[] args) {
        DbConnection db = new DbConnection();
        db.connect();

        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}
