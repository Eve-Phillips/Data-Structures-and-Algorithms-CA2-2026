package ie.setu.gallery.service;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

public class WalkableMapLoader {

    // Converts the black/white walkable map into a boolean grid.
    // White/light pixels are treated as walkable.
    // Black/dark pixels are treated as blocked.
    public boolean[][] loadWalkableGrid(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        boolean[][] walkable = new boolean[height][width];
        PixelReader reader = image.getPixelReader();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color colour = reader.getColor(x, y);

                boolean isWalkable = colour.getBrightness() > 0.75;

                walkable[y][x] = isWalkable;
            }
        }

        return walkable;
    }
}