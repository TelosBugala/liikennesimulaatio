package traffic;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ControlBar extends JPanel {

    private static final DateTimeFormatter CLOCK_FORMAT =
        DateTimeFormatter.ofPattern("EEE yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    private final Simulation simulation;
    private final SimulationRunner runner;
    private final JButton pauseButton = new JButton("Pause");
    private final JButton slowerButton = new JButton("Slower");
    private final JButton fasterButton = new JButton("Faster");
    private final JLabel speedLabel = new JLabel();
    private final JLabel clockLabel = new JLabel();

    public ControlBar(Simulation simulation, SimulationRunner runner) {
        super(new BorderLayout());
        this.simulation = simulation;
        this.runner = runner;
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 13);
        speedLabel.setFont(mono);
        clockLabel.setFont(mono);

        // Kiinteä leveys Pause-painikkeelle: mitataan pidempi teksti etukäteen
        pauseButton.setText("Resume");
        Dimension pauseSize = pauseButton.getPreferredSize();
        pauseButton.setPreferredSize(pauseSize);

        pauseButton.addActionListener(e -> {
            runner.togglePause();
            refresh();
        });
        slowerButton.addActionListener(e -> {
            runner.slower();
            refresh();
        });
        fasterButton.addActionListener(e -> {
            runner.faster();
            refresh();
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.add(pauseButton);
        controls.add(slowerButton);
        controls.add(speedLabel);
        controls.add(fasterButton);

        add(controls, BorderLayout.WEST);
        add(clockLabel, BorderLayout.EAST);
        refresh();
    }

    public void refresh() {
        clockLabel.setText(CLOCK_FORMAT.format(simulation.simTime()));
        speedLabel.setText(String.format("%5s", runner.speedLabel()));
        pauseButton.setText(runner.isPaused() ? "Resume" : "Pause");
        slowerButton.setEnabled(runner.canSlower());
        fasterButton.setEnabled(runner.canFaster());
    }
}