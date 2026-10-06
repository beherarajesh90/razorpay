package com.systemdesign.razorpay.gateway.security;

import com.systemdesign.razorpay.gateway.config.GatewayRoutesProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Verifies the caller at the edge. On success, forwards the request with X-Merchant-Id and
 * X-Key-Id headers and without the Authorization header.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class GatewayAuthFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION = "Authorization";

    private final RestClient authRestClient;
    private final GatewayRoutesProperties routes;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (isPublic(request.getRequestURI())) {
            // No identity on public routes, but still strip any client-sent identity headers.
            filterChain.doFilter(new HeaderAugmentingRequestWrapper(request, null, null), response);
            return;
        }

        String authorization = request.getHeader(AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing Authorization header");
            return;
        }

        GatewayPrincipal principal;
        try {
            principal = authRestClient.post()
                    .uri("/internal/v1/auth/verify")
                    .header(AUTHORIZATION, authorization)
                    .retrieve()
                    .body(GatewayPrincipal.class);
        } catch (RestClientResponseException e) {
            log.warn("Auth rejected for {} with status {}", request.getRequestURI(), e.getStatusCode());
            response.sendError(e.getStatusCode().value(), "Authentication failed");
            return;
        } catch (ResourceAccessException e) {
            log.error("merchant-service unreachable during auth", e);
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Auth service unavailable");
            return;
        }

        if (principal == null || principal.merchantId() == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
            return;
        }

        filterChain.doFilter(new HeaderAugmentingRequestWrapper(request, principal.merchantId(), principal.keyId()), response);
    }

    private boolean isPublic(String uri) {
        return routes.getPublicPaths().stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    /**
     * Mirrors merchant-service's AuthPrincipal JSON. Kept local so the gateway has no common-lib dependency.
     */
    public record GatewayPrincipal(UUID merchantId, String keyId, String subject, String role) {
    }
}
