package com.isoft;

import java.util.function.Consumer;

/**
 * Factory class for pre-built {@link TransportStrategy.TransportResult}
 * observers, following the
 * <a href="https://en.wikipedia.org/wiki/Observer_pattern">Observer pattern</a>.
 *
 * <p>
 * Each static factory method returns a {@code Consumer<TransportResult>} that
 * reacts to transport updates in a specific way. Consumers can be composed
 * (via {@link Consumer#andThen(Consumer)}) to build pipelines of independent
 * observers without coupling them to one another.
 * </p>
 *
 * <p>
 * Following the same functional paradigm used throughout this package, all
 * observers are expressed as lambdas. Any state they require (e.g. alert
 * thresholds) is captured immutably in the closure at construction time,
 * keeping each observer self-contained and free of shared mutable state.
 * </p>
 *
 * <p>
 * All observers delegate to the singleton {@link Logger} for output, inheriting
 * its thread-safety guarantee and consistent color-coded formatting.
 * </p>
 */
public class TransportObservers {

    private static final Logger logger = Logger.getInstance();

    /**
     * Returns an observer that prints a complete summary of every
     * {@link TransportStrategy.TransportResult} it receives to standard output.
     *
     * <p>
     * Output is formatted across two lines:
     * </p>
     * <ul>
     *   <li>A header line at {@link Severity#INFO} (gray) identifying the
     *       transport mode by name.</li>
     *   <li>A detail line at {@link Severity#DEBUG} (green) reporting the
     *       distance in kilometres, cost in the application's base currency
     *       unit, and estimated time of arrival in minutes.</li>
     * </ul>
     *
     * @return a {@code Consumer<TransportResult>} that logs every result
     *         unconditionally
     */
    public static Consumer<TransportStrategy.TransportResult> consolePrinter() {
        return result -> {
            logger.logInfo.accept("--- Transport Update: " + result.name() + " ---");
            logger.logDebug.accept(String.format("Distance: %.2f km | Cost: $%.2f | ETA: %d min",
                    result.distance(), result.cost(), result.eta()));
        };
    }

    /**
     * Returns a threshold-aware observer that emits warnings or errors when a
     * {@link TransportStrategy.TransportResult} breaches the supplied limits.
     *
     * <p>
     * Both thresholds are captured in the returned lambda's closure at
     * construction time and remain constant for the lifetime of the observer:
     * </p>
     * <ul>
     *   <li>If {@code result.cost()} exceeds {@code costThreshold}, a
     *       {@link Severity#WARN} (yellow) message is emitted.</li>
     *   <li>If {@code result.eta()} exceeds {@code etaThreshold}, a
     *       {@link Severity#ERROR} (red) message is emitted.</li>
     * </ul>
     *
     * <p>
     * Both conditions are evaluated independently; a single result may trigger
     * zero, one, or both alerts.
     * </p>
     *
     * @param costThreshold the maximum acceptable journey cost (exclusive) in
     *                      the application's base currency unit; results above
     *                      this value trigger a cost warning
     * @param etaThreshold  the maximum acceptable estimated time of arrival
     *                      (exclusive) in minutes; results above this value
     *                      trigger a critical ETA error
     * @return a {@code Consumer<TransportResult>} that silently passes results
     *         within the thresholds and logs alerts for those that exceed them
     */
    public static Consumer<TransportStrategy.TransportResult> alertObserver(double costThreshold, int etaThreshold) {
        return result -> {
            if (result.cost() > costThreshold) {
                logger.logWarning.accept("ALERT: Cost threshold exceeded! Current: $" + result.cost());
            }
            if (result.eta() > etaThreshold) {
                logger.logError.accept("CRITICAL: ETA threshold exceeded! Current: " + result.eta() + " min");
            }
        };
    }
}