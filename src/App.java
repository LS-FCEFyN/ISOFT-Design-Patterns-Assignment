import com.isoft.*;
import com.isoft.TransportStrategy.TransportResult;
import java.util.Random;
import java.util.Scanner;

public class App {
    public static void main(String[] args) throws InterruptedException {
        Logger logger = Logger.getInstance();
        logger.logInfo.accept("Comenzando Aplicacion");

        Random rng = new Random();
        TransportStrategy bus = d -> new TransportResult("Bus", 150 + rng.nextDouble() * 100, d, 20 + rng.nextInt(30));
        TransportStrategy taxi = d -> new TransportResult("Taxi", 800 + rng.nextDouble() * 700, d, 5 + rng.nextInt(15));
        TransportStrategy bicycle = d -> new TransportResult("Bicycle", 0, d, 30 + rng.nextInt(20));

        TransportMonitor monitor = new TransportMonitor(bus);
        monitor.subscribe(TransportObservers.consolePrinter());
        monitor.subscribe(TransportObservers.alertObserver(900, 35));
        monitor.start(1000, 10);

        logger.logInfo.accept("Comandos: 1=Taxi | 2=Bus | 3=Bici | q=salir");

        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine() && monitor.isRunning()) {
                String line = scanner.nextLine().trim();
                switch (line) {
                    case "1" -> monitor.setStrategy(taxi);
                    case "2" -> monitor.setStrategy(bus);
                    case "3" -> monitor.setStrategy(bicycle);
                    case "q", "Q" -> {
                        monitor.stop();
                        return;
                    }
                    default -> logger.logWarning.accept("Comando no reconocido: \"" + line + "\"");
                }
            }
        }

        logger.logInfo.accept("!10 ciclos finalizados, adios!");
    }
}