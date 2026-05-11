package ie.setu.gallery;

import ie.setu.gallery.graph.GalleryGraph;
import ie.setu.gallery.graph.GraphLoader;
import ie.setu.gallery.model.Route;
import ie.setu.gallery.service.InterestingRouteFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class InterestingRouteFinderTest {

    private InterestingRouteFinder interestingRouteFinder;

    @BeforeEach
    void setUp() throws IOException {
        GalleryGraph graph = GraphLoader.loadFromCsv(
                "data/rooms.csv",
                "data/edges.csv",
                "data/exhibits.csv"
        );

        interestingRouteFinder = new InterestingRouteFinder(graph);
    }

    @Test
    void interestingRouteShouldFindRoute() {
        Route route = interestingRouteFinder.findMostInterestingRoute(
                "R34", "R22", Set.of("Turner", "Botticelli"), Set.of()
        );

        assertNotNull(route);
        assertFalse(route.getRooms().isEmpty());
    }

    @Test
    void interestingRouteShouldReturnNullIfRequiredRoomIsAvoided() {
        Route route = interestingRouteFinder.findMostInterestingRoute(
                "R34", "R22", Set.of("Turner", "Botticelli"), Set.of("R35")
        );

        assertNull(route);
    }

    @Test
    void interestingRouteShouldAvoidOptionalExhibitRoomAndStillFindRoute() {
        Route route = interestingRouteFinder.findMostInterestingRoute(
                "R34", "R22", Set.of("Turner", "Botticelli"), Set.of("E5")
        );

        assertNotNull(route);
        assertTrue(route.getRooms().stream().noneMatch(r -> r.getId().equals("R19")));
    }

    @Test
    void interestingRouteWithWaypointShouldIncludeWaypointRoom() {
        Route route = interestingRouteFinder.findMostInterestingRouteWithWaypoints(
                "R34", "R22", List.of("E5"), Set.of("Turner", "Botticelli"), Set.of()
        );

        assertNotNull(route);
        assertTrue(route.getRooms().stream().anyMatch(r -> r.getId().equals("R19")));
    }
}