package lab2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/* Стратегии выбора следующего потока.
 * FirstAlive — этап 1, Replay — этап 2, RandomPick — этап 3, Dfs — этап 4 (ограничение bound — этап 5).
 * enabled — номера потоков, которые могут сделать шаг, по возрастанию; last — прошлый выбранный (-1 в начале). */
public final class Strategies {
    private Strategies() { }

    /** Всегда первый поток из enabled. */
    public static final class FirstAlive implements Strategy {
        public int pick(List<Integer> enabled, int last) {
            return enabled.getFirst();
        }
    }

    /** Проигрывает трассу: на i-м решении выбирает поток trace[i] ('A' -> 0, 'B' -> 1). */
    public static final class Replay implements Strategy {
        private final String trace;
        private int position;

        public Replay(String trace) {
            this.trace = trace;
        }

        /** Сбросить позицию в трассе перед прогоном. */
        public void beginRun() {
            position = 0;
        }

        public int pick(List<Integer> enabled, int last) {
            if(position >= trace.length()){
                throw new IllegalStateException("The trace ended before the run was complete");
            }

            int nextThread = trace.charAt(position) - 'A';
            if(!enabled.contains(nextThread)){
                throw new IllegalStateException("Thread " + trace.charAt(position) + " is currently unavailable");
            }
            position++;
            return nextThread;
        }
    }

    /** Случайный поток: enabled.get(rnd.nextInt(enabled.size())), rnd = new java.util.Random(seed). */
    public static final class RandomPick implements Strategy {
        private final Random random;

        public RandomPick(long seed) {
            this.random = new Random(seed);
        }

        public int pick(List<Integer> enabled, int last) {
            return enabled.get(random.nextInt(enabled.size()));
        }
    }

    /** Stateless DFS по дереву выборов: каждый прогон с нуля, повторяет префикс прошлого и берёт следующий вариант.
     *  bound — максимум вытеснений (этап 5); для dfs runner передаёт Integer.MAX_VALUE. */
    public static final class Dfs implements Strategy {
        private final int bound;

        private final List<List<Integer>> options = new ArrayList<>();
        private final List<Integer> selected = new ArrayList<>();

        private int position;

        public Dfs(int bound) {
            this.bound = bound;
        }

        /** Подготовиться к новому прогону (позиция в пути, счётчик вытеснений). */
        public void beginRun() {
            position = 0;
        }

        public int pick(List<Integer> enabled, int last) {
            if (position == options.size()) {
                options.add(new ArrayList<>(enabled));
                selected.add(0);
            }

            List<Integer> variants = options.get(position);
            int variantIndex = selected.get(position);
            int next = variants.get(variantIndex);

            position++;

            return next;
        }

        /** Выбрать следующий путь; false — дерево обойдено. */
        public boolean endRun() {
            while (options.size() > position) {
                options.removeLast();
                selected.removeLast();
            }

            while (!options.isEmpty()) {
                int step = options.size() - 1;
                int nextChoice = selected.get(step) + 1;

                if (nextChoice < options.get(step).size()) {
                    selected.set(step, nextChoice);
                    return true;
                }

                options.removeLast();
                selected.removeLast();
            }

            return false;
        }
    }
}
