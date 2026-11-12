package puffpatrol;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--export-art")) {
            ArtExporter.main(args.length > 1 ? new String[]{args[1]} : new String[0]);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Puffball Patrol");
            GamePanel panel = new GamePanel();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(panel);
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.start();
        });
    }
}
