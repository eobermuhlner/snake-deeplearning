package snake.wall;

import snake.domain.SnakeMap;
import snake.domain.Tile;

import java.util.Random;

public class DotsWallBuilder implements WallBuilder {
    private final Random random = new Random();

    @Override
    public int createWall(SnakeMap map) {
        int n = map.width * map.height / 100;

        for (int i = 0; i < n; i++) {
            int x = random.nextInt(map.width);
            int y = random.nextInt(map.height);
            map.set(x, y, Tile.Wall);
        }
        return n;
    }
}
