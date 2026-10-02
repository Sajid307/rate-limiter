package com.sd.rate_limiter.service.implementation;

import com.sd.rate_limiter.AllowRequestDTO;
import com.sd.rate_limiter.entity.TokenBucket;
import com.sd.rate_limiter.entity.User;
import com.sd.rate_limiter.service.RateLimiterService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Primary
public class TokenBucketRateLimiterImpl implements RateLimiterService {

    private final ConcurrentHashMap<Long, TokenBucket> tokenBucket;
    private final List<User> user;

    @Value("${tokens.max_capacity}")
    private long tokenCapacity;

    @Value("${tokens.refillTokenPerSec}")
    private long refillTokenPerSecond;

    public TokenBucketRateLimiterImpl(List<User> user) {
        this.user = user;
        this.tokenBucket = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {

        for (int i = 0; i<10; i++) {
            user.add(new User(i, "User" + i));
            tokenBucket.put(user.get(i).getUserId(), new TokenBucket(tokenCapacity, refillTokenPerSecond, System.currentTimeMillis()));
        }
    }


    @Override
    public void addTokens(String username) {

    }

    @Override
    public Integer getTokens(String user) {
        return 0;
    }

    @Override
    public AllowRequestDTO allowRequests(long requestId) {

        AllowRequestDTO allowRequestDTO = new AllowRequestDTO();

        if (tokenBucket.get(requestId).getCapacity() > 0) {
            tokenBucket.get(requestId).setCapacity(tokenBucket.get(requestId).getCapacity()-1);
            allowRequestDTO.setAllowRequest(true);
            allowRequestDTO.setUser(new User(user.get((int) requestId).getUserId(), user.get((int) requestId).getUserName()));
            allowRequestDTO.setTokenBucket(tokenBucket.get(requestId));
        }
        updateTokens();
        return allowRequestDTO;
    }


    private void updateTokens() {
        for (Map.Entry<Long, TokenBucket> map : tokenBucket.entrySet()) {
            if(map.getValue().getCapacity() < tokenCapacity) {
                try {
                    refill(map);
                }
                finally {
                    System.out.println("do nothing");
                }
            }
        }
    }

    private void refill(Map.Entry<Long, TokenBucket> map) {
        long elapsedTime = System.currentTimeMillis() - map.getValue().getLastRefillTimeStamp();

        if (elapsedTime > 0) {
            long refillTokens = elapsedTime/1000 * map.getValue().getRefillTokenPerSecond();
            long currentCapacity = map.getValue().getCapacity();
            map.setValue(new TokenBucket(
                    Math.min(tokenCapacity, currentCapacity + refillTokens),
                    map.getValue().getRefillTokenPerSecond(),
                    System.currentTimeMillis()));
        }
    }
}
