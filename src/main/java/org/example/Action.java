package org.example;

public enum Action {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    private final int dx;
    private final int dy;

    Action(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    public Action opposite() {
        switch (this) {
            case UP:
                return DOWN;

            case DOWN:
                return UP;

            case LEFT:
                return RIGHT;

            case RIGHT:
                return LEFT;

            default:
                throw new IllegalStateException("Неожиданное направление");
        }
    }
}