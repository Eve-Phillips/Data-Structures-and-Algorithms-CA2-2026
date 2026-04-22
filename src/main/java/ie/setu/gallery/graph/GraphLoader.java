package ie.setu.gallery.graph;

import ie.setu.gallery.model.Exhibit;
import ie.setu.gallery.model.Room;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GraphLoader {

    // Loads graph data from three CSV files:
    // 1. rooms
    // 2. edges
    // 3. exhibits
    public static GalleryGraph loadFromCsv(String roomsFile, String edgesFile, String exhibitsFile) throws IOException {
        GalleryGraph graph = new GalleryGraph();

        loadRooms(graph, Path.of(roomsFile));
        loadEdges(graph, Path.of(edgesFile));
        loadExhibits(graph, Path.of(exhibitsFile));

        return graph;
    }

    private static void loadRooms(GalleryGraph graph, Path path) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line = br.readLine(); // skip header row

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                String id = parts[0].trim();
                String name = parts[1].trim();
                double x = Double.parseDouble(parts[2].trim());
                double y = Double.parseDouble(parts[3].trim());

                graph.addRoom(new Room(id, name, x, y));
            }
        }
    }

    private static void loadEdges(GalleryGraph graph, Path path) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line = br.readLine(); // skip header row

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                String from = parts[0].trim();
                String to = parts[1].trim();
                double distance = Double.parseDouble(parts[2].trim());

                graph.addUndirectedEdge(from, to, distance);
            }
        }
    }

    private static void loadExhibits(GalleryGraph graph, Path path) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line = br.readLine(); // skip header row

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                String id = parts[0].trim();
                String title = parts[1].trim();
                String artist = parts[2].trim();
                String roomId = parts[3].trim();
                String imagePath = parts.length > 4 ? parts[4].trim() : "";

                graph.addExhibitToRoom(new Exhibit(id, title, artist, roomId, imagePath));
            }
        }
    }
}
