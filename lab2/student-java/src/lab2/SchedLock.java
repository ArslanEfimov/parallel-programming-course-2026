package lab2;

/* Этап 2: лок, о котором знает планировщик. Поток, ждущий занятый лок, не попадает в enabled;
 * обычный synchronized / ReentrantLock здесь нельзя — поток уснёт с ходом в руках и всё зависнет.
 * Лог: "lock <имя>", "unlock <имя>". Планировщик должен знать все локи, чтобы печатать held={a:A, b:B}. */
public final class SchedLock {
    final String name;
    private Scheduler.Worker currentLockOwner;
    private final Scheduler scheduler;

    /** Создаётся в setup() тестовой программы; зарегистрируйте лок в планировщике s. */
    public SchedLock(Scheduler s, String name) {
        this.name = name;
        this.scheduler = s;
        scheduler.registerLock(this);
    }

    /** Пауза; если лок занят — ждать, не участвуя в выборе; затем стать владельцем. */
    public void lock() {
        Scheduler.Worker worker = scheduler.getCurrentWorker();

        if (worker.needsPause || !isFree()) {
            worker.needsPause = false;
            worker.waitingLock = this;
            worker.state = Scheduler.State.WAITING_LOCK;

            scheduler.suspendCurrentWorker(worker);
            worker.waitingLock = null;
        }

        currentLockOwner = worker;

        Scheduler.requirePause();
        Scheduler.logOperation("lock " + name);
    }

    /** Пауза; освободить лок, ждавшие потоки снова могут быть выбраны. */
    public void unlock() {
        Scheduler.pause();
        currentLockOwner = null;
        Scheduler.requirePause();
        Scheduler.logOperation("unlock " + name);
    }

    public Scheduler.Worker getCurrentLockOwner() {
        return currentLockOwner;
    }

    public boolean isFree() {
        return currentLockOwner == null;
    }
}
