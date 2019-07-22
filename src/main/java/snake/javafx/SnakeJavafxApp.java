package snake.javafx;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
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
import java.text.Format;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private ListProperty<DeeplearningSnakeController> deeplearningControllerListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private ObjectProperty<DeeplearningSnakeController> deeplearningControllerProperty = new SimpleObjectProperty<>();
    private ObjectProperty<SnakeController> teacherControllerProperty = new SimpleObjectProperty<>();
    private IntegerProperty epochProperty = new SimpleIntegerProperty();

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
            DeeplearningSnakeController controller = new DeeplearningSnakeController(name);

            controllerListProperty.add(controller);
            deeplearningControllerListProperty.add(controller);
        }
    }

    private Node createAiView() {
        BorderPane masterDetailPane = new BorderPane();

        ListView<DeeplearningSnakeController> listView = new ListView<>();
        masterDetailPane.setLeft(listView);
        Bindings.bindBidirectional(listView.itemsProperty(), deeplearningControllerListProperty);
        deeplearningControllerProperty.bind(listView.getSelectionModel().selectedItemProperty());

        BorderPane editorPane = new BorderPane();
        masterDetailPane.setCenter(editorPane);

        // properties pane
        GridPane propertiesPane = new GridPane();
        editorPane.setLeft(propertiesPane);

        int rowIndex = 0;
        Label nameLabel = addLabel(propertiesPane, rowIndex++, "Name:");

        addComboBox(propertiesPane, rowIndex++, "Teacher:", controllerListProperty, teacherControllerProperty);

        Button startTrainButton = new Button("Start Training");
        propertiesPane.add(startTrainButton, 1, rowIndex);
        rowIndex++;

        Button stopTrainButton = new Button("Stop Training");
        propertiesPane.add(stopTrainButton, 1, rowIndex);
        rowIndex++;

        Button saveButton = new Button("Save");
        propertiesPane.add(saveButton, 1, rowIndex);
        rowIndex++;

        // line charts
        VBox chartsPane = new VBox();
        editorPane.setCenter(chartsPane);

        ObservableList<XYChart.Data<Number, Number>> scoreData = addLineChart(chartsPane, "Score");
        ObservableList<XYChart.Data<Number, Number>> statisticsDeadData = addLineChart(chartsPane, "Dead");
        ObservableList<XYChart.Data<Number, Number>> statisticsEatenData = addLineChart(chartsPane, "Eaten");

        // actions
        AtomicBoolean training = new AtomicBoolean(false);
        startTrainButton.addEventHandler(ActionEvent.ACTION, event -> {
            training.set(true);
            startTrainButton.setDisable(true);
            stopTrainButton.setDisable(false);
            new Thread(() -> {
                while (training.get()) {
                    DeeplearningSnakeController controller = deeplearningControllerProperty.get();
                    double score = controller.train(1, teacherControllerProperty.get());
                    DeeplearningSnakeController.Statistics statistics = controller.test();
                    Platform.runLater(() -> {
                        int epoch = epochProperty.get();
                        scoreData.add(new XYChart.Data<>(epoch, score));
                        statisticsDeadData.add(new XYChart.Data<>(epoch, statistics.dead));
                        statisticsEatenData.add(new XYChart.Data<>(epoch, statistics.eaten));
                        if (epoch % 10000 == 0) {
                            reduceData(scoreData, 100);
                            reduceData(statisticsDeadData, 100);
                            reduceData(statisticsEatenData, 100);
                        } else if (epoch % 1000 == 0) {
                            reduceData(scoreData, 10);
                            reduceData(statisticsDeadData, 10);
                            reduceData(statisticsEatenData, 10);
                        }

                        epochProperty.setValue(epoch + 1);
                    });
                }
                Platform.runLater(() -> {
                    startTrainButton.setDisable(false);
                    stopTrainButton.setDisable(true);
                });
            }).start();
        });
        stopTrainButton.addEventHandler(ActionEvent.ACTION, event -> {
            training.set(false);
        });
        saveButton.addEventHandler(ActionEvent.ACTION, event -> {
            saveButton.setDisable(true);
            DeeplearningSnakeController controller = deeplearningControllerProperty.get();
            controller.save();
            saveButton.setDisable(false);
        });

        deeplearningControllerProperty.addListener((observable, oldValue, newController) -> {
            nameLabel.setText(newController.toString());
            epochProperty.setValue(0);
            scoreData.clear();
        });

        listView.getSelectionModel().select(0);

        return masterDetailPane;
    }

    private void reduceData(ObservableList<XYChart.Data<Number, Number>> data, int reduceToStep) {
        Iterator<XYChart.Data<Number, Number>> iterator = data.iterator();
        Double maxYValue = null;
        while (iterator.hasNext()) {
            XYChart.Data<Number, Number> singleData = iterator.next();
            double yValue = singleData.getYValue().doubleValue();
            maxYValue = maxYValue == null ? yValue : Math.max(maxYValue, yValue);
            if ((int)singleData.getXValue() % reduceToStep == 0) {
                singleData.setYValue(maxYValue);
                maxYValue = null;
            } else {
                iterator.remove();
            }
        }
    }

    @NotNull
    private ObservableList<XYChart.Data<Number, Number>> addLineChart(VBox chartsPane, String yAxisLabel) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Epoch");
        xAxis.setTickUnit(1.0);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yAxisLabel);

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setCreateSymbols(false);
        lineChart.setLegendVisible(false);
        lineChart.setMaxHeight(200);
        chartsPane.getChildren().add(lineChart);

        ObservableList<XYChart.Series<Number, Number>> series = FXCollections.observableArrayList();
        lineChart.dataProperty().set(series);

        ObservableList<XYChart.Data<Number, Number>> data = FXCollections.observableArrayList();
        series.add(new XYChart.Series<>(data));

        return data;
    }

    private Node createPropertiesPane() {
        GridPane propertiesPane = new GridPane();
        int rowIndex = 0;

        addLabel(propertiesPane, rowIndex++, "Status:", statusProperty);
        addLabel(propertiesPane, rowIndex++, "Length:", lengthProperty, integerFormat);
        addLabel(propertiesPane, rowIndex++, "Steps:", stepsProperty, integerFormat);
        addLabel(propertiesPane, rowIndex++, "Hunger:", hungerProperty, integerFormat);

        return propertiesPane;
    }

    private Label addLabel(GridPane gridPane, int rowIndex, String label) {
        gridPane.add(new Label(label), 0, rowIndex);
        Label control = new Label();
        gridPane.add(control, 1, rowIndex);
        return control;
    }

    private Label addLabel(GridPane gridPane, int rowIndex, String label, StringProperty property) {
        Label control = addLabel(gridPane, rowIndex, label);
        Bindings.bindBidirectional(control.textProperty(), property);
        return control;
    }

    private Label addLabel(GridPane gridPane, int rowIndex, String label, Property property, Format format) {
        Label control = addLabel(gridPane, rowIndex, label);
        Bindings.bindBidirectional(control.textProperty(), property, format);
        return control;
    }

    private TextField addTextField(GridPane gridPane, int rowIndex, String label, StringProperty property) {
        gridPane.add(new Label(label), 0, rowIndex);
        TextField control = new TextField();
        gridPane.add(control, 1, rowIndex);
        Bindings.bindBidirectional(control.textProperty(), statusProperty);
        return control;
    }

    private <T> ComboBox<T> addComboBox(GridPane gridPane, int rowIndex, String label, ListProperty<T> listProperty, ObjectProperty<T> elementProperty) {
        gridPane.add(new Label(label), 0, rowIndex);
        ComboBox<T> control = new ComboBox<>();
        gridPane.add(control, 1, rowIndex);
        Bindings.bindBidirectional(control.itemsProperty(), listProperty);
        control.valueProperty().bindBidirectional(elementProperty);
        elementProperty.setValue(listProperty.get(0));
        return control;
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
