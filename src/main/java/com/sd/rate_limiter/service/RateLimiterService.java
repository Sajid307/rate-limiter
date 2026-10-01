package com.sd.rate_limiter.service;

public interface RateLimiterService {

    default void addTokensForUser() {
        addTokens("sajid");
    }

    Integer getTokens(String user);

    void addTokens(String user);
}
