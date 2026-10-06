package com.systemdesign.razorpay.gateway.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Identity headers come only from here. Any X-Merchant-Id / X-Key-Id / Authorization sent by the
 * client is hidden, so a caller cannot pick its own merchant. A null merchantId means no identity.
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
        switch (name.toLowerCase(Locale.ROOT)) {
            case "authorization":
                return null;
            case "x-merchant-id":
                return merchantId == null ? null : merchantId.toString();
            case "x-key-id":
                return keyId;
            default:
                return super.getHeader(name);
        }
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
            if (!isGatewayOwned(name)) {
                names.add(name);
            }
        }
        if (merchantId != null) {
            names.add(HEADER_MERCHANT_ID);
        }
        if (keyId != null) {
            names.add(HEADER_KEY_ID);
        }
        return Collections.enumeration(names);
    }

    private boolean isGatewayOwned(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.equals(AUTHORIZATION.toLowerCase(Locale.ROOT))
                || lower.equals(HEADER_MERCHANT_ID.toLowerCase(Locale.ROOT))
                || lower.equals(HEADER_KEY_ID.toLowerCase(Locale.ROOT));
    }
}
