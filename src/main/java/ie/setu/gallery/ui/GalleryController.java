package ie.setu.gallery.ui;

import ie.setu.gallery.graph.GalleryGraph;
import ie.setu.gallery.graph.GraphLoader;
import ie.setu.gallery.model.GridPoint;
import ie.setu.gallery.model.Room;
import ie.setu.gallery.model.Route;
import ie.setu.gallery.service.BfsMapRouteFinder;
import ie.setu.gallery.service.InterestingRouteFinder;
import ie.setu.gallery.service.RouteFinder;
import ie.setu.gallery.service.WalkableMapLoader;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GalleryController {

    @FXML private TextField startField;
    @FXML private TextField endField;
    @FXML private TextField avoidField;
    @FXML private TextField waypointField;
    @FXML private TextField artistField;
    @FXML private TextField maxRoutesField;
    @FXML private TextArea outputArea;

    // The ImageView shows the actual gallery map.
    @FXML private ImageView mapImageView;

    // The pane sits over the map and is used for markers/routes.
    @FXML private Pane mapPane;

    private GalleryGraph graph;
    private RouteFinder routeFinder;
    private InterestingRouteFinder interestingRouteFinder;
    private BfsMapRouteFinder bfsMapRouteFinder;

    private Image mapImage;
    private Image walkableImage;
    private boolean[][] walkableGrid;

    private static final int MAP_WIDTH = 800;
    private static final int MAP_HEIGHT = 500;

    @FXML
    public void initialize() {
        try {
            graph = GraphLoader.loadFromCsv("data/rooms.csv", "data/edges.csv", "data/exhibits.csv");

            routeFinder = new RouteFinder(graph);
            interestingRouteFinder = new InterestingRouteFinder(graph);
            bfsMapRouteFinder = new BfsMapRouteFinder();

            loadMapImages();
            drawRoomMarkers();
            setUpMapCoordinateClick();

        } catch (IOException e) {
            outputArea.setText("Failed to load graph data: " + e.getMessage());
        } catch (Exception e) {
            outputArea.setText("Failed to initialise map: " + e.getMessage());
        }
    }

    private void loadMapImages() {
        URL mapUrl = getClass().getResource("/ie/setu/gallery/gallery-map.png");
        URL walkableUrl = getClass().getResource("/ie/setu/gallery/gallery-walkable.png");

        if (mapUrl == null) {
            throw new IllegalStateException("Could not find gallery-map.png in resources.");
        }

        if (walkableUrl == null) {
            throw new IllegalStateException("Could not find gallery-walkable.png in resources.");
        }

        mapImage = new Image(mapUrl.toExternalForm());
        walkableImage = new Image(walkableUrl.toExternalForm());

        mapImageView.setImage(mapImage);
        mapImageView.setFitWidth(MAP_WIDTH);
        mapImageView.setFitHeight(MAP_HEIGHT);
        mapImageView.setPreserveRatio(false);

        WalkableMapLoader loader = new WalkableMapLoader();
        walkableGrid = loader.loadWalkableGrid(walkableImage);
    }

    // Temporary helper for calibrating room coordinates in rooms.csv.
    // Click the map, then copy the printed x/y values into the CSV.
    private void setUpMapCoordinateClick() {
        mapPane.setOnMouseClicked(event -> {
            long x = Math.round(event.getX());
            long y = Math.round(event.getY());

            outputArea.setText(
                    "Clicked map coordinate:\n" +
                            "x = " + x + "\n" +
                            "y = " + y + "\n\n" +
                            "Use these values in rooms.csv as mapX,mapY."
            );
        });
    }

    @FXML
    public void handleFindShortestRoute() {
        clearRouteLines();

        String start = startField.getText().trim();
        String end = endField.getText().trim();
        Set<String> avoid = parseCsvSet(avoidField.getText());
        List<String> waypoints = parseCsvList(waypointField.getText());

        Route route;

        if (waypoints.isEmpty()) {
            route = routeFinder.findShortestRouteDijkstra(start, end, avoid);
        } else {
            route = routeFinder.findShortestRouteWithWaypoints(start, end, waypoints, avoid);
        }

        if (route == null) {
            outputArea.setText("No shortest route found.");
            return;
        }

        outputArea.setText(formatSingleRouteOutput("Shortest Route (Dijkstra)", route));
        drawRoute(route);
    }

    @FXML
    public void handleFindDfsRoutes() {
        clearRouteLines();

        String start = startField.getText().trim();
        String end = endField.getText().trim();
        Set<String> avoid = parseCsvSet(avoidField.getText());
        List<String> waypoints = parseCsvList(waypointField.getText());

        int maxRoutes = parseMaxRoutes();

        List<Route> routes;

        if (waypoints.isEmpty()) {
            routes = routeFinder.findAllRoutesDFS(start, end, avoid, maxRoutes);
        } else {
            routes = routeFinder.findAllRoutesDFSWithWaypoints(start, end, waypoints, avoid, maxRoutes);
        }

        if (routes.isEmpty()) {
            outputArea.setText("No DFS routes found.");
            return;
        }

        outputArea.setText(formatMultipleRoutesOutput("DFS Route Permutations", routes));
        drawRoute(routes.get(0));
    }

    @FXML
    public void handleFindInterestingRoute() {
        clearRouteLines();

        String start = startField.getText().trim();
        String end = endField.getText().trim();
        Set<String> avoid = parseCsvSet(avoidField.getText());
        Set<String> preferredArtists = parseCsvSet(artistField.getText());
        List<String> waypoints = parseCsvList(waypointField.getText());

        Route route;

        if (waypoints.isEmpty()) {
            route = interestingRouteFinder.findMostInterestingRoute(start, end, preferredArtists, avoid);
        } else {
            route = interestingRouteFinder.findMostInterestingRouteWithWaypoints(
                    start, end, waypoints, preferredArtists, avoid
            );
        }

        if (route == null) {
            outputArea.setText("No interesting route found.");
            return;
        }

        outputArea.setText(formatSingleRouteOutput("Most Interesting Route", route));
        drawRoute(route);
    }

    @FXML
    public void handleFindBfsMapRoute() {
        clearRouteLines();

        if (walkableGrid == null) {
            outputArea.setText("Walkable map has not loaded.");
            return;
        }

        String startId = startField.getText().trim();
        String endId = endField.getText().trim();

        Room startRoom = graph.resolveRoomOrExhibit(startId);
        Room endRoom = graph.resolveRoomOrExhibit(endId);

        if (startRoom == null || endRoom == null) {
            outputArea.setText("Invalid BFS start or end. Use a valid room id or exhibit id.");
            return;
        }

        GridPoint start = toGridPoint(startRoom);
        GridPoint end = toGridPoint(endRoom);

        // If the marker lands on a black wall/line, try to nudge it to nearby white pixels.
        start = findNearestWalkablePoint(start, 10);
        end = findNearestWalkablePoint(end, 10);

        if (start == null || end == null) {
            outputArea.setText("Start or end point is not on a walkable part of the map.");
            return;
        }

        List<GridPoint> path = bfsMapRouteFinder.findPath(walkableGrid, start, end);

        if (path.isEmpty()) {
            outputArea.setText("No BFS map route found. The walkable map may have blocked gaps.");
            return;
        }

        drawBfsPath(path);

        outputArea.setText(
                "BFS Map Route\n" +
                        "Start: " + startRoom.getId() + "\n" +
                        "End: " + endRoom.getId() + "\n" +
                        "Pixel steps: " + (path.size() - 1) + "\n\n" +
                        "This route uses the black/white walkable map."
        );
    }

    @FXML
    public void handleFindAnyValidRoute() {
        clearRouteLines();

        String start = startField.getText().trim();
        String end = endField.getText().trim();
        Set<String> avoid = parseCsvSet(avoidField.getText());
        List<String> waypoints = parseCsvList(waypointField.getText());

        Route route;

        if (waypoints.isEmpty()) {
            route = routeFinder.findAnyValidRoute(start, end, avoid);
        } else {
            route = routeFinder.findAnyValidRouteWithWaypoints(start, end, waypoints, avoid);
        }

        if (route == null) {
            outputArea.setText("No valid route found.");
            return;
        }

        outputArea.setText(formatSingleRouteOutput("Any Valid Route", route));
        drawRoute(route);
    }

    private Set<String> parseCsvSet(String input) {
        if (input == null || input.isBlank()) return Set.of();

        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    private List<String> parseCsvList(String input) {
        if (input == null || input.isBlank()) return List.of();

        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private void drawRoomMarkers() {
        mapPane.getChildren().removeIf(node -> "marker".equals(node.getUserData()));

        for (Room room : graph.getAllRooms()) {
            Circle circle = new Circle(room.getMapX(), room.getMapY(), 6, Color.DARKBLUE);
            circle.setUserData("marker");

            Text label = new Text(room.getMapX() + 8, room.getMapY() - 8, room.getId());
            label.setUserData("marker");
            label.setFill(Color.BLACK);
            label.setStyle("-fx-font-size: 12px;");

            mapPane.getChildren().addAll(circle, label);
        }
    }

    private void drawRoute(Route route) {
        List<Room> rooms = route.getRooms();

        for (int i = 0; i < rooms.size() - 1; i++) {
            Room a = rooms.get(i);
            Room b = rooms.get(i + 1);

            Line line = new Line(a.getMapX(), a.getMapY(), b.getMapX(), b.getMapY());
            line.setStroke(Color.RED);
            line.setStrokeWidth(3);
            line.setUserData("routeLine");

            mapPane.getChildren().add(line);
        }
    }

    private void clearRouteLines() {
        mapPane.getChildren().removeIf(node -> "routeLine".equals(node.getUserData()));
    }

    private GridPoint toGridPoint(Room room) {
        int imageWidth = (int) walkableImage.getWidth();
        int imageHeight = (int) walkableImage.getHeight();

        double scaleX = imageWidth / (double) MAP_WIDTH;
        double scaleY = imageHeight / (double) MAP_HEIGHT;

        int gridX = (int) Math.round(room.getMapX() * scaleX);
        int gridY = (int) Math.round(room.getMapY() * scaleY);

        return new GridPoint(gridX, gridY);
    }

    private GridPoint findNearestWalkablePoint(GridPoint point, int searchRadius) {
        if (isWalkable(point)) {
            return point;
        }

        for (int radius = 1; radius <= searchRadius; radius++) {
            for (int y = point.y() - radius; y <= point.y() + radius; y++) {
                for (int x = point.x() - radius; x <= point.x() + radius; x++) {
                    GridPoint nearby = new GridPoint(x, y);

                    if (isWalkable(nearby)) {
                        return nearby;
                    }
                }
            }
        }

        return null;
    }

    private boolean isWalkable(GridPoint point) {
        return point.y() >= 0
                && point.y() < walkableGrid.length
                && point.x() >= 0
                && point.x() < walkableGrid[0].length
                && walkableGrid[point.y()][point.x()];
    }

    private void drawBfsPath(List<GridPoint> path) {
        Polyline polyline = new Polyline();

        int imageWidth = (int) walkableImage.getWidth();
        int imageHeight = (int) walkableImage.getHeight();

        double scaleX = MAP_WIDTH / (double) imageWidth;
        double scaleY = MAP_HEIGHT / (double) imageHeight;

        for (GridPoint point : path) {
            double x = point.x() * scaleX;
            double y = point.y() * scaleY;

            polyline.getPoints().addAll(x, y);
        }

        polyline.setStroke(Color.BLUE);
        polyline.setStrokeWidth(2);
        polyline.setUserData("routeLine");

        mapPane.getChildren().add(polyline);
    }

    private int parseMaxRoutes() {
        if (maxRoutesField == null || maxRoutesField.getText().isBlank()) {
            return 10;
        }

        try {
            int value = Integer.parseInt(maxRoutesField.getText().trim());
            return Math.max(1, value);
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    private String formatSingleRouteOutput(String title, Route route) {
        String pathText = route.getRooms().stream()
                .map(room -> room.getId() + " (" + room.getName() + ")")
                .collect(Collectors.joining(" -> "));

        String artistsText = routeFinder.getArtistsOnRoute(route).isEmpty()
                ? "None"
                : String.join(", ", routeFinder.getArtistsOnRoute(route));

        String exhibitsText = route.getRooms().stream()
                .flatMap(room -> room.getExhibits().stream())
                .map(exhibit -> exhibit.getId() + " - " + exhibit.getTitle() + " by " + exhibit.getArtist())
                .distinct()
                .collect(Collectors.joining("\n"));

        if (exhibitsText.isBlank()) {
            exhibitsText = "None";
        }

        return title + "\n" +
                "Distance: " + String.format("%.1f", route.getTotalDistance()) + "\n" +
                "Rooms visited: " + route.getRooms().size() + "\n\n" +
                "Path:\n" + pathText + "\n\n" +
                "Artists on route:\n" + artistsText + "\n\n" +
                "Exhibits encountered:\n" + exhibitsText;
    }

    private String formatMultipleRoutesOutput(String title, List<Route> routes) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        sb.append("Routes found: ").append(routes.size()).append("\n\n");

        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);

            String pathText = route.getRooms().stream()
                    .map(Room::getId)
                    .collect(Collectors.joining(" -> "));

            sb.append(i + 1)
                    .append(". ")
                    .append(pathText)
                    .append(" | distance = ")
                    .append(String.format("%.1f", route.getTotalDistance()))
                    .append("\n");
        }

        return sb.toString();
    }
}