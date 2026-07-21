package com.systemdesign.razorpay.payment.service;

import com.systemdesign.razorpay.payment.dto.request.PaymentInitRequest;
import com.systemdesign.razorpay.payment.dto.response.PaymentResponse;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse initiate(UUID merchantId, @Valid PaymentInitRequest request);
}
