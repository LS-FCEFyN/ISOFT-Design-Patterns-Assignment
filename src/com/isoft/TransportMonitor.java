package com.isoft;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Observable subject that periodically recomputes a {@link TransportStrategy}
 * and notifies all registered observers with the latest snapshot.
 *
 * <p>
 * Implements the <em>Subject</em> role of the
 * <a href="https://en.wikipedia.org/wiki/Observer_pattern">Observer pattern</a>.
 * Observers are plain {@code Consumer<TransportStrategy.TransportResult>}
 * instances, consistent with the functional style used throughout this package.
 * </p>
 *
 * <p>
 * The monitor runs on a dedicated {@link ScheduledExecutorService} so the
 * update loop does not block the calling thread. The active strategy can be
 * swapped at any time via {@link #setStrategy(TransportStrategy)} without
 * stopping the loop.
 * </p>
 *
 * <p>
 * Observers are stored in a {@link CopyOnWriteArrayList} so that
 * {@link #subscribe} and {@link #unsubscribe} calls made from threads other
 * than the scheduler thread are safe without explicit locking.
 * </p>
 */
public class TransportMonitor {

    // ── Fields ───────────────────────────────────────────────────────────────

    private static final Logger logger = Logger.getInstance();

    /** Fixed distance (km) used for every strategy computation. */
    private static final double FIXED_DISTANCE_KM = 10.0;

    /** Thread-safe list of registered observers. */
    private final List<Consumer<TransportStrategy.TransportResult>> observers =
            new CopyOnWriteArrayList<>();

    /** The currently active strategy; volatile so swaps are immediately visible. */
    private volatile TransportStrategy strategy;

    /** Scheduler that drives the periodic update loop. */
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "transport-monitor");
                t.setDaemon(true); // don't prevent JVM shutdown
                return t;
            });

    /** Handle to the running task, kept so it can be cancelled if needed. */
    private ScheduledFuture<?> task;

    // ── Constructor ──────────────────────────────────────────────────────────

    /**
     * Creates a new monitor with the given initial strategy.
     *
     * @param initialStrategy the strategy to use before any call to
     *                        {@link #setStrategy(TransportStrategy)}
     */
    public TransportMonitor(TransportStrategy initialStrategy) {
        this.strategy = initialStrategy;
    }

    // ── Observer management ──────────────────────────────────────────────────

    /**
     * Adds an observer to the notification list.
     *
     * @param observer the consumer to register; ignored if {@code null}
     */
    public void subscribe(Consumer<TransportStrategy.TransportResult> observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    /**
     * Removes a previously registered observer.
     *
     * @param observer the consumer to remove; no-op if not present
     */
    public void unsubscribe(Consumer<TransportStrategy.TransportResult> observer) {
        observers.remove(observer);
    }

    // ── Strategy hot-swap ────────────────────────────────────────────────────

    /**
     * Replaces the active transport strategy without interrupting the running loop.
     * The next scheduled tick will use the new strategy.
     *
     * @param newStrategy the strategy to activate
     */
    public void setStrategy(TransportStrategy newStrategy) {
        this.strategy = newStrategy;
        logger.logInfo.accept("Strategy changed to: " + newStrategy.compute(FIXED_DISTANCE_KM).name());
    }

    // ── Loop control ─────────────────────────────────────────────────────────

    /**
     * Starts the periodic update loop.
     *
     * <p>
     * Each tick:
     * <ol>
     *   <li>Calls {@code strategy.compute()} to obtain a fresh
     *       {@link TransportStrategy.TransportResult}.</li>
     *   <li>Notifies every registered observer with that result.</li>
     * </ol>
     * </p>
     *
     * <p>
     * If {@code maxCycles} is greater than zero the loop stops automatically
     * after that many ticks; pass {@code 0} for an indefinite loop.
     * </p>
     *
     * @param intervalMs the delay between ticks in milliseconds (must be &gt; 0)
     * @param maxCycles  maximum number of ticks before auto-stop; {@code 0} = unlimited
     */
    public void start(long intervalMs, int maxCycles) {
        logger.logInfo.accept("TransportMonitor started"
                + (maxCycles > 0 ? " (" + maxCycles + " cycles)" : " (unlimited)"));

        AtomicInteger cycleCount = new AtomicInteger(0);

        task = scheduler.scheduleAtFixedRate(() -> {
            try {
                TransportStrategy.TransportResult result = strategy.compute(FIXED_DISTANCE_KM);
                notifyObservers(result);

                if (maxCycles > 0 && cycleCount.incrementAndGet() >= maxCycles) {
                    logger.logInfo.accept("TransportMonitor completed " + maxCycles + " cycles. Stopping.");
                    stop();
                }
            } catch (Exception e) {
                logger.logError.accept("Error during monitor cycle: " + e.getMessage());
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Cancels the scheduled update task. Already-running ticks are not interrupted.
     */
    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
            logger.logInfo.accept("TransportMonitor stopped.");
        }
    }

    /**
     * Returns {@code true} if the loop is currently running.
     */
    public boolean isRunning() {
        return task != null && !task.isCancelled() && !task.isDone();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /** Delivers {@code result} to every registered observer in insertion order. */
    private void notifyObservers(TransportStrategy.TransportResult result) {
        for (Consumer<TransportStrategy.TransportResult> observer : observers) {
            observer.accept(result);
        }
    }
}