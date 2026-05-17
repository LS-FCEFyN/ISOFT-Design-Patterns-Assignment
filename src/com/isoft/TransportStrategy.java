package com.isoft;

/**
 * Functional interface defining the contract for a transport strategy
 * within the application.
 *
 * <p>
 * Implementations of this interface represent a specific mode of transport
 * (e.g. car, bike, train) and are responsible for computing cost, distance,
 * and estimated arrival time for a given journey.
 * </p>
 *
 * <p>
 * This interface is intended for use with the
 * <a href="https://en.wikipedia.org/wiki/Strategy_pattern">Strategy
 * pattern</a>,
 * allowing transport modes to be selected and swapped at runtime. Being a
 * {@link FunctionalInterface}, strategies can be expressed concisely as
 * lambdas or method references.
 * </p>
 */
@FunctionalInterface
public interface TransportStrategy {

    /**
     * An immutable value object aggregating the results of a transport
     * strategy computation.
     *
     * @param name     a human-readable name identifying the transport mode
     * @param cost     the total cost of the journey in the application's
     *                 base currency unit
     * @param distance the total distance of the journey in kilometres
     * @param eta      the estimated time of arrival in minutes
     */
    public record TransportResult(
            String name,
            double cost,
            double distance,
            int eta) {
    }

    /**
     * Computes and returns the result of this transport strategy for a
     * given journey.
     *
     * @return a {@link TransportResult} containing the name, cost, distance,
     *         and ETA for the journey
     */
    TransportResult compute(double distanceKm);
}