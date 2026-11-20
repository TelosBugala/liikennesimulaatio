package traffic;

public class Car {

    private final int id;
    private final Direction direction;
    private int x;
    private int y;

    public Car(int id, int x, int y, Direction direction) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
    }

    public void advance() {
        x += direction.dx;
        y += direction.dy;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int id() { return id; }
    public int x() { return x; }
    public int y() { return y; }
    public Direction direction() { return direction; }
}