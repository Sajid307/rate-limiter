package com.sd.rate_limiter.service;

import com.sd.rate_limiter.AllowRequestDTO;

public interface RateLimiterService {

    Integer getTokens(String user);

    void addTokens(String user);

    AllowRequestDTO allowRequests(long request);
}
