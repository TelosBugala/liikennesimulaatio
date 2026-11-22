package traffic;

import java.awt.Component;
import javax.swing.JOptionPane;

public class PointEditor {

    private final Simulation simulation;
    private final SimulationRunner runner;
    private final Component parent;

    public PointEditor(Simulation simulation, SimulationRunner runner, Component parent) {
        this.simulation = simulation;
        this.runner = runner;
        this.parent = parent;
    }

    public void leftClick(int x, int y) {
        MeasurementPoint existing = simulation.pointAt(x, y);
        if (existing != null) {
            rename(existing);
        } else if (simulation.map().isRoad(x, y)) {
            add(x, y);
        }
    }

    public void rightClick(int x, int y) {
        MeasurementPoint existing = simulation.pointAt(x, y);
        if (existing != null) {
            simulation.removePoint(existing);
            parent.repaint();
        }
    }

    private void add(int x, int y) {
        String name = askName("Add measurement point", "");
        if (name != null) {
            simulation.addPoint(name, x, y);
            parent.repaint();
        }
    }

    private void rename(MeasurementPoint point) {
        String name = askName("Rename measurement point", point.name());
        if (name != null && !name.equals(point.name())) {
            simulation.renamePoint(point, name);
            parent.repaint();
        }
    }

    // Pysäyttää simulaation dialogin ajaksi ja palauttaa sen entiseen tilaan.
    // Palauttaa kelvollisen nimen tai null, jos käyttäjä peruutti.
    private String askName(String title, String initial) {
        boolean wasPaused = runner.isPaused();
        runner.setPaused(true);
        try {
            String text = initial;
            while (true) {
                Object answer = JOptionPane.showInputDialog(
                    parent,
                    "Name (letters, digits, spaces and hyphens, max "
                        + MeasurementPoint.MAX_NAME_LENGTH + " characters):",
                    title,
                    JOptionPane.PLAIN_MESSAGE,
                    null, null, text);
                if (answer == null) {
                    return null;
                }
                text = (String) answer;
                String name = text.strip();
                if (MeasurementPoint.isValidName(name)) {
                    return name;
                }
                JOptionPane.showMessageDialog(
                    parent,
                    "The name must be 1-" + MeasurementPoint.MAX_NAME_LENGTH
                        + " characters: letters, digits, spaces and hyphens only.",
                    "Invalid name",
                    JOptionPane.WARNING_MESSAGE);
            }
        } finally {
            runner.setPaused(wasPaused);
        }
    }
}