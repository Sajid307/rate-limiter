package com.sd.rate_limiter.controller;

import com.sd.rate_limiter.service.RateLimiterService;
import org.apache.catalina.util.RateLimiter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/rate-limiter")
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    public RateLimiterController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/addTokens/{username}")
    public ResponseEntity<Void> addTokens(@PathVariable String username) {
        rateLimiterService.addTokens(username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/getTokens/{username}")
    public ResponseEntity<Integer> getTokens(@PathVariable String username) {
        rateLimiterService.addTokensForUser();
        return ResponseEntity.ok(rateLimiterService.getTokens(username));
    }
}
