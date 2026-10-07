package org.example;

/**
 * Общий цикл обучения для окна (MazeApp) и консоли (Trainer).
 * Раньше он был скопирован в оба класса; теперь исправлять нужно в одном месте.
 */
public class Training {
    public static final double ALPHA = 0.1;
    public static final double GAMMA = 0.99;   // 0.95 мало: ловушка −30 на единственном пути выглядит хуже, чем бродить вечно
    public static final double MIN_EPSILON = 0.1;
    public static final double EPSILON_DECAY = 0.9995;

    private Training() {
    }

    public static QLearning newBrain(Maze maze) {
        return new QLearning(maze.getWidth(), maze.getHeight(), maze.getWaterCount(), ALPHA, GAMMA, 1.0);
    }

    /** Награда за шаг: стена, выход, ловушка, вода, обычный шаг. */
    public static double reward(Maze maze, Mouse mouse, boolean moved) {
        if (!moved) {
            return Reward.WALL;
        }
        if (mouse.reachedExit(maze)) {
            return Reward.CHEESE;
        }
        if (maze.isTrap(mouse.getX(), mouse.getY())) {
            return Reward.SHOCK;
        }
        if (maze.drinkWater(mouse.getX(), mouse.getY())) {
            return Reward.WATER;
        }
        return Reward.STEP;
    }

    /** Один эпизод: мышь от входа до выхода или до лимита шагов. Возвращает true, если дошла. */
    public static boolean episode(Maze maze, Mouse mouse, QLearning brain, int maxSteps) {
        maze.resetEpisode();
        mouse.setToEntrance(maze);

        boolean done = false;
        int steps = 0;

        while (!done && steps < maxSteps) {
            int x = mouse.getX();
            int y = mouse.getY();
            int mask = maze.getWaterMask();

            Action action = brain.chooseAction(x, y, mask);

            boolean moved = maze.canMove(x, y, action);
            if (moved) {
                mouse.move(action, maze);
            }

            double reward = reward(maze, mouse, moved);
            done = moved && mouse.reachedExit(maze);

            brain.learn(x, y, mask, action, reward, mouse.getX(), mouse.getY(), maze.getWaterMask(), done);

            steps++;
        }

        return done;
    }

    /** Обучение: episodes эпизодов, доля случайных шагов уменьшается от 1 до MIN_EPSILON. */
    public static void train(Maze maze, Mouse mouse, QLearning brain, int episodes, int maxSteps) {
        double epsilon = 1.0;

        for (int i = 1; i <= episodes; i++) {
            brain.setEpsilon(epsilon);
            epsilon = Math.max(MIN_EPSILON, epsilon * EPSILON_DECAY);
            episode(maze, mouse, brain, maxSteps);
        }

        brain.setEpsilon(0);
    }

    /** Мышь идёт строго по выученному (без случайных шагов). Возвращает число шагов или −1, если не дошла. */
    public static int evaluate(Maze maze, Mouse mouse, QLearning brain, int maxSteps) {
        maze.resetEpisode();
        mouse.setToEntrance(maze);

        int steps = 0;
        while (!mouse.reachedExit(maze) && steps < maxSteps) {
            int x = mouse.getX();
            int y = mouse.getY();
            Action action = brain.bestAction(x, y, maze.getWaterMask());
            boolean moved = maze.canMove(x, y, action);
            if (moved) {
                mouse.move(action, maze);
                reward(maze, mouse, true);   // выпить воду, если она есть
            }
            steps++;
        }

        return mouse.reachedExit(maze) ? steps : -1;
    }
}
