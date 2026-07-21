package com.systemdesign.razorpay.payment.processor.dto.request;

import com.systemdesign.razorpay.common.entity.Money;
import com.systemdesign.razorpay.common.enums.PaymentMethod;

import java.util.Map;

public record PaymentProcessorRequest(
        Money amount,
        PaymentMethod method,
        Map<String, Object> methodDetails
) {
}
