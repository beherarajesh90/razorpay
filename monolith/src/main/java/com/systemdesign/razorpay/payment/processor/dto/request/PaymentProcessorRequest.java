package com.systemdesign.razorpay.payment.processor.dto.request;

import com.systemdesign.razorpay.common.entity.Money;
import com.systemdesign.razorpay.common.enums.PaymentMethod;

import java.util.Map;
import java.util.UUID;

public record PaymentProcessorRequest(
        UUID processingId,
        UUID paymentId,
        Money amount,
        PaymentMethod method,
        String pan,
        String expiry,
        Map<String, Object> methodDetails
) {

    public static PaymentProcessorRequest card(UUID paymentId, String pan, String expiry, Money amount, Map<String, Object> details){
        return new PaymentProcessorRequest(UUID.randomUUID(), paymentId, amount, PaymentMethod.CARD, pan, expiry, details);
    }

    public static PaymentProcessorRequest nonCard(UUID paymentId, PaymentMethod method, Money amount, Map<String, Object> details){
        return new PaymentProcessorRequest(UUID.randomUUID(), paymentId, amount, method, null, null, details);
    }
}
