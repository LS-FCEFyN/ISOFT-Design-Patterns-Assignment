package com.isoft;

import java.util.function.Consumer;

/**
 * Clase utilitaria que provee observadores preconstruidos de
 * {@link TransportStrategy.TransportResult}, siguiendo el
 * <a href="https://en.wikipedia.org/wiki/Observer_pattern">patrón Observer</a>.
 *
 * <p>
 * Cada método estático devuelve un
 * {@code Consumer<TransportResult>} que reacciona a actualizaciones
 * de transporte de una manera específica.
 * Los consumidores pueden componerse mediante
 * {@link Consumer#andThen(Consumer)} para encadenar observadores
 * independientes sin acoplarlos entre sí.
 * </p>
 *
 * <p>
 * Siguiendo el mismo paradigma funcional utilizado a lo largo de éste
 * paquete, todos los observadores se expresan mediante lambdas.
 * Cualquier estado requerido (por ejemplo umbrales de alerta) es
 * capturado de manera inmutable dentro del closure al momento de su
 * construcción, manteniendo cada observador autocontenido y libre de
 * estado mutable compartido.
 * </p>
 *
 * <p>
 * Todos los observadores delegan la salida al singleton
 * {@link Logger}, heredando sus garantías de thread-safety y formato
 * consistente codificado por colores.
 * </p>
 */
public class TransportObservers {

    private static final Logger logger = Logger.getInstance();

    /**
     * Devuelve un observador que imprime a stdout un resumen completo de
     * cada {@link TransportStrategy.TransportResult} recibido.
     *
     * <p>
     * La salida se formatea en dos lineas:
     * </p>
     * <ul>
     * <li>Una linea de cabecera con severidad
     * {@link Severity#INFO} (gris) identificando el modo de
     * transporte mediante su nombre.</li>
     * <li>Una linea de detalle con severidad
     * {@link Severity#DEBUG} (verde) reportando distancia en
     * kilómetros, costo en la unidad monetaria base de la
     * aplicación y tiempo estimado de llegada en minutos.</li>
     * </ul>
     *
     * @return un {@code Consumer<TransportResult>} que registra cada
     *         resultado de manera incondicional
     */
    public static Consumer<TransportStrategy.TransportResult> consolePrinter() {
        return result -> {
            logger.logInfo.accept("--- Transport Update: " + result.name() + " ---");
            logger.logDebug.accept(String.format("Distance: %.2f km | Cost: $%.2f | ETA: %d min",
                    result.distance(), result.cost(), result.eta()));
        };
    }

    /**
     * Devuelve un observador sensible a umbrales que emite advertencias
     * o errores cuando un {@link TransportStrategy.TransportResult}
     * excede los límites especificados.
     *
     * <p>
     * Ambos umbrales son capturados dentro del closure de la lambda
     * devuelta al momento de su construcción y permanecen constantes
     * durante toda la vida del observador:
     * </p>
     * <ul>
     * <li>Si {@code result.cost()} excede
     * {@code costThreshold}, se emite un mensaje con severidad
     * {@link Severity#WARN} (amarillo).</li>
     * <li>Si {@code result.eta()} excede
     * {@code etaThreshold}, se emite un mensaje con severidad
     * {@link Severity#ERROR} (rojo).</li>
     * </ul>
     *
     * <p>
     * Ambas condiciones son evaluadas de manera independiente; un mismo
     * resultado puede disparar cero, una o ambas alertas.
     * </p>
     *
     * @param costThreshold el costo máximo aceptable del viaje
     *                      (exclusivo) expresado en la unidad monetaria
     *                      base de la aplicación; resultados superiores
     *                      a éste valor disparan una advertencia de costo
     * @param etaThreshold  el tiempo máximo aceptable de llegada
     *                      estimada (exclusivo) expresado en minutos;
     *                      resultados superiores a éste valor disparan
     *                      un error crítico de ETA
     * @return un {@code Consumer<TransportResult>} que ignora
     *         silenciosamente resultados dentro de los umbrales y
     *         registra alertas para aquellos que los excedan
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