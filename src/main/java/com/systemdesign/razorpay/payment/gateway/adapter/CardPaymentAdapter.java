package com.systemdesign.razorpay.payment.gateway.adapter;

import com.systemdesign.razorpay.payment.gateway.PaymentAdapter;
import com.systemdesign.razorpay.payment.gateway.dto.request.PaymentRequest;
import com.systemdesign.razorpay.payment.gateway.dto.respose.PaymentResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CardPaymentAdapter implements PaymentAdapter {

    @Override
    public PaymentResult initiate(PaymentRequest request) {
        return null;
    }

    @Override
    public PaymentResult capture(UUID paymentId) {
        return null;
    }
}
