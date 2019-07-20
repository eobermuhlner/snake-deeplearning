package snake;

public class SnakeApp {
    public static void main(String[] args) {
        runSnake();
    }

    private static void runSnake() {
        RandomSnakeController controller = new RandomSnakeController();
        SnakeGame game = new SnakeGame(controller);
        while (game.step()) {
            print(game.snakeMap);
        }
    }

    private static void print(SnakeMap snakeMap) {
        for (int y = 0; y < snakeMap.height; y++) {
            for (int x = 0; x < snakeMap.width; x++) {
                System.out.print(snakeMap.get(x, y).tileChar);
            }
            System.out.println();
        }
        System.out.println();
    }
}
