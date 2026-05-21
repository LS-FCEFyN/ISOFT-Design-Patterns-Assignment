# ISOFT Design Patterns — Functional Programming

Una aplicación Java que demuestra tres patrones de diseño clásicos de la Gang of Four (GoF) implementados con un paradigma de programación funcional. El dominio es un sistema de monitoreo de transporte que calcula el costo, la distancia y el ETA para diferentes modos de tránsito.

## Project Structure

```
ISOFT-Design-Patterns/
├── src/
│   ├── App.java                      # Punto de entrada y driver de la aplicación
│   └── com/isoft/
│       ├── Logger.java               # Singleton — logger thread-safe
│       ├── Severity.java             # Enum de nivel de log con códigos de color ANSI
│       ├── TransportMonitor.java     # Subject de Observer con un executor programado
│       ├── TransportObservers.java   # Métodos fábrica de Observer
│       └── TransportStrategy.java    # Interfaz de Strategy + record TransportResult
├── transport_uml_class_diagram.html  # Diagrama de clases de UML interactivo en Mermaid
└── Assignment.pdf                    # Requerimientos originales del proyecto
```

## Design Patterns

### 1. Singleton — `Logger`

`Logger` proporciona una instancia única de logger, accesible globalmente y segura para hilos (thread-safe) utilizando el idioma **Initialization-on-demand Holder**. La instancia se crea de forma "lazily" al primer acceso sin sobrecarga de sincronización.

Elementos funcionales:
- `formatTimestamp` — función pura `() → String`
- `formatMessage` — función pura `(Severity, String) → String`
- `logDebug`, `logInfo`, `logWarning`, `logError` — instancias de `Consumer<String>` preconstruidas
- `at(Severity)` — función de orden superior que devuelve un `Consumer<String>` para la selección dinámica de severity

### 2. Strategy — `TransportStrategy`

`TransportStrategy` es una `@FunctionalInterface` cuyo único método `compute(double distanceKm)` devuelve un record `TransportResult` inmutable. Se definen tres estrategias concretas como lambdas en `App.main()`:

| Strategy   |       Cost         |   Speed    |
|------------|--------------------|------------|
| `bus`      | Fijo $10           | 30 km/h    |
| `taxi`     | Aleatorio $50–$150 | 60 km/h    |
| `bicycle`  | Gratis ($0)        | 15 km/h    |

`TransportResult` es un record de Java con los campos `name`, `cost`, `distance` y `eta`. La estrategia activa se puede intercambiar en caliente (hot-swapped) en tiempo de ejecución a través de `TransportMonitor.setStrategy()` sin detener el monitor.

### 3. Observer — `TransportMonitor` + `TransportObservers`

`TransportMonitor` es el **Subject**. Programa cálculos periódicos de la estrategia y notifica a todos los observers registrados:

- Los observers son `Consumer<TransportResult>` — no se necesita una interfaz de observer separada
- `subscribe(Consumer<TransportResult>)` — registra un observer
- `unsubscribe(Consumer<TransportResult>)` — elimina un observer
- `start(intervalMs, maxCycles)` — comienza la ejecución periódica; se detiene automáticamente después de `maxCycles` si es > 0
- `stop()` — cancela el monitor

`TransportObservers` proporciona dos métodos fábrica:

| Observer | Behavior |
|----------|----------|
| `consolePrinter()`  | Registra `name`, `cost`, `distance` y `ETA` del transporte en cada ciclo |
| `alertObserver(costThreshold, etaThreshold)` | Emite un `WARN` si el `cost` supera el umbral; un `ERROR` si el `ETA` supera el umbral |

Los umbrales se capturan de forma inmutable en un closure — no se requiere estado mutable.

## Requirements

- Java 14 o posterior (usa la sintaxis de `record` de la JDK 14)
- Sin dependencias externas — solo la biblioteca estándar

## Build & Run

**Compilar:**
```bash
mkdir -p bin
javac -d bin src/App.java src/com/isoft/*.java
```

**Ejecutar:**
```bash
java -cp bin App
```

**Controles interactivos (mientras el monitor está ejecutándose):**

| Input | Action                             |
|-------|------------------------------------|
| `1`   | Cambiar a la estrategia de Taxi    |
| `2`   | Cambiar a la estrategia de Bus     |
| `3`   | Cambiar a la estrategia de Bicycle |
| `q`   | Salir                              |

El monitor se ejecuta durante 10 ciclos (de 1 segundo cada uno) y se detiene automáticamente, o se detiene inmediatamente al presionar `q`.

## UML Class Diagram

Abrí `transport_uml_class_diagram.html` en un navegador para ver un diagrama interactivo de Mermaid que muestra todas las clases, interfaces, campos, métodos y relaciones.

## Concurrency Notes

La seguridad de hilos (thread safety) se logra sin el uso de bloqueos tradicionales:

