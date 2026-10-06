package com.systemdesign.razorpay.gateway.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Adds identity headers and hides Authorization from downstream services.
 */
public class HeaderAugmentingRequestWrapper extends HttpServletRequestWrapper {

    static final String HEADER_MERCHANT_ID = "X-Merchant-Id";
    static final String HEADER_KEY_ID = "X-Key-Id";
    private static final String AUTHORIZATION = "Authorization";

    private final UUID merchantId;
    private final String keyId;

    public HeaderAugmentingRequestWrapper(HttpServletRequest request, UUID merchantId, String keyId) {
        super(request);
        this.merchantId = merchantId;
        this.keyId = keyId;
    }

    @Override
    public String getHeader(String name) {
        if (AUTHORIZATION.equalsIgnoreCase(name)) {
            return null;
        }
        if (HEADER_MERCHANT_ID.equalsIgnoreCase(name)) {
            return merchantId.toString();
        }
        if (HEADER_KEY_ID.equalsIgnoreCase(name) && keyId != null) {
            return keyId;
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String value = getHeader(name);
        if (value == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(Collections.singletonList(value));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new LinkedHashSet<>();
        Enumeration<String> original = super.getHeaderNames();
        while (original.hasMoreElements()) {
            String name = original.nextElement();
            if (!AUTHORIZATION.equalsIgnoreCase(name)) {
                names.add(name);
            }
        }
        names.add(HEADER_MERCHANT_ID);
        if (keyId != null) {
            names.add(HEADER_KEY_ID);
        }
        return Collections.enumeration(names);
    }
}
