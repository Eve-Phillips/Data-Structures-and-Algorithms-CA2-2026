package ie.setu.gallery.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Route {
    // Ordered list of rooms in the route
    private final List<Room> rooms;

    // Total route distance
    private final double totalDistance;

    public Route(List<Room> rooms, double totalDistance) {
        this.rooms = new ArrayList<>(rooms);
        this.totalDistance = totalDistance;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    @Override
    public String toString() {
        return rooms.stream()
                .map(Room::getId)
                .collect(Collectors.joining(" -> ")) +
                " | distance = " + totalDistance;
    }
}