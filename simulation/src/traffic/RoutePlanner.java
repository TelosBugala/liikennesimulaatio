package traffic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class RoutePlanner {

    private record Tile(int x, int y) {
    }

    private final CityMap map;
    private final Random random;

    public RoutePlanner(CityMap map, Random random) {
        this.map = map;
        this.random = random;
    }

    // Palauttaa lyhimmän reitin suuntalistana, tai null jos reittiä ei ole.
    public List<Direction> plan(int startX, int startY, int goalX, int goalY) {
        boolean[][] visited = new boolean[map.height()][map.width()];
        Direction[][] arrivedBy = new Direction[map.height()][map.width()];
        Queue<Tile> queue = new ArrayDeque<>();

        visited[startY][startX] = true;
        queue.add(new Tile(startX, startY));

        while (!queue.isEmpty()) {
            Tile current = queue.remove();
            if (current.x() == goalX && current.y() == goalY) {
                return buildRoute(arrivedBy, startX, startY, goalX, goalY);
            }

            List<Direction> directions = new ArrayList<>(List.of(Direction.values()));
            Collections.shuffle(directions, random);

            for (Direction direction : directions) {
                if (!map.roadAllows(current.x(), current.y(), direction)) {
                    continue;
                }
                int nextX = current.x() + direction.dx;
                int nextY = current.y() + direction.dy;
                if (!map.isInside(nextX, nextY) || visited[nextY][nextX]) {
                    continue;
                }
                if (!map.roadAllows(nextX, nextY, direction)) {
                    continue;
                }
                visited[nextY][nextX] = true;
                arrivedBy[nextY][nextX] = direction;
                queue.add(new Tile(nextX, nextY));
            }
        }
        return null;
    }

    private List<Direction> buildRoute(Direction[][] arrivedBy,
                                       int startX, int startY,
                                       int goalX, int goalY) {
        List<Direction> route = new ArrayList<>();
        int x = goalX;
        int y = goalY;
        while (x != startX || y != startY) {
            Direction direction = arrivedBy[y][x];
            route.add(direction);
            x -= direction.dx;
            y -= direction.dy;
        }
        Collections.reverse(route);
        return route;
    }

    // Esim. S2 E5 N2 (9 steps)
    public static String describe(List<Direction> route) {
        if (route == null) {
            return "no route";
        }
        StringBuilder text = new StringBuilder();
        int i = 0;
        while (i < route.size()) {
            Direction direction = route.get(i);
            int count = 0;
            while (i < route.size() && route.get(i) == direction) {
                count++;
                i++;
            }
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(direction.name().charAt(0)).append(count);
        }
        text.append(" (").append(route.size()).append(" steps)");
        return text.toString();
    }
}