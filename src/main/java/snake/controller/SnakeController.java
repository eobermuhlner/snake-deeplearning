package snake.controller;

import snake.domain.Move;
import snake.domain.Snake;
import snake.domain.SnakeMap;

public interface SnakeController {

    Move getMove(Snake snake, SnakeMap snakeMap);
}
