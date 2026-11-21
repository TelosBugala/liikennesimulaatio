package traffic;

public class CityMap {

    private final String[] rows;

    public CityMap(String[] rows) {
        this.rows = rows;
    }

    public static CityMap createDefault() {
        return new CityMap(new String[] {
            "...|....|...",
            "...|....|...",
            "---+----+---",
            "...|....|...",
            "...|....|...",
            "...|....|...",
            "---+----+---",
            "...|....|...",
            "...|....|..."
        });
    }

    public int width() {
        return rows[0].length();
    }

    public int height() {
        return rows.length;
    }

    public char tileAt(int x, int y) {
        return rows[y].charAt(x);
    }

    public boolean isInside(int x, int y) {
        return x >= 0 && x < width() && y >= 0 && y < height();
    }

    public boolean roadAllows(int x, int y, Direction direction) {
        char tile = tileAt(x, y);
        return tile == '+'
            || (tile == '-' && direction.isHorizontal())
            || (tile == '|' && !direction.isHorizontal());
    }
}