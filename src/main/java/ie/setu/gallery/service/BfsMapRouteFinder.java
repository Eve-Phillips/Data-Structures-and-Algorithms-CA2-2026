package ie.setu.gallery.service;

import ie.setu.gallery.model.GridPoint;

import java.util.*;

public class BfsMapRouteFinder {

    // Finds the shortest path in an unweighted 2D grid using BFS.
    // true = walkable
    // false = blocked
    public List<GridPoint> findPath(boolean[][] walkable, GridPoint start, GridPoint end) {
        if (walkable == null || walkable.length == 0 || walkable[0].length == 0) {
            return List.of();
        }

        int height = walkable.length;
        int width = walkable[0].length;

        if (!isInside(start, width, height) || !isInside(end, width, height)) {
            return List.of();
        }

        if (!walkable[start.y()][start.x()] || !walkable[end.y()][end.x()]) {
            return List.of();
        }

        Queue<GridPoint> queue = new LinkedList<>();
        Set<GridPoint> visited = new HashSet<>();
        Map<GridPoint, GridPoint> previous = new HashMap<>();

        queue.add(start);
        visited.add(start);

        // 4-direction movement: up, down, left, right
        int[][] directions = {
                {0, -1},
                {0, 1},
                {-1, 0},
                {1, 0}
        };

        while (!queue.isEmpty()) {
            GridPoint current = queue.poll();

            if (current.equals(end)) {
                return reconstructPath(previous, end);
            }

            for (int[] dir : directions) {
                GridPoint next = new GridPoint(current.x() + dir[0], current.y() + dir[1]);

                if (isInside(next, width, height)
                        && walkable[next.y()][next.x()]
                        && !visited.contains(next)) {
                    visited.add(next);
                    previous.put(next, current);
                    queue.add(next);
                }
            }
        }

        return List.of(); // no path found
    }

    private boolean isInside(GridPoint point, int width, int height) {
        return point.x() >= 0 && point.x() < width
                && point.y() >= 0 && point.y() < height;
    }

    private List<GridPoint> reconstructPath(Map<GridPoint, GridPoint> previous, GridPoint end) {
        LinkedList<GridPoint> path = new LinkedList<>();
        GridPoint current = end;

        while (current != null) {
            path.addFirst(current);
            current = previous.get(current);
        }

        return path;
    }
}