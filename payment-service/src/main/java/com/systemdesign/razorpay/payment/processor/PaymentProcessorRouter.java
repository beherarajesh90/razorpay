package com.systemdesign.razorpay.payment.processor;

import com.systemdesign.razorpay.common.enums.PaymentMethod;
import com.systemdesign.razorpay.payment.entity.Payment;
import com.systemdesign.razorpay.common.dto.PaymentProcessorRequest;
import com.systemdesign.razorpay.common.dto.PaymentProcessorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentProcessorRouter {

    private final Map<PaymentMethod, PaymentProcessor> paymentProcessors;

    public PaymentProcessorResponse charge(PaymentProcessorRequest request){
        PaymentProcessor paymentProcessor = paymentProcessors.get(request.method());
        if(paymentProcessor == null){
            throw new IllegalArgumentException("No payment processor registered for method: "+ request.method());
        }

        return paymentProcessor.charge(request);
    }
}
