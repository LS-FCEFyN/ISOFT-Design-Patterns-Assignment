package com.isoft;

/**
 * Severity levels for the {@link Logger}, each carrying its associated ANSI
 * color code for color-coded console output.
 *
 * <h2>Severity / Color Mapping</h2>
 * <table border="1">
 * <caption>ANSI color codes used by each severity level</caption>
 * <tr><th>Level</th><th>Color</th><th>ANSI Code</th></tr>
 * <tr><td>DEBUG</td><td>Green</td><td>{@code \033[32m}</td></tr>
 * <tr><td>INFO</td><td>Gray</td><td>{@code \033[90m}</td></tr>
 * <tr><td>WARN</td><td>Yellow</td><td>{@code \033[33m}</td></tr>
 * <tr><td>ERROR</td><td>Red</td><td>{@code \033[31m}</td></tr>
 * </table>
 *
 * <p>Each constant exposes its ANSI escape sequence via the {@link #color}
 * field, which the {@link Logger} prepends to a log line to colorize it.
 * A trailing reset code ({@code \033[0m}) is appended by the logger to
 * ensure subsequent terminal output is not affected.</p>
 */
public enum Severity {

    /** Fine-grained diagnostic information, rendered in green. */
    DEBUG("\u001B[32m"),

    /** General informational messages about normal operation, rendered in gray. */
    INFO("\u001B[90m"),

    /**
     * Potentially harmful situations that warrant attention, rendered in yellow.
     */
    WARN("\u001B[33m"),

    /**
     * Error events that may still allow the application to continue, rendered in
     * red.
     */
    ERROR("\u001B[31m");

    /**
     * The ANSI escape sequence used to colorize log output for this severity level.
     * Prepend this to a string and append {@code \033[0m} to apply and then reset
     * the color.
     */
    public final String color;

    /**
     * Constructs a {@code Severity} constant with the given ANSI color code.
     *
     * @param color the ANSI escape sequence for this severity level
     */
    Severity(String color) {
        this.color = color;
    }
}