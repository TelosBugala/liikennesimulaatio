package traffic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Simulation {

    private final CityMap map;
    private final Consumer<PassEvent> eventListener;
    private final List<Car> cars = new ArrayList<>();
    private final List<TrafficLight> lights = new ArrayList<>();
    private final List<MeasurementPoint> points = new ArrayList<>();
    private long tick = 0;

    public Simulation(CityMap map, Consumer<PassEvent> eventListener) {
        this.map = map;
        this.eventListener = eventListener;
        createLights();
        createPoints();
        // Vaakatie (rivi 2), itään
        cars.add(new Car(1, 6, 2, Direction.EAST, 3));  // hidas
        cars.add(new Car(2, 0, 2, Direction.EAST, 1));  // nopea
        // Pystytie (sarake 3), etelään
        cars.add(new Car(3, 3, 0, Direction.SOUTH, 2));
        cars.add(new Car(4, 3, 5, Direction.SOUTH, 1));
    }

    private void createLights() {
        for (int y = 0; y < map.height(); y++) {
            for (int x = 0; x < map.width(); x++) {
                if (map.tileAt(x, y) == '+') {
                    lights.add(new TrafficLight(x, y));
                }
            }
        }
    }

    private void createPoints() {
        points.add(new MeasurementPoint(1, 1, 2));
        points.add(new MeasurementPoint(2, 10, 2));
        points.add(new MeasurementPoint(3, 3, 4));
    }

    public void step() {
        tick++;
        for (Car car : cars) {
            if (!car.isDue(tick)) {
                continue;
            }
            int nextX = Math.floorMod(car.x() + car.direction().dx, map.width());
            int nextY = Math.floorMod(car.y() + car.direction().dy, map.height());
            if (isRedLightAt(nextX, nextY, car.direction())) {
                continue;
            }
            if (!isOccupied(nextX, nextY)) {
                car.moveTo(nextX, nextY);
                reportPassings(car);
            }
        }
    }

    private void reportPassings(Car car) {
        for (MeasurementPoint point : points) {
            if (point.x() == car.x() && point.y() == car.y()) {
                eventListener.accept(
                    new PassEvent(tick, point.id(), car.id(), car.direction()));
            }
        }
    }

    private boolean isRedLightAt(int x, int y, Direction direction) {
        for (TrafficLight light : lights) {
            if (light.x() == x && light.y() == y) {
                return !light.allows(direction, tick);
            }
        }
        return false;
    }

    private boolean isOccupied(int x, int y) {
        for (Car other : cars) {
            if (other.x() == x && other.y() == y) {
                return true;
            }
        }
        return false;
    }

    public CityMap map() { return map; }
    public List<Car> cars() { return cars; }
    public List<TrafficLight> lights() { return lights; }
    public long tick() { return tick; }
}