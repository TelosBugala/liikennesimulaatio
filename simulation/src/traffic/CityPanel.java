package traffic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JPanel;

public class CityPanel extends JPanel {

    private static final int TILE_SIZE = 48;
    private static final int CAR_LENGTH = 36;
    private static final int CAR_WIDTH = 20;
    private static final int LIGHT_SIZE = 6;

    private final Simulation simulation;

    public CityPanel(Simulation simulation) {
        this.simulation = simulation;
        CityMap map = simulation.map();
        setPreferredSize(new Dimension(
            map.width() * TILE_SIZE,
            map.height() * TILE_SIZE));
    }

     @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawMap(g);
        drawLights(g);
        for (Car car : simulation.cars()) {
            drawCar(g, car);
        }
    }

    private void drawMap(Graphics g) {
        CityMap map = simulation.map();
        for (int y = 0; y < map.height(); y++) {
            for (int x = 0; x < map.width(); x++) {
                g.setColor(colorFor(map.tileAt(x, y)));
                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    private void drawLights(Graphics g) {
        long tick = simulation.tick();
        for (TrafficLight light : simulation.lights()) {
            int left = light.x() * TILE_SIZE;
            int top = light.y() * TILE_SIZE;

            // Vaakasuunnan valo: ruudun vasen reuna, keskellä pystysuunnassa
            g.setColor(lightColor(light.allows(Direction.EAST, tick)));
            g.fillRect(left, top + (TILE_SIZE - LIGHT_SIZE) / 2,
                       LIGHT_SIZE, LIGHT_SIZE);

            // Pystysuunnan valo: ruudun yläreuna, keskellä vaakasuunnassa
            g.setColor(lightColor(light.allows(Direction.SOUTH, tick)));
            g.fillRect(left + (TILE_SIZE - LIGHT_SIZE) / 2, top,
                       LIGHT_SIZE, LIGHT_SIZE);
        }
    }

    private Color lightColor(boolean green) {
        return green ? new Color(0, 220, 0) : new Color(230, 0, 0);
    }

    private void drawCar(Graphics g, Car car) {
        boolean horizontal = car.direction() == Direction.EAST
                          || car.direction() == Direction.WEST;
        int w = horizontal ? CAR_LENGTH : CAR_WIDTH;
        int h = horizontal ? CAR_WIDTH : CAR_LENGTH;
        int px = car.x() * TILE_SIZE + (TILE_SIZE - w) / 2;
        int py = car.y() * TILE_SIZE + (TILE_SIZE - h) / 2;
        g.setColor(car.moveEveryTicks() > 1 ? Color.RED : Color.YELLOW);
        g.fillRect(px, py, w, h);
    }

    private Color colorFor(char tile) {
        return switch (tile) {
            case '-', '|', '+' -> Color.DARK_GRAY;
            default -> new Color(60, 120, 60);
        };
    }
}