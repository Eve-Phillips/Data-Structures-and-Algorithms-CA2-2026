package ie.setu.gallery.benchmark;

import ie.setu.gallery.graph.GalleryGraph;
import ie.setu.gallery.graph.GraphLoader;
import ie.setu.gallery.model.GridPoint;
import ie.setu.gallery.model.Route;
import ie.setu.gallery.service.BfsMapRouteFinder;
import ie.setu.gallery.service.InterestingRouteFinder;
import ie.setu.gallery.service.RouteFinder;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class RouteBenchmark {

    private RouteFinder routeFinder;
    private InterestingRouteFinder interestingRouteFinder;
    private BfsMapRouteFinder bfsMapRouteFinder;

    private boolean[][] walkableGrid;
    private GridPoint bfsStart;
    private GridPoint bfsEnd;

    @Setup(Level.Trial)
    public void setUp() throws IOException {
        GalleryGraph graph = GraphLoader.loadFromCsv(
                "data/rooms.csv",
                "data/edges.csv",
                "data/exhibits.csv"
        );

        routeFinder = new RouteFinder(graph);
        interestingRouteFinder = new InterestingRouteFinder(graph);
        bfsMapRouteFinder = new BfsMapRouteFinder();

        walkableGrid = createBenchmarkGrid(600, 400);
        bfsStart = new GridPoint(20, 20);
        bfsEnd = new GridPoint(560, 360);
    }

    @Benchmark
    public Route benchmarkDijkstraShortestRoute() {
        return routeFinder.findShortestRouteDijkstra("E1", "E4", Set.of());
    }

    @Benchmark
    public Route benchmarkDijkstraShortestRouteWithWaypoints() {
        return routeFinder.findShortestRouteWithWaypoints(
                "E1",
                "E6",
                List.of("E3", "E5"),
                Set.of()
        );
    }

    @Benchmark
    public List<Route> benchmarkDfsMultipleRoutes() {
        return routeFinder.findAllRoutesDFS("R34", "R22", Set.of(), 10);
    }

    @Benchmark
    public Route benchmarkInterestingRoute() {
        return interestingRouteFinder.findMostInterestingRoute(
                "E1",
                "E4",
                Set.of("Turner", "Botticelli"),
                Set.of()
        );
    }

    @Benchmark
    public List<GridPoint> benchmarkBfsMapRoute() {
        return bfsMapRouteFinder.findPath(walkableGrid, bfsStart, bfsEnd);
    }

    private boolean[][] createBenchmarkGrid(int width, int height) {
        boolean[][] grid = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = true;
            }
        }

        // Vertical wall with a gap
        for (int y = 40; y < 360; y++) {
            if (y < 180 || y > 220) {
                grid[y][250] = false;
            }
        }

        // Horizontal wall with a gap
        for (int x = 120; x < 520; x++) {
            if (x < 300 || x > 340) {
                grid[200][x] = false;
            }
        }

        return grid;
    }
}