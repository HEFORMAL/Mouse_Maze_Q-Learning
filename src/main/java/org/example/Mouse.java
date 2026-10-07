package org.example;

import java.util.Random;

public class Mouse {
    private int x;
    private int y;

    private final String icon = "\uD83D\uDC2D";

    private final Random random = new Random();

    public Mouse(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Mouse(Maze maze) {
        setToEntrance(maze);
    }

    public void setToEntrance(Maze maze) {
        this.x = maze.getEntranceX();
        this.y = maze.getEntranceY();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getIcon() {
        return icon;
    }

    /**
     * Движение мыши с учётом лабиринта.
     * Если в направлении есть стена, мышь не двигается.
     */
    public void move(Action action, Maze maze) {
        if (maze.canMove(x, y, action)) {
            x += action.getDx();
            y += action.getDy();
        }
    }

    /**
     * Случайный шаг.
     * Это пригодится позже, когда начнём делать автономное движение.
     */
    public void randomStep(Maze maze) {
        Action[] actions = Action.values();
        Action action = actions[random.nextInt(actions.length)];
        move(action, maze);
    }

    /**
     * Проверяем, дошла ли мышь до выхода.
     */
    public boolean reachedExit(Maze maze) {
        return maze.isExitCell(x, y);
    }
}