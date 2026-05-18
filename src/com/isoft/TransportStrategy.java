package com.isoft;

/**
 * Interfaz funcional que define el contrato para una estrategia de
 * transporte dentro de la aplicación.
 *
 * <p>
 * Implementaciones de ésta interfaz representan un modo específico de
 * transporte (por ejemplo automóvil, bicicleta o tren) y son responsables
 * de calcular costo, distancia y tiempo estimado de llegada para un viaje
 * determinado.
 * </p>
 *
 * <p>
 * Ésta interfaz está destinada a ser utilizada junto al
 * <a href="https://en.wikipedia.org/wiki/Strategy_pattern">patrón
 * Strategy</a>,
 * permitiendo seleccionar e intercambiar modos de transporte en tiempo de
 * ejecución.
 * Al ser una {@link FunctionalInterface}, las estrategias pueden expresarse
 * de manera concisa mediante lambdas o referencias a métodos.
 * </p>
 */
@FunctionalInterface
public interface TransportStrategy {

    /**
     * Objeto inmutable que agrupa los resultados producidos por
     * el cálculo de una estrategia de transporte.
     *
     * @param name     nombre legible utilizado para identificar el modo de
     *                 transporte
     * @param cost     costo total del viaje expresado en la unidad monetaria
     *                 base de la aplicación
     * @param distance distancia total del viaje en kilómetros
     * @param eta      tiempo estimado de llegada en minutos
     */
    public record TransportResult(
            String name,
            double cost,
            double distance,
            int eta) {
    }

    /**
     * Calcula y devuelve el resultado de ésta estrategia de transporte
     * para un viaje determinado.
     *
     * @return un {@link TransportResult} conteniendo nombre, costo,
     *         distancia y ETA del viaje
     */
    TransportResult compute(double distanceKm);
}