package ie.setu.gallery.model;

public class Exhibit {
    // Unique exhibit id, e.g. E1
    private final String id;

    // Exhibit title and artist name
    private final String title;
    private final String artist;

    // The id of the room this exhibit belongs to
    private final String roomId;

    // Optional image path for later use in the UI
    private final String imagePath;

    public Exhibit(String id, String title, String artist, String roomId, String imagePath) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.roomId = roomId;
        this.imagePath = imagePath;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getImagePath() {
        return imagePath;
    }

    @Override
    public String toString() {
        return title + " by " + artist;
    }
}