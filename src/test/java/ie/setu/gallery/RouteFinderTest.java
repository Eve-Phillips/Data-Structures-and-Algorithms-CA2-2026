package ie.setu.gallery;

import ie.setu.gallery.graph.GalleryGraph;
import ie.setu.gallery.graph.GraphLoader;
import ie.setu.gallery.model.Route;
import ie.setu.gallery.service.RouteFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;
import java.util.List;

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

    @Test
    void avoidRequiredExhibitShouldMakeRouteImpossible() {
        Route route = routeFinder.findShortestRouteDijkstra("R34", "R22", Set.of("E2"));

        assertNull(route);
    }

    @Test
    void avoidOptionalExhibitShouldStillFindRoute() {
        Route route = routeFinder.findShortestRouteDijkstra("R34", "R22", Set.of("E5"));

        assertNotNull(route);
        assertTrue(route.getRooms().stream().noneMatch(r -> r.getId().equals("R19")));
    }

    @Test
    void waypointExhibitShouldForceRouteThroughThatExhibitsRoom() {
        Route route = routeFinder.findShortestRouteWithWaypoints("R34", "R22", List.of("E5"), Set.of());

        assertNotNull(route);
        assertTrue(route.getRooms().stream().anyMatch(r -> r.getId().equals("R19")));
    }

    @Test
    void dfsWithWaypointShouldPassThroughWaypointRoom() {
        Route route = routeFinder.findAnyValidRouteWithWaypoints("R34", "R22", List.of("R19"), Set.of());

        assertNotNull(route);
        assertTrue(route.getRooms().stream().anyMatch(r -> r.getId().equals("R19")));
    }

    @Test
    void dfsWithExhibitWaypointShouldPassThroughThatExhibitsRoom() {
        Route route = routeFinder.findAnyValidRouteWithWaypoints("R34", "R22", List.of("E3"), Set.of());

        assertNotNull(route);
        assertTrue(route.getRooms().stream().anyMatch(r -> r.getId().equals("R21")));
    }

    @Test
    void dfsMaxRoutesShouldRespectLimit() {
        List<Route> routes = routeFinder.findAllRoutesDFS("R34", "R22", Set.of(), 2);

        assertFalse(routes.isEmpty());
        assertTrue(routes.size() <= 2);
    }

    @Test
    void startAndEndCanBeExhibits() {
        Route route = routeFinder.findShortestRouteDijkstra("E1", "E4", Set.of());

        assertNotNull(route);
        assertEquals("R34", route.getRooms().getFirst().getId());
        assertEquals("R22", route.getRooms().getLast().getId());
    }
}