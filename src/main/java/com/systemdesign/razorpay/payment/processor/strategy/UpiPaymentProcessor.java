package com.systemdesign.razorpay.payment.processor.strategy;

import com.systemdesign.razorpay.payment.processor.PaymentProcessor;
import com.systemdesign.razorpay.payment.processor.dto.request.PaymentProcessorRequest;
import com.systemdesign.razorpay.payment.processor.dto.response.PaymentProcessorResponse;

public class UpiPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentProcessorResponse charge(PaymentProcessorRequest request) {
        return null;
    }
}