- `volatile` en la estrategia activa — visibilidad atómica para intercambios en caliente (hot-swaps)
- `CopyOnWriteArrayList` para la lista de observers — operaciones seguras y concurrentes de añadir, eliminar e iterar (add/remove/iterate)
- `AtomicInteger` para el contador de ciclos
- `ScheduledExecutorService` con un solo hilo demonio (daemon thread) — no bloquea el apagado de la JVM
- `synchronized print()` en `Logger` — salida por consola serializada a través de los hilos

## Discussion Questions

### 1. Singleton — ¿Por qué un logger global es un buen candidato para Singleton? ¿Qué problemas puede causar en aplicaciones multihilo (multithreaded)? ¿Cómo los resolverías?

Un logger global es un candidato natural para un Singleton porque hay un único destino de salida (la consola) y toda la aplicación debería compartir el mismo formato, colores y configuración de niveles de log. Tener múltiples instancias no aporta valor y genera una salida inconsistente.

En aplicaciones multihilo, el riesgo principal es una condición de carrera de escritura (write race condition): si dos hilos llaman a `print()` simultáneamente, sus mensajes pueden entrelazarse en la consola y volverse ilegibles. También existe el clásico riesgo de inicialización: si dos hilos llaman a `getInstance()` al mismo tiempo antes de que la instancia exista, ambos podrían crear su propia copia, rompiendo la garantía de unicidad.

Esta implementación resuelve ambos problemas mediante dos mecanismos:

- **Initialization-on-demand Holder**: la instancia se crea dentro de una clase interna estática privada (`Holder`). La JVM garantiza que la carga de la clase ocurra exactamente una vez, sin necesidad de usar `synchronized` o `volatile` en `getInstance()`. Esto elimina el problema de la doble inicialización.
- **Synchronized `print()`**: el método de escritura se declara como `synchronized`, asegurando que solo un hilo escriba en la consola a la vez y evitando mensajes entrelazados.

### 2. Strategy — ¿Modificarías o crearías código para agregar un nuevo modo de transporte? ¿Qué principio SOLID ilustra esto?

Para agregar un nuevo modo de transporte (por ejemplo, un subway), solo necesitas crear una nueva lambda que implemente `TransportStrategy.compute()`. No es necesario modificar ninguna clase existente — ni `TransportMonitor`, ni `TransportObservers`, ni ninguna estrategia existente.

```java
TransportStrategy subway = distanceKm -> new TransportResult(
    "Subway",
    15 + Math.random() * 5,
    distanceKm,
    (int)(distanceKm / 40.0 * 60)
);
monitor.setStrategy(subway);
```

Este es el **Principio de Abierto/Cerrado (Open/Closed Principle - OCP)** de SOLID: el sistema está abierto para la extensión (se pueden agregar nuevas estrategias) pero cerrado para la modificación (el código existente no se toca). `TransportStrategy` como interfaz funcional es precisamente ese punto de extensión.

### 3. Observer — ¿Qué pasa si un observer es lento al procesar una notificación? ¿Cómo desacoplarías la velocidad del subject de la del observer?

En la implementación actual, los observers son notificados de forma sincrónica dentro del hilo del `ScheduledExecutorService`. Si un observer es lento, bloquea todas las notificaciones subsiguientes y puede retrasar o hacer que se pierda el próximo ciclo del monitor.

Dos enfoques para desacoplar sus velocidades:

- **Despacho asincrónico por observer (Async dispatch)**: en lugar de llamar al consumer directamente, se envía cada notificación como una tarea a un `ExecutorService` dedicado. Cada observer procesa a su propio ritmo sin bloquear al subject.
- **Cola de eventos (Event queue)**: el subject encola los resultados en una `BlockingQueue` y cada observer consume de esa cola en su propio hilo. Un observer lento almacena los mensajes en un búfer en lugar de bloquear al subject. Una cola acotada (bounded queue) con una política de descarte (descartar el más viejo o el más nuevo) evita el crecimiento desmedido de la memoria.

El enfoque de la cola también aísla al subject de las fallas del observer: si un observer lanza una excepción, el subject nunca se entera.

### 4. Integración — `ConsolePrinter` y `AlertObserver` usan el mismo logger de forma diferente. ¿Por qué es posible esto sin que se conozcan entre sí?

Ambos observers acceden al logger a través de `Logger.getInstance()`. Debido al patrón Singleton, esa llamada siempre devuelve la misma instancia independientemente de quién la llame. `ConsolePrinter` usa `logInfo` y `logDebug`; `AlertObserver` usa `logWarning` y `logError` — dos usos completamente diferentes del mismo objeto, sin que ninguno sepa que el otro existe.

Esto muestra el beneficio de combinar Singleton con Observer: el logger es un recurso compartido accesible globalmente, mientras que los observers son unidades independientes que solo conocen su propia lógica. Se preserva el desacoplamiento porque ningún observer referencia a otro; ambos simplemente consumen la misma API pública del Singleton.