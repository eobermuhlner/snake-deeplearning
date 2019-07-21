package snake.javafx;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import snake.controller.*;
import snake.domain.SnakeGame;
import snake.domain.Tile;
import snake.wall.CrosshairWallBuilder;
import snake.wall.DotsWallBuilder;
import snake.wall.NoWallBuilder;
import snake.wall.WallBuilder;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DecimalFormat;

public class SnakeJavafxApp extends Application {

    private static final DecimalFormat integerFormat = new DecimalFormat("##0");

    private SnakeGame game;

    private Canvas snakeCanvas;
    private Timeline simulationTimeline = new Timeline();
    private double simulationRate = 50; // milliseconds

    private IntegerProperty mapSizeProperty = new SimpleIntegerProperty();
    private ListProperty<WallBuilder> wallBuilderListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private ObjectProperty<WallBuilder> wallBuilderProperty = new SimpleObjectProperty<>();
    private ListProperty<SnakeController> controllerListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private ObjectProperty<SnakeController> controllerProperty = new SimpleObjectProperty<>();

    private Button runButton;
    private Button stepButton;
    private Button stopButton;

    private StringProperty statusProperty = new SimpleStringProperty();
    private IntegerProperty lengthProperty = new SimpleIntegerProperty();
    private IntegerProperty stepsProperty = new SimpleIntegerProperty();
    private IntegerProperty hungerProperty = new SimpleIntegerProperty();

    @Override
    public void start(Stage primaryStage) {
        Group root = new Group();
        Scene scene = new Scene(root);

        TabPane mainTabPane = new TabPane();
        root.getChildren().add(mainTabPane);
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        mainTabPane.getTabs().add(new Tab("Play", createPlayView()));
        mainTabPane.getTabs().add(new Tab("AI", createAiView()));

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Node createPlayView() {
        BorderPane borderPane = new BorderPane();

        HBox toolbar = new HBox();
        borderPane.setTop(toolbar);

        mapSizeProperty.setValue(20);
        TextField mapSizeField = new TextField();
        mapSizeField.setMaxWidth(50);
        toolbar.getChildren().add(mapSizeField);
        Bindings.bindBidirectional(mapSizeField.textProperty(), mapSizeProperty, integerFormat);
        mapSizeProperty.addListener((observable, oldValue, newValue) -> resetSimulation());

        wallBuilderListProperty.add(new NoWallBuilder());
        wallBuilderListProperty.add(new DotsWallBuilder(2));
        wallBuilderListProperty.add(new CrosshairWallBuilder());
        wallBuilderProperty.set(wallBuilderListProperty.get(0));
        ComboBox<WallBuilder> wallBuilderComboBox = new ComboBox<>();
        toolbar.getChildren().add(wallBuilderComboBox);
        Bindings.bindBidirectional(wallBuilderComboBox.itemsProperty(), wallBuilderListProperty);
        wallBuilderComboBox.valueProperty().bindBidirectional(wallBuilderProperty);
        wallBuilderProperty.addListener((observable, oldValue, newValue) -> resetSimulation());

        controllerListProperty.add(new LookaheadRandomSnakeController());
        controllerListProperty.add(new RandomSnakeController());
        controllerListProperty.add(new BoringSnakeController());
        addControllersFromFiles();
        controllerProperty.set(controllerListProperty.get(0));
        ComboBox<SnakeController> controllerComboBox = new ComboBox<>();
        toolbar.getChildren().add(controllerComboBox);
        Bindings.bindBidirectional(controllerComboBox.itemsProperty(), controllerListProperty);
        controllerComboBox.valueProperty().bindBidirectional(controllerProperty);
        controllerProperty.addListener((observable, oldValue, newValue) -> resetSimulation());

        Button resetButton = new Button("Reset");
        toolbar.getChildren().add(resetButton);
        runButton = new Button("Run");
        toolbar.getChildren().add(runButton);
        stepButton = new Button("Step");
        toolbar.getChildren().add(stepButton);
        stopButton = new Button("Stop");
        toolbar.getChildren().add(stopButton);

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

        Node propertiesPane = createPropertiesPane();
        borderPane.setRight(propertiesPane);

        snakeCanvas = new Canvas(600, 600);
        borderPane.setCenter(snakeCanvas);

        resetSimulation();
        updateRunButtons(false);
        setupRendering();

        return borderPane;
    }

    private void addControllersFromFiles() {
        File dir = new File(".");
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".dl4j");
            }
        });
        for (File file : files) {
            String name = file.getName();
            name = name.substring(0, name.length() - ".dl4j".length());
            controllerListProperty.add(new DeeplearningSnakeController(name));
        }
    }

    private Node createAiView() {
        BorderPane borderPane = new BorderPane();



        return borderPane;
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
        game = new SnakeGame(
                mapSizeProperty.get(),
                mapSizeProperty.get(),
                wallBuilderProperty.get(),
                1,
                controllerProperty.get());
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
        updateRunButtons(true);
    }

    private void stopSimulation() {
        simulationTimeline.stop();
        updateRunButtons(false);
    }

    private void updateRunButtons(boolean running) {
        runButton.setDisable(running);
        stopButton.setDisable(!running);
        stepButton.setDisable(running);
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

        gc.setFill(Color.WHITE);
        gc.fill();

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
