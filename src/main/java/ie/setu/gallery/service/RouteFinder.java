package ie.setu.gallery.service;

import ie.setu.gallery.graph.GalleryGraph;
import ie.setu.gallery.model.Edge;
import ie.setu.gallery.model.Room;
import ie.setu.gallery.model.Route;

import java.util.*;
import java.util.stream.Collectors;

public class RouteFinder {

    private final GalleryGraph graph;

    public RouteFinder(GalleryGraph graph) {
        this.graph = graph;
    }

    // Finds one valid route using DFS
    // This does not guarantee the shortest path
    public Route findAnyValidRoute(String startId, String endId, Set<String> avoidRoomIds) {
        Room start = graph.resolveRoomOrExhibit(startId);
        Room end = graph.resolveRoomOrExhibit(endId);

        if (start == null || end == null) return null;

        List<Room> path = new ArrayList<>();
        Set<Room> visited = new HashSet<>();
        Set<String> avoid = avoidRoomIds == null ? Set.of() : avoidRoomIds;

        boolean found = dfsFindSingle(start, end, visited, path, avoid);
        if (!found) return null;

        return new Route(path, calculateRouteDistance(path));
    }

    // Recursive DFS helper for finding a single valid route
    private boolean dfsFindSingle(Room current, Room destination, Set<Room> visited,
                                  List<Room> path, Set<String> avoid) {
        if (avoid.contains(current.getId())) return false;

        visited.add(current);
        path.add(current);

        if (current.equals(destination)) {
            return true;
        }

        for (Edge edge : graph.getNeighbours(current)) {
            Room next = edge.getTo();

            if (!visited.contains(next) && !avoid.contains(next.getId())) {
                if (dfsFindSingle(next, destination, visited, path, avoid)) {
                    return true;
                }
            }
        }

        // Backtrack if this branch does not reach the destination
        path.remove(path.size() - 1);
        return false;
    }

    // Finds multiple possible routes using DFS
    // maxRoutes is used so the search does not explode if there are too many paths
    public List<Route> findAllRoutesDFS(String startId, String endId, Set<String> avoidRoomIds, int maxRoutes) {
        Room start = graph.resolveRoomOrExhibit(startId);
        Room end = graph.resolveRoomOrExhibit(endId);

        if (start == null || end == null) return List.of();

        List<Route> routes = new ArrayList<>();
        dfsFindAll(start, end, new HashSet<>(), new ArrayList<>(),
                avoidRoomIds == null ? Set.of() : avoidRoomIds, routes, maxRoutes);

        return routes;
    }

    private void dfsFindAll(Room current, Room destination, Set<Room> visited,
                            List<Room> path, Set<String> avoid, List<Route> routes, int maxRoutes) {
        if (routes.size() >= maxRoutes) return;
        if (avoid.contains(current.getId())) return;

        visited.add(current);
        path.add(current);

        if (current.equals(destination)) {
            routes.add(new Route(path, calculateRouteDistance(path)));
        } else {
            for (Edge edge : graph.getNeighbours(current)) {
                Room next = edge.getTo();

                if (!visited.contains(next) && !avoid.contains(next.getId())) {
                    dfsFindAll(next, destination, visited, path, avoid, routes, maxRoutes);
                }
            }
        }

        // Standard DFS backtracking step
        path.remove(path.size() - 1);
        visited.remove(current);
    }

    // Finds the shortest route by total distance using Dijkstra's algorithm
    public Route findShortestRouteDijkstra(String startId, String endId, Set<String> avoidRoomIds) {
        Room start = graph.resolveRoomOrExhibit(startId);
        Room end = graph.resolveRoomOrExhibit(endId);

        if (start == null || end == null) return null;

        Set<String> avoid = avoidRoomIds == null ? Set.of() : avoidRoomIds;

        Map<Room, Double> dist = new HashMap<>();
        Map<Room, Room> prev = new HashMap<>();

        PriorityQueue<RoomDistance> pq =
                new PriorityQueue<>(Comparator.comparingDouble(rd -> rd.distance));

        for (Room room : graph.getAllRooms()) {
            dist.put(room, Double.POSITIVE_INFINITY);
        }

        dist.put(start, 0.0);
        pq.add(new RoomDistance(start, 0.0));

        while (!pq.isEmpty()) {
            RoomDistance current = pq.poll();

            // Ignore outdated queue entries
            if (current.distance > dist.get(current.room)) continue;

            if (current.room.equals(end)) break;

            for (Edge edge : graph.getNeighbours(current.room)) {
                Room next = edge.getTo();

                if (avoid.contains(next.getId())) continue;

                double alt = dist.get(current.room) + edge.getDistance();

                if (alt < dist.get(next)) {
                    dist.put(next, alt);
                    prev.put(next, current.room);
                    pq.add(new RoomDistance(next, alt));
                }
            }
        }

        if (dist.get(end) == Double.POSITIVE_INFINITY) {
            return null;
        }

        List<Room> path = reconstructPath(prev, start, end);
        return new Route(path, dist.get(end));
    }

    // Supports waypoints by solving the route in sections:
    // start -> waypoint1 -> waypoint2 -> ... -> destination
    public Route findShortestRouteWithWaypoints(String startId, String endId,
                                                List<String> waypointIds, Set<String> avoidRoomIds) {
        List<String> stops = new ArrayList<>();
        stops.add(startId);

        if (waypointIds != null) {
            stops.addAll(waypointIds);
        }

        stops.add(endId);

        List<Room> combinedRooms = new ArrayList<>();
        double totalDistance = 0.0;

        for (int i = 0; i < stops.size() - 1; i++) {
            Route leg = findShortestRouteDijkstra(stops.get(i), stops.get(i + 1), avoidRoomIds);

            if (leg == null) {
                return null;
            }

            List<Room> legRooms = leg.getRooms();

            // Avoid duplicating the connecting room between route segments
            if (i > 0) {
                legRooms = legRooms.subList(1, legRooms.size());
            }

            combinedRooms.addAll(legRooms);
            totalDistance += leg.getTotalDistance();
        }

        return new Route(combinedRooms, totalDistance);
    }

    // Calculates total path length by summing distances between consecutive rooms
    public double calculateRouteDistance(List<Room> path) {
        double total = 0.0;

        for (int i = 0; i < path.size() - 1; i++) {
            total += graph.getDistanceBetween(path.get(i), path.get(i + 1));
        }

        return total;
    }

    // Rebuilds the final path by walking backwards from end to start
    private List<Room> reconstructPath(Map<Room, Room> prev, Room start, Room end) {
        LinkedList<Room> path = new LinkedList<>();
        Room current = end;

        while (current != null) {
            path.addFirst(current);
            current = prev.get(current);
        }

        if (!path.isEmpty() && path.getFirst().equals(start)) {
            return path;
        }

        return List.of();
    }

    // Small helper class for the Dijkstra priority queue
    private static class RoomDistance {
        private final Room room;
        private final double distance;

        public RoomDistance(Room room, double distance) {
            this.room = room;
            this.distance = distance;
        }
    }

    // Handy for showing which artists appear along a route
    public List<String> getArtistsOnRoute(Route route) {
        return route.getRooms().stream()
                .flatMap(room -> room.getExhibits().stream())
                .map(exhibit -> exhibit.getArtist())
                .distinct()
                .collect(Collectors.toList());
    }
}