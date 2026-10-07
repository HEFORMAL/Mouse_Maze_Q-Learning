package org.example;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MazeApp extends Application {
    private static final int CELL_SIZE = 40;

    private static final String WATER_ICON = "\uD83D\uDCA7";
    private static final String TRAP_ICON = "\u26A1";
    private static final String CHEESE_ICON = "\uD83E\uDDC0";

    private Maze maze;
    private Mouse mouse;
    private QLearning brain;

    private Canvas canvas;
    private Label statusLabel;

    private Button trainButton;
    private Button runButton;
    private Button newMazeButton;

    private Timeline runTimeline;
    private int runSteps;

    @Override
    public void start(Stage stage) {
        createGame();

        canvas = new Canvas(
                maze.getWidth() * CELL_SIZE,
                maze.getHeight() * CELL_SIZE
        );

        statusLabel = new Label("Нажми \"Обучить мышь\"");

        trainButton = new Button("Обучить мышь");
        runButton = new Button("Запустить мышь");
        newMazeButton = new Button("Новый лабиринт");

        runButton.setDisable(true);

        trainButton.setOnAction(e -> trainMouse());
        runButton.setOnAction(e -> startRun());
        newMazeButton.setOnAction(e -> {
            stopRun();
            createGame();
            runButton.setDisable(true);
            statusLabel.setText("Создан новый лабиринт. Обучи мышь!");
            draw();
        });

        HBox buttons = new HBox(10, trainButton, runButton, newMazeButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(buttons);
        root.setCenter(canvas);
        root.setBottom(statusLabel);

        BorderPane.setAlignment(statusLabel, Pos.CENTER);
        statusLabel.setPadding(new Insets(5));

        Scene scene = new Scene(root);

        stage.setTitle("Мышь и лабиринт: Q-обучение");
        stage.setScene(scene);
        stage.show();

        draw();
    }

    private void createGame() {
        maze = new Maze(10, 7);
        mouse = new Mouse(maze);
        brain = Training.newBrain(maze);
    }

    private void trainMouse() {
        stopRun();
        trainButton.setDisable(true);
        runButton.setDisable(true);
        // пока идёт обучение, лабиринт менять нельзя: фоновый поток работает с maze, mouse и brain
        newMazeButton.setDisable(true);
        statusLabel.setText("Обучение... (это займёт несколько секунд)");

        Maze trainMaze = maze;
        Mouse trainMouse = mouse;
        QLearning trainBrain = brain;

        Thread thread = new Thread(() -> {
            Training.train(trainMaze, trainMouse, trainBrain, 10000, 500);

            Platform.runLater(() -> {
                trainButton.setDisable(false);
                runButton.setDisable(false);
                newMazeButton.setDisable(false);
                draw();
                statusLabel.setText("Обучение завершено! Нажми \"Запустить мышь\"");
            });
        });

        thread.setDaemon(true);
        thread.start();
    }

    private void startRun() {
        stopRun();

        maze.resetEpisode();
        mouse.setToEntrance(maze);
        runSteps = 0;

        statusLabel.setText("Мышь бежит по лабиринту...");
        draw();

        runTimeline = new Timeline(
                new KeyFrame(Duration.millis(150), e -> runStep())
        );
        runTimeline.setCycleCount(Timeline.INDEFINITE);
        runTimeline.play();
    }

    private void runStep() {
        if (mouse.reachedExit(maze)) {
            stopRun();
            statusLabel.setText("Мышь нашла сыр! " + CHEESE_ICON);
            return;
        }

        if (runSteps >= 500) {
            stopRun();
            statusLabel.setText("Мышь заблудилась. Обучи её ещё раз!");
            return;
        }

        int x = mouse.getX();
        int y = mouse.getY();

        // Мышь идёт строго по выученному: лучшее действие для клетки и выпитой воды
        Action action = brain.bestAction(x, y, maze.getWaterMask());

        if (maze.canMove(x, y, action)) {
            mouse.move(action, maze);

            int newX = mouse.getX();
            int newY = mouse.getY();

            if (maze.isTrap(newX, newY)) {
                statusLabel.setText("Бррр! Электротравма " + TRAP_ICON);
            } else if (maze.drinkWater(newX, newY)) {
                statusLabel.setText("Мышь попила воду " + WATER_ICON);
            }
        }

        runSteps++;
        draw();

        if (mouse.reachedExit(maze)) {
            stopRun();
            statusLabel.setText("Мышь нашла сыр! " + CHEESE_ICON);
        }
    }

    private void stopRun() {
        if (runTimeline != null) {
            runTimeline.stop();
            runTimeline = null;
        }
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        g.setFill(Color.WHITE);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFont(Font.font(22));

        // Особые клетки
        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                double px = x * CELL_SIZE;
                double py = y * CELL_SIZE;

                if (maze.isEntranceCell(x, y)) {
                    g.setFill(Color.LIGHTGREEN);
                    g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                }

                if (maze.isExitCell(x, y)) {
                    g.setFill(Color.LIGHTYELLOW);
                    g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                    drawEmoji(g, CHEESE_ICON, x, y);
                } else if (maze.isWaterAvailable(x, y)) {
                    g.setFill(Color.LIGHTBLUE);
                    g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                    drawEmoji(g, WATER_ICON, x, y);
                } else if (maze.isTrap(x, y)) {
                    g.setFill(Color.MISTYROSE);
                    g.fillRect(px, py, CELL_SIZE, CELL_SIZE);
                    drawEmoji(g, TRAP_ICON, x, y);
                }
            }
        }

        // Стены
        g.setStroke(Color.BLACK);
        g.setLineWidth(2.0);

        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                Cell cell = maze.getCell(x, y);

                double px = x * CELL_SIZE;
                double py = y * CELL_SIZE;

                if (cell.hasWall(Action.UP)) {
                    g.strokeLine(px, py, px + CELL_SIZE, py);
                }

                if (cell.hasWall(Action.RIGHT)) {
                    g.strokeLine(px + CELL_SIZE, py, px + CELL_SIZE, py + CELL_SIZE);
                }

                if (cell.hasWall(Action.DOWN)) {
                    g.strokeLine(px, py + CELL_SIZE, px + CELL_SIZE, py + CELL_SIZE);
                }

                if (cell.hasWall(Action.LEFT)) {
                    g.strokeLine(px, py, px, py + CELL_SIZE);
                }
            }
        }

        // Мышь
        drawEmoji(g, mouse.getIcon(), mouse.getX(), mouse.getY());
    }

    private void drawEmoji(GraphicsContext g, String emoji, int x, int y) {
        double centerX = x * CELL_SIZE + CELL_SIZE / 2.0;
        double centerY = y * CELL_SIZE + CELL_SIZE / 2.0;

        g.setFill(Color.BLACK);
        g.fillText(emoji, centerX, centerY);
    }

    public static void main(String[] args) {
        launch(args);
    }
}