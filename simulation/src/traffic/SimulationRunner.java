package traffic;

import javax.swing.Timer;

public class SimulationRunner {

    private static final int BASE_TICK_MILLIS = 200;
    private static final int MIN_FRAME_MILLIS = 25;
    private static final double[] SPEEDS = {0.5, 1, 2, 4, 8, 16, 32};

    private final Timer timer;
    private int speedIndex = 1;
    private int stepsPerFrame = 1;
    private boolean paused = false;
    private Runnable frameListener = () -> { };

    public SimulationRunner(Simulation simulation) {
        timer = new Timer(BASE_TICK_MILLIS, e -> {
            for (int i = 0; i < stepsPerFrame; i++) {
                simulation.step();
            }
            frameListener.run();
        });
        applySpeed();
    }

    public void setFrameListener(Runnable frameListener) {
        this.frameListener = frameListener;
    }

    public void start() {
        timer.start();
    }

    public void togglePause() {
        paused = !paused;
        if (paused) {
            timer.stop();
        } else {
            timer.start();
        }
    }

    public void faster() {
        if (canFaster()) {
            speedIndex++;
            applySpeed();
        }
    }

    public void slower() {
        if (canSlower()) {
            speedIndex--;
            applySpeed();
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean canFaster() {
        return speedIndex < SPEEDS.length - 1;
    }

    public boolean canSlower() {
        return speedIndex > 0;
    }

    public String speedLabel() {
        double speed = SPEEDS[speedIndex];
        String number = speed == Math.floor(speed)
            ? String.valueOf((int) speed)
            : String.valueOf(speed);
        return number + "x";
    }

    private void applySpeed() {
        double speed = SPEEDS[speedIndex];
        int delay = (int) Math.max(MIN_FRAME_MILLIS,
                                   Math.round(BASE_TICK_MILLIS / speed));
        stepsPerFrame = (int) Math.max(1,
                                       Math.round(delay * speed / BASE_TICK_MILLIS));
        timer.setDelay(delay);
        timer.setInitialDelay(delay);
    }
}