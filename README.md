# Concurrent File Download Strategies

## Problem Statement
Our current approach to file caching: Fetch files in fixed-size batches (e.g., 5, 10, 20 at a time). Each batch must wait for the slowest file to finish before the next batch starts. This leads to:
- Head-of-line blocking: one slow download delays all remaining work.
- Underutilized resources: faster downloads sit idle while waiting.
- Hard tuning: picking a single batch size that works for heterogeneous file sizes and network variability is difficult.
- Potential memory pressure if large batches are pre-allocated.

## Goal
Stream file downloads with bounded concurrency (a maximum number of simultaneous downloads) while:
- Reducing latency for fast files (they complete as soon as possible).
- Preserving backpressure so production does not overwhelm consumption.
- Providing clear per-download status transitions.
- Supporting cancellation and error isolation.

## Constraints & Requirements
- Limit concurrent downloads (e.g., configurable: 5–50).
- Avoid unbounded internal queues.
- Support cancellation via structured concurrency.
- Record status: Pending → Downloading → Completed | Failed.
- Handle failures without stopping unrelated downloads unless policy requires.

## Solution Approaches
We explore four Kotlin coroutine-based strategies:

### 1. Async–Await (Eager Dispatch)
Create a list of `Deferred` tasks and `awaitAll()`:
- Simple for small, known sets.
- Harder to apply backpressure mid-stream.
- Risk of large spikes if the entire set is launched at once.

### 2. Launch + Semaphore
Use a `Semaphore` to cap concurrent `launch` blocks:
- Straightforward mental model.
- Explicit acquisition/release around each download.
- Requires manual aggregation of results/errors.

### 3. Flow-Based Concurrency
Represent downloads as a cold `Flow` of entries; transform with operators (`flatMapMerge`, `buffer`, etc.):
- Built-in backpressure and composition.
- Natural integration with transformation pipelines (mapping, filtering, metrics).
- Slightly more abstract for imperative status updates.

### 4. Channel (Producer / Consumers)
Use a bounded `Channel` to feed work to consumer coroutines:
- Precise control of capacity and backpressure (send suspends when full).
- Clear producer/consumer separation.
- More boilerplate but flexible for prioritization, retries, and multi-queue designs.

## Comparison (Qualitative)
Aspect | Async-Await | Semaphore | Flow | Channel
------ | ----------- | --------- | ---- | -------
Backpressure | No | Partial | Yes | Yes
Bounded Queue | No | Manual | Yes (buffer) | Yes (capacity)
Failure Isolation | Medium | Medium | High | High
Code Verbosity | Low | Low | Medium | Medium
Flexibility | Low | Medium | High | High

## When to Choose What
- Use Channel when you need explicit producer/consumer control or advanced scheduling.
- Use Flow when composing transformations or integrating with reactive pipelines.
- Use Semaphore for minimal incremental change to an existing loop.
- Use Async–Await only for small, fixed sets where all tasks can be fired safely.

## Status Lifecycle
Each strategy updates repository state via a `DownloadRepository` (conceptual):
1. Enqueue → Pending
2. Start → Downloading
3. Success → Completed
4. Error → Failed (with cause)

## Observability & Metrics
Recommended instrumentation:
- Total wall-clock time.
- Average & percentile completion latency (per file).
- Throughput (files/sec).
- Error count & retry statistics.

## Extensions / Future Enhancements
- Retry policy with exponential backoff.
- Priority scheduling (multiple channels / weighted queues).
- Circuit breaker for repeated remote failures.
- Global timeout or cancellation trigger.
- Adaptive concurrency (increase/decrease based on success latency).

## Project Layout (Relevant)
- `Main.kt` – entry point to experiment with strategies.
- `data/RemoteDataSource.kt` – simulates or performs the actual download.
- `data/DownloadRepository.kt` – tracks statuses.

(Strategy-specific functions can be added under appropriate packages, e.g., `channel/`, `asyncAwait/`, etc.)

## How To Experiment
1. Adjust concurrency limits (e.g., constant or CLI arg) in the chosen strategy implementation.
2. Run the application:

```bash
./gradlew run
```

3. Add logging timestamps to measure latency differences among strategies.
4. Iterate: switch between implementations (e.g., comment/uncomment or pass a mode flag).

## Suggested Mode Flag (Optional Enhancement)
Add a CLI argument (`--mode=channel|flow|semaphore|async`) to select the strategy at runtime.

## Summary
Moving from rigid fixed-size batch downloads to streaming with bounded, continuous concurrency reduces idle time, mitigates head-of-line blocking, and improves overall throughput while keeping resource usage predictable. The four coroutine strategies offer trade-offs in complexity, flexibility, and backpressure control—choose the one that best fits your evolution path and future feature needs.

_Last updated: 2025-11-24_

