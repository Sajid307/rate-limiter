package com.sd.rate_limiter.service;

import com.sd.rate_limiter.dto.AllowRequestDTO;

public interface RateLimiterService {

    AllowRequestDTO getTokens(String user);

    void addTokens(String user);

    AllowRequestDTO allowRequests(String request);
}
