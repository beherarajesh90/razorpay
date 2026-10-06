package com.systemdesign.razorpay.common.dto;

import com.systemdesign.razorpay.common.entity.Money;

import java.util.Map;
import java.util.UUID;

/**
 * payment-service -> vault-service: charge a stored card token.
 */
public record VaultChargeRequest(
        UUID paymentId,
        String token,
        Money amount,
        Map<String, Object> methodDetails
) {
}
