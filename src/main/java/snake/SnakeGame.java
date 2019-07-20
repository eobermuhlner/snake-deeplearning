package snake;

import snake.wall.NoWallBuilder;
import snake.wall.WallBuilder;

import java.util.Random;

public class SnakeGame {
    public final int width;
    public final int height;
    private final int wallCount;

    public final SnakeMap snakeMap;
    public final Snake snake;

    private final SnakeController snakeController;

    private boolean hasEaten;
    private SnakeController.Move move;

    private Random random = new Random();

    public SnakeGame(SnakeController snakeController) {
        this(10, 10, new NoWallBuilder(), 1, snakeController);
    }

    public SnakeGame(int width, int height, WallBuilder wallBuilder, int initialLength, SnakeController snakeController) {
        this.width = width;
        this.height = height;
        this.snakeController = snakeController;

        this.snakeMap = new SnakeMap(width, height);

        wallCount = wallBuilder.createWall(snakeMap);

        int x;
        int y;
        do {
            x = random.nextInt(width);
            y = random.nextInt(height);
        } while (snakeMap.get(x, y) != Tile.Empty);
        this.snake = new Snake(x, y, width*height, initialLength);
        snake.render(snakeMap);

        setRandomApple();
    }

    private void setRandomApple() {
        int x;
        int y;
        do {
            x = random.nextInt(width);
            y = random.nextInt(height);
        } while (snakeMap.get(x, y) != Tile.Empty);

        snakeMap.setApple(x, y);
    }

    public Tile getTiles(int x, int y) {
        return snakeMap.get(x, y);
    }

    public boolean step() {
        move = snakeController.getMove(snake, snakeMap);
        hasEaten = snake.move(move.dX, move.dY, snakeMap);
        if (snake.getLength() >= width*height-wallCount-1) {
            return false; // WIN!
        }
        if (hasEaten) {
            setRandomApple();
        }
        return snake.alive;
    }

    public boolean getHasEaten() {
        return hasEaten;
    }

    public SnakeController.Move getMove() {
        return move;
    }
}
