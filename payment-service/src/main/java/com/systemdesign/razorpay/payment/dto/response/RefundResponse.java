package com.systemdesign.razorpay.payment.dto.response;

import com.systemdesign.razorpay.common.entity.Money;
import com.systemdesign.razorpay.common.enums.RefundStatus;

import java.util.UUID;

public record RefundResponse(
        UUID refundId,
        UUID paymentId,
        Money amount,
        RefundStatus status,
        String bankReference,
        String errorCode,
        String errorDescription
) {
}
