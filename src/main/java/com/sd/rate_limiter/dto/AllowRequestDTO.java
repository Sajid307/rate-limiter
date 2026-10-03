package com.sd.rate_limiter.dto;

import com.sd.rate_limiter.config.TokenBucketConfig;
import com.sd.rate_limiter.entity.User;

public class AllowRequestDTO {

    private boolean allowRequest;

    private User user;

    private TokenBucketConfig tokenBucket;

    public TokenBucketConfig getTokenBucket() {
        return tokenBucket;
    }

    public void setTokenBucket(TokenBucketConfig tokenBucket) {
        this.tokenBucket = tokenBucket;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isAllowRequest() {
        return allowRequest;
    }

    public void setAllowRequest(boolean allowRequest) {
        this.allowRequest = allowRequest;
    }
}
