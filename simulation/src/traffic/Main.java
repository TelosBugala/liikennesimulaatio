package traffic;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {

    private static final String DEFAULT_SERVICE_URL = "http://localhost:5256";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createWindow);
    }

    private static void createWindow() {
        CityMap map = CityMap.createDefault();

        String serviceUrl = System.getProperty("traffic.serviceUrl", DEFAULT_SERVICE_URL);
        EventSender sender = new EventSender(serviceUrl);
        System.out.println("Sending events to " + serviceUrl);

        Simulation simulation = new Simulation(map, sender::send);
        CityPanel panel = new CityPanel(simulation);
        SimulationRunner runner = new SimulationRunner(simulation);
        ControlBar controlBar = new ControlBar(simulation, runner);
        runner.setFrameListener(() -> {
            panel.repaint();
            controlBar.refresh();
        });

        JFrame frame = new JFrame("Traffic simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(controlBar, BorderLayout.NORTH);
        frame.add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        runner.start();
    }
}