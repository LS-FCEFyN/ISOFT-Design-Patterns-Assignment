package com.isoft;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Singleton logger that writes color-coded, timestamped messages to
 * standard output.
 *
 * <p>
 * Only one instance of this class can exist at any time. Obtain the
 * instance via {@link #getInstance()}. Direct construction, copy
 * operations, and external instantiation are intentionally prevented
 * to preserve the singleton invariant.
 * </p>
 *
 * <h2>Severity / Color Mapping</h2>
 * <table border="1">
 * <caption>ANSI color codes used by the logger</caption>
 * <tr><th>Level</th><th>Color</th><th>ANSI Code</th></tr>
 * <tr><td>DEBUG</td><td>Green</td><td>{@code \033[32m}</td></tr>
 * <tr><td>INFO</td><td>Gray</td><td>{@code \033[90m}</td></tr>
 * <tr><td>WARN</td><td>Yellow</td><td>{@code \033[33m}</td></tr>
 * <tr><td>ERROR</td><td>Red</td><td>{@code \033[31m}</td></tr>
 * </table>
 *
 * <p>
 * This implementation uses the
 * <a href="https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom">
 * Initialization-on-demand holder idiom</a> to achieve lazy, thread-safe
 * singleton initialization without explicit synchronization.
 * </p>
 *
 * <p>
 * Following a functional paradigm, all logging behavior is expressed as
 * immutable {@code final} function fields. Pure formatting logic is kept
 * separate from the impure printing side effect, making formatting independently
 * testable without capturing stdout.
 * </p>
 */

public final class Logger {

    private static final String RESET = "\u001B[0m";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Singleton ────────────────────────────────────────────────────────────

    private Logger() {
    }

    private static class LazyHolder {
        static final Logger INSTANCE = new Logger();
    }

    /**
     * Returns the singleton instance of the logger.
     *
     * @return the global {@code Logger} instance
     */
    public static Logger getInstance() {
        return LazyHolder.INSTANCE;
    }

    // ── Pure functions ───────────────────────────────────────────────────────

    /**
     * Pure function that formats a {@link LocalDateTime} into a human-readable
     * timestamp string of the form {@code yyyy-MM-dd HH:mm:ss}.
     */
    public final Function<LocalDateTime, String> formatTimestamp = dt -> dt
            .format(FORMATTER);

    /**
     * Pure function that composes a fully formatted, color-coded log line from
     * a {@link Severity} and a message string.
     *
     * <p>
     * The returned string includes the ANSI color prefix, a timestamp
     * produced by {@link #formatTimestamp}, the severity label, the message,
     * and a trailing ANSI reset code.
     * </p>
     */
    public final BiFunction<Severity, String, String> formatMessage = (severity, message) -> severity.color
            + "[" + formatTimestamp.apply(LocalDateTime.now()) + "] "
            + "[" + severity.name() + "] "
            + message
            + RESET;

    // ── Side effect, isolated at the boundary ────────────────────────────────

    /**
     * Writes a formatted, color-coded log line to standard output.
     *
     * <p>
     * Synchronized to ensure that concurrent calls from multiple threads
     * do not interleave output on the same line.
     * </p>
     *
     * @param severity the log level to use for color and label
     * @param message  the message to write
     */
    private synchronized void print(Severity severity, String message) {
        System.out.println(formatMessage.apply(severity, message));
    }

    /**
     * Consumer that delegates to {@link #print(Severity, String)}, inheriting
     * its thread-safety guarantee.
     *
     * <p>
     * This is the only impure operation in the logger. All other fields
     * are pure functions that delegate here, keeping the side effect isolated
     * at a single boundary.
     * </p>
     */
    public final BiConsumer<Severity, String> log = this::print;

    // ── Partially applied convenience consumers ──────────────────────────────

    /**
     * Partially applied consumer that logs at {@link Severity#DEBUG} (green).
     */
    public final Consumer<String> logDebug = msg -> print(Severity.DEBUG, msg);

    /**
     * Partially applied consumer that logs at {@link Severity#INFO} (gray).
     */
    public final Consumer<String> logInfo = msg -> print(Severity.INFO, msg);

    /**
     * Partially applied consumer that logs at {@link Severity#WARN} (yellow).
     */
    public final Consumer<String> logWarning = msg -> print(Severity.WARN, msg);

    /**
     * Partially applied consumer that logs at {@link Severity#ERROR} (red).
     */
    public final Consumer<String> logError = msg -> print(Severity.ERROR, msg);

    /**
     * Higher-order function that returns a {@code Consumer<String>} with the
     * given {@link Severity} baked in. Useful when the severity level is only
     * known at runtime and you want to pass a single-argument consumer downstream.
     *
     * @param severity the log level to bind
     * @return a {@code Consumer<String>} fixed to that severity
     */
    public Consumer<String> at(Severity severity) {
        return msg -> print(severity, msg);
    }
}