package snake.wall;

import snake.SnakeMap;

public class NoWallBuilder implements WallBuilder {
    @Override
    public int createWall(SnakeMap map) {
        return 0;
    }
}
