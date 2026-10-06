package com.systemdesign.razorpay.payment.service;

import com.systemdesign.razorpay.payment.dto.request.RefundRequest;
import com.systemdesign.razorpay.payment.dto.response.RefundResponse;

import java.util.UUID;

public interface RefundService {
    RefundResponse initiateRefund(UUID paymentId, UUID merchantId, RefundRequest refundRequest, String idempotencyKey);

    RefundResponse getRefund(UUID refundId, UUID merchantId);

    void processRefund(UUID refundId);
}
