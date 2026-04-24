package ie.setu.gallery.ui;

import ie.setu.gallery.graph.GalleryGraph;
import ie.setu.gallery.graph.GraphLoader;
import ie.setu.gallery.model.Room;
import ie.setu.gallery.model.Route;
import ie.setu.gallery.model.GridPoint;
import ie.setu.gallery.service.RouteFinder;
import ie.setu.gallery.service.BfsMapRouteFinder;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ie.setu.gallery.service.InterestingRouteFinder;

public class GalleryController {

    @FXML private TextField startField;
    @FXML private TextField endField;
    @FXML private TextField avoidField;
    @FXML private TextField waypointField;
    @FXML private TextField artistField;
    @FXML private TextArea outputArea;
    @FXML private Pane mapPane;


    private GalleryGraph graph;
    private RouteFinder routeFinder;
    private InterestingRouteFinder interestingRouteFinder;
    private BfsMapRouteFinder bfsMapRouteFinder;

    private static final int BFS_CELL_SIZE = 5;
    private static final int MAP_WIDTH = 800;
    private static final int MAP_HEIGHT = 500;

    @FXML
    public void initialize() {
        try {
            graph = GraphLoader.loadFromCsv("data/rooms.csv", "data/edges.csv", "data/exhibits.csv");
            routeFinder = new RouteFinder(graph);
            interestingRouteFinder = new InterestingRouteFinder(graph);
            bfsMapRouteFinder = new BfsMapRouteFinder();

            drawTemporaryBfsObstacles();
            drawRoomMarkers();
        } catch (IOException e) {
            outputArea.setText("Failed to load graph data: " + e.getMessage());
        }
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
            outputArea.setText("No route found.");
            return;
        }

        outputArea.setText(route.toString());
        drawRoute(route);
    }

    @FXML
    public void handleFindDfsRoutes() {
        clearRouteLines();

        String start = startField.getText().trim();
        String end = endField.getText().trim();
        Set<String> avoid = parseCsvSet(avoidField.getText());

        List<Route> routes = routeFinder.findAllRoutesDFS(start, end, avoid, 10);

        if (routes.isEmpty()) {
            outputArea.setText("No routes found.");
            return;
        }

        outputArea.setText(
                routes.stream()
                        .map(Route::toString)
                        .collect(Collectors.joining("\n"))
        );

        // For now just draw the first route found
        drawRoute(routes.get(0));
    }

    @FXML
    public void handleFindInterestingRoute() {
        clearRouteLines();

        String start = startField.getText().trim();
        String end = endField.getText().trim();
        Set<String> avoid = parseCsvSet(avoidField.getText());
        Set<String> preferredArtists = parseCsvSet(artistField.getText());

        Route route = interestingRouteFinder.findMostInterestingRoute(
                start, end, preferredArtists, avoid
        );

        if (route == null) {
            outputArea.setText("No interesting route found.");
            return;
        }

        outputArea.setText(
                "Most Interesting Route:\n" +
                        route + "\n\n" +
                        "Artists on route: " +
                        String.join(", ", routeFinder.getArtistsOnRoute(route))
        );

        drawRoute(route);
    }

    @FXML
    public void handleFindBfsMapRoute() {
        clearRouteLines();

        String startId = startField.getText().trim();
        String endId = endField.getText().trim();

        Room startRoom = graph.resolveRoomOrExhibit(startId);
        Room endRoom = graph.resolveRoomOrExhibit(endId);

        if (startRoom == null || endRoom == null) {
            outputArea.setText("Invalid BFS start or end. Use a valid room id or exhibit id.");
            return;
        }

        boolean[][] walkable = createTemporaryWalkableGrid();

        GridPoint start = toGridPoint(startRoom);
        GridPoint end = toGridPoint(endRoom);

        var path = bfsMapRouteFinder.findPath(walkable, start, end);

        if (path.isEmpty()) {
            outputArea.setText("No BFS map route found.");
            return;
        }

        drawBfsPath(path);

        outputArea.setText(
                "BFS Map Route (temporary demo)\n" +
                        "Start: " + startRoom.getId() + "\n" +
                        "End: " + endRoom.getId() + "\n" +
                        "Steps: " + (path.size() - 1) + "\n\n" +
                        "Note: this currently uses a temporary hardcoded grid.\n" +
                        "We will replace the real image-based map later."
        );
    }

    // Parses comma-separated input into a Set
    private Set<String> parseCsvSet(String input) {
        if (input == null || input.isBlank()) return Set.of();

        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    // Parses comma-separated input into a List
    private List<String> parseCsvList(String input) {
        if (input == null || input.isBlank()) return List.of();

        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    // Draws a simple marker for each room on the map pane
    private void drawRoomMarkers() {
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

    // Draws straight lines between rooms in the chosen route
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

    // Removes old route lines before drawing a new route
    private void clearRouteLines() {
        mapPane.getChildren().removeIf(node -> "routeLine".equals(node.getUserData()));
    }

    private boolean[][] createTemporaryWalkableGrid() {
        int cols = MAP_WIDTH / BFS_CELL_SIZE;
        int rows = MAP_HEIGHT / BFS_CELL_SIZE;

        boolean[][] walkable = new boolean[rows][cols];

        for (int y = 0; y < rows; y++) {
            Arrays.fill(walkable[y], true);
        }

        // Temporary walls for demo purposes
        blockRectangle(walkable, 300, 120, 20, 220);
        blockRectangle(walkable, 390, 80, 20, 140);

        return walkable;
    }

    private void blockRectangle(boolean[][] walkable, int pixelX, int pixelY, int pixelWidth, int pixelHeight) {
        int startCol = pixelX / BFS_CELL_SIZE;
        int endCol = (pixelX + pixelWidth) / BFS_CELL_SIZE;
        int startRow = pixelY / BFS_CELL_SIZE;
        int endRow = (pixelY + pixelHeight) / BFS_CELL_SIZE;

        for (int row = startRow; row < endRow && row < walkable.length; row++) {
            for (int col = startCol; col < endCol && col < walkable[0].length; col++) {
                if (row >= 0 && col >= 0) {
                    walkable[row][col] = false;
                }
            }
        }
    }

    private void drawTemporaryBfsObstacles() {
        Rectangle wall1 = new Rectangle(300, 120, 20, 220);
        wall1.setFill(Color.LIGHTGRAY);
        wall1.setStroke(Color.BLACK);
        wall1.setUserData("bfsObstacle");

        Rectangle wall2 = new Rectangle(390, 80, 20, 140);
        wall2.setFill(Color.LIGHTGRAY);
        wall2.setStroke(Color.BLACK);
        wall2.setUserData("bfsObstacle");

        mapPane.getChildren().addAll(wall1, wall2);
    }

    private GridPoint toGridPoint(Room room) {
        int gridX = (int) (room.getMapX() / BFS_CELL_SIZE);
        int gridY = (int) (room.getMapY() / BFS_CELL_SIZE);
        return new GridPoint(gridX, gridY);
    }

    private void drawBfsPath(List<GridPoint> path) {
        Polyline polyline = new Polyline();

        for (GridPoint point : path) {
            double x = point.x() * BFS_CELL_SIZE + (BFS_CELL_SIZE / 2.0);
            double y = point.y() * BFS_CELL_SIZE + (BFS_CELL_SIZE / 2.0);

            polyline.getPoints().addAll(x, y);
        }

        polyline.setStroke(Color.BLUE);
        polyline.setStrokeWidth(2);
        polyline.setUserData("routeLine");

        mapPane.getChildren().add(polyline);
    }
}

