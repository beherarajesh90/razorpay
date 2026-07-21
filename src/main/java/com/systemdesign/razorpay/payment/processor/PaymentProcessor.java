package com.systemdesign.razorpay.payment.processor;

import com.systemdesign.razorpay.payment.processor.dto.request.PaymentProcessorRequest;
import com.systemdesign.razorpay.payment.processor.dto.response.PaymentProcessorResponse;

public interface PaymentProcessor {

    PaymentProcessorResponse charge(PaymentProcessorRequest request);
}
