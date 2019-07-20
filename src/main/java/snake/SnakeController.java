package snake;

public interface SnakeController {
    enum Move {
        Up(0, -1),
        Down(0, +1),
        Left(-1, 0),
        Right(+1, 0);

        public final int dX;
        public final int dY;

        Move(int dX, int dY) {
            this.dX = dX;
            this.dY = dY;
        }

        public Move counterMove() {
            switch (this) {
                case Up:
                    return Down;
                case Down:
                    return Up;
                case Left:
                    return Right;
                case Right:
                    return Left;
            }
            throw new RuntimeException("Unknown: " + this);
        }
    }

    Move getMove(Snake snake, SnakeMap snakeMap);
}
