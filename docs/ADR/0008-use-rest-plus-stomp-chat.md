# ADR 0008: Use REST Plus STOMP WebSocket Chat

## Status
Approved

## Context
Loopin groups need persisted chat history and real-time coordination. REST endpoints are simple for loading existing messages, while real-time delivery benefits from a bidirectional messaging protocol that can broadcast to group-specific channels.

## Decision
We chose a hybrid chat design: **REST** for persisted group message reads and writes, and **STOMP over WebSocket** for real-time group message delivery.

## Consequences
* **Reliable History:** Messages are persisted in PostgreSQL and can be loaded through regular HTTP endpoints.
* **Real-Time UX:** WebSocket subscriptions allow group members to receive new messages without polling.
* **Authorization Surface:** Both REST and WebSocket paths must enforce group membership and authenticated identity.
* **Scale Consideration:** Multi-node WebSocket deployments require a shared broker or Redis Pub/Sub strategy so messages reach users connected to different API instances.
