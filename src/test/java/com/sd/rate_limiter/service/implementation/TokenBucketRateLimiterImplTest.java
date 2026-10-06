package com.sd.rate_limiter.service.implementation;

import com.sd.rate_limiter.config.TokenBucketConfig;
import com.sd.rate_limiter.dto.AllowRequestDTO;
import com.sd.rate_limiter.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TokenBucketRateLimiterImplTest {

    @Spy
    private List<User> userList = new ArrayList<>();

    @InjectMocks
    private TokenBucketRateLimiterImpl rateLimiter;

    @BeforeEach
    void setUp() {
        // The SUT's constructor is called by Mockito before this method.
        // We must reset the internal state for each test to ensure isolation.
        userList.clear();
        ReflectionTestUtils.setField(rateLimiter, "users", new ConcurrentHashMap<>());
        ReflectionTestUtils.setField(rateLimiter, "buckets", new ConcurrentHashMap<>());
    }

    @Test
    @DisplayName("Test 1: Initial burst of requests consumes all tokens")
    void testInitialBurst() {
        long capacity = 5;
        String userId = "user-A";
        setupUserAndBucket(userId, capacity, 1, capacity);

        // Consume all available tokens
        for (int i = 0; i < capacity; i++) {
            AllowRequestDTO result = rateLimiter.allowRequests(userId);
            assertTrue(result.isAllowRequest(), "Request " + (i + 1) + " should be allowed.");
        }

        // The next request should be rejected
        AllowRequestDTO rejectedResult = rateLimiter.allowRequests(userId);
        assertFalse(rejectedResult.isAllowRequest(), "Request " + (capacity + 1) + " should be rejected.");
    }

    @Test
    @DisplayName("Test 2: Tokens are refilled over time")
    void testRefill() {
        long capacity = 5;
        long refillRate = 2; // 2 tokens per second
        String userId = "user-A";
        TokenBucketConfig bucket = setupUserAndBucket(userId, capacity, refillRate, 0);

        // Simulate waiting for 1 second by setting the last refill time to 1s in the past
        bucket.setLastRefillTimeStamp(System.currentTimeMillis() - 1000);

        // After 1s, 2 tokens should be refilled (0 + 1s * 2 tokens/s)
        AllowRequestDTO result1 = rateLimiter.allowRequests(userId);
        assertTrue(result1.isAllowRequest(), "First request after 1s wait should be allowed.");
        assertEquals(1, result1.getTokenBucket().getCapacity());

        AllowRequestDTO result2 = rateLimiter.allowRequests(userId);
        assertTrue(result2.isAllowRequest(), "Second request after 1s wait should be allowed.");
        assertEquals(0, result2.getTokenBucket().getCapacity());

        AllowRequestDTO result3 = rateLimiter.allowRequests(userId);
        assertFalse(result3.isAllowRequest(), "Third request should be rejected.");
    }

    @Test
    @DisplayName("Test 3: Token count does not exceed capacity")
    void testCapacityNotExceeded() {
        long capacity = 5;
        long refillRate = 10;
        String userId = "user-A";
        TokenBucketConfig bucket = setupUserAndBucket(userId, capacity, refillRate, 0);

        // Simulate waiting for 100 seconds
        bucket.setLastRefillTimeStamp(System.currentTimeMillis() - 100_000);

        // Trigger a refill. Expected tokens = min(capacity, 0 + 100s * 10 tokens/s) = 5
        AllowRequestDTO result = rateLimiter.allowRequests(userId);
        assertTrue(result.isAllowRequest());

        // After consuming one token, the capacity should be max_capacity - 1
        assertEquals(capacity - 1, result.getTokenBucket().getCapacity());
    }

    @Test
    @DisplayName("Test 4: Different clients are handled independently")
    void testDifferentClients() {
        long capacity = 5;
        String userA = "user-A";
        String userB = "user-B";
        setupUserAndBucket(userA, capacity, 1, capacity);
        setupUserAndBucket(userB, capacity, 1, capacity);

        // Consume all tokens for user-A
        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.allowRequests(userA).isAllowRequest());
        }

        // Verify user-A is rate limited
        assertFalse(rateLimiter.allowRequests(userA).isAllowRequest());

        // Verify user-B is not affected and can make a request
        AllowRequestDTO userBResult = rateLimiter.allowRequests(userB);
        assertTrue(userBResult.isAllowRequest());
        assertEquals(capacity - 1, userBResult.getTokenBucket().getCapacity());
    }

    @Test
    @DisplayName("Test 5: Fractional time should refill fractional tokens")
    void testFractionalRefill() {
        long capacity = 10;
        long refillRate = 2; // 2 tokens/sec -> 1 token/500ms
        String userId = "user-A";
        TokenBucketConfig bucket = setupUserAndBucket(userId, capacity, refillRate, 0);

        // Simulate waiting for 500ms
        bucket.setLastRefillTimeStamp(System.currentTimeMillis() - 500);

        // After 500ms, 1 token should be refilled: (500ms * 2 tokens/sec) / 1000 = 1
        AllowRequestDTO result1 = rateLimiter.allowRequests(userId);
        assertTrue(result1.isAllowRequest(), "Request after 500ms should be allowed (1 token refilled).");

        AllowRequestDTO result2 = rateLimiter.allowRequests(userId);
        assertFalse(result2.isAllowRequest(), "Second request should be rejected as only 1 token was refilled.");
    }

    @Test
    @DisplayName("Test 6: Concurrent requests are handled correctly")
    void testConcurrentRequests() throws InterruptedException {
        long capacity = 100;
        int totalRequests = 1000;
        String userId = "user-A";
        setupUserAndBucket(userId, capacity, 10, capacity); // Use a non-zero refill rate

        ExecutorService executor = Executors.newFixedThreadPool(200);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger allowedRequests = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                if (rateLimiter.allowRequests(userId).isAllowRequest()) {
                    allowedRequests.incrementAndGet();
                }
                latch.countDown();
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        // With synchronization, exactly 'capacity' requests should be allowed initially.
        // Some minimal number of additional requests might be allowed due to refills during the test execution,
        // but it should be very close to the capacity. We assert it's less than a reasonable upper bound.
        assertTrue(allowedRequests.get() >= capacity, "At least " + capacity + " requests should be allowed.");
        assertTrue(allowedRequests.get() < capacity + 50, "Allowed requests should not significantly exceed capacity.");
    }

    @Test
    @DisplayName("Test getTokens: Should return current bucket state without consuming a token")
    void testGetTokens() {
        long capacity = 10;
        long initialTokens = 7;
        String userId = "user-A";
        setupUserAndBucket(userId, capacity, 1, initialTokens);

        // Act
        AllowRequestDTO result = rateLimiter.getTokens(userId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isAllowRequest());
        assertNotNull(result.getUser());
        assertEquals(userId, result.getUser().getUserId());

        assertNotNull(result.getTokenBucket());
        assertEquals(initialTokens, result.getTokenBucket().getCapacity());

        // Verify that getTokens did not consume a token by making a subsequent call
        AllowRequestDTO afterGetTokens = rateLimiter.allowRequests(userId);
        assertTrue(afterGetTokens.isAllowRequest());
        assertEquals(initialTokens - 1, afterGetTokens.getTokenBucket().getCapacity());
    }

    private TokenBucketConfig setupUserAndBucket(String userId, long capacity, long refillRate, long initialTokens) {
        // Set the global capacity and refill rate for the SUT instance
        ReflectionTestUtils.setField(rateLimiter, "tokenCapacity", capacity);
        ReflectionTestUtils.setField(rateLimiter, "refillTokenPerSecond", refillRate);

        // Add user to the SUT's internal user map
        @SuppressWarnings("unchecked")
        Map<String, User> users = (Map<String, User>) ReflectionTestUtils.getField(rateLimiter, "users");
        users.put(userId, new User(userId, "User " + userId));

        // For tests that need a specific starting state, we pre-populate the bucket.
        // For other scenarios, the SUT's computeIfAbsent would handle creation.
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, TokenBucketConfig> buckets = (ConcurrentHashMap<String, TokenBucketConfig>) ReflectionTestUtils.getField(rateLimiter, "buckets");
        TokenBucketConfig bucket = new TokenBucketConfig(initialTokens, refillRate, System.currentTimeMillis());
        buckets.put(userId, bucket);

        return bucket;
    }
}
