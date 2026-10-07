package org.example;

/** Консольное обучение: статистика по ходу обучения, карта политики и проверка обученной мыши. */
public class Trainer {
    public static void main(String[] args) {
        Maze maze = new Maze(10, 7);

        System.out.println("Вход: (" + maze.getEntranceX() + ", " + maze.getEntranceY() + ")");
        System.out.println("Выход: (" + maze.getExitX() + ", " + maze.getExitY() + ")");
        System.out.println();

        QLearning brain = Training.newBrain(maze);
        Mouse mouse = new Mouse(maze);

        int episodes = 15000;
        int maxSteps = 500;
        double epsilon = 1.0;
        int successWindow = 0;

        for (int episode = 1; episode <= episodes; episode++) {
            brain.setEpsilon(epsilon);
            epsilon = Math.max(Training.MIN_EPSILON, epsilon * Training.EPSILON_DECAY);

            if (Training.episode(maze, mouse, brain, maxSteps)) {
                successWindow++;
            }

            if (episode % 1000 == 0) {
                brain.setEpsilon(0);
                int evalSteps = Training.evaluate(maze, mouse, brain, maxSteps);
                System.out.println("Эпизод " + episode
                        + " | успехов за 1000: " + successWindow
                        + " | по выученному: " + (evalSteps > 0 ? evalSteps + " шагов" : "не доходит")
                        + " | epsilon: " + String.format("%.3f", epsilon));
                successWindow = 0;
            }
        }

        brain.setEpsilon(0);

        System.out.println();
        printPolicy(brain, maze);
        System.out.println();
        printValues(brain, maze);

        System.out.println();
        System.out.println("----- Проверка обученной мыши -----");

        maze.resetEpisode();
        mouse.setToEntrance(maze);

        int steps = 0;
        double totalReward = 0;
        int waterDrunk = 0;
        int shocks = 0;

        while (!mouse.reachedExit(maze) && steps < maxSteps) {
            int x = mouse.getX();
            int y = mouse.getY();

            Action action = brain.bestAction(x, y, maze.getWaterMask());

            boolean moved = maze.canMove(x, y, action);
            if (moved) {
                mouse.move(action, maze);
            }

            double reward = Training.reward(maze, mouse, moved);
            if (reward == Reward.SHOCK) {
                shocks++;
            } else if (reward == Reward.WATER) {
                waterDrunk++;
            }

            totalReward += reward;
            steps++;
        }

        System.out.println("Длина пути: " + steps);
        System.out.println("Дошла до выхода: " + mouse.reachedExit(maze));
        System.out.println("Выпила воды: " + waterDrunk);
        System.out.println("Получила ударов током: " + shocks);
        System.out.println("Сумма выигрыша/проигрыша: " + totalReward);
    }

    /** Карта политики в начале эпизода (вся вода ещё не выпита — маска 0). */
    private static void printPolicy(QLearning brain, Maze maze) {
        System.out.println("----- Карта политики (вода ещё не выпита) -----");

        for (int y = 0; y < maze.getHeight(); y++) {
            StringBuilder sb = new StringBuilder();

            for (int x = 0; x < maze.getWidth(); x++) {
                if (maze.isEntranceCell(x, y)) {
                    sb.append("S ");
                    continue;
                }

                if (maze.isExitCell(x, y)) {
                    sb.append("F ");
                    continue;
                }

                switch (brain.bestAction(x, y, 0)) {
                    case UP -> sb.append("↑ ");
                    case DOWN -> sb.append("↓ ");
                    case LEFT -> sb.append("← ");
                    case RIGHT -> sb.append("→ ");
                }
            }

            System.out.println(sb);
        }
    }

    private static void printValues(QLearning brain, Maze maze) {
        System.out.println("----- max Q по клеткам (вода ещё не выпита) -----");

        for (int y = 0; y < maze.getHeight(); y++) {
            StringBuilder sb = new StringBuilder();

            for (int x = 0; x < maze.getWidth(); x++) {
                sb.append(String.format("%6.1f ", brain.maxValue(x, y, 0)));
            }

            System.out.println(sb);
        }
    }
}
