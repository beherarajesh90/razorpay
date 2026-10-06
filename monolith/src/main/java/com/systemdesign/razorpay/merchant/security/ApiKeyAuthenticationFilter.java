package com.systemdesign.razorpay.merchant.security;

import com.systemdesign.razorpay.common.exception.RateLimitException;
import com.systemdesign.razorpay.common.ratelimit.RateLimitResult;
import com.systemdesign.razorpay.common.ratelimit.RateLimiter;
import com.systemdesign.razorpay.merchant.cache.ApiKeyCache;
import com.systemdesign.razorpay.merchant.cache.ApiKeyCacheEntry;
import com.systemdesign.razorpay.merchant.entity.ApiKey;
import com.systemdesign.razorpay.merchant.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String BASIC_PREFIX = "Basic ";

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final MerchantContext merchantContext;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final ApiKeyCache apiKeyCache;
    private final RateLimiter rateLimiter;

    @Value("${app.rate-limit.use-case.api-key.max-requests}")
    private Integer maxRequests;

    @Value("${app.rate-limit.use-case.api-key.window-seconds}")
    private Long windowSeconds;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("Incoming request: {}", request.getRequestURI());

        try {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith(BASIC_PREFIX)){
                filterChain.doFilter(request, response);
                return;
            }
//          Authorization: Basic key_asdlfjaosduf:secret_asdflauouadf
//          Authorization: Basic ASDFUAOSJDFLAKSJDFA89SDUFLIJalsdjflakjsdflk==

            String[] credentials = decode(header);

            if(credentials == null){
                throw new BadRequestException("Malformed API key header");
            }

            String keyId = credentials[0];
            String rawSecret = credentials[1];

            ApiKeyCacheEntry apiKeyCacheEntry = apiKeyCache.get(keyId)
                    .orElseGet(() -> loadAndCache(keyId));

            if(apiKeyCacheEntry!=null && !apiKeyCacheEntry.enabled() || !secretMatches(rawSecret, apiKeyCacheEntry)){
                throw new BadRequestException("Invalid or missing API key");
            }

            RateLimitResult rateLimitResult = rateLimiter.checkRateLimit(apiKeyCacheEntry.keyId(), maxRequests, windowSeconds);
            if(!rateLimitResult.isAllowed()){
                log.warn("Too many requests for keyId: {}", keyId);
                throw new RateLimitException("Too many requests", rateLimitResult.retryAfterSeconds());
            }

            var auth = new UsernamePasswordAuthenticationToken(keyId, null,
                    List.of(new SimpleGrantedAuthority("API_KEY_ROLE")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            merchantContext.setMerchantId(apiKeyCacheEntry.merchantId());
            merchantContext.setKeyId(apiKeyCacheEntry.keyId());

            filterChain.doFilter(request, response);

        } catch (Exception e){
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }

    private ApiKeyCacheEntry loadAndCache(String keyId) {
        ApiKey apiKey = apiKeyRepository.findByKeyId(keyId).orElseGet(null);

        ApiKeyCacheEntry apiKeyCacheEntry = new ApiKeyCacheEntry(
                keyId,apiKey.getKeySecretHash(), apiKey.getPreviousKeySecretHash(), apiKey.getGracePeriodExpiresAt(),
                apiKey.getMerchant().getId(),apiKey.getEnvironment(), apiKey.isEnabled()
        );

        apiKeyCache.put(keyId, apiKeyCacheEntry);
        return apiKeyCacheEntry;
    }

    private boolean secretMatches(String rawSecret, ApiKeyCacheEntry apiKeyCacheEntry) {
        if(passwordEncoder.matches(rawSecret, apiKeyCacheEntry.keySecretHash())){
            return true;
        }

        return apiKeyCacheEntry.isInGracePeriod() && apiKeyCacheEntry.previousKeySecretHash()!=null
                && passwordEncoder.matches(rawSecret, apiKeyCacheEntry.previousKeySecretHash());
    }

    private String[] decode(String header) {
        String encoded = header.substring(BASIC_PREFIX.length());
        String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);

        int colon = decoded.indexOf(":");
        if(colon < 1) return null;

        return new String[]{decoded.substring(0, colon), decoded.substring(colon+1)};
    }
}
