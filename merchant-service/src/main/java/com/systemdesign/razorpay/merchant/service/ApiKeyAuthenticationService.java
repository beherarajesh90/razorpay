package com.systemdesign.razorpay.merchant.service;

import com.systemdesign.razorpay.common.dto.AuthPrincipal;

/**
 * Verifies an API key from a Basic Authorization header (keyId:secret, base64 encoded).
 */
public interface ApiKeyAuthenticationService {

    AuthPrincipal authenticate(String keyId, String rawSecret);
}
