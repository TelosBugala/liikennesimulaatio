package traffic;

public final class Debug {

    public static final boolean ENABLED = Boolean.getBoolean("traffic.debug");

    private Debug() {
    }

    public static void log(String message) {
        if (ENABLED) {
            System.out.println(message);
        }
    }
}