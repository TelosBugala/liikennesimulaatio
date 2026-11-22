package traffic;

import java.awt.BorderLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ControlBar extends JPanel {

    private static final DateTimeFormatter CLOCK_FORMAT =
        DateTimeFormatter.ofPattern("EEE yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    private final Simulation simulation;
    private final JLabel clockLabel = new JLabel();

    public ControlBar(Simulation simulation) {
        super(new BorderLayout());
        this.simulation = simulation;
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        clockLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        add(clockLabel, BorderLayout.EAST);
        refresh();
    }

    public void refresh() {
        clockLabel.setText(CLOCK_FORMAT.format(simulation.simTime()));
    }
}