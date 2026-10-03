package com.sd.rate_limiter.config;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokenBucketConfig {

    private long capacity;
    private long refillTokenPerSecond;
    private long lastRefillTimeStamp;

    public TokenBucketConfig(long capacity, long refillTokenPerSecond, long lastRefillTimeStamp) {
        this.capacity = capacity;
        this.refillTokenPerSecond = refillTokenPerSecond;
        this.lastRefillTimeStamp = lastRefillTimeStamp;
    }

}
