package lab2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.BooleanSupplier;

/* Этап 1: планировщик, который ведёт настоящие потоки строго по одному (передача хода через семафоры).
 * Этап 2: обнаружение deadlock в главном цикле и корректное завершение застрявших потоков.
 * Внутреннее устройство (свои классы, семафоры, вспомогательные методы для Shared и SchedLock) придумываете сами. */
public final class Scheduler {

    private final List<Worker> workers = new ArrayList<>();
    private final List<Thread> threads = new ArrayList<>();

    private final Semaphore schedulerSignal = new Semaphore(0);

    private static final ThreadLocal<Worker> currentWorker = new ThreadLocal<>();
    private static final ThreadLocal<Scheduler> currentScheduler = new ThreadLocal<>();

    private final StringBuilder trace = new StringBuilder();

    private final List<SchedLock> locks = new ArrayList<>();

    private volatile boolean aborted;

    /** Если true, каждая операция с общими данными добавляет строку в log. Выставляет runner. */
    boolean logging;
    /** Лог прогона: строки "<поток>  <операция>", например "A  lock mu", "A  balance -> 100". */
    final List<String> log = new ArrayList<>();
    /** null, если deadlock не было; иначе "held={a:A, b:B}" (этап 2). */
    String deadlock;

    public Scheduler() { }

    /** Зарегистрировать поток. Потоки получают буквы A, B, C… в порядке вызова spawn. */
    public void spawn(Runnable body) {
        workers.add(new Worker(body));
    }

    /** Запустить потоки и вести их по одному: на каждой паузе спросить strategy.pick(enabled, last). */
    public void run(Strategy strategy) {
        for (Worker worker : workers) {
            Thread thread = new Thread(() -> {
                currentScheduler.set(this);
                currentWorker.set(worker);

                try {
                    worker.body.run();
                } catch (AbortedException ignored){

                } finally {
                    worker.state = State.FINISHED;
                    currentWorker.remove();
                    currentScheduler.remove();
                    schedulerSignal.release();
                }
            });

            threads.add(thread);

            worker.state = State.RUNNING;
            thread.start();

            schedulerSignal.acquireUninterruptibly();
        }

        int last = -1;
        List<Integer> enabled = new ArrayList<>();
        while(true){
            enabled.clear();
            boolean allThreadIsFinished = true;
            for (int i = 0; i < workers.size(); i++) {
                Worker worker = workers.get(i);

                if(worker.state != State.FINISHED){
                    allThreadIsFinished = false;
                }

                if(worker.state == State.WAITING_TURN
                        || (worker.state == State.WAITING_CONDITION && worker.waitingCondition.getAsBoolean())
                        || (worker.state == State.WAITING_LOCK && worker.waitingLock.isFree())){
                    enabled.add(i);
                }
            }

            if(allThreadIsFinished) break;

            if (enabled.isEmpty()) {
                handleDeadlock();
                break;
            }


            int nextThread = strategy.pick(enabled, last);
            Worker nextWorker = workers.get(nextThread);

            trace.append((char) ('A' + nextThread));

            nextWorker.state = State.RUNNING;
            nextWorker.permit.release();

            schedulerSignal.acquireUninterruptibly();

            last = nextThread;
        }

        for (Thread thread : threads){
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    /** Трасса прогона: буквы потоков, выбранных во всех решениях подряд, например "AAAABB". */
    public String traceString() {
        return trace.toString();
    }

    /** window() из тестовых программ: пауза перед следующей операцией с общими данными обязательна. */
    public static void point() {
        requirePause();
        logOperation("window");

    }

    /** Ждать, пока cond не станет true. До этого поток не попадает в enabled. */
    public static void await(BooleanSupplier cond) {
        Worker worker = currentWorker.get();
        Scheduler scheduler = currentScheduler.get();

        if (worker.needsPause || !cond.getAsBoolean()) {
            worker.needsPause = false;
            worker.waitingCondition = cond;
            worker.state = State.WAITING_CONDITION;

            scheduler.suspendCurrentWorker(worker);

            worker.waitingCondition = null;
        }

        logOperation("await done");
        requirePause();
    }

    public static void pause() {
        Worker worker = currentWorker.get();
        Scheduler scheduler = currentScheduler.get();

        if (!worker.needsPause) {
            return;
        }

        worker.needsPause = false;
        worker.state = State.WAITING_TURN;

        scheduler.suspendCurrentWorker(worker);
    }

    public static void requirePause(){
        currentWorker.get().needsPause = true;
    }

    public static void logOperation(String operation){
        Scheduler scheduler = currentScheduler.get();

        if (scheduler.logging) {
            Worker worker = currentWorker.get();
            int id = scheduler.workers.indexOf(worker);
            char name = (char) ('A' + id);

            scheduler.log.add(name + "  " + operation);
        }
    }

    public void registerLock(SchedLock schedLock){
        locks.add(schedLock);
    }

    private void handleDeadlock() {
        List<String> held = new ArrayList<>();

        for (SchedLock lock : locks) {
            if (!lock.isFree()) {
                int ownerId = workers.indexOf(lock.getCurrentLockOwner());
                char ownerName = (char) ('A' + ownerId);
                held.add(lock.name + ":" + ownerName);
            }
        }

        deadlock = "held={" + String.join(", ", held) + "}";
        aborted = true;

        for (Worker worker : workers) {
            if (worker.state != State.FINISHED) {
                worker.permit.release();
            }
        }
    }

    private static final class AbortedException extends RuntimeException{}

    private static void checkAborted(){
        if(currentScheduler.get().aborted){
            throw new AbortedException();
        }
    }

    public Worker getCurrentWorker() {
        return currentWorker.get();
    }

    public void suspendCurrentWorker(Worker currentWorker) {
        schedulerSignal.release();
        currentWorker.permit.acquireUninterruptibly();

        checkAborted();
    }

    public enum State{
        NEW,
        RUNNING,
        WAITING_TURN,
        WAITING_LOCK,
        WAITING_CONDITION,
        FINISHED
    }

    public static class Worker {
        final Runnable body;
        final Semaphore permit = new Semaphore(0);
        State state = State.NEW;
        boolean needsPause = true;
        BooleanSupplier waitingCondition;
        SchedLock waitingLock;

        Worker(Runnable body){
            this.body = body;
        }
    }
}
