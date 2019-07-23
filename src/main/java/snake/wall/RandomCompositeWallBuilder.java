package snake.wall;

import snake.domain.SnakeMap;

import java.util.Random;

public class RandomCompositeWallBuilder implements WallBuilder {
    private final Random random = new Random();
    private final WallBuilder[] wallBuilders;

    public RandomCompositeWallBuilder() {
        this(new NoWallBuilder(), new DotsWallBuilder(2), new DotsWallBuilder(5), new DotsWallBuilder(10), new CrosshairWallBuilder());
    }

    public RandomCompositeWallBuilder(WallBuilder... wallBuilders) {
        this.wallBuilders = wallBuilders;
    }

    @Override
    public int createWall(SnakeMap map) {
        int r = random.nextInt(wallBuilders.length);
        return wallBuilders[r].createWall(map);
    }

    @Override
    public String toString() {
        return "Random";
    }
}
