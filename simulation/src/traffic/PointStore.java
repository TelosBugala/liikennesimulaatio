package traffic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class PointStore {

    public record Loaded(List<MeasurementPoint> points, int nextId) {
    }

    private static final String NEXT_ID_PREFIX = "nextId=";

    private final Path file;

    public PointStore(Path file) {
        this.file = file;
    }

    public Path file() {
        return file;
    }

    // Palauttaa null, jos tiedostoa ei ole.
    public Loaded load() throws IOException {
        if (!Files.exists(file)) {
            return null;
        }
        List<MeasurementPoint> points = new ArrayList<>();
        int nextId = 1;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            if (line.startsWith(NEXT_ID_PREFIX)) {
                nextId = Integer.parseInt(line.substring(NEXT_ID_PREFIX.length()).strip());
            } else {
                points.add(parsePoint(line));
            }
        }
        return new Loaded(points, nextId);
    }

    public void save(List<MeasurementPoint> points, int nextId) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(NEXT_ID_PREFIX + nextId);
        for (MeasurementPoint point : points) {
            lines.add(point.id() + ";" + point.x() + ";" + point.y() + ";" + point.name());
        }
        // Kirjoitetaan ensin väliaikaiseen tiedostoon ja siirretään sitten paikalleen,
        // jotta kesken kirjoituksen tapahtuva kaatuminen ei riko vanhaa tiedostoa.
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(temp, lines, StandardCharsets.UTF_8);
        Files.move(temp, file,
                   StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    // Rivin muoto: tunniste;x;y;nimi
    private MeasurementPoint parsePoint(String line) {
        String[] parts = line.split(";", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid line: " + line);
        }
        return new MeasurementPoint(
            Integer.parseInt(parts[0].strip()),
            parts[3].strip(),
            Integer.parseInt(parts[1].strip()),
            Integer.parseInt(parts[2].strip()));
    }
}