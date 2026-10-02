package com.sd.rate_limiter;

import com.sd.rate_limiter.entity.TokenBucket;
import com.sd.rate_limiter.entity.User;

public class AllowRequestDTO {

    private boolean allowRequest;

    private User user;

    private TokenBucket tokenBucket;

    public TokenBucket getTokenBucket() {
        return tokenBucket;
    }

    public void setTokenBucket(TokenBucket tokenBucket) {
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
