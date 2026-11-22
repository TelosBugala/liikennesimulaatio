package traffic;

import java.util.List;

public class Car {

    private final int id;
    private final int moveEveryTicks;
    private final List<Direction> route;
    private int routeIndex = 0;
    private Direction direction;
    private int x;
    private int y;

    public Car(int id, int x, int y, Direction direction,
               int moveEveryTicks, List<Direction> route) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.moveEveryTicks = moveEveryTicks;
        this.route = route;
    }

    public boolean isDue(long tick) {
        return tick % moveEveryTicks == 0;
    }

        // Nimellisnopeus km/h: ruudun pituus jaettuna ajalla, jonka auto käyttää yhteen ruutuun.
    public int speedKmh() {
        double seconds = (double) moveEveryTicks * SimTime.SECONDS_PER_TICK;
        double metersPerSecond = CityMap.TILE_LENGTH_METERS / seconds;
        return (int) Math.round(metersPerSecond * 3.6);
    }
    
    public boolean hasArrived() {
        return routeIndex >= route.size();
    }

    // Suunta, johon auto aikoo seuraavaksi ajaa.
    public Direction nextDirection() {
        return route.get(routeIndex);
    }

    // Suunta sitä seuraavan askeleen jälkeen, tai null jos reitti loppuu siihen.
    public Direction directionAfterNext() {
        return routeIndex + 1 < route.size() ? route.get(routeIndex + 1) : null;
    }

    public void moveTo(int x, int y) {
        this.direction = route.get(routeIndex);
        this.x = x;
        this.y = y;
        routeIndex++;
    }

    public int id() { return id; }
    public int x() { return x; }
    public int y() { return y; }
    public Direction direction() { return direction; }
    public int moveEveryTicks() { return moveEveryTicks; }
}