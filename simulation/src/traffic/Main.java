package traffic;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Main {

    private static final int TICK_MILLIS = 200;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createWindow);
    }

    private static void createWindow() {
        CityMap map = CityMap.createDefault();
        Simulation simulation = new Simulation(map);
        CityPanel panel = new CityPanel(simulation);

        JFrame frame = new JFrame("Traffic simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Timer timer = new Timer(TICK_MILLIS, e -> {
            simulation.step();
            panel.repaint();
        });
        timer.start();
    }
}