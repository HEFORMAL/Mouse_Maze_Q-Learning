package org.example;

public class State {
    private int x;
    private int y;
    private final String icon;

    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public String getIcon() {
        return icon;
    }
    public State(int x, int y) {
        this.x = x;
        this.y = y;
        this.icon = "\uD83D\uDC2D";
    }

    public void setCoords(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public int[][] getCoords() {
        return new int[][]{{x, y}};
    }

}
