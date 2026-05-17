import com.isoft.*;
import com.isoft.TransportStrategy.TransportResult;

import java.util.Scanner;

/**
 * Entry point demonstrating the three design patterns working together:
 *
 * <ol>
 * <li><b>Singleton</b> — {@link Logger}: one instance shared across the whole
 * app.</li>
 * <li><b>Strategy</b> — {@link TransportStrategy}: taxi, bus, bicycle are
 * interchangeable and hot-swappable at runtime.</li>
 * <li><b>Observer</b> — {@link TransportMonitor} notifies
 * {@link ConsolePrinter}
 * and {@link AlertObserver} on every tick without coupling them.</li>
 * </ol>
 *
 * <h2>Console commands</h2>
 * 
 * <pre>
 *   1  →  switch to Taxi
 *   2  →  switch to Colectivo
 *   3  →  switch to Bicicleta
 *   q  →  quit
 * </pre>
 * 
 * 1
 */
public class App {

    @SuppressWarnings("unused")
    public static void main(String[] args) throws InterruptedException {

        Logger logger = Logger.getInstance();
        logger.logInfo.accept("Starting Application");

        TransportStrategy bus = d -> new TransportResult("Bus", 1720.0, d, (int) (d * 45 / 7.1));
        TransportStrategy taxi = d -> new TransportResult("Taxi", 1363.60 * d, d, (int) (d / 0.5));
        TransportStrategy bicycle = d -> new TransportResult("Bicycle", 0, d, (int) (d / 0.25));

        System.out.println(bus.compute(7));

        // ── Console interaction — change strategy without stopping monitor ────
        logger.logInfo.accept("Commands: 1=Taxi | 2=Colectivo | 3=Bicicleta | q=quit");

        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                switch (line) {
                    case "1" -> {
                        logger.logInfo.accept("Strategy switched to → Taxi");
                    }
                    case "2" -> {
                        logger.logInfo.accept("Strategy switched to → Colectivo");
                    }
                    case "3" -> {
                        logger.logInfo.accept("Strategy switched to → Bicicleta");
                    }
                    case "q", "Q" -> {
                        logger.logInfo.accept("Shutting down. Goodbye!");
                        System.exit(0);
                    }
                    default ->
                        logger.logWarning.accept("Unknown command: \"" + line
                                + "\"  — use 1, 2, 3 or q");
                }
            }
        }
    }
}