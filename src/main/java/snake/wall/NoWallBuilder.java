package snake.wall;

import snake.domain.SnakeMap;

public class NoWallBuilder implements WallBuilder {
    @Override
    public int createWall(SnakeMap map) {
        return 0;
    }
}
