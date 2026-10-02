package com.sd.rate_limiter.entity;



public class TokenBucket {

    private long capacity;
    private long refillTokenPerSecond;

    private long lastRefillTimeStamp;


    public TokenBucket(long capacity, long refillTokenPerSecond, long lastRefillTimeStamp) {
        this.capacity = capacity;
        this.refillTokenPerSecond = refillTokenPerSecond;
        this.lastRefillTimeStamp = lastRefillTimeStamp;
    }

    public long getLastRefillTimeStamp() {
        return lastRefillTimeStamp;
    }

    public void setLastRefillTimeStamp(long lastRefillTimeStamp) {
        this.lastRefillTimeStamp = lastRefillTimeStamp;
    }

    public long getRefillTokenPerSecond() {
        return refillTokenPerSecond;
    }

    public void setRefillTokenPerSecond(long refillTokenPerSecond) {
        this.refillTokenPerSecond = refillTokenPerSecond;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }
}
