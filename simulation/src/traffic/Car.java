package traffic;

public class Car {

    private final int id;
    private final Direction direction;
    private final int moveEveryTicks;
    private int x;
    private int y;

    public Car(int id, int x, int y, Direction direction, int moveEveryTicks) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.moveEveryTicks = moveEveryTicks;
    }

    public boolean isDue(long tick) {
        return tick % moveEveryTicks == 0;
    }

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int id() { return id; }
    public int x() { return x; }
    public int y() { return y; }
    public Direction direction() { return direction; }
    public int moveEveryTicks() { return moveEveryTicks; }
}