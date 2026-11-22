package traffic;

import java.time.LocalDateTime;

public final class SimTime {

    public static final int SECONDS_PER_TICK = 5;
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 5, 6, 0);

    private SimTime() {
    }

    public static LocalDateTime at(long tick) {
        return START.plusSeconds(tick * SECONDS_PER_TICK);
    }
}