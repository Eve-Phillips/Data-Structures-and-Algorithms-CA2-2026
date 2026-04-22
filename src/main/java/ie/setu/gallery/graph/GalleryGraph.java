package ie.setu.gallery.graph;

import ie.setu.gallery.model.Edge;
import ie.setu.gallery.model.Exhibit;
import ie.setu.gallery.model.Room;

import java.util.*;

public class GalleryGraph {

    // Stores rooms by their id for quick lookup
    private final Map<String, Room> roomsById = new HashMap<>();

    // Adjacency list representation of the graph
    // Each room maps to a list of edges leaving that room
    private final Map<Room, List<Edge>> adjacencyList = new HashMap<>();

    // Lets us resolve an exhibit id to the room it belongs to
    private final Map<String, Room> exhibitToRoom = new HashMap<>();

    public void addRoom(Room room) {
        roomsById.put(room.getId(), room);
        adjacencyList.putIfAbsent(room, new ArrayList<>());
    }

    // Adds a connection in both directions since movement between rooms
    // should normally be possible both ways
    public void addUndirectedEdge(String roomId1, String roomId2, double distance) {
        Room r1 = roomsById.get(roomId1);
        Room r2 = roomsById.get(roomId2);

        if (r1 == null || r2 == null) {
            throw new IllegalArgumentException("Edge references unknown room(s): " + roomId1 + ", " + roomId2);
        }

        adjacencyList.get(r1).add(new Edge(r1, r2, distance));
        adjacencyList.get(r2).add(new Edge(r2, r1, distance));
    }

    // Attaches an exhibit to a room and records the exhibit -> room lookup
    public void addExhibitToRoom(Exhibit exhibit) {
        Room room = roomsById.get(exhibit.getRoomId());
        if (room == null) {
            throw new IllegalArgumentException("Unknown room for exhibit: " + exhibit.getRoomId());
        }

        room.addExhibit(exhibit);
        exhibitToRoom.put(exhibit.getId(), room);
    }

    public Room getRoomById(String roomId) {
        return roomsById.get(roomId);
    }

    public Collection<Room> getAllRooms() {
        return roomsById.values();
    }

    public List<Edge> getNeighbours(Room room) {
        return adjacencyList.getOrDefault(room, Collections.emptyList());
    }

    // Accepts either a room id or exhibit id
    // If it is a room, return that room
    // If it is an exhibit, return the room containing it
    public Room resolveRoomOrExhibit(String id) {
        if (roomsById.containsKey(id)) {
            return roomsById.get(id);
        }
        return exhibitToRoom.get(id);
    }

    // Returns the direct distance between two connected rooms
    public double getDistanceBetween(Room a, Room b) {
        for (Edge edge : getNeighbours(a)) {
            if (edge.getTo().equals(b)) {
                return edge.getDistance();
            }
        }
        return Double.POSITIVE_INFINITY;
    }
}