package com.sd.rate_limiter.service.implementation;

import com.sd.rate_limiter.service.RateLimiterService;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterImpl implements RateLimiterService {

    private static final ConcurrentHashMap<String, Integer> concurrentHashMap = new ConcurrentHashMap<>();

    private static final int MAX_TOKENS = 10;


    @Override
    public Integer getTokens(String user) {
        return concurrentHashMap.get(user);
    }

    @Override
    public void addTokens(String user) {
        concurrentHashMap.put(user, MAX_TOKENS);

    }
}
