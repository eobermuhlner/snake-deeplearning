package snake.controller;

import snake.domain.Move;
import snake.domain.Snake;
import snake.domain.SnakeMap;

import java.util.Random;

public class RandomSnakeController implements SnakeController {
    private final Random random = new Random();

    @Override
    public Move getMove(Snake snake, SnakeMap snakeMap) {
        return randomMove();
    }

    private Move randomMove() {
        Move[] values = Move.values();
        int index = random.nextInt(values.length);
        return values[index];
    }

    @Override
    public String toString() {
        return "Random";
    }
}
