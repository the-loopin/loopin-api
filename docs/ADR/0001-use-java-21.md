# ADR 0001: Use Java 21

## Status
Approved

## Context
When designing the Loopin API backend, we needed to select the Java Virtual Machine runtime version. We had options ranging from Java 17 (the previous LTS release) to Java 21 (the current LTS release at the time of development).

## Decision
We chose to develop and run the backend using **Java 21**.

## Consequences
* **Virtual Threads (Project Loom):** Access to light-weight threads that reduce the resource overhead of blocking operations (ideal for our Spring Boot Web and WebSocket workloads).
* **Language Features:** Utilization of modern language enhancements including pattern matching for switch, record patterns, and sequenced collections.
* **Support:** Java 21 is a Long Term Support (LTS) release, ensuring stability, security updates, and active library maintenance.
* **Compatibility:** All deployment environments (Docker base images, local setups) must support Java 21.
