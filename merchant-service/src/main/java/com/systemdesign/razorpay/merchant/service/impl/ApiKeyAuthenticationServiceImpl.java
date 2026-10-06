package com.systemdesign.razorpay.merchant.service.impl;

import com.systemdesign.razorpay.common.dto.AuthPrincipal;
import com.systemdesign.razorpay.common.exception.RateLimitException;
import com.systemdesign.razorpay.common.ratelimit.RateLimitResult;
import com.systemdesign.razorpay.common.ratelimit.RateLimiter;
import com.systemdesign.razorpay.merchant.cache.ApiKeyCache;
import com.systemdesign.razorpay.merchant.cache.ApiKeyCacheEntry;
import com.systemdesign.razorpay.merchant.entity.ApiKey;
import com.systemdesign.razorpay.merchant.repository.ApiKeyRepository;
import com.systemdesign.razorpay.merchant.service.ApiKeyAuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthenticationServiceImpl implements ApiKeyAuthenticationService {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyCache apiKeyCache;
    private final RateLimiter rateLimiter;

    @Value("${app.rate-limit.use-case.api-key.max-requests}")
    private Integer maxRequests;

    @Value("${app.rate-limit.use-case.api-key.window-seconds}")
    private Long windowSeconds;

    @Override
    public AuthPrincipal authenticate(String keyId, String rawSecret) {
        ApiKeyCacheEntry entry = apiKeyCache.get(keyId).orElseGet(() -> loadAndCache(keyId));

        if (!entry.enabled() || !secretMatches(rawSecret, entry)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing API key");
        }

        RateLimitResult rateLimitResult = rateLimiter.checkRateLimit(entry.keyId(), maxRequests, windowSeconds);
        if (!rateLimitResult.isAllowed()) {
            log.warn("Too many requests for keyId: {}", keyId);
            throw new RateLimitException("Too many requests", rateLimitResult.retryAfterSeconds());
        }

        return new AuthPrincipal(entry.merchantId(), entry.keyId(), null, null);
    }

    private ApiKeyCacheEntry loadAndCache(String keyId) {
        ApiKey apiKey = apiKeyRepository.findByKeyId(keyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing API key"));

        ApiKeyCacheEntry entry = new ApiKeyCacheEntry(
                keyId, apiKey.getKeySecretHash(), apiKey.getPreviousKeySecretHash(), apiKey.getGracePeriodExpiresAt(),
                apiKey.getMerchant().getId(), apiKey.getEnvironment(), apiKey.isEnabled()
        );

        apiKeyCache.put(keyId, entry);
        return entry;
    }

    private boolean secretMatches(String rawSecret, ApiKeyCacheEntry entry) {
        if (passwordEncoder.matches(rawSecret, entry.keySecretHash())) {
            return true;
        }

        return entry.isInGracePeriod() && entry.previousKeySecretHash() != null
                && passwordEncoder.matches(rawSecret, entry.previousKeySecretHash());
    }
}
