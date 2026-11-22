package traffic;

public record MeasurementPoint(int id, String name, int x, int y) {

    public static final int MAX_NAME_LENGTH = 30;

    public MeasurementPoint {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Invalid point name: " + name);
        }
    }

    public static boolean isValidName(String name) {
        return name != null
            && !name.isBlank()
            && name.equals(name.strip())
            && name.length() <= MAX_NAME_LENGTH
            && name.matches("[\\p{L}\\p{N} -]+");
    }
}