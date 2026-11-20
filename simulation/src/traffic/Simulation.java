package traffic;

public class Simulation {

    private final CityMap map;
    private final Car car;
    private long tick = 0;

    public Simulation(CityMap map) {
        this.map = map;
        this.car = new Car(1, 0, 2, Direction.EAST);
    }

    public void step() {
        tick++;
        car.advance();
        car.setPosition(
            Math.floorMod(car.x(), map.width()),
            Math.floorMod(car.y(), map.height()));
    }

    public CityMap map() { return map; }
    public Car car() { return car; }
    public long tick() { return tick; }
}