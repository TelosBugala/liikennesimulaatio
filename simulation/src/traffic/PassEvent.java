package traffic;

public record PassEvent(long tick, int pointId, int carId, Direction direction) {
}