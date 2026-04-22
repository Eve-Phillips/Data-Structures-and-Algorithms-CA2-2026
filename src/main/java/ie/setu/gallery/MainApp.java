package ie.setu.gallery;

import ie.setu.gallery.graph.GalleryGraph;
import ie.setu.gallery.graph.GraphLoader;
import ie.setu.gallery.model.Route;
import ie.setu.gallery.service.InterestingRouteFinder;
import ie.setu.gallery.service.RouteFinder;

import java.util.List;
import java.util.Set;

public class MainApp {

    public static void main(String[] args) {
        try {
            GalleryGraph graph = GraphLoader.loadFromCsv(
                    "data/rooms.csv",
                    "data/edges.csv",
                    "data/exhibits.csv"
            );

            RouteFinder routeFinder = new RouteFinder(graph);

            System.out.println("=== ANY VALID ROUTE ===");
            Route anyRoute = routeFinder.findAnyValidRoute("R34", "R22", Set.of());
            System.out.println(anyRoute);

            System.out.println("\n=== MULTIPLE DFS ROUTES ===");
            List<Route> routes = routeFinder.findAllRoutesDFS("R34", "R22", Set.of(), 10);
            routes.forEach(System.out::println);

            System.out.println("\n=== SHORTEST ROUTE DIJKSTRA ===");
            Route shortest = routeFinder.findShortestRouteDijkstra("R34", "R22", Set.of());
            System.out.println(shortest);

            System.out.println("\n=== WITH WAYPOINT ===");
            Route waypointRoute = routeFinder.findShortestRouteWithWaypoints(
                    "R34", "R22", List.of("R20"), Set.of()
            );
            System.out.println(waypointRoute);

            System.out.println("\n=== AVOID ROOM R20 ===");
            Route avoidRoute = routeFinder.findShortestRouteDijkstra("R34", "R22", Set.of("R20"));
            System.out.println(avoidRoute);

            System.out.println("\n=== MOST INTERESTING ROUTE ===");
            InterestingRouteFinder interestingRouteFinder = new InterestingRouteFinder(graph);
            Route interesting = interestingRouteFinder.findMostInterestingRoute(
                    "R34", "R22", Set.of("Turner", "Botticelli"), Set.of()
            );
            System.out.println(interesting);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}