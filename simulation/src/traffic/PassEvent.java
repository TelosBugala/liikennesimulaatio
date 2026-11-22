package traffic;

import java.time.LocalDateTime;

public record PassEvent(long tick, LocalDateTime simTime,
                        int pointId, int carId, Direction direction) {
}