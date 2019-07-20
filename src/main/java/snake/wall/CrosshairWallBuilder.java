package snake.wall;

import snake.domain.SnakeMap;
import snake.domain.Tile;

public class CrosshairWallBuilder implements WallBuilder {
    @Override
    public int createWall(SnakeMap map) {
        int count = 0;

        int startX = map.width / 4;
        int endX = map.width - startX;
        int centerX = map.width / 2;

        int startY = map.height / 4;
        int endY = map.height  - startY;
        int centerY = map.width / 2;

        for (int x = startX; x < endX; x++) {
            if (map.get(x, centerY) == Tile.Empty) {
                count++;
                map.set(x, centerY, Tile.Wall);
            }
        }
        for (int y = startY; y < endY; y++) {
            if (map.get(centerX, y) == Tile.Empty) {
                count++;
                map.set(centerX, y, Tile.Wall);
            }
        }

        return count;
    }
}
