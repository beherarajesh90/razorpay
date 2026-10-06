package com.systemdesign.razorpay.common.ratelimit;

public interface RateLimiter {

    RateLimitResult checkRateLimit(String key, int maxRequestsAllowed, long windowSeconds);
}
