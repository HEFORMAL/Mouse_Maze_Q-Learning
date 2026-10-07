package org.example;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Maze {
    private static final Action[] ALL_ACTIONS = Action.values();

    private final int width;
    private final int height;

    private final Cell[][] cells;

    private final Random random = new Random();

    private Cell entrance;
    private Cell exit;

    private Action entranceSide;
    private Action exitSide;

    private final boolean[][] water;
    private final boolean[][] trap;
    private final boolean[][] waterAvailable;

    // Номера клеток с водой: i-я клетка с водой даёт i-й бит в маске выпитой воды
    private final List<int[]> waterCells = new ArrayList<>();

    public Maze(int width, int height) {
        this(width, height, 5, 5); // 5 клеток воды 5 ловушек
    }

    public Maze(int width, int height, int waterCount, int trapCount) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }

        this.width = width;
        this.height = height;

        this.cells = new Cell[height][width];
        this.water = new boolean[height][width];
        this.trap = new boolean[height][width];
        this.waterAvailable = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cells[y][x] = new Cell(x, y);
            }
        }

        generate();
        createRandomEntranceAndExit();
        placeWaterAndTraps(waterCount, trapCount);
        resetEpisode();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Cell getCell(int x, int y) {
        if (!isInside(x, y)) {
            throw new IndexOutOfBoundsException("Cell is outside the maze");
        }

        return cells[y][x];
    }

    public Cell getEntrance() {
        return entrance;
    }

    public Cell getExit() {
        return exit;
    }

    public Action getEntranceSide() {
        return entranceSide;
    }

    public Action getExitSide() {
        return exitSide;
    }

    public int getEntranceX() {
        return entrance.getX();
    }

    public int getEntranceY() {
        return entrance.getY();
    }

    public int getExitX() {
        return exit.getX();
    }

    public int getExitY() {
        return exit.getY();
    }

    public boolean isInside(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean isEntranceCell(int x, int y) {
        return entrance.getX() == x && entrance.getY() == y;
    }

    public boolean isExitCell(int x, int y) {
        return exit.getX() == x && exit.getY() == y;
    }

    public boolean isFinished(int x, int y) {
        return isExitCell(x, y);
    }

    /**
     * Проверяет, может ли объект сделать шаг из клетки (x, y)
     * в направлении action, оставаясь внутри лабиринта.
     */
    public boolean canMove(int x, int y, Action action) {
        if (!isInside(x, y)) {
            return false;
        }

        if (cells[y][x].hasWall(action)) {
            return false;
        }

        int newX = x + action.getDx();
        int newY = y + action.getDy();

        return isInside(newX, newY);
    }

    /**
     * Проверяет, может ли объект выйти из лабиринта через выход.
     * Этот метод можно будет использовать позже, если мы захотим,
     * чтобы мышь реально выходила за пределы лабиринта.
     */
    public boolean canMoveOrExit(int x, int y, Action action) {
        if (!isInside(x, y)) {
            return false;
        }

        if (cells[y][x].hasWall(action)) {
            return false;
        }

        int newX = x + action.getDx();
        int newY = y + action.getDy();

        if (isInside(newX, newY)) {
            return true;
        }

        return isExitMove(x, y, action);
    }

    public boolean isExitMove(int x, int y, Action action) {
        return isExitCell(x, y) && action == exitSide;
    }

    private void generate() {
        ArrayDeque<Cell> stack = new ArrayDeque<>();

        Cell start = cells[0][0];
        start.setVisited(true);
        stack.push(start);

        while (!stack.isEmpty()) {
            Cell current = stack.peek();

            List<Action> unvisitedDirections = getUnvisitedDirections(current);

            if (unvisitedDirections.isEmpty()) {
                stack.pop();
            } else {
                Action action = unvisitedDirections.get(
                        random.nextInt(unvisitedDirections.size())
                );

                Cell neighbor = getNeighbor(current, action);

                removeWallBetween(current, neighbor, action);

                neighbor.setVisited(true);
                stack.push(neighbor);
            }
        }
    }

    private List<Action> getUnvisitedDirections(Cell cell) {
        List<Action> directions = new ArrayList<>();

        for (Action action : ALL_ACTIONS) {
            Cell neighbor = getNeighbor(cell, action);

            if (neighbor != null && !neighbor.isVisited()) {
                directions.add(action);
            }
        }

        return directions;
    }

    private Cell getNeighbor(Cell cell, Action action) {
        int x = cell.getX() + action.getDx();
        int y = cell.getY() + action.getDy();

        if (!isInside(x, y)) {
            return null;
        }

        return cells[y][x];
    }

    private void removeWallBetween(Cell current, Cell neighbor, Action action) {
        current.removeWall(action);
        neighbor.removeWall(action.opposite());
    }

    private void createRandomEntranceAndExit() {
        // Особый случай: лабиринт 1 x 1
        if (width == 1 && height == 1) {
            entrance = cells[0][0];
            exit = entrance;

            entranceSide = Action.LEFT;
            exitSide = Action.RIGHT;

            entrance.removeWall(entranceSide);
            exit.removeWall(exitSide);

            return;
        }

        // Пытаемся выбрать вход и выход на противоположных сторонах
        for (int attempt = 0; attempt < 100; attempt++) {
            entranceSide = ALL_ACTIONS[random.nextInt(ALL_ACTIONS.length)];
            exitSide = entranceSide.opposite();

            entrance = randomCellOnSide(entranceSide);
            exit = randomCellOnSide(exitSide);

            if (entrance != exit) {
                break;
            }
        }

        // Если вдруг не получилось выбрать разные клетки,
        // выбираем любые разные клетки на границе лабиринта
        if (entrance == exit) {
            chooseAnyDifferentBoundaryEntranceExit();
        }

        entrance.removeWall(entranceSide);
        exit.removeWall(exitSide);
    }

    private Cell randomCellOnSide(Action side) {
        switch (side) {
            case UP:
                return cells[0][random.nextInt(width)];

            case DOWN:
                return cells[height - 1][random.nextInt(width)];

            case LEFT:
                return cells[random.nextInt(height)][0];

            case RIGHT:
                return cells[random.nextInt(height)][width - 1];

            default:
                throw new IllegalStateException("Unexpected side: " + side);
        }
    }

    private void chooseAnyDifferentBoundaryEntranceExit() {
        List<Cell> boundaryCells = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    boundaryCells.add(cells[y][x]);
                }
            }
        }

        entrance = boundaryCells.get(random.nextInt(boundaryCells.size()));

        do {
            exit = boundaryCells.get(random.nextInt(boundaryCells.size()));
        } while (exit == entrance);

        entranceSide = randomOuterSide(entrance);
        exitSide = randomOuterSide(exit);
    }

    private Action randomOuterSide(Cell cell) {
        List<Action> possibleSides = new ArrayList<>();

        int x = cell.getX();
        int y = cell.getY();

        if (y == 0) {
            possibleSides.add(Action.UP);
        }

        if (y == height - 1) {
            possibleSides.add(Action.DOWN);
        }

        if (x == 0) {
            possibleSides.add(Action.LEFT);
        }

        if (x == width - 1) {
            possibleSides.add(Action.RIGHT);
        }

        if (possibleSides.isEmpty()) {
            throw new IllegalStateException("Cell is not on the boundary");
        }

        return possibleSides.get(random.nextInt(possibleSides.size()));
    }

    public void print() {
        // Верхняя граница
        for (int x = 0; x < width; x++) {
            System.out.print("+");
            System.out.print(cells[0][x].hasWall(Action.UP) ? "---" : "   ");
        }
        System.out.println("+");

        for (int y = 0; y < height; y++) {
            // Тело строки
            for (int x = 0; x < width; x++) {
                System.out.print(cells[y][x].hasWall(Action.LEFT) ? "|" : " ");
                System.out.print(getCellContent(x, y));
            }

            System.out.println(cells[y][width - 1].hasWall(Action.RIGHT) ? "|" : " ");

            // Нижняя граница текущей строки
            for (int x = 0; x < width; x++) {
                System.out.print("+");
                System.out.print(cells[y][x].hasWall(Action.DOWN) ? "---" : "   ");
            }

            System.out.println("+");
        }
    }

    private String getCellContent(int x, int y) {
        boolean isEntrance = isEntranceCell(x, y);
        boolean isExit = isExitCell(x, y);

        if (isEntrance && isExit) {
            return "S/F";
        }

        if (isEntrance) {
            return " S ";
        }

        if (isExit) {
            return " F ";
        }

        return "   ";
    }
    private void placeWaterAndTraps(int waterCount, int trapCount) {
        int maxItems = Math.max(0, width * height - 2);

        waterCount = Math.min(waterCount, maxItems / 2);
        trapCount = Math.min(trapCount, maxItems / 2);

        for (int i = 0; i < waterCount; i++) {
            int[] pos = randomFreeCell();
            water[pos[1]][pos[0]] = true;
            waterCells.add(pos);
        }

        for (int i = 0; i < trapCount; i++) {
            int[] pos = randomFreeCell();
            trap[pos[1]][pos[0]] = true;
        }
    }

    private int[] randomFreeCell() {
        while (true) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);

            if (isEntranceCell(x, y) || isExitCell(x, y)) {
                continue;
            }

            if (water[y][x] || trap[y][x]) {
                continue;
            }

            return new int[]{x, y};
        }
    }

    public boolean isWater(int x, int y) {
        return water[y][x];
    }

    public boolean isTrap(int x, int y) {
        return trap[y][x];
    }

    /**
     * Мышь пытается попить воду.
     * Возвращает true, если вода была и она выпита.
     */
    public boolean drinkWater(int x, int y) {
        if (waterAvailable[y][x]) {
            waterAvailable[y][x] = false;
            return true;
        }

        return false;
    }
    public boolean isWaterAvailable(int x, int y) {
        return waterAvailable[y][x];
    }

    /** Сколько клеток с водой (от этого зависит, сколько бывает масок выпитой воды). */
    public int getWaterCount() {
        return waterCells.size();
    }

    /**
     * Какая вода уже выпита в этом эпизоде: бит i = 1, если выпита i-я клетка с водой.
     * Мышь должна это «помнить», иначе для неё «вода есть» и «вода выпита» — одна и та же ситуация,
     * и она ходит к уже выпитой воде по кругу.
     */
    public int getWaterMask() {
        int mask = 0;
        for (int i = 0; i < waterCells.size(); i++) {
            int[] pos = waterCells.get(i);
            if (!waterAvailable[pos[1]][pos[0]]) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    /**
     * В начале нового эпизода вода появляется снова.
     */
    public void resetEpisode() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                waterAvailable[y][x] = water[y][x];
            }
        }
    }
}