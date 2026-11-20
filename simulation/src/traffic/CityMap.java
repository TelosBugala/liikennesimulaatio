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
}