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
 * Sujeto observable que recomputa periódicamente una
 * {@link TransportStrategy} y notifica a todos los observadores
 * registrados con el estado más reciente.
 *
 * <p>
 * Implementa el rol de <em>Subject</em> del
 * <a href="https://en.wikipedia.org/wiki/Observer_pattern">patrón Observer</a>.
 * Los observadores son simples instancias de
 * {@code Consumer<TransportStrategy.TransportResult>}, manteniendo
 * coherencia con el estilo funcional utilizado a lo largo de éste
 * paquete.
 * </p>
 *
 * <p>
 * El monitor se ejecuta sobre un
 * {@link ScheduledExecutorService} dedicado para que el bucle de
 * actualización no bloquee al hilo invocador.
 * La estrategia activa puede intercambiarse en cualquier momento
 * mediante {@link #setStrategy(TransportStrategy)} sin detener el
 * bucle.
 * </p>
 *
 * <p>
 * Los observadores se almacenan dentro de un
 * {@link CopyOnWriteArrayList} para que llamadas a
 * {@link #subscribe} y {@link #unsubscribe} realizadas desde hilos
 * distintos al hilo del scheduler sean seguras sin necesidad de
 * sincronización explicita.
 * </p>
 */
public class TransportMonitor {

    // ── Campos ───────────────────────────────────────────────────────────────

    private static final Logger logger = Logger.getInstance();

    /** Distancia fija (km) utilizada para cada cálculo de estrategia. */
    private static final double FIXED_DISTANCE_KM = 10.0;

    /** Lista thread-safe de observadores registrados. */
    private final List<Consumer<TransportStrategy.TransportResult>> observers = new CopyOnWriteArrayList<>();

    /**
     * Estrategia actualmente activa; volatile para que cambios sean visibles
     * inmediatamente.
     */
    private volatile TransportStrategy strategy;

    /** Scheduler responsable de ejecutar el bucle periódico de actualización. */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "transport-monitor");
        t.setDaemon(true); // no impedir que la JVM finalice
        return t;
    });

    /**
     * Referencia a la tarea en ejecución, conservada para poder cancelarla si fuese
     * necesario.
     */
    private ScheduledFuture<?> task;

    // ── Constructor ──────────────────────────────────────────────────────────

    /**
     * Crea un nuevo monitor utilizando la estrategia inicial especificada.
     *
     * @param initialStrategy estrategia a utilizar antes de cualquier llamada
     *                        a {@link #setStrategy(TransportStrategy)}
     */
    public TransportMonitor(TransportStrategy initialStrategy) {
        this.strategy = initialStrategy;
    }

    // ── Gestión de observadores ──────────────────────────────────────────────

    /**
     * Añade un observador a la lista de notificaciones.
     *
     * @param observer consumidor a registrar; ignorado si es {@code null}
     */
    public void subscribe(Consumer<TransportStrategy.TransportResult> observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    /**
     * Elimina un observador previamente registrado.
     *
     * @param observer consumidor a eliminar; no realiza ninguna acción si
     *                 no se encuentra registrado
     */
    public void unsubscribe(Consumer<TransportStrategy.TransportResult> observer) {
        observers.remove(observer);
    }

    // ── Intercambio dinámico de estrategia ───────────────────────────────────

    /**
     * Reemplaza la estrategia de transporte activa sin interrumpir el
     * bucle en ejecución.
     * La próxima iteración programada utilizará la nueva estrategia.
     *
     * @param newStrategy estrategia a activar
     */
    public void setStrategy(TransportStrategy newStrategy) {
        this.strategy = newStrategy;
        logger.logInfo.accept("Strategy changed to: " + newStrategy.compute(FIXED_DISTANCE_KM).name());
    }

    // ── Control del bucle ────────────────────────────────────────────────────

    /**
     * Inicia el bucle periódico de actualización.
     *
     * <p>
     * Cada iteración:
     * <ol>
     * <li>Invoca {@code strategy.compute()} para obtener un nuevo
     * {@link TransportStrategy.TransportResult}.</li>
     * <li>Notifica a cada observador registrado con dicho resultado.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Si {@code maxCycles} es mayor que cero, el bucle se detiene
     * automáticamente luego de esa cantidad de iteraciones;
     * pasar {@code 0} produce un bucle indefinido.
     * </p>
     *
     * @param intervalMs intervalo entre iteraciones expresado en
     *                   milisegundos (debe ser &gt; 0)
     * @param maxCycles  cantidad máxima de iteraciones antes de detenerse
     *                   automáticamente; {@code 0} = ilimitado
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
     * Cancela la tarea programada de actualización.
     * Iteraciones ya iniciadas no son interrumpidas.
     */
    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
            logger.logInfo.accept("TransportMonitor stopped.");
        }
    }

    /**
     * Devuelve {@code true} si el bucle se encuentra actualmente en
     * ejecución.
     */
    public boolean isRunning() {
        return task != null && !task.isCancelled() && !task.isDone();
    }

    // ── Helpers privados ────────────────────────────────────────────────────

    /**
     * Entrega {@code result} a cada observador registrado en orden de inserción.
     */
    private void notifyObservers(TransportStrategy.TransportResult result) {
        for (Consumer<TransportStrategy.TransportResult> observer : observers) {
            observer.accept(result);
        }
    }
}