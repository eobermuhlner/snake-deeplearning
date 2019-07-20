package snake.domain;

public enum Tile {
    Empty('.'),
    Wall('#'),
    SnakeHead('O'),
    SnakeTail('o'),
    Apple('X');

    public final char tileChar;

    Tile(char tileChar) {
        this.tileChar = tileChar;
    }

    public static boolean isValidMove(Tile tile) {
        return tile == Tile.Empty || tile == Tile.Apple;
    }
}
