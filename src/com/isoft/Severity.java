package com.isoft;

/**
 * Niveles de severidad para el {@link Logger}, cada uno asociado a su
 * correspondiente código de color ANSI para salida coloreada en consola.
 *
 * <h2>Severidad / Código de colores</h2>
 * <table border="1">
 * <caption>Códigos de color ANSI utilizados por cada nivel de severidad</caption>
 * <tr><th>Nivel</th><th>Color</th><th>Código ANSI</th></tr>
 * <tr><td>DEBUG</td><td>Verde</td><td>{@code \033[32m}</td></tr>
 * <tr><td>INFO</td><td>Gris</td><td>{@code \033[90m}</td></tr>
 * <tr><td>WARN</td><td>Amarillo</td><td>{@code \033[33m}</td></tr>
 * <tr><td>ERROR</td><td>Rojo</td><td>{@code \033[31m}</td></tr>
 * </table>
 *
 * <p>
 * Cada constante expone su secuencia de escape ANSI mediante el campo
 * {@link #color}, el cual el {@link Logger} antepone a una linea del
 * registrador para colorearla.
 * El registrador añade una secuencia de reinicio
 * ({@code \033[0m}) al final para asegurar que salidas posteriores de la
 * terminal no se vean afectadas.
 * </p>
 */
public enum Severity {

    /** Información de diagnóstico detallada, representada en verde. */
    DEBUG("\u001B[32m"),

    /**
     * Mensajes informativos generales sobre operación normal, representados en
     * gris.
     */
    INFO("\u001B[90m"),

    /**
     * Situaciones potencialmente dañinas que ameritan atención,
     * representadas en amarillo.
     */
    WARN("\u001B[33m"),

    /**
     * Eventos de error que aún pueden permitir que la aplicación continúe
     * ejecutándose, representados en rojo.
     */
    ERROR("\u001B[31m");

    /**
     * Secuencia de escape ANSI utilizada para colorear la salida del
     * registrador correspondiente a éste nivel de severidad.
     *
     * <p>
     * Anteponer ésta secuencia a una cadena de texto y añadir
     * {@code \033[0m} al final aplica y posteriormente reinicia el color.
     * </p>
     */
    public final String color;

    /**
     * Construye una constante {@code Severity} con el código de color ANSI
     * especificado.
     *
     * @param color la secuencia de escape ANSI correspondiente a éste nivel
     *              de severidad
     */
    Severity(String color) {
        this.color = color;
    }
}