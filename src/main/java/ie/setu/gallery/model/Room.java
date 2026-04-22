package ie.setu.gallery.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Room {
    // Unique room id, e.g. R34
    private final String id;

    // Human-readable room name
    private final String name;

    // Approximate coordinates for displaying the room on the map
    private final double mapX;
    private final double mapY;

    // Exhibits stored in this room
    private final List<Exhibit> exhibits = new ArrayList<>();

    public Room(String id, String name, double mapX, double mapY) {
        this.id = id;
        this.name = name;
        this.mapX = mapX;
        this.mapY = mapY;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getMapX() {
        return mapX;
    }

    public double getMapY() {
        return mapY;
    }

    public List<Exhibit> getExhibits() {
        return exhibits;
    }

    // Adds an exhibit to this room
    public void addExhibit(Exhibit exhibit) {
        exhibits.add(exhibit);
    }

    // Rooms are treated as equal if their ids match
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Room room)) return false;
        return Objects.equals(id, room.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id + " - " + name;
    }
}