package com.sd.rate_limiter.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeakingBucketConfig {

    private final int capacity;
    private final int requestPerSecond;


    public LeakingBucketConfig(int capacity, int requestPerSecond) {
        this.capacity = capacity;
        this.requestPerSecond = requestPerSecond;
    }


}
