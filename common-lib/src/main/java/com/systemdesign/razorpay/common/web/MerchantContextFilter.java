package com.systemdesign.razorpay.common.web;

import com.systemdesign.razorpay.common.context.MerchantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Copies identity headers set by the api-gateway into the request-scoped MerchantContext.
 * Requests without the headers (service-to-service calls) pass through untouched.
 */
@Component
@RequiredArgsConstructor
public class MerchantContextFilter extends OncePerRequestFilter {

    public static final String HEADER_MERCHANT_ID = "X-Merchant-Id";
    public static final String HEADER_KEY_ID = "X-Key-Id";

    private final MerchantContext merchantContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String merchantId = request.getHeader(HEADER_MERCHANT_ID);
        if (merchantId != null && !merchantId.isBlank()) {
            merchantContext.setMerchantId(UUID.fromString(merchantId));
            merchantContext.setKeyId(request.getHeader(HEADER_KEY_ID));
        }
        filterChain.doFilter(request, response);
    }
}
