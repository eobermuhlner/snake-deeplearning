package snake.controller;

import snake.domain.Snake;
import snake.domain.SnakeMap;
import snake.domain.Tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LookaheadRandomSnakeController implements SnakeController {
    private final Random random = new Random();
    private Move lastMove = randomMove();

    @Override
    public Move getMove(Snake snake, SnakeMap snakeMap) {
        int x = snake.getX(0);
        int y = snake.getY(0);

        List<Move> validMoves = new ArrayList<>();
        for (Move move : Move.values()) {
            Tile targetTile = snakeMap.get(x + move.dX, y + move.dY);
            if (Tile.isValidMove(targetTile)) {
                validMoves.add(move);
            }
        }

        if (validMoves.isEmpty()) {
            return Move.Up;
        }

        if (validMoves.contains(lastMove) && random.nextInt(100) < 80) {
            return lastMove;
        }

        int r = random.nextInt(validMoves.size());
        lastMove = validMoves.get(r);
        return lastMove;
    }

    private Move randomMove() {
        Move[] values = Move.values();
        int index = random.nextInt(values.length);
        return values[index];
    }
}
