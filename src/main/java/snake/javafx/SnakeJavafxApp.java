package snake.javafx;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.util.Duration;
import snake.controller.DeeplearningSnakeController;
import snake.domain.SnakeGame;
import snake.domain.Tile;
import snake.wall.CrosshairWallBuilder;
import snake.wall.DotsWallBuilder;
import snake.wall.WallBuilder;

import java.text.DecimalFormat;

public class SnakeJavafxApp extends Application {

    private static final DecimalFormat integerFormat = new DecimalFormat("##0");

    private SnakeGame game;
    private DeeplearningSnakeController deeplearningSnakeController = new DeeplearningSnakeController("snake");

    private Canvas snakeCanvas;
    private Timeline simulationTimeline = new Timeline();
    private double simulationRate = 50; // milliseconds

    private Button runButton;
    private Button stepButton;
    private Button stopButton;
    private Button trainButton;

    private StringProperty statusProperty = new SimpleStringProperty();
    private IntegerProperty lengthProperty = new SimpleIntegerProperty();
    private IntegerProperty stepsProperty = new SimpleIntegerProperty();
    private IntegerProperty hungerProperty = new SimpleIntegerProperty();

    @Override
    public void start(Stage primaryStage) {
        Group root = new Group();
        Scene scene = new Scene(root);

        BorderPane borderPane = new BorderPane();
        root.getChildren().add(borderPane);

        HBox toolbar = new HBox();
        borderPane.setTop(toolbar);

        Button resetButton = new Button("Reset");
        toolbar.getChildren().add(resetButton);
        runButton = new Button("Run");
        toolbar.getChildren().add(runButton);
        stepButton = new Button("Step");
        toolbar.getChildren().add(stepButton);
        stopButton = new Button("Stop");
        toolbar.getChildren().add(stopButton);
        trainButton = new Button("Train");
        toolbar.getChildren().add(trainButton);

        resetButton.addEventHandler(ActionEvent.ACTION, event -> {
            resetSimulation();
        });
        runButton.addEventHandler(ActionEvent.ACTION, event -> {
            runSimulation();
        });
        stepButton.addEventHandler(ActionEvent.ACTION, event -> {
            stepSimulation();
            drawSnakeMap();
        });
        stopButton.addEventHandler(ActionEvent.ACTION, event -> {
            stopSimulation();
        });
        trainButton.addEventHandler(ActionEvent.ACTION, event -> {
            trainSimulation();
        });

        Node propertiesPane = createPropertiesPane();
        borderPane.setRight(propertiesPane);

        snakeCanvas = new Canvas(800, 600);
        borderPane.setCenter(snakeCanvas);

        resetSimulation();
        updateRunButtons(false, false);
        setupRendering();

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Node createPropertiesPane() {
        GridPane propertiesPane = new GridPane();
        int rowIndex = 0;

        {
            propertiesPane.add(new Label("Status:"), 0, rowIndex);
            Label label = new Label("");
            propertiesPane.add(label, 1, rowIndex);
            Bindings.bindBidirectional(label.textProperty(), statusProperty);
            rowIndex++;
        }
        {
            propertiesPane.add(new Label("Length:"), 0, rowIndex);
            Label label = new Label("");
            Bindings.bindBidirectional(label.textProperty(), lengthProperty, integerFormat);
            propertiesPane.add(label, 1, rowIndex);
            rowIndex++;
        }
        {
            propertiesPane.add(new Label("Steps:"), 0, rowIndex);
            Label label = new Label("");
            Bindings.bindBidirectional(label.textProperty(), stepsProperty, integerFormat);
            propertiesPane.add(label, 1, rowIndex);
            rowIndex++;
        }
        {
            propertiesPane.add(new Label("Hunger:"), 0, rowIndex);
            Label label = new Label("");
            Bindings.bindBidirectional(label.textProperty(), hungerProperty, integerFormat);
            propertiesPane.add(label, 1, rowIndex);
            rowIndex++;
        }

        return propertiesPane;
    }

    private void resetSimulation() {
        stopSimulation();
        WallBuilder wallBuilder = new DotsWallBuilder(2);
        //WallBuilder wallBuilder = new CrosshairWallBuilder();
        game = new SnakeGame(20, 20, wallBuilder, 1, deeplearningSnakeController);
        drawSnakeMap();

        statusProperty.setValue("Alive");
        lengthProperty.setValue(game.snake.getLength());
        stepsProperty.setValue(0);
        hungerProperty.setValue(0);
    }

    private void stepSimulation() {
        boolean alive = game.step();
        if (!alive) {
            stopSimulation();
            statusProperty.setValue("Dead");
        }

        lengthProperty.setValue(game.snake.getLength());
        stepsProperty.setValue(stepsProperty.getValue() + 1);
        if (game.getHasEaten()) {
            hungerProperty.setValue(0);
        } else {
            hungerProperty.setValue(hungerProperty.getValue() + 1);
        }
    }

    private void runSimulation() {
        simulationTimeline.play();
        updateRunButtons(true, false);
    }

    private void stopSimulation() {
        simulationTimeline.stop();
        updateRunButtons(false, false);
    }

    private void trainSimulation() {
        updateRunButtons(false, true);
        double score = deeplearningSnakeController.train(1);
        System.out.println("SCORE " + score);
        updateRunButtons(false, false);
    }

    private void updateRunButtons(boolean running, boolean training) {
        runButton.setDisable(running || training);
        stopButton.setDisable(!running && !training);
        stepButton.setDisable(running || training);

        trainButton.setDisable(running || training);
    }

    private void setupRendering() {
        drawSnakeMap();

        simulationTimeline.setCycleCount(Timeline.INDEFINITE);
        simulationTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(simulationRate), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                stepSimulation();
                drawSnakeMap();
            }
        }));
    }

    private void drawSnakeMap() {
        GraphicsContext gc = snakeCanvas.getGraphicsContext2D();

        double tileWidth = 5;
        double tileHeight = 5;
        double tileStepX = 1;
        double tileStepY = 1;

        for (int y = 0; y < game.height; y++) {
            for (int x = 0; x < game.width; x++) {
                double pixelX = x * (tileWidth + tileStepX);
                double pixelY = y * (tileWidth + tileStepY);
                double pixelW = tileWidth;
                double pixelH = tileHeight;
                gc.setFill(toColor(game.snakeMap.get(x, y)));
                gc.fillRect(pixelX, pixelY, pixelW, pixelH);
            }
        }
    }

    private Paint toColor(Tile tile) {
        switch (tile) {
            case Empty:
                return Color.LIGHTGRAY;
            case Wall:
                return Color.DARKGRAY;
            case SnakeHead:
                return Color.RED;
            case SnakeTail:
                return Color.ORANGE;
            case Apple:
                return Color.LAWNGREEN;
        }
        throw new RuntimeException("Unknown: " + tile);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
