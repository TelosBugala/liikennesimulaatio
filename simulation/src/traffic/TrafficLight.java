package traffic;

public class TrafficLight {

    private static final int GREEN_TICKS = 20;
    private static final int ALL_RED_TICKS = 4;
    private static final int CYCLE_TICKS = 2 * (GREEN_TICKS + ALL_RED_TICKS);

    private final int x;
    private final int y;

    public TrafficLight(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean allows(Direction direction, long tick) {
        long t = tick % CYCLE_TICKS;
        if (direction.isHorizontal()) {
            return t < GREEN_TICKS;
        }
        long verticalStart = GREEN_TICKS + ALL_RED_TICKS;
        return t >= verticalStart && t < verticalStart + GREEN_TICKS;
    }

    public int x() { return x; }
    public int y() { return y; }
}