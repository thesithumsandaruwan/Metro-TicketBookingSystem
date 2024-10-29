import javax.swing.*;

public class MetroBookingSystem {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new MetroBookingGUI().setVisible(true);
        });
    }
}
