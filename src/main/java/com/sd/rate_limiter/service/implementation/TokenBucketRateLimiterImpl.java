package com.sd.rate_limiter.service.implementation;

import com.sd.rate_limiter.config.TokenBucketConfig;
import com.sd.rate_limiter.dto.AllowRequestDTO;
import com.sd.rate_limiter.entity.User;
import com.sd.rate_limiter.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Primary
public class TokenBucketRateLimiterImpl implements RateLimiterService {

    private final ConcurrentHashMap<String, TokenBucketConfig> buckets;
    private final Map<String, User> users;

    @Value("${tokens.max_capacity}")
    private long tokenCapacity;

    @Value("${tokens.refillTokenPerSec}")
    private long refillTokenPerSecond;

    public TokenBucketRateLimiterImpl(List<User> userList) {
        // Store users in a map for efficient O(1) lookup.
        this.users = userList.stream().collect(Collectors.toConcurrentMap(User::getUserId, Function.identity()));
        this.buckets = new ConcurrentHashMap<>();
    }

    @Override
    public AllowRequestDTO allowRequests(String requestId) {
        // Lazily create a bucket for a user on their first request.
        TokenBucketConfig bucket = buckets.computeIfAbsent(requestId, id ->
                new TokenBucketConfig(tokenCapacity, refillTokenPerSecond, System.currentTimeMillis()));

        // Synchronize on the individual bucket to ensure thread safety per user
        // without blocking requests for other users.
        synchronized (bucket) {
            refill(bucket);

            AllowRequestDTO dto = new AllowRequestDTO();
            dto.setUser(users.get(requestId));

            if (bucket.getCapacity() > 0) {
                bucket.setCapacity(bucket.getCapacity() - 1);
                dto.setAllowRequest(true);
            } else {
                dto.setAllowRequest(false);
            }


            // Always return a snapshot of the bucket's state for client-side info.
            // A new object prevents external modification of the internal state.
            dto.setTokenBucket(new TokenBucketConfig(bucket.getCapacity(), bucket.getRefillTokenPerSecond(), bucket.getLastRefillTimeStamp()));
            return dto;
        }
    }

    private void refill(TokenBucketConfig bucket) {
        long now = System.currentTimeMillis();
        long elapsedTime = now - bucket.getLastRefillTimeStamp();

        if (elapsedTime > 0) {
            // FIX: Multiply before dividing to handle fractional time with integer arithmetic.
            long tokensToAdd = (elapsedTime * bucket.getRefillTokenPerSecond()) / 1000;

            if (tokensToAdd > 0) {
                long newCapacity = Math.min(tokenCapacity, bucket.getCapacity() + tokensToAdd);
                bucket.setCapacity(newCapacity);
                bucket.setLastRefillTimeStamp(now);
            }
        }
    }

    // Unimplemented methods from the interface
    @Override
    public void addTokens(String username) {
        // Implementation needed
    }

    @Override
    public AllowRequestDTO getTokens(String user) {
        AllowRequestDTO allowRequestDTO = new AllowRequestDTO();

        allowRequestDTO.setAllowRequest(true);
        allowRequestDTO.setUser(users.get(user));
        allowRequestDTO.setTokenBucket(buckets.get(user));

        return allowRequestDTO;
    }
}






