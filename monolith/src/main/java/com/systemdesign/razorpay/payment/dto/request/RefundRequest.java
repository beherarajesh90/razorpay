package com.systemdesign.razorpay.payment.dto.request;

import jakarta.validation.constraints.Positive;

public record RefundRequest(
        @Positive Long amount,
        String reason
) {
}
