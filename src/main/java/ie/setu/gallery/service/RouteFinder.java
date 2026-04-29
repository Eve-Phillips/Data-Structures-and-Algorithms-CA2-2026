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

    // Converts a set of room/exhibit ids into room ids only.
    private Set<String> normaliseToRoomIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) return Set.of();

        Set<String> normalised = new HashSet<>();

        for (String id : ids) {
            Room room = graph.resolveRoomOrExhibit(id);
            if (room != null) {
                normalised.add(room.getId());
            }
        }

        return normalised;
    }

    // Finds one valid route using DFS
    // This does not guarantee the shortest path
    public Route findAnyValidRoute(String startId, String endId, Set<String> avoidRoomIds) {
        Room start = graph.resolveRoomOrExhibit(startId);
        Room end = graph.resolveRoomOrExhibit(endId);

        if (start == null || end == null) return null;

        List<Room> path = new ArrayList<>();
        Set<Room> visited = new HashSet<>();
        Set<String> avoid = normaliseToRoomIds(avoidRoomIds);

        boolean found = dfsFindSingle(start, end, visited, path, avoid);
        if (!found) return null;

        return new Route(path, calculateRouteDistance(path));
    }

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

        path.remove(path.size() - 1);
        return false;
    }

    // Finds multiple possible routes using DFS
    public List<Route> findAllRoutesDFS(String startId, String endId, Set<String> avoidRoomIds, int maxRoutes) {
        Room start = graph.resolveRoomOrExhibit(startId);
        Room end = graph.resolveRoomOrExhibit(endId);

        if (start == null || end == null) return List.of();

        List<Route> routes = new ArrayList<>();
        Set<String> avoid = normaliseToRoomIds(avoidRoomIds);

        dfsFindAll(start, end, new HashSet<>(), new ArrayList<>(), avoid, routes, maxRoutes);

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

        path.remove(path.size() - 1);
        visited.remove(current);
    }

    public List<Route> findAllRoutesDFSWithWaypoints(String startId, String endId,
                                                     List<String> waypointIds,
                                                     Set<String> avoidRoomIds,
                                                     int maxRoutes) {
        List<String> stops = new ArrayList<>();
        stops.add(startId);

        if (waypointIds != null) {
            stops.addAll(waypointIds);
        }

        stops.add(endId);

        List<List<Route>> legRouteLists = new ArrayList<>();

        for (int i = 0; i < stops.size() - 1; i++) {
            List<Route> legRoutes = findAllRoutesDFS(stops.get(i), stops.get(i + 1), avoidRoomIds, maxRoutes);

            if (legRoutes.isEmpty()) {
                return List.of();
            }

            legRouteLists.add(legRoutes);
        }

        List<Route> results = new ArrayList<>();
        combineWaypointLegRoutes(legRouteLists, 0, new ArrayList<>(), 0.0, results, maxRoutes);
        return results;
    }

    // DFS with waypoints
    public Route findAnyValidRouteWithWaypoints(String startId, String endId,
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
            Route leg = findAnyValidRoute(stops.get(i), stops.get(i + 1), avoidRoomIds);

            if (leg == null) {
                return null;
            }

            List<Room> legRooms = leg.getRooms();

            if (i > 0) {
                legRooms = legRooms.subList(1, legRooms.size());
            }

            combinedRooms.addAll(legRooms);
            totalDistance += leg.getTotalDistance();
        }

        return new Route(combinedRooms, totalDistance);
    }

    // Finds the shortest route by total distance using Dijkstra's algorithm
    public Route findShortestRouteDijkstra(String startId, String endId, Set<String> avoidRoomIds) {
        Room start = graph.resolveRoomOrExhibit(startId);
        Room end = graph.resolveRoomOrExhibit(endId);

        if (start == null || end == null) return null;

        Set<String> avoid = normaliseToRoomIds(avoidRoomIds);

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

            if (i > 0) {
                legRooms = legRooms.subList(1, legRooms.size());
            }

            combinedRooms.addAll(legRooms);
            totalDistance += leg.getTotalDistance();
        }

        return new Route(combinedRooms, totalDistance);
    }

    public double calculateRouteDistance(List<Room> path) {
        double total = 0.0;

        for (int i = 0; i < path.size() - 1; i++) {
            total += graph.getDistanceBetween(path.get(i), path.get(i + 1));
        }

        return total;
    }

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

    private static class RoomDistance {
        private final Room room;
        private final double distance;

        public RoomDistance(Room room, double distance) {
            this.room = room;
            this.distance = distance;
        }
    }

    public List<String> getArtistsOnRoute(Route route) {
        return route.getRooms().stream()
                .flatMap(room -> room.getExhibits().stream())
                .map(exhibit -> exhibit.getArtist())
                .distinct()
                .collect(Collectors.toList());
    }

    private void combineWaypointLegRoutes(List<List<Route>> legRouteLists,
                                          int legIndex,
                                          List<Room> currentRooms,
                                          double currentDistance,
                                          List<Route> results,
                                          int maxRoutes) {
        if (results.size() >= maxRoutes) {
            return;
        }

        if (legIndex == legRouteLists.size()) {
            results.add(new Route(currentRooms, currentDistance));
            return;
        }

        for (Route legRoute : legRouteLists.get(legIndex)) {
            if (results.size() >= maxRoutes) {
                return;
            }

            List<Room> nextRooms = new ArrayList<>(currentRooms);
            List<Room> legRooms = new ArrayList<>(legRoute.getRooms());

            if (legIndex > 0 && !legRooms.isEmpty()) {
                legRooms = legRooms.subList(1, legRooms.size());
            }

            nextRooms.addAll(legRooms);

            combineWaypointLegRoutes(
                    legRouteLists,
                    legIndex + 1,
                    nextRooms,
                    currentDistance + legRoute.getTotalDistance(),
                    results,
                    maxRoutes
            );
        }
    }
}