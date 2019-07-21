package snake.domain;

public class Snake {
    private int[] x;
    private int[] y;
    private int length;
    private int headIndex;
    private int tailIndex;

    public boolean alive = true;

    public Snake(int startX, int startY, int fullLength, int initialLength) {
        x = new int[fullLength];
        y = new int[fullLength];
        length = initialLength;
        headIndex = initialLength;
        tailIndex = 0;
        for (int i = 0; i <= initialLength; i++) {
            x[i] = startX;
            y[i] = startY;
        }
    }

    public void render(SnakeMap map) {
        for (int i = 0; i <= length; i++) {
            Tile tile = i == 0 ? Tile.SnakeHead : Tile.SnakeTail;
            map.set(getX(i), getY(i), tile);
        }
    }

    public boolean move(int dX, int dY, SnakeMap snakeMap) {
        int oldHeadX = x[headIndex];
        int oldHeadY = y[headIndex];

        int newHeadX = oldHeadX + dX;
        int newHeadY = oldHeadY + dY;

        boolean eaten = snakeMap.get(newHeadX, newHeadY) == Tile.Apple;

        if (!eaten) {
            int endX = x[tailIndex];
            int endY = y[tailIndex];
            snakeMap.set(endX, endY, Tile.Empty);

            tailIndex++;
            if (tailIndex >= x.length) {
                tailIndex = 0;
            }
        } else {
            length++;
        }

        if (length > 0) {
            snakeMap.set(oldHeadX, oldHeadY, Tile.SnakeTail);
        }

        headIndex++;
        if (headIndex >= x.length) {
            headIndex = 0;
        }

        x[headIndex] = newHeadX;
        y[headIndex] = newHeadY;

        Tile underHead = snakeMap.get(x[headIndex], y[headIndex]);
        alive = underHead == Tile.Empty || underHead == Tile.Apple;

        snakeMap.set(newHeadX, newHeadY, Tile.SnakeHead);

        return eaten;
    }

    public int getLength() {
        return length;
    }

    public int getX(int index) {
        int i = (headIndex + x.length - index) % x.length;
        return x[i];
    }

    public int getY(int index) {
        int i = (headIndex + y.length - index) % y.length;
        return y[i];
    }
}
