package traffic;

import java.time.LocalDateTime;

public record PassEvent(long tick, LocalDateTime simTime,
                        int pointId, String pointName,
                        int carId, Direction direction, int speedKmh) {
}