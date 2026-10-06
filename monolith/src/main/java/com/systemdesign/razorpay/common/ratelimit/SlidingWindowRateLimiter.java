package com.systemdesign.razorpay.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rate-limit.method", havingValue = "slidingWindow")
public class SlidingWindowRateLimiter implements RateLimiter{

    private final StringRedisTemplate redis;

    @Override
    public RateLimitResult checkRateLimit(String key, int maxRequestAllowed, long windowSeconds) {
        long nowMs = System.currentTimeMillis();
        long floorMs = nowMs - windowSeconds*1000;

        String redisKey = "rate_limit:sliding_window:" + key;

        var zSet = redis.opsForZSet();
        zSet.removeRangeByScore(redisKey, Double.NEGATIVE_INFINITY, floorMs);

        Long zCard = zSet.zCard(redisKey);
        long current = zCard!= null ? zCard : 0;

        if(current >= maxRequestAllowed){

        }

        zSet.add(redisKey, UUID.randomUUID().toString(), nowMs);
        redis.expire(redisKey, Duration.ofSeconds(windowSeconds+1));
        return RateLimitResult.allowed((int)(maxRequestAllowed - current - 1));
    }
}
