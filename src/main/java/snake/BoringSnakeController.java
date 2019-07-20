package snake;

public class BoringSnakeController implements SnakeController {
    @Override
    public Move getMove(Snake snake, SnakeMap snakeMap) {
        int x = snake.getX(0);
        int y = snake.getY(0);

        if (x == 0) {
            if (y == 0) {
                return Move.Right;
            }
            return Move.Up;
        }

        if (y == snakeMap.height - 1) {
            return Move.Left;
        }

        if (y % 2 == 0) {
            if (x == snakeMap.width - 1) {
                return Move.Down;
            }
            return Move.Right;
        } else {
            if (x == 1) {
                return Move.Down;
            }
            return Move.Left;
        }
    }
}
