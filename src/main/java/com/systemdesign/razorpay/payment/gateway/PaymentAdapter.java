package com.systemdesign.razorpay.payment.gateway;

import com.systemdesign.razorpay.common.entity.Money;
import com.systemdesign.razorpay.payment.gateway.dto.request.PaymentRequest;
import com.systemdesign.razorpay.payment.gateway.dto.respose.PaymentResult;

import java.util.UUID;

public interface PaymentAdapter {

    PaymentResult initiate(PaymentRequest request);

    PaymentResult capture(UUID paymentId);

    PaymentResult refund(UUID paymentId, Money amount);
}
