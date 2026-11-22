package traffic;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class Simulation {

    private static final int MIN_SPAWN_DELAY = 5;
    private static final int MAX_SPAWN_DELAY = 20;
    private static final int MAX_MOVE_EVERY_TICKS = 3;

    private final CityMap map;
    private final Consumer<PassEvent> eventListener;
    private final Random random = new Random();
    private final RoutePlanner planner;
    private final List<Car> cars = new ArrayList<>();
    private final List<TrafficLight> lights = new ArrayList<>();
    private final List<MeasurementPoint> points = new ArrayList<>();
    private final List<SpawnPoint> spawnPoints = new ArrayList<>();
    private int nextCarId = 1;
    private int ticksUntilSpawn;
    private long tick = 0;

    public Simulation(CityMap map, Consumer<PassEvent> eventListener) {
        this.map = map;
        this.eventListener = eventListener;
        this.planner = new RoutePlanner(map, random);
        createLights();
        createPoints();
        createSpawnPoints();
        ticksUntilSpawn = nextSpawnDelay();
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

    private void createSpawnPoints() {
        for (int y = 0; y < map.height(); y++) {
            for (int x = 0; x < map.width(); x++) {
                Direction inward = inwardDirectionAt(x, y);
                if (inward != null && map.roadAllows(x, y, inward)) {
                    spawnPoints.add(new SpawnPoint(x, y, inward));
                }
            }
        }
    }

    private Direction inwardDirectionAt(int x, int y) {
        if (x == 0) return Direction.EAST;
        if (x == map.width() - 1) return Direction.WEST;
        if (y == 0) return Direction.SOUTH;
        if (y == map.height() - 1) return Direction.NORTH;
        return null;
    }

    public void step() {
        tick++;
        moveCars();
        spawnCar();
    }

    private void moveCars() {
        Iterator<Car> iterator = cars.iterator();
        while (iterator.hasNext()) {
            Car car = iterator.next();
            if (!car.isDue(tick)) {
                continue;
            }
            if (car.hasArrived()) {
                iterator.remove();
                continue;
            }
            Direction direction = car.nextDirection();
            int nextX = car.x() + direction.dx;
            int nextY = car.y() + direction.dy;
            if (isRedLightAt(nextX, nextY, direction)) {
                continue;
            }
            if (wouldBlockIntersection(car, nextX, nextY)) {
                continue;
            }
            if (!isOccupied(nextX, nextY, direction)) {
                car.moveTo(nextX, nextY);
                reportPassings(car);
            }
        }
    }

    private void spawnCar() {
        ticksUntilSpawn--;
        if (ticksUntilSpawn > 0 || spawnPoints.size() < 2) {
            return;
        }
        int spawnIndex = random.nextInt(spawnPoints.size());
        SpawnPoint spawn = spawnPoints.get(spawnIndex);
        if (isOccupied(spawn.x(), spawn.y(), spawn.direction())) {
            return; // yritetään uudelleen seuraavalla tickillä
        }

        // Määränpää: jokin muu syntymispiste
        int goalIndex = random.nextInt(spawnPoints.size() - 1);
        if (goalIndex >= spawnIndex) {
            goalIndex++;
        }
        SpawnPoint goal = spawnPoints.get(goalIndex);

        List<Direction> route = planner.plan(spawn.x(), spawn.y(), goal.x(), goal.y());
        if (route == null) {
            return; // reittiä ei ole, yritetään uudelleen
        }

        int moveEveryTicks = 1 + random.nextInt(MAX_MOVE_EVERY_TICKS);
        Car car = new Car(nextCarId++, spawn.x(), spawn.y(),
                          spawn.direction(), moveEveryTicks, route);
        cars.add(car);
        Debug.log("Car " + car.id() + ": (" + spawn.x() + "," + spawn.y()
            + ") -> (" + goal.x() + "," + goal.y() + "): "
            + RoutePlanner.describe(route));
        ticksUntilSpawn = nextSpawnDelay();
    }

    private int nextSpawnDelay() {
        return MIN_SPAWN_DELAY + random.nextInt(MAX_SPAWN_DELAY - MIN_SPAWN_DELAY + 1);
    }

    private void reportPassings(Car car) {
        for (MeasurementPoint point : points) {
            if (point.x() == car.x() && point.y() == car.y()) {
                 eventListener.accept(new PassEvent(
                    tick, SimTime.at(tick), point.id(), car.id(), car.direction()));
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

    private boolean wouldBlockIntersection(Car car, int x, int y) {
        if (map.tileAt(x, y) != '+') {
            return false;
        }
        Direction exit = car.directionAfterNext();
        if (exit == null) {
            return false;
        }
        return isOccupied(x + exit.dx, y + exit.dy, exit);
    }

    private boolean isOccupied(int x, int y, Direction direction) {
        boolean intersection = map.tileAt(x, y) == '+';
        for (Car other : cars) {
            if (other.x() == x && other.y() == y
                    && (intersection || other.direction() == direction)) {
                return true;
            }
        }
        return false;
    }

    public CityMap map() { return map; }
    public List<Car> cars() { return cars; }
    public List<TrafficLight> lights() { return lights; }
    public List<MeasurementPoint> points() { return points; }
    public long tick() { return tick; }
        public LocalDateTime simTime() { return SimTime.at(tick); }
}