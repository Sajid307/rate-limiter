package com.sd.rate_limiter.service.implementation;

import com.sd.rate_limiter.config.LeakingBucketConfig;
import com.sd.rate_limiter.config.TokenBucketConfig;
import com.sd.rate_limiter.dto.AllowRequestDTO;
import com.sd.rate_limiter.entity.User;
import com.sd.rate_limiter.service.RateLimiterService;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Queue;

@Service
public class LeakingBucketRateLimiterImpl implements RateLimiterService {

    private final Queue<Integer> bucket;

    private final LeakingBucketConfig config;

    LeakingBucketRateLimiterImpl(LeakingBucketConfig config) {
        this.config = config;
        bucket = new ArrayDeque<>();
    }


    @Override
    public AllowRequestDTO getTokens(String user) {
        return null;
    }

    @Override
    public void addTokens(String user) {

    }

    @Override
    public AllowRequestDTO allowRequests(String request) {

        if(bucket.isEmpty() || bucket.size() < config.getCapacity()) {
            bucket.offer(Integer.valueOf(request));
        }

        AllowRequestDTO allowRequestDTO = new AllowRequestDTO();

        if (!(bucket.poll() == null)) {
            allowRequestDTO.setAllowRequest(true);
        }
        else {
            allowRequestDTO.setAllowRequest(false);
        }
        allowRequestDTO.setUser(new User(request.substring(0), request.substring(1)));
        allowRequestDTO.setTokenBucket((TokenBucketConfig) bucket);

        return allowRequestDTO;
    }
}
