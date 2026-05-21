# ISOFT Design Patterns — Functional Programming

A Java application demonstrating three classic Gang of Four (GoF) design patterns implemented with a functional programming paradigm. The domain is a transport monitoring system that computes cost, distance, and ETA for different transit modes.

## Project Structure

```
ISOFT-Design-Patterns-FP/
├── src/
│   ├── App.java                      # Entry point and application driver
│   └── com/isoft/
│       ├── Logger.java               # Singleton — color-coded, thread-safe logger
│       ├── Severity.java             # Log level enum with ANSI color codes
│       ├── TransportMonitor.java     # Observer subject with scheduled executor
│       ├── TransportObservers.java   # Observer factory methods
│       └── TransportStrategy.java   # Strategy interface + TransportResult record
├── transport_uml_class_diagram.html  # Interactive Mermaid UML class diagram
└── Assignment.pdf                    # Original project requirements
```

## Design Patterns

### 1. Singleton — `Logger`

`Logger` provides a single, globally accessible, thread-safe logger using the **Initialization-on-demand Holder** idiom. The instance is created lazily on first access with no synchronization overhead.

Functional elements:
- `formatTimestamp` — pure function `() → String`
- `formatMessage` — pure function `(Severity, String) → String`
- `logDebug`, `logInfo`, `logWarning`, `logError` — pre-built `Consumer<String>` instances
- `at(Severity)` — higher-order function that returns a `Consumer<String>` for dynamic severity selection

### 2. Strategy — `TransportStrategy`

`TransportStrategy` is a `@FunctionalInterface` whose single method `compute(double distanceKm)` returns an immutable `TransportResult` record. Three concrete strategies are defined as lambdas in `App.main()`:

| Strategy   | Cost            | Speed   |
|------------|-----------------|---------|
| `bus`      | Fixed $10       | 30 km/h |
| `taxi`     | Random $50–$150 | 60 km/h |
| `bicycle`  | Free ($0)       | 15 km/h |

`TransportResult` is a Java **record** with fields `name`, `cost`, `distance`, and `eta`. The active strategy can be hot-swapped at runtime via `TransportMonitor.setStrategy()` without stopping the monitor.

### 3. Observer — `TransportMonitor` + `TransportObservers`

`TransportMonitor` is the **Subject**. It schedules periodic strategy computations and notifies all registered observers:

- Observers are `Consumer<TransportResult>` — no separate observer interface is needed
- `subscribe(Consumer<TransportResult>)` — registers an observer
- `unsubscribe(Consumer<TransportResult>)` — removes an observer
- `start(intervalMs, maxCycles)` — begins periodic execution; auto-stops after `maxCycles` if > 0
- `stop()` — cancels the monitor

`TransportObservers` provides two factory methods:

| Observer | Behavior |
|----------|----------|
| `consolePrinter()` | Logs transport name, cost, distance, and ETA on every cycle |
| `alertObserver(costThreshold, etaThreshold)` | Emits WARN if cost exceeds threshold; ERROR if ETA exceeds threshold |

Thresholds are captured immutably in a closure — no mutable state required.

## Requirements

- Java 14 or later (uses `record` syntax from JDK 14)
- No external dependencies — standard library only

## Build & Run

**Compile:**
```bash
mkdir -p bin
javac -d bin src/App.java src/com/isoft/*.java
```

**Run:**
```bash
java -cp bin App
```

**Interactive controls (while the monitor is running):**

| Input | Action                     |
|-------|----------------------------|
| `1`   | Switch to Taxi strategy    |
| `2`   | Switch to Bus strategy     |
| `3`   | Switch to Bicycle strategy |
| `q`   | Quit                       |

The monitor runs for 10 cycles (1 second each) and stops automatically, or stops immediately on `q`.

## UML Class Diagram

Open `transport_uml_class_diagram.html` in a browser to view an interactive Mermaid diagram showing all classes, interfaces, fields, methods, and relationships.

## Concurrency Notes

Thread safety is achieved without traditional locking:

- `volatile` on the active strategy — atomic visibility for hot-swaps
- `CopyOnWriteArrayList` for the observer list — safe concurrent add/remove/iterate
- `AtomicInteger` for the cycle counter
- `ScheduledExecutorService` with a single daemon thread — does not block JVM shutdown
- `synchronized print()` in `Logger` — serialized console output across threads

## Discussion Questions

### 1. Singleton — Why is a global logger a good Singleton candidate? What problems can it cause in multithreaded apps? How would you solve them?

A global logger is a natural Singleton candidate because there is a single output destination (the console) and the entire application should share the same format, colors, and log level configuration. Having multiple instances adds no value and produces inconsistent output.

In multithreaded applications, the main risk is a **write race condition**: if two threads call `print()` simultaneously, their messages can interleave on the console and become unreadable. There is also the classic initialization risk: if two threads call `getInstance()` at the same time before the instance exists, both could create their own copy, breaking the uniqueness guarantee.

This implementation solves both with two mechanisms:

- **Initialization-on-demand Holder**: the instance is created inside a private static inner class (`Holder`). The JVM guarantees that class loading happens exactly once, with no need for `synchronized` or `volatile` on `getInstance()`. This eliminates the double-initialization problem.
- **Synchronized `print()`**: the write method is declared `synchronized`, ensuring only one thread writes to the console at a time and preventing interleaved messages.

### 2. Strategy — Would you modify or create to add a new transport mode? What SOLID principle does this illustrate?

To add a new transport mode (e.g., subway), you only need to **create** a new lambda implementing `TransportStrategy.compute()`. No existing class needs to be modified — not `TransportMonitor`, not `TransportObservers`, not any existing strategy.

```java
TransportStrategy subway = distanceKm -> new TransportResult(
    "Subway",
    15 + Math.random() * 5,
    distanceKm,
    (int)(distanceKm / 40.0 * 60)
);
monitor.setStrategy(subway);
```

This is the **Open/Closed Principle (OCP)** from SOLID: the system is *open* for extension (new strategies can be added) but *closed* for modification (existing code is untouched). `TransportStrategy` as a functional interface is precisely that extension point.

### 3. Observer — What happens if an observer is slow to process a notification? How would you decouple the subject's speed from the observer's?

In the current implementation, observers are notified **synchronously** inside the `ScheduledExecutorService` thread. If one observer is slow, it blocks all subsequent notifications and can delay or miss the next monitor cycle.

Two approaches to decouple their speeds:

- **Async dispatch per observer**: instead of calling the consumer directly, submit each notification as a task to a dedicated `ExecutorService`. Each observer processes at its own pace without blocking the subject.
- **Event queue**: the subject enqueues results into a `BlockingQueue` and each observer drains that queue in its own thread. A slow observer buffers messages rather than blocking the subject. A bounded queue with a drop policy (drop oldest or drop newest) prevents unbounded memory growth.

The queue approach also isolates the subject from observer failures: if an observer throws, the subject never notices.

### 4. Integration — `ConsolePrinter` and `AlertObserver` use the same logger differently. Why is this possible without them knowing each other?

Both observers reach the logger through `Logger.getInstance()`. Because of the Singleton pattern, that call always returns the same instance regardless of who calls it. `ConsolePrinter` uses `logInfo` and `logDebug`; `AlertObserver` uses `logWarning` and `logError` — two completely different uses of the same object, with neither knowing the other exists.

This shows the benefit of combining Singleton with Observer: the logger is a globally accessible shared resource, while the observers are independent units that only know their own logic. Decoupling is preserved because no observer references another; both simply consume the same public API of the Singleton.