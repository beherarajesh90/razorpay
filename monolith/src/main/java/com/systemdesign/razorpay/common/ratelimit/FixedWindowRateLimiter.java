package com.systemdesign.razorpay.common.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rate-limit.method", havingValue = "fixedWindow")
public class FixedWindowRateLimiter implements RateLimiter{

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public RateLimitResult checkRateLimit(String key, int maxRequestsAllowed, long windowSeconds) {
        String redisKey = "rate_limit:fixedWindow" + key;
        Long count = stringRedisTemplate.opsForValue().increment(redisKey);

        if(count == null) return RateLimitResult.allowed(maxRequestsAllowed);   // redis unavailable

        if(count == 1){
            stringRedisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }

        if(count > maxRequestsAllowed){
            Long ttl = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            int retryAfter = ttl!=null && ttl>0 ? ttl.intValue() : (int) windowSeconds;
            return RateLimitResult.denied(retryAfter);
        }

        return RateLimitResult.allowed((int)(maxRequestsAllowed - count));
    }
}
