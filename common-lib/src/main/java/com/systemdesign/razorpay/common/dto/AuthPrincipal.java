package com.systemdesign.razorpay.common.dto;

import java.util.UUID;

/**
 * Result of authenticating a request at the gateway. Forwarded downstream as headers.
 *
 * @param merchantId merchant the caller acts for
 * @param keyId      API key id when the caller used an API key, null for JWT callers
 * @param subject    user email for JWT callers, null for API key callers
 * @param role       user role for JWT callers, null for API key callers
 */
public record AuthPrincipal(UUID merchantId, String keyId, String subject, String role) {
}
