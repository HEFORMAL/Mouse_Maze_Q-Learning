package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Q-таблица и Q-обучение.
 * Состояние мыши — клетка (x, y) и маска выпитой воды (какая вода уже выпита в этом эпизоде),
 * поэтому таблица: q[маска][y][x][действие]. При 5 клетках с водой масок 2^5 = 32.
 */
public class QLearning {
    private final double[][][][] q;

    private double alpha;
    private double gamma;
    private double epsilon;

    private final Random random = new Random();

    public QLearning(int width, int height, int waterCount, double alpha, double gamma, double epsilon) {
        this.alpha = alpha;
        this.gamma = gamma;
        this.epsilon = epsilon;

        this.q = new double[1 << waterCount][height][width][Action.values().length];

        // Optimistic initialization
        for (double[][][] byMask : q) {
            for (double[][] row : byMask) {
                for (double[] cell : row) {
                    java.util.Arrays.fill(cell, 10.0);
                }
            }
        }
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    public Action chooseAction(int x, int y, int mask) {
        if (random.nextDouble() < epsilon) {
            Action[] actions = Action.values();
            return actions[random.nextInt(actions.length)];
        }

        return bestAction(x, y, mask);
    }

    /**
     * Лучшее действие. Если несколько действий имеют одинаковое
     * максимальное значение, выбирает случайно среди них.
     */
    public Action bestAction(int x, int y, int mask) {
        double bestValue = maxValue(x, y, mask);

        // Собираем все действия с максимальным значением
        List<Action> bestActions = new ArrayList<>();
        for (Action action : Action.values()) {
            double value = q[mask][y][x][action.ordinal()];
            // Сравниваем с небольшим допуском для float
            if (Math.abs(value - bestValue) < 0.0001) {
                bestActions.add(action);
            }
        }

        // Выбираем случайно среди лучших
        return bestActions.get(random.nextInt(bestActions.size()));
    }

    public double maxValue(int x, int y, int mask) {
        double max = Double.NEGATIVE_INFINITY;

        for (double value : q[mask][y][x]) {
            if (value > max) {
                max = value;
            }
        }

        return max;
    }

    /**
     * Q[s][a] += α · (награда + γ · max Q[s'] − Q[s][a]).
     * Удар о стену — обычный шаг: мышь осталась в той же клетке, её будущее — лучшее число этой клетки.
     * Будущего нет только у выхода (done).
     */
    public void learn(int x, int y, int mask, Action action, double reward,
                      int newX, int newY, int newMask, boolean done) {

        double oldValue = q[mask][y][x][action.ordinal()];

        double futureValue = done ? 0 : maxValue(newX, newY, newMask);

        double target = reward + gamma * futureValue;

        q[mask][y][x][action.ordinal()] = oldValue + alpha * (target - oldValue);
    }

    public double[][][][] snapshot() {
        double[][][][] copy = new double[q.length][][][];

        for (int m = 0; m < q.length; m++) {
            copy[m] = new double[q[m].length][][];
            for (int y = 0; y < q[m].length; y++) {
                copy[m][y] = new double[q[m][y].length][];
                for (int x = 0; x < q[m][y].length; x++) {
                    copy[m][y][x] = q[m][y][x].clone();
                }
            }
        }

        return copy;
    }

    public void restore(double[][][][] copy) {
        for (int m = 0; m < q.length; m++) {
            for (int y = 0; y < q[m].length; y++) {
                for (int x = 0; x < q[m][y].length; x++) {
                    q[m][y][x] = copy[m][y][x].clone();
                }
            }
        }
    }
}
