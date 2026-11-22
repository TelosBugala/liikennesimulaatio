package traffic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;

public class CityPanel extends JPanel {

    private static final int TILE_SIZE = 48;
    private static final int CAR_LENGTH = 36;
    private static final int CAR_WIDTH = 20;
    private static final int LIGHT_SIZE = 6;
    private static final int LANE_OFFSET = TILE_SIZE / 4;
    private static final int POINT_LINE_THICKNESS = 4;
    private static final Color POINT_COLOR = new Color(0, 200, 255);

    private final Simulation simulation;
    private PointEditor pointEditor;

        public CityPanel(Simulation simulation) {
        this.simulation = simulation;
        CityMap map = simulation.map();
        setPreferredSize(new Dimension(
            map.width() * TILE_SIZE,
            map.height() * TILE_SIZE));
        setToolTipText("Left click: add or rename a measurement point. "
            + "Right click: remove it.");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouse(e);
            }
        });
    }

    public void setPointEditor(PointEditor pointEditor) {
        this.pointEditor = pointEditor;
    }

    private void handleMouse(MouseEvent e) {
        if (pointEditor == null) {
            return;
        }
        CityMap map = simulation.map();
        int x = e.getX() / TILE_SIZE;
        int y = e.getY() / TILE_SIZE;
        if (x < 0 || y < 0 || x >= map.width() || y >= map.height()) {
            return;
        }
        if (SwingUtilities.isLeftMouseButton(e)) {
            pointEditor.leftClick(x, y);
        } else if (SwingUtilities.isRightMouseButton(e)) {
            pointEditor.rightClick(x, y);
        }
    }

       @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawMap(g);
        drawMeasurementPoints(g);
        drawLights(g);
        for (Car car : simulation.cars()) {
            drawCar(g, car);
        }
        drawPointLabels(g);
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

        private void drawMeasurementPoints(Graphics g) {
        CityMap map = simulation.map();
        int line = POINT_LINE_THICKNESS;
        g.setColor(POINT_COLOR);
        for (MeasurementPoint point : simulation.points()) {
            int left = point.x() * TILE_SIZE;
            int top = point.y() * TILE_SIZE;
            char tile = map.tileAt(point.x(), point.y());
            if (tile == '-') {
                // Vaakatie: pystysuora viiva tien poikki
                g.fillRect(left + (TILE_SIZE - line) / 2, top, line, TILE_SIZE);
            } else if (tile == '|') {
                // Pystytie: vaakasuora viiva tien poikki
                g.fillRect(left, top + (TILE_SIZE - line) / 2, TILE_SIZE, line);
            } else {
                // Risteys tai muu ruutu: kehys ruudun reunaan
                g.drawRect(left + 1, top + 1, TILE_SIZE - 3, TILE_SIZE - 3);
            }
        }
    }

    private void drawPointLabels(Graphics g) {
        g.setFont(g.getFont().deriveFont(Font.BOLD, 11f));
        FontMetrics metrics = g.getFontMetrics();
        for (MeasurementPoint point : simulation.points()) {
            String name = point.name();
            int textWidth = metrics.stringWidth(name);
            int x = point.x() * TILE_SIZE + 3;
            int y = point.y() * TILE_SIZE + 12;
            x = Math.max(2, Math.min(x, getWidth() - textWidth - 2));

            g.setColor(new Color(0, 0, 0, 170));
            g.fillRect(x - 2, y - metrics.getAscent(),
                       textWidth + 4, metrics.getHeight());
            g.setColor(Color.WHITE);
            g.drawString(name, x, y);
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