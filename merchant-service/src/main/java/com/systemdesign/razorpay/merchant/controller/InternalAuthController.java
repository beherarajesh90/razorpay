package com.systemdesign.razorpay.merchant.controller;

import com.systemdesign.razorpay.common.dto.AuthPrincipal;
import com.systemdesign.razorpay.merchant.security.JwtUtil;
import com.systemdesign.razorpay.merchant.service.ApiKeyAuthenticationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Called by api-gateway only. Verifies the Authorization header and returns the caller identity.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/auth")
public class InternalAuthController {

    private static final String BASIC_PREFIX = "Basic ";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final JwtUtil jwtUtil;

    @PostMapping("/verify")
    public AuthPrincipal verify(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization header");
        }
        if (authorization.startsWith(BASIC_PREFIX)) {
            String[] credentials = decodeBasic(authorization);
            return apiKeyAuthenticationService.authenticate(credentials[0], credentials[1]);
        }
        if (authorization.startsWith(BEARER_PREFIX)) {
            return verifyJwt(authorization.substring(BEARER_PREFIX.length()));
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unsupported Authorization scheme");
    }

    private AuthPrincipal verifyJwt(String token) {
        try {
            Claims claims = jwtUtil.validateAccessToken(token);
            return new AuthPrincipal(
                    UUID.fromString(jwtUtil.extractMerchantId(claims)),
                    null,
                    claims.getSubject(),
                    jwtUtil.extractRole(claims)
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }

    private String[] decodeBasic(String header) {
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(header.substring(BASIC_PREFIX.length())), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed API key header");
        }
        int colon = decoded.indexOf(':');
        if (colon < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed API key header");
        }
        return new String[]{decoded.substring(0, colon), decoded.substring(colon + 1)};
    }
}
