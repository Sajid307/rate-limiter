Here is the documentation for the Token Bucket Rate Limiter, based on the provided implementation and general best practices.

***

# Token Bucket Rate Limiter

## Overview

This document describes the `TokenBucketRateLimiterImpl` service, a server-side rate limiter for a Spring Boot application. It uses the **Token Bucket algorithm** to control the rate of requests on a per-user basis. The primary goal is to prevent system overload and ensure fair usage by enforcing limits on how frequently a user can make requests.

## How It Works

The Token Bucket algorithm is best understood with a simple analogy:

-   Imagine each user has a bucket of a fixed size (`capacity`).
-   This bucket is continuously filled with tokens at a constant rate (`refill rate`).
-   Each token represents permission to make one request.
-   When a user makes a request, they must have a token. If they do, a token is removed from the bucket, and the request is processed.
-   If the bucket is empty, the user has no tokens, and the request is rejected.
-   If the bucket is full, any new tokens that are supposed to be added are discarded. This ensures the user cannot accumulate an infinite burst capacity.

This model allows for short **bursts** of requests (up to the bucket's capacity) and then enforces a steady average rate of requests over time.

## Algorithm

The implementation follows these steps for each incoming request:

1.  **Identify User:** The request is associated with a user via a unique `requestId` (e.g., a user ID).
2.  **Find or Create Bucket:** The system looks up the token bucket for that user. If one doesn't exist (i.e., it's the user's first request), a new bucket is created for them, initialized with a full capacity of tokens.
3.  **Refill Tokens (On-Demand):** The system calculates how much time has passed since the bucket was last refilled. Based on this elapsed time and the configured `refillTokenPerSecond`, it calculates how many new tokens should be added.
4.  **Enforce Capacity:** The newly calculated tokens are added to the bucket, but the total number of tokens is never allowed to exceed the `tokenCapacity`.
5.  **Check for Tokens:** The system checks if the bucket contains at least one token.
6.  **Allow or Reject:**
    -   If a token is available, it is consumed (the count is decremented), and the request is allowed to proceed.
    -   If no token is available, the request is rejected.

## Implementation

The core logic is encapsulated within the `TokenBucketRateLimiterImpl.java` class.

-   **`buckets` (ConcurrentHashMap<String, TokenBucketConfig>):** This is the primary data structure. It's a thread-safe map where the key is the user's `requestId` and the value is their `TokenBucketConfig` object, which stores the user's current token count, refill rate, and the timestamp of the last refill.
-   **`allowRequests(String requestId)`:** This is the main entry point. It orchestrates the entire process: finding the bucket, refilling it, and checking for token availability.
-   **`refill(TokenBucketConfig bucket)`:** A private helper method that contains the logic for calculating and adding new tokens based on elapsed time.
-   **Lazy Initialization:** The `buckets.computeIfAbsent(...)` method is used to efficiently create a new bucket for a user only when their first request arrives. This saves memory by not pre-allocating buckets for all possible users.

## Thread Safety

Rate limiting is an inherently concurrent problem. This implementation ensures thread safety for requests from the same user using a **fine-grained locking** strategy.

-   **The Problem:** Without proper synchronization, two concurrent requests from the same user could both read the token count, see that a token is available, and both proceed to consume it, resulting in more requests being allowed than permitted (a race condition).
-   **The Solution:** The `synchronized (bucket)` block in the `allowRequests` method. By synchronizing on the individual user's `bucket` object, we ensure that only one thread can modify a single user's bucket at a time. This approach is highly performant because it does **not** block requests for different users. Requests for `user-A` and `user-B` can be processed in parallel without interfering with each other.

## Time Handling

The refill mechanism is designed to be highly efficient by not relying on background threads or timers.

-   **On-Demand Calculation:** Tokens are not added in the background. Instead, they are calculated "just-in-time" when a request arrives. The `refill` method uses `System.currentTimeMillis()` to determine the time elapsed since the last access for that specific user.
-   **Fractional Refills:** The formula `(elapsedTime * refillRate) / 1000` correctly handles fractional time. By multiplying before dividing, we avoid integer division issues. For example, if the refill rate is 2 tokens/sec and 500ms have passed, the calculation `(500 * 2) / 1000` correctly yields `1` token.

## Complexity

-   **Time Complexity: `O(1)`**
    -   Each request involves a `ConcurrentHashMap` lookup, a short synchronized block, and a few arithmetic calculations. These are all constant-time operations on average. The performance does not degrade as the number of users increases.
-   **Space Complexity: `O(N)`**
    -   Where `N` is the number of unique users that have made at least one request. The system stores one `TokenBucketConfig` object in memory for each active user.

## Advantages

-   **Per-User Granularity:** Each user gets their own independent rate limit.
-   **Allows Bursts:** Users can make a burst of requests up to the bucket capacity, which is useful for front-end applications that may make several API calls on page load.
-   **High Concurrency:** Fine-grained locking on individual buckets ensures high throughput, as requests for different users do not block each other.
-   **Efficient:** The O(1) time complexity and on-demand refill logic make it very fast.
-   **Memory-Aware:** Buckets are only created for users who actually make requests.

## Limitations

-   **Single-Instance State:** The state of all token buckets is stored in the memory of a single application instance. This implementation is not suitable for a distributed environment where multiple instances of the service run behind a load balancer. A request from a user could hit different instances, each with a separate (and incorrect) view of the user's token count.
-   **No Persistence:** If the application restarts, all rate-limiting states are lost, and all buckets are reset to full capacity.
-   **Potential Memory Growth:** In a system with a very large number of unique users (e.g., millions), the `buckets` map can grow indefinitely, consuming significant memory.

## Test Cases

The implementation has been validated against a comprehensive test suite covering the following scenarios:

1.  **Initial Burst:** Verifies that a user can make requests up to the bucket's capacity before being limited.
2.  **Refill Logic:** Ensures tokens are correctly added back to the bucket over time.
3.  **Capacity Capping:** Confirms that the number of tokens in a bucket never exceeds its maximum capacity, even after long idle periods.
4.  **Client Isolation:** Guarantees that rate limiting for one user does not affect another user.
5.  **Fractional Refills:** Tests that refills work correctly for time intervals less than a full second.
6.  **Concurrency:** Validates that the implementation is thread-safe and correctly handles simultaneous requests for the same user without race conditions.

## Future Improvements

-   **Distributed Rate Limiting:** To address the single-instance limitation, the state could be moved to a centralized, external store like **Redis** or **Hazelcast**. Atomic operations (e.g., Redis Lua scripts) would be required to maintain correctness.
-   **Bucket Eviction Policy:** To manage memory growth, an eviction policy could be implemented. For example, using a cache like **Caffeine** or **Guava Cache** to automatically remove buckets for users who have been inactive for a certain period.
-   **Dynamic Configuration:** Expose endpoints or use a configuration server to allow administrators to change rate-limiting parameters (e.g., `capacity`, `refillRate`) at runtime without needing to restart the application.
-   **Idempotency:** how to handle idempotent request 
