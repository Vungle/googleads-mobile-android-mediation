# ADR-003: Singleton Initialization Pattern (VungleInitializer)

## Status
Accepted

## Context
The Vungle SDK (and many other network SDKs) must be initialized exactly once before any ad requests. Multiple adapter instances for different ad formats may attempt initialization concurrently. The Google mediation SDK may create multiple adapter instances.

## Decision
A dedicated `VungleInitializer` singleton manages SDK initialization:
- Private constructor with static `getInstance()` access
- Thread-safe initialization state tracking
- Queues initialization callbacks when init is already in progress
- Caches the initialization result to immediately callback on subsequent requests
- `VungleManager` handles shared SDK state and placement management separately

## Consequences
- SDK is initialized exactly once regardless of how many adapter instances exist
- Concurrent initialization requests are safely queued
- Clear separation: Initializer handles init lifecycle, Manager handles ad state
- Singleton pattern makes unit testing harder (requires reset/mock capability)
- All adapter instances share the same initialization state
