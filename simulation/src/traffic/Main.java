package traffic;

import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {

    private static final String DEFAULT_SERVICE_URL = "http://localhost:5256";
    private static final String POINTS_FILE = "measurement-points.txt";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createWindow);
    }

    private static void createWindow() {
        CityMap map = CityMap.createDefault();

        String serviceUrl = System.getProperty("traffic.serviceUrl", DEFAULT_SERVICE_URL);
        EventSender sender = new EventSender(serviceUrl);
        System.out.println("Sending events to " + serviceUrl);

        Simulation simulation = new Simulation(map, sender::send);
        PointStore store = new PointStore(Path.of(POINTS_FILE));
        System.out.println("Measurement points file: " + store.file().toAbsolutePath());
        loadPoints(simulation, store);
        simulation.setPointsChangedListener(() -> savePoints(simulation, store));

        CityPanel panel = new CityPanel(simulation);
        SimulationRunner runner = new SimulationRunner(simulation);
        ControlBar controlBar = new ControlBar(simulation, runner);
        panel.setPointEditor(new PointEditor(simulation, runner, panel));
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

    private static void loadPoints(Simulation simulation, PointStore store) {
        try {
            PointStore.Loaded loaded = store.load();
            if (loaded == null) {
                simulation.addDefaultPoints();
                savePoints(simulation, store);
            } else {
                simulation.restorePoints(loaded.points(), loaded.nextId());
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Could not read " + store.file() + ": " + e.getMessage()
                + " (using default points)");
            simulation.addDefaultPoints();
        }
    }

    private static void savePoints(Simulation simulation, PointStore store) {
        try {
            store.save(simulation.points(), simulation.nextPointId());
        } catch (IOException e) {
            System.err.println("Could not save " + store.file() + ": " + e.getMessage());
        }
    }
}