package org.example;

public class Cell {
    private final int x; //Мы сделали их final, потому что клетка в лабиринте не должна двигаться.
    private final int y;

    private boolean visited;

    private boolean wallUp; //Стены
    private boolean wallRight;
    private boolean wallDown;
    private boolean wallLeft;

    public Cell(int x, int y) { //Создаём клетку
        this.x = x;
        this.y = y;
        this.visited = false;

        this.wallUp = true;
        this.wallRight = true;
        this.wallDown = true;
        this.wallLeft = true;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public boolean hasWall(Action action) { //Проверяем шде стены
        switch (action) {
            case UP:
                return wallUp;

            case DOWN:
                return wallDown;

            case LEFT:
                return wallLeft;

            case RIGHT:
                return wallRight;

            default:
                throw new IllegalStateException("Unexpected action");
        }
    }

    public void setWall(Action action, boolean wall) {
        switch (action) {
            case UP:
                this.wallUp = wall;
                break;

            case DOWN:
                this.wallDown = wall;
                break;

            case LEFT:
                this.wallLeft = wall;
                break;

            case RIGHT:
                this.wallRight = wall;
                break;

            default:
                throw new IllegalStateException("Unexpected action");
        }
    }

    public void removeWall(Action action) { //e,bhftv
        setWall(action, false);
    }

    @Override
    public String toString() {
        return "Cell{x=" + x + ", y=" + y + "}";
    }
}