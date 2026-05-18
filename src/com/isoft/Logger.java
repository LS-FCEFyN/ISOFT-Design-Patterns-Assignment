package com.isoft;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Registrador que utiliza el patrón Singleton que despacha a stdout mensajes con
 * marcas temporales y codificacion de colores
 *
 * <p>
 * Sólo una instancia de ésta clase puede existir a la vez. La instancia se
 * obtiene mediante llamadas al método {@link #getInstance()}.
 * Llamadas explicitas al constructor, copias, e instanciaciones externas
 * son intencionalmente deshabilitadas para preservar el patron.
 * </p>
 *
 * <h2>Severidad / Código de colores</h2>
 * <table border="1">
 * <caption>Los siguientes códigos de colores ANSI son utilizados por el Registrador</caption>
 * <tr><th>Level</th><th>Color</th><th>Código ANSI</th></tr>
 * <tr><td>DEBUG</td><td>Verde</td><td>{@code \033[32m}</td></tr>
 * <tr><td>INFO</td><td>Gris</td><td>{@code \033[90m}</td></tr>
 * <tr><td>WARN</td><td>Amarillo</td><td>{@code \033[33m}</td></tr>
 * <tr><td>ERROR</td><td>Rojo</td><td>{@code \033[31m}</td></tr>
 * </table>
 *
 * <p>
 * Ésta implementacion utiliza el
 * <a href="https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom">
 * modismo Initialization-on-demand holder</a> para conseguir que la inicialización del
 * singleton sea lazy y thread-safe sin necesidad de sincronización explicita.
 * </p>
 *
 * <p>
 * Siguiendo un paradigma funcional, todo comportamiento de registro se expresa como campos
 * del tipo 'funcion' no mutables mediante el uso de la palabra reservada {@code final}.
 * La lógica pura de formateo se mantiene separada. 
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

    // ── Funciones Puras ───────────────────────────────────────────────────────

    /**
     * Funcion pura que formatea un {@link LocalDateTime} en una cadena de texto
     * de la forma {@code yyyy-MM-dd HH:mm:ss}.
     */
    public final Function<LocalDateTime, String> formatTimestamp = dt -> dt
            .format(FORMATTER);

    /**
     * Funcion pura que compone una linea del registrador ya codificada por color
     * proveniente de {@link Severity} y una cadena de texto que contiene el mensaje
     * a mostrar
     * 
     * <p>
     * La cadena de texto resultante contiene el prefijo ANSI de color,
     * la marca temporal producida por {@link #formatTimestamp},
     * el nivel de severidad, el mensaje y la secuencia de reinicio
     * de color ANSI.
     * </p>
     */
    public final BiFunction<Severity, String, String> formatMessage = (severity, message) -> severity.color
            + "[" + formatTimestamp.apply(LocalDateTime.now()) + "] "
            + "[" + severity.name() + "] "
            + message
            + RESET;

    // ── Funciones con efectos secundarios, aisladas en la frontera ───────────

    /**
     * Escribe a stdout una linea codificada por color
     *
     * <p>
     * Sincronizada para asegurar que llamadas concurrentes o paralelas desde
     * diversos
     * hilos no causen problemas como entrelazado de salida al tener dos hilos
     * tratando de escribir
     * a la misma linea.
     * En rigor, esto no sería estrictamente necesario, dado que
     * {@link java.io.PrintStream#println(String)}
     * ya se encuentra sincronizado.
     * </p>
     *
     * @param severity el nivel de severidad a utilizar para colorear y rotular
     * @param message  el mensaje a escribir
     */
    private synchronized void print(Severity severity, String message) {
        System.out.println(formatMessage.apply(severity, message));
    }

    // ── Consumidores de conveniencia parcialmente aplicados ───────────────────

    /**
     * Consumidor parcialmente aplicado que registra mensajes con nivel
     * {@link Severity#DEBUG} (verde).
     */
    public final Consumer<String> logDebug = msg -> print(Severity.DEBUG, msg);

    /**
     * Consumidor parcialmente aplicado que registra mensajes con nivel
     * {@link Severity#INFO} (gris).
     */
    public final Consumer<String> logInfo = msg -> print(Severity.INFO, msg);

    /**
     * Consumidor parcialmente aplicado que registra mensajes con nivel
     * {@link Severity#WARN} (amarillo).
     */
    public final Consumer<String> logWarning = msg -> print(Severity.WARN, msg);

    /**
     * Consumidor parcialmente aplicado que registra mensajes con nivel
     * {@link Severity#ERROR} (rojo).
     */
    public final Consumer<String> logError = msg -> print(Severity.ERROR, msg);

    /**
     * Función de orden superior que devuelve un {@code Consumer<String>}
     * con una {@link Severity} preconfigurada.
     *
     * <p>
     * Resulta útil cuando el nivel de severidad sólo se conoce en tiempo
     * de ejecución y se desea pasar posteriormente un consumidor de un
     * único argumento.
     * </p>
     *
     * @param severity el nivel de severidad a asociar
     * @return un {@code Consumer<String>} fijado a dicha severidad
     */
    public Consumer<String> at(Severity severity) {
        return msg -> print(severity, msg);
    }
}