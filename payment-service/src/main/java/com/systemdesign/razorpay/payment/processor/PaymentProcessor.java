package com.systemdesign.razorpay.payment.processor;

import com.systemdesign.razorpay.common.dto.PaymentProcessorRequest;
import com.systemdesign.razorpay.common.dto.PaymentProcessorResponse;

public interface PaymentProcessor {

    PaymentProcessorResponse charge(PaymentProcessorRequest request);
}
