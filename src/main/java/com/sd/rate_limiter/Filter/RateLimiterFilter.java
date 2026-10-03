package com.sd.rate_limiter.Filter;

import com.sd.rate_limiter.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimiterFilter extends OncePerRequestFilter {

    private RateLimiterService rateLimiterService;

    private static final int TOO_MANY_REQUESTS = 429;

    public RateLimiterFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("RateLimiterFilter.doFilterInternal");
        String user = request.getHeader("x-userid");
        String uri = request.getRequestURI();

        String key = user + ":" + uri;

        if (!rateLimiterService.allowRequests(key).isAllowRequest()) {
            response.setStatus(TOO_MANY_REQUESTS);
            response.getWriter().write("Try again after 1 sec");
        }
        else {
            filterChain.doFilter(request, response);
        }
    }
}
