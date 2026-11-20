package traffic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JPanel;

public class CityPanel extends JPanel {

    private static final int TILE_SIZE = 48;

    private final CityMap map;

    public CityPanel(CityMap map) {
        this.map = map;
        setPreferredSize(new Dimension(
            map.width() * TILE_SIZE,
            map.height() * TILE_SIZE));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int y = 0; y < map.height(); y++) {
            for (int x = 0; x < map.width(); x++) {
                g.setColor(colorFor(map.tileAt(x, y)));
                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    private Color colorFor(char tile) {
        return switch (tile) {
            case '-', '|', '+' -> Color.DARK_GRAY;
            default -> new Color(60, 120, 60);
        };
    }
}