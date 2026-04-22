package ie.setu.gallery;

import ie.setu.gallery.model.GridPoint;
import ie.setu.gallery.service.BfsMapRouteFinder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BfsMapRouteFinderTest {

    @Test
    void bfsShouldFindPathAroundWall() {
        BfsMapRouteFinder finder = new BfsMapRouteFinder();

        // 5 rows x 5 columns
        boolean[][] walkable = {
                {true,  true,  false, true,  true},
                {true,  true,  false, true,  true},
                {true,  true,  false, true,  true},
                {true,  true,  false, true,  true},
                {true,  true,  true,  true,  true}
        };

        GridPoint start = new GridPoint(0, 0);
        GridPoint end = new GridPoint(4, 0);

        List<GridPoint> path = finder.findPath(walkable, start, end);

        assertFalse(path.isEmpty());
        assertEquals(start, path.getFirst());
        assertEquals(end, path.getLast());
    }

    @Test
    void bfsShouldReturnEmptyIfNoPathExists() {
        BfsMapRouteFinder finder = new BfsMapRouteFinder();

        boolean[][] walkable = {
                {true,  false, true},
                {false, false, false},
                {true,  false, true}
        };

        GridPoint start = new GridPoint(0, 0);
        GridPoint end = new GridPoint(2, 2);

        List<GridPoint> path = finder.findPath(walkable, start, end);

        assertTrue(path.isEmpty());
    }

    @Test
    void bfsShouldReturnEmptyIfStartOrEndBlocked() {
        BfsMapRouteFinder finder = new BfsMapRouteFinder();

        boolean[][] walkable = {
                {false, true, true},
                {true,  true, true},
                {true,  true, false}
        };

        List<GridPoint> path1 = finder.findPath(walkable, new GridPoint(0, 0), new GridPoint(1, 1));
        List<GridPoint> path2 = finder.findPath(walkable, new GridPoint(1, 1), new GridPoint(2, 2));

        assertTrue(path1.isEmpty());
        assertTrue(path2.isEmpty());
    }
}