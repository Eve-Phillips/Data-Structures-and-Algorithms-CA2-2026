package ie.setu.gallery.model;

public class Edge {
    // Start room, destination room, and distance between them
    private final Room from;
    private final Room to;
    private final double distance;

    public Edge(Room from, Room to, double distance) {
        this.from = from;
        this.to = to;
        this.distance = distance;
    }

    public Room getFrom() {
        return from;
    }

    public Room getTo() {
        return to;
    }

    public double getDistance() {
        return distance;
    }
}