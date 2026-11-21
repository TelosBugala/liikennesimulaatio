package traffic;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Main {

    private static final int TICK_MILLIS = 200;
    private static final String DEFAULT_SERVICE_URL = "http://localhost:5256";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createWindow);
    }

    private static void createWindow() {
        CityMap map = CityMap.createDefault();
        String serviceUrl = System.getProperty("traffic.serviceUrl", DEFAULT_SERVICE_URL);
        EventSender sender = new EventSender(serviceUrl);
        Simulation simulation = new Simulation(map, event -> {
            System.out.println(event);
            sender.send(event);
        });
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