package snake.domain;

import java.util.Arrays;

public class SnakeMap {
    public final int width;
    public final int height;
    private Tile[] tiles;

    private int appleX;
    private int appleY;

    public SnakeMap(int width, int height) {
        this.width = width;
        this.height = height;
        tiles = new Tile[width*height];
        Arrays.setAll(tiles, (i) -> Tile.Empty);
    }

    public Tile get(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return Tile.Wall;
        }

        return tiles[x + y * width];
    }

    public void set(int x, int y, Tile tile) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }

        tiles[x + y * width] = tile;
    }

    public void setApple(int x, int y) {
        appleX = x;
        appleY = y;
        set(x, y, Tile.Apple);
    }

    public int getAppleX() {
        return appleX;
    }

    public int getAppleY() {
        return appleY;
    }
}
