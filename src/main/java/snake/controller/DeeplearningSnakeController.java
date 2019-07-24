package snake.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.AdaDelta;
import org.nd4j.linalg.lossfunctions.ILossFunction;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import snake.domain.*;
import snake.wall.DotsWallBuilder;
import snake.wall.RandomCompositeWallBuilder;
import snake.wall.WallBuilder;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;

public class DeeplearningSnakeController implements SnakeController {
    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;

    private static final int OUTPUT_COUNT = 4;

    private static final boolean PRINT_DEBUG = false;

    private static Map<Tile, Double> tileToInput = new HashMap<>();
    static {
        tileToInput.put(Tile.Empty, 0.9);
        tileToInput.put(Tile.Apple, 1.0);
        tileToInput.put(Tile.Wall, 0.0);
        tileToInput.put(Tile.SnakeHead, 0.0);
        tileToInput.put(Tile.SnakeTail, 0.0);
    }

    private static Map<Integer, Move> indexToMove = new HashMap<>();
    static {
        indexToMove.put(UP, Move.Up);
        indexToMove.put(DOWN, Move.Down);
        indexToMove.put(LEFT, Move.Left);
        indexToMove.put(RIGHT, Move.Right);
    }

    private final Random random = new Random();
    private final String name;
    private final DeeplearningConfiguration deeplearningConfiguration;
    private final MultiLayerNetwork model;

    private DeeplearningSnakeController(String name, DeeplearningConfiguration deeplearningConfiguration, MultiLayerNetwork model) {
        this.name = name;
        this.deeplearningConfiguration = deeplearningConfiguration;
        this.model = model;
    }

    public void save() {
        save(name, deeplearningConfiguration, model);
    }

    @Override
    public Move getMove(Snake snake, SnakeMap snakeMap) {
        int x = snake.getX(0);
        int y = snake.getY(0);

        double[] input = toInput(deeplearningConfiguration, snake, snakeMap);
        INDArray inputArray = Nd4j.create(input);

        INDArray outputArray = model.output(inputArray);
        Move move = pickMove(outputArray);

        if (PRINT_DEBUG) {
            int inputWidth = deeplearningConfiguration.inputWidth;
            int inputRadius = inputWidth / 2;
            if (!Tile.isValidMove(snakeMap.get(x + move.dX, y + move.dY))) {
                System.out.println("INVALID " + move);
                System.out.println("  input =" + inputArray);
                System.out.println("  output=" + outputArray);
                for (int inputY = 0; inputY < inputWidth; inputY++) {
                    for (int inputX = 0; inputX < inputWidth; inputX++) {
                        System.out.print(snakeMap.get(x + (inputX - inputRadius), y + (inputY - inputRadius)).tileChar);
                    }
                    System.out.println();
                }
                System.out.println();
            }
        }

        return move;
    }

    private Move pickMove(INDArray outputArray) {
        double sum = 0;
        for(int index : Arrays.asList(UP, DOWN, LEFT, RIGHT)) {
            double value = outputArray.getDouble(index);
            sum += value;
        }

        double r = random.nextDouble() * sum;
        sum = 0;
        for(int index : Arrays.asList(UP, DOWN, LEFT, RIGHT)) {
            double value = outputArray.getDouble(index);
            sum += value;
            if (r <= sum) {
                return indexToMove.get(index);
            }
        }

        return Move.Up;
    }

    public double train(int n) {
        return train(n, this, new RandomCompositeWallBuilder());
    }

    public double train(int n, SnakeController teacher, WallBuilder wallBuilder) {
        for (int i = 0; i < n; i++) {
            List<DataSet> dataSets = createGameDataSets(deeplearningConfiguration, teacher, wallBuilder);

            if (!dataSets.isEmpty()) {
                DataSet dataSet = DataSet.merge(dataSets);
                model.fit(dataSet);
            }
        }

        return model.score();
    }

    public Statistics test() {
        return test(1000);
    }

    public Statistics test(int steps) {
        return test(steps, new DotsWallBuilder(2));
    }

    public Statistics test(int steps, WallBuilder wallBuilder) {
        int countEaten = 0;
        int countDead = 0;

        SnakeGame game = null;
        boolean alive = true;
        for (int i = 0; i < steps; i++) {
            if (game == null || !alive) {
                game = new SnakeGame(20, 20, wallBuilder, 1, this);
            }
            alive = game.step();
            if (alive) {
                boolean hasEaten = game.getHasEaten();
                if (hasEaten) {
                    countEaten++;
                }
            } else {
                countDead++;
            }
        }
        return new Statistics((double)countDead/steps, (double)countEaten/steps);
    }

