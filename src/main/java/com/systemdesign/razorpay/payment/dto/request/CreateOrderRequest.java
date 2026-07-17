package com.systemdesign.razorpay.payment.dto.request;

import com.systemdesign.razorpay.common.entity.Money;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Map;

public record CreateOrderRequest(
        @NotNull
        Money amount,

        @Size(max = 100)
        String receipt,

        Map<String, Object> notes,  // orderId (known to merchant)

        LocalDateTime expiresAt
) {
}
