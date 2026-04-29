package ie.setu.gallery.service;

import ie.setu.gallery.graph.GalleryGraph;
import ie.setu.gallery.model.Edge;
import ie.setu.gallery.model.Room;
import ie.setu.gallery.model.Route;

import java.util.*;

public class InterestingRouteFinder {

    private final GalleryGraph graph;

    public InterestingRouteFinder(GalleryGraph graph) {
        this.graph = graph;
    }

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

    public Route findMostInterestingRoute(String startId, String endId,
                                          Set<String> preferredArtists,
                                          Set<String> avoidRoomIds) {
        Room start = graph.resolveRoomOrExhibit(startId);
        Room end = graph.resolveRoomOrExhibit(endId);

        if (start == null || end == null) return null;

        Set<String> avoid = normaliseToRoomIds(avoidRoomIds);
        Set<String> prefs = preferredArtists == null ? Set.of() : preferredArtists;

        Map<Room, Double> dist = new HashMap<>();
        Map<Room, Room> prev = new HashMap<>();

        PriorityQueue<NodeCost> pq =
                new PriorityQueue<>(Comparator.comparingDouble(n -> n.cost));

        for (Room room : graph.getAllRooms()) {
            dist.put(room, Double.POSITIVE_INFINITY);
        }

        dist.put(start, 0.0);
        pq.add(new NodeCost(start, 0.0));

        while (!pq.isEmpty()) {
            NodeCost current = pq.poll();

            if (current.cost > dist.get(current.room)) continue;
            if (current.room.equals(end)) break;

            for (Edge edge : graph.getNeighbours(current.room)) {
                Room next = edge.getTo();

                if (avoid.contains(next.getId())) continue;

                double bonus = getInterestBonus(next, prefs);
                double adjustedWeight = Math.max(1.0, edge.getDistance() - bonus);

                double alt = dist.get(current.room) + adjustedWeight;

                if (alt < dist.get(next)) {
                    dist.put(next, alt);
                    prev.put(next, current.room);
                    pq.add(new NodeCost(next, alt));
                }
            }
        }

        if (dist.get(end) == Double.POSITIVE_INFINITY) {
            return null;
        }

        LinkedList<Room> path = new LinkedList<>();
        Room current = end;

        while (current != null) {
            path.addFirst(current);
            current = prev.get(current);
        }

        double realDistance = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            realDistance += graph.getDistanceBetween(path.get(i), path.get(i + 1));
        }

        return new Route(path, realDistance);
    }

    public Route findMostInterestingRouteWithWaypoints(String startId, String endId,
                                                       List<String> waypointIds,
                                                       Set<String> preferredArtists,
                                                       Set<String> avoidRoomIds) {
        List<String> stops = new ArrayList<>();
        stops.add(startId);

        if (waypointIds != null) {
            stops.addAll(waypointIds);
        }

        stops.add(endId);

        List<Room> combinedRooms = new ArrayList<>();
        double totalDistance = 0.0;

        for (int i = 0; i < stops.size() - 1; i++) {
            Route leg = findMostInterestingRoute(
                    stops.get(i),
                    stops.get(i + 1),
                    preferredArtists,
                    avoidRoomIds
            );

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

    private double getInterestBonus(Room room, Set<String> preferredArtists) {
        long matches = room.getExhibits().stream()
                .map(exhibit -> exhibit.getArtist().toLowerCase())
                .filter(artist -> preferredArtists.stream()
                        .map(String::toLowerCase)
                        .anyMatch(pref -> pref.equals(artist)))
                .count();

        return matches * 5.0;
    }

    private static class NodeCost {
        Room room;
        double cost;

        NodeCost(Room room, double cost) {
            this.room = room;
            this.cost = cost;
        }
    }
}