    @Override
    public String toString() {
        return "AI " + name;
    }

    public static DeeplearningSnakeController create(String name) {
        String snakeFileName = name + ".snake";

        DeeplearningConfiguration deeplearningConfiguration = loadDeeplearningConfiguration(snakeFileName);
        return create(name, deeplearningConfiguration);
    }

    public static DeeplearningSnakeController create(String name, DeeplearningConfiguration deeplearningConfiguration) {
        String dl4jFileName = name + ".dl4j";
        MultiLayerNetwork model = loadNetwork(dl4jFileName, deeplearningConfiguration);

        return new DeeplearningSnakeController(name, deeplearningConfiguration, model);
    }

    private static void save(String name, DeeplearningConfiguration deeplearningConfiguration, MultiLayerNetwork model) {
        String snakeFileName = name + ".snake";
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        String json = gson.toJson(deeplearningConfiguration);

        try (FileWriter writer = new FileWriter(snakeFileName)) {
            writer.write(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String dl4jFileName = name + ".dl4j";
        try {
            ModelSerializer.writeModel(model, dl4jFileName, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static DeeplearningConfiguration loadDeeplearningConfiguration(String snakeFileName) {
        File snakeFile = new File(snakeFileName);
        if (snakeFile.exists()) {
            try {
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();
                DeeplearningConfiguration deeplearningConfiguration = gson.fromJson(new FileReader(snakeFile), DeeplearningConfiguration.class);
                return deeplearningConfiguration;
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        return new DeeplearningConfiguration();
    }

    private static MultiLayerNetwork loadNetwork(String fileName, DeeplearningConfiguration deeplearningConfiguration) {
        try {
            if (new File(fileName).exists()) {
                return ModelSerializer.restoreMultiLayerNetwork(fileName);
            } else {
                MultiLayerNetwork model = createNetwork(deeplearningConfiguration);
                model.init();
                return model;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        try {
            //train("snake", new BoringSnakeController(), 60);
            train("snake", new LookaheadRandomSnakeController(), 0);
            //train("snake", null, 1 * 60);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void train(String name, SnakeController teacher, long seconds) throws IOException, InterruptedException {
        DeeplearningSnakeController deeplearningSnakeController = create(name);
        WallBuilder wallBuilder = new RandomCompositeWallBuilder();

        if (teacher == null) {
            teacher = deeplearningSnakeController;
        }

        long endMillis = System.currentTimeMillis() + seconds * 1000;
        do {
            double score = deeplearningSnakeController.train(1, teacher, wallBuilder);
            System.out.println("Score: " + score);
        } while (System.currentTimeMillis() < endMillis);

        /*
        DataSet testDataSet = DataSet.merge(createGameDataSets(teacher));
        Evaluation evaluation = new Evaluation(OUTPUT_COUNT);
        evaluation.eval(testDataSet.getLabels(), testDataSet.getFeatures());
        System.out.println(evaluation.stats());
        */

        deeplearningSnakeController.save();
    }

    private static List<DataSet> createGameDataSets(DeeplearningConfiguration deeplearningConfiguration, SnakeController controller, WallBuilder wallBuilder) {
        return createGameDataSets(deeplearningConfiguration, () -> {
            Random random = new Random();
            int width = random.nextInt(10) + 5;
            int height = random.nextInt(10) + 5;
            int initialLength = random.nextInt(width * height / 2) + 1;
            return new SnakeGame(width, height, wallBuilder, initialLength, controller);
        });
    }

    private static List<DataSet> createGameDataSets(DeeplearningConfiguration deeplearningConfiguration, Supplier<SnakeGame> gameCreator) {
        List<DataSet> resultDataSets = new ArrayList<>();

        SnakeGame game = null;
        boolean hasEaten = false;
        while (resultDataSets.size() < 1000) {
            if (game == null || !game.snake.alive || !hasEaten) {
                game = gameCreator.get();
            }
            List<DataSet> tempDataSets = new ArrayList<>();

            int stepCounter = 0;
            do {
                stepCounter++;
                double[] input = toInput(deeplearningConfiguration, game);
                game.step();
                if (game.snake.alive) {
                    int outputLabel = game.getMove().ordinal();
                    DataSet dataSet = toDataSet(outputLabel, OUTPUT_COUNT, input);
                    //System.out.println(game.getMove());
                    //System.out.println(dataSet);
                    //System.out.println();

                    tempDataSets.add(dataSet);
                }
                hasEaten = game.getHasEaten();
            } while (game.snake.alive && !hasEaten && stepCounter < 100);

            if (game.snake.alive && hasEaten) {
                resultDataSets.addAll(tempDataSets);
            }
        }

        return resultDataSets;
    }

    private static double[] toInput(DeeplearningConfiguration deeplearningConfiguration, SnakeGame game) {
        return toInput(deeplearningConfiguration, game.snake, game.snakeMap);
    }

    private static double[] toInput(DeeplearningConfiguration deeplearningConfiguration, Snake snake, SnakeMap snakeMap) {
        int x = snake.getX(0);
        int y = snake.getY(0);

        int appleX = snakeMap.getAppleX();
        int appleY = snakeMap.getAppleY();

        int directionAppleX = appleX - x;
        int directionAppleY = appleY - y;
        double relDirectionAppleX = (double)directionAppleX / snakeMap.width;
        double relDirectionAppleY = (double)directionAppleY / snakeMap.height;

        int inputWidth = deeplearningConfiguration.inputWidth;
        int inputRadius = inputWidth / 2;
        int inputCount = inputWidth * inputWidth + 2;
        double[] input = new double[inputCount];
        for (int inputY = 0; inputY < inputWidth; inputY++) {
            for (int inputX = 0; inputX < inputWidth; inputX++) {
                input[inputX + inputY * inputWidth] = tileToInput.get(snakeMap.get(x + (inputX - inputRadius), y + (inputY - inputRadius)));
            }
        }
        input[inputCount - 2] = relDirectionAppleX;
        input[inputCount - 1] = relDirectionAppleY;
        return input;
    }

    private static DataSet toDataSet(int outputLabel, int outputLabelCount, double... input) {
        INDArray inputArray = Nd4j.create(input);

        double[] output = new double[outputLabelCount];
        for (int i = 0; i < output.length; i++) {
            output[i] = i == outputLabel ? 1.0 : 0.0;
        }
        INDArray outputArray = Nd4j.create(output);
        return new DataSet(inputArray, outputArray);
    }

    private static MultiLayerNetwork createNetwork(DeeplearningConfiguration deeplearningConfiguration) {
        final int inputWidth = deeplearningConfiguration.inputWidth;
        final int inputCount = inputWidth * inputWidth + 2;
        final int denseCount = deeplearningConfiguration.inputWidth;
        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .seed(123)
                .l2(0.0001)
                .miniBatch(true)
                .activation(deeplearningConfiguration.defaultActivation)
                .weightInit(deeplearningConfiguration.defaultWeightInit)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new AdaDelta())
                .list()
                .layer(0, new DenseLayer.Builder().nIn(inputCount).nOut(denseCount).build())
                .layer(1, new DenseLayer.Builder().nIn(denseCount).nOut(denseCount).build())
                .layer(2, new DenseLayer.Builder().nIn(denseCount).nOut(denseCount).build())
                .layer(3, new OutputLayer.Builder().nIn(denseCount).nOut(OUTPUT_COUNT)
                        .activation(deeplearningConfiguration.outputActivation)
                        .lossFunction(deeplearningConfiguration.outputLossFunction)
                        .build())
                .build();
        MultiLayerNetwork network = new MultiLayerNetwork(configuration);
        return network;
    }

    public DeeplearningConfiguration getDeeplearningConfiguration() {
        return deeplearningConfiguration;
    }

    public static class Statistics {
        public final double dead;
        public final double eaten;

        public Statistics(double dead, double eaten) {
            this.dead = dead;
            this.eaten = eaten;
        }
    }

    public static class DeeplearningConfiguration {
        public int inputWidth = 5;

        public Activation defaultActivation = Activation.RELU;
        public WeightInit defaultWeightInit = WeightInit.XAVIER;

        public Activation outputActivation = Activation.SOFTMAX;
        public LossFunctions.LossFunction outputLossFunction = LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD;

        public DeeplearningConfiguration() {
        }
    }
}
