package traffic;

import java.util.ArrayList;
import java.util.List;

public class Simulation {

    private final CityMap map;
    private final List<Car> cars = new ArrayList<>();
    private long tick = 0;

    public Simulation(CityMap map) {
        this.map = map;
        cars.add(new Car(1, 6, 2, Direction.EAST, 3));  // hidas
        cars.add(new Car(2, 0, 2, Direction.EAST, 1));  // nopea
        cars.add(new Car(3, 10, 2, Direction.EAST, 1)); // nopea
    }

    public void step() {
        tick++;
        for (Car car : cars) {
            if (!car.isDue(tick)) {
                continue;
            }
            int nextX = Math.floorMod(car.x() + car.direction().dx, map.width());
            int nextY = Math.floorMod(car.y() + car.direction().dy, map.height());
            if (!isOccupied(nextX, nextY)) {
                car.moveTo(nextX, nextY);
            }
        }
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
    public long tick() { return tick; }
}