# rate-limiter
A Java and Spring Boot project implementing the algorithms of Rate Limiting with REST APIs, concurrent handling, Redis-Based distributed rate limiting, and performance comparisons

## High-Level Design

## Rate Limiter Algorithms
1. Token Bucket

### 1. Token Bucket
A bucket having tokens with certain capacity, for each request tokens are consumed from the bucket. Refilling works with number of tokens to add in a given time(s or ms).

Refer: [Token Bucket RateLimiter](docs/token-bucket.md)
