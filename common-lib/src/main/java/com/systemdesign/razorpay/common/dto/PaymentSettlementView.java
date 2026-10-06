package com.systemdesign.razorpay.common.dto;

import com.systemdesign.razorpay.common.entity.Money;

import java.util.UUID;

/**
 * Captured payment as seen by operations-service when building a settlement.
 */
public record PaymentSettlementView(UUID paymentId, UUID merchantId, Money amount) {
}
