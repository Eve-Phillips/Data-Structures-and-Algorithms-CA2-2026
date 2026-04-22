package ie.setu.gallery;

import ie.setu.gallery.graph.GalleryGraph;
import ie.setu.gallery.graph.GraphLoader;
import ie.setu.gallery.model.Route;
import ie.setu.gallery.service.RouteFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RouteFinderTest {

    private RouteFinder routeFinder;

    @BeforeEach
    void setUp() throws IOException {
        GalleryGraph graph = GraphLoader.loadFromCsv(
                "data/rooms.csv",
                "data/edges.csv",
                "data/exhibits.csv"
        );

        routeFinder = new RouteFinder(graph);
    }

    @Test
    void dijkstraShouldFindRoute() {
        Route route = routeFinder.findShortestRouteDijkstra("R34", "R22", Set.of());

        assertNotNull(route);
        assertFalse(route.getRooms().isEmpty());
    }

    @Test
    void avoidRoomShouldAffectRoute() {
        Route route = routeFinder.findShortestRouteDijkstra("R34", "R22", Set.of("R20"));

        if (route != null) {
            assertTrue(route.getRooms().stream().noneMatch(r -> r.getId().equals("R20")));
        }
    }

    @Test
    void dfsShouldFindAtLeastOneRoute() {
        assertFalse(routeFinder.findAllRoutesDFS("R34", "R22", Set.of(), 10).isEmpty());
    }
}