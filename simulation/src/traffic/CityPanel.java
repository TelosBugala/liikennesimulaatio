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
    private static final int LANE_OFFSET = TILE_SIZE / 4;

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
                char tile = map.tileAt(x, y);
                g.setColor(colorFor(tile));
                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                drawLaneDivider(g, tile, x, y);
            }
        }
    }

    private void drawLaneDivider(Graphics g, char tile, int x, int y) {
        int dash = TILE_SIZE / 2;
        int thick = 2;
        int left = x * TILE_SIZE;
        int top = y * TILE_SIZE;
        g.setColor(Color.LIGHT_GRAY);
        if (tile == '-') {
            g.fillRect(left + (TILE_SIZE - dash) / 2,
                       top + (TILE_SIZE - thick) / 2, dash, thick);
        } else if (tile == '|') {
            g.fillRect(left + (TILE_SIZE - thick) / 2,
                       top + (TILE_SIZE - dash) / 2, thick, dash);
        }
    }

    private void drawLights(Graphics g) {
        long tick = simulation.tick();
        int mid = (TILE_SIZE - LIGHT_SIZE) / 2;
        int far = TILE_SIZE - LIGHT_SIZE;
        for (TrafficLight light : simulation.lights()) {
            int left = light.x() * TILE_SIZE;
            int top = light.y() * TILE_SIZE;

            // Vaakasuunta: vasen reuna (itään) ja oikea reuna (länteen)
            g.setColor(lightColor(light.allows(Direction.EAST, tick)));
            g.fillRect(left, top + mid, LIGHT_SIZE, LIGHT_SIZE);
            g.setColor(lightColor(light.allows(Direction.WEST, tick)));
            g.fillRect(left + far, top + mid, LIGHT_SIZE, LIGHT_SIZE);

            // Pystysuunta: yläreuna (etelään) ja alareuna (pohjoiseen)
            g.setColor(lightColor(light.allows(Direction.SOUTH, tick)));
            g.fillRect(left + mid, top, LIGHT_SIZE, LIGHT_SIZE);
            g.setColor(lightColor(light.allows(Direction.NORTH, tick)));
            g.fillRect(left + mid, top + far, LIGHT_SIZE, LIGHT_SIZE);
        }
    }

    private void drawCar(Graphics g, Car car) {
        boolean horizontal = car.direction().isHorizontal();
        int w = horizontal ? CAR_LENGTH : CAR_WIDTH;
        int h = horizontal ? CAR_WIDTH : CAR_LENGTH;

        // Siirtymä kohti ajosuunnan oikeaa puolta
        int laneX = -car.direction().dy * LANE_OFFSET;
        int laneY = car.direction().dx * LANE_OFFSET;

        int px = car.x() * TILE_SIZE + (TILE_SIZE - w) / 2 + laneX;
        int py = car.y() * TILE_SIZE + (TILE_SIZE - h) / 2 + laneY;
        g.setColor(car.moveEveryTicks() > 1 ? Color.RED : Color.YELLOW);
        g.fillRect(px, py, w, h);
    }

    private Color colorFor(char tile) {
        return switch (tile) {
            case '-', '|', '+' -> Color.DARK_GRAY;
            default -> new Color(60, 120, 60);
        };
    }

    private Color lightColor(boolean green) {
        return green ? new Color(0, 220, 0) : new Color(230, 0, 0);
    }
